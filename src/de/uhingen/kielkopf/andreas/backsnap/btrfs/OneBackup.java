package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uhingen.kielkopf.andreas.beans.Version;
import de.uhingen.kielkopf.andreas.beans.minijson.Etc;

/**
 * Konfiguration für ein Backup von einem Subvolume
 * 
 * @author Andreas Kielkopf
 *
 */
public record OneBackup(Path etcPath, Pc srcPc, Path srcPath, Path backupLabel, String flags)
         implements Comparable<OneBackup> {
   public static Pc backupPc=null;
   public static String backupId=null;
   public static ConcurrentSkipListSet<OneBackup> backupList=new ConcurrentSkipListSet<>();
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
   public boolean isLocalConfig() {
      String x=srcPc.extern();
      return (x.contains("@localhost") || (!x.contains("@")));
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
   static Pattern linePattern=Pattern.compile("^( *[a-zA-Z0-9._]{2,80} *)=(.+)"); // Kommentare ausblenden
   /**
    * @param etc
    */
   public static void setConfig(Etc etc0) {
      if (etc0 instanceof Etc etc)
         for (Entry<Path, List<String>> entry:etc.confFiles.entrySet()) {
            Pc pc=null;
            String flags=null;
            for (String line:entry.getValue()) {
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
                        if (pfad.isAbsolute())
                           backupList.add(new OneBackup(entry.getKey(), pc, pfad, label, flags));
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
   /**
    * @return
    */
   public static String getBasisText() {
      return new StringBuilder().append((backupPc == null) ? "no Pc" : backupPc.toString())
               .append((backupId == null) ? " & no Id" : " & Id:" + backupId).toString();
   }
   /**
    * @return
    */
   public static List<String> getConfigText() {
      ArrayList<String> l=new ArrayList<>();
      l.add(getBasisText());
      for (OneBackup backup:backupList)
         l.add(backup.toString());
      return l;
   }
   @Override
   public int compareTo(OneBackup o) {
      if (o == null)
         return 1;
      if (isLocalConfig() != o.isLocalConfig())
         return isLocalConfig() ? 1 : -1;
      return etcPath.compareTo(o.etcPath);
   }
}
