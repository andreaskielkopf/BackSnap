/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;

/**
 * Alle Snapshots eines bestimmten Mountpoints sortiert in einem Tree
 * 
 * @author Andreas Kielkopf
 */
public record SnapTree(Mount mount, TreeMap<String, Snapshot> fileMap) {
   final static ConcurrentSkipListMap<String, SnapTree> snapTreeCache=new ConcurrentSkipListMap<>();
   /**
    * Hole ein Verzeichniss in die Map
    * 
    * @param mount
    * @throws IOException
    */
   public SnapTree(Mount mount) throws IOException {
      this(mount, new TreeMap<>());
      populate();
   }
   private void populate() throws IOException {// otime kommt nur bei snapshots
      String        extern  =mount.mountList().extern();
      String        device  =mount.device();
      String        cacheKey=extern + ":" + device;
      String        dirName =mount.mountPoint();
      // mit -a bekommt man alle Snapshots für dieses Device
      StringBuilder btrfsCmd=new StringBuilder("btrfs subvolume list -aspcguqR ").append(dirName);
      if ((mount.mountList().extern() instanceof String x) && (!x.isBlank()))
         if (x.startsWith("sudo "))
            btrfsCmd.insert(0, x);
         else
            btrfsCmd.insert(0, "ssh " + x + " '").append("'");
      System.out.println(btrfsCmd);
      try (CmdStream snapshotList=Commandline.executeCached(btrfsCmd, cacheKey)) {
         snapshotList.backgroundErr();
         snapshotList.erg().forEachOrdered(line -> { // if (file.isDirectory()) {// später prüfen
            Snapshot snapshot=new Snapshot(mount, line);
            // if (line.contains("srv"))
            // System.out.println(line);
            fileMap.put(snapshot.path().toString(), snapshot);// nach pfad sortiert
         });
         for (String line:snapshotList.errList())
            if (line.contains("No route to host") || line.contains("Connection closed")
                     || line.contains("connection unexpectedly closed"))
               throw new IOException(line);
      }
   }
   /**
    * @param mount2
    * @param mountPoint
    * @param oextern2
    * @return
    * @throws IOException
    */
   public static SnapTree getSnapTree(Mount mount2/* , String omountPoint, String oextern2 */) throws IOException {
      String deviceKey=mount2.oextern()+"->"+ mount2.device(); // +mount2.extern
      if (!snapTreeCache.containsKey(deviceKey)) {
         SnapTree st=new SnapTree(mount2/* , omountPoint, oextern2 */);
         snapTreeCache.put(deviceKey, st);// nach deviceKey sortiert
         System.out.println("set " + deviceKey + " into treeCache");
      } else
         System.err.println("take " + deviceKey + " from treeCache");
      return snapTreeCache.get(deviceKey);
   }
   @Override
   public String toString() {
      StringBuilder sb=new StringBuilder("SnapTree [").append(mount.mountList().extern()).append(":")
               .append(mount.device()).append(" -> ").append(mount.mountPoint()).append("[");
      sb.append(fileMap.size()).append(":");
      for (Snapshot s:fileMap.values())
         sb.append(s.dirName()).append(",");
      sb.setLength(sb.length() - 1);
      sb.append("]]");
      return sb.toString();
   }
}
