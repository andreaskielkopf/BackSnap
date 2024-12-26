/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.beans.RecordParser.getString;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

import de.uhingen.kielkopf.andreas.backsnap.config.Log;
import de.uhingen.kielkopf.andreas.backsnap.config.Log.LEVEL;
import de.uhingen.kielkopf.andreas.beans.data.format.BKMGTPE;

/**
 * @author Andreas Kielkopf Record für die Infos von pv
 * 
 *         extract Infos from all pv-line
 *
 */
public record PvInfo(String size, String time, String speed, String progress) {
   static final Pattern SIZE=Pattern.compile("([0-9.,KMGTPEi]+ ?B)");
   static final Pattern TIME=Pattern.compile(" ([0-9]+:[0-9:]+) ");
   static final Pattern SPEED=Pattern.compile("([0-9.,KMGTiB]+/s)");
   static final Pattern PROGRESS=Pattern.compile(" (\\[[ <=>]+\\])");
   static final Pattern TIME2=Pattern.compile("([0-9]+):([0-9:]+):([0-9:]+)");
   static final Pattern SPEED2=Pattern.compile("([ 0-9,KMGTiB]+/s)");
   static final Pattern PROGRESS2=Pattern.compile(" (\\[[ <=>]+\\])");
   public PvInfo(String pv) {
      this(getString(SIZE.matcher(pv)), getString(TIME.matcher(pv)), getString(SPEED.matcher(pv)),
               getString(PROGRESS.matcher(pv)));
      updatePart();
   }
   public String size() {
      return size != null ? size : "";
   }
   public long getSize() {
      return BKMGTPE.getSize(size());
   }
   public String time() {
      return time != null ? time : "";
   }
   public long getSec() {
      // System.out.println(size()+"-"+time());
      try {
         Matcher t=TIME2.matcher(time());
         if (t.find()) {
            int h=Integer.parseInt(t.group(1));
            int m=Integer.parseInt(t.group(2));
            int s=Integer.parseInt(t.group(3));
            s+=60 * m + 3600 * h;
            return s;
         }
      } catch (NumberFormatException e) {
         System.out.println(time());
         e.printStackTrace();
      }
      return 0;
   }
   public String speed() {
      return speed != null ? speed : "";
   }
   public String getSpeed() {
      return getSpeed(getSize(), getSec());
   }
   public static String getSpeed(long sizeL, long secL) {
      if (secL >= 1) {
         double bs=sizeL;
         bs/=secL;
         return BKMGTPE.vier1024((long) bs);
      }
      return " --- ";
   }
   public String progress() {
      return progress != null ? progress : "";
   }
   private volatile static long gesSize=1;
   private volatile static int count=0;
   private volatile static long gesSec=1;
   private volatile static long partSize=1;
   private volatile static long partSec=1;
   /** mit jeder PV-Zeile die Werte aktualisieren */
   public void updatePart() {
      if (getSize() > partSize)
         partSize=getSize();// count++;
      if (getSec() > partSec)
         partSec=getSec();
   }
   /** Nach jedem Snapshot die Summen der übertragenen Bytes aktualisieren und anzeigen */
   public static void addPart() {
      count++;// count+=1000;
      Log.lfLog(getPartSpeed() + " (with " + getPartSize() + " in " + getPartSec() + ")", LEVEL.CACHE);
      gesSize+=partSize;
      partSize=0;
      gesSec+=partSec;
      partSec=0;
      Log.lfLog(getGesSpeed() + " (" + count + " backups with " + getGesSize() + " in " + getGesSec() + ")",
               LEVEL.BASIC);
   }
   public static String getPartSpeed() {
      return getSpeed(partSize, partSec) + "/s";
   }
   public static String getPartSize() {
      return BKMGTPE.vier1024(partSize);
   }
   public static String getPartSec() {
      return partSec + "Sec";
   }
   public static String getGesSpeed() {
      return getSpeed(gesSize, gesSec) + "/s";
   }
   public static String getGesSize() {
      return BKMGTPE.vier1024(gesSize);
   }
   public static String getGesSec() {
      return gesSec + "Sec";
   }
   @Override
   public final String toString() {
      StringBuilder sb=new StringBuilder();
      sb.append(getSpeed()).append(":");
      sb.append("Size=").append(size()).append("(").append(getSize()).append(")");
      sb.append("Sec=").append(time()).append("(").append(getSec()).append(")");
      return sb.toString();
   }
}
