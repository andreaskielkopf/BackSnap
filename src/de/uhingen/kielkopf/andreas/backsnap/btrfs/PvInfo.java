/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.util.regex.Pattern;

/**
 * @author Andreas Kielkopf Record für die Infos von pv
 *
 */
public record PvInfo(String size, String time, String speed, String progress) {
   final static Pattern SIZE=Pattern.compile("([0-9,KMGTi]+ ?B)");
   final static Pattern TIME=Pattern.compile(" ([0-9]+:[0-9:]+) ");
   final static Pattern SPEED=Pattern.compile("([ 0-9,KMGTiB]+/s)");
   final static Pattern PROGRESS=Pattern.compile(" (\\[[ <=>]+\\])");
   public PvInfo(String pv) {
      this(Snapshot.getString(SIZE.matcher(pv)), Snapshot.getString(TIME.matcher(pv)),
               Snapshot.getString(SPEED.matcher(pv)), Snapshot.getString(PROGRESS.matcher(pv)));
      System.out.println(pv);
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
}
