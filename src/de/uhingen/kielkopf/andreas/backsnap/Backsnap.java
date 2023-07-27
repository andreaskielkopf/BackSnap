package de.uhingen.kielkopf.andreas.backsnap;

import static java.lang.System.err;

import java.awt.Frame;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
   static private final String       DEFAULT_SRC      ="sudo:/";
   static private final String       DEFAULT_BACKUP   ="sudo:/mnt/BackSnap/manjaro23";
   static boolean                    usePv            =false;
   static int                        lastLine         =0;
   static String                     canNotFindParent;
   static public int                 disconnectCount  =0;
   static Future<?>                  task             =null;
   static public BacksnapGui         bsGui            =null;
   // static private Mount refreshBackupVolume=null;
   // static private String refreshBackupLabel =null;
   static private int                textVorhanden    =0;
   static final Flag                 HELP             =new Flag('h', "help");           // show usage
   static final Flag                 VERSION          =new Flag('x', "version");        // show date and version
   static final Flag                 DRYRUN           =new Flag('d', "dryrun");         // do not do anythimg ;-)
   static public final Flag          VERBOSE          =new Flag('v', "verbose");
   static public final Flag          SINGLESNAPSHOT   =new Flag('s', "singlesnapshot"); // backup exactly one snapshot
   static public final Flag          TIMESHIFT        =new Flag('t', "timeshift");
   static final Flag                 GUI              =new Flag('g', "gui");            // enable gui (works only with
                                                                                        // sudo)
   static final Flag                 AUTO             =new Flag('a', "auto");           // auto-close gui when ready
   static public final Flag          COMPRESSED       =new Flag('c', "compressed");
   static public final String        SNAPSHOT         ="snapshot";
   static public final String        DOT_SNAPSHOTS    =".snapshots";
   static public final String        AT_SNAPSHOTS     ="@snapshots";
   static public final Flag          DELETEOLD        =new Flag('o', "deleteold");      // mark old snapshots for
                                                                                        // deletion
   static public final Flag          KEEP_MINIMUM     =new Flag('m', "keepminimum");    // mark all but minimum
                                                                                        // snapshots
   static public final String        BACK_SNAP_VERSION=                                 // version
            "BackSnap for Snapper and Timeshift(beta) Version 0.6.2.14 (2023/07/27)";
   static public final ReentrantLock BTRFS_LOCK       =new ReentrantLock();
   static public void main(String[] args) {
      Flag.setArgs(args, DEFAULT_SRC + " " + DEFAULT_BACKUP);
      logln(0, BACK_SNAP_VERSION);
      logln(1, "args > " + Flag.getArgs());
      if (VERSION.get())
         System.exit(0);
      if (DRYRUN.get())
         logln(0, "Doing a dry run ! ");
      TIMESHIFT.set(true);
      // Parameter sammeln für SOURCE
      String[] source=Flag.getParameterOrDefault(0, DEFAULT_SRC).split("[:]");
      Pc srcPc=Pc.getPc(switch (source.length) {
         // case 0 -> throw new IllegalArgumentException("Ein Configfile wird noch nicht unterstützt");
         case 1 -> null; // localhost
         case 2 -> source[0]; // extern
         default -> throw new IllegalArgumentException("Mehr als ein Doppelpunkt ist nicht erlaubt");
      });
      Path srcDir=Path.of("/", source[source.length - 1].replace(DOT_SNAPSHOTS, ""));
      BTRFS_LOCK.lock();
      try {
         if (TIMESHIFT.get())
            srcPc.mountBtrfsRoot(srcDir, true);
         // Start collecting information
         SnapConfig srcConfig=SnapConfig.getConfig(srcPc, srcDir);
         if (srcConfig == null)
            throw new RuntimeException("Could not find snapshots for srcDir: " + srcDir);
         srcConfig.volumeMount().populate();
         if (srcConfig.volumeMount().btrfsMap().isEmpty())
            throw new RuntimeException("Ingnoring, because there are no snapshots in: " + srcDir);
         logln(1, "Backup snapshots from " + srcConfig.volumeMount().keyM());
         // BackupVolume ermitteln
         String[] backup=Flag.getParameterOrDefault(1, DEFAULT_BACKUP).split("[:]");
         Pc backupPc=Pc.getPc(switch (backup.length) {
            // case 0 -> throw new IllegalArgumentException("Ein Configfile wird noch nicht unterstützt");
            case 1 -> null; // localhost
            case 2 -> backup[0]; // extern
            default -> throw new IllegalArgumentException("Mehr als ein Doppelpunkt ist nicht erlaubt");
         });
         String backupDir=backup[backup.length - 1];
         // String backupLabel=Paths.get(backupDir).getFileName().toString();
         backupPc.setBackupLabel(Paths.get(backupDir).getFileName().toString());
         backupPc.getMountList(false); // eventuell unnötig
         Mount backupVolume=backupPc.getBackupVolume(/* backupDir */);
         if (backupVolume == null)
            throw new RuntimeException(
                     "Could not find Volume for backupDir: " + Pc.MNT_BACKSNAP + backupPc.getBackupLabel());
         if (backupVolume.devicePath().equals(srcConfig.volumeMount().devicePath()) && (srcPc == backupPc))
            throw new RuntimeException(
                     "Backup not possible onto same device: " + backupPc.getBackupLabel() + " <= " + srcDir);
         Usage usage=new Usage(backupVolume, false);
         if (usage.needsBalance())
            System.err.println("It seems urgently advisable to balance the backup volume");
         logln(2, "Try to use backupDir  " + backupVolume.keyM());
         SnapTree backupTree=SnapTree.getSnapTree(backupVolume);
         if (disconnectCount > 0) {
            err.println("no SSH Connection");
            ende("X");
            System.exit(0);
         }
         if (GUI.get())
            bsGui=BacksnapGui.getGui(srcConfig, backupTree, backupPc, usage);
         if (usage.isFull())
            throw new RuntimeException(
                     "Backup volume has less than 10GiB unallocated: " + usage.unallcoated() + " of " + usage.size());
         try {
            usePv=Paths.get("/bin/pv").toFile().canExecute();
         } catch (Exception e1) {/* */}
         /// Alle Snapshots einzeln sichern
         int counter=0;
         for (Snapshot sourceSnapshot:srcConfig.volumeMount().otimeKeyMap().values()) {
            counter++;
            if (canNotFindParent != null) {
               err.println("Please remove " + Pc.MNT_BACKSNAP + "/" + backupPc.getBackupLabel() + "/" + canNotFindParent
                        + "/" + SNAPSHOT + " !");
               ende("X");
               System.exit(-9);
            } else
               if (disconnectCount > 3) {
                  err.println("SSH Connection lost !");
                  ende("X");
                  System.exit(-8);
               }
            try {
               if (bsGui instanceof BacksnapGui gui && gui.getProgressBar() instanceof JProgressBar progressbar) {
                  progressbar.setValue(counter);
                  progressbar.setString(counter + "/" + srcConfig.volumeMount().otimeKeyMap().size());
                  progressbar.repaint(50);
               }
               if (!backup(sourceSnapshot, srcConfig.snapshotMount(), backupTree,
                        /* backupPc.getBackupLabel(), */ srcPc, backupPc))
                  continue;
               // Anzeige im Progressbar anpassen
               if (bsGui != null)
                  bsGui.refreshGUI(backupPc);
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
         if (TIMESHIFT.get())
            try {
               srcPc.mountBtrfsRoot(srcDir, false);
            } catch (IOException e) {/* */ } // umount
      }
      ende("X");
      System.exit(-2);
   }
   static private Snapshot parentSnapshot=null;
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
   static private boolean backup(Snapshot srcSnapshot, Mount srcVolume, SnapTree backupMap, //
            /* String backupLabel, */
            Pc srcPc1, Pc backupPc1) throws IOException {
      if (bsGui instanceof BacksnapGui gui)
         gui.setBackupInfo(srcSnapshot, parentSnapshot);
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
      logln(9, "Paths.get(backupDir=" + backupPc1.getBackupLabel() + " dirName=" + srcSnapshot.dirName() + ")");
      Path bDir=Paths.get(Pc.MNT_BACKSNAP, backupPc1.getBackupLabel(), srcSnapshot.dirName());
      Path relMdir=backupMap.mount().mountPath().relativize(bDir);
      Path bpq=backupMap.mount().btrfsPath().resolve(relMdir);
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
      mkDirs(bDir, backupPc1);
      rsyncFiles(srcPc1, backupPc1, sDir, bDir);
      if (GUI.get())
         bsGui.mark(srcSnapshot);
      Pc backupPc=backupMap.mount().pc();
      if (sendBtrfs(srcPc1, backupPc1, srcSnapshot, bDir, backupPc))
         parentSnapshot=srcSnapshot;
      return true;
   }
   static private boolean sendBtrfs(Pc srcPc4, Pc backupPc4, Snapshot s, Path bDir, Pc backupPc) throws IOException {
      boolean sameSsh=(srcPc4.isExtern() && srcPc4.equals(backupPc4));
      StringBuilder btrfsSendSB=new StringBuilder("/bin/btrfs send ");
      if (bsGui != null) {
         bsGui.getLblSnapshot().setText("backup of : ");
         bsGui.getTxtSnapshot().setText(s.dirName());
         bsGui.getLblParent().setText((parentSnapshot == null) ? " " : "based on:");
         bsGui.getTxtParent().setText((parentSnapshot == null) ? " " : parentSnapshot.dirName());
         bsGui.getPanelWork().repaint(50);
      }
      if (COMPRESSED.get()) {
         if (s.mount().pc().getBtrfsVersion() instanceof Version v)
            if (v.getMayor() < 6)
               COMPRESSED.set(false);
         if (s.mount().pc().getKernelVersion() instanceof Version v)
            if (v.getMayor() < 6)
               COMPRESSED.set(false);
         if (backupPc.getBtrfsVersion() instanceof Version v)
            if (v.getMayor() < 6)
               COMPRESSED.set(false);
      }
      if (COMPRESSED.get())
         btrfsSendSB.append("--proto 2 --compressed-data ");
      if (parentSnapshot != null) // @todo genauer prüfen
         btrfsSendSB.append("-p ").append(parentSnapshot.getSnapshotMountPath()).append(" ");
      if (s.btrfsPath().toString().contains("timeshift-btrfs"))
         Snapshot.setReadonly(parentSnapshot, s, true);
      logln(2, "");
      btrfsSendSB.append(s.getSnapshotMountPath());
      if (!sameSsh)
         if (srcPc4.isExtern())
            btrfsSendSB.insert(0, "ssh " + srcPc4.extern() + " '").append("'");
         else
            btrfsSendSB.insert(0, srcPc4.extern());
      if (usePv)
         btrfsSendSB.append("|/bin/pv -f");
      btrfsSendSB.append("|");
      if (!sameSsh)
         if (backupPc4.isExtern())
            btrfsSendSB.append("ssh " + backupPc4.extern() + " '");
         else
            btrfsSendSB.append(backupPc4.extern());
      btrfsSendSB.append("/bin/btrfs receive ").append(bDir).append(";/bin/sync");
      if (sameSsh)
         if (srcPc4.isExtern())
            btrfsSendSB.insert(0, "ssh " + srcPc4.extern() + " '");
      // else
      // send_cmd.insert(0, srcSsh); // @todo für einzelnes sudo anpassen ?
      if (backupPc4.isExtern())
         btrfsSendSB.append("'");
      logln(2, btrfsSendSB.toString());
      if (!DRYRUN.get()) {
         BTRFS_LOCK.lock();
         try (CmdStream btrfsSendStream=Commandline.executeCached(btrfsSendSB, null)) {
            if (bsGui != null)
               SwingUtilities.invokeLater(() -> bsGui.getPanelMaintenance().updateButtons());
            task=Commandline.background.submit(() -> btrfsSendStream.err().forEach(line -> {
               if (line.contains("ERROR: cannot find parent subvolume"))
                  Backsnap.canNotFindParent=line;
               if (line.contains("No route to host") || line.contains("Connection closed")
                        || line.contains("connection unexpectedly closed"))
                  Backsnap.disconnectCount=10;
               if (line.contains("<=>")) { // from pv
                  err.print(line);
                  show(line);
                  String lf=(Backsnap.lastLine == 0) ? "\n" : "\r";
                  err.print(lf);
                  Backsnap.lastLine=line.length();
                  if (line.contains(":00 ")) {
                     err.print("\n");
                     Backsnap.disconnectCount=0;
                  }
                  if (line.contains("0,00 B/s")) {
                     err.println();
                     err.println("HipCup");
                     Backsnap.disconnectCount++;
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
         Snapshot.setReadonly(parentSnapshot, s, false);
      return true;
   }
   static StringBuilder pv=new StringBuilder("- Info -");
   /**
    * @param line
    */
   static private final void show(String line) {
      if (bsGui == null)
         return;
      if (line.equals("\n") || line.equals("\r"))
         return;
      bsGui.lblPvSetText(line);
   }
   static private void rsyncFiles(Pc srcPc3, Pc backupPc3, Path sDir, Path bDir) throws IOException {
      StringBuilder rsyncSB=new StringBuilder("/bin/rsync -vdcptgo --exclude \"@*\" --exclude \"" + SNAPSHOT + "\" ");
      if (DRYRUN.get())
         rsyncSB.append("--dry-run ");
      if (!srcPc3.equals(backupPc3))
         if (srcPc3.isExtern())
            rsyncSB.append(srcPc3.extern()).append(":");
      rsyncSB.append(sDir.getParent()).append("/ ");
      if (!srcPc3.equals(backupPc3))
         if (backupPc3.isExtern())
            rsyncSB.append(backupPc3.extern()).append(":");
      rsyncSB.append(bDir).append("/");
      if (srcPc3.equals(backupPc3))
         if (srcPc3.isExtern())
            rsyncSB.insert(0, "ssh " + srcPc3.extern() + " '").append("'"); // gesamten Befehl senden ;-)
         else
            rsyncSB.insert(0, srcPc3.extern()); // nur sudo, kein quoting !
      String rsyncCmd=rsyncSB.toString();
      logln(4, rsyncCmd);// if (!DRYRUN.get())
      try (CmdStream rsyncStream=Commandline.executeCached(rsyncCmd, null)) { // not cached
         rsyncStream.backgroundErr();
         rsyncStream.erg().forEach(t -> logln(4, t));
         rsyncStream.waitFor();
         for (String line:rsyncStream.errList())
            if (line.contains("No route to host") || line.contains("Connection closed")
                     || line.contains("connection unexpectedly closed")) {
               Backsnap.disconnectCount=10;
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
   static public void removeSnapshot(Snapshot s) throws IOException {
      Path bmp=s.getBackupMountPath();
      if (!bmp.toString().startsWith(Pc.MNT_BACKSNAP) || bmp.toString().contains("../"))
         throw new SecurityException("I am not allowed to delete " + bmp.toString());
      StringBuilder removeSB=new StringBuilder("/bin/btrfs subvolume delete -Cv ").append(bmp);
      String removeCmd=s.mount().pc().getCmd(removeSB);
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
   static private void mkDirs(Path d, Pc backupPc2) throws IOException {
      if (d.isAbsolute()) {
         StringBuilder mkdirSB=new StringBuilder("mkdir -pv ").append(d);
         String mkdirCmd=backupPc2.getCmd(mkdirSB);
         log(6, mkdirCmd);
         if (DRYRUN.get())
            return;
         if (!backupPc2.isExtern())
            if (d.toFile().mkdirs())
               return; // erst mit sudo, dann noch mal mit localhost probieren
         try (CmdStream mkdirStream=Commandline.executeCached(mkdirCmd, null)) {
            mkdirStream.backgroundErr();
            mkdirStream.waitFor();
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
   static private final void ende(String t) {
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
                  JProgressBar exitBar=bsGui.getSpeedBar();
                  JToggleButton pauseButton=bsGui.getTglPause();
                  int countdownStart=10;
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
   static public void log(int level, String text) {
      if (Backsnap.VERBOSE.getParameterOrDefault(1) instanceof Integer v)
         if (v >= level)
            System.out.print(text);
   }
   static public void logln(int level, String text) {
      if (Backsnap.VERBOSE.getParameterOrDefault(1) instanceof Integer v)
         if (v >= level)
            System.out.println(text);
   }
}
