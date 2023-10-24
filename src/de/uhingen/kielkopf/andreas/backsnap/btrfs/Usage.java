/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.backsnap.btrfs.Btrfs.BTRFS;
import static de.uhingen.kielkopf.andreas.beans.RecordParser.getString;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.config.Log;
import de.uhingen.kielkopf.andreas.backsnap.config.Log.LEVEL;
import de.uhingen.kielkopf.andreas.beans.shell.CmdStreams;

/**
 * @author Andreas Kielkopf
 *
 */
public record Usage(String size, String allocated, String unallcoated, String missing, String slack, //
         String used, String ratioD, String ratioM, String free, String reserve, String data, String metadata,
         String system) {
   static final String A="[\\t ]+([0-9.KMGTiB]+)";
   static final Pattern SIZE=Pattern.compile("Device size:" + A);
   static final Pattern ALLOCATED=Pattern.compile(" allocated:" + A);
   static final Pattern UNALLOCATED=Pattern.compile("nallocated:" + A);
   static final Pattern MISSING=Pattern.compile("Device missing:" + A);
   static final Pattern SLACK=Pattern.compile("Device slack:" + A);
   static final Pattern USED=Pattern.compile("Used:" + A);
   static final Pattern RATIO_D=Pattern.compile("Data ratio:" + A);
   static final Pattern RATIO_M=Pattern.compile("Metadata ratio:" + A);
   static final Pattern FREE=Pattern.compile("Free [a-z,() ]+:" + A);
   static final Pattern RESERVE=Pattern.compile("Global reserve:" + A);
   static final Pattern DATA=Pattern.compile("Data,single:" + A);
   static final Pattern METADATA=Pattern.compile("Metadata,DUP:" + A);
   static final Pattern SYSTEM=Pattern.compile("System,DUP:" + A);
   static private final long K=1024;
   public Usage(String u) {
      this(getString(SIZE.matcher(u)), getString(ALLOCATED.matcher(u)), getString(UNALLOCATED.matcher(u)),
               getString(MISSING.matcher(u)), getString(SLACK.matcher(u)), getString(USED.matcher(u)),
               getString(RATIO_D.matcher(u)), getString(RATIO_M.matcher(u)), getString(FREE.matcher(u)),
               getString(RESERVE.matcher(u)), getString(DATA.matcher(u)), getString(METADATA.matcher(u)),
               getString(SYSTEM.matcher(u)));
      // System.out.println(u);
   }
   public Usage(Mount m, boolean b) throws IOException {
      this(getMString(m, b));
      if (needsBalance())
         Log.errln("It seems urgently advisable to balance the backup volume",LEVEL.ERRORS);
   }
   /**
    * @param m
    * @return
    * @throws IOException
    */
   static private String getMString(Mount m, boolean b) throws IOException {
      String dir=b ? "-b " + Pc.TMP_BACKUP_ROOT.toString() : Pc.TMP_BACKUP_ROOT.toString();
      String usageCmd=m.pc().getCmd(
               new StringBuilder(Btrfs.FILESYSTEM_USAGE).append(dir).append(";").append(Btrfs.DEVICE_USAGE).append(dir),
               false);// TODO This may be wrong, and sometimes sudo may be needed
      Log.logln(usageCmd, LEVEL.BTRFS);
      BTRFS.readLock().lock();
      try (CmdStreams usageStream=CmdStreams.getDirectStream(usageCmd)) {
         StringBuilder usageLine=new StringBuilder();
         usageStream.outBGerr().forEach(line -> usageLine.append(line).append('\n'));
         Optional<String> erg=usageStream.errLines().filter(line -> (line.contains("No route to host")
                  || line.contains("Connection closed") || line.contains("connection unexpectedly closed"))).findAny();
         if (erg.isPresent()) {
            Backsnap.disconnectCount=10;
            throw new IOException(erg.get());
         }
         Log.logln(usageLine.toString(), LEVEL.BTRFS_ANSWER);
         return usageLine.toString();
      } finally {
         BTRFS.readLock().unlock();
      }
      }
   static public double getZahl(String s) {
      IB f=IB.valueOf(s.replaceAll("[0-9.]", ""));
      return Double.parseDouble(s.replaceAll("[KMGTiB]", "")) * f.f;
   }
   enum IB {
      KiB(K), MiB(K * K), GiB(K * K * K), TiB(K * K * K * K);
      final long f;
      IB(long l) {
         f=l;
      }
      /**
       * @param z
       * @return
       */
      String get(double z) {
         double erg=z / f;
         String s=Long.toString((long) erg) + name();
         return " ".repeat(7 - s.length()) + s;
      }
      static String getText(double z) {
         for (IB ib:IB.values())
            if (z < 3000 * ib.f)
               return ib.get(z);
         return Long.toString((long) z);
      }
   }
   public boolean isFull() {
      double ual=getZahl(unallcoated);
      return ual < (10 * IB.GiB.f);
   }
   public boolean needsBalance() {
      return getFree() < 0.2d;
   }
   public double getFree() {
      double s=getZahl(size);
      double u=getZahl(unallcoated);
      double f=u / s;
      return f;
   }
}
