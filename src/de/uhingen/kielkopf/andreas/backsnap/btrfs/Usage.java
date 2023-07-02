/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

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
   final static String A="[\\t ]+([0-9.KMGTiB]+)";
   final static Pattern SIZE=Pattern.compile("Device size:" + A);
   final static Pattern ALLOCATED=Pattern.compile(" allocated:" + A);
   final static Pattern UNALLOCATED=Pattern.compile("nallocated:" + A);
   final static Pattern MISSING=Pattern.compile("Device missing:" + A);
   final static Pattern SLACK=Pattern.compile("Device slack:" + A);
   final static Pattern USED=Pattern.compile("Used:" + A);
   final static Pattern RATIO_D=Pattern.compile("Data ratio:" + A);
   final static Pattern RATIO_M=Pattern.compile("Metadata ratio:" + A);
   final static Pattern FREE=Pattern.compile("Free [a-z,() ]+:" + A);
   final static Pattern RESERVE=Pattern.compile("Global reserve:" + A);
   final static Pattern DATA=Pattern.compile("Data,single:" + A);
   final static Pattern METADATA=Pattern.compile("Metadata,DUP:" + A);
   final static Pattern SYSTEM=Pattern.compile("System,DUP:" + A);
   private static final long KiB=1024;
   private static final long MiB=KiB * KiB;
   private static final long GiB=KiB * MiB;
   private static final long TiB=MiB * MiB;
   public Usage(String u) {
      this(Snapshot.getString(SIZE.matcher(u)), Snapshot.getString(ALLOCATED.matcher(u)),
               Snapshot.getString(UNALLOCATED.matcher(u)), Snapshot.getString(MISSING.matcher(u)),
               Snapshot.getString(SLACK.matcher(u)), Snapshot.getString(USED.matcher(u)),
               Snapshot.getString(RATIO_D.matcher(u)), Snapshot.getString(RATIO_M.matcher(u)),
               Snapshot.getString(FREE.matcher(u)), Snapshot.getString(RESERVE.matcher(u)),
               Snapshot.getString(DATA.matcher(u)), Snapshot.getString(METADATA.matcher(u)),
               Snapshot.getString(SYSTEM.matcher(u)));
      // System.out.println(u);
   }
   public Usage(Mount m, boolean b) throws IOException {
      this(getString(m, b));
   }
   /**
    * @param m
    * @return
    * @throws IOException
    */
   private static String getString(Mount m, boolean b) throws IOException {
      String dir="/mnt/BackSnap";
      if (b)
         dir="-b " + dir;
      StringBuilder usageSB =new StringBuilder()                            //
               .append("btrfs filesystem usage ").append(dir).append(";")   //
               .append("btrfs device usage ").append(dir);
      String        usageCmd=m.pc().getCmd(usageSB);
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
   private static double getZahl(String s) {
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
