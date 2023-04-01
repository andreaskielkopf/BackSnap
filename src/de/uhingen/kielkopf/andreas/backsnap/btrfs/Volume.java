/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.backsnap.btrfs.Snapshot.getString;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;

import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;

/**
 * @author Andreas Kielkopf
 *
 */
public record Volume(String extern, String device, String label, String uuid) {
   final static Pattern VOLUMELABEL=Pattern.compile("^Label: ('.+')");
   final static Pattern UUID=Pattern.compile("uuid: ([-0-9a-f]{36})");
   final static Pattern DEVICE=Pattern.compile("devid:.+ (/dev/.+)");
   /** @return key zum sortieren */
   // public String key() { return mountList.extern() + ":" + mountPoint; }
   /**
    * @param line
    *           Eine Zeile die filesystem show geliefert hat
    * @param extern
    *           um den Zugriff über ssh zu ermöglichen
    * @throws IOException
    */
   public Volume(String extern, String line1, String line3) throws IOException {
      this(extern, getString(DEVICE.matcher(line3)), getString(VOLUMELABEL.matcher(line1)),
               getString(UUID.matcher(line1)));
      populate();
   }
   /**
    * Nachschauen, ob dieses Mount snapshots hat
    * 
    * @param snapTree
    * @throws IOException
    */
   void populate() throws IOException {
      System.out.println(this);
   }
   @Override
   public String toString() {
      StringBuilder sb=new StringBuilder("Volume [").append(extern).append(":").append(device).append(" uuid[")
               .append(uuid).append("]");
      return sb.toString();
   }
   public static ConcurrentSkipListMap<String, Volume> getList(String extern, boolean onlyMounted) {
      // @todo cache einbauen
      ConcurrentSkipListMap<String, Volume> list             =new ConcurrentSkipListMap<>();
      StringBuilder                         filesystemShowCmd=new StringBuilder("btrfs filesystem show -")
               .append(onlyMounted ? "m" : "d");
      injectSsh(filesystemShowCmd, extern);
      System.out.println(filesystemShowCmd);
      String cacheKey=filesystemShowCmd.toString();
      try (CmdStream volumeList=Commandline.executeCached(filesystemShowCmd, cacheKey)) {
         volumeList.backgroundErr();
         List<String> lines=volumeList.erg().toList();
         for (int i=0; i < lines.size() - 3; i++)
            if (lines.get(i).startsWith("Label:")) {
               try {
                  Volume v;
                  v=new Volume(extern, lines.get(i), lines.get(i + 2));
                  list.put(v.device, v);
               } catch (IOException e) {
                  e.printStackTrace();
               }
            }
      } catch (IOException e1) {
         e1.printStackTrace();
      }
      return list;
   }
   /**
    * @param filesystemShowCmd
    * @param extern2
    */
   public static void injectSsh(StringBuilder cmd, String extern) {
      if ((extern instanceof String x) && (!x.isBlank()))
         if (x.startsWith("sudo "))
            cmd.insert(0, x);
         else
            cmd.insert(0, "ssh " + x + " '").append("'");
   }
}
