/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.backsnap.Backsnap.AT_SNAPSHOTS;
import static de.uhingen.kielkopf.andreas.backsnap.Backsnap.DOT_SNAPSHOTS;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;
import de.uhingen.kielkopf.andreas.beans.cli.Flag;

/**
 * @author Andreas Kielkopf
 *
 */
public record Snapshot(Integer id, Integer gen, Integer cgen, Integer parent, Integer top_level, //
         String otime, String parent_uuid, String received_uuid, String uuid, //
         Path path) {
   final static Pattern ID=createPatternFor("ID");
   final static Pattern GEN=createPatternFor("gen");
   final static Pattern CGEN=createPatternFor("cgen");
   final static Pattern PARENT=createPatternFor("parent");
   final static Pattern TOP_LEVEL=createPatternFor("top level");
   final static Pattern OTIME=Pattern.compile("[ \\[]" + "otime" + "[ =]([^ ]+ [^ ,\\]]+)");// [ =\\[]([^ ,\\]]+)
   final static Pattern PARENT_UUID=createPatternFor("parent_uuid");
   final static Pattern RECEIVED_UUID=createPatternFor("received_uuid");
   final static Pattern UUID=createPatternFor("uuid");
   final static Pattern PATH=createPatternFor("path");
   final static Pattern NUMERIC_DIRNAME=Pattern.compile("([0-9]+)/snapshot$");
   final static Pattern DIRNAME=Pattern.compile("([^/]+)/snapshot$");
   final static Pattern SUBVOLUME=Pattern.compile("^(@[0-9a-zA-Z.]+)/.*[0-9]+/snapshot$");
   public Snapshot(String from_btrfs) {
      this(getInt(ID.matcher(from_btrfs)), getInt(GEN.matcher(from_btrfs)), getInt(CGEN.matcher(from_btrfs)),
               getInt(PARENT.matcher(from_btrfs)), getInt(TOP_LEVEL.matcher(from_btrfs)), //
               getString(OTIME.matcher(from_btrfs)), getString(PARENT_UUID.matcher(from_btrfs)),
               getString(RECEIVED_UUID.matcher(from_btrfs)), getString(UUID.matcher(from_btrfs)),
               getPath(PATH.matcher(from_btrfs)));
   }
   /**
    * @param Matcher
    * @return String
    */
   public static String getString(Matcher m) {
      if (!m.find())
         return null;
      return m.group(1);
   }
   /**
    * @param Matcher
    * @return Integer
    */
   @SuppressWarnings("boxing")
   public static Integer getInt(Matcher m) {
      if (!m.find())
         return null;
      return Integer.parseUnsignedInt(m.group(1));
   }
   /**
    * @param Matcher
    * @return Path
    */
   public static Path getPath(Matcher m) {
      if (!m.find())
         return null;
      return Path.of(m.group(1));
   }
   private static Pattern createPatternFor(String s) {
      return Pattern.compile("^(?:.*[ \\[])?" + s + "[ =]([^ ,\\]]+)");
   }
   /**
    * @return Key um snapshot zu sortieren sofern im Pfad ein numerischer WERT steht
    */
   public String key() {
      final int SORT_LEN=8;                                       // Reicht 100 Jahre ???
      Matcher   m       =NUMERIC_DIRNAME.matcher(path.toString());
      if (!m.find())
         return "§" + path.toString();
      String name=m.group(1);
      if (name.length() >= SORT_LEN)
         return name;
      return ".".repeat(SORT_LEN - name.length()).concat(name); // ??? numerisch sortieren ;-)
   }
   public String dirName() {
      Matcher m=DIRNAME.matcher(path.toString());
      if (!m.find())
         return null;
      return m.group(1);
   }
   /**
    * @return Subvolume dieses Snapshots sofern im Pfad enthalten
    */
   public String subvolume() {
      Matcher m=SUBVOLUME.matcher(path.toString());
      if (!m.find())
         return "";
      return m.group(1);
   }
   public boolean isBackup() {
      return received_uuid().length() > 8;
   }
   public Path getPathOn(String root, List<SnapConfig> snapConfigs) {
      for (SnapConfig snapConfig:snapConfigs) {
         if (snapConfig.original().mountPoint().equals(root)) {
            String k=snapConfig.kopie().mountPoint();
            String w=this.dirName();
            Path   p=Path.of(k).resolve(w);
            // StringBuilder q =new StringBuilder(path.toString());
            return p;
         }
      }
      return null;
   }
   public static void main(String[] args) {
      try {
         Flag.setArgs(args, "sudo:/" + DOT_SNAPSHOTS + " /mnt/BACKUP/" + AT_SNAPSHOTS + "/manjaro");// Parameter
                                                                                                    // sammeln
         String backupDir=Flag.getParameterOrDefault(1, "@BackSnap");
         // List<Subvolume> quellen =new ArrayList<>();
         String source   =Flag.getParameter(0);
         String externSsh=source.contains(":") ? source.substring(0, source.indexOf(":")) : "";
         String sourceDir=externSsh.isBlank() ? source : source.substring(externSsh.length() + 1);
         if (externSsh.startsWith("sudo"))
            externSsh="sudo ";
         if (externSsh.isBlank())
            externSsh="root@localhost";
         // SnapTree snapTree=new SnapTree("/", externSsh);
         if (sourceDir.endsWith(DOT_SNAPSHOTS))
            sourceDir=sourceDir.substring(0, sourceDir.length() - DOT_SNAPSHOTS.length());
         if (sourceDir.endsWith("//"))
            sourceDir=sourceDir.substring(0, sourceDir.length() - 2);
         // SrcVolume ermitteln
         SubVolumeList subVolumes=new SubVolumeList(externSsh);
         Subvolume     srcVolume =subVolumes.subvTree().get(sourceDir);
         if (srcVolume == null)
            throw new RuntimeException("Could not find srcDir: " + sourceDir);
         if (srcVolume.snapshotTree().isEmpty())
            throw new RuntimeException("Ingnoring, because there are no snapshots in: " + sourceDir);
         System.out.println("backup snapshots from: " + srcVolume.key());
         // BackupVolume ermitteln
         Subvolume backupVolume=subVolumes.getBackupVolume(backupDir);
         if (backupVolume == null)
            throw new RuntimeException("Could not find backupDir: " + backupDir);
         System.out.println("Will try to use backupDir: " + backupVolume.key());
         // Subdir ermitteln
         Path pbd =Path.of(backupDir);
         Path pbv =Path.of(backupVolume.key());
         Path pbsd=pbv.relativize(pbd);
         System.out.println(pbsd);
         // Verifizieren !#
         if (!subVolumes.subvTree().isEmpty())
            for (Entry<String, Subvolume> e:subVolumes.subvTree().entrySet()) {
               Subvolume subv=e.getValue();
               if (!subv.snapshotTree().isEmpty()) {// interessant sind nur die Subvolumes mit snapshots
                  String commonName=subv.getCommonName();
                  System.out.println("Found snapshots for: " + e.getKey() + " at (" + commonName + ")");
                  for (Entry<String, Snapshot> e4:subv.snapshotTree().entrySet())
                     System.out.println(" -> " + e4.getKey() + " -> " + e4.getValue().key()); // System.out.println();
               } else
                  System.out.println("NO snapshots of: " + e.getKey());
            }

         Subvolume bbv=subVolumes.getBackupVolume(null);
         System.out.println(bbv);
         System.exit(-9);
         List<Snapshot> snapshots=new ArrayList<>();
         StringBuilder  cmd      =new StringBuilder("btrfs subvolume list -spuqR ").append(backupDir);
         if ((externSsh instanceof String x) && (!x.isBlank()))
            if (x.startsWith("sudo "))
               cmd.insert(0, x);
            else
               cmd.insert(0, "ssh " + x + " '").append("'");
         System.out.println(cmd);
         try (CmdStream std=Commandline.execute(cmd)) {
            std.backgroundErr();
            std.erg().forEach(line -> {
               try {
                  System.out.println(line);
                  snapshots.add(new Snapshot(" " + line));
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
            if (snapshot.received_uuid() instanceof String ru)
               // if (!ru.startsWith("-"))
               System.out.println(snapshot.key() + " => " + snapshot.toString());
         }
         Commandline.cleanup();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
