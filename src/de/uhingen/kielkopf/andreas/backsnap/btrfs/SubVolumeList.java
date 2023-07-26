/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListMap;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;

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
   public Mount getBackupVolume(/* String backupDir */) {
      // if (!backupDir.contains(""))
      // return null;
      String vorschlag=pc.extern() + ":" + Backsnap.MNT_BACKSNAP;
      if (mountTree.containsKey(vorschlag))
         return mountTree.get(vorschlag);
      return null;
      // String vorschlag2=Paths.get(vorschlag).getParent().toString();
      // if (mountTree.containsKey(vorschlag2))
      // return mountTree.get(vorschlag2);
      // Entry<String, Mount> test=null;
      // for (Entry<String, Mount> e:mountTree.entrySet())
      // if (vorschlag.startsWith(e.getKey())) // find the longest matching key
      // if ((test == null) || (test.getKey().length() < e.getKey().length()))
      // test=e;
      // if (test != null) {
      // if (!test.getKey().contains(":/mnt/BackSnap"))
      // throw new IllegalArgumentException("Backup volume seems not to be mounted");
      // return test.getValue();
      // }
      // List<Mount> treffer=new ArrayList<>();
      // for (Entry<String, Mount> e:mountTree.entrySet())
      // if (e.getKey().contains(vorschlag))
      // treffer.add(e.getValue());
      // if (treffer.size() == 1) {
      // if (!treffer.get(0).keyM().contains(":/mnt/BackSnap"))
      // throw new IllegalArgumentException("Backup volume seems not to be mounted");
      // return treffer.get(0);
      // }
      // Path p=Paths.get(vorschlag);
      // Path pp=p.getParent();
      // if (pp.getParent() == null)
      // return null;
      // Mount bv=getBackupVolume(pp.toString());
      // if (bv == null)
      // return bv;
      // try {
      // bv.populate();
      // } catch (IOException e1) {/* */ }
      // if (!bv.keyM().contains(":/mnt/BackSnap"))
      // throw new IllegalArgumentException("Backup volume seems not to be mounted");
      // return bv;
   }
}
