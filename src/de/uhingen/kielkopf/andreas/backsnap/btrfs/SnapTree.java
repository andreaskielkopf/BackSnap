/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;

/**
 * Alle Snapshots eines bestimmten Subvolumes sortiert in einem Tree
 * 
 * @author Andreas Kielkopf
 */
public record SnapTree(String dirName, String extern, TreeMap<String, Snapshot> fileMap) {
   /**
    * Hole ein Verzeichniss in die Map
    * 
    * @param dirName
    * @param extern
    */
   public SnapTree(String dirName, String extern) {
      this(dirName, extern, new TreeMap<>());
      populate();
   }
   // private fileMap=new TreeMap<>();
   private void populate() {// otime kommt nur bei snapshots
      StringBuilder btrfsCmd=new StringBuilder("btrfs subvolume list -spcguqR ").append(dirName);
      if ((extern instanceof String x) && (!x.isBlank()))
         if (x.startsWith("sudo "))
            btrfsCmd.insert(0, x);
         else
            btrfsCmd.insert(0, "ssh " + x + " '").append("'");
      System.out.println(btrfsCmd);
      // cmd.append("/bin/ls "); // cmd.append(dirName); // if (!extern.isBlank())
      try (CmdStream snapshotList=Commandline.execute(btrfsCmd)) {
         Future<?> task=Commandline.background.submit(() -> {// Some Error handling in background
            if (snapshotList.err().peek(System.err::println).anyMatch(line -> {
               return line.contains("No route to host") || line.contains("Connection closed")
                        || line.contains("connection unexpectedly closed");
            }))
               throw new IOException("connection unexpectedly closed");
            return "";
         });
         snapshotList.erg().forEachOrdered(line -> { // if (file.isDirectory()) {// später prüfen
            Snapshot snapshot=new Snapshot(line);
            // if (snapshot.key().startsWith("§")) System.err.print("\n" + snapshot.key() + " -> " + line); else
            fileMap.put(snapshot.path().toString(), snapshot);
         });
         task.get(); // ende("");// T
         // out.println();
      } catch (IOException | ExecutionException | InterruptedException e) {
         e.printStackTrace();
      }
   }
}