/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.backsnap.btrfs.Snapshot.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;

/**
 * @author Andreas Kielkopf results of mount | grep -E 'btrfs' as records
 */
public record Mount(SubVolumeList mountList, Path devicePath, Path mountPath, Path btrfsPath, String options,
         ConcurrentSkipListMap<Path, Snapshot> snapshotMap, ConcurrentSkipListSet<String> name) {
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
      Matcher m=COMMON.matcher(snapshotMap.firstEntry().getKey().toString());
      if (!m.find())
         return null;
      String c=m.group(1);
      for (Path key:snapshotMap.keySet())
         if (!key.startsWith(c))
            return null;
      return c;
   }
   /**
    * @return key specifiing this mount on this machine with mountPath
    */
   public String keyM() {
      return mountList.extern() + ":" + mountPath;
   }
   /**
    * @return key specifiing this mount on this machine with devicePath
    */
   public String keyD() {
      return mountList.extern() + ":" + devicePath;
   }
   /**
    * @param line
    *           Eine Zeile die mount geliefert hat
    * @param extern
    *           um den Zugriff über ssh zu ermöglichen
    * @throws IOException
    */
   public Mount(SubVolumeList mountList, String line) throws IOException {
      this(mountList, getPath(DEVICE.matcher(line)), getPath(MOUNTPOINT.matcher(line)),
               getPath(SUBVOLUME.matcher(line)), getString(OPTIONS.matcher(line)), new ConcurrentSkipListMap<>(),
               new ConcurrentSkipListSet<>());
      // populate(); erstmal unvollständig erzeugen
   }
   /**
    * Nachschauen, ob dieser Mount/Subvolume snapshots hat
    * 
    * @param snapTree
    * @throws IOException
    */
   public void populate() throws IOException {
      SnapTree      snapTree         =SnapTree.getSnapTree(this);
      boolean       snapTreeVorhanden=(snapTree instanceof SnapTree st) ? !st.dateMap().isEmpty() : false;
      StringBuilder btrfsCmd         =new StringBuilder("btrfs subvolume show ").append(mountPath);
      if ((mountList.extern() instanceof String x) && (!x.isBlank()))
         if (x.startsWith("sudo "))
            btrfsCmd.insert(0, x);
         else
            btrfsCmd.insert(0, "ssh " + x + " '").append("'");
      System.out.println(btrfsCmd);
      try (CmdStream snapshotList=Commandline.executeCached(btrfsCmd, keyM())) {
         snapshotList.backgroundErr();
         snapshotList.erg().forEach(line -> {
            Matcher mn=NAME.matcher(line);
            if (mn.find())
               name.add(mn.group(1));// store Name: ...
            else
               if (snapTreeVorhanden) {
                  Matcher ms=SNAPSHOT.matcher(line);
                  if (ms.find()) {
                     Path     btrfsPath1=Path.of("/", ms.group(1));
                     Snapshot zeiger    =snapTree.btrfsPathMap().get(btrfsPath1);
                     if (zeiger != null) {
                        if (!zeiger.mount().mountPath.startsWith(this.mountPath))
                           System.err.println("Mount passt nicht für: " + this + " -> " + zeiger);
                        snapshotMap.put(btrfsPath1, zeiger);
                     } else
                        System.out.println("Not visible: " + btrfsPath1);
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
      StringBuilder sb=new StringBuilder("Mount [").append(mountList.extern()).append(":").append(devicePath)
               .append(" -> ").append(mountPath);
      if (!name.isEmpty())
         sb.append("(").append(name.first()).append(":").append(snapshotMap.size()).append(")");
      sb.append("]");
      return sb.toString();
   }
}
