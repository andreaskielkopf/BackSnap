/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.backsnap.config.Log.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.config.Log;
import de.uhingen.kielkopf.andreas.backsnap.config.Log.LEVEL;
import de.uhingen.kielkopf.andreas.backsnap.gui.BacksnapGui;
import de.uhingen.kielkopf.andreas.backsnap.gui.part.SnapshotLabel.STATUS;
import de.uhingen.kielkopf.andreas.beans.shell.*;

/**
 * @author Andreas Kielkopf
 *
 */
public class Btrfs {
   /* Liefert eine Map der verfügbaren Volumes sortiert nach UUID */
   public static final String                     DEVICE_USAGE    ="btrfs device usage ";
   private static final String                    FILESYSTEM_SHOW ="btrfs filesystem show ";
   public static final String                     FILESYSTEM_USAGE="btrfs filesystem usage ";
   public static final String                     SUBVOLUME_LIST_1="btrfs subvolume list -apuqRs ";
   public static final String                     SUBVOLUME_LIST_2="btrfs subvolume list -apuqRcg ";
   public static final String                     SUBVOLUME_SHOW  ="btrfs subvolume show ";
   public static final String                     PROPERTY_SET    ="btrfs property set ";
   public static final String                     PROPERTY_GET    ="btrfs property get ";
   public static final String                     VERSION         ="btrfs version ";
   private static final String                    SEND            ="btrfs send ";
   public static final String                     RECEIVE         ="btrfs receive ";
   private static final String                    SUBVOLUME_DELETE="btrfs subvolume delete -v";
   private static final String                    SUBVOLUME_CREATE="btrfs subvolume create ";
   public static final String                     SUBVOLUME_LIST  ="btrfs subvolume list ";
   public static final String                     BALANCE_START   ="btrfs balance start ";
   public static final String                     BALANCE_STATUS  ="btrfs balance status ";
   private static final String                    SYS_RECLAIM_1   ="/sys/fs/btrfs/";
   private static final String                    SYS_RECLAIM_2   ="/allocation/data/bg_reclaim_threshold";
   private static final Pattern                   STD_MIN_        =Pattern.compile(" [0-9]:[0-9][0-9]:");
   private static String                          std_min_;                                                // =" 0:00:";
   public static final ReentrantReadWriteLock     BTRFS           =new ReentrantReadWriteLock(true);
   private static Boolean                         pvUsable        =null;
   private static int                             lastLine        =0;
   // private static boolean skip =false;
   private static ConcurrentLinkedQueue<Snapshot> removeQueue     =new ConcurrentLinkedQueue<>();
   /**
    * Löscht Backups im Auftrag der GUI
    * 
    * Übergeben wird eine Liste zu löschender Backups. Diese werden alle in eine queue eingetragen. Danach werden immer mehrere dieser Backups
    * gemeinsam gelöscht um nicht zu viele Commands absetzen zu müssen.
    */
   static public void removeSnapshots(List<Snapshot> list, AtomicBoolean deleteUnterbrechen, JButton jButton,
            BacksnapGui gui) {
      // SwingUtilities.invokeLater(()-> jButton.setEnabled(false));
      removeQueue.addAll(list);
      Thread.ofVirtual().start(() -> {
         try {
            Pc.mountBackupRoot(true);
            StringBuilder removeSB=new StringBuilder();// für den Befehl
            StringBuilder dirList=new StringBuilder();// für die Anzeige
            if (gui != null)
               gui.setDeleteInfo("");
            while (removeQueue.poll() instanceof Snapshot snap)
               if (!deleteUnterbrechen.get()) {
                  try {
                     Path cd=Pc.getBackupMount().mountPath().resolve(snap.btrfsPath().subpath(0, 2)); // gemeinsam
                     Path delete=snap.btrfsPath().subpath(2, 4);// individuell
                     if (!cd.toString().startsWith(Pc.TMP_BACKUP_ROOT.toString()) // nur im Backupvolume
                              || cd.toString().contains("../")// kein Pfad-traversal erlaubt
                              || delete.toString().contains("../"))// "
                        throw new SecurityException("I am not allowed to delete " + cd + delete);
                     removeSB.append(" " + delete);
                     dirList.append(" " + snap.dirName());
                     if (gui != null)
                        gui.mark(snap.received_uuid(), STATUS.INPROGRESS);// snap blau anzeigen
                     if (removeQueue.isEmpty() || removeSB.length() > 128) { // weiter sammeln oder jetzt ausführen
                        if (gui != null)
                           gui.setDeleteInfo(dirList.toString());// Info einblenden
                        if (gui != null)
                           gui.getPanelMaintenance().updateButtons();
                        // removeSB.setLength(removeSB.length() - 1);// letztes Zeichen entfernen
                        removeSB.insert(0, "cd " + cd + ";" + SUBVOLUME_DELETE);// Befehl davorsetzen
                        String removeCmd=snap.mount().pc().getCmd(removeSB, true); // Befehl ssh oder sudo
                        Log.logln(removeCmd, LEVEL.BTRFS);
                        BTRFS.writeLock().lock();
                        try (DirectCmdStreams removeStream=new DirectCmdStreams(removeCmd);
                                 BufferedCmdReader out=removeStream.out()) {
                           removeStream.print2Err();
                           out.lines().forEach(line -> {
                              Log.logln(line, LEVEL.DELETE);
                              if (gui != null)
                                 gui.lblPvSetText(line);
                           });
                        } catch (Exception e) {
                           e.printStackTrace();
                        } finally {
                           BTRFS.writeLock().unlock();
                           if (gui != null)
                              gui.getPanelMaintenance().updateButtons();
                        }
                        removeSB.setLength(0);
                        dirList.setLength(0);
                     }
                  } catch (IOException e) {
                     e.printStackTrace();
                  }
                  if (Backsnap.SINGLESNAPSHOT.get())
                     break;
               }
         } finally {
            if (gui != null)
               gui.refreshGUI();
            if (gui != null)
               gui.getPanelMaintenance().updateButtons();
            // SwingUtilities.invokeLater(()-> jButton.setEnabled(true));
         }
      });
   }
   public static ConcurrentSkipListMap<String, Volume> show(Pc pc, boolean onlyMounted, boolean refresh) {
      ConcurrentSkipListMap<String, Volume> list=new ConcurrentSkipListMap<>();
      String volumeListCmd=pc.getCmd(new StringBuilder(FILESYSTEM_SHOW).append(onlyMounted ? " -m" : " -d"), true);
      Log.logln(volumeListCmd, LEVEL.BTRFS);
      if (refresh)
         CmdStreams.removeFromCache(volumeListCmd);
      BTRFS.readLock().lock();
      try (CmdStreams volumeListStream=CmdStreams.getCachedStream(volumeListCmd)) {
         ArrayList<String> tmpList=new ArrayList<>();
         volumeListStream.outBGerr().forEachOrdered(line -> {
            // if (!line.isEmpty()) {
            if (!line.isBlank())
               tmpList.add(line);
            else
               if (!tmpList.isEmpty()) {
                  Volume v=Volume.getVolume(pc, tmpList);
                  list.put(v.uuid(), v);
                  tmpList.clear();
               }
            // }
         });
         volumeListStream.errPrintln();
      } catch (IOException e1) {
         e1.printStackTrace();
      } finally {
         BTRFS.readLock().unlock();
      }
      return list;
   }
   public static void createSubvolume(Pc pc, Path p) throws IOException {
      if (p instanceof Path backsnap && backsnap.equals(Pc.TMP_BACKSNAP)) {
         String createCmd=pc.getCmd(new StringBuilder(SUBVOLUME_CREATE).append(backsnap), true);
         Log.log(createCmd, LEVEL.BTRFS);
         BTRFS.writeLock().lock();
         try (CmdStreams createStream=CmdStreams.getCachedStream(createCmd)) {
            createStream.outBGerr().forEach(line -> Log.log(line, LEVEL.BTRFS));
            createStream.errPrintln();
         } finally {
            BTRFS.writeLock().unlock();
         }
      }
   }
   public static boolean testSubvolume(Pc pc, Path p) throws IOException {
      if (p instanceof Path backsnap && backsnap.equals(Pc.TMP_BACKSNAP)) {
         String testCmd=pc.getCmd(new StringBuilder(SUBVOLUME_LIST).append(backsnap), true);
         Log.log(testCmd, LEVEL.BTRFS);
         BTRFS.readLock().lock();
         try (CmdStreams testStream=CmdStreams.getDirectStream(testCmd)) {
            return testStream.outBGerr().peek(line -> Log.log(line, LEVEL.ALLES))
                     .anyMatch(line -> line.endsWith(Pc.TMP_BACKSNAP.getFileName().toString()));
         } finally {
            BTRFS.readLock().unlock();
         }
      }
      return false;
   }
   public static int getReclaimBalance() {
      if (OneBackup.getBackupId() instanceof String id && !id.isBlank()) {
         try {
            String reclaimpos=SYS_RECLAIM_1 + id + SYS_RECLAIM_2;
            Mount bm=Pc.getBackupMount();
            Pc pc=bm.pc();
            String testCmd=pc.getCmd(new StringBuilder("cat ").append(reclaimpos), true);
            Log.log(testCmd, LEVEL.BTRFS);
            try (CmdStreams testStream=CmdStreams.getDirectStream(testCmd)) {
               Optional<String> b=testStream.outBGerr().peek(line -> Log.log(line, LEVEL.ALLES)).findAny();
               if (b.isPresent())
                  return Integer.parseInt(b.get());
            } catch (IOException e1) {
               e1.printStackTrace();
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      return 0;
   }
   public static void setReclaimBalance(int reclaim) {
      if (OneBackup.getBackupId() instanceof String id && !id.isBlank()) {
         try {
            String reclaimpos=SYS_RECLAIM_1 + id + SYS_RECLAIM_2;
            Mount bm=Pc.getBackupMount();
            Pc pc=bm.pc();
            String testCmd=pc.getCmd(new StringBuilder("echo ").append(reclaim).append(" > ").append(reclaimpos), true);
            Log.log(testCmd, LEVEL.BTRFS);
            try (CmdStreams testStream=CmdStreams.getDirectStream(testCmd)) {
               Optional<String> b=testStream.outBGerr().peek(line -> Log.log(line, LEVEL.ALLES)).findAny();
               if (b.isPresent())
                  return;
            } catch (IOException e1) {
               e1.printStackTrace();
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }
   public static boolean isBalanceRunning() {
      // if (p instanceof Path backsnap && backsnap.equals(Pc.TMP_BACKSNAP)) {
      try {
         Mount bm=Pc.getBackupMount();
         Pc pc=bm.pc();
         String testCmd=pc.getCmd(new StringBuilder(BALANCE_STATUS).append(Pc.TMP_BACKUP_ROOT), true);
         Log.log(testCmd, LEVEL.BTRFS);
         BTRFS.readLock().lock();
         try (CmdStreams testStream=CmdStreams.getDirectStream(testCmd)) {
            return testStream.outBGerr().peek(line -> Log.log(line, LEVEL.ALLES))
                     .anyMatch(line -> line.contains("is running"));
         } catch (IOException e1) {
            e1.printStackTrace();
         } finally {
            BTRFS.readLock().unlock();
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      return false;
   }
   public static boolean startBalance(int percent, int max) {
      if ((percent > 90) | (percent < 10) | isBalanceRunning())
         return true;
      try {
         String usage="usage=" + Integer.toString(percent);
         String limit=(max < 1) ? "" : ",limit=" + Integer.toString(max);
         Pc pc=Pc.getBackupMount().pc();
         String testCmd=pc.getCmd(new StringBuilder(BALANCE_START)/* .append("-m").append(usage) */ //
                  .append("-d").append(usage).append(limit).append(" ").append(Pc.TMP_BACKUP_ROOT)/* .append(" &") */,
                  true);// Background it ??
         Log.log(testCmd, LEVEL.BTRFS);
         Thread.ofPlatform().start(() -> {// Background it ??
            try (CmdStreams testStream=CmdStreams.getDirectStream(testCmd)) {
               @SuppressWarnings("unused")
               boolean q=testStream.outBGerr().peek(line -> Log.log(line, LEVEL.ALLES))
                        .anyMatch(line -> line.contains("chunks"));
            } catch (IOException e1) {
               e1.printStackTrace();
            }
         });
         return true;
      } catch (IOException e) {
         e.printStackTrace();
      }
      return false;
   }
   /**
    * 
    * @param oneBackup
    * @param s
    * @param parent
    * @param bDir
    * @param bsGui
    * @param dryrun
    * @param compressed
    * @return
    * @throws IOException
    */
   static public boolean send_pv_receive(OneBackup oneBackup, Snapshot s, Snapshot parent, Path bDir, //
            BacksnapGui bsGui, boolean dryrun, boolean compressed) throws IOException {
      if (bsGui instanceof BacksnapGui gui)
         gui.setBackupInfo(s, parent);
      StringBuilder btrfsSendSB=new StringBuilder(SEND);
      if (compressed && oneBackup.compressionPossible())
         btrfsSendSB.append("--compressed-data ");
      if (parent instanceof Snapshot p)
         btrfsSendSB.append("-p ").append(p.getSnapshotMountPath()).append(" ");
      if (s.btrfsPath().toString().contains("timeshift-btrfs"))
         Snapshot.setReadonly(parent, s, true);
      btrfsSendSB.append(s.getSnapshotMountPath());
      if (!oneBackup.isSameSsh())
         if (oneBackup.isExtern())
            btrfsSendSB.insert(0, "ssh " + oneBackup.extern() + " '").append("'");
         else
            btrfsSendSB.insert(0, oneBackup.extern());
      if (usePv())
         btrfsSendSB.append("|pv -pteabfW -i 0.2");
      btrfsSendSB.append("|");
      if (!oneBackup.isSameSsh())
         if (OneBackup.isBackupExtern())
            btrfsSendSB.append("ssh " + OneBackup.backupPc.extern() + " '");
         else
            btrfsSendSB.append(OneBackup.backupPc.extern());
      btrfsSendSB.append(Btrfs.RECEIVE).append(bDir);
      if (oneBackup.isSameSsh())
         if (oneBackup.isExtern())
            btrfsSendSB.insert(0, "ssh " + oneBackup.extern() + " '");
      if (OneBackup.isBackupExtern())
         btrfsSendSB.append("'");
      Log.logln(btrfsSendSB.toString(), LEVEL.BTRFS);
      if (!dryrun) {
         if (bsGui != null)
            bsGui.getPanelMaintenance().updateButtons();
         std_min_=" 0:00:";
         BTRFS.writeLock().lock();
         try (CmdStreams btrfsSendStream=CmdStreams.getDirectStream(btrfsSendSB.toString())) {
            Thread.ofVirtual().name(SEND).start(() -> btrfsSendStream.outLines().forEach(line -> extractOuput(line)));
            btrfsSendStream.errLines().forEach(line -> extractPv(bsGui, line));
            // try (DirectCmdStreams btrfsSendStream=new DirectCmdStreams(btrfsSendSB.toString());
            // BufferedCmdReader err=btrfsSendStream.err();
            // BufferedCmdReader out=btrfsSendStream.out()) {
            // Thread.ofPlatform().name(SEND).start(() -> out.lines().forEach(line -> extractOuput(line)));
            // Thread.ofPlatform().name(SEND).start(() -> err.lines().forEach(line -> extractPv(bsGui, line)));
            // // err.lines().forEach(line -> extractPv(bsGui, line));// Ausgabe von pv kommt in err an ;-)
            // } catch (InterruptedException | ExecutionException e) {
            // e.printStackTrace();
         } finally {
            BTRFS.writeLock().unlock();
            Runtime r=Runtime.getRuntime();
            long n=1024 * 1024;
            System.err.println("free(" + r.freeMemory() / n + "),max(" + r.maxMemory() / n + "),total("
                     + r.totalMemory() / n + ")");
            System.gc();
            lnlog("", LEVEL.PROGRESS);
         }
         if (bsGui != null)
            bsGui.getPanelMaintenance().updateButtons();
      }
      if (s.btrfsPath().toString().contains("timeshift-btrfs"))
         Snapshot.setReadonly(parent, s, false);
      return true;
   }
   /**
    * Teste ob pv da ist
    * 
    * @return
    */
   private static boolean usePv() {
      if (pvUsable == null)
         try {
            pvUsable=false;
            pvUsable=Paths.get("/bin/pv").toFile().canExecute();
         } catch (Exception ignore) {/* */}
      return pvUsable;
   }
   private static void extractOuput(String s) {
      try {
         if (lastLine != 0) {
            lastLine=0;
            lnlog("d: ", LEVEL.PROGRESS);
         }
         if (s.equals("At snapshot snapshot"))
            lnlog("", LEVEL.PROGRESS);
         else
            lnlog("e:" + s, LEVEL.PROGRESS); // TODO
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   private static void extractPv(final BacksnapGui bsGui, String line) {
      try { // lnlog(line, LEVEL.PROGRESS);
         if (line.contains("ERROR: cannot find parent subvolume"))
            Backsnap.cantFindParent=line;
         if (line.contains("No route to host") || line.contains("Connection closed")
                  || line.contains("connection unexpectedly closed"))
            Backsnap.disconnectCount=10;
         if (line.contains("At ")) {
            lnlog(line, LEVEL.PROGRESS);
         }
         if (line.contains("<=>")) { // from pv // log(line, LEVEL.PROGRESS);
            Matcher m1=STD_MIN_.matcher(line);
            if (m1.find()) {
               Owlog(line, LEVEL.PROGRESS);
               if (!m1.group().startsWith(std_min_)) {// Eine Minute abgelaufen
                  std_min_=m1.group();
                  lnlog("", LEVEL.PROGRESS);
               }
            } else {
               lnlog("q:", LEVEL.PROGRESS);
               lnlog("?: " + line, LEVEL.PROGRESS);
            }
         } else {
            lnlog(line, LEVEL.PROGRESS);
         }
         show(line, bsGui);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   /**
    * @param line
    */
   private static final void show(String line, BacksnapGui bsGui) {
      if (bsGui == null)
         return;
      line.replaceAll("[\n\r]?", " "); // if (line.equals("\n") || line.equals("\r")) return;
      if (!line.isBlank())
         bsGui.lblPvSetText(line);
   }
}
