/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.backsnap.btrfs.Snapshot.getString;

import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;

/**
 * @author Andreas Kielkopf
 *
 */
public record Mount(SubVolumeList mountList, String device, String mountPoint, String subvol, String options,
         String oextern, ConcurrentSkipListMap<String, Snapshot> snapshotMap, ConcurrentSkipListSet<String> namen) {
   final static Pattern DEVICE=Pattern.compile("^(?:.*[ \\[]device=)?([^ ,]+)");
   final static Pattern MOUNTPOINT=Pattern.compile("(?: on |[ \\[]mountPoint=)([^ ,]+)");
   final static Pattern SUBVOLUME=Pattern.compile("(?:, ?subvol=)([^ ,)\\]]+)");
   final static Pattern OPTIONS=Pattern.compile("(?:[\\[]options=| )(\\(.+\\))");
   final static Pattern SNAPSHOT=Pattern.compile("^\t\t+([^ \t]+)");
   final static Pattern NAME=Pattern.compile("Name:[ \\t]+([^ \\t]+)");
   final static Pattern COMMON=Pattern.compile("^(/@[^/]*/)");
   /**
    * @return gemeinsamen Start des Pfads
    */
   public String getCommonName() {
      if (snapshotMap.isEmpty())
         return null;
      Matcher m=COMMON.matcher(snapshotMap.firstEntry().getKey());
      if (!m.find())
         return null;
      String c=m.group(1);
      for (String key:snapshotMap.keySet())
         if (!key.startsWith(c))
            return null;
      return c;
   }
   /**
    * @return key zum sortieren
    */
   public String key() {
      return mountList.extern() + ":" + mountPoint;
   }
   /**
    * @param line
    *           Eine Zeile die mount geliefert hat
    * @param extern
    *           um den Zugriff über ssh zu ermöglichen
    * @throws IOException
    */
   public Mount(SubVolumeList mountList, String line, String extern) throws IOException {
      this(mountList, getString(DEVICE.matcher(line)), getString(MOUNTPOINT.matcher(line)),
               getString(SUBVOLUME.matcher(line)), getString(OPTIONS.matcher(line)), extern,
               new ConcurrentSkipListMap<>(), new ConcurrentSkipListSet<>());
      populate();
   }
   /**
    * Nachschauen, ob dieses Mount snapshots hat
    * 
    * @param snapTree
    * @throws IOException
    */
   void populate() throws IOException {
      SnapTree      snapTree         =SnapTree.getSnapTree(this/* , mountPoint, oextern */);
      boolean       snapTreeVorhanden=(snapTree instanceof SnapTree st) ? !st.dateMap().isEmpty() : false;
      StringBuilder btrfsCmd         =new StringBuilder("btrfs subvolume show ").append(mountPoint);
      if ((oextern instanceof String x) && (!x.isBlank()))
         if (x.startsWith("sudo "))
            btrfsCmd.insert(0, x);
         else
            btrfsCmd.insert(0, "ssh " + x + " '").append("'");
      System.out.println(btrfsCmd);
      String extern  =mountList.extern();
      String cacheKey=extern + ":" + mountPoint;
      try (CmdStream snapshotList=Commandline.executeCached(btrfsCmd, cacheKey)) {
         snapshotList.backgroundErr();
         snapshotList.erg().forEach(line -> {
            Matcher mn=NAME.matcher(line);
            if (mn.find()) {
               String name=mn.group(1);
               namen.add(name);// "<FS_TREE>" wenn / gemountet ist
            } else {
               Matcher m=SNAPSHOT.matcher(line);
               if (m.find()) {
                  Snapshot zeiger=null;
                  String   p     ="/" + m.group(1);
                  if (snapTreeVorhanden) {
                     zeiger=snapTree.pathMap().get(p);
                     if (zeiger == null) {
                        System.out.println(p);
                        if (p.startsWith("@/"))
                           p=p.substring(2);
                        zeiger=snapTree.pathMap().get(p);
                        if (zeiger == null)
                           System.out.println(p);
                     }
                  }
                  snapshotMap.put(p, zeiger);
               }
            }
         });
         snapshotList.waitFor();
         for (String line:snapshotList.errList())
            if (line.contains("No route to host") || line.contains("Connection closed")
                     || line.contains("connection unexpectedly closed"))
               throw new IOException(line);
      }
   }
   @Override
   public String toString() {
      StringBuilder sb=new StringBuilder("Mount [").append(mountList.extern()).append(":").append(device).append(" -> ")
               .append(mountPoint);
      if (!namen.isEmpty()) {
         sb.append("(").append(namen.first()).append(":").append(snapshotMap.size()).append(")");
      }
      sb.append("]");
      return sb.toString();
   }
}
