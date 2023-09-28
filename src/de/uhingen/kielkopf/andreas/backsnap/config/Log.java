/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Andreas Kielkopf
 *
 */
public class Log {
   static public int              logPos   =0;
   static public final int        logMAXLEN=120;
   static public String           lastline ="1234567890";
   static public ArrayList<LEVEL> logLevels=new ArrayList<>(List.of(LEVEL.PROGRESS));
   static private ReentrantLock   OUT      =new ReentrantLock();
   static public enum LEVEL {
      NOTHING(0), // extra quiet
      ERRORS(1), // quiet
      MOUNT(2), // mountBackupvolume
      BASIC(2), // standard
      CONFIG(3), // Configuration
      SNAPSHOTS(4), // show what Snapshots are considdered
      PROGRESS(5),
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
   static public void log(String text, LEVEL... levels) {
      boolean l=tryLock();
      if (needsPrinting(levels) && dedup(text) instanceof String s ) {
         if (Log.logPos + s.length() > Log.logMAXLEN) {
            System.out.print(System.lineSeparator());
            Log.logPos=0;
         }
         System.out.print(s);
         Log.logPos+=s.length();
      }
      tryUnlock(l);
   }
   static public void lnlog(String text, LEVEL... levels) {
      boolean l=tryLock();
      if (needsPrinting(levels) && dedup(text) instanceof String s) {
         System.out.print(System.lineSeparator() + s);
         Log.logPos=s.length();
      }
      tryUnlock(l);
   }
   static public void Owlog(String text, LEVEL... levels) {
      boolean l=tryLock();
      if (needsPrinting(levels) && dedup(text) instanceof String s ) {
         System.out.print("\r" + s);
         Log.logPos=s.length();
      }
      tryUnlock(l);
   }
   static public void logln(String text, LEVEL... levels) {
      boolean l=tryLock();
      if (needsPrinting(levels) && dedup(text) instanceof String s ) {
         if (Log.logPos + s.length() > Log.logMAXLEN)
            System.out.print(System.lineSeparator());
         System.out.print(s + System.lineSeparator());
         Log.logPos=0;
      }
      tryUnlock(l);
   }
   static public void logln(List<String> texts, LEVEL... levels) {
      boolean l=tryLock();
      if (needsPrinting(levels)) {
         for (String line:texts)
            if (dedup(line) instanceof String s ) {
               if (Log.logPos + s.length() > Log.logMAXLEN)
                  System.out.print(System.lineSeparator());
               System.out.print(s + System.lineSeparator());
               Log.logPos=0;
            }
      }
      tryUnlock(l);
   }
   static public void logOw(String text, LEVEL... levels) {
      boolean l=tryLock();
      if (needsPrinting(levels) && dedup(text) instanceof String s ) {
         if (Log.logPos + s.length() > Log.logMAXLEN)
            System.out.print("\r");
         System.out.print(s + "\r");
         Log.logPos=0;
      }
      tryUnlock(l);
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
   private static void tryUnlock(boolean l) {
      if (l && OUT.isHeldByCurrentThread())
         OUT.unlock();
   }
   private static boolean tryLock() {
      try {
         return OUT.tryLock(200, TimeUnit.MILLISECONDS);
      } catch (InterruptedException ignore) {/* */}
      return false;
   }
}
