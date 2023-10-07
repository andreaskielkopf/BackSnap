/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.backsnap.config.Log.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;
import de.uhingen.kielkopf.andreas.backsnap.config.Log;
import de.uhingen.kielkopf.andreas.backsnap.config.Log.LEVEL;
import de.uhingen.kielkopf.andreas.backsnap.gui.BacksnapGui;
import de.uhingen.kielkopf.andreas.backsnap.gui.part.SnapshotLabel.STATUS;
import de.uhingen.kielkopf.andreas.beans.shell.DirectCmdStream;

/**
 * @author Andreas Kielkopf
 *
 */
public class Btrfs {
   /* Liefert eine Map der verfügbaren Volumes sortiert nach UUID */
   public static final String                 DEVICE_USAGE    ="btrfs device usage ";
   public static final String                 FILESYSTEM_SHOW ="btrfs filesystem show ";
   public static final String                 FILESYSTEM_USAGE="btrfs filesystem usage ";
   public static final String                 SUBVOLUME_LIST_1="btrfs subvolume list -apuqRs ";
   public static final String                 SUBVOLUME_LIST_2="btrfs subvolume list -apuqRcg ";
   public static final String                 SUBVOLUME_SHOW  ="btrfs subvolume show ";
   public static final String                 PROPERTY_SET    ="btrfs property set ";
   public static final String                 PROPERTY_GET    ="btrfs property get ";
   public static final String                 VERSION         ="btrfs version ";
   public static final String                 SEND            ="btrfs send ";
   public static final String                 RECEIVE         ="btrfs receive ";
   public static final String                 SUBVOLUME_DELETE="btrfs subvolume delete -Cv ";
   public static final String                 SUBVOLUME_CREATE="btrfs subvolume create ";
   public static final String                 SUBVOLUME_LIST  ="btrfs subvolume list ";
   public static final ReentrantReadWriteLock BTRFS           =new ReentrantReadWriteLock(true);
   private static Boolean                     pvUsable        =null;
   static int                                 lastLine        =0;
   /**
    * löscht eines der Backups im Auftrag der GUI
    * 
    * @param s
    * @throws IOException
    */
   static public void removeSnapshot(Snapshot s) throws IOException {
      Path bmp=Pc.getBackupMount().mountPath(); // s.getBackupMountPath();
      Path rel=s.btrfsPath().getRoot().relativize(s.btrfsPath());
      bmp=bmp.resolve(rel);
      if (!bmp.toString().startsWith(Pc.TMP_BACKUP_ROOT.toString()) || bmp.toString().contains("../"))
         throw new SecurityException("I am not allowed to delete " + bmp.toString());
      StringBuilder removeSB=new StringBuilder(SUBVOLUME_DELETE).append(bmp);
      String removeCmd=s.mount().pc().getCmd(removeSB, true);
      if (Backsnap.bsGui != null)
         Backsnap.bsGui.setDeleteInfo(s);
      Log.log(removeCmd, LEVEL.BTRFS);
      if (Backsnap.bsGui != null) {
         Backsnap.bsGui.getPanelMaintenance().updateButtons();
         String text="<html>" + s.btrfsPath().toString();
         Log.logln(text, LEVEL.BTRFS);
         Backsnap.bsGui.mark(s.received_uuid(), STATUS.INPROGRESS);
      }
      BTRFS.writeLock().lock();
      try (CmdStream removeStream=Commandline.executeCached(removeCmd, null)) {
         removeStream.backgroundErr();
         Log.logln("", LEVEL.DELETE);
         removeStream.erg().forEach(line -> {
            Log.log(line, LEVEL.DELETE);
            if (Backsnap.GUI.get())
               Backsnap.bsGui.lblPvSetText(line);
         });
         removeStream.waitFor();
      } finally {
         BTRFS.writeLock().unlock();
      }
      if (Backsnap.bsGui != null)
         Backsnap.bsGui.getPanelMaintenance().updateButtons();
   }
   public static ConcurrentSkipListMap<String, Volume> show(Pc pc, boolean onlyMounted, boolean refresh) {
      ConcurrentSkipListMap<String, Volume> list=new ConcurrentSkipListMap<>();
      String volumeListCmd=pc.getCmd(new StringBuilder(FILESYSTEM_SHOW).append(onlyMounted ? " -m" : " -d"), true);
      Log.logln(volumeListCmd, LEVEL.BTRFS);
      if (refresh)
         Commandline.removeFromCache(volumeListCmd);
      BTRFS.readLock().lock();
      try (CmdStream volumeListStream=Commandline.executeCached(volumeListCmd)) {
         volumeListStream.backgroundErr();
         List<String> lines=volumeListStream.erg().toList();
         volumeListStream.waitFor();
         ArrayList<String> tmpList=new ArrayList<>();
         for (String line:lines)
            if (!line.isBlank())
               tmpList.add(line);
            else
               if (!tmpList.isEmpty()) {
                  Volume v=Volume.getVolume(pc, tmpList);
                  String uuid=v.uuid();
                  list.put(uuid, v);
                  tmpList.clear();
               }
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
         try (CmdStream createStream=Commandline.executeCached(createCmd, null)) {
            createStream.backgroundErr();
            createStream.erg().forEach(line -> {
               Log.log(line, LEVEL.BTRFS);
            });
            createStream.waitFor();
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
         try (CmdStream testStream=Commandline.executeCached(testCmd, null)) {
            testStream.backgroundErr();
            long c=testStream.erg().peek(line -> {
               Log.log(line, LEVEL.ALLES);
            }).filter(line -> line.endsWith(Pc.TMP_BACKSNAP.getFileName().toString())).count();
            testStream.waitFor();
            return c != 0;
         } finally {
            BTRFS.readLock().unlock();
         }
      }
      return false;
   }
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
         btrfsSendSB.append("|pv -f");
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
         BTRFS.writeLock().lock();
         try (DirectCmdStream btrfsSendStream=DirectCmdStream.getCmdStream(btrfsSendSB)) {
            btrfsSendStream.errBgOut().forEach(line -> extractPv(bsGui, line));
            btrfsSendStream.out().forEach(line -> extractOuput());
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
   static private boolean usePv() {
      if (pvUsable == null)
         try {
            pvUsable=false;
            pvUsable=Paths.get("/bin/pv").toFile().canExecute();
         } catch (Exception ignore) {/* */}
      return pvUsable;
   }
   private static void extractOuput() {
      try {
         if (lastLine != 0) {
            lastLine=0;
            logln("", LEVEL.PROGRESS);
         }
         logln("", LEVEL.PROGRESS);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   private static void extractPv(BacksnapGui bsGui, String line) {
      try {
         lnlog(line, LEVEL.PROGRESS);
         if (line.contains("ERROR: cannot find parent subvolume"))
            Backsnap.cantFindParent=line;
         if (line.contains("No route to host") || line.contains("Connection closed")
                  || line.contains("connection unexpectedly closed"))
            Backsnap.disconnectCount=10;
         if (line.contains("<=>")) { // from pv
            log(line, LEVEL.PROGRESS);
            if (lastLine == 0)
               lnlog("", LEVEL.PROGRESS);
            else
               Owlog("", LEVEL.PROGRESS);
            show(line, bsGui);
            lastLine++;
            if (line.contains(":00 ")) {
               logln("", LEVEL.PROGRESS);
               Backsnap.disconnectCount=0;
            }
            if (line.contains("0,00 B/s")) {
               lnlog("HipCup", LEVEL.PROGRESS);
               logln("", LEVEL.PROGRESS);
               Backsnap.disconnectCount++;
            }
         } else {
            if (lastLine != 0) {
               lastLine=0;
               logln("", LEVEL.PROGRESS);
            }
            logln(line, LEVEL.PROGRESS);
            show(line, bsGui);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   /**
    * @param line
    */
   static private final void show(String line, BacksnapGui bsGui) {
      if (bsGui == null)
         return;
      line.replaceAll("[\n\r]?", " "); // if (line.equals("\n") || line.equals("\r")) return;
      if (!line.isBlank())
         bsGui.lblPvSetText(line);
   }
}
