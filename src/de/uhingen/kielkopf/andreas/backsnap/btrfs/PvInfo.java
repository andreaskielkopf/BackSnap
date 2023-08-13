/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.beans.RecordParser.getString;

import java.util.regex.Pattern;

/**
 * @author Andreas Kielkopf Record für die Infos von pv
 *
 */
public record PvInfo(String size, String time, String speed, String progress) {
   static final Pattern SIZE=Pattern.compile("([0-9,KMGTi]+ ?B)");
   static final Pattern TIME=Pattern.compile(" ([0-9]+:[0-9:]+) ");
   static final Pattern SPEED=Pattern.compile("([ 0-9,KMGTiB]+/s)");
   static final Pattern PROGRESS=Pattern.compile(" (\\[[ <=>]+\\])");
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
}
