/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;
import de.uhingen.kielkopf.andreas.backsnap.gui.part.SnapshotLabel.STATUS;

/**
 * @author Andreas Kielkopf
 *
 */
public class Btrfs {
   /* Liefert eine Map der verfügbaren Volumes sortiert nach UUID */
   public static final String        DEVICE_USAGE    ="btrfs device usage ";
   public static final String        FILESYSTEM_SHOW ="btrfs filesystem show ";
   public static final String        FILESYSTEM_USAGE="btrfs filesystem usage ";
   public static final String        SUBVOLUME_LIST_1="btrfs subvolume list -apuqRs ";
   public static final String        SUBVOLUME_LIST_2="btrfs subvolume list -apuqRcg ";
   public static final String        SUBVOLUME_SHOW  ="btrfs subvolume show ";
   public static final String        PROPERTY_SET    ="btrfs property set ";
   public static final String        PROPERTY_GET    ="btrfs property get ";
   public static final String        VERSION         ="btrfs version ";
   public static final String        SEND            ="btrfs send ";
   public static final String        RECEIVE         ="btrfs receive ";
   public static final String        SUBVOLUME_DELETE="btrfs subvolume delete -Cv ";
   public static final String        SUBVOLUME_CREATE="btrfs subvolume create ";
   public static final String        SUBVOLUME_LIST  ="btrfs subvolume list ";
   public static final ReentrantLock LOCK            =new ReentrantLock();
   /**
    * löscht eines der Backups im Auftrag der GUI
    * 
    * @param s
    * @throws IOException
    */
   static public void removeSnapshot(Snapshot s) throws IOException {
      Path bmp=Pc.getBackupMount().mountPath(); // s.getBackupMountPath();
      if (!bmp.toString().startsWith(Pc.TMP_BACKUP_ROOT.toString()) || bmp.toString().contains("../"))
         throw new SecurityException("I am not allowed to delete " + bmp.toString());
      StringBuilder removeSB=new StringBuilder(SUBVOLUME_DELETE).append(bmp);
      String removeCmd=s.mount().pc().getCmd(removeSB);
      if (Backsnap.bsGui != null)
         Backsnap.bsGui.setDeleteInfo(s);
      Backsnap.log(4, removeCmd);
      if (Backsnap.bsGui != null) {
         Backsnap.bsGui.getPanelMaintenance().updateButtons();
         String text="<html>" + s.btrfsPath().toString();
         Backsnap.logln(7, text);
         Backsnap.bsGui.mark(s.received_uuid(), STATUS.INPROGRESS);
      }
      LOCK.lock();
      try (CmdStream removeStream=Commandline.executeCached(removeCmd, null)) {
         removeStream.backgroundErr();
         Backsnap.logln(1, "");
         removeStream.erg().forEach(line -> {
            Backsnap.log(1, line);
            if (Backsnap.GUI.get())
               Backsnap.bsGui.lblPvSetText(line);
         });
         removeStream.waitFor();
      } finally {
         LOCK.unlock();
         if (Backsnap.bsGui != null)
            Backsnap.bsGui.getPanelMaintenance().updateButtons();
      }
   }
   public static ConcurrentSkipListMap<String, Volume> show(Pc pc, boolean onlyMounted, boolean refresh) {
      ConcurrentSkipListMap<String, Volume> list=new ConcurrentSkipListMap<>();
      String volumeListCmd=pc.getCmd(new StringBuilder(FILESYSTEM_SHOW).append(onlyMounted ? " -m" : " -d"));
      Backsnap.logln(7, volumeListCmd);
      if (refresh)
         Commandline.removeFromCache(volumeListCmd);
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
      }
      return list;
   }
   public static void createSubvolume(Pc pc, Path p) throws IOException {
      if (p instanceof Path backsnap && backsnap.equals(Pc.TMP_BACKSNAP)) {
         String createCmd=pc.getCmd(new StringBuilder(SUBVOLUME_CREATE).append(backsnap));
         Backsnap.log(4, createCmd);
         LOCK.lock();
         try (CmdStream createStream=Commandline.executeCached(createCmd, null)) {
            createStream.backgroundErr();
            createStream.erg().forEach(line -> {
               Backsnap.log(4, line);
            });
            createStream.waitFor();
         } finally {
            LOCK.unlock();
         }
      }
   }
   public static boolean testSubvolume(Pc pc, Path p) throws IOException {
      if (p instanceof Path backsnap && backsnap.equals(Pc.TMP_BACKSNAP)) {
         String testCmd=pc.getCmd(new StringBuilder(SUBVOLUME_LIST).append(backsnap));
         Backsnap.log(4, testCmd);
         LOCK.lock();
         try (CmdStream testStream=Commandline.executeCached(testCmd, null)) {
            testStream.backgroundErr();
            long c=testStream.erg().peek(line -> {
               Backsnap.log(9, line);
            }).filter(line -> line.endsWith(Pc.TMP_BACKSNAP.getFileName().toString())).count();
            testStream.waitFor();
            return c != 0;
         } finally {
            LOCK.unlock();
         }
      }
      return false;
   }
}
