/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
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
      for (Mount mount:pc.getMountList(false).values()) {
         mountTree.put(mount.keyM(), mount);
         mount.populate();
      }
   }
   /**
    * @param vorschlag
    * @return
    */
//   public Mount getBackupVolume() {
//      String vorschlag=pc.extern() + ":" + Pc.MNT_BACKSNAP;
//      if (mountTree.containsKey(vorschlag))
//         return mountTree.get(vorschlag);
//      return null;
//   }
}
