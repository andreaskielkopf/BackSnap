/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.config;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andreas Kielkopf
 *
 */
public class Log {
   static public int              logPos   =0;
   static public final int        logMAXLEN=120;
   static public String           lastline ="1234567890";
   static public ArrayList<LEVEL> logLevels=new ArrayList<>(List.of(LEVEL.PROGRESS));
   static public enum LEVEL {
      NOTHING(0),
      ERRORS(1),
      BASIC(2),
      PROGRESS(3),
      SNAPSHOTS(4),
      DELETE(5),
      BTRFS(6),
      RSYNC(7),
      COMMANDS(8),
      GUI(9),
      ALLES(10),
      INIT(11),
      BTRFS_ANSWER(15),
      CACHE(19),
      DEBUG(20);
      final public Integer l;
      private LEVEL(int i) {
         l=Integer.valueOf(i);
      }
   }
   static public void lnlog(String text, LEVEL... levels) {
      synchronized (lastline) {
         if (needsPrinting(levels) && dedup(text) instanceof String s && !s.isBlank()) {
            System.out.print(System.lineSeparator() + s);
            Log.logPos=s.length();
         }
      }
   }
   static public void logln(String text, LEVEL... levels) {
      synchronized (lastline) {
         if (needsPrinting(levels) && dedup(text) instanceof String s && !s.isBlank()) {
            if (Log.logPos + s.length() > Log.logMAXLEN)
               System.out.print(System.lineSeparator());
            System.out.print(s + System.lineSeparator());
            Log.logPos=0;
         }
      }
   }
   static public void log(String text, LEVEL... levels) {
      synchronized (lastline) {
         if (needsPrinting(levels) && dedup(text) instanceof String s && !s.isBlank()) {
            if (Log.logPos + s.length() > Log.logMAXLEN) {
               System.out.print(System.lineSeparator());
               Log.logPos=0;
            }
            System.out.print(s);
            Log.logPos+=s.length();
         }
      }
   }
   /**
    * Verhindere das doppelte logging von gleichen Texten
    * 
    * @param text
    * @return
    */
   private static String dedup(String text) {
      if (text instanceof String s && !s.isBlank()) {
         if (s.startsWith(lastline))
            s=s.substring(lastline.length());
         if (s.length() >= 1)
            lastline=s;
         return s;
      }
      return text;
   }
   /**
    * testet ob der Loglevel eingeschaltet ist
    * 
    * @param levels
    * @return
    */
   private static boolean needsPrinting(LEVEL... levels) {
      for (LEVEL level:levels)
         if (logLevels.contains(level))
            return true;
      return false;
   }
   /**
    * @param loglevel
    *           the loglevel to set
    */
   public static void setLoglevel(Object loglevel) {
      if (loglevel instanceof Integer i) {
         logLevels.clear();
         Integer limit=(i < 0) ? Integer.valueOf(0) : i;
         for (LEVEL level:LEVEL.values())
            if (level.l <= limit)
               logLevels.add(level);
      }
   }
}
