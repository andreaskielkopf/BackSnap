/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andreas Kielkopf
 *
 */
public record SnapConfig(Subvolume original, Subvolume kopie) {
   /**
    * Ermittle die Paare von original subvolume und subvolume mit den snapshots
    * 
    * @param srcSubVolumes
    * @return
    */
   public static List<SnapConfig> getList(SubVolumeList srcSubVolumes) {
      ArrayList<SnapConfig> l=new ArrayList<>();
      for (Subvolume original:srcSubVolumes.subvTree().values()) { // über alle subvolumes laufen
         if (original.snapshotTree().isEmpty())
            continue;
         String pfad=original.snapshotTree().firstEntry().getValue().path().toString();
         for (Subvolume kopie:srcSubVolumes.subvTree().values()) { // über alle subvolumes laufen
            if (!kopie.snapshotTree().isEmpty()) // von der kopie darf es keine eigenen snapshots geben
               continue;
            if (!original.device().equals(kopie.device())) // nur auf dem selben device kann es snapshots geben
               continue;
            if (kopie.subvol().equals(original.subvol())) // das darf nicht das selbe sein
               continue;
            String sdir=kopie.subvol();
            if (sdir.length() > 1)
               if (sdir.startsWith("/"))
                  sdir=sdir.substring(1);
            if (!pfad.startsWith(sdir))
               continue;
            l.add(new SnapConfig(original, kopie));
            break;
         }
      }
      return l;
   }
}
