/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.nio.file.Path;
import java.util.*;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;

/**
 * @author Andreas Kielkopf
 *
 */
public record SnapConfig(Mount volumeMount, Mount snapshotMount) {
   /**
    * Ermittle die Paare von subVolumeMount und subvolume mit den dazu passenden Snapshots
    * 
    * @param srcSubVolumes
    * @return
    */
//   public static List<SnapConfig> getList(SubVolumeList srcSubVolumes) {
//      ArrayList<SnapConfig> l=new ArrayList<>();
//      for (Mount volumeMount:srcSubVolumes.mountTree().values()) { // über alle Subvolumes laufen
////         if (volumeMount.otimeKeyMap().isEmpty())
////            continue;
//         o: for (Snapshot snap_o:volumeMount.otimeKeyMap().values()) { // über die Snapshots
//            Path btrfsPath=snap_o.btrfsPath();
//            for (Mount snapshotMount:srcSubVolumes.mountTree().values()) { // über alle subvolumes laufen
//               if (!volumeMount.devicePath().equals(snapshotMount.devicePath())) // nur auf diesem device möglich
//                  continue;
//               Path sdir=snapshotMount.btrfsPath();
//               int  le2 =snapshotMount.btrfsMap().size();
//               if (le2 > 1) {// von der snapshotMount darf es keine eigenen snapshotMount geben
//                  // sdir+="/";
//                  if (!btrfsPath.startsWith(sdir + "/"))
//                     continue;
//                  l.add(new SnapConfig(volumeMount, snapshotMount));
//                  break o;
//               }
//               if (sdir.equals(volumeMount.btrfsPath())) // das darf nicht das selbe sein
//                  continue;
//               if (!btrfsPath.startsWith(sdir))
//                  continue;
//               l.add(new SnapConfig(volumeMount, snapshotMount));
//               break o;
//            }
//            System.out.println("nix gefunden");
//            break;
//         }
//      }
//      return l;
//   }
   public static SnapConfig getConfig(List<SnapConfig> list, Path srcDir) {
      for (SnapConfig snapConfig:list) {
         Backsnap.logln(9, snapConfig.toString());
         if (snapConfig.volumeMount.mountPath().equals(srcDir))
            return snapConfig;
         if (snapConfig.snapshotMount.mountPath().equals(srcDir))
            return snapConfig;
      }
      return null;
   }
   @Override
   public String toString() {
      return new StringBuilder("SnapConfig [volumeMount=").append(volumeMount.mountPath()).append(", snapshotMount=")
               .append(snapshotMount.mountPath()).append("]").toString();
   }
}
