/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uhingen.kielkopf.andreas.beans.Version;
import de.uhingen.kielkopf.andreas.beans.minijson.Etc;

/**
 * @author Andreas Kielkopf
 *
 */
public record OneBackup(Pc srcPc, Path srcPath, Path backupLabel, String flags) {
   public static Pc backupPc=null;
   public static String backupId=null;
   public static List<OneBackup> backupList=new ArrayList<>();
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
      return (srcPc().getBtrfsVersion() instanceof Version v0 && (v0.getMayor() >= 6))
               && (srcPc().getKernelVersion() instanceof Version v1 && (v1.getMayor() >= 6))
               && (backupPc.getBtrfsVersion() instanceof Version v2 && (v2.getMayor() >= 6));
   }
   static Pattern linePattern=Pattern.compile("^( *[a-zA-Z0-9._]{2,80} *)=(.+)");
   /**
    * @param etc
    */
   public static void setConfig(Etc etc0) {
      if (etc0 instanceof Etc etc)
         for (List<String> file:etc.conf.values()) {
            Pc pc=null;
            String flags=null;
            for (String line:file) {
               // if (line.startsWith("#")) continue; // Kommentare ausblenden
               Matcher m=linePattern.matcher(line);
               if (m.matches()) {
                  String a=m.group(1).strip();
                  String b=m.group(2).strip();
                  switch (a.toLowerCase()) {
                     case "pc":
                        if (pc == null)
                           pc=Pc.getPc(b.startsWith("localhost") ? Pc.SUDO : b);
                        break;
                     case "backup_id":
                        if (backupPc == null)
                           backupPc=pc;
                        if (backupId == null)
                           backupId=b;
                        break;
                     case "flags":
                        flags=b;
                        break;
                     default:
                        Path label=Path.of(a); // relativ
                        Path pfad=Path.of(b); // absolut
                        if (pfad.isAbsolute()) {
                           OneBackup oneBackup=new OneBackup(pc, pfad, label, flags);
                           System.out.println("Add " + oneBackup);
                           backupList.add(oneBackup);
                        }
                        break;
                  }
               }
            }
         }
   }
   public static void setBasis(Etc etc1) {
      setConfig(etc1);
      backupList.clear();
   }
   /**
    * @return
    */
   public static List<Pc> getPcs() {
      // TODO Auto-generated method stub
      return null;
   }
   /**
    * @return
    */
   public static List<OneBackup> getBackups() {
      // TODO Auto-generated method stub
      return null;
   }
}
