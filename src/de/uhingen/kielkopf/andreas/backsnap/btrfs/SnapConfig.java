/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import de.uhingen.kielkopf.andreas.backsnap.config.Log;
import de.uhingen.kielkopf.andreas.backsnap.config.Log.LEVEL;

/**
 * @author Andreas Kielkopf
 *
 */
public record SnapConfig(Mount volumeMount, Mount snapshotMount) {
   static public SnapConfig getConfig(OneBackup oneBackup) throws IOException {
      List<SnapConfig> l=oneBackup.srcPc().getSnapConfigs();
      for (SnapConfig snapConfig:l) {
         if (snapConfig.volumeMount.mountPath().equals(oneBackup.srcPath())) {
            Log.logln(snapConfig.toString(), LEVEL.BTRFS);
            return snapConfig;
         }
         if (snapConfig.snapshotMount.mountPath().equals(oneBackup.srcPath())) {
            Log.errln("Treffer: snapshotMount " + oneBackup.srcPath(),LEVEL.ERRORS);
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
