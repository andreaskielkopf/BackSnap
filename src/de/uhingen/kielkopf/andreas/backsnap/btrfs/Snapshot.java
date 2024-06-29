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
import de.uhingen.kielkopf.andreas.beans.shell.CmdStreams;

/**
 * @author Andreas Kielkopf
 * 
 *         Snapshot (readony) oder Subvolume (writable)
 */
public record Snapshot(Mount mount, Integer id, Integer gen, Integer cgen, Integer parent, Integer top_level, //
         String otime, String parent_uuid, String received_uuid, String uuid, Path btrfsPath, Boolean[] readonly) {
   private static final Pattern ID=createPatternFor("ID");
   private static final Pattern GEN=createPatternFor("gen");
   private static final Pattern CGEN=createPatternFor("cgen");
   private static final Pattern PARENT=createPatternFor("parent");
   private static final Pattern TOP_LEVEL=createPatternFor("top level");
   private static final Pattern OTIME=Pattern.compile("[ \\[]" + "otime" + "[ =]([^ ]+ [^ ,\\]]+)");// [ =\\[]([^ ,\\]]+)
   private static final Pattern PARENT_UUID=createPatternFor("parent_uuid");
   private static final Pattern RECEIVED_UUID=createPatternFor("received_uuid");
   private static final Pattern UUID=createPatternFor("uuid");
   private static final Pattern BTRFS_PATH=Pattern.compile("^(?:.*? )path (?:<[^>]+>)?([^ ]+).*?$");
   private static final Pattern NUMERIC_DIRNAME=Pattern.compile("([0-9]+)/snapshot$");
   private static final Pattern DIRNAME=Pattern.compile("([^/]+)/snapshot$");
   // private static final Pattern SUBVOLUME=Pattern.compile("^(@[0-9a-zA-Z.]+)/.*[0-9]+/snapshot$");
   public Snapshot(Mount mount, String from_btrfs) throws IOException {
      this(getMount(mount, getPath(BTRFS_PATH.matcher(from_btrfs))), getInt(ID.matcher(from_btrfs)),
               getInt(GEN.matcher(from_btrfs)), getInt(CGEN.matcher(from_btrfs)), getInt(PARENT.matcher(from_btrfs)),
               getInt(TOP_LEVEL.matcher(from_btrfs)), //
               getString(OTIME.matcher(from_btrfs)), getString(PARENT_UUID.matcher(from_btrfs)),
               getString(RECEIVED_UUID.matcher(from_btrfs)), getString(UUID.matcher(from_btrfs)),
               getPath(BTRFS_PATH.matcher(from_btrfs)), new Boolean[1]);
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
      if (readonly[0] == null) { // dann müssen wir das erst mal ermitteln
         String getRoCmd=mount().pc()
                  .getCmd(new StringBuilder(Btrfs.PROPERTY_GET).append(getSnapshotMountPath()).append(" ro"), false);
         Log.logln(getRoCmd, LEVEL.BTRFS);
         BTRFS.readLock().lock();
         readonly[0]=false; // Das schlimmste annehmen
         try (CmdStreams getRoStream=CmdStreams.getDirectStream(getRoCmd)) {
            Optional<String> roFlag=getRoStream.outBGerr().peek(t -> Log.logln(t, LEVEL.BTRFS))
                     .filter(t -> t.startsWith("ro=")).findAny();
            if (getRoStream.errLines().anyMatch(line -> line.contains("No route to host")
                     || line.contains("Connection closed") || line.contains("connection unexpectedly closed")))
               Backsnap.disconnectCount=10;
            if (roFlag.isPresent())
               return readonly[0]=Boolean.parseBoolean(roFlag.get().split("=")[1]);
         } finally {
            BTRFS.readLock().unlock();
         }
      }
      return readonly[0];
   }
   public boolean isBackup() {
      return received_uuid().length() > 8;
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
   private static Mount getMount(Mount mount0, Path btrfsPath1) throws IOException {
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
   /** liefert die Info für die Anzeige mit hover */
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
    * Setze/lösche das Readonly-Attribut dieses Snapshots
    * 
    * Das wird nur für Snapshots erlaubt die Timeshisft angelegt hat
    * 
    * @param parentSnapshot2
    * @param s
    * @param b
    * @throws IOException
    */
   static public void setReadonly(Snapshot parent, Snapshot snapshot, boolean ro) throws IOException {
      StringBuilder readonlySB=new StringBuilder();
      if (parent instanceof Snapshot p && p.btrfsPath().toString().contains("timeshift") && (p.isReadonly() != ro)) {
         readonlySB.append(Btrfs.PROPERTY_SET).append(p.getSnapshotMountPath()).append(" ro ").append(ro).append(";");
         p.readonly[0]=null; // bisherigen Wert löschen
      }
      if (snapshot instanceof Snapshot s && s.btrfsPath().toString().contains("timeshift") && (s.isReadonly() != ro)) {
         readonlySB.append(Btrfs.PROPERTY_SET).append(s.getSnapshotMountPath()).append(" ro ").append(ro).append(";");
         s.readonly[0]=null;
      }
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
   static public final String SNAPSHOT="snapshot";
   static public final String DOT_SNAPSHOTS=".snapshots";
   static public final String AT_SNAPSHOTS="@snapshots";
}
