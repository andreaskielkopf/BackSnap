/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;
import de.uhingen.kielkopf.andreas.beans.data.Link;

/**
 * @author Andreas Kielkopf
 *
 */
public record Pc(String extern, ConcurrentSkipListMap<Path, Mount> mounts, Link<SubVolumeList> cachedSubVolumeList,
         Link<Version> cachedBtrfsVersion, Link<Version> cachedKernelVersion) {
   public Pc(String extern) {
      this((extern == null) ? "" : extern, new ConcurrentSkipListMap<>(), new Link<SubVolumeList>(),
               new Link<Version>(), new Link<Version>());
   }
   public void updateMounts() throws IOException {
      ConcurrentSkipListMap<Path, Mount> ml=Mount.getMountList(this, null);
      mounts.putAll(ml);
   }
   public boolean isExtern() {
      return extern.contains("@");
   }
   /**
    * ergänze die commandos um den notwendigen Zugriff per ssh oder sudo
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
   /**
    * @return
    * @throws IOException
    */
   public SubVolumeList getSubVolumeList() throws IOException {
      if (cachedSubVolumeList.get() == null)
         cachedSubVolumeList.set(new SubVolumeList(this));
      return cachedSubVolumeList.get();
   }
   /**
    * Ermittle die btrfs-version des PC
    * @return
    * @throws IOException
    */
   public Version getBtrfsVersion() throws IOException {
      if (cachedBtrfsVersion.get() == null) {
         String versionCmd=getCmd(new StringBuilder("btrfs version"));
         Backsnap.logln(6, versionCmd);
         try (CmdStream versionStream=Commandline.executeCached(versionCmd, versionCmd)) {
            versionStream.backgroundErr();
            for (String line:versionStream.erg().toList())
               cachedBtrfsVersion.set(new Version(line));
            versionStream.waitFor();
            for (String line:versionStream.errList())
               if (line.contains("No route to host") || line.contains("Connection closed")
                        || line.contains("connection unexpectedly closed"))
                  throw new IOException(line);
         }
      }
      return cachedBtrfsVersion.get();
   }
   /** 
    * Ermittle die KernelVersion des PC
    * @return
    * @throws IOException
    */
   public Version getKernelVersion() throws IOException {
      if (cachedKernelVersion.get() == null) {
         String versionCmd=getCmd(new StringBuilder("uname -rs"));
         Backsnap.logln(6, versionCmd);
         try (CmdStream versionStream=Commandline.executeCached(versionCmd, versionCmd)) {
            versionStream.backgroundErr();
            for (String line:versionStream.erg().toList())
               cachedKernelVersion.set(new Version(line));
            versionStream.waitFor();
            for (String line:versionStream.errList())
               if (line.contains("No route to host") || line.contains("Connection closed")
                        || line.contains("connection unexpectedly closed"))
                  throw new IOException(line);
         }
      }
      return cachedKernelVersion.get();
   }
}
