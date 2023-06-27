/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;

/**
 * @author Andreas Kielkopf
 *
 */
public record Pc(String extern, ConcurrentSkipListMap<Path, Mount> mounts) {
   public Pc(String extern) {
      this((extern == null) ? "" : extern, new ConcurrentSkipListMap<>());
   }
   public void updateMounts() throws IOException {
      ConcurrentSkipListMap<Path, Mount> ml=Mount.getMountList(this, null);
      mounts.putAll(ml);
   }
   public boolean isExtern() {
      return extern.contains("@");
   }
   /**
    * ergänze die commandos um den notwendigen zugriff per ssh oder sudo
    * 
    * @param cmds
    * @return
    */
   public String getCmd(StringBuilder cmds) {
      if (isExtern()) {
         cmds.insert(0, "ssh " + extern + " '").append("'");
      } else {
         String[] cmdList=cmds.toString().split(";");
         cmds.setLength(0);
         for (int i=0; i < cmdList.length; i++)
            cmds.append("sudo ").append(cmdList[i]).append(";");
         cmds.setLength(cmds.length() - 1);
      }
      return cmds.toString();
   }
   /**
    * @return
    * 
    */
   public Optional<Mount> getTimeshiftBase() {
      return mounts.values().stream().filter(m -> m.mountPath().toString().equals(Backsnap.TMP_BTRFS_ROOT)).findFirst();
   }
}
