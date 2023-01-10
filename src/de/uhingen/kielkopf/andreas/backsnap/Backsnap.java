package de.uhingen.kielkopf.andreas.backsnap;

import static java.lang.System.err;
import static java.lang.System.out;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;
import de.uhingen.kielkopf.andreas.backsnap.btrfs.*;
import de.uhingen.kielkopf.andreas.beans.cli.Flag;

public class Backsnap {
   static String              parentKey       =null;
   private static Snapshot    parentSnapshot  =null;
   private static boolean     usePv           =false;
   static int                 lastLine        =0;
   static String              canNotFindParent=null;
   static int                 connectionLost  =0;
   static Future<?>           task            =null;
   final public static String SNAPSHOT        ="snapshot";
   // final private static String SNAPSHOTS ="snapshots";
   // final private static String DOT_SNAPSHOT =".snapshot";
   final public static String DOT_SNAPSHOTS   =".snapshots";
   final public static String AT_SNAPSHOTS    ="@snapshots";
   final static Flag          GUI             =new Flag('g', "gui");
   final static Flag          DRYRUN          =new Flag('d', "dryrun");
   final static Flag          VERBOSE         =new Flag('v', "verbose");
   // final static String srcSsH ="root@localhost";
   // final static String backupSsH =srcSsH;
   public static void main(String[] args) {
      StringBuilder sb=new StringBuilder("args > ");
      for (String s:args)
         sb.append(" ").append(s);
      System.out.println(sb);
      Flag.setArgs(args, "sudo:/" + DOT_SNAPSHOTS + " sudo:/mnt/BACKUP/" + AT_SNAPSHOTS + "/manjaro18");
      if (DRYRUN.get())
         System.out.println("Doing a dry run ! ");
      // Parameter sammeln
      String source=Flag.getParameterOrDefault(0, "sudo:/" + DOT_SNAPSHOTS);
      String srcSsh=source.contains(":") ? source.substring(0, source.indexOf(":")) : "";
      String srcDir=srcSsh.isBlank() ? source : source.substring(srcSsh.length() + 1);
      if (srcSsh.startsWith("sudo"))
         srcSsh="sudo ";
      if (srcDir.endsWith(DOT_SNAPSHOTS))
         srcDir=srcDir.substring(0, srcDir.length() - DOT_SNAPSHOTS.length());
      if ((srcDir.length() > 1) && srcDir.endsWith("/"))
         srcDir=srcDir.substring(0, srcDir.length() - 1);
      try {
         SubVolumeList srcSubVolumes=new SubVolumeList(srcSsh);
         Subvolume     srcVolume    =srcSubVolumes.subvTree().get(srcDir);
         if (srcVolume == null)
            throw new RuntimeException("Could not find srcDir: " + srcDir);
         if (srcVolume.snapshotTree().isEmpty())
            throw new RuntimeException("Ingnoring, because there are no snapshots in: " + srcDir);
         System.out.println("backup snapshots from: " + srcVolume.key());
         // BackupVolume ermitteln
         String backup   =Flag.getParameterOrDefault(1, "@BackSnap");
         String backupSsh=backup.contains(":") ? backup.substring(0, backup.indexOf(":")) : "";
         String backupDir=backupSsh.isBlank() ? backup : backup.substring(backupSsh.length() + 1);
         if (backupSsh.startsWith("sudo"))
            backupSsh="sudo ";
         SubVolumeList backupSubVolumes=new SubVolumeList(backupSsh);
         Subvolume     backupVolume    =backupSubVolumes.getBackupVolume(backupDir);
         if (backupVolume == null)
            throw new RuntimeException("Could not find backupDir: " + backupDir);
         if ((backupVolume.device().equals(srcVolume.device())) && backupSsh.equals(srcSsh))
            throw new RuntimeException("Backup not possible onto same device: " + backupDir + " <= " + srcDir);
         SnapTree backupTree=new SnapTree(backupVolume.mountPoint(), backupSsh);
         System.out.println("Will try to use backupDir: " + backupVolume.key());
         TreeMap<String, Snapshot> receivedSnapshots=new TreeMap<>();
         List<SnapConfig>          snapConfigs      =SnapConfig.getList(srcSubVolumes);
         for (Snapshot s:backupTree.fileMap().values())
            if (s.isBackup())
               receivedSnapshots.put(s.received_uuid(), s);
         // backupVolume.populate(backupTree);
         if (GUI.get()) {
            BacksnapGui bs=new BacksnapGui();
            BacksnapGui.setGui(bs);
            BacksnapGui.main(args);
            bs.setSrc(srcVolume);
            bs.setBackup(backupVolume, backupTree.fileMap(), backupDir);
         } else {
            out.println("Backup Snapshots from " + srcSsh + (srcSsh.contains("@") ? ":" : "") + srcDir + " to "
                     + backupDir + " ");
            try {
               usePv=Paths.get("/bin/pv").toFile().canExecute();
            } catch (Exception e1) {/**/}
            /// Alle Snapshots einzeln sichern
            if (connectionLost > 0) {
               err.println("no SSH Connection");
               ende("X");
               System.exit(0);
            }
            TreeMap<String, Snapshot> sortedSnapshots=new TreeMap<>();
            for (Snapshot s:srcVolume.snapshotTree().values())
               sortedSnapshots.put(s.key(), s);
            for (Snapshot sourceSnapshot:sortedSnapshots.values()) {// for (String sourceKey:sfMap.keySet()) {
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
                  if (!backup(sourceSnapshot, srcVolume, receivedSnapshots, backupDir, srcSsh, backupSsh, snapConfigs))
                     continue;
               } catch (NullPointerException n) {
                  n.printStackTrace();
                  break;
               }
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
    * Versuchen genau diesen einzelnen Snapshot zu sichern
    * 
    * @param snapConfigs
    * 
    * @param sourceKey
    * @param sMap
    * @param dMap
    * @throws IOException
    */
   private static boolean backup(Snapshot srcSnapshot, Subvolume srcVolume, TreeMap<String, Snapshot> receivedSnapshots,
            String backupDir, String srcSsh, String backupSsh, List<SnapConfig> snapConfigs) throws IOException {
      // String sourceName=srcSnapshot.path().toString();
      // boolean existAlready=false;
      String uuid=srcSnapshot.uuid();
      if (srcSnapshot.isBackup()) {
         err.println("Überspringe backup vom backup: " + srcSnapshot.dirName());
         return false; // Backups von backups verhindern
      }
      if (receivedSnapshots.containsKey(uuid)) { // Den Snapshot gibt es bereits -> überspringen
         out.println("Überspringe bereits vorhandenen Snapshot: " + srcSnapshot.dirName());
         parentSnapshot=srcSnapshot;
         return false;
      }
      // if (dMap.containsKey(sourceKey)) {
      // Path p=Paths.get(destDir, dMap.get(sourceKey), SNAPSHOT);
      // if (Files.isDirectory(p))
      // existAlready=true;
      // }
      // if (existAlready) {
      // parentKey=sourceKey;
      // return false;
      // }
      // if (!dMap.containsKey(sourceKey))
      Path sDir=srcSnapshot.getPathOn(srcVolume.mountPoint(), snapConfigs);
      Path bDir=Paths.get(backupDir, srcSnapshot.dirName());
      out.print("Backup of " + srcSnapshot.dirName());
      if (parentSnapshot != null) // @todo genauer prüfen
         out.println(" based on " + parentSnapshot.dirName());
      rsyncFiles(srcSsh, backupSsh, sDir, bDir);
      mkDirs(bDir, backupSsh);
      sendBtrfs(srcVolume, srcSsh, backupSsh, sDir, bDir, snapConfigs);
      parentSnapshot=srcSnapshot;
      // ende("Xstop");
      // System.exit(-11);
      return true;
   }
   private static void sendBtrfs(Subvolume srcVolume, String srcSsh, String backupSsh, Path sDir, Path bDir,
            List<SnapConfig> snapConfigs) throws IOException {
      boolean       sameSsh =(srcSsh.contains("@") && srcSsh.equals(backupSsh));
      StringBuilder send_cmd=new StringBuilder("/bin/btrfs send ");
      if (parentSnapshot != null) // @todo genauer prüfen
         send_cmd.append("-p ").append(parentSnapshot.getPathOn(srcVolume.mountPoint(), snapConfigs).resolve(SNAPSHOT))
                  .append(" ");
      out.println();
      send_cmd.append(sDir.resolve(SNAPSHOT));
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
         try (CmdStream btrfs_send=Commandline.execute(send_cmd)) {
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
         }
      // ende("S");// B
   }
   private static void rsyncFiles(String srcSsh, String backupSsh, Path sDir, Path bDir) throws IOException {
      StringBuilder copyCmd=new StringBuilder("/bin/rsync -vcptgo --exclude \"" + SNAPSHOT + "\" ");
      if (DRYRUN.get())
         copyCmd.append("--dry-run ");
      if (!srcSsh.equals(backupSsh))
         if (srcSsh.contains("@"))
            copyCmd.append(srcSsh).append(":");
      copyCmd.append(sDir).append("/* ").append(bDir).append("/");
      if (srcSsh.equals(backupSsh))
         if (srcSsh.contains("@"))
            copyCmd.insert(0, "ssh " + srcSsh + " '").append("'"); // gesamten Befehl senden ;-)
         else
            copyCmd.insert(0, srcSsh); // nur sudo, kein quoting !
      out.println(copyCmd.toString());
      // if (!DRYRUN.get())
      try (CmdStream rsync=Commandline.execute(copyCmd.toString())) {
         rsync.backgroundErr();
         rsync.erg().forEach(out::println);
         for (String line:rsync.errList())
            if (line.contains("No route to host") || line.contains("Connection closed")
                     || line.contains("connection unexpectedly closed")) {
               Backsnap.connectionLost=10;
               break;
            }
         // ende("");// R
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
         try (CmdStream mkdir=Commandline.execute(mkdirCmd)) {
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
         Commandline.background.shutdown();
         out.print(" to");
         out.print(" exit");
         Commandline.cleanup();
         out.print(" java");
      }
      out.println();
   }
}
