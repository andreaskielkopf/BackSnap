/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.nio.file.Path;

/**
 * @author Andreas Kielkopf
 *
 */
public record SnapConfig(Mount volumeMount, Mount snapshotMount) {
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
