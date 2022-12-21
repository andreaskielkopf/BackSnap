package de.uhingen.kielkopf.andreas.backsnap;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Backsnap {
   static String               parentKey=null;
   final static ProcessBuilder pb       =new ProcessBuilder();
   public static void main(String[] args) {
      // Parameter sammeln
      // final Map<String, String> env =System.getenv();
      // final List<String> erg =execute("echo \"$HOST$HOSTNAME\"");
      // final String hostname =env.getOrDefault("BS_HOST",
      // ((erg != null) && !erg.isEmpty()) ? erg.get(0) : "manjaro");
      // final String mountpoint =env.getOrDefault("BS_MOUNT", "/mnt/BACKUP");
      // final String defaultDest=mountpoint + "/@snapshots/" + hostname;
      // final String uuid =env.getOrDefault("BS_UUID", "03417033-3745-4ae7-9451-efafcbb9124e");
      // boolean wasMounted=mount(uuid, mountpoint);
      final String defaultDest="/mnt/@snapshots/manjaro";
      final String source     =(args.length > 0) ? args[0] : "/.snapshots";
      final String destDir    =(args.length > 1) ? args[1] : defaultDest;
      final String externSsh  =source.contains(":") ? source.substring(0, source.indexOf(":")) : "";
      final String sourceDir  =externSsh.isBlank() ? source : source.substring(externSsh.length() + 1);
      System.out.println("Backup Snapshots from " + (externSsh.isBlank() ? "" : externSsh + ":") + sourceDir + " to "
               + destDir + " ");
      /// Alle Snapshots einzeln sichern
      try {
         TreeMap<String, String> sfMap=getMap(sourceDir, externSsh);
         TreeMap<String, String> dfMap=getMap(destDir, "");
         for (String sourceKey:sfMap.keySet()) {
            try {
               if (!backup(sourceDir, sourceKey, sfMap, dfMap, destDir, externSsh))
                  continue;
            } catch (NullPointerException n) {
               n.printStackTrace();
               break;
            }
         }
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      }
      // if (!wasMounted)
      // execute("umount " + mountpoint);
   }
   /**
    * Versuchen genau diesen einzelnen Snapshot zu sichern
    * 
    * @param sourceKey
    * @param sMap
    * @param dMap
    * @throws FileNotFoundException
    */
   static boolean backup(String sourceDir, String sourceKey, TreeMap<String, String> sMap, TreeMap<String, String> dMap,
            final String destDir, String externSsh) throws FileNotFoundException {
      String  sourceName  =sMap.get(sourceKey);
      boolean existAlready=false;
      if (dMap.containsKey(sourceKey)) {
         // String dn=source + "/" + dfMap.get(sourceName) + "/snapshot";
         Path p=Paths.get(destDir, dMap.get(sourceKey), "snapshot");
         if (Files.isDirectory(p))
            existAlready=true;
      }
      if (existAlready) {
         // StringBuilder cp=new StringBuilder("rsync -vcptgo --exclude 'snapshot' ");
         // cp.append(sfMap.get(sourceName).getPath());
         // cp.append("/* ");
         // cp.append(Paths.get(dest, name).toFile().getPath());
         // cp.append("/");
         // System.out.println(cp.toString());
         // execute(cp.toString());
         parentKey=sourceKey;
         return false;
      }
      if (!dMap.containsKey(sourceKey))
         if (!Paths.get(destDir, sourceName).toFile().mkdirs())
            throw new FileNotFoundException("Could not create dir: " + sourceName);
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
      cmd.append(" ");
      if (!externSsh.isBlank())
         cmd.append("\" ");
      if (Paths.get("/bin/pv").toFile().canExecute())
         cmd.append("| /bin/pv -f ");
      cmd.append("| /bin/btrfs receive ");
      cmd.append(Paths.get(destDir, sourceName).toFile().getPath()); // ???
      cmd.append(" ; sync");
      List<String> erg=execute(cmd.toString());
      if (erg != null)
         for (String s:erg)
            System.out.println(s);
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
      System.out.println(cp.toString());
      erg=execute(cp.toString());
      if (erg != null)
         for (String s:erg)
            System.out.println(s);
      parentKey=sourceKey;
      return true;
   }
   /**
    * Einen Befehl ausführen
    * 
    * @param cmd
    * @return
    */
   static List<String> execute(String cmd) {
      try {
         final ArrayList<String> command=new ArrayList<>();
         command.add("/bin/bash");
         command.add("-c");
         command.add(cmd);
         try (BufferedReader br=new BufferedReader(
                  new InputStreamReader(pb.command(command).redirectErrorStream(true).start().getInputStream()))) {
            return br.lines().toList();
         }
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return null;
   }
   /**
    * Hole ein Verzeichniss in die Map
    * 
    * @param name
    * @param extern
    * @return
    * @throws FileNotFoundException
    */
   final static TreeMap<String, String> getMap(final String name, final String extern) throws FileNotFoundException {
      TreeMap<String, String> fileMap=new TreeMap<>();
      if (!extern.isBlank()) {
         List<String> e=execute("ssh " + extern + " 'ls " + name + "'");
         for (String file:e) { // if (file.isDirectory()) {// später prüfen
            System.out.print(file + " ");
            String s=".".repeat(10).concat(file); // ??? numerisch sortieren ;-)
            s=s.substring(s.length() - 10);
            fileMap.put(s, file);
         }
      } else {
         File dir=Paths.get(name).toFile();
         if (!dir.isDirectory())
            throw new FileNotFoundException(name + "<-(is no directory)");
         for (File file:dir.listFiles())
            if (file.isDirectory()) {
               System.out.print(file.getName() + " ");
               String s=".".repeat(10).concat(file.getName()); // ??? numerisch sortieren ;-)
               s=s.substring(s.length() - 10);
               fileMap.put(s, file.getName());
            }
      }
      System.out.println();
      return fileMap;
   }
   /**
    * Hole ein Verzeichniss in die Map
    * 
    * @param name
    * @param extern
    * @return
    * @throws FileNotFoundException
    */
   final static TreeMap<String, File> getMap(final String name) throws FileNotFoundException {
      File dir=Paths.get(name).toFile();
      if (!dir.isDirectory())
         throw new FileNotFoundException(name);
      TreeMap<String, File> fileMap=new TreeMap<>();
      for (File file:dir.listFiles())
         if (file.isDirectory()) {
            System.out.print(file.getName() + " ");
            String s=".".repeat(10).concat(file.getName());
            fileMap.put(s.substring(s.length() - 10), file);
         }
      System.out.println();
      return fileMap;
   }
   /**
    * Testet ob das Backup-Volume gemountet ist
    * 
    * @param mountpoint
    * @return
    */
   private static boolean isMounted(String mountpoint) {
      List<String> erg=execute("mount |grep -E btrfs");
      if (erg == null)
         return false;
      String mp=" " + mountpoint + " ";
      for (String s:erg)
         if (s.contains(mp))
            return true;
      return false;
   }
   /**
    * Mountet das Backup-Volume per fstab oder per UUID
    * 
    * @param uuid
    * @param mountpoint
    * @return
    */
   private static boolean mount(String uuid, String mountpoint) {
      if (isMounted(mountpoint))
         return true;
      if (uuid.length() < 20)
         execute("mount " + mountpoint); // per fstab mounten
      else { // mount UUID=03417033-3745-4ae7-9451-efafcbb9124e -o noatime,subvol=/,compress=zstd:9 /mnt/BACKUP
         String btrfs  ="-t btrfs";
         String options="-o noatime,subvol=/,compress=zstd:9";
         execute("mount " + btrfs + " " + options + " UUID=" + uuid + " " + mountpoint);
      }
      if (!isMounted(mountpoint))
         throw new UnsupportedOperationException("Mount failed for UUID='" + uuid + "' at '" + mountpoint + "'");
      return false;
   }
}
