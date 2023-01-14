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
         int le=original.snapshotTree().size();
         if (le <= 1)
            continue;
         for (Object o:original.snapshotTree().values()) {
            if (o instanceof Snapshot s) {
               // Snapshot v=original.snapshotTree().firstEntry().getValue();
               // if (v == null)
               // continue;
               String pfad=s.path().toString();
               for (Mount kopie:srcSubVolumes.mountTree().values()) { // über alle subvolumes laufen
                  int le2=kopie.snapshotTree().size();
                  if (le2 > 1) // von der kopie darf es keine eigenen snapshots geben
                     continue;
                  if (!original.device().equals(kopie.device())) // nur auf dem selben device kann es snapshots geben
                     continue;
                  String sdir=kopie.subvol();
                  if (sdir.equals(original.subvol())) // das darf nicht das selbe sein
                     continue;
                  // if (sdir.length() > 1)
                  // if (sdir.startsWith("/"))
                  // sdir=sdir.substring(1);
                  if (!pfad.startsWith(sdir))
                     continue;
                  l.add(new SnapConfig(original, kopie));
                  break;
               }
            }
            break;
         }
      }
      return l;
   }
}
