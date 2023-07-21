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

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;

/**
 * @author Andreas Kielkopf results of mount | grep -E 'btrfs' as records
 */
public record Mount(Pc pc, /* SubVolumeList svList, */ Path devicePath, Path mountPath, Path btrfsPath, String options,
         ConcurrentSkipListMap<Path, Snapshot> btrfsMap, ConcurrentSkipListMap<String, Snapshot> otimeKeyMap,
         ConcurrentSkipListSet<String> name) {
   static final Pattern DEVICE=Pattern.compile("^(?:.*[ \\[]device=)?([^ ,]+)");
   static final Pattern MOUNTPOINT=Pattern.compile("(?: on |[ \\[]mountPoint=)([^ ,]+)");
   static final Pattern SUBVOLUME=Pattern.compile("(?:, ?subvol=)([^ ,)\\]]+)");
   static final Pattern OPTIONS=Pattern.compile("(?:[\\[]options=| )(\\(.+\\))");
   static final Pattern SNAPSHOT=Pattern.compile("^\t\t+([^ \t]+)");
   static final Pattern NAME=Pattern.compile("Name:[ \\t]+([^< \\t]+)");
   static final Pattern COMMON=Pattern.compile("^(/@[^/]*/)");
   /**
    * @param line
    *           Eine Zeile die mount geliefert hat
    * @param extern
    *           um den Zugriff über ssh zu ermöglichen
    * @throws IOException
    */
   protected Mount(Pc pc, String line) throws IOException {
      this(pc, /* svList, */ getPath(DEVICE.matcher(line)), getPath(MOUNTPOINT.matcher(line)),
               getPath(SUBVOLUME.matcher(line)), getString(OPTIONS.matcher(line)), new ConcurrentSkipListMap<>(),
               new ConcurrentSkipListMap<>(), new ConcurrentSkipListSet<>());
      // populate(); erstmal unvollständig erzeugen
   }
   /**
    * @return gemeinsamen Start des Pfads
    */
   public String getCommonName() {
      if (btrfsMap.isEmpty())
         return null;
      Matcher m=COMMON.matcher(btrfsMap.firstEntry().getKey().toString());
      if (!m.find())
         return null;
      String c=m.group(1);
      for (Path key:btrfsMap.keySet())
         if (!key.startsWith(c))
            return null;
      return c;
   }
   /**
    * @return key specifiing this mount on this machine with mountPath
    */
   public String keyM() {
      return pc.extern() + ":" + mountPath;
   }
   /**
    * @return key specifiing this mount on this machine with devicePath
    */
   public String keyD() {
      return pc.extern() + ":" + devicePath;
   }
   /**
    * Nachschauen, ob dieser Mount/Subvolume snapshots hat
    * 
    * @param snapTree
    * @throws IOException
    */
   void populate() throws IOException {
      SnapTree      snapTree         =SnapTree.getSnapTree(this);
      boolean       snapTreeVorhanden=(snapTree instanceof SnapTree st) ? !st.dateMap().isEmpty() : false;
      StringBuilder subvolumeShowSB  =new StringBuilder("btrfs subvolume show ").append(mountPath);
      String        subvolumeSchowCmd=pc.getCmd(subvolumeShowSB);
      Backsnap.logln(3, subvolumeSchowCmd);
      try (CmdStream snapshotStream=Commandline.executeCached(subvolumeSchowCmd, keyM())) {
         snapshotStream.backgroundErr();
         snapshotStream.erg().forEach(line -> {
            Backsnap.logln(9, line);
            Matcher mn=NAME.matcher(line);
            if (mn.find())
               name.add("/" + mn.group(1));// store Name: ...
            else
               if (snapTreeVorhanden) {
                  Matcher ms=SNAPSHOT.matcher(line);
                  if (ms.find()) {
                     Path     btrfsPath1=Path.of("/", ms.group(1));
                     Snapshot snapshot  =snapTree.btrfsPathMap().get(btrfsPath1);
                     if ((snapshot != null) && (snapshot.mount() != null)) {
                        if (!snapshot.mount().mountPath.startsWith(this.mountPath))
                           System.err.println("Mount passt nicht für: " + this + " -> " + snapshot);
                        btrfsMap.put(btrfsPath1, snapshot);
                        otimeKeyMap.put(snapshot.keyO(), snapshot);
                     } else {
                        System.out.println("Not visible: " + btrfsPath1);
                        if (snapshot != null)
                           otimeKeyMap.put(snapshot.keyO(), snapshot);
                     }
                  }
               }
         });
         if (name.isEmpty())
            name.add("/");
         snapshotStream.waitFor();
         for (String line:snapshotStream.errList())
            if (line.contains("No route to host") || line.contains("Connection closed")
                     || line.contains("connection unexpectedly closed"))
               throw new IOException(line);
      }
   }
   public void updateSnapshots() throws IOException {
      SnapTree      snapTree         =SnapTree.getSnapTree(this);
      boolean       snapTreeVorhanden=(snapTree instanceof SnapTree st) ? !st.dateMap().isEmpty() : false;
      StringBuilder subvolumeShowSB  =new StringBuilder("btrfs subvolume show ").append(mountPath);
      String        subvolumeShowCmd =pc.getCmd(subvolumeShowSB);
      Backsnap.logln(3, subvolumeShowCmd);
      try (CmdStream snapshotStream=Commandline.executeCached(subvolumeShowCmd, keyM())) {
         snapshotStream.backgroundErr();
         snapshotStream.erg().forEach(line -> {
            Backsnap.logln(9, line);
            Matcher mn=NAME.matcher(line);
            if (mn.find())
               name.add("/" + mn.group(1));// store Name: ...
            else
               if (snapTreeVorhanden) {
                  Matcher ms=SNAPSHOT.matcher(line);
                  if (ms.find()) {
                     Path     btrfsPath1=Path.of("/", ms.group(1));
                     Snapshot snapshot  =snapTree.btrfsPathMap().get(btrfsPath1);
                     if ((snapshot != null) && (snapshot.mount() != null)) {
                        if (!snapshot.mount().mountPath.startsWith(this.mountPath))
                           System.err.println("Mount passt nicht für: " + this + " -> " + snapshot);
                        btrfsMap.put(btrfsPath1, snapshot);
                        otimeKeyMap.put(snapshot.keyO(), snapshot);
                     } else {
                        System.out.println("Not visible: " + btrfsPath1);
                        if (snapshot != null)
                           otimeKeyMap.put(snapshot.keyO(), snapshot);
                     }
                  }
               }
         });
         if (name.isEmpty())
            name.add("/");
         snapshotStream.waitFor();
         for (String line:snapshotStream.errList())
            if (line.contains("No route to host") || line.contains("Connection closed")
                     || line.contains("connection unexpectedly closed"))
               throw new IOException(line);
      }
   }
   @Override
   public String toString() {
      StringBuilder sb=new StringBuilder("Mount [").append(pc.extern()).append(":").append(devicePath).append(" -> ")
               .append(mountPath).append("(");
      if (!name.isEmpty())
         sb.append(name.first());
      sb.append(":").append(btrfsMap.size()).append(")").append("]");
      return sb.toString();
   }
   /**
    * @return
    * @throws IOException
    * 
    */
   public SnapTree getSnapTree() throws IOException {
      return new SnapTree(this);
   }
}
