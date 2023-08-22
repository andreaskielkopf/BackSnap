/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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
    * @throws IOException
    */
   // static public List<SnapConfig> getList(SubVolumeList srcSubVolumes) {
   // ArrayList<SnapConfig> l=new ArrayList<>();
   // for (Mount volumeMount:srcSubVolumes.mountTree().values()) { // über alle Subvolumes laufen
   //// if (volumeMount.otimeKeyMap().isEmpty())
   //// continue;
   // o: for (Snapshot snap_o:volumeMount.otimeKeyMap().values()) { // über die Snapshots
   // Path btrfsPath=snap_o.btrfsPath();
   // for (Mount snapshotMount:srcSubVolumes.mountTree().values()) { // über alle subvolumes laufen
   // if (!volumeMount.devicePath().equals(snapshotMount.devicePath())) // nur auf diesem device möglich
   // continue;
   // Path sdir=snapshotMount.btrfsPath();
   // int le2 =snapshotMount.btrfsMap().size();
   // if (le2 > 1) {// von der snapshotMount darf es keine eigenen snapshotMount geben
   // // sdir+="/";
   // if (!btrfsPath.startsWith(sdir + "/"))
   // continue;
   // l.add(new SnapConfig(volumeMount, snapshotMount));
   // break o;
   // }
   // if (sdir.equals(volumeMount.btrfsPath())) // das darf nicht das selbe sein
   // continue;
   // if (!btrfsPath.startsWith(sdir))
   // continue;
   // l.add(new SnapConfig(volumeMount, snapshotMount));
   // break o;
   // }
   // System.out.println("nix gefunden");
   // break;
   // }
   // }
   // return l;
   // }
//   @Deprecated
//   static public SnapConfig getConfig(Pc pc, Path srcDir) throws IOException {
//      List<SnapConfig> l=pc.getSnapConfigs();
//      for (SnapConfig snapConfig:l) {
//         Backsnap.logln(9, snapConfig.toString());
//         if (snapConfig.volumeMount.mountPath().equals(srcDir)) {
//            System.err.println("Treffer: volumeMount " + srcDir);
//            return snapConfig;
//         }
//         if (snapConfig.snapshotMount.mountPath().equals(srcDir)) {
//            System.err.println("Treffer: snapshotMount " + srcDir);
//            return snapConfig;
//         }
//      }
//      return null;
//   }
   static public SnapConfig getConfig(OneBackup oneBackup) throws IOException {
      List<SnapConfig> l=oneBackup.srcPc().getSnapConfigs();
      for (SnapConfig snapConfig:l) {
         Backsnap.logln(9, snapConfig.toString());
         if (snapConfig.volumeMount.mountPath().equals(oneBackup.srcPath())) {
            System.err.println("Treffer: volumeMount " + oneBackup.srcPath());
            return snapConfig;
         }
         if (snapConfig.snapshotMount.mountPath().equals(oneBackup.srcPath())) {
            System.err.println("Treffer: snapshotMount " + oneBackup.srcPath());
            return snapConfig;
         }
      }
      throw new RuntimeException(
               System.lineSeparator() + "Could not find any snapshots for srcDir: " + oneBackup.srcPath());
   }
   public Pc pc() {
      return snapshotMount.pc();
   }
   public Path mountPath() {
      return snapshotMount.mountPath();
   }
   @Override
   public String toString() {
      return new StringBuilder("SnapConfig [volumeMount=").append(volumeMount.mountPath()).append(", snapshotMount=")
               .append(snapshotMount.mountPath()).append("]").toString();
   }
}
