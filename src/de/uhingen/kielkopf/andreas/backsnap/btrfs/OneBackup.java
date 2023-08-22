/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.nio.file.Path;

import de.uhingen.kielkopf.andreas.beans.Version;

/**
 * @author Andreas Kielkopf
 *
 */
public record OneBackup(Pc srcPc, Path srcPath, Path backupLabel) {
   public static Pc backupPc=null;
   /**
    * @throws IOException
    */
   public void mountBtrfsRoot() throws IOException {
      srcPc().mountBtrfsRoot(srcPath(), true);
   }
   public boolean isSamePc() {
      return srcPc.equals(backupPc);
   }
   public boolean isSameSsh() {
      return srcPc.equals(backupPc) && srcPc.isExtern();
   }
   public boolean isExtern() {
      return srcPc.isExtern();
   }
   public String extern() {
      return srcPc.extern();
   }
   public static boolean isBackupExtern() {
      return backupPc.isExtern();
   }
   /**
    * @return
    * @throws IOException
    */
   public boolean compressionPossible() throws IOException {
      return (srcPc().getBtrfsVersion() instanceof Version v0 && v0.getMayor() < 6)
               && (srcPc().getKernelVersion() instanceof Version v1 && v1.getMayor() < 6)
               && (backupPc.getBtrfsVersion() instanceof Version v2 && v2.getMayor() < 6);
   }
}
