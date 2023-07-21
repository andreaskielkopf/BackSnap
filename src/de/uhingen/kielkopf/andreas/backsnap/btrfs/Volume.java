/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.backsnap.btrfs.Snapshot.getPath;
import static de.uhingen.kielkopf.andreas.backsnap.btrfs.Snapshot.getString;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * @author Andreas Kielkopf
 *
 */
public record Volume(Pc pc, Path device, String label, String uuid) {
   static final Pattern VOLUMELABEL=Pattern.compile("^Label: ('.+'|none)");
   static final Pattern UUID=Pattern.compile("uuid: ([-0-9a-f]{36})");
   static final Pattern DEVICE=Pattern.compile("devid .+ path (/dev/.+)");
   /**
    * @param line
    *           Eine Zeile die filesystem show geliefert hat
    * @param extern
    *           um den Zugriff über ssh zu ermöglichen
    * @throws IOException
    */
   public Volume(Pc pc, String line1, String line3) throws IOException {
      this(pc, getPath(DEVICE.matcher(line3)), getString(VOLUMELABEL.matcher(line1)),
               getString(UUID.matcher(line1)));
//      populate();
   }
   /**
    * Nachschauen, ob dieses Mount snapshots hat
    * 
    * @param snapTree
    * @throws IOException
    */
//   void populate() throws IOException {
//      System.out.println(this);
//   }
//   public String extern() {
//      return (extern != null) ? extern : "local";
//   }
   @Override
   public String toString() {
      StringBuilder sb=new StringBuilder("Volume [")//
               .append("uuid=").append(uuid()).append(" ")//
               .append(pc.extern()).append(":").append(device())//
               .append(" ").append(label()).append("]");
      return sb.toString();
   }
//   static public ConcurrentSkipListMap<String, Volume> getList(String extern, boolean onlyMounted) {
//      // @todo cache einbauen
//      ConcurrentSkipListMap<String, Volume> list             =new ConcurrentSkipListMap<>();
//      StringBuilder                         filesystemShowCmd=new StringBuilder("btrfs filesystem show -")
//               .append(onlyMounted ? "m" : "d");
//      injectSsh(filesystemShowCmd, extern);
//      Backsnap.logln(7, filesystemShowCmd.toString());
//      String cacheKey=filesystemShowCmd.toString();
//      try (CmdStream volumeList=Commandline.executeCached(filesystemShowCmd, cacheKey)) {
//         volumeList.backgroundErr();
//         List<String> lines=volumeList.erg().toList();
//         for (int i=0; i < lines.size() - 3; i++) {
//            String line1=lines.get(i);
//            if (line1.startsWith("Label:")) {
//               try {
//                  String line3=lines.get(i + 2);
//                  Volume v;
//                  v=new Volume(extern, line1, line3);
//                  String key=v.uuid() + v.device();
//                  list.put(key, v);
//               } catch (IOException e) {
//                  e.printStackTrace();
//               }
//            }
//         }
//      } catch (IOException e1) {
//         e1.printStackTrace();
//      }
//      return list;
//   }
   /**
    * @param filesystemShowCmd
    * @param extern2
    * @deprecated
    */
//   @Deprecated
//   static public void injectSsh(StringBuilder cmd, String extern) {
//      if ((extern instanceof String x) && (!x.isBlank()))
//         if (x.startsWith("sudo "))
//            cmd.insert(0, x);
//         else
//            cmd.insert(0, "ssh " + x + " '").append("'");
//   }
}
