/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.util.TreeMap;

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
    * @throws IOException
    */
   public SnapTree(String dirName, String extern) throws IOException {
      this(dirName, extern, new TreeMap<>());
      populate();
   }
   private void populate() throws IOException {// otime kommt nur bei snapshots
      StringBuilder btrfsCmd=new StringBuilder("btrfs subvolume list -spcguqR ").append(dirName);
      if ((extern instanceof String x) && (!x.isBlank()))
         if (x.startsWith("sudo "))
            btrfsCmd.insert(0, x);
         else
            btrfsCmd.insert(0, "ssh " + x + " '").append("'");
      System.out.println(btrfsCmd);
      try (CmdStream snapshotList=Commandline.execute(btrfsCmd)) {
         snapshotList.backgroundErr();
         snapshotList.erg().forEachOrdered(line -> { // if (file.isDirectory()) {// später prüfen
            Snapshot snapshot=new Snapshot(line);
            fileMap.put(snapshot.path().toString(), snapshot);
         });
         for (String line:snapshotList.errList())
            if (line.contains("No route to host") || line.contains("Connection closed")
                     || line.contains("connection unexpectedly closed"))
               throw new IOException(line);
      }
   }
}
