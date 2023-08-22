/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.nio.file.Path;

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
}
