package de.uhingen.kielkopf.andreas.backsnap;

import static java.lang.System.err;

import java.awt.Frame;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import javax.swing.*;

import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;
import de.uhingen.kielkopf.andreas.backsnap.btrfs.*;
import de.uhingen.kielkopf.andreas.backsnap.gui.BacksnapGui;

import de.uhingen.kielkopf.andreas.beans.cli.Flag;

/**
 * License: 'GNU General Public License v3.0'
 * 
 * © 2023
 * 
 * @author Andreas Kielkopf
 * @see https://github.com/andreaskielkopf/BackSnap
 * @see https://forum.manjaro.org/t/howto-hilfsprogramm-fur-backup-btrfs-snapshots-mit-send-recieve timeshift
 * 
 */
public class Backsnap {
   static String                     parentKey          =null;
   private static Snapshot           parentSnapshot     =null;
   private static boolean            usePv              =false;
   static int                        lastLine           =0;
   static String                     canNotFindParent   =null;
   static int                        connectionLost     =0;
   static Future<?>                  task               =null;
   private static BacksnapGui        bsGui              =null;
   private static String             refreshGUIcKey     =null;
   private static Mount              refreshBackupVolume=null;
   private static String             refreshBackupDir   =null;
   private static int                textVorhanden      =0;
   private static String             srcSsh             =null;
   private static Path               srcDir             =null;
   private static Pc                 srcPc              =null;
   public static String              TMP_BTRFS_ROOT     ="/tmp/BtrfsRoot";
   final static Flag                 HELP               =new Flag('h', "help");           // show usage
   final static Flag                 VERSION            =new Flag('x', "version");        // show date and version
   final static Flag                 DRYRUN             =new Flag('d', "dryrun");         // do not do anythimg ;-)
   final static Flag                 GUI                =new Flag('g', "gui");            // enable gui (works only with
                                                                                          // sudo)
   final static Flag                 AUTO               =new Flag('a', "auto");           // auto-close gui when ready
   final public static Flag          VERBOSE            =new Flag('v', "verbose");
   final public static Flag          TIMESHIFT          =new Flag('t', "timeshift");
   final public static String        SNAPSHOT           ="snapshot";
   final public static String        DOT_SNAPSHOTS      =".snapshots";
   final public static String        AT_SNAPSHOTS       ="@snapshots";
   public final static Flag          SINGLESNAPSHOT     =new Flag('s', "singlesnapshot"); // backup exactly one snapshot
   public final static Flag          DELETEOLD          =new Flag('o', "deleteold");      // mark old snapshots for
                                                                                          // deletion
   public final static Flag          KEEP_MINIMUM       =new Flag('m', "keepminimum");    // mark all but minimum
                                                                                          // snapshots
   public static final String        BACK_SNAP_VERSION  =                                 // version
            "BackSnap for Snapper and Timeshift(beta) Version 0.6.0.23 (2023/07/01)";
   public static final ReentrantLock BTRFS_LOCK         =new ReentrantLock();
   public static void main(String[] args) {
      Flag.setArgs(args, "sudo:/" + DOT_SNAPSHOTS + " sudo:/mnt/BACKUP/" + AT_SNAPSHOTS + "/manjaro18");
      StringBuilder argLine=new StringBuilder("args > ");
      for (String s:args)
         argLine.append(" ").append(s);
      logln(1, argLine.toString());
      if (VERSION.get()) {
         logln(0, BACK_SNAP_VERSION);
         System.exit(0);
      }
      if (DRYRUN.get())
         logln(0, "Doing a dry run ! ");
      TIMESHIFT.set(true);
      // Parameter sammeln für SOURCE
      String source=Flag.getParameterOrDefault(0, "sudo:/");
      srcSsh=source.contains(":") ? source.substring(0, source.indexOf(":")) : "";
      srcDir=Path.of("/", srcSsh.isBlank() ? source : source.substring(srcSsh.length() + 1));
      if (srcSsh.startsWith("sudo"))
         srcSsh="sudo ";
      if (srcDir.endsWith(DOT_SNAPSHOTS))
         srcDir=srcDir.getParent();
      BTRFS_LOCK.lock();
      try {
         if (bsGui != null)
            SwingUtilities.invokeLater(() -> bsGui.getPanelMaintenance().updateButtons());
         srcPc=new Pc(srcSsh);
         if (TIMESHIFT.get())
            mountBtrfsRoot(srcPc, srcDir, true);
         // Start collecting information
         SubVolumeList    srcSubVolumes=new SubVolumeList(srcPc);
         List<SnapConfig> snapConfigs  =SnapConfig.getList(srcSubVolumes);
         SnapConfig       srcConfig    =SnapConfig.getConfig(snapConfigs, srcDir);
         if (srcConfig == null)
            throw new RuntimeException("Could not find snapshots for srcDir: " + srcDir);
         if (srcConfig.volumeMount().btrfsMap().isEmpty())
            throw new RuntimeException("Ingnoring, because there are no snapshots in: " + srcDir);
         logln(1, "Backup snapshots from " + srcConfig.volumeMount().keyM());
         // BackupVolume ermitteln
         String backup=Flag.getParameterOrDefault(1, "Back@Snap");
         if (backup.equals("Back@Snap")) {
            err.println("2nd parameter missing. Where should i save the backups ?");
            ende("X");
            System.exit(0);
         }
         String backupSsh=backup.contains(":") ? backup.substring(0, backup.indexOf(":")) : "";
         String backupDir=backupSsh.isBlank() ? backup : backup.substring(backupSsh.length() + 1);
         if (backupSsh.startsWith("sudo"))
            backupSsh="sudo ";
         boolean samePC  =(backupSsh.equals(srcSsh));
         Pc      backupPc=samePC ? srcPc : new Pc(backupSsh);
         backupPc.updateMounts();
         SubVolumeList backupSubVolumes=samePC ? srcSubVolumes : new SubVolumeList(backupPc);
         String        backupKey       =backupSsh + ":" + backupDir;
         Mount         backupVolume    =backupSubVolumes.getBackupVolume(backupKey);
         if (backupVolume == null)
            throw new RuntimeException("Could not find backupDir: " + backupDir);
         if (backupVolume.devicePath().equals(srcConfig.volumeMount().devicePath()) && samePC)
            throw new RuntimeException("Backup not possible onto same device: " + backupDir + " <= " + srcDir);
         logln(2, "Try to use backupDir  " + backupVolume.keyM());
         SnapTree backupTree=SnapTree.getSnapTree(backupVolume/* , backupVolume.mountPoint(), backupSsh */);
         if (GUI.get()) {
            bsGui=new BacksnapGui();
            BacksnapGui.setGui(bsGui);
            BacksnapGui.main2(args);
            bsGui.setArgs(argLine.substring(7));
            bsGui.setSrc(srcConfig);
            bsGui.setBackup(backupTree, backupDir);
            bsGui.getSplitPaneSnapshots().setDividerLocation(1d / 3d);
         }
         try {
            usePv=Paths.get("/bin/pv").toFile().canExecute();
         } catch (Exception e1) {/* */}
         /// Alle Snapshots einzeln sichern
         if (connectionLost > 0) {
            err.println("no SSH Connection");
            ende("X");
            System.exit(0);
         }
         int counter=0;
         if (bsGui != null)
            bsGui.getProgressBar().setMaximum(srcConfig.volumeMount().otimeKeyMap().size());
         for (Snapshot sourceSnapshot:srcConfig.volumeMount().otimeKeyMap().values()) {
            counter++;
            if (canNotFindParent != null) {
               err.println("Please remove " + backupDir + "/" + canNotFindParent + "/" + SNAPSHOT + " !");
               ende("X");
               System.exit(-9);
            } else
               if (connectionLost > 3) {
                  err.println("SSH Connection lost !");
                  ende("X");
                  System.exit(-8);
               }
            try {
               if ((bsGui != null) && (bsGui.getProgressBar() instanceof JProgressBar progressbar)) {
                  progressbar.setValue(counter);
                  progressbar.setString(Integer.toString(counter) + "/"
                           + Integer.toString(srcConfig.volumeMount().otimeKeyMap().size()));
                  progressbar.repaint(50);
               }
               if (!backup(sourceSnapshot, srcConfig.snapshotMount(), backupTree, backupDir, srcSsh, backupSsh,
                        snapConfigs))
                  continue;
               // Anzeige im Progressbar anpassen
               if (bsGui != null)
                  refreshGUI(backupVolume, backupDir, backupSsh);
               if (SINGLESNAPSHOT.get())// nur einen Snapshot übertragen und dann abbrechen
                  break;
            } catch (NullPointerException n) {
               n.printStackTrace();
               break;
            }
         }
      } catch (IOException e) {
         if ((e.getMessage().startsWith("ssh: connect to host"))
                  || (e.getMessage().startsWith("Could not find snapshot:")))
            System.err.println(e.getMessage());
         else
            e.printStackTrace();
         ende("Xabbruch");
         System.exit(-1);
      } finally {
         BTRFS_LOCK.unlock();
         if (bsGui != null)
            SwingUtilities.invokeLater(() -> bsGui.getPanelMaintenance().updateButtons());
      }
      ende("X");
      System.exit(-2);
   }
   /**
    * @param string
    * @param string2
    * @throws IOException
    */
   private static void mountBtrfsRoot(Pc srcPc1, Path srcDir1, boolean doMount) throws IOException {
      srcPc1.updateMounts();
      Collection<Mount> ml=srcPc1.mounts().values();
      if (doMount == ml.stream().anyMatch(m -> m.mountPath().toString().equals(TMP_BTRFS_ROOT)))
         return; // mount hat schon den gewünschten status
      Optional<Mount> m3=ml.stream().filter(m -> m.mountPath().toString().equals(srcDir1.toString())).findAny();
      if (m3.isEmpty())
         throw new RuntimeException("Not able to find the right device for: " + srcPc1 + ":" + srcDir1.toString());
      StringBuilder mountSB=new StringBuilder();
      if (doMount) {
         mountSB.append("mkdir --mode=000 -p ").append(TMP_BTRFS_ROOT).append(";");
         mountSB.append("mount -t btrfs -o subvol=/ ").append(m3.get().devicePath()).append(" ").append(TMP_BTRFS_ROOT);
      } else {
         mountSB.append("umount ").append(TMP_BTRFS_ROOT).append(";");
         mountSB.append("rmdir ").append(TMP_BTRFS_ROOT);
      }
      String mountCmd=srcPc1.getCmd(mountSB);
      logln(4, mountCmd);// if (!DRYRUN.get())
      try (CmdStream mountStream=Commandline.executeCached(mountCmd, null)) { // not cached
         mountStream.backgroundErr();
         mountStream.erg().forEach(t -> logln(4, t));
         for (String line:mountStream.errList())
            if (line.contains("No route to host") || line.contains("Connection closed")
                     || line.contains("connection unexpectedly closed")) {
               Backsnap.connectionLost=10;
               break;
            } // ende("");// R
      }
      srcPc1.updateMounts();
   }
   /**
    * @param backupVolume
    * @param backupDir
    * @param backupSsh
    * @throws IOException
    * 
    */
   private static void refreshGUI(Mount backupVolume, String backupDir, String backupSsh) throws IOException {
      String extern    =backupVolume.pc().extern();
      Path   devicePath=backupVolume.devicePath();
      String cacheKey  =extern + ":" + devicePath;
      Commandline.removeFromCache(cacheKey);
      SnapTree backupTree=new SnapTree(backupVolume);// umgeht den cache
      bsGui.setBackup(backupTree, backupDir);
      refreshGUIcKey=cacheKey;
      refreshBackupVolume=backupVolume;
      refreshBackupDir=backupDir;
   }
   public static void refreshGUI() throws IOException {
      if (refreshGUIcKey == null)
         return;
      Commandline.removeFromCache(refreshGUIcKey);
      SnapTree backupTree=new SnapTree(refreshBackupVolume);// umgeht den cache
      bsGui.setBackup(backupTree, refreshBackupDir);
   }
   /**
    * Versuchen genau diesen einzelnen Snapshot zu sichern
    * 
    * @param snapConfigs
    * 
    * @param sourceKey
    * @param sMap
    * @param dMap
    * @throws IOException
    */
   private static boolean backup(Snapshot srcSnapshot, Mount srcVolume, SnapTree backupMap, String backupDir,
            String srcSsh1, String backupSsh, List<SnapConfig> snapConfigs) throws IOException {
      if (bsGui != null) {
         String text="<html>" + srcSnapshot.getSnapshotMountPath().toString();
         logln(7, text);
         bsGui.getLblSnapshot().setText("backup of : ");
         bsGui.getTxtSnapshot().setText(srcSnapshot.dirName());
         bsGui.getLblParent().setText((parentSnapshot == null) ? " " : "based on:");
         bsGui.getTxtParent().setText((parentSnapshot == null) ? " " : parentSnapshot.dirName());
         bsGui.getPanelWork().repaint(50);
      }
      if (srcSnapshot.isBackup()) {
         err.println("Überspringe backup vom backup: " + srcSnapshot.dirName());
         return false;
      }
      if (backupMap.rUuidMap().containsKey(srcSnapshot.uuid())) {
         if (textVorhanden == 0) {
            logln(5, "");
            log(5, "Überspringe bereits vorhandene Snapshots:");
            textVorhanden=42;
         } else
            if (textVorhanden >= 120) {
               logln(5, "");
               textVorhanden=0;
            }
         log(5, " " + srcSnapshot.dirName());
         textVorhanden+=srcSnapshot.dirName().length() + 1;
         parentSnapshot=srcSnapshot;
         return false;
      }
      textVorhanden=0;
      Path sDir=srcSnapshot.getSnapshotMountPath();
      if (sDir == null)
         throw new FileNotFoundException("Could not find dir: " + srcVolume);
      logln(9, "Paths.get(backupDir=" + backupDir + " dirName=" + srcSnapshot.dirName() + ")");
      Path bDir    =Paths.get(backupDir, srcSnapshot.dirName());
      Path relMdir =backupMap.mount().mountPath().relativize(bDir);
      Path bpq     =backupMap.mount().btrfsPath().resolve(relMdir);
      Path bSnapDir=bpq.resolve(SNAPSHOT);
      logln(3, "");
      log(3, bSnapDir.toString());
      if (backupMap.btrfsPathMap().containsKey(bSnapDir)) {
         logln(5, "Der Snapshot scheint schon da zu sein ????");
         return true;
      }
      log(3, "Backup of " + srcSnapshot.dirName());
      if (parentSnapshot != null) // @todo genauer prüfen
         logln(3, " based on " + parentSnapshot.dirName());
      mkDirs(bDir, backupSsh);
      rsyncFiles(srcSsh1, backupSsh, sDir, bDir);
      if (GUI.get())
         bsGui.mark(srcSnapshot);
      if (sendBtrfs(srcVolume, srcSsh1, backupSsh, srcSnapshot, bDir, snapConfigs))
         parentSnapshot=srcSnapshot;
      return true;
   }
   private static boolean sendBtrfs(Mount srcVolume, String srcSsh1, String backupSsh, Snapshot s, Path bDir,
            List<SnapConfig> snapConfigs) throws IOException {
      boolean       sameSsh    =(srcSsh1.contains("@") && srcSsh1.equals(backupSsh));
      StringBuilder btrfsSendSB=new StringBuilder("/bin/btrfs send ");
      if (bsGui != null) {
         bsGui.getLblSnapshot().setText("backup of : ");
         bsGui.getTxtSnapshot().setText(s.dirName());
         bsGui.getLblParent().setText((parentSnapshot == null) ? " " : "based on:");
         bsGui.getTxtParent().setText((parentSnapshot == null) ? " " : parentSnapshot.dirName());
         bsGui.getPanelWork().repaint(50);
      }
      if (parentSnapshot != null) // @todo genauer prüfen
         btrfsSendSB.append("-p ").append(parentSnapshot.getSnapshotMountPath()).append(" ");
      if (s.btrfsPath().toString().contains("timeshift-btrfs"))
         setReadonly(parentSnapshot, s, true);
      logln(2, "");
      btrfsSendSB.append(s.getSnapshotMountPath());
      if (!sameSsh)
         if (srcSsh1.contains("@"))
            btrfsSendSB.insert(0, "ssh " + srcSsh1 + " '").append("'");
         else
            btrfsSendSB.insert(0, srcSsh1);
      if (usePv)
         btrfsSendSB.append("|/bin/pv -f");
      btrfsSendSB.append("|");
      if (!sameSsh)
         if (backupSsh.contains("@"))
            btrfsSendSB.append("ssh " + backupSsh + " '");
         else
            btrfsSendSB.append(backupSsh);
      btrfsSendSB.append("/bin/btrfs receive ").append(bDir).append(";/bin/sync");
      if (sameSsh)
         if (srcSsh1.contains("@"))
            btrfsSendSB.insert(0, "ssh " + srcSsh1 + " '");
      // else
      // send_cmd.insert(0, srcSsh); // @todo für einzelnes sudo anpassen ?
      if (backupSsh.contains("@"))
         btrfsSendSB.append("'");
      logln(2, btrfsSendSB.toString());
      if (!DRYRUN.get()) {
         BTRFS_LOCK.lock();
         try (CmdStream btrfsSendStream=Commandline.executeCached(btrfsSendSB, null)) {
            if (bsGui != null)
               SwingUtilities.invokeLater(() -> bsGui.getPanelMaintenance().updateButtons());
            task=Commandline.background.submit(() -> btrfsSendStream.err().forEach(line -> {
               if (line.contains("ERROR: cannot find parent subvolume"))
                  Backsnap.canNotFindParent=Backsnap.parentKey;
               if (line.contains("No route to host") || line.contains("Connection closed")
                        || line.contains("connection unexpectedly closed"))
                  Backsnap.connectionLost=10;
               if (line.contains("<=>")) { // from pv
                  err.print(line);
                  show(line);
                  String lf=(Backsnap.lastLine == 0) ? "\n" : "\r";
                  err.print(lf);
                  Backsnap.lastLine=line.length();
                  if (line.contains(":00 ")) {
                     err.print("\n");
                     Backsnap.connectionLost=0;
                  }
                  if (line.contains("0,00 B/s")) {
                     err.println();
                     err.println("HipCup");
                     Backsnap.connectionLost++;
                  }
               } else {
                  if (Backsnap.lastLine != 0) {
                     Backsnap.lastLine=0;
                     err.println();
                  }
                  err.println(line);
                  show(line);
               }
            }));
            btrfsSendStream.erg().forEach(line -> {
               if (lastLine != 0) {
                  lastLine=0;
                  logln(3, "");
               }
               logln(3, "");
            });
         } finally {
            BTRFS_LOCK.unlock();
            if (bsGui != null)
               SwingUtilities.invokeLater(() -> bsGui.getPanelMaintenance().updateButtons());
         } // ende("S");// B
      }
      if (s.btrfsPath().toString().contains("timeshift-btrfs"))
         setReadonly(parentSnapshot, s, false);
      return true;
   }
   /**
    * @param parentSnapshot2
    * @param s
    * @param b
    * @throws IOException
    */
   private static void setReadonly(Snapshot parent, Snapshot snapshot, boolean readonly) throws IOException {
      if (!snapshot.btrfsPath().toString().contains("timeshift"))
         return;
      BTRFS_LOCK.lock();
      try {
         if (bsGui != null)
            SwingUtilities.invokeLater(() -> bsGui.getPanelMaintenance().updateButtons());
         StringBuilder readonlySB=new StringBuilder();
         if (parent != null)
            readonlySB.append("btrfs property set ").append(parent.getSnapshotMountPath()).append(" ro ")
                     .append(readonly).append(";");
         readonlySB.append("btrfs property set ").append(snapshot.getSnapshotMountPath()).append(" ro ")
                  .append(readonly);
         String readonlyCmd=snapshot.mount().pc().getCmd(readonlySB);
         logln(4, readonlyCmd);// if (!DRYRUN.get())
         try (CmdStream readonlyStream=Commandline.executeCached(readonlyCmd, null)) { // not cached
            readonlyStream.backgroundErr();
            readonlyStream.erg().forEach(t -> logln(4, t));
            for (String line:readonlyStream.errList())
               if (line.contains("No route to host") || line.contains("Connection closed")
                        || line.contains("connection unexpectedly closed")) {
                  Backsnap.connectionLost=10;
                  break;
               } // ende("");// R
         }
      } finally {
         BTRFS_LOCK.unlock();
         if (bsGui != null)
            SwingUtilities.invokeLater(() -> bsGui.getPanelMaintenance().updateButtons());
      }
   }
   static StringBuilder pv=new StringBuilder("- Info -");
   /**
    * @param line
    */
   private final static void show(String line) {
      if (bsGui == null)
         return;
      if (line.equals("\n") || line.equals("\r"))
         return;
      bsGui.lblPvSetText(line);
   }
   private static void rsyncFiles(String srcSsh1, String backupSsh, Path sDir, Path bDir) throws IOException {
      StringBuilder rsyncSB=new StringBuilder("/bin/rsync -vdcptgo --exclude \"@*\" --exclude \"" + SNAPSHOT + "\" ");
      if (DRYRUN.get())
         rsyncSB.append("--dry-run ");
      if (!srcSsh1.equals(backupSsh))
         if (srcSsh1.contains("@"))
            rsyncSB.append(srcSsh1).append(":");
      rsyncSB.append(sDir.getParent()).append("/ ");
      if (!srcSsh1.equals(backupSsh))
         if (backupSsh.contains("@"))
            rsyncSB.append(backupSsh).append(":");
      rsyncSB.append(bDir).append("/");
      if (srcSsh1.equals(backupSsh))
         if (srcSsh1.contains("@"))
            rsyncSB.insert(0, "ssh " + srcSsh1 + " '").append("'"); // gesamten Befehl senden ;-)
         else
            rsyncSB.insert(0, srcSsh1); // nur sudo, kein quoting !
      String rsyncCmd=rsyncSB.toString();
      logln(4, rsyncCmd);// if (!DRYRUN.get())
      try (CmdStream rsyncStream=Commandline.executeCached(rsyncCmd, null)) { // not cached
         rsyncStream.backgroundErr();
         rsyncStream.erg().forEach(t -> logln(4, t));
         for (String line:rsyncStream.errList())
            if (line.contains("No route to host") || line.contains("Connection closed")
                     || line.contains("connection unexpectedly closed")) {
               Backsnap.connectionLost=10;
               break;
            } // ende("");// R
      }
   }
   /**
    * löscht eines der Backups im Auftrag der GUI
    * 
    * @param s
    * @throws IOException
    */
   public static void removeSnapshot(Snapshot s) throws IOException {
      StringBuilder removeSB =new StringBuilder("/bin/btrfs subvolume delete -Cv ").append(s.getBackupMountPath());
      String        removeCmd=s.mount().pc().getCmd(removeSB);
      if (bsGui != null) {
         bsGui.getLblSnapshot().setText("remove backup of:");
         bsGui.getTxtSnapshot().setText(s.dirName());
         bsGui.getLblParent().setText(" ");
         bsGui.getTxtParent().setText(" ");
         bsGui.getPanelWork().repaint(50);
      }
      log(4, removeCmd);
      // if (!DRYRUN.get())
      BTRFS_LOCK.lock();
      try {
         if (bsGui != null) {
            SwingUtilities.invokeLater(() -> bsGui.getPanelMaintenance().updateButtons());
            String text="<html>" + s.btrfsPath().toString();
            logln(7, text);
            bsGui.mark(s);
         }
         try (CmdStream removeStream=Commandline.executeCached(removeCmd, null)) {
            removeStream.backgroundErr();
            logln(1, "");
            removeStream.erg().forEach(line -> {
               log(1, line);
               if (GUI.get())
                  bsGui.lblPvSetText(line);
            });
            removeStream.waitFor();
         }
      } finally {
         BTRFS_LOCK.unlock();
         if (bsGui != null)
            SwingUtilities.invokeLater(() -> bsGui.getPanelMaintenance().updateButtons());
      }
   }
   /**
    * @param d
    * @param backupSsh
    * @return
    * @throws IOException
    */
   private static void mkDirs(Path d, String backupSsh) throws IOException {
      if (d.isAbsolute()) {
         log(6, " mkdir:" + d);
         if (DRYRUN.get())
            return;
         if (backupSsh.isBlank())
            if (d.toFile().mkdirs())
               return; // erst mit sudo, dann noch mal mit localhost probieren
         StringBuilder mkdirSB=new StringBuilder("mkdir -pv ").append(d);
         if (backupSsh.contains("@"))
            mkdirSB.insert(0, "ssh " + backupSsh + " '").append("'");
         else
            mkdirSB.insert(0, backupSsh);
         try (CmdStream mkdirStream=Commandline.executeCached(mkdirSB, null)) {
            mkdirStream.backgroundErr();
            if (mkdirStream.erg().peek(t -> logln(6, t)).anyMatch(Pattern.compile("mkdir").asPredicate()))
               return;
            if (mkdirStream.errList().isEmpty())
               return;
         }
      }
      throw new FileNotFoundException("Could not create dir: " + d);
   }
   /**
    * prozesse aufräumen
    * 
    * @param t
    */
   private final static void ende(String t) {
      log(4, "ende:");
      if (task != null)
         try {
            task.get(10, TimeUnit.SECONDS);
         } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
         }
      log(4, t);
      if (t.startsWith("X")) {
         log(4, " ready");
         if (GUI.get()) {
            if (bsGui != null) {
               JProgressBar sl=bsGui.getSpeedBar();
               sl.setString("Ready to exit".toString());
               sl.repaint(100);
            }
            if (AUTO.get()) {
               if ((bsGui != null) && (bsGui.frame instanceof Frame frame)) {
                  JProgressBar  exitBar       =bsGui.getSpeedBar();
                  JToggleButton pauseButton   =bsGui.getTglPause();
                  int           countdownStart=10;
                  if (AUTO.getParameterOrDefault(10) instanceof Integer n)
                     countdownStart=n;
                  exitBar.setMaximum(countdownStart);
                  int countdown=countdownStart;
                  while (countdown-- > 0) {
                     exitBar.setString(Integer.toString(countdown) + " sec till exit");
                     exitBar.setValue(countdownStart - countdown);
                     do {
                        try {
                           Thread.sleep(1000);
                        } catch (InterruptedException ignore) {/* ignore */}
                     } while (pauseButton.isSelected());
                  }
                  frame.setVisible(false);
                  frame.dispose();
               }
            } else
               while (bsGui != null)
                  try {
                     if (bsGui.frame == null)
                        break;
                     Thread.sleep(1000);
                  } catch (InterruptedException ignore) {/* */}
         }
         if (TIMESHIFT.get())
            try {
               mountBtrfsRoot(srcPc, srcDir, false);
            } catch (IOException e) {/* */ } // umount
         log(4, " to");
         Commandline.background.shutdown();
         log(4, " exit");
         Commandline.cleanup();
         log(4, " java");
         if (AUTO.get())
            System.exit(0);
      }
      logln(4, "");
   }
   public static void log(int level, String text) {
      if (Backsnap.VERBOSE.getParameterOrDefault(1) instanceof Integer v)
         if (v >= level)
            System.out.print(text);
   }
   public static void logln(int level, String text) {
      if (Backsnap.VERBOSE.getParameterOrDefault(1) instanceof Integer v)
         if (v >= level)
            System.out.println(text);
   }
}
