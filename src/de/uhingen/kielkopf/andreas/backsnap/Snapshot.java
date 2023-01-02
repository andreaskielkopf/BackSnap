/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
   static String getString(Matcher m) {
      if (!m.find())
         return null;
      return m.group(1);
   }
   /**
    * @param Matcher
    * @return Integer
    */
   @SuppressWarnings("boxing")
   static Integer getInt(Matcher m) {
      if (!m.find())
         return null;
      return Integer.parseUnsignedInt(m.group(1));
   }
   /**
    * @param Matcher
    * @return Path
    */
   static Path getPath(Matcher m) {
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
   /**
    * @return Subvolume dieses Snapshots sofern im Pfad enthalten
    */
   public String subvolume() {
      Matcher m=SUBVOLUME.matcher(path.toString());
      if (!m.find())
         return "";
      return m.group(1);
   }
   public static void main(String[] args) {
      try {
         Flag.setArgs(args, "/mnt/BACKUP");
         String        backupDir=Flag.getParameter(0);
         SubVolumeList sv       =new SubVolumeList("root@localhost");
         if (!sv.subvTree().isEmpty()) {
            Entry<String, Subvolume> d =sv.subvTree().firstEntry();
            SnapTree                 st=new SnapTree("/", d.getValue().extern());
            for (Entry<String, Subvolume> e:sv.subvTree().entrySet()) {
               Subvolume subv=e.getValue();
               System.out.println("Found snapshots for: " + e.getKey());
               // for (Entry<String, Snapshot> e2:subv.snapshotTree().entrySet())
               // System.out.println(" -> " + e2.getKey());
               // String mp=subv.mountPoint();
               // for (Entry<String, Snapshot> e3:st.fileMap().entrySet())
               // System.out.println(e3);
               @SuppressWarnings("unchecked")
               Set<Entry<String, Snapshot>> c=((TreeMap<String, Snapshot>) subv.snapshotTree().clone()).entrySet();
               for (Entry<String, Snapshot> e4:c) {
                  String mp1=e4.getKey();
                  System.out.print(" -> " + mp1);
                  for (Entry<String, Snapshot> e5:st.fileMap().entrySet()) {
                     Snapshot v  =e5.getValue();
                     String   mp2=v.path.toString();
                     if (mp1.equals(mp2)) {
                        System.out.print(" -> " + v.key());
                        subv.snapshotTree().put(mp1, v);
                        break;
                     }
                  }
                  System.out.println();
               }
            }
         }
         System.exit(-9);
         // System.out.println(st);
         // Path path =Paths.get(backupDir);
         StringBuilder cmd=new StringBuilder("ssh root@localhost 'btrfs subvolume list -spuqR ");
         cmd.append(backupDir);
         cmd.append("'");
         System.out.println(cmd);
         List<Snapshot> snapshots=new ArrayList<>();
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
         CmdStream.cleanup();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
