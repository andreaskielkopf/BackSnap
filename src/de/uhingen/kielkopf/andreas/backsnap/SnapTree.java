/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap;

import static java.lang.System.out;

import java.io.IOException;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
   private void populate() {
      StringBuilder btrfsCmd=new StringBuilder("btrfs subvolume list -spcguqR ");// otime kommt nur bei snapshots
      btrfsCmd.append(dirName);
      if ((extern instanceof String x) && (!x.isBlank()))
         btrfsCmd.insert(0, "ssh " + x + " '").append("'");
      System.out.println(btrfsCmd);
      // cmd.append("/bin/ls "); // cmd.append(dirName); // if (!extern.isBlank())
      try (CmdStream snapshotList=Commandline.execute(btrfsCmd)) {
         Future<?> task=Commandline.background.submit(() -> {// Some Error handling in background
            if (snapshotList.err().peek(System.err::println).anyMatch(line -> {
               return line.contains("Connection closed") || line.contains("connection unexpectedly closed");
            }))
               throw new IOException("connection unexpectedly closed");
            return "";
         });
         snapshotList.erg().forEachOrdered(line -> { // if (file.isDirectory()) {// später prüfen
            Snapshot snapshot=new Snapshot(line);
            // if (snapshot.key().startsWith("§")) System.err.print("\n" + snapshot.key() + " -> " + line); else
            fileMap.put(snapshot.key(), snapshot);
         });
         task.get(); // ende("");// T
         out.println();
      } catch (IOException | ExecutionException | InterruptedException e) {
         e.printStackTrace();
      }
   }
}
