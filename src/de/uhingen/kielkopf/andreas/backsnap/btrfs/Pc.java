/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;
import de.uhingen.kielkopf.andreas.beans.data.Link;

/**
 * @author Andreas Kielkopf Repräsentation eines kompletten PC
 */
public record Pc(String extern, // Marker für diesen PC
         ConcurrentSkipListMap<String, Mount> mountCache, // cache der Mounts
         Link<SubVolumeList> cachedSubVolumeList, // Liste der Subvolumes
         Link<Version> cachedBtrfsVersion, // BTRFS-Version
         Link<Version> cachedKernelVersion) {// Kernel-Version
   static final ConcurrentSkipListMap<String, Pc> pcCache=new ConcurrentSkipListMap<String, Pc>();
   /* In /tmp werden bei Timeshift Pcs die Snapshots vorübergehnd eingehängt */
   static public final String TMP_BTRFS_ROOT="/tmp/BtrfsRoot";
   /**
    * Sicherstellen, dass jeder Pc nur einmal erstellt wird
    * 
    * @param extern
    *           Netzwerkpfad zum PC
    * @return Pc
    * @throws IOException
    */
   static public Pc getPc(String extern) /* throws IOException */ {
      String x=(extern == null) ? "" : extern.startsWith("sudo") ? "sudo " : extern;
      if (!pcCache.containsKey(x))
         pcCache.put(x, new Pc(x));
      return pcCache.get(x);
   }
   private Pc(String extern)/* throws IOException */ {
      this(extern, new ConcurrentSkipListMap<>(), new Link<SubVolumeList>(), new Link<Version>(), new Link<Version>());
   }
   public boolean isExtern() {
      return extern.contains("@");
   }
   /**
    * ergänze die commandos an diesen PC um den notwendigen Zugriff per ssh oder sudo
    * 
    * @param cmds
    * @return
    */
   public String getCmd(StringBuilder cmds) {
      if (isExtern()) {
         cmds.insert(0, "ssh " + extern + " '").append("'");
      } else {
         String[] cmdList=cmds.toString().split(";");
         cmds.setLength(0);
         for (int i=0; i < cmdList.length; i++) // Für jeden Befehl
            cmds.append("sudo ").append(cmdList[i]).append(";");// sudo einfügen
         cmds.setLength(cmds.length() - 1);
      }
      return cmds.toString();
   }
   /**
    * Sicherstellen, dass jeder Mount dieses Pcs nur einmal erstellt wird
    * 
    * @param line
    * @return
    * @throws IOException
    */
   public Mount getMount(String line) throws IOException {
      if (line == null)
         return null;
      if (!mountCache.containsKey(line))
         mountCache.put(line, new Mount(this, line));
      return mountCache.get(line);
   }
   /**
    * @return Optional<Mount> wenn Timeshiftbase schon eingehängt ist
    */
   public Optional<Mount> getTimeshiftBase() {
      return mountCache.values().stream().filter(m -> m.mountPath().toString().equals(TMP_BTRFS_ROOT)).findFirst();
   }
   /**
    * @return
    * @throws IOException
    */
   public SubVolumeList getSubVolumeList() throws IOException {
      if (cachedSubVolumeList.get() == null)
         cachedSubVolumeList.set(new SubVolumeList(this));
      return cachedSubVolumeList.get();
   }
   /**
    * Ermittle alle Mounts eines Rechners
    * 
    * @param doReadAgain
    *           soll auf jeden Fall neu gelesen werden ?
    * @throws IOException
    */
   public ConcurrentSkipListMap<String, Mount> getMountList(boolean doReadAgain) throws IOException {
      if (mountCache.isEmpty() || doReadAgain) {
         String mountCmd=getCmd(new StringBuilder("mount -t btrfs"));
         Backsnap.logln(3, mountCmd);
         ConcurrentSkipListMap<String, Mount> mountList2=new ConcurrentSkipListMap<>();
         try (CmdStream mountStream=Commandline.executeCached(mountCmd, doReadAgain ? null : mountCmd)) {
            mountStream.backgroundErr();
            for (String line:mountStream.erg().toList())
               mountList2.put(line, mountCache.containsKey(line) //
                        ? mountCache.get(line) // reuse existing
                        : new Mount(this, line)); // create new
            mountCache.clear();
            mountCache.putAll(mountList2);
            mountStream.waitFor();
            for (String line:mountStream.errList())
               if (line.contains("No route to host") || line.contains("Connection closed")
                        || line.contains("connection unexpectedly closed"))
                  throw new IOException(line);
            Backsnap.logln(3, "");
         }
      }
      return mountCache;
   }
   @SuppressWarnings("unused")
   private ConcurrentSkipListMap<String, Volume> getVolumeList() {
      ConcurrentSkipListMap<String, Volume> list         =new ConcurrentSkipListMap<>();
      // .append(onlyMounted ? "m" : "d");
      String                                volumeListCmd=getCmd(new StringBuilder("btrfs filesystem show"));
      Backsnap.logln(7, volumeListCmd);
      String cacheKey=volumeListCmd;
      try (CmdStream volumeListStream=Commandline.executeCached(volumeListCmd, cacheKey)) {
         volumeListStream.backgroundErr();
         List<String> lines=volumeListStream.erg().toList();
         for (int i=0; i < lines.size() - 3; i++)
            if (lines.get(i).startsWith("Label:"))
               try {
                  Volume v  =new Volume(this, lines.get(i), lines.get(i + 2));
                  String key=v.uuid() + v.device();
                  list.put(key, v);
               } catch (IOException e) {
                  e.printStackTrace();
               }
      } catch (IOException e1) {
         e1.printStackTrace();
      }
      return list;
   }
   /**
    * Ermittle die btrfs-version des PC
    * 
    * @return
    * @throws IOException
    */
   public final Version getBtrfsVersion() throws IOException {
      if (cachedBtrfsVersion.get() == null) {
         String versionCmd=getCmd(new StringBuilder("btrfs version"));
         Backsnap.logln(6, versionCmd);
         try (CmdStream versionStream=Commandline.executeCached(versionCmd, versionCmd)) {
            versionStream.backgroundErr();
            for (String line:versionStream.erg().toList())
               cachedBtrfsVersion.set(new Version(line));
            versionStream.waitFor();
            for (String line:versionStream.errList())
               if (line.contains("No route to host") || line.contains("Connection closed")
                        || line.contains("connection unexpectedly closed"))
                  throw new IOException(line);
         }
      }
      return cachedBtrfsVersion.get();
   }
   /**
    * Ermittle die KernelVersion des PC
    * 
    * @return
    * @throws IOException
    */
   public final Version getKernelVersion() throws IOException {
      if (cachedKernelVersion.get() == null) {
         String versionCmd=getCmd(new StringBuilder("uname -rs"));
         Backsnap.logln(6, versionCmd);
         try (CmdStream versionStream=Commandline.executeCached(versionCmd, versionCmd)) {
            versionStream.backgroundErr();
            for (String line:versionStream.erg().toList())
               cachedKernelVersion.set(new Version(line));
            versionStream.waitFor();
            for (String line:versionStream.errList())
               if (line.contains("No route to host") || line.contains("Connection closed")
                        || line.contains("connection unexpectedly closed"))
                  throw new IOException(line);
         }
      }
      return cachedKernelVersion.get();
   }
   // @Override public int compareTo(Pc o) { if (o instanceof Pc pc) return extern.compareTo(o.extern); return 1; }
   // @Override public boolean equals(Object o) { if (o instanceof Pc pc) return Objects.equals(extern, pc.extern);
   // return false; }
   // @Override public int hashCode() { return Objects.hash(extern); }
   /**
    * @param backupKey
    * @return
    * @throws IOException
    */
   public Mount getBackupVolume(String backupKey) throws IOException {
      return getSubVolumeList().getBackupVolume(backupKey);
   }
   @Override
   public String toString() {
      return new StringBuilder("Pc[").append(extern).append("]").toString();
   }
   /**
    * Mountet die timeshift-Snapshots temorär damit BackSnap zugriff hat
    * 
    * @param srcDir
    * @param doMount
    *           mount oder umount ?
    * @throws IOException
    */
   public void mountBtrfsRoot(Path srcDir1, boolean doMount) throws IOException {
      Collection<Mount> ml=getMountList(true).values(); // eventuell reicht false;
      if (doMount == ml.stream().anyMatch(m -> m.mountPath().toString().equals(TMP_BTRFS_ROOT)))
         return; // mount hat schon den gewünschten Status
      Optional<Mount> mount=ml.stream().filter(m -> m.mountPath().toString().equals(srcDir1.toString())).findAny();
      if (mount.isEmpty())
         throw new RuntimeException("Not able to find the right device for: " + this + ":" + srcDir1.toString());
      StringBuilder mountSB=new StringBuilder();
      if (doMount) {
         mountSB.append("mkdir --mode=000 -p ").append(TMP_BTRFS_ROOT).append(";");
         mountSB.append("mount -t btrfs -o subvol=/ ").append(mount.get().devicePath()).append(" ")
                  .append(TMP_BTRFS_ROOT);
      } else {
         mountSB.append("umount ").append(TMP_BTRFS_ROOT).append(";");
         mountSB.append("rmdir ").append(TMP_BTRFS_ROOT);
      }
      String mountCmd=getCmd(mountSB);
      Backsnap.logln(4, mountCmd);// if (!DRYRUN.get())
      try (CmdStream mountStream=Commandline.executeCached(mountCmd, null)) { // not cached
         mountStream.backgroundErr();
         mountStream.erg().forEach(t -> Backsnap.logln(4, t));
         for (String line:mountStream.errList())
            if (line.contains("No route to host") || line.contains("Connection closed")
                     || line.contains("connection unexpectedly closed")) {
               Backsnap.connectionLost=10;
               break;
            } // ende("");// R
      }
      getMountList(true);// liest erneut ein !
   }
   /**
    * @return
    * @throws IOException
    */
   public List<SnapConfig> getSnapConfigs() throws IOException {
      ArrayList<SnapConfig>                l =new ArrayList<>();
      ConcurrentSkipListMap<String, Mount> ml=getMountList(true);
      for (Mount m:ml.values()) {
         SnapTree st=m.getSnapTree();
         String   mp=m.btrfsPath().toString();
//         String   dp=m.devicePath().toString();
         System.out.print("mp=" + mp + " > ");
         Optional<Snapshot> first=st.btrfsPathMap().values().stream().
         // filter(s -> s.d)
                  filter(s -> s.btrfsPath().toString().equals(mp)).findFirst();
         if (first.isPresent()) { // ein subVolume
            Snapshot subVolume=first.get();
            String   uuid     =subVolume.uuid();
            System.out.print(uuid);
            if (!subVolume.isSubvolume())
               continue;
            Optional<Snapshot> any=st.btrfsPathMap().values().stream().filter(s -> s.parent_uuid().equals(uuid))
                     .findAny();
            if (any.isPresent()) {
               Snapshot snapshot=any.get();
               System.out.print(" child is: ");
               System.out.print(snapshot.uuid());
               String          sp  =snapshot.btrfsPath().toString();
               // suche den passenden Mount
               Optional<Mount> ziel=ml.values().stream().filter(n -> sp.startsWith(n.btrfsPath().toString())).findAny();
               if (ziel.isPresent()) {
                  SnapConfig sc=new SnapConfig(m, ziel.get());
                  l.add(sc);
               }
            }
         }
         System.out.println();
      }
      for (Mount volumeMount:getSubVolumeList().mountTree().values()) { // über alle Subvolumes laufen
         // if (volumeMount.otimeKeyMap().isEmpty())
         // continue;
         o: for (Snapshot snap_o:volumeMount.otimeKeyMap().values()) { // über die einzelnen Snapshots
            Path btrfsPath=snap_o.btrfsPath();
            for (Mount snapshotMount:getSubVolumeList().mountTree().values()) { // über alle subvolumes laufen
               if (!volumeMount.devicePath().equals(snapshotMount.devicePath())) // nur auf diesem device möglich
                  continue;
               Path sdir=snapshotMount.btrfsPath();
               int  le2 =snapshotMount.btrfsMap().size();
               if (le2 > 1) {// von diesem subolume darf es keine eigenen Snapshot geben
                  // sdir+="/";
                  if (!btrfsPath.startsWith(sdir + "/"))
                     continue;
                  l.add(new SnapConfig(volumeMount, snapshotMount));
                  break o;
               }
               if (sdir.equals(volumeMount.btrfsPath())) // das darf nicht das selbe sein
                  continue;
               if (!btrfsPath.startsWith(sdir))
                  continue;
               l.add(new SnapConfig(volumeMount, snapshotMount));
               break o;
            }
            System.out.println("nix gefunden");
            break;
         }
      }
      return l;
      // return SnapConfig.getList(getSubVolumeList());
   }
}
