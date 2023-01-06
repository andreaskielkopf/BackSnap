/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.backsnap.btrfs.Snapshot.getString;

import java.io.IOException;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;

/**
 * @author Andreas Kielkopf
 *
 */
public record Subvolume(String device, String mountPoint, String subvol, String options, String extern,
         TreeMap<String, Snapshot> snapshotTree) {
   final static Pattern DEVICE=Pattern.compile("^(?:.*[ \\[]device=)?([^ ,]+)");
   final static Pattern MOUNTPOINT=Pattern.compile("(?: on |[ \\[]mountPoint=)([^ ,]+)");
   final static Pattern SUBVOLUME=Pattern.compile("(?:, ?subvol=)([^ ,)\\]]+)");
   final static Pattern OPTIONS=Pattern.compile("(?:[\\[]options=| )(\\(.+\\))");
   final static Pattern SNAPSHOT=Pattern.compile("^\t\t+([^ \t]+)");
   final static Pattern COMMON=Pattern.compile("^(@[^/]*/)");
   /**
    * @return key zum sortieren
    */
   /**
    * @return gemeinsamen Start des Pfads
    */

   public String getCommonName() {
      if (snapshotTree.isEmpty())
         return null;
      String  k=snapshotTree.firstEntry().getKey();
      Matcher m=COMMON.matcher(k);
      if (!m.find())
         return null;
      String c=m.group(1);
      for (String key:snapshotTree.keySet())
         if (!key.startsWith(c))
            return null;
      return c;
   }
   public String key() {
      return mountPoint;
   }
   /**
    * @param line
    *           Eine Zeile die mount geliefert hat
    * @param extern
    *           um den Zugriff über ssh zu ermöglichen
    */
   public Subvolume(String line, String extern) {
      this(getString(DEVICE.matcher(line)), getString(MOUNTPOINT.matcher(line)), getString(SUBVOLUME.matcher(line)),
               getString(OPTIONS.matcher(line)), extern, new TreeMap<>());
      populate();
   }
   /**
    * Nachschauen, ob dieses Subvolume snapshots hat
    * 
    * @param snapTree
    */
   void populate() {
      SnapTree      snapTree         =new SnapTree(mountPoint, extern);
      boolean       snapTreeVorhanden=(snapTree instanceof SnapTree st) ? !st.fileMap().isEmpty() : false;
      StringBuilder btrfsCmd         =new StringBuilder("btrfs subvolume show ").append(mountPoint);
      if ((extern instanceof String x) && (!x.isBlank()))
         if (x.startsWith("sudo "))
            btrfsCmd.insert(0, x);
         else
            btrfsCmd.insert(0, "ssh " + x + " '").append("'");
      System.out.println(btrfsCmd);
      try (CmdStream snapshotList=Commandline.execute(btrfsCmd)) {
         Future<?> errorHandling=Commandline.background.submit(() -> {// Some Error handling in background
            if (snapshotList.err().peek(System.err::println).anyMatch(line -> {
               return line.contains("No route to host") || line.contains("Connection closed")
                        || line.contains("connection unexpectedly closed");
            }))
               throw new IOException("connection unexpectedly closed");
            return "";
         });
         snapshotList.erg().forEach(line -> {
            Matcher m=SNAPSHOT.matcher(line);
            if (m.find()) {
               Snapshot zeiger=null;
               if (snapTreeVorhanden)
                  zeiger=snapTree.fileMap().get(m.group(1));
               snapshotTree.put(m.group(1), zeiger);
            }
         });
         errorHandling.get();
      } catch (IOException | ExecutionException | InterruptedException e) {
         e.printStackTrace();
      }
   }
}