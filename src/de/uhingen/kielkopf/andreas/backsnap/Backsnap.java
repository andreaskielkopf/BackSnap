package de.uhingen.kielkopf.andreas.backsnap;

import static de.uhingen.kielkopf.andreas.backsnap.btrfs.Btrfs.BTRFS;
import static de.uhingen.kielkopf.andreas.backsnap.config.Log.lnlog;

import java.awt.Frame;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.*;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import de.uhingen.kielkopf.andreas.backsnap.btrfs.*;
import de.uhingen.kielkopf.andreas.backsnap.config.Log;
import de.uhingen.kielkopf.andreas.backsnap.config.Log.LEVEL;
import de.uhingen.kielkopf.andreas.backsnap.config.OnTheFly;
import de.uhingen.kielkopf.andreas.backsnap.gui.BacksnapGui;
import de.uhingen.kielkopf.andreas.backsnap.gui.part.SnapshotLabel.STATUS;
import de.uhingen.kielkopf.andreas.beans.Version;
import de.uhingen.kielkopf.andreas.beans.cli.Flag;
import de.uhingen.kielkopf.andreas.beans.minijson.Etc;
import de.uhingen.kielkopf.andreas.beans.shell.CmdStreams;

/**
 * License: 'GNU General Public License v3.0'
 * 
 * © 2023
 * 
 * @author Andreas Kielkopf
 * @see https://github.com/andreaskielkopf/BackSnap
 * @see https://forum.manjaro.org/t/howto-hilfsprogramm-fur-backup-btrfs-snapshots-mit-send-recieve timeshift ssh
 */
public class Backsnap {
   static public final ExecutorService virtual        =Version.getVx();
   static public String                cantFindParent =null;
   static public int                   disconnectCount=0;
   static Future<?>                    task           =null;
   static public BacksnapGui           bsGui          =null;
   static public OneBackup             actualBackup   =null;
   static private int                  skipCount      =0;
   static public final String          LF             =System.lineSeparator();
   static final Flag                   HELP           =new Flag('h', "help");           // show usage
   static final Flag                   VERSION        =new Flag('x', "version");        // show date and version
   static final Flag                   DRYRUN         =new Flag('d', "dryrun");         // do not do anythimg ;-)
   public static final Flag            VERBOSE        =new Flag('v', "verbose");
   static public final Flag            SINGLESNAPSHOT =new Flag('s', "singlesnapshot"); // backup exactly one snapshot
   // static public final Flag TIMESHIFT =new Flag('t', "timeshift");
   static public final Flag            GUI            =new Flag('g', "gui");            // enable gui (only with sudo)
   static final Flag                   AUTO           =new Flag('a', "auto");           // auto-close gui when ready
   static final Flag                   COMPRESSED     =new Flag('c', "compressed");     // use protokoll 2
   static final Flag                   INIT           =new Flag('i', "init");           // init /etc/backsnap.d/local.conf
   static public final Flag            DELETEOLD      =new Flag('o', "deleteold");      // mark old snapshots for deletion
   static public final Flag            KEEP_MINIMUM   =new Flag('m', "keepminimum");    // mark all but minimum snapshots
   static final Flag                   ECLIPSE        =new Flag('z', "eclipse");
   static final Flag                   PEXEC          =new Flag('p', "pexec");          // use pexec instead of sudo
   static public final String          BS_VERSION     ="BackSnap Version 0.6.7.7"       //
            + " (2024/07/01)";
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
      try { // Wenn notwendig initialisieren und configuration laden
         OneBackup.setConfig((INIT.get() ? OnTheFly.prepare() : Etc.getConfig("backsnap")));
      } catch (IOException e) {
         e.printStackTrace();
      }
      // Wenn Parameter da sind, dann zuerst die auswerten
      if (!Flag.getParameter(0).isBlank())
         processParameters();
      Log.logln(OneBackup.getConfigText(), LEVEL.CONFIG);
      OneBackup lastBackup=null;
      if (HELP.get())
         System.exit(2);
      for (OneBackup ob:OneBackup.getSortedBackups()) {
         actualBackup=ob;
         if (!actualBackup.srcPc().isReachable())
            continue;
         BTRFS.writeLock().lock();
         try {
            // Passe die Flags an
            if (actualBackup.flags() instanceof String s) {
               String a=String.join(" ", args).concat(" ").concat(s);
               Flag.setArgs(a.split(" "), "");
            } else
               Flag.setArgs(args, "");
            if (lastBackup instanceof OneBackup last && actualBackup.srcPc() != last.srcPc())
               try {
                  last.srcPc().mountBtrfsRoot(last.srcPath(), false);// umount
               } catch (IOException e) {/*  */ }
            actualBackup.mountBtrfsRoot();// mount
            if (!actualBackup.srcPc().isReachable())
               continue;
            lastBackup=actualBackup;
            // Start collecting information
            SnapConfig srcConfig=actualBackup.getSnapConfig();
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
            actualBackup.backupTree()[0]=SnapTree.getSnapTree(backupMount, false);
            if (disconnectCount > 0) {
               Log.errln("no SSH Connection", LEVEL.ERRORS);
               ende("X");
               System.exit(0);
            }
            if (GUI.get())
               bsGui=BacksnapGui.getGui(srcConfig, actualBackup.backupTree()[0], usage);
            if (bsGui instanceof BacksnapGui g) {
               final JProgressBar speedBar=g.getSpeedBar();
               SwingUtilities.invokeLater(() -> {
                  speedBar.setValue(0);
                  speedBar.setString("doing Backups");
               });
            }
            if (usage.getFreeGB() < 0) // isFull
               throw new RuntimeException(
                        LF + "The backup volume has less than 10GiB unallocated: " + usage.unallcoated() + " of "
                                 + usage.size() + LF + "Please free some space on the backup volume");
            /// Alle Snapshots einzeln sichern
            int counter=0;
            for (Snapshot sourceSnapshot:srcConfig.volumeMount().otimeKeyMap().values()) {
               counter++;
               if (cantFindParent != null) {
                  Log.errln("Please remove " + Pc.TMP_BACKSNAP + "/" + OneBackup.backupPc.getBackupLabel() + "/"
                           + cantFindParent + "/" + Snapshot.SNAPSHOT + " !", LEVEL.ERRORS);
                  ende("X");
                  System.exit(-9);
               } else
                  if (disconnectCount > 3) {
                     Log.errln("SSH Connection lost !", LEVEL.ERRORS);
                     ende("X");
                     System.exit(-8);
                  }
               try {
                  if (bsGui instanceof BacksnapGui gui)
                     gui.updateProgressbar(counter, srcConfig.volumeMount().otimeKeyMap().size());
                  // -------------------------------------------------
                  if (!backup(actualBackup, sourceSnapshot))
                     continue;
                  // -------------------------------------------------
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
               Log.errln(e.getMessage(), LEVEL.ERRORS);
            else
               e.printStackTrace();
            if (OneBackup.size() <= 1) {
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
   }// filaized
   /**
    * Bearbeite die übergebenen Parameter und wähle die entsprechenden Backups aus
    */
   private static void processParameters() {
      if (OneBackup.unsortedMap.containsKey(Flag.getParameter(0)) || Flag.getParameter(0).matches(".*[*+?|].*")) {
         List<String> pList=Flag.getParameterList(); // System.out.println("Treffer");
         Keys: for (String key:OneBackup.unsortedMap.keySet()) {// für jedes OneBackup
            for (String param:pList)// für jeden parameter
               if (parameterPasst(key, param))
                  continue Keys;// in den Maps lassen !
            OneBackup value=OneBackup.unsortedMap.remove(key); // mit key löschen
            for (String sortedKey:OneBackup.sortedMap.keySet())
               OneBackup.sortedMap.remove(sortedKey, value); // mit value löschen
         }
      } else // legacy
         if (!Flag.getParameter(1).isBlank()) { // Wenn 2 Parameter da sind, dann diese verwenden
            OneBackup.unsortedMap.clear(); // Kommandozeile statt config, aber Basisconfig behalten
            OneBackup.sortedMap.clear();
            String[] source=Flag.getParameter(0).split("[:]"); // Parameter sammeln für SOURCE
            String[] backup=Flag.getParameter(1).split("[:]"); // BackupVolume ermitteln
            OneBackup.backupPc=(backup.length == 1) ? Pc.getPc(null) : Pc.getPc(backup[0]);
            if (OneBackup.backupPc instanceof Pc bPc) // Btrfs.BTRFS.lock();
               bPc.setBackupLabel(Paths.get(backup[backup.length - 1]).getFileName());
            else
               throw new RuntimeException(LF + "Could not find Backuplabel " + String.join(" : ", backup));
            OneBackup o=new OneBackup(Path.of(""), Pc.getPc(source[0]),
                     Path.of("/", source[source.length - 1].replace(Snapshot.DOT_SNAPSHOTS, "")),
                     OneBackup.backupPc.getBackupLabel(), null, new SnapTree[1]);
            OneBackup.unsortedMap.put(OneBackup.backupPc.getBackupLabel().toString(), o);
            OneBackup.sortedMap.put(OneBackup.backupPc.getBackupLabel().toString(), o);
         }
      // Ansonsten alle Backups durchführen
   }
   /**
    * @param key
    * @param param
    * @return
    */
   private static boolean parameterPasst(String key, String param) {
      // if (key.equals(param))
      // return true;
      try {// versuche Regular expression
         if (key.matches(param))
            return true;
      } catch (PatternSyntaxException ignore) {
         System.err.println(ignore);
      }
      if (param.endsWith("*")) {
         // if (key.equals(param.substring(0, param.length() - 1)))
         // return true;
         if (key.startsWith(param.substring(0, param.length() - 1) + "."))
            return true;
      }
      return false;
   }
   static private Snapshot parentSnapshot=null;
   /**
    * Versuchen genau diesen einzelnen Snapshot zu sichern
    * 
    * @param oneBackup
    *           Das aktuell durchzuführende Backup
    * @param srcSnapshot
    *           Der nächste Snapshot der dran ist
    * @param backupSnapTree
    * @throws IOException
    * @return false bei Misserfolg
    */
   static private boolean backup(OneBackup oneBackup, Snapshot srcSnapshot) throws IOException {
      if (bsGui instanceof BacksnapGui gui)
         gui.setBackupInfo(srcSnapshot, parentSnapshot);
      if (srcSnapshot.isBackup()) {
         lnlog("Ignore:" + srcSnapshot.dirName(), LEVEL.CONFIG);
         return false;
      }
      if (oneBackup.backupTree()[0].containsBackupOf(srcSnapshot)) {
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
      Path bSnapDir=oneBackup.backupTree()[0].getSnapPath(bDir);
      // sMount().btrfsPath().resolve(backupSnapTree.sMount().mountPath().relativize(bDir))
      // .resolve(Snapshot.SNAPSHOT);
      if (oneBackup.backupTree()[0].containsPath(bSnapDir)) {
         Log.logln("Der Snapshot scheint schon da zu sein ????", LEVEL.SNAPSHOTS);
         return true;
      }
      Log.logln(oneBackup.backupLabel() + ": Backup of " + srcSnapshot.dirName()
               + (parentSnapshot instanceof Snapshot ps ? " based on " + ps.dirName() : ""), LEVEL.SNAPSHOTS);
      mkDirs(bDir);
      rsyncFiles(oneBackup, srcSnapshot.getSnapshotMountPath(), bDir);
      if (GUI.get())
         bsGui.mark(srcSnapshot.uuid(), STATUS.INPROGRESS); // Pc backupPc=backupMap.mount().pc();
      if (Btrfs.send_pv_receive(oneBackup, srcSnapshot, parentSnapshot, bDir, bsGui, DRYRUN.get(), COMPRESSED.get()))
         parentSnapshot=srcSnapshot;
      return true;
   }
   static StringBuilder pv=new StringBuilder("- Info -");
   private static Usage usage;
   static private void rsyncFiles(OneBackup oneBackup, Path sDir, Path bDir) throws IOException {
      StringBuilder rsyncSB=new StringBuilder(
               "rsync -vdcptgo --exclude \"@*\" --exclude \"" + Snapshot.SNAPSHOT + "\" ");
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
      try (CmdStreams rsyncStream=CmdStreams.getDirectStream(rsyncCmd)) {
         rsyncStream.outBGerr().forEach(t -> Log.logln(t, LEVEL.RSYNC));
         if (rsyncStream.errLines().anyMatch(line -> (line.contains("No route to host")
                  || line.contains("Connection closed") || line.contains("connection unexpectedly closed")))) {
            Backsnap.disconnectCount=10;
         }
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
         Log.log(mkdirCmd, LEVEL.BASIC);
         if (DRYRUN.get())
            return;
         if (!OneBackup.isBackupExtern()) {
            if (bdir.toFile().isDirectory())
               return;
            if (bdir.toFile().mkdirs())
               return; // erst mit sudo, dann noch mal mit localhost probieren
         }
         try (CmdStreams mkdirStream=CmdStreams.getDirectStream(mkdirCmd)) {
            if (mkdirStream.outBGerr().peek(t -> Log.logln(t, LEVEL.BASIC))
                     .anyMatch(Pattern.compile("mkdir").asPredicate()))
               return;
            // if (mkdirStream.errBuffer().queue(). isEmpty())
            // return;
            mkdirStream.errPrintln();
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
            if (bsGui != null) {
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
         CmdStreams.cleanup();
         Log.logln(" java", LEVEL.BASIC);
         if (AUTO.get())
            System.exit(0);
      }
      // logln(4, "");
   }
}
