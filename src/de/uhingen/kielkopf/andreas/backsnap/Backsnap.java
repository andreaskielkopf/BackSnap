package de.uhingen.kielkopf.andreas.backsnap;

import static java.lang.System.err;
import static java.lang.System.out;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;
import de.uhingen.kielkopf.andreas.backsnap.btrfs.*;
import de.uhingen.kielkopf.andreas.backsnap.gui.BacksnapGui;
import de.uhingen.kielkopf.andreas.beans.cli.Flag;

public class Backsnap {
   static String              parentKey          =null;
   private static Snapshot    parentSnapshot     =null;
   private static boolean     usePv              =false;
   static int                 lastLine           =0;
   static String              canNotFindParent   =null;
   static int                 connectionLost     =0;
   static Future<?>           task               =null;
   private static BacksnapGui bs;
   private static String      refreshGUIcKey     =null;
   private static Mount       refreshBackupVolume=null;
   private static String      refreshBackupDir   =null;
   final static Flag          GUI                =new Flag('g', "gui");            // show and wait for gui
   final static Flag          DRYRUN             =new Flag('d', "dryrun");         // do not do anythimg
   final static Flag          VERBOSE            =new Flag('v', "verbose");
   final static Flag          HELP               =new Flag('h', "help");           // show usage
   final static Flag          VERSION            =new Flag('x', "version");        // show version info
   final public static String SNAPSHOT           ="snapshot";
   final public static String DOT_SNAPSHOTS      =".snapshots";
   final public static String AT_SNAPSHOTS       ="@snapshots";
   public final static Flag   SINGLESNAPSHOT     =new Flag('s', "singlesnapshot"); // make one s
   public final static Flag   DELETEOLD          =new Flag('o', "deleteold");      // delete older s
   public final static Flag   MINIMUMSNAPSHOTS   =new Flag('m', "keepminimum");    // keep at least
   public static void main(String[] args) {
      Flag.setArgs(args, "sudo:/" + DOT_SNAPSHOTS + " sudo:/mnt/BACKUP/" + AT_SNAPSHOTS + "/manjaro18");
      StringBuilder sb=new StringBuilder("args > ");
      for (String s:args)
         sb.append(" ").append(s);
      System.out.println(sb);
      if (VERSION.get()) {
         System.out.println("BackSnap Version 0.4.1  (2023/01/14)");
         System.exit(0);
      }
      if (DRYRUN.get())
         System.out.println("Doing a dry run ! ");
      // Parameter sammeln für SOURCE
      String source=Flag.getParameterOrDefault(0, "sudo:/" + DOT_SNAPSHOTS);
      String srcSsh=source.contains(":") ? source.substring(0, source.indexOf(":")) : "";
      Path   srcDir=Path.of("/", srcSsh.isBlank() ? source : source.substring(srcSsh.length() + 1));
      if (srcSsh.startsWith("sudo"))
         srcSsh="sudo ";
      if (srcDir.endsWith(DOT_SNAPSHOTS))
         srcDir=srcDir.getParent();
      try {
         SubVolumeList    srcSubVolumes=new SubVolumeList(srcSsh);
         List<SnapConfig> snapConfigs  =SnapConfig.getList(srcSubVolumes);
         SnapConfig       srcConfig    =SnapConfig.getConfig(snapConfigs, srcDir);
         if (srcConfig == null)
            throw new RuntimeException("Could not find srcDir: " + srcDir);
         if (srcConfig.original().btrfsMap().isEmpty())
            throw new RuntimeException("Ingnoring, because there are no snapshots in: " + srcDir);
         System.out.println("Backup snapshots from: " + srcConfig.original().keyM());
         // BackupVolume ermitteln
         String backup   =Flag.getParameterOrDefault(1, "@BackSnap");
         String backupSsh=backup.contains(":") ? backup.substring(0, backup.indexOf(":")) : "";
         String backupDir=backupSsh.isBlank() ? backup : backup.substring(backupSsh.length() + 1);
         if (backupSsh.startsWith("sudo"))
            backupSsh="sudo ";
         boolean       samePC          =(backupSsh.equals(srcSsh));
         SubVolumeList backupSubVolumes=samePC ? srcSubVolumes : new SubVolumeList(backupSsh);
         String        backupKey       =backupSsh + ":" + backupDir;
         Mount         backupVolume    =backupSubVolumes.getBackupVolume(backupKey);
         if (backupVolume == null)
            throw new RuntimeException("Could not find backupDir: " + backupDir);
         if (backupVolume.devicePath().equals(srcConfig.original().devicePath()) && samePC)
            throw new RuntimeException("Backup not possible onto same device: " + backupDir + " <= " + srcDir);
         System.out.println("Try to use backupDir : " + backupVolume.keyM());
         SnapTree backupTree=SnapTree.getSnapTree(backupVolume/* , backupVolume.mountPoint(), backupSsh */);
         if (GUI.get()) {
            bs=new BacksnapGui();
            BacksnapGui.setGui(bs);
            BacksnapGui.main2(args);
            bs.setSrc(srcConfig);
            bs.setBackup(backupTree, backupDir);
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
         for (Snapshot sourceSnapshot:srcConfig.original().otimeMap().values()) {
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
               // ende("A");
               out.print(".");
               if (!backup(sourceSnapshot, srcConfig.kopie(), backupTree, backupDir, srcSsh, backupSsh, snapConfigs))
                  continue;
               if (GUI.get())
                  refreshGUI(backupVolume, backupDir, backupSsh);
               if (SINGLESNAPSHOT.get())// nur einen Snapshot übertragen
                  break;
            } catch (NullPointerException n) {
               n.printStackTrace();
               break;
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
         ende("Xabbruch");
         System.exit(-1);
      }
      ende("X");
   }
   /**
    * @param backupVolume
    * @param backupDir
    * @param backupSsh
    * @throws IOException
    * 
    */
   private static void refreshGUI(Mount backupVolume, String backupDir, String backupSsh) throws IOException {
      String extern    =backupVolume.mountList().extern();
      Path   devicePath=backupVolume.devicePath();
      String cacheKey  =extern + ":" + devicePath;
      Commandline.removeFromCache(cacheKey);
      SnapTree backupTree=new SnapTree(backupVolume);// umgeht den cache
      bs.setBackup(backupTree, backupDir);
      refreshGUIcKey=cacheKey;
      refreshBackupVolume=backupVolume;
      refreshBackupDir=backupDir;
   }
   public static void refreshGUI() throws IOException {
      if (refreshGUIcKey == null)
         return;
      Commandline.removeFromCache(refreshGUIcKey);
      SnapTree backupTree=new SnapTree(refreshBackupVolume);// umgeht den cache
      bs.setBackup(backupTree, refreshBackupDir);
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
            String srcSsh, String backupSsh, List<SnapConfig> snapConfigs) throws IOException {
      if (srcSnapshot.isBackup()) {
         err.println("Überspringe backup vom backup: " + srcSnapshot.dirName());
         return false;
      }
      if (backupMap.rUuidMap().containsKey(srcSnapshot.uuid())) {
         out.println("Überspringe bereits vorhandenen Snapshot: " + srcSnapshot.dirName());
         parentSnapshot=srcSnapshot;
         return false;
      }
      Path sDir=srcSnapshot.getPathOn(srcVolume.mountPath(), snapConfigs);
      if (sDir == null)
         throw new FileNotFoundException("Could not find dir: " + srcVolume);
      Path bDir=Paths.get(backupDir, srcSnapshot.dirName());
      out.print("Backup of " + srcSnapshot.dirName());
      if (parentSnapshot != null) // @todo genauer prüfen
         out.println(" based on " + parentSnapshot.dirName());
      mkDirs(bDir, backupSsh);
      rsyncFiles(srcSsh, backupSsh, sDir, bDir);
      sendBtrfs(srcVolume, srcSsh, backupSsh, sDir, bDir, snapConfigs);
      parentSnapshot=srcSnapshot;
      // ende("Xstop");
      // System.exit(-11);
      return true;
   }
   private static void sendBtrfs(Mount srcVolume, String srcSsh, String backupSsh, Path sDir, Path bDir,
            List<SnapConfig> snapConfigs) throws IOException {
      boolean       sameSsh =(srcSsh.contains("@") && srcSsh.equals(backupSsh));
      StringBuilder send_cmd=new StringBuilder("/bin/btrfs send ");
      if (parentSnapshot != null) // @todo genauer prüfen
         send_cmd.append("-p ").append(parentSnapshot.getPathOn(srcVolume.mountPath(), snapConfigs)).append(" ");
      out.println();
      send_cmd.append(sDir);
      if (!sameSsh)
         if (srcSsh.contains("@"))
            send_cmd.insert(0, "ssh " + srcSsh + " '").append("'");
         else
            send_cmd.insert(0, srcSsh);
      if (usePv)
         send_cmd.append("|/bin/pv -f");
      send_cmd.append("|");
      if (!sameSsh)
         if (backupSsh.contains("@"))
            send_cmd.append("ssh " + backupSsh + " '");
         else
            send_cmd.append(backupSsh);
      send_cmd.append("/bin/btrfs receive ").append(bDir).append(";/bin/sync");
      if (sameSsh)
         if (srcSsh.contains("@"))
            send_cmd.insert(0, "ssh " + srcSsh + " '");
      // else
      // send_cmd.insert(0, srcSsh); // @todo für einzelnes sudo anpassen ?
      if (backupSsh.contains("@"))
         send_cmd.append("'");
      out.println(send_cmd);
      if (!DRYRUN.get())
         try (CmdStream btrfs_send=Commandline.executeCached(send_cmd, null)) {
            task=Commandline.background.submit(() -> btrfs_send.err().forEach(line -> {
               if (line.contains("ERROR: cannot find parent subvolume"))
                  Backsnap.canNotFindParent=Backsnap.parentKey;
               if (line.contains("No route to host") || line.contains("Connection closed")
                        || line.contains("connection unexpectedly closed"))
                  Backsnap.connectionLost=10;
               if (line.contains("<=>")) { // from pv
                  err.print(line);
                  if (Backsnap.lastLine == 0)
                     err.print("\n");
                  else
                     err.print("\r");
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
               }
            }));
            btrfs_send.erg().forEach(line -> {
               if (lastLine != 0) {
                  lastLine=0;
                  out.println();
               }
               out.println();
            });
         } // ende("S");// B
   }
   private static void rsyncFiles(String srcSsh, String backupSsh, Path sDir, Path bDir) throws IOException {
      StringBuilder copyCmd=new StringBuilder("/bin/rsync -vcptgo --exclude \"" + SNAPSHOT + "\" ");
      if (DRYRUN.get())
         copyCmd.append("--dry-run ");
      if (!srcSsh.equals(backupSsh))
         if (srcSsh.contains("@"))
            copyCmd.append(srcSsh).append(":");
      copyCmd.append(sDir.getParent()).append("/* ").append(bDir).append("/");
      if (srcSsh.equals(backupSsh))
         if (srcSsh.contains("@"))
            copyCmd.insert(0, "ssh " + srcSsh + " '").append("'"); // gesamten Befehl senden ;-)
         else
            copyCmd.insert(0, srcSsh); // nur sudo, kein quoting !
      out.println(copyCmd.toString());// if (!DRYRUN.get())
      try (CmdStream rsync=Commandline.executeCached(copyCmd.toString(), null)) { // not cached
         rsync.backgroundErr();
         rsync.erg().forEach(out::println);
         for (String line:rsync.errList())
            if (line.contains("No route to host") || line.contains("Connection closed")
                     || line.contains("connection unexpectedly closed")) {
               Backsnap.connectionLost=10;
               break;
            } // ende("");// R
      }
   }
   public static void removeSnapshot(Snapshot s) throws IOException {
      StringBuilder remove_cmd=new StringBuilder("/bin/btrfs subvolume delete -Cv ");
      remove_cmd.append(s.mount().mountPath());
      remove_cmd.append(s.btrfsPath());
      if ((s.mount().mountList().extern() instanceof String x) && (!x.isBlank()))
         if (x.startsWith("sudo "))
            remove_cmd.insert(0, x);
         else
            remove_cmd.insert(0, "ssh " + x + " '").append("'");
      out.println();
      out.print(remove_cmd);
      // if (!DRYRUN.get())
      try (CmdStream remove_snap=Commandline.executeCached(remove_cmd, null)) {
         remove_snap.backgroundErr();
         remove_snap.erg().forEach(line -> {
            if (lastLine != 0) {
               lastLine=0;
               out.println();
            }
            out.println();
         });
         remove_snap.waitFor();
         out.println(" # ");
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
         System.out.println("mkdir:" + d);
         if (DRYRUN.get())
            return;
         if (backupSsh.isBlank())
            if (d.toFile().mkdirs())
               return; // erst mit sudo, dann noch mal mit localhost probieren
         StringBuilder mkdirCmd=new StringBuilder("mkdir -pv ").append(d);
         if (backupSsh.contains("@"))
            mkdirCmd.insert(0, "ssh " + backupSsh + " '").append("'");
         else
            mkdirCmd.insert(0, backupSsh);
         try (CmdStream mkdir=Commandline.executeCached(mkdirCmd, null)) {
            mkdir.backgroundErr();
            if (mkdir.erg().peek(System.out::println).anyMatch(Pattern.compile("mkdir").asPredicate()))
               return;
            if (mkdir.errList().isEmpty())
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
      out.print("ende:");
      if (task != null)
         try {
            task.get(10, TimeUnit.SECONDS);
         } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
         }
      out.print(t);
      if (t.startsWith("X")) {
         out.print(" ready");
         if (GUI.get()) {
            while (bs != null) {
               if (bs.frame == null)
                  break;
            }
         }
         out.print(" to");
         Commandline.background.shutdown();
         out.print(" exit");
         Commandline.cleanup();
         out.print(" java");
      }
      out.println();
   }
}
