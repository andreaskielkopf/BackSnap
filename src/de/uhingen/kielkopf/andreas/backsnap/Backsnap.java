package de.uhingen.kielkopf.andreas.backsnap;

import static java.lang.System.err;
import static java.lang.System.out;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;
import de.uhingen.kielkopf.andreas.beans.cli.Flag;

public class Backsnap {
   static String          parentKey       =null;
   private static boolean usePv           =false;
   static int             lastLine        =0;
   static String          canNotFindParent=null;
   static int             connectionLost  =0;
   static Future<?>       task            =null;
   private static String  SNAPSHOT        ="snapshot";
   private static Flag    GUI             =new Flag('g', "gui");
   public static void main(String[] args) {
      Flag.setArgs(args, "");// Parameter sammeln
      final String source   =Flag.getParameterOrDefault(0, "/.snapshots");
      final String destDir  =Flag.getParameterOrDefault(1, "/mnt/BACKUP/@snapshots/manjaro");
      final String externSsh=source.contains(":") ? source.substring(0, source.indexOf(":")) : "";
      final String sourceDir=externSsh.isBlank() ? source : source.substring(externSsh.length() + 1);
      if (GUI.get()) {
         BacksnapGui.main(args);
      } else {
         out.println("Backup Snapshots from " + (externSsh.isBlank() ? "" : externSsh + ":") + sourceDir + " to "
                  + destDir + " ");
         try {
            usePv=Paths.get("/bin/pv").toFile().canExecute();
         } catch (Exception e1) {/**/}
         /// Alle Snapshots einzeln sichern
         try {
            // SnapTree sfTree=new SnapTree(sourceDir, externSsh);
            TreeMap<String, String> sfMap=getMap(sourceDir, externSsh);
            TreeMap<String, String> dfMap=getMap(destDir, "");
            if (connectionLost > 0) {
               err.println("no SSH Connection");
               ende("X");
               System.exit(0);
            }
            for (String sourceKey:sfMap.keySet()) {
               if (canNotFindParent != null) {
                  err.println("Please remove " + destDir + "/" + canNotFindParent + "/" + SNAPSHOT + " !");
                  ende("X");
                  System.exit(-9);
               } else
                  if (connectionLost > 3) {
                     err.println("SSH Connection lost !");
                     ende("X");
                     System.exit(-8);
                  }
               try {
                  ende("A");
                  out.print(".");
                  if (!backup(sourceDir, sourceKey, sfMap, dfMap, destDir, externSsh))
                     continue;
               } catch (NullPointerException n) {
                  n.printStackTrace();
                  break;
               }
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      ende("X");
   }
   /**
    * Versuchen genau diesen einzelnen Snapshot zu sichern
    * 
    * @param sourceKey
    * @param sMap
    * @param dMap
    * @throws IOException
    */
   private static boolean backup(String sourceDir, String sourceKey, TreeMap<String, String> sMap,
            TreeMap<String, String> dMap, final String destDir, String externSsh) throws IOException {
      String  sourceName  =sMap.get(sourceKey);
      boolean existAlready=false;
      if (dMap.containsKey(sourceKey)) {
         Path p=Paths.get(destDir, dMap.get(sourceKey), SNAPSHOT);
         if (Files.isDirectory(p))
            existAlready=true;
      }
      if (existAlready) {
         parentKey=sourceKey;
         return false;
      }
      if (!dMap.containsKey(sourceKey))
         if (!Paths.get(destDir, sourceName).toFile().mkdirs())
            throw new FileNotFoundException("Could not create dir: " + destDir + "/" + sourceName);
      out.print("Backup of " + sourceName);
      StringBuilder send_cmd=new StringBuilder();
      if (!externSsh.isBlank()) {
         send_cmd.append("ssh ");
         send_cmd.append(externSsh);
         send_cmd.append(" \"");
      }
      send_cmd.append("/bin/btrfs send ");
      if (parentKey != null) {
         out.print(" based on " + parentKey);
         send_cmd.append("-p ");
         send_cmd.append(Paths.get(sourceDir, sMap.get(parentKey), SNAPSHOT));
         send_cmd.append(" ");
      }
      out.println();
      send_cmd.append(Paths.get(sourceDir, sMap.get(sourceKey), SNAPSHOT));
      // cmd.append(" ");
      if (!externSsh.isBlank())
         send_cmd.append("\"");
      if (usePv)
         send_cmd.append("|/bin/pv -f");
      send_cmd.append("|/bin/btrfs receive ");
      send_cmd.append(Paths.get(destDir, sourceName).toFile().getPath()); // ???
      send_cmd.append(";/bin/sync");
      out.println(send_cmd);
      CmdStream btrfs_send=Commandline.execute(send_cmd.toString());
      task=background.submit(() -> btrfs_send.err().forEach(line -> {
         if (line.contains("ERROR: cannot find parent subvolume"))
            Backsnap.canNotFindParent=Backsnap.parentKey;
         if (line.contains("Connection closed") || line.contains("connection unexpectedly closed"))
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
      ende("");// B
      StringBuilder copy_cmd=new StringBuilder("rsync -vcptgo --exclude '" + SNAPSHOT + "' ");
      if (!externSsh.isBlank()) {
         copy_cmd.append(externSsh);
         copy_cmd.append(":");
      }
      Path pc=Paths.get(sourceDir, sMap.get(sourceKey));
      copy_cmd.append(pc);
      copy_cmd.append("/* ");
      copy_cmd.append(Paths.get(destDir, sourceName).toFile().getPath());
      copy_cmd.append("/");
      out.print(copy_cmd.toString());
      CmdStream rsync=Commandline.execute(copy_cmd.toString());
      task=background.submit(() -> rsync.err().forEach(line -> {
         if (line.contains("Connection closed") || line.contains("connection unexpectedly closed"))
            Backsnap.connectionLost=10;
         err.println(line);
      }));
      rsync.erg().forEach(out::println);
      ende("");// R
      parentKey=sourceKey;
      return true;
   }
   private static final Pattern numericDirname=Pattern.compile("^[0-9]+$");
   final static ExecutorService background    =Executors.newCachedThreadPool();
   /**
    * Hole ein Verzeichniss in die Map
    * 
    * @param dirName
    * @param extern
    * @return
    * @throws IOException
    */
   private final static TreeMap<String, String> getMap(final String dirName, final String extern) throws IOException {
      TreeMap<String, String> fileMap=new TreeMap<>();
      StringBuilder           cmd    =new StringBuilder();
      if (!extern.isBlank()) {
         cmd.append("ssh ");
         cmd.append(extern);
         cmd.append(" '");
      }
      cmd.append("/bin/ls ");
      cmd.append(dirName);
      if (!extern.isBlank())
         cmd.append("'");
      Commandline.CmdStream std=Commandline.execute(cmd.toString());
      // Error handling in background
      task=background.submit(() -> std.err().forEach(line -> {
         if (line.contains("Connection closed") || line.contains("connection unexpectedly closed"))
            Backsnap.connectionLost=10;
         err.println(line);
      }));
      std.erg().forEachOrdered(file -> { // if (file.isDirectory()) {// später prüfen
         if (numericDirname.matcher(file).matches()) {
            out.print(file + " ");
            String s=".".repeat(10).concat(file); // ??? numerisch sortieren ;-)
            s=s.substring(s.length() - 10);
            fileMap.put(s, file);
         } else {
            err.println();
            err.print(file);
         }
      });
      ende("");// T
      out.println();
      return fileMap;
   }
   /**
    * prozesse aufräumen
    * 
    * @param t
    */
   private final static void ende(String t) {
      if (task != null)
         try {
            task.get(10, TimeUnit.SECONDS);
         } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
         }
      // for (Process process2:Commandline.processList) {
      // if (process2.isAlive()) {
      // out.print(t.toUpperCase());
      // try {
      // process2.waitFor(5, TimeUnit.SECONDS);
      // } catch (InterruptedException e) {/**/ }
      // out.print(t.toLowerCase());
      // }
      // }
      if (t.startsWith("X")) {
         out.print(" ready");
         background.shutdown();
         out.print(" to");
         out.print(" exit");
         background.shutdownNow();
         out.println(" java");
      }
   }
}
