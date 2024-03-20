/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.beans.RecordParser.*;
import static de.uhingen.kielkopf.andreas.backsnap.btrfs.Btrfs.BTRFS;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.config.Log;
import de.uhingen.kielkopf.andreas.backsnap.config.Log.LEVEL;
import de.uhingen.kielkopf.andreas.backsnap.gui.BacksnapGui;
import de.uhingen.kielkopf.andreas.beans.cli.Flag;
import de.uhingen.kielkopf.andreas.beans.data.Link;
import de.uhingen.kielkopf.andreas.beans.shell.CmdStreams;

/**
 * @author Andreas Kielkopf
 * 
 *         Snapshot (readony) oder Subvolume (writable)
 */
public record Snapshot(Mount mount, Integer id, Integer gen, Integer cgen, Integer parent, Integer top_level, //
         String otime, String parent_uuid, String received_uuid, String uuid, Path btrfsPath, Link<Boolean> readonlyL) {
   static final Pattern ID=createPatternFor("ID");
   static final Pattern GEN=createPatternFor("gen");
   static final Pattern CGEN=createPatternFor("cgen");
   static final Pattern PARENT=createPatternFor("parent");
   static final Pattern TOP_LEVEL=createPatternFor("top level");
   static final Pattern OTIME=Pattern.compile("[ \\[]" + "otime" + "[ =]([^ ]+ [^ ,\\]]+)");// [ =\\[]([^ ,\\]]+)
   static final Pattern PARENT_UUID=createPatternFor("parent_uuid");
   static final Pattern RECEIVED_UUID=createPatternFor("received_uuid");
   static final Pattern UUID=createPatternFor("uuid");
   static final Pattern BTRFS_PATH=Pattern.compile("^(?:.*? )path (?:<[^>]+>)?([^ ]+).*?$");
   static final Pattern NUMERIC_DIRNAME=Pattern.compile("([0-9]+)/snapshot$");
   static final Pattern DIRNAME=Pattern.compile("([^/]+)/snapshot$");
   static final Pattern SUBVOLUME=Pattern.compile("^(@[0-9a-zA-Z.]+)/.*[0-9]+/snapshot$");
   public Snapshot(Mount mount, String from_btrfs) throws IOException {
      this(getMount(mount, getPath(BTRFS_PATH.matcher(from_btrfs))), getInt(ID.matcher(from_btrfs)),
               getInt(GEN.matcher(from_btrfs)), getInt(CGEN.matcher(from_btrfs)), getInt(PARENT.matcher(from_btrfs)),
               getInt(TOP_LEVEL.matcher(from_btrfs)), //
               getString(OTIME.matcher(from_btrfs)), getString(PARENT_UUID.matcher(from_btrfs)),
               getString(RECEIVED_UUID.matcher(from_btrfs)), getString(UUID.matcher(from_btrfs)),
               getPath(BTRFS_PATH.matcher(from_btrfs)), new Link<Boolean>("readonly"));
      if ((btrfsPath == null) || (mount == null))
         throw new FileNotFoundException("btrfs-path is missing for snapshot: " + mount + from_btrfs);
   }
   static private Pattern createPatternFor(String s) {
      return Pattern.compile("^(?:.*[ \\[])?" + s + "[ =]([^ ,\\]]+)");
   }
   static public final int SORT_LEN=10; // reichen 100 Jahre ???
   /**
    * @return Key um snapshot zu sortieren sofern im Pfad ein numerischer WERT steht
    */
   public String key() {
      Matcher m=NUMERIC_DIRNAME.matcher(btrfsPath.toString());
      if (m.find())
         return dir2key(m.group(1)) + btrfsPath.toString(); // ??? numerisch sortieren ;-)
      return btrfsPath.toString();
   }
   public String keyO() {
      return new StringBuilder((mount == null) ? "null:" : mount().keyM()).append(otime()).append(idN())
               .append(sortableDirname()).append(btrfsPath().getFileName()).toString();
   }
   public String keyB() {
      return new StringBuilder((mount == null) ? "null:" : mount().keyM()).append(sortableDirname())
               .append(btrfsPath().getFileName()).append(idN()).toString();
   }
   /**
    * @return sortable Integer
    */
   private String idN() {
      String s=Integer.toUnsignedString(id());
      String t="0".repeat(SORT_LEN - s.length()) + s;
      return t;
   }
   static private final String dir2key(@Nullable String dir) { // numerisch sortierbar ;-)
      return (dir instanceof String d) ? (d.length() >= SORT_LEN) ? d : ".".repeat(SORT_LEN - d.length()).concat(d)
               : null;
   }
   public String dirName() {
      Matcher m=DIRNAME.matcher(btrfsPath.toString());
      if (m.find())
         return m.group(1);
      Path dn=btrfsPath.getParent().getFileName();
      if (dn == null)
         return null;
      return dn.toString();
   }
   public String sortableDirname() {
      return dir2key(dirName());
   }
   /**
    * Das soll den Zeitpunkt liefern, an dem der Snapshot gemacht wurde, wenn der berechnet werden kann
    * 
    * @return Instant
    */
   public final Instant stunden() {
      try {
         String[] t=dirName().split("_");
         return Instant.parse(t[0] + "T" + t[1].replace('-', ':') + "Z");
      } catch (Exception e) {/* ignore */ }
      try {
         long nr=Long.parseLong(dirName()) * 3_600L;// 1 Stunde
         Instant i=Instant.now().plusSeconds(nr);
         return i;
      } catch (Exception e) {/* ignore */ }
      return null;
   }
   /**
    * @return Mount dieses Snapshots sofern im Pfad enthalten
    */
   public String subvolume() {
      Matcher m=SUBVOLUME.matcher(btrfsPath.toString());
      return (m.find()) ? m.group(1) : "";
   }
   public boolean isSubvolume() throws IOException {
      // if(isPlaisSnapshot()) return false;
      if (isReadonly())// Alles was readonly ist, ist ganz sicher kein Subvolume
         return false;
      if (isSnapper()) {
         if (isDirectMount()) // wenn er direkt gemountet ist wird er jetzt als Subvolume genutzt
            return true;
         if (!hasParent()) // Wenn keine ParentUID da ist, ist es wahrscheinlich ein Subvolume
            return true;
         return false;
      }
      if (isTimeshift()) { // Wenn es ein Timeshift-Name ist
         if (isDirectMount()) // wenn er direkt gemountet ist wird er jetzt als Subvolume genutzt
            return true;
         if (!hasParent()) // Wenn keine ParentUID da ist, ist es sicher jetzt ein Subvolume
            return true;
         return false; // ansonsten ist es ein Snapshot
      }
      if (isPlainSnapshot())
         return false;
      return true;
   }
   /**
    * The subvolume flag currently implemented is the ro property. Read-write subvolumes have that set to false, snapshots as true. In addition to
    * that, a plain snapshot will also have last change generation and creation generation equal.
    * 
    * @return
    */
   boolean isPlainSnapshot() {
      return cgen == gen;
   }
   /**
    * @return Ist dieser Snapshot ReadOnly ?
    * @throws IOException
    */
   private boolean isReadonly() throws IOException {
      if (readonlyL().get() == null) {
         String getReadonlyCmd=mount().pc()
                  .getCmd(new StringBuilder(Btrfs.PROPERTY_GET).append(getSnapshotMountPath()).append(" ro"), false);
         Log.logln(getReadonlyCmd, LEVEL.BTRFS);
         BTRFS.readLock().lock();
         try (CmdStreams getReadonlyStream=CmdStreams.getDirectStream(getReadonlyCmd)) {
            Optional<String> readonly=getReadonlyStream.outBGerr().peek(t -> Log.logln(t, LEVEL.BTRFS))
                     .filter(t -> t.startsWith("ro=")).findAny();
            if (getReadonlyStream.errLines().anyMatch(line -> line.contains("No route to host")
                     || line.contains("Connection closed") || line.contains("connection unexpectedly closed")))
               Backsnap.disconnectCount=10;
            if (readonly.isPresent())
               return readonlyL().set(Boolean.parseBoolean(readonly.get().split("=")[1]));
         } finally {
            BTRFS.readLock().unlock();
         } // return false;
      }
      return readonlyL().get();
   }
   public boolean isBackup() {
      return received_uuid().length() > 8;
   }
   private boolean isDirectMount() {
      return false;
   }
   private boolean hasParent() {
      return parent_uuid().length() <= 8;
   }
   /**
    * @return ist das ein Timeshift-Snapshot mit standardpfad ?
    */
   private boolean isTimeshift() {
      return btrfsPath().toString().startsWith("/timeshift-btrfs/snapshots/");
   }
   private boolean isSnapper() {
      return false;
   }
   /**
    * gibt es einen mount der für diesen snapshot passt ?
    * 
    * @return mountpoint oder null
    */
   public Path getBackupMountPath() {
      if (mount == null)
         return null;
      Path rel=mount.btrfsPath().relativize(btrfsPath);
      Path abs=mount.mountPath().resolve(rel);
      return abs;
   }
   public Path getSnapshotMountPath() throws FileNotFoundException {
      if (mount == null)
         throw new FileNotFoundException("Could not find dir: " + btrfsPath);
      // if (Backsnap.TIMESHIFT.get()) {
      Optional<Mount> om=mount.pc().getTimeshiftBase();
      if (om.isPresent())
         if (om.get().devicePath().equals(mount.devicePath())) {
            Path rel=Path.of("/").relativize(btrfsPath);
            Path abs=om.get().mountPath().resolve(rel);
            return abs;
         }
      // }
      Path rel=mount.btrfsPath().relativize(btrfsPath);
      Path abs=mount.mountPath().resolve(rel);
      return abs;
   }
   /**
    * Search a mountpoint that fits for this snapshot
    * 
    * @param mount0
    *           suggested mountpoint
    * @param btrfsPath1
    *           needed path
    * @return
    * @throws IOException
    */
   static private Mount getMount(Mount mount0, Path btrfsPath1) throws IOException {
      if (btrfsPath1 == null)
         return null;
      Path b2=btrfsPath1;
      Mount erg=null; // default ?
      if (!b2.toString().contains("timeshift-btrfs")) {
         for (Mount mount1:mount0.pc().getMountList(false).values())
            if (mount0.devicePath().equals(mount1.devicePath())) // only from same device
               if (b2.startsWith(mount1.btrfsPath())) // only if same path or starts with the same path
                  if ((erg == null) || (erg.btrfsPath().getNameCount() < mount1.btrfsPath().getNameCount()))
                     erg=mount1;
      } else {
         for (Mount mount1:mount0.pc().getMountList(false).values())
            if (mount0.devicePath().equals(mount1.devicePath())) // only from same device
               if (b2.getFileName().equals(mount1.btrfsPath().getFileName())) // only if ends with the same path
                  if ((erg == null) || (erg.btrfsPath().getNameCount() < mount1.btrfsPath().getNameCount()))
                     erg=mount1;
      }
      if (erg == null)
         if (b2.toString().contains("ack"))
            return null;
      return erg;
   }
   public Stream<Entry<String, String>> getInfo() {
      Map<String, String> infoMap=new LinkedHashMap<>();
      infoMap.put("btrfsPath : ", btrfsPath.toString());
      infoMap.put("otime : ", otime);
      infoMap.put("uuid : ", uuid);
      infoMap.put("parent_uuid : ", parent_uuid);
      infoMap.put("received_uuid : ", received_uuid);
      infoMap.put("gen : ", gen.toString());
      infoMap.put("id : ", id.toString());
      return infoMap.entrySet().stream();
   }
   /**
    * @param parentSnapshot2
    * @param s
    * @param b
    * @throws IOException
    */
   static public void setReadonly(Snapshot parent, Snapshot snapshot, boolean ro) throws IOException {
      StringBuilder readonlySB=new StringBuilder();
      if (parent instanceof Snapshot p && p.btrfsPath().toString().contains("timeshift"))
         readonlySB.append(Btrfs.PROPERTY_SET).append(p.getSnapshotMountPath()).append(" ro ").append(ro).append(";");
      if (snapshot instanceof Snapshot s && s.btrfsPath().toString().contains("timeshift"))
         readonlySB.append(Btrfs.PROPERTY_SET).append(s.getSnapshotMountPath()).append(" ro ").append(ro).append(";");
      if (readonlySB.isEmpty())
         return;
      if (Backsnap.bsGui instanceof BacksnapGui gui)
         gui.getPanelMaintenance().updateButtons();
      String readonlyCmd=snapshot.mount().pc().getCmd(readonlySB, true);
      Log.logln(readonlyCmd, LEVEL.BTRFS);
      BTRFS.writeLock().lock();
      try (CmdStreams readonlyStream=CmdStreams.getDirectStream(readonlyCmd)) {
         readonlyStream.outBGerr().forEach(t -> Log.logln(t, LEVEL.BTRFS));
         if (readonlyStream.errLines().anyMatch(line -> line.contains("No route to host")
                  || line.contains("Connection closed") || line.contains("connection unexpectedly closed")))
            Backsnap.disconnectCount=10;
      } finally {
         BTRFS.writeLock().unlock();
      }
      if (Backsnap.bsGui instanceof BacksnapGui gui)
         gui.getPanelMaintenance().updateButtons();
   }
   /**
    * Setze das Readonly-Attribut dieses Snapshots
    * 
    * @param readonly
    * @throws IOException
    */
   public void setReadonly(boolean readonly) throws IOException {
      if (!isTimeshift())
         return;
      if (isReadonly() == readonly)
         return;
      BTRFS.writeLock().lock();
      readonlyL().clear(); // nicht weiter im cache halten
      try {
         String setReadonlyCmd=mount().pc().getCmd(
                  new StringBuilder(Btrfs.PROPERTY_SET).append(getSnapshotMountPath()).append(" ro ").append(readonly),
                  true);
         Log.logln(setReadonlyCmd, LEVEL.BTRFS);// if (!DRYRUN.get())
         try (CmdStreams setReadonlyStream=CmdStreams.getDirectStream(setReadonlyCmd)) {
            setReadonlyStream.outBGerr().forEach(t -> Log.logln(t, LEVEL.BTRFS));
            if (setReadonlyStream.errLines().anyMatch(line -> line.contains("No route to host")
                     || line.contains("Connection closed") || line.contains("connection unexpectedly closed")))
               Backsnap.disconnectCount=10;
         }
      } finally {
         BTRFS.writeLock().unlock();
      }
   }
   static public final String DOT_SNAPSHOTS=".snapshots";
   static public final String AT_SNAPSHOTS="@snapshots";
   @Deprecated
   static public void mkain(String[] args) {
      try {
         Flag.setArgs(args, Pc.SUDO + ":/" + DOT_SNAPSHOTS + " /mnt/BACKUP/" + AT_SNAPSHOTS + "/manjaro");// Par. sammeln
         String backupDir=Flag.getParameterOrDefault(1, "@BackSnap");
         String source=Flag.getParameter(0);
         String externSsh=source.contains(":") ? source.substring(0, source.indexOf(":")) : "";
         String sourceDir=externSsh.isBlank() ? source : source.substring(externSsh.length() + 1);
         if (externSsh.startsWith(Pc.SUDO))
            externSsh=Pc.SUDO_;
         if (externSsh.isBlank())
            externSsh=Pc.ROOT_LOCALHOST;
         if (sourceDir.endsWith(DOT_SNAPSHOTS))
            sourceDir=sourceDir.substring(0, sourceDir.length() - DOT_SNAPSHOTS.length());
         if (sourceDir.endsWith("//"))
            sourceDir=sourceDir.substring(0, sourceDir.length() - 2);
         // SrcVolume ermitteln
         SubVolumeList subVolumes=new SubVolumeList(Pc.getPc(externSsh));
         Mount srcVolume=subVolumes.mountTree().get(sourceDir);
         if (srcVolume == null)
            throw new RuntimeException(Backsnap.LF + "Could not find srcDir: " + sourceDir);
         if (srcVolume.btrfsMap().isEmpty())
            throw new RuntimeException(Backsnap.LF + "Ingnoring, because there are no snapshots in: " + sourceDir);
         Log.logln("backup snapshots from: " + srcVolume.keyM(), LEVEL.BASIC);
         subVolumes.pc();
         // BackupVolume ermitteln
         Mount backupMount=Pc.getBackupMount(/* true */);
         if (backupMount == null)
            throw new RuntimeException(Backsnap.LF + "Could not find backupDir: " + backupDir);
         Log.logln("Will try to use backupDir: " + backupMount.keyM(), LEVEL.BASIC);
         // Subdir ermitteln
         Path pathBackupDir=backupMount.mountPath().relativize(Path.of(backupDir));
         System.out.println(pathBackupDir);
         // Verifizieren !#
         if (!subVolumes.mountTree().isEmpty())
            for (Entry<String, Mount> e:subVolumes.mountTree().entrySet()) {
               Mount subv=e.getValue();
               if (!subv.btrfsMap().isEmpty()) {// interessant sind nur die Subvolumes mit snapshots
                  String commonName=subv.getCommonName();
                  System.out.println("Found snapshots for: " + e.getKey() + " at (" + commonName + ")");
                  for (Entry<Path, Snapshot> e4:subv.btrfsMap().entrySet())
                     if (e4.getValue() instanceof Snapshot s) // @Todo obsolet ?
                        System.out.println(" -> " + e4.getKey() + " -> " + s.dirName());
               } else
                  System.out.println("NO snapshots of: " + e.getKey());
            }
         subVolumes.pc();
         // Mount backupVolumeMount=Pc.getBackupVolumeMount();
         System.out.println(backupMount);
         System.exit(-9);
         List<Snapshot> snapshots=new ArrayList<>();
         StringBuilder subvolumeListCmd=new StringBuilder(Btrfs.SUBVOLUME_LIST_1).append(backupDir);
         if ((externSsh instanceof String x) && (!x.isBlank()))
            if (x.startsWith(Pc.SUDO_))
               subvolumeListCmd.insert(0, x);
            else
               subvolumeListCmd.insert(0, "ssh " + x + " '").append("'");
         System.out.println(subvolumeListCmd);
         try (CmdStreams std=CmdStreams.getDirectStream(subvolumeListCmd.toString())) {
            std.errBGout().forEach(line -> {
               try {
                  System.out.println(line);
               } catch (Exception e) {
                  System.err.println(e);
               }
            });
         } catch (IOException e) {
            throw e;
         } catch (Exception e) {
            e.printStackTrace();
         }
         for (Snapshot snapshot:snapshots) {
            if (snapshot.received_uuid() instanceof @SuppressWarnings("unused") String ru)
               System.out.println(snapshot.dirName() + " => " + snapshot.toString());
         }
//         CmdStreams.cleanup();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
