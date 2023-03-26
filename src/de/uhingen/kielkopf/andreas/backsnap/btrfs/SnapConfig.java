/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
//         ConcurrentSkipListMap<Path, Snapshot> snapTree=original.snapshotMap();
         if (original.snapshotMap().isEmpty())
            continue;
         o: for (Snapshot o:original.snapshotMap().values()) {
//            String pfad=o.btrfsPath().toString();
            Path btrfsPath=o.btrfsPath();
            for (Mount kopie:srcSubVolumes.mountTree().values()) { // über alle subvolumes laufen
               if (!original.devicePath().equals(kopie.devicePath())) // nur auf dem selben device kann es snapshots geben
                  continue;
               Path sdir=kopie.btrfsPath();
               int    le2 =kopie.snapshotMap().size();
               if (le2 > 1) {// von der kopie darf es keine eigenen snapshots geben
                  // sdir+="/";
                  if (!btrfsPath.startsWith(sdir + "/"))
                     continue;
                  l.add(new SnapConfig(original, kopie));
                  break o;
               }
               if (sdir.equals(original.btrfsPath())) // das darf nicht das selbe sein
                  continue;
               if (!btrfsPath.startsWith(sdir))
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
