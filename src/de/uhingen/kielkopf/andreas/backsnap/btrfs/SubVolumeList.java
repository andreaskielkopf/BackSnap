/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Create a List of all subVolumes of this pc, that are mounted explicit
 * 
 * @author Andreas Kielkopf
 *
 */
public record SubVolumeList(Pc pc, ConcurrentSkipListMap<String, Mount> mountTree) {
   /**
    * @param string
    *           mit Zugang zum PC
    * @param snapTree
    *           Liste mit Snapshots dieses PCs
    * @throws IOException
    */
   public SubVolumeList(Pc pc) throws IOException {
      this(pc, new ConcurrentSkipListMap<>());
      populate();
   }
   /**
    * Ermittle alle Subvolumes die Snapshots enthalten indem "mount" gefragt wird
    * 
    * @param snapTree
    * @throws IOException
    */
   private void populate() throws IOException {
      for (Mount mount:Mount.getMountList(pc, this).values()) {
         mountTree.put(mount.keyM(), mount);
         mount.populate();
      }
      // StringBuilder mountCmd=new StringBuilder("mount|grep btrfs");
      // if (!extern.isBlank())
      // if (extern.startsWith("sudo"))
      // mountCmd.insert(0, extern);
      // else
      // mountCmd.insert(0, "ssh " + extern + " '").append("'");
      // Backsnap.logln(3, mountCmd.toString());
      // try (CmdStream mountList=Commandline.executeCached(mountCmd)) {
      // mountList.backgroundErr();
      // for (String line:mountList.erg().toList())
      // mountTree.put(new Mount(this, line).keyM(), new Mount(this, line));
      // for (Mount mount:mountTree.values())
      // mount.populate(); // Snapshots zuweisen
      // mountList.waitFor();
      // for (String line:mountList.errList())
      // if (line.contains("No route to host") || line.contains("Connection closed")
      // || line.contains("connection unexpectedly closed"))
      // throw new IOException(line);
      // Backsnap.logln(3, "");
      // }
   }
   /**
    * @param vorschlag
    * @return
    */
   public Mount getBackupVolume(String vorschlag) {
      if (mountTree.containsKey(vorschlag))
         return mountTree.get(vorschlag);
      Entry<String, Mount> test=null;
      for (Entry<String, Mount> e:mountTree.entrySet())
         if (vorschlag.startsWith(e.getKey())) // find the longest matching key
            if ((test == null) || (test.getKey().length() < e.getKey().length()))
               test=e;
      if (test != null)
         return test.getValue();
      List<Mount> treffer=new ArrayList<>();
      for (Entry<String, Mount> e:mountTree.entrySet())
         if (e.getKey().contains(vorschlag))
            treffer.add(e.getValue());
      if (treffer.size() == 1)
         return treffer.get(0);
      Path p =Paths.get(vorschlag);
      Path pp=p.getParent();
      if (pp.getParent() == null)
         return null;
      Mount bv=getBackupVolume(pp.toString());
      try {
         bv.populate();
      } catch (IOException e1) {/* */ }
      return bv;
   }
}
