package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uhingen.kielkopf.andreas.backsnap.config.Log;
import de.uhingen.kielkopf.andreas.backsnap.config.Log.LEVEL;
import de.uhingen.kielkopf.andreas.beans.Version;
import de.uhingen.kielkopf.andreas.beans.minijson.Etc;

/**
 * Konfiguration für ein Backup von einem Subvolume
 * 
 * @author Andreas Kielkopf
 * @param etcPath
 *           wo die Konfiguration herkommt
 * @param srcPc
 *           Der Pc von dem das Backup germacht werden soll
 * @param srcPath
 *           Der Pfad auf diesem PC *
 * @param backupLabel
 * @param flags
 * @param backupTree[]
 */
public record OneBackup(Path etcPath, Pc srcPc, Path srcPath, Path backupLabel, String flags, SnapTree[] backupTree,
         DataSet[] dataSet) implements Comparable<OneBackup> {
   public static Pc backupPc=null;
   private static String backupId=null;
   /** sortierte Liste mit den vorgesehenen Backups */
   final public static ConcurrentSkipListMap<String, OneBackup> unsortedMap=new ConcurrentSkipListMap<>();
   final public static ConcurrentSkipListMap<String, OneBackup> sortedMap=new ConcurrentSkipListMap<>();
   private static Pattern linePattern=Pattern.compile("^( *[a-zA-Z0-9._]{2,80} *)=(.+)"); // Kommentare ausblenden
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
   private boolean isLocalConfig() {
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
                        if (getBackupId() == null)
                           backupId=b;
                        break;
                     case "flags":
                        flags=b;
                        break;
                     default:
                        Path label=Path.of(a); // relativ
                        Path pfad=Path.of(b); // absolut
                        if (pfad.isAbsolute()) {
                           OneBackup o=new OneBackup(entry.getKey(), pc, pfad, label, flags, new SnapTree[1],
                                    new DataSet[1]);
                           unsortedMap.put(label.toString(), o);
                           sortedMap.put(entry.getKey().getFileName() + ":" + label, o);
                        }
                        break;
                  }
               }
            }
         }
   }
   /**
    * @return
    */
   private static String getBasisText() {
      return new StringBuilder().append((backupPc == null) ? "no Pc" : backupPc.toString())
               .append((getBackupId() == null) ? " & no Id" : " & Id:" + getBackupId()).toString();
   }
   /**
    * @return
    */
   public static List<String> getConfigText() {
      ArrayList<String> l=new ArrayList<>();
      l.add(getBasisText());
      for (OneBackup backup:sortedMap.values())
         l.add(backup.toString());
      return l;
   }
   @Override
   public int compareTo(OneBackup o) {
      if (o == null)
         return -1;
      if (isLocalConfig() != o.isLocalConfig())// localconfig an die erste Stelle
         return isLocalConfig() ? -1 : 1;
      int ep=etcPath.compareTo(o.etcPath);// pfad der Configdatei vergleichen
      if (ep != 0)
         return ep;
      int sp=srcPath.compareTo(o.srcPath);// Pfad des Volume vergleichen
      return sp;
   }
   public static String getBackupId() {
      return backupId;
   }
   /**
    * @return
    */
   public SnapConfig getSnapConfig() throws IOException {
      for (SnapConfig sc:srcPc().getSnapConfigs()) {
         if (sc.volumeMount().mountPath().equals(srcPath())) {
            Log.logln(sc.toString(), LEVEL.BTRFS);
            return sc;
         }
         if (sc.snapshotMount().mountPath().equals(srcPath())) {
            Log.errln("Treffer: snapshotMount " + srcPath(), LEVEL.ERRORS);
            return sc;
         }
      }
      throw new RuntimeException(System.lineSeparator() + "Could not find any snapshots for srcDir: " + srcPath());
   }
   public static Collection<OneBackup> getSortedBackups() {
      return sortedMap.values();
   }
   @Override
   public final String toString() {
      StringBuilder sb=new StringBuilder();
      sb.append("OneBackup[").append(etcPath.getFileName());
      sb.append(", ").append(srcPc);
      sb.append(":").append(srcPath);
      sb.append(" (").append(backupLabel).append(")");
      sb.append(" flags=").append(flags);
      if (backupTree[0] != null)
         sb.append(backupTree[0].sMount());
      sb.append("]");
      return sb.toString();
   }
   /**
    * @return
    */
   public static int size() {
      return sortedMap.size();
   }
}
