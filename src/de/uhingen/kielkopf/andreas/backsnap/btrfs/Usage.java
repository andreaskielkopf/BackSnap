/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.beans.RecordParser.getString;

import java.io.IOException;
import java.util.regex.Pattern;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;

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
   static private final long KiB=1024;
   static private final long MiB=KiB * KiB;
   static private final long GiB=KiB * MiB;
   static private final long TiB=MiB * MiB;
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
         System.err.println("It seems urgently advisable to balance the backup volume");
   }
   /**
    * @param m
    * @return
    * @throws IOException
    */
   static private String getMString(Mount m, boolean b) throws IOException {
      String dir=b ? "-b " + Pc.MNT_BACKSNAP : Pc.MNT_BACKSNAP;
      String usageCmd=m.pc().getCmd(new StringBuilder("btrfs filesystem usage ").append(dir).append(";")
               .append("btrfs device usage ").append(dir));
      Backsnap.logln(3, usageCmd);
      try (CmdStream usageStream=Commandline.executeCached(usageCmd, null)) {
         usageStream.backgroundErr();
         StringBuilder usageLine=new StringBuilder();
         for (String line:usageStream.erg().toList())
            usageLine.append(line).append('\n');
         usageStream.waitFor();
         for (String line:usageStream.errList())
            if (line.contains("No route to host") || line.contains("Connection closed")
                     || line.contains("connection unexpectedly closed"))
               throw new IOException(line);
         Backsnap.logln(7, usageLine.toString());
         return usageLine.toString();
      }
   }
   static private double getZahl(String s) {
      long f=switch (s.replaceAll("[0-9.]", "")) {
         case "TiB" -> TiB;
         case "GiB" -> GiB;
         case "MiB" -> MiB;
         case "KiB" -> KiB;
         default -> 1L;
      };
      return Double.parseDouble(s.replaceAll("[KMGTiB]", "")) * f;
   }
   public boolean isFull() {
      double ual=getZahl(unallcoated);
      return ual < (10 * GiB);
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
