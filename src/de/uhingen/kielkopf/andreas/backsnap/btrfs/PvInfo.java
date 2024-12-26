/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.beans.RecordParser.getString;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uhingen.kielkopf.andreas.backsnap.config.Log;
import de.uhingen.kielkopf.andreas.backsnap.config.Log.LEVEL;
import de.uhingen.kielkopf.andreas.beans.data.format.BKMGTPE;

/**
 * @author Andreas Kielkopf Record für die Infos von pv
 * 
 *         extract Infos from all pv-line
 *
 */
public record PvInfo(long sizeL, long secL, long speedL, String progress, String txt) {
   static final Pattern SIZE=Pattern.compile("([0-9.,KMGTPEi]+ ?B)");
   static final Pattern TIME=Pattern.compile(" ([0-9]+:[0-9:]+) ");
   static final Pattern SPEED=Pattern.compile("([0-9.,KMGTiB]+/s)");
   static final Pattern PROGRESS=Pattern.compile(" (\\[[ <=>]+\\])");
   static final Pattern TIME2=Pattern.compile("([0-9]+):([0-9:]+):([0-9:]+)");
   static final Pattern SPEED2=Pattern.compile("([ 0-9,KMGTiB]+/s)");
   static final Pattern PROGRESS2=Pattern.compile(" (\\[[ <=>]+\\])");
   final static DecimalFormat zwei=new DecimalFormat("00");
   public PvInfo(String pv) {
      this(BKMGTPE.getSize(getString(SIZE.matcher(pv))), //
               getSec(getString(TIME.matcher(pv))), //
               BKMGTPE.getSize(getString(SPEED.matcher(pv))), //
               getString(PROGRESS.matcher(pv)), pv);
      updatePart();
   }
   public static long getSec(String text) { // System.out.println(size()+"-"+time());
      if (text instanceof String txt) {
         Matcher t=TIME2.matcher(txt);
         if (t.find()) {
            try {
               int h=Integer.parseInt(t.group(1));
               int m=Integer.parseInt(t.group(2));
               int s=Integer.parseInt(t.group(3));
               s+=60 * m + 3600 * h;
               return s;
            } catch (NumberFormatException e) {
               System.out.println(txt);
               e.printStackTrace();
            }
         }
      }
      return 0;
   }
   public static String getTimeS(long sec) {
      long s=sec % 60;
      long m=(sec / 60) % 60;
      long h=(sec / 3600) % 24;
      StringBuilder sb=new StringBuilder();
      sb.append(zwei.format(h)).append(":");
      sb.append(zwei.format(m)).append(":");
      sb.append(zwei.format(s));
      return sb.toString();
   }
   public String getSpeed() {
      return getSpeed(sizeL(), secL());
   }
   public static String getSpeed(long sizeL, long secL) {
      return (secL < 1) ? " --- " : BKMGTPE.vier1024((long) (sizeL / (double) secL));
   }
   public String progress() {
      return progress != null ? progress : "";
   }
   private volatile static long sumSize=0;
   private volatile static int count=0;
   private volatile static long sumSec=1;
   private volatile static long partSize=0;
   private volatile static long partSec=1;
   private volatile static long tmpSize=0;
   private volatile static long tmpSec=1;
   /** mit jeder PV-Zeile die Werte aktualisieren */
   public void updatePart() {
      if (sizeL() > partSize) {
         partSize=sizeL();
         tmpSize=sumSize + partSize;
      }
      if (secL() > partSec) {
         partSec=secL();
         tmpSec=sumSec + partSec;
      }
   }
   /** Nach jedem Snapshot die Summen der übertragenen Bytes aktualisieren und anzeigen */
   public static void addPart() {
      count++;
      Log.lfLog(getPartSpeed() + " (with " + getPartSize() + " in " + getPartSec() + ")", LEVEL.CACHE);
      sumSize+=partSize;
      partSize=0;
      sumSec+=partSec;
      partSec=0;
      Log.lfLog(getGesSpeed() + " (" + count + " backups with " + getGesSize() + " in " + getGesSec() + ")",
               LEVEL.BASIC);
   }
   private static String getPartSpeed() {
      return getSpeed(partSize, partSec) + "/s";
   }
   private static String getPartSize() {
      return BKMGTPE.vier1024(partSize);
   }
   private static String getPartSec() {
      return partSec + "Sec";
   }
   public static String getGesSpeed() {
      return getSpeed(tmpSize, tmpSec) + "/s";
   }
   public static String getGesSize() {
      return BKMGTPE.vier1024(tmpSize);
   }
   public static String getGesSec() {
      return tmpSec + " Sec";
   }
   @Override
   public final String toString() {
      StringBuilder sb=new StringBuilder();
      sb.append(getSpeed()).append(":");
      sb.append("Size=").append(sizeL());
      sb.append("Sec=").append(secL());
      return sb.toString();
   }
}
