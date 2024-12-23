/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.beans.RecordParser.getString;

import java.util.regex.*;

/**
 * @author Andreas Kielkopf Record für die Infos von pv 
 * 
 * extract Infos from all pv-line
 *
 */
public record PvInfo(String size, String time, String speed, String progress) {
   static final Pattern SIZE=Pattern.compile("([0-9.,KMGTi]+ ?B)");
   static final Pattern TIME=Pattern.compile(" ([0-9]+:[0-9:]+) ");
   static final Pattern SPEED=Pattern.compile("([0-9.,KMGTiB]+/s)");
   static final Pattern PROGRESS=Pattern.compile(" (\\[[ <=>]+\\])");
   static final Pattern SIZE2=Pattern.compile("([0-9.,]+)([KMGTi]+)( ?B)");
   static final Pattern TIME2=Pattern.compile(" ([0-9]+:[0-9:]+) ");
   static final Pattern SPEED2=Pattern.compile("([ 0-9,KMGTiB]+/s)");
   static final Pattern PROGRESS2=Pattern.compile(" (\\[[ <=>]+\\])");
   public PvInfo(String pv) {
      this(getString(SIZE.matcher(pv)), getString(TIME.matcher(pv)), getString(SPEED.matcher(pv)),
               getString(PROGRESS.matcher(pv)));
   }
   public String size() {
      return size != null ? size : "";
   }
   public String time() {
      return time != null ? time : "";
   }
   public String speed() {
      return speed != null ? speed : "";
   }
   public String progress() {
      return progress != null ? progress : "";
   }
   public long getSize() {
      long l=0;
      try {
         Matcher s=SIZE2.matcher(size());
         if (s.find()) {
            String group1=s.group(1);
            double d=Double.parseDouble(group1);
//            System.out.print(group1);
            String group2=s.group(2);
            long factor=switch (group2) {
               case "Ti":
                  yield 1024 * 1024 * 1024 * 1024L;
               case "Gi":
                  yield 1024 * 1024 * 1024L;
               case "Mi":
                  yield 1024 * 1024L;
               case "Ki":
                  yield 1024L;
               default:
                  yield 1L;
            };
            l=(long) (d * factor);
         }
      } catch (NumberFormatException e) {
         System.out.println(size());
         e.printStackTrace();
      }
      return l;
   }
   @Override
   public final String toString() {
      StringBuilder sb=new StringBuilder();
      sb.append("Size=").append(size()).append("(").append(getSize()).append(")");
      return sb.toString();
   }
}
