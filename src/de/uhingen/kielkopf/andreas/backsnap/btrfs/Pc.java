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
import de.uhingen.kielkopf.andreas.beans.Version;
import de.uhingen.kielkopf.andreas.beans.data.Link;

/**
 * @author Andreas Kielkopf Repräsentation eines kompletten PC
 */
public record Pc(String extern, // Marker für diesen PC
         ConcurrentSkipListMap<String, Mount> mountCache, // cache der Mounts
         Link<SubVolumeList> subVolumeList, // Liste der Subvolumes
         Link<Version> btrfsVersion, // BTRFS-Version
         Link<Version> kernelVersion, // Kernel-Version
         Link<Path> backupLabel) {// BackupLabel am BackupPC
   static final ConcurrentSkipListMap<String, Pc> pcCache=new ConcurrentSkipListMap<String, Pc>();
   /* In /tmp werden bei Timeshift Pcs die Snapshots vorübergehnd eingehängt */
   static public final String TMP_BTRFS_ROOT="/tmp/BtrfsRoot";
   static public final String MNT_BACKSNAP="/mnt/BackSnap";
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
      this(extern, new ConcurrentSkipListMap<>(), new Link<SubVolumeList>("subVolumeList"), new Link<Version>("BTRFS"),
               new Link<Version>("Kernel"), new Link<Path>("backupLabel"));
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
      if (subVolumeList.get() == null)
         subVolumeList.set(new SubVolumeList(this));
      return subVolumeList.get();
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
      ConcurrentSkipListMap<String, Volume> list=new ConcurrentSkipListMap<>();
      // .append(onlyMounted ? "m" : "d");
      String volumeListCmd=getCmd(new StringBuilder("btrfs filesystem show"));
      Backsnap.logln(7, volumeListCmd);
      String cacheKey=volumeListCmd;
      try (CmdStream volumeListStream=Commandline.executeCached(volumeListCmd, cacheKey)) {
         volumeListStream.backgroundErr();
         List<String> lines=volumeListStream.erg().toList();
         volumeListStream.waitFor();
         for (int i=0; i < lines.size() - 3; i++)
            if (lines.get(i).startsWith("Label:"))
               try {
                  Volume v=new Volume(this, lines.get(i), lines.get(i + 2));
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
      if (btrfsVersion.get() == null) {
         String versionCmd=getCmd(new StringBuilder("btrfs version"));
         Backsnap.logln(6, versionCmd);
         try (CmdStream versionStream=Commandline.executeCached(versionCmd, versionCmd)) {
            versionStream.backgroundErr();
            for (String line:versionStream.erg().toList())
               btrfsVersion.set(new Version(line));
            versionStream.waitFor();
            for (String line:versionStream.errList())
               if (line.contains("No route to host") || line.contains("Connection closed")
                        || line.contains("connection unexpectedly closed"))
                  throw new IOException(line);
         }
         Backsnap.logln(1, this + " btrfs: " + btrfsVersion.get());
      }
      return btrfsVersion.get();
   }
   /**
    * Ermittle die KernelVersion des PC
    * 
    * @return
    * @throws IOException
    */
   public final Version getKernelVersion() throws IOException {
      if (kernelVersion.get() == null) {
         String versionCmd=getCmd(new StringBuilder("uname -rs"));
         Backsnap.logln(6, versionCmd);
         try (CmdStream versionStream=Commandline.executeCached(versionCmd, versionCmd)) {
            versionStream.backgroundErr();
            for (String line:versionStream.erg().toList())
               kernelVersion.set(new Version(line));
            versionStream.waitFor();
            for (String line:versionStream.errList())
               if (line.contains("No route to host") || line.contains("Connection closed")
                        || line.contains("connection unexpectedly closed"))
                  throw new IOException(line);
         }
         Backsnap.logln(0, this + " kernel: " + kernelVersion.get());
      }
      return kernelVersion.get();
   }
   // @Override public int compareTo(Pc o) { if (o instanceof Pc pc) return extern.compareTo(o.extern); return 1; }
   // @Override public boolean equals(Object o) { if (o instanceof Pc pc) return Objects.equals(extern, pc.extern);
   // return false; }
   // @Override public int hashCode() { return Objects.hash(extern); }
   public final Path getBackupLabel() {
      return backupLabel.get();
   }
   public final void setBackupLabel(Path bl) {
      backupLabel.set(bl);
   }
   /**
    * @param backupKey
    * @return
    * @throws IOException
    */
   public Mount getBackupVolume() throws IOException {
      ConcurrentSkipListMap<String, Mount> mt=getSubVolumeList().mountTree();
      String vorschlag=extern() + ":" + MNT_BACKSNAP;
      if (mt.get(vorschlag) instanceof Mount m) {
//         if (!m.btrfsMap().isEmpty())
            return m;
//         throw new FileNotFoundException(
//                  System.lineSeparator() + "Ingnoring, because there are no snapshots in: " + this);
      }
      throw new RuntimeException(System.lineSeparator() + "Could not find the volume for backupDir: " + Pc.MNT_BACKSNAP
               + "/" + OneBackup.backupPc.getBackupLabel() + System.lineSeparator()
               + "Maybe it needs to be mounted first");
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
         throw new RuntimeException(
                  Backsnap.LF + "Not able to find the right device for: " + this + ":" + srcDir1.toString());
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
         mountStream.waitFor();
         for (String line:mountStream.errList())
            if (line.contains("No route to host") || line.contains("Connection closed")
                     || line.contains("connection unexpectedly closed")) {
               Backsnap.disconnectCount=10;
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
      ArrayList<SnapConfig> l=new ArrayList<>();
      ConcurrentSkipListMap<String, Mount> ml=getMountList(true);
      for (Mount m:ml.values()) {
         SnapTree st=m.getSnapTree();
         Optional<Snapshot> first=st.btrfsPathMap().values().stream()
                  .filter(s -> s.btrfsPath().toString().equals(m.btrfsPath().toString())).findFirst();
         if (first.isPresent()) { // ein subVolume
            Snapshot subVolume=first.get();
            if (subVolume.isPlainSnapshot())
               continue;
            // gibt es einen Snapshot von diesem SubVolume ?
            Optional<Snapshot> any=st.btrfsPathMap().values().stream()
                     .filter(s -> s.parent_uuid().equals(subVolume.uuid())).findAny();
            if (any.isPresent())
               if (any.get().mount() instanceof Mount mount)
                  l.add(new SnapConfig(m, mount));
         }
      }
      return l;
   }
}
