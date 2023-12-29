/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.backsnap.config.Log.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.config.Log;
import de.uhingen.kielkopf.andreas.backsnap.config.Log.LEVEL;
import de.uhingen.kielkopf.andreas.backsnap.gui.BacksnapGui;
import de.uhingen.kielkopf.andreas.backsnap.gui.part.SnapshotLabel.STATUS;
import de.uhingen.kielkopf.andreas.beans.shell.CmdStreams;

/**
 * @author Andreas Kielkopf
 *
 */
public class Btrfs {
   /* Liefert eine Map der verfügbaren Volumes sortiert nach UUID */
   public static final String                 DEVICE_USAGE    ="btrfs device usage ";
   private static final String                FILESYSTEM_SHOW ="btrfs filesystem show ";
   public static final String                 FILESYSTEM_USAGE="btrfs filesystem usage ";
   public static final String                 SUBVOLUME_LIST_1="btrfs subvolume list -apuqRs ";
   public static final String                 SUBVOLUME_LIST_2="btrfs subvolume list -apuqRcg ";
   public static final String                 SUBVOLUME_SHOW  ="btrfs subvolume show ";
   public static final String                 PROPERTY_SET    ="btrfs property set ";
   public static final String                 PROPERTY_GET    ="btrfs property get ";
   public static final String                 VERSION         ="btrfs version ";
   private static final String                SEND            ="btrfs send ";
   public static final String                 RECEIVE         ="btrfs receive ";
   private static final String                SUBVOLUME_DELETE="btrfs subvolume delete -v ";
   private static final String                SUBVOLUME_CREATE="btrfs subvolume create ";
   public static final String                 SUBVOLUME_LIST  ="btrfs subvolume list ";
   private static final Pattern               STD_MIN_        =Pattern.compile(" [0-9]:[0-9][0-9]:");
   private static String                      std_min_;                                              // =" 0:00:";
   public static final ReentrantReadWriteLock BTRFS           =new ReentrantReadWriteLock(true);
   private static Boolean                     pvUsable        =null;
   private static int                         lastLine        =0;
   // private static boolean skip =false;
   /**
    * löscht eines der Backups im Auftrag der GUI
    * 
    * @param s
    * @throws IOException
    */
   static public void removeSnapshot(Snapshot s) throws IOException {
      Path bmp=Pc.getBackupMount().mountPath();
      Path rel=s.btrfsPath().getRoot().relativize(s.btrfsPath());
      bmp=bmp.resolve(rel);
      if (!bmp.toString().startsWith(Pc.TMP_BACKUP_ROOT.toString()) || bmp.toString().contains("../"))
         throw new SecurityException("I am not allowed to delete " + bmp.toString());
      StringBuilder removeSB=new StringBuilder(SUBVOLUME_DELETE).append(bmp);
      String removeCmd=s.mount().pc().getCmd(removeSB, true);
      Log.log(removeCmd, LEVEL.BTRFS);
      if (Backsnap.bsGui instanceof BacksnapGui gui) {
         gui.setDeleteInfo(s);
         gui.getPanelMaintenance().updateButtons();
         gui.mark(s.received_uuid(), STATUS.INPROGRESS);
      }
      BTRFS.writeLock().lock();
      try (CmdStreams removeStream=CmdStreams.getDirectStream(removeCmd)) {
         removeStream.outBGerr().forEach(line -> {
//            if (!line.isEmpty()) {
               Log.logln(line, LEVEL.DELETE);
               if (Backsnap.bsGui instanceof BacksnapGui gui)
                  gui.lblPvSetText(line);
//            }
         });
         removeStream.errPrintln();
      } finally {
         BTRFS.writeLock().unlock();
      }
      if (Backsnap.bsGui instanceof BacksnapGui gui)
         gui.getPanelMaintenance().updateButtons();
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
//            if (!line.isEmpty()) {
               if (!line.isBlank())
                  tmpList.add(line);
               else
                  if (!tmpList.isEmpty()) {
                     Volume v=Volume.getVolume(pc, tmpList);
                     list.put(v.uuid(), v);
                     tmpList.clear();
                  }
//            }
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
         } finally {
            BTRFS.writeLock().unlock();
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
            logln("", LEVEL.PROGRESS);
         }
         logln(s, LEVEL.PROGRESS); // TODO
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
         if (line.contains("<=>")) { // from pv // log(line, LEVEL.PROGRESS);
            Matcher m1=STD_MIN_.matcher(line);
            if (m1.find()) {
               if (m1.group().startsWith(std_min_)) {
                  Owlog("o: " + line, LEVEL.PROGRESS);
               } else {
                  std_min_=m1.group();
                  lnlog("", LEVEL.PROGRESS);
                  Owlog(">: " + line, LEVEL.PROGRESS);// Eine Minute abgelaufen
               }
            } else {
               logln("?: " + line, LEVEL.PROGRESS);
            }
            // if (lastLine == 0)
            // logln("l: " + line, LEVEL.PROGRESS);
            // else {
            // if (line.contains(":00 ")) {
            // if (skip) {
            // skip=false; // logln("m: " + line, LEVEL.PROGRESS);
            // Backsnap.disconnectCount=0;
            // } else {
            // Owlog(">: " + line, LEVEL.PROGRESS);
            // }
            // } else {
            // skip=true;
            // Owlog("o: " + line, LEVEL.PROGRESS);
            // }
            // }
            // show(line, bsGui);
            // lastLine++;
         } else {
            // if (lastLine != 0) {
            // lastLine=0;
            // logln(" ", LEVEL.PROGRESS);
            // }
            logln(" v ", LEVEL.PROGRESS);
            logln("x: " + line, LEVEL.PROGRESS);
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
