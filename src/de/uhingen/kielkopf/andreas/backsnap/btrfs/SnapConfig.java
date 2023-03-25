/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author Andreas Kielkopf
 *
 */
public record SnapConfig(Mount original, Mount kopie) {
   /**
    * Ermittle die Paare von original subvolume und subvolume mit den snapshots
    * 
    * @param srcSubVolumes
    * @return
    */
   public static List<SnapConfig> getList(SubVolumeList srcSubVolumes) {
      ArrayList<SnapConfig> l=new ArrayList<>();
      for (Mount original:srcSubVolumes.mountTree().values()) { // über alle subvolumes laufen
         ConcurrentSkipListMap<String, Snapshot> snapTree=original.snapshotMap();
         if (snapTree.isEmpty())
            continue;
         o: for (Snapshot o:original.snapshotMap().values()) {
            String pfad=o.btrfsPath().toString();
            for (Mount kopie:srcSubVolumes.mountTree().values()) { // über alle subvolumes laufen
               if (!original.device().equals(kopie.device())) // nur auf dem selben device kann es snapshots geben
                  continue;
               String sdir=kopie.subvol();
               int    le2 =kopie.snapshotMap().size();
               if (le2 > 1) {// von der kopie darf es keine eigenen snapshots geben
                  // sdir+="/";
                  if (!pfad.startsWith(sdir + "/"))
                     continue;
                  l.add(new SnapConfig(original, kopie));
                  break o;
               }
               if (sdir.equals(original.subvol())) // das darf nicht das selbe sein
                  continue;
               if (!pfad.startsWith(sdir))
                  continue;
               l.add(new SnapConfig(original, kopie));
               break o;
            }
            System.out.println("nix gefunden");
            break;
         }
      }
      return l;
   }
}
