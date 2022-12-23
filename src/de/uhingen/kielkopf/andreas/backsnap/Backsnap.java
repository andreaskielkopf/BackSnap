package de.uhingen.kielkopf.andreas.backsnap;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Backsnap {
   /**
    * 
    */
   final static ProcessBuilder  processBuilder  =new ProcessBuilder();
   static String                parentKey       =null;
   private static boolean       usePv           =false;
   static int                   lastLine        =0;
   private static List<Process> processList     =new CopyOnWriteArrayList<>();
   private static String        canNotFindParent=null;
   private static int           connectionLost  =0;
   private static Future<?>     task            =null;
   public static void main(String[] args) {
      // Parameter sammeln
      final String defaultDest="/mnt/@snapshots/manjaro";
      final String source     =(args.length > 0) ? args[0] : "/.snapshots";
      final String destDir    =(args.length > 1) ? args[1] : defaultDest;
      final String externSsh  =source.contains(":") ? source.substring(0, source.indexOf(":")) : "";
      final String sourceDir  =externSsh.isBlank() ? source : source.substring(externSsh.length() + 1);
      System.out.println("Backup Snapshots from " + (externSsh.isBlank() ? "" : externSsh + ":") + sourceDir + " to "
               + destDir + " ");
      try {
         usePv=Paths.get("/bin/pv").toFile().canExecute();
      } catch (Exception e1) {/**/}
      /// Alle Snapshots einzeln sichern
      try {
         TreeMap<String, String> sfMap=getMap(sourceDir, externSsh);
         TreeMap<String, String> dfMap=getMap(destDir, "");
         if (connectionLost > 0) {
            System.err.println("no SSH Connection");
            ende("X");
            System.exit(0);
         }
         for (String sourceKey:sfMap.keySet()) {
            if (canNotFindParent != null) {
               System.err.println("Please remove " + destDir + "/" + canNotFindParent + "/snapshot !");
               ende("X");
               System.exit(-9);
            } else
               if (connectionLost > 3) {
                  System.err.println("SSH Connection lost !");
                  ende("X");
                  System.exit(-8);
               }
            try {
               ende("A");
               System.out.print(".");
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
         Path p=Paths.get(destDir, dMap.get(sourceKey), "snapshot");
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
      System.out.print("Backup of " + sourceName);
      StringBuilder cmd=new StringBuilder();
      if (!externSsh.isBlank()) {
         cmd.append("ssh ");
         cmd.append(externSsh);
         cmd.append(" \"");
      }
      cmd.append("/bin/btrfs send ");
      if (parentKey != null) {
         System.out.print(" based on " + parentKey);
         cmd.append("-p ");
         cmd.append(Paths.get(sourceDir, sMap.get(parentKey), "snapshot"));
         cmd.append(" ");
      }
      System.out.println();
      cmd.append(Paths.get(sourceDir, sMap.get(sourceKey), "snapshot"));
      // cmd.append(" ");
      if (!externSsh.isBlank())
         cmd.append("\"");
      if (usePv)
         cmd.append("|/bin/pv -f");
      cmd.append("|/bin/btrfs receive ");
      cmd.append(Paths.get(destDir, sourceName).toFile().getPath()); // ???
      cmd.append(";/bin/sync");
      System.out.println(cmd);
      execute(cmd.toString()).forEach(line -> {
         if (lastLine != 0) {
            lastLine=0;
            System.out.println();
         }
         System.out.println();
      });
      ende("");// B
      StringBuilder cp=new StringBuilder("rsync -vcptgo --exclude 'snapshot' ");
      if (!externSsh.isBlank()) {
         cp.append(externSsh);
         cp.append(":");
      }
      Path pc=Paths.get(sourceDir, sMap.get(sourceKey));
      cp.append(pc);
      cp.append("/* ");
      cp.append(Paths.get(destDir, sourceName).toFile().getPath());
      cp.append("/");
      System.out.print(cp.toString());
      execute(cp.toString()).forEach(System.out::println);
      ende("");// R
      parentKey=sourceKey;
      return true;
   }
   private final static ExecutorService background=Executors.newCachedThreadPool();
   private static final String          UTF_8     ="UTF-8";
   /**
    * Einen Befehl ausführen, Fehlermeldungen direkt ausgeben, stdout als stream zurückgeben
    * 
    * @param cmd
    * @return Stream<String>
    * @throws IOException
    */
   private static Stream<String> execute(String cmd) throws IOException {
      Process process=processBuilder.command(List.of("/bin/bash", "-c", cmd)).start();
      processList.add(process);
      BufferedReader stdErr=new BufferedReader(new InputStreamReader(process.getErrorStream(), UTF_8));
      BufferedReader stdOut=new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8));
      task=background.submit(() -> stdErr.lines().forEach(line -> {
         if (line.contains("ERROR: cannot find parent subvolume"))
            canNotFindParent=parentKey;
         if (line.contains("Connection closed") || line.contains("connection unexpectedly closed"))
            connectionLost=10;
         if (line.contains("<=>")) {
            System.err.print(line);
            if (lastLine == 0)
               System.err.print("\n");
            else
               System.err.print("\r");
            lastLine=line.length();
            if (line.contains(":00 ")) {
               System.err.print("\n");
               connectionLost=0;
            }
            if (line.contains("0,00 B/s")) {
               System.err.println();
               System.err.println("HipCup");
               connectionLost++;
            }
         } else {
            if (lastLine != 0) {
               lastLine=0;
               System.err.println();
            }
            System.err.println(line);
         }
      }));
      return stdOut.lines(); // collect all Lines into a stream
   }
   private static final Pattern numericDirname=Pattern.compile("^[0-9]+$");
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
      execute(cmd.toString()).forEachOrdered(file -> { // if (file.isDirectory()) {// später prüfen
         if (numericDirname.matcher(file).matches()) {
            System.out.print(file + " ");
            String s=".".repeat(10).concat(file); // ??? numerisch sortieren ;-)
            s=s.substring(s.length() - 10);
            fileMap.put(s, file);
         } else {
            System.err.println();
            System.err.print(file);
         }
      });
      ende("");// T
      System.out.println();
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
      for (Process process2:processList) {
         if (process2.isAlive()) {
            System.out.print(t.toUpperCase());
            try {
               process2.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {/**/ }
            System.out.print(t.toLowerCase());
         }
      }
      if (t.startsWith("X")) {
         System.out.print(" ready");
         background.shutdown();
         System.out.print(" to");
         System.out.print(" exit");
         background.shutdownNow();
         System.out.println(" java");
      }
   }
}
