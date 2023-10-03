package de.uhingen.kielkopf.andreas.backsnap;

import java.awt.Frame;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import static de.uhingen.kielkopf.andreas.backsnap.btrfs.Btrfs.BTRFS;
import static de.uhingen.kielkopf.andreas.backsnap.config.Log.*;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;
import de.uhingen.kielkopf.andreas.backsnap.btrfs.*;
import de.uhingen.kielkopf.andreas.backsnap.config.Log;
import de.uhingen.kielkopf.andreas.backsnap.config.Log.LEVEL;
import de.uhingen.kielkopf.andreas.backsnap.config.OnTheFly;
import de.uhingen.kielkopf.andreas.backsnap.gui.BacksnapGui;
import de.uhingen.kielkopf.andreas.backsnap.gui.part.SnapshotLabel.STATUS;
import de.uhingen.kielkopf.andreas.beans.Version;
import de.uhingen.kielkopf.andreas.beans.cli.Flag;
import de.uhingen.kielkopf.andreas.beans.minijson.Etc;

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
   static public final ExecutorService virtual        =Version.getVx();
   static int                          lastLine       =0;
   static String                       cantFindParent;
   static public int                   disconnectCount=0;
   static Future<?>                    task           =null;
   static public BacksnapGui           bsGui;
   static public OneBackup             actualBackup   =null;
   static private int                  skipCount      =0;
   static final Flag                   HELP           =new Flag('h', "help");                   // show usage
   static final Flag                   VERSION        =new Flag('x', "version");                // show date and version
   static final Flag                   DRYRUN         =new Flag('d', "dryrun");                 // do not do anythimg ;-)
   public static final Flag            VERBOSE        =new Flag('v', "verbose");
   static public final Flag            SINGLESNAPSHOT =new Flag('s', "singlesnapshot");         // backup exactly one snapshot
   // static public final Flag TIMESHIFT =new Flag('t', "timeshift");
   static public final Flag            GUI            =new Flag('g', "gui");                    // enable gui (only with sudo)
   static final Flag                   AUTO           =new Flag('a', "auto");                   // auto-close gui when ready
   // static final Flag NOSYNC =new Flag('n', "nosync"); // no sync after every command
   static final Flag                   COMPRESSED     =new Flag('c', "compressed");             // use protokoll 2
   static final Flag                   INIT           =new Flag('i', "init");                   // init /etc/backsnap.d/local.conf
   static public final Flag            DELETEOLD      =new Flag('o', "deleteold");              // mark old snapshots for deletion
   static public final Flag            KEEP_MINIMUM   =new Flag('m', "keepminimum");            // mark all but minimum snapshots
   static final Flag                   ECLIPSE        =new Flag('z', "eclipse");
   static final Flag                   PEXEC          =new Flag('p', "pexec");                  // use pexec instead of sudo
   static public final String          SNAPSHOT       ="snapshot";
   static public final String          BS_VERSION     ="BackSnap Version 0.6.6.17 (2023/09/28)";
   static public final String          LF             =System.lineSeparator();
   static public void main(String[] args) {
      Flag.setArgs(args, "");
      Log.setLoglevel(Backsnap.VERBOSE.getParameterOrDefault(LEVEL.PROGRESS.l));
      Log.logln(BS_VERSION, LEVEL.BASIC);
      Log.logln("args > " + Flag.getArgs(), LEVEL.BASIC);
      Log.logln(Version.getJava().toString(), LEVEL.BASIC);
      Log.logln(Version.getVxText(), LEVEL.BASIC);
      if (VERSION.get())
         System.exit(0);
      if (DRYRUN.get())
         Log.logln("Doing a dry run ! ", LEVEL.BASIC);
      if (GUI.get() && Commandline.processBuilder.environment() instanceof Map<String, String> env)
         env.putIfAbsent("SSH_ASKPASS_REQUIRE", "prefer");
      try { // Wenn notwendig initialisieren und configuration laden
         OneBackup.setConfig((INIT.get() ? OnTheFly.prepare() : Etc.getConfig("backsnap")));
      } catch (IOException e) {
         e.printStackTrace();
      }
      // Wenn 2 Parameter da sind, dann diese verwenden
      if (!Flag.getParameter(1).isBlank()) {
         OneBackup.backupList.clear(); // Kommandozeile statt config, aber Basisconfig behalten
         String[] source=Flag.getParameter(0).split("[:]"); // Parameter sammeln für SOURCE
         String[] backup=Flag.getParameter(1).split("[:]");// BackupVolume ermitteln
         OneBackup.backupPc=(backup.length == 1) ? Pc.getPc(null) : Pc.getPc(backup[0]);
         if (OneBackup.backupPc instanceof Pc bPc)
            // Btrfs.BTRFS.lock();
            bPc.setBackupLabel(Paths.get(backup[backup.length - 1]).getFileName());
         else
            throw new RuntimeException(LF + "Could not find Backuplabel " + String.join(" : ", backup));
         OneBackup.backupList.add(new OneBackup(Pc.getPc(source[0]),
                  Path.of("/", source[source.length - 1].replace(Snapshot.DOT_SNAPSHOTS, "")),
                  OneBackup.backupPc.getBackupLabel(), null));
      } // Wenn keine 2 Parameter da sind, config verwenden
      Log.logln(OneBackup.getConfigText(), LEVEL.CONFIG);
      OneBackup lastBackup=null;
      for (OneBackup ob:OneBackup.backupList) {
         actualBackup=ob;
         BTRFS.writeLock().lock();
         try {
            if (actualBackup.flags() instanceof String s) {
               String a=String.join(" ", args).concat(" ").concat(s);
               Flag.setArgs(a.split(" "), "");
            } else
               Flag.setArgs(args, "");
            if (lastBackup != null)
               if (actualBackup.srcPc() != lastBackup.srcPc())
                  try {
                     lastBackup.srcPc().mountBtrfsRoot(lastBackup.srcPath(), false);
                  } catch (IOException e) {/*  */ } // umount
            actualBackup.mountBtrfsRoot();
            lastBackup=actualBackup;
            // Start collecting information
            SnapConfig srcConfig=SnapConfig.getConfig(actualBackup);
            srcConfig.volumeMount().populate();
            Log.logln("Backup snapshots from " + srcConfig.volumeMount().keyM(), LEVEL.SNAPSHOTS);
            Pc.mountBackupRoot(true);
            OneBackup.backupPc.getMountList(false); // eventuell unnötig
            Mount backupMount=Pc.getBackupMount(/* true */);
            if (backupMount.devicePath().equals(srcConfig.volumeMount().devicePath()) && actualBackup.isSamePc())
               throw new RuntimeException(LF + "Backup is not possible onto the same device: "
                        + OneBackup.backupPc.getBackupLabel() + " <= " + actualBackup.srcPath() + LF
                        + "Please select another partition for the backup");
            Log.logln("Try to use backupDir  " + backupMount.keyM(), LEVEL.SNAPSHOTS);
            usage=new Usage(backupMount, false);
            backupTree=SnapTree.getSnapTree(backupMount);
            if (disconnectCount > 0) {
               System.err.println("no SSH Connection");
               ende("X");
               System.exit(0);
            }
            bsGui=GUI.get() ? bsGui=BacksnapGui.getGui(srcConfig, backupTree, usage) : null;
            if (bsGui instanceof BacksnapGui g) {
               final JProgressBar speedBar=g.getSpeedBar();
               SwingUtilities.invokeLater(() -> {
                  speedBar.setValue(0);
                  speedBar.setString("doing Backups");
               });
            }
            if (usage.isFull())
               throw new RuntimeException(
                        LF + "The backup volume has less than 10GiB unallocated: " + usage.unallcoated() + " of "
                                 + usage.size() + LF + "Please free some space on the backup volume");
            /// Alle Snapshots einzeln sichern
            int counter=0;
            for (Snapshot sourceSnapshot:srcConfig.volumeMount().otimeKeyMap().values()) {
               counter++;
               if (cantFindParent != null) {
                  System.err.println("Please remove " + Pc.TMP_BACKSNAP + "/" + OneBackup.backupPc.getBackupLabel()
                           + "/" + cantFindParent + "/" + SNAPSHOT + " !");
                  ende("X");
                  System.exit(-9);
               } else
                  if (disconnectCount > 3) {
                     System.err.println("SSH Connection lost !");
                     ende("X");
                     System.exit(-8);
                  }
               try {
                  if (bsGui instanceof BacksnapGui gui)
                     gui.updateProgressbar(counter, srcConfig.volumeMount().otimeKeyMap().size());
                  if (!backup(actualBackup, sourceSnapshot, backupTree))
                     continue;
                  // Anzeige im Progressbar anpassen
                  if (bsGui instanceof BacksnapGui gui)
                     gui.refreshGUI();
                  if (SINGLESNAPSHOT.get())// nur einen Snapshot übertragen und dann abbrechen
                     break;
               } catch (NullPointerException n) {
                  n.printStackTrace();
                  break;
               }
            }
            Log.logln("", LEVEL.SNAPSHOTS);
         } catch (IOException e) {
            if ((e.getMessage().startsWith("ssh: connect to host"))
                     || (e.getMessage().startsWith("Could not find snapshot:")))
               System.err.println(e.getMessage());
            else
               e.printStackTrace();
            if (OneBackup.backupList.size() <= 1) {
               try {
                  if (lastBackup != null)
                     lastBackup.srcPc().mountBtrfsRoot(lastBackup.srcPath(), false);
                  Pc.mountBackupRoot(false);
               } catch (IOException ignore) {/* */ } // umount
               ende("Xabbruch");
               System.exit(-1);
            }
         } finally {
            BTRFS.writeLock().unlock();
         }
         if (bsGui instanceof BacksnapGui gui)
            gui.getPanelMaintenance().updateButtons();
         pause();
      }
      try {
         if (lastBackup != null)
            lastBackup.srcPc().mountBtrfsRoot(lastBackup.srcPath(), false);
         Pc.mountBackupRoot(false);
      } catch (IOException ignore) {/* */ } // umount
      ende("X");
      System.exit(-2);
   }
   static private Snapshot parentSnapshot=null;
   /**
    * Versuchen genau diesen einzelnen Snapshot zu sichern
    * 
    * @param snapConfigs
    * @param sourceKey
    * @param sMap
    * @param dMap
    * @throws IOException
    * @return false bei Misserfolg
    */
   static private boolean backup(OneBackup oneBackup, Snapshot srcSnapshot, SnapTree backupMap) throws IOException {
      if (bsGui instanceof BacksnapGui gui)
         gui.setBackupInfo(srcSnapshot, parentSnapshot);
      if (srcSnapshot.isBackup()) {
         lnlog("Ignore:" + srcSnapshot.dirName(), LEVEL.CONFIG);
         return false;
      }
      if (backupMap.rUuidMap().containsKey(srcSnapshot.uuid())) {
         if (skipCount == 0)
            lnlog("Skip:", LEVEL.SNAPSHOTS);
         Log.log(" " + srcSnapshot.dirName(), LEVEL.SNAPSHOTS);
         skipCount++;
         parentSnapshot=srcSnapshot;
         return false;
      }
      if (skipCount > 0) {
         skipCount=0;
         Log.logln("", LEVEL.SNAPSHOTS);
      }
      Path bDir=Pc.TMP_BACKSNAP.resolve(oneBackup.backupLabel()).resolve(srcSnapshot.dirName());
      Path bSnapDir=backupMap.mount().btrfsPath().resolve(backupMap.mount().mountPath().relativize(bDir))
               .resolve(SNAPSHOT);
      if (backupMap.btrfsPathMap().containsKey(bSnapDir)) {
         Log.logln("Der Snapshot scheint schon da zu sein ????", LEVEL.SNAPSHOTS);
         return true;
      }
      Log.logln(oneBackup.backupLabel() + ": Backup of " + srcSnapshot.dirName()
               + (parentSnapshot instanceof Snapshot ps ? " based on " + ps.dirName() : ""), LEVEL.SNAPSHOTS);
      mkDirs(bDir);
      rsyncFiles(oneBackup, srcSnapshot.getSnapshotMountPath(), bDir);
      if (GUI.get())
         bsGui.mark(srcSnapshot.uuid(), STATUS.INPROGRESS); // Pc backupPc=backupMap.mount().pc();
      if (sendBtrfs(oneBackup, srcSnapshot, bDir))
         parentSnapshot=srcSnapshot;
      return true;
   }
   static Boolean usePv=null;
   static private boolean sendBtrfs(OneBackup oneBackup, Snapshot s, Path bDir) throws IOException {
      if (usePv == null)
         try { // teste ob pv da ist
            usePv=Paths.get("/bin/pv").toFile().canExecute();
         } catch (Exception e1) {/* */}
      StringBuilder btrfsSendSB=new StringBuilder(Btrfs.SEND);
      if (bsGui instanceof BacksnapGui gui)
         gui.setBackupInfo(s, parentSnapshot);
      if (COMPRESSED.get() && !oneBackup.compressionPossible())
         COMPRESSED.set(false);
      if (COMPRESSED.get())
         btrfsSendSB.append("--compressed-data ");
      if (parentSnapshot instanceof Snapshot p)
         btrfsSendSB.append("-p ").append(p.getSnapshotMountPath()).append(" ");
      if (s.btrfsPath().toString().contains("timeshift-btrfs"))
         Snapshot.setReadonly(parentSnapshot, s, true);
      // Log.logln(" ", LEVEL.BASIC);
      btrfsSendSB.append(s.getSnapshotMountPath());
      if (!oneBackup.isSameSsh())
         if (oneBackup.isExtern())
            btrfsSendSB.insert(0, "ssh " + oneBackup.extern() + " '").append("'");
         else
            btrfsSendSB.insert(0, oneBackup.extern());
      if (usePv)
         btrfsSendSB.append("|pv -f");
      btrfsSendSB.append("|");
      if (!oneBackup.isSameSsh())
         if (OneBackup.isBackupExtern())
            btrfsSendSB.append("ssh " + OneBackup.backupPc.extern() + " '");
         else
            btrfsSendSB.append(OneBackup.backupPc.extern());
      btrfsSendSB.append(Btrfs.RECEIVE).append(bDir);
      // if (NOSYNC.get())
      // btrfsSendSB.append(";sync");
      if (oneBackup.isSameSsh())
         if (oneBackup.isExtern())
            btrfsSendSB.insert(0, "ssh " + oneBackup.extern() + " '");
      // send_cmd.insert(0, srcSsh); // @todo für einzelnes sudo anpassen ?
      if (OneBackup.isBackupExtern())
         btrfsSendSB.append("'");
      Log.logln(btrfsSendSB.toString(), LEVEL.BTRFS);
      if (!DRYRUN.get()) {
         if (bsGui != null)
            bsGui.getPanelMaintenance().updateButtons();
         BTRFS.writeLock().lock();
         try (CmdStream btrfsSendStream=Commandline.executeCached(btrfsSendSB)) {
            Log.logln("########1#########", LEVEL.PROGRESS);
            task=virtual.submit(() -> btrfsSendStream.err().forEach(line -> {
               try {
                  // System.err.println(line);
                  lnlog(line, LEVEL.PROGRESS);
                  if (line.contains("ERROR: cannot find parent subvolume"))
                     Backsnap.cantFindParent=line;
                  if (line.contains("No route to host") || line.contains("Connection closed")
                           || line.contains("connection unexpectedly closed"))
                     Backsnap.disconnectCount=10;
                  if (line.contains("<=>")) { // from pv
                     log(line, LEVEL.PROGRESS);
                     if (Backsnap.lastLine == 0)
                        lnlog("", LEVEL.PROGRESS);
                     else
                        Owlog("", LEVEL.PROGRESS);
                     show(line);
                     Backsnap.lastLine++;
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
                     if (Backsnap.lastLine != 0) {
                        Backsnap.lastLine=0;
                        logln("", LEVEL.PROGRESS);
                     }
                     logln(line, LEVEL.PROGRESS);
                     show(line);
                  }
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }));
            Log.logln("#########2########", LEVEL.PROGRESS);
            btrfsSendStream.erg().forEach(line -> {
               try {
                  if (lastLine != 0) {
                     lastLine=0;
                     logln("", LEVEL.PROGRESS);
                  }
                  logln("", LEVEL.PROGRESS);
               } catch (Exception e) {
                  e.printStackTrace();
               }
            });
            Log.logln("########3##########", LEVEL.PROGRESS);
            task.get();
            Log.logln("########4##########", LEVEL.PROGRESS);
            // task.get();
            Log.logln("########5##########", LEVEL.PROGRESS);
            btrfsSendStream.waitFor();
            Log.logln("########6##########", LEVEL.PROGRESS);
            ConcurrentLinkedQueue<String> l=btrfsSendStream.errList();
            // ArrayList<String> m=new ArrayList<>(l);
            logln(l, LEVEL.PROGRESS);
         } catch (InterruptedException e) {
            e.printStackTrace();
         } catch (ExecutionException e) {
            e.printStackTrace();
         } finally {
            BTRFS.writeLock().unlock();
            lnlog("", LEVEL.PROGRESS);
         }
         if (bsGui != null)
            bsGui.getPanelMaintenance().updateButtons();
      }
      if (s.btrfsPath().toString().contains("timeshift-btrfs"))
         Snapshot.setReadonly(parentSnapshot, s, false);
      return true;
   }
   static StringBuilder    pv=new StringBuilder("- Info -");
   private static Usage    usage;
   private static SnapTree backupTree;
   /**
    * @param line
    */
   static private final void show(String line) {
      if (bsGui == null)
         return;
      line.replaceAll("[\n\r]?", " "); // if (line.equals("\n") || line.equals("\r")) return;
      if (!line.isBlank())
         bsGui.lblPvSetText(line);
   }
   static private void rsyncFiles(OneBackup oneBackup, Path sDir, Path bDir) throws IOException {
      StringBuilder rsyncSB=new StringBuilder("rsync -vdcptgo --exclude \"@*\" --exclude \"" + SNAPSHOT + "\" ");
      if (DRYRUN.get())
         rsyncSB.append("--dry-run ");
      if (!oneBackup.isSamePc() && (oneBackup.isExtern()))
         rsyncSB.append(oneBackup.extern()).append(":");
      rsyncSB.append(sDir.getParent()).append("/ ");
      if (!oneBackup.isSamePc() && (OneBackup.isBackupExtern()))
         rsyncSB.append(OneBackup.backupPc.extern()).append(":");
      rsyncSB.append(bDir).append("/");
      if (oneBackup.isSamePc())
         if (oneBackup.isExtern())
            rsyncSB.insert(0, "ssh " + oneBackup.extern() + " '").append("'"); // gesamten Befehl senden ;-)
         else
            rsyncSB.insert(0, oneBackup.extern()); // nur sudo, kein quoting !
      String rsyncCmd=rsyncSB.toString();
      Log.logln(rsyncCmd, LEVEL.RSYNC);// if (!DRYRUN.get())
      try (CmdStream rsyncStream=Commandline.executeCached(rsyncCmd, null)) { // not cached
         rsyncStream.backgroundErr();
         rsyncStream.erg().forEach(t -> Log.logln(t, LEVEL.RSYNC));
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
    * @param bdir
    * @param backupSsh
    * @return
    * @throws IOException
    */
   static private void mkDirs(Path bdir) throws IOException {
      if (bdir.isAbsolute()) {
         String mkdirCmd=OneBackup.backupPc.getCmd(new StringBuilder("mkdir -pv ").append(bdir), true);
         Log.log(mkdirCmd, LEVEL.RSYNC);
         if (DRYRUN.get())
            return;
         if (!OneBackup.isBackupExtern())
            if (bdir.toFile().mkdirs())
               return; // erst mit sudo, dann noch mal mit localhost probieren
         try (CmdStream mkdirStream=Commandline.executeCached(mkdirCmd, null)) {
            mkdirStream.backgroundErr();
            mkdirStream.waitFor();
            if (mkdirStream.erg().peek(t -> Log.logln(t, LEVEL.RSYNC)).anyMatch(Pattern.compile("mkdir").asPredicate()))
               return;
            if (mkdirStream.errList().isEmpty())
               return;
         }
      }
      throw new FileNotFoundException("Could not create dir: " + bdir);
   }
   static private final void pause() {
      if (GUI.get()) {
         if (bsGui != null)
            SwingUtilities.invokeLater(() -> {
               bsGui.getSpeedBar().setString("Ready"); // sl.repaint(100);
               bsGui.refreshGUI();
            });
         if (AUTO.get()) {
            if ((bsGui != null) && (bsGui.frame instanceof Frame frame)) {
               final float FAKTOR=2f;
               final int countdownStart=(int) (FAKTOR
                        * ((AUTO.getParameterOrDefault(10) instanceof Integer n) ? n : 10));
               final JProgressBar speedBar=bsGui.getSpeedBar();
               SwingUtilities.invokeLater(() -> speedBar.setMaximum(countdownStart));
               int countdown=countdownStart;
               while (countdown-- > 0) {
                  final float f=countdown / FAKTOR;
                  final int p=countdownStart - countdown;
                  SwingUtilities.invokeLater(() -> {
                     speedBar.setValue(p);
                     speedBar.setString(String.format("waiting for %2.1f sec", f));
                  });
                  try {
                     Thread.sleep((long) (1000 / FAKTOR));
                     while (bsGui.getTglPause().isSelected())
                        Thread.sleep(50);
                  } catch (InterruptedException ignore) {/* ignore */}
               }
            }
         } else
            while (bsGui != null)
               try {
                  if (bsGui.frame == null)
                     break;
                  if (!bsGui.getTglPause().isSelected())
                     break;
                  Thread.sleep(1000);
               } catch (InterruptedException ignore) {/* */}
      }
   }
   /**
    * prozesse aufräumen
    * 
    * @param t
    */
   static private final void ende(String t) {
      Log.log("ende:", LEVEL.BASIC);
      if (task != null)
         try {
            task.get(30, TimeUnit.SECONDS);
         } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
         }
      Log.log(t, LEVEL.BASIC);
      if (t.startsWith("X")) {
         Log.log(" ready", LEVEL.BASIC);
         if (GUI.get()) {
            if (bsGui != null)
               SwingUtilities.invokeLater(() -> {
                  JProgressBar sl=bsGui.getSpeedBar();
                  sl.setString("Ready to exit".toString()); // sl.repaint(100);
               });
            if (AUTO.get()) {
               if ((bsGui != null) && (bsGui.frame instanceof Frame frame)) {
                  final float FAKTOR=2f;
                  final int countdownStart=(int) (FAKTOR
                           * ((AUTO.getParameterOrDefault(10) instanceof Integer n) ? n : 10));
                  final JProgressBar speedBar=bsGui.getSpeedBar();
                  SwingUtilities.invokeLater(() -> speedBar.setMaximum(countdownStart));
                  int countdown=countdownStart;
                  while (countdown-- > 0) {
                     final float f=countdown / FAKTOR;
                     final int p=countdownStart - countdown;
                     SwingUtilities.invokeLater(() -> {
                        speedBar.setValue(p);
                        speedBar.setString(String.format("%2.1f sec till exit", f));
                        // speedBar.repaint(50);
                     });
                     try {
                        Thread.sleep((long) (1000 / FAKTOR));
                        while (bsGui.getTglPause().isSelected())
                           Thread.sleep(50);
                     } catch (InterruptedException ignore) {/* ignore */}
                  }
                  bsGui.getPrefs().saveFramePos(bsGui.frame);
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
         Log.log(" to", LEVEL.BASIC);
         try {
            virtual.shutdown();
            Log.log(" ex", LEVEL.BASIC);
            virtual.awaitTermination(120, TimeUnit.SECONDS);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
         Log.log("it", LEVEL.BASIC);
         Commandline.cleanup();
         Log.logln(" java", LEVEL.BASIC);
         if (AUTO.get())
            System.exit(0);
      }
      // logln(4, "");
   }
}// getVx
