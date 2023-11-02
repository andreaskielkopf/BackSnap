/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.uhingen.kielkopf.andreas.backsnap.btrfs.Btrfs.BTRFS;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.config.Log;
import de.uhingen.kielkopf.andreas.backsnap.config.Log.LEVEL;
import de.uhingen.kielkopf.andreas.beans.Version;
import de.uhingen.kielkopf.andreas.beans.data.Link;
import de.uhingen.kielkopf.andreas.beans.shell.CmdStreams;

/**
 * @author Andreas Kielkopf Repräsentation eines kompletten PC
 */
public record Pc(String extern, // Marker für diesen PC
         ConcurrentSkipListMap<String, Mount> mountCache, // cache der Mounts
         Link<SubVolumeList> subVolumeList, // Liste der Subvolumes
         Link<Version> btrfsVersion, // BTRFS-Version
         Link<Version> kernelVersion, // Kernel-Version
         Link<Path> backupLabel, //
         AtomicBoolean notReachable) {// BackupLabel am BackupPC
   static final ConcurrentSkipListMap<String, Pc> pcCache=new ConcurrentSkipListMap<String, Pc>();
   /* In /tmp werden die Snapshots vorübergehend eingehängt */
   static public final Path TMP_BTRFS_ROOT=Path.of("/tmp/BtrfsRoot");
   static public final Path TMP_BACKUP_ROOT=Path.of("/tmp/BackupRoot");
   static public final Path TMP_BACKSNAP=TMP_BACKUP_ROOT.resolve("@BackSnap");
   static public final String ROOT="root";
   static public final String ROOT_LOCALHOST="root@localhost";
   static public final String SUDO="sudo";
   static public final String SUDO_="sudo ";
   static public final String PKEXEC="pkexec";
   static public final String PKEXEC_="pkexec ";
   static final Pattern allowExtern=Pattern.compile("[a-zA-Z_0-9]+@[a-zA-Z_0-9.]+|" + SUDO_ + "|" + PKEXEC_);
   private static final String BACKUP_OPTIONS=",compress=zstd:9 ";
   private static final String MOUNT_BTRFS="mount -t btrfs ";
   /**
    * Sicherstellen, dass jeder Pc nur einmal erstellt wird
    *
    * @param extern
    *           Netzwerkpfad zum PC
    * @return Pc
    * @throws IOException
    */
   static public Pc getPc(String extern) /* throws IOException */ {
      String x=(extern == null) ? SUDO_
               : extern.startsWith(SUDO) ? SUDO_ //
                        : extern.startsWith(PKEXEC) ? PKEXEC_ : extern;
      if ((x == SUDO_) && System.getenv().containsKey("ECLIPSE_RUN"))
         x=ROOT_LOCALHOST;
      Matcher m=allowExtern.matcher(x);
      if (m.matches())
         if (!pcCache.containsKey(x))
            pcCache.put(x, new Pc(x));
      return pcCache.get(x);
   }
   private Pc(String extern)/* throws IOException */ {
      this(extern, new ConcurrentSkipListMap<>(), new Link<SubVolumeList>("subVolumeList"), new Link<Version>("Btrfs"),
               new Link<Version>("Kernel"), new Link<Path>("backupLabel"), new AtomicBoolean(false));
   }
   public boolean isReachable() {
      return !notReachable.get();
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
   public String getCmd(StringBuilder cmds, boolean needsSudo) {
      if (isExtern())
         cmds.insert(0, "ssh " + extern + " '").append("'");
      else
         if (needsSudo) {
            String[] cmdList=cmds.toString().split(";");
            cmds.setLength(0);
            for (int i=0; i < cmdList.length; i++) // Für jeden Befehl
               cmds.append(extern().equals(PKEXEC_) ? PKEXEC_ : SUDO_).append(cmdList[i]).append(";");// pkexec einfügen
            cmds.setLength(cmds.length() - 1);// ; entfernen
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
      return mountCache.values().stream().filter(m -> m.mountPath().equals(TMP_BTRFS_ROOT)).findFirst();
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
    * @param refresh
    *           soll auf jeden Fall neu gelesen werden ?
    * @throws IOException
    */
   public ConcurrentSkipListMap<String, Mount> getMountList(boolean refresh) throws IOException {
      if (mountCache.isEmpty() || refresh) {
         ConcurrentSkipListMap<String, Mount> mountList2=new ConcurrentSkipListMap<>();
         String mountCmd=getCmd(new StringBuilder(MOUNT_BTRFS), false);
         Log.logln(mountCmd, LEVEL.BTRFS);
         BTRFS.readLock().lock();
         try (CmdStreams mountStream=CmdStreams.getDirectStream(mountCmd)) {
            mountStream.outBGerr().forEachOrdered(line -> mountList2.put(line, mountCache.containsKey(line)//
                     ? mountCache.get(line) // reuse existing
                     : new Mount(this, line))); // create new
            Optional<String> x=mountStream.errLines().filter(l -> (l.contains("No route to host")
                     || l.contains("Connection closed") || l.contains("connection unexpectedly closed"))).findAny();
            if (x.isPresent())
               throw new IOException(x.get());
            mountCache.clear();
            mountCache.putAll(mountList2);// Log.logln("", LEVEL.BTRFS);
         } finally {
            BTRFS.readLock().unlock();
         }
      }
      return mountCache;
   }
   /**
    * Ermittle die btrfs-version des PC
    * 
    * @return
    * @throws IOException
    */
   public final Version getBtrfsVersion() throws IOException {
      if (btrfsVersion.get() == null) {
         String btrfsVersionCmd=getCmd(new StringBuilder(Btrfs.VERSION), false);
         Log.logln(btrfsVersionCmd, LEVEL.COMMANDS);
         BTRFS.readLock().lock();
         try (CmdStreams btrfsVersionStream=CmdStreams.getDirectStream(btrfsVersionCmd)) {
            btrfsVersionStream.outBGerr().forEach(line -> btrfsVersion.set(new Version("btrfs", line)));
            Optional<String> x=btrfsVersionStream.errLines().filter(l -> (l.contains("No route to host")
                     || l.contains("Connection closed") || l.contains("connection unexpectedly closed"))).findAny();
            if (x.isPresent())
               throw new IOException(x.get());
         } finally {
            BTRFS.readLock().unlock();
         }
         Log.logln(this + " " + btrfsVersion.get(), LEVEL.CONFIG);
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
         String versionCmd=getCmd(new StringBuilder("uname -rs"), false);
         Log.logln(versionCmd, LEVEL.COMMANDS);
         try (CmdStreams versionStream=CmdStreams.getDirectStream(versionCmd)) {
            versionStream.outBGerr().forEach(line -> kernelVersion.set(new Version("kernel", line)));
            Optional<String> x=versionStream.errLines().filter(l -> (l.contains("No route to host")
                     || l.contains("Connection closed") || l.contains("connection unexpectedly closed"))).findAny();
            if (x.isPresent())
               throw new IOException(x.get());
         }
         Log.logln(this + " " + kernelVersion.get(), LEVEL.CONFIG);
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
   static public Mount getBackupMount(/* boolean refresh */) throws IOException {
      synchronized (pcCache) {
         Optional<Mount> o=OneBackup.backupPc.getMountList(false).values().stream()
                  .filter(m -> m.mountPath().toString().equals(TMP_BACKUP_ROOT.toString())).findFirst();
         if (o.isPresent())
            return o.get();
         OneBackup.backupPc.getMountList(false).values().stream()
                  .forEach(m -> Log.errln(m.mountPath().toString(), LEVEL.ERRORS));
         throw new RuntimeException(System.lineSeparator() + "Could not find the volume for backupDir: "
                  + Pc.TMP_BACKSNAP + "/" + OneBackup.backupPc.getBackupLabel() + System.lineSeparator()
                  + "Maybe it needs to be mounted first");
      }
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
      Collection<Mount> ml=getMountList(false).values(); // eventuell reicht false;
      if (doMount == ml.stream().filter(m -> m.mountPath() != null)
               .anyMatch(m -> m.mountPath().toString().equals(TMP_BTRFS_ROOT.toString())))
         return; // mount hat schon den gewünschten Status
      Optional<Mount> mount=ml.stream().filter(m -> m.mountPath() != null)
               .filter(m -> m.mountPath().toString().equals(srcDir1.toString())).findAny();
      if (mount.isEmpty()) {
         notReachable.set(true);
         throw new UnknownHostException(
                  Backsnap.LF + "Not able to find the right device for: " + this + ":" + srcDir1.toString());
      }
      mount(TMP_BTRFS_ROOT, mount.get().devicePath(), doMount, "");
      getMountList(true);// liest erneut ein !
   }
   /**
    * @return
    * @throws IOException
    */
   public List<SnapConfig> getSnapConfigs() throws IOException {
      ArrayList<SnapConfig> l=new ArrayList<>();
      ConcurrentSkipListMap<String, Mount> ml=getMountList(false);
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
   /* Liefert true, wenn wir wissen, dass gemountet ist */
   static public void mountBackupRoot(boolean doMount) {
      if (OneBackup.backupId instanceof String uuid && uuid.length() >= 8 && OneBackup.backupPc instanceof Pc pc)
         pc.mountBackupRoot(OneBackup.backupPc, uuid, doMount);
   }
   public void mountBackupRoot(Pc pc, String uuid, boolean doMount) {
      Btrfs.show(pc, false, false).entrySet().stream().filter(e -> e.getKey().contains(uuid)).map(e -> e.getValue())
               .findFirst().ifPresent(volume -> {
                  try {
                     mountBackupRoot(volume, doMount);
                  } catch (IOException e1) {
                     e1.printStackTrace();
                  }
               });
   }
   public void mountBackupRoot(Volume volume, boolean doMount) throws IOException {
      if (volume instanceof Volume v && !v.devices().isEmpty()
               && v.devices().firstEntry().getValue() instanceof Path device)
         mount(Pc.TMP_BACKUP_ROOT, device, doMount, BACKUP_OPTIONS);
   }
   public void mount(Path mountPoint, Path device, boolean doMount, String options) throws IOException {
      if (doMount == getMountList(false).values().stream().anyMatch(m -> m.mountPath().equals(mountPoint)))
         return;
      StringBuilder mountSB=new StringBuilder();
      if (doMount) {
         mountSB.append("mkdir --mode=000 -p ").append(mountPoint).append(";").append(MOUNT_BTRFS).append("-o subvol=/")
                  .append(options).append(" ").append(device).append(" ").append(mountPoint);
      } else {
         mountSB.append("umount -v ").append(mountPoint).append(";rmdir ").append(mountPoint);
      }
      String mountCmd=getCmd(mountSB, true);
      Log.logln(mountCmd, LEVEL.MOUNT);
//      BTRFS.writeLock().tryLock();
      try (CmdStreams mountStream=CmdStreams.getDirectStream(mountCmd)) {
         mountStream.outBGerr().forEach(t -> Log.logln(t, LEVEL.BTRFS_ANSWER));
         if (mountStream.errLines().anyMatch(l -> (l.contains("No route to host") || l.contains("Connection closed")
                  || l.contains("connection unexpectedly closed"))))
            Backsnap.disconnectCount=10;
//      } finally {
//         if (BTRFS.isWriteLockedByCurrentThread())
//            BTRFS.writeLock().unlock();
      }
      getMountList(true); // Mountlist neu einlesen und Mounts gegen-prüfen
   }
}
