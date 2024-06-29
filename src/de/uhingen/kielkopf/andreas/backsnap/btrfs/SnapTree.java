/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.backsnap.btrfs.Btrfs.BTRFS;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.config.Log;
import de.uhingen.kielkopf.andreas.backsnap.config.Log.LEVEL;
import de.uhingen.kielkopf.andreas.beans.shell.CmdStreams;

/**
 * For one Subvolume that is mounted,
 * 
 * collect all Snapshots of this Volume(device) in sorted trees
 * 
 * @author Andreas Kielkopf
 */
public record SnapTree(Mount sMount, ConcurrentSkipListMap<String, Snapshot> sUuidMap,
         ConcurrentSkipListMap<String, Snapshot> rUuidMap, ConcurrentSkipListMap<Path, Snapshot> btrfsPathMap,
         ConcurrentSkipListMap<String, Snapshot> dateMap) {
   private static final ConcurrentSkipListMap<String, SnapTree> snapTreeCache=new ConcurrentSkipListMap<>();
   /**
    * create record and populate all Maps
    * 
    * @param sMount
    * @throws IOException
    */
   public SnapTree(Mount mount) throws IOException {
      this(mount, new ConcurrentSkipListMap<>(), new ConcurrentSkipListMap<>(), new ConcurrentSkipListMap<>(),
               new ConcurrentSkipListMap<>());
      populate();
   }
   private void populate() throws IOException {// otime kommt nur bei snapshots
      // mit -a bekommt man alle Snapshots für dieses Device
      StringBuilder svListCommand=new StringBuilder(Btrfs.SUBVOLUME_LIST_2).append(sMount.mountPath());
      String svListCmd=sMount.pc().getCmd(svListCommand, true);
      Log.logln(svListCmd, LEVEL.BTRFS);
      BTRFS.readLock().lock();
      try (CmdStreams svListStream=CmdStreams.getCachedStream(svListCmd, sMount.keyD())) {
         svListStream.outBGerr().forEachOrdered(line -> {
            try {
               if (line.contains("timeshift"))
                  Log.logln(line, LEVEL.BTRFS_ANSWER);
               add(new Snapshot(sMount, line));
            } catch (Exception e) {
               System.err.println(" --> " + e);
               e.printStackTrace();
            }
         });
         Optional<String> erg=svListStream.errLines().filter(line -> (line.contains("No route to host")
                  || line.contains("Connection closed") || line.contains("connection unexpectedly closed"))).findAny();
         if (erg.isPresent()) {
            Backsnap.disconnectCount=10;
            throw new IOException(erg.get());
         }
         Thread.onSpinWait();
      } finally {
         BTRFS.readLock().unlock();
      }
   }
   private void add(Snapshot snapshot) {
      sUuidMap.put(snapshot.uuid(), snapshot);
      btrfsPathMap.put(snapshot.btrfsPath(), snapshot);// nach pfad sortiert
      dateMap.put(snapshot.keyO(), snapshot);
      if (snapshot.isBackup())
         rUuidMap.put(snapshot.received_uuid(), snapshot);
   }
   @SuppressWarnings("unused")
   private void remove(Snapshot snapshot) {
      sUuidMap.remove(snapshot.uuid(), snapshot);
      btrfsPathMap.remove(snapshot.btrfsPath(), snapshot);// nach pfad sortiert
      dateMap.remove(snapshot.keyO(), snapshot);
      if (snapshot.isBackup())
         rUuidMap.remove(snapshot.received_uuid(), snapshot);
   }
   /**
    * Look for Snapshots of the specified mounted subvolume (But we get all snapshots of the underlying Volume, so this is worth caching)
    * 
    * @param mount2
    * @param mountPoint
    * @param oextern2
    * @return a SnapTree
    * @throws IOException
    */
   static public SnapTree getSnapTree(Mount mount2) throws IOException {
      String deviceKey=mount2.keyD();
      if (!snapTreeCache.containsKey(deviceKey)) {
         snapTreeCache.put(deviceKey, new SnapTree(mount2));
         Log.logln("set " + deviceKey + " into treeCache", LEVEL.CACHE);
      } else
         Log.logln("take " + deviceKey + " from treeCache", LEVEL.CACHE);
      return snapTreeCache.get(deviceKey);
   }
   @Override
   public String toString() {
      StringBuilder sb=new StringBuilder("SnapTree [").append(sMount.pc().extern()).append(":")
               .append(sMount.devicePath()).append(" -> ").append(sMount.mountPath()).append("[")//
               .append(sUuidMap.size()).append(":");
      for (Snapshot s:dateMap.values())
         sb.append(s.dirName()).append(",");
      sb.setLength(sb.length() - 1);
      sb.append("]]");
      return sb.toString();
   }
   public boolean containsBackupOf(Snapshot snap) {
      return rUuidMap.containsKey(snap.uuid());
   }
   public boolean containsPath(Path path) {
      return btrfsPathMap.containsKey(path);
   }
   public Snapshot getByPath(Path path) {
      return btrfsPathMap.get(path);
   }
   public Optional<Snapshot> getFirstByPath(Mount m) {
      String pfad=m.btrfsPath().toString();
      return btrfsPathMap().values().stream().filter(s -> s.btrfsPath().toString().equals(pfad)).findFirst();
   }
   public Optional<Snapshot> getAnyBackupOf(Snapshot subVolume) {
      String id=subVolume.uuid();
      return btrfsPathMap().values().stream().filter(s -> s.parent_uuid().equals(id)).findAny();
   }
   public boolean isEmpty() {
      return dateMap.isEmpty();
   }
   public Collection<Snapshot> valuesByDate() {
      return dateMap.values();
   }
   public Path getSnapPath(Path backupDir) {
      return sMount().btrfsPath().resolve(sMount().mountPath().relativize(backupDir)).resolve(Snapshot.SNAPSHOT);
   }
}
