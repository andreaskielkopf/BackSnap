/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.config;

import java.util.*;
import java.util.Map.Entry;

// import java.util.concurrent.locks.ReentrantLock;
// free(
/**
 * @author Andreas Kielkopf
 *
 */
public class Log {
   public static boolean          withTimestamp=true;
   final private static long      start        =System.currentTimeMillis();
   static private int             logPos       =0;
   // static private int errPos =0;
   static public final int        logMAXLEN    =120;
   static public String           lastline     ="1234567890";
   static public ArrayList<LEVEL> logLevels    =new ArrayList<>(List.of(LEVEL.PROGRESS));
   // static private ReentrantLock OUT =new ReentrantLock(true);
   static public enum LEVEL {
      NOTHING(0), // extra quiet
      ERRORS(1), // quiet
      MOUNT(2), // mountBackupvolume
      BASIC(2), // standard
      CONFIG(3), // Configuration
      SNAPSHOTS(4), // show what Snapshots are considdered
      PROGRESS(5), // show progress logged by PV
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
      if (needsPrinting(levels) && dedup(text) instanceof String s) {
         StringBuilder sb=new StringBuilder(s);
         if (Log.logPos + s.length() > Log.logMAXLEN) {
            lfLog(sb, false); // System.out.print(System.lineSeparator());
            Log.logPos=0;
         } else
            log(sb, false); // System.out.print(s);
         Log.logPos+=s.length();
      }
      tryUnlock(l);
   }
   static public void lfLog(String text, LEVEL... levels) {
      boolean l=tryLock();
      if (needsPrinting(levels) && dedup(text) instanceof String s) {
         StringBuilder sb=new StringBuilder(s); // System.out.print(System.lineSeparator() + s);
         lfLog(sb, false);
         Log.logPos=s.length();
      }
      tryUnlock(l);
   }
   static public void crLog(String text, LEVEL... levels) {
      boolean l=tryLock();
      if (needsPrinting(levels) && dedup(text) instanceof String s) {
         crLog(new StringBuilder(s), false);// System.out.print("\r" + s);
         Log.logPos=s.length();
      }
      tryUnlock(l);
   }
   @SuppressWarnings("unused")
   static private void logln(String text, LEVEL... levels) {
      boolean l=tryLock();
      if (needsPrinting(levels) && dedup(text) instanceof String s) {
         StringBuilder sb=new StringBuilder(s).append(LF);
         if (Log.logPos + s.length() > Log.logMAXLEN)
            lfLog(sb, false);// System.out.print(s + System.lineSeparator());
         else
            log(sb.insert(0, getTS()), false);// System.out.print(System.lineSeparator());
         Log.logPos=0;
      }
      tryUnlock(l);
   }
   static public void lfLog(Collection<String> texts, LEVEL... levels) {
      boolean l=tryLock();
      if (needsPrinting(levels)) {
         for (String line:texts)
            if (dedup(line) instanceof String s) {
               // if (Log.logPos + s.length() > Log.logMAXLEN)
               // System.out.print(System.lineSeparator());
               lfLog(new StringBuilder(s), false); // System.out.print(s + System.lineSeparator());
               Log.logPos=0;
            }
      }
      tryUnlock(l);
   }
   @SuppressWarnings("unused")
   static private void logOw(String text, LEVEL... levels) {
      boolean l=tryLock();
      if (needsPrinting(levels) && dedup(text) instanceof String s) {
         StringBuilder sb=new StringBuilder(s).append(CR);
         if (Log.logPos + s.length() > Log.logMAXLEN)
            crLog(sb, false);// System.out.print("\r");
         else
            log(sb.insert(0, getTS()), false);// System.out.print(s); System.out.print("\r");
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
      // if (l && OUT.isHeldByCurrentThread())
      // OUT.unlock();
   }
   private static boolean tryLock() {
      // try {
      // return OUT.tryLock(5, TimeUnit.MILLISECONDS);
      // } catch (InterruptedException ignore) {/* */}
      return false;
   }
   /**
    * @param string
    * @param errors
    */
   public static void lfErr(String text, LEVEL levels) {
      boolean l=tryLock();
      if (needsPrinting(levels) && dedup(text) instanceof String s) {
         StringBuilder sb=new StringBuilder(s);
         // if (Log.errPos + s.length() > Log.logMAXLEN)
         // sb., true);// System.err.print(System.lineSeparator());
         // else
         log(sb, true);// System.err.print(s + System.lineSeparator());
         // Log.errPos=0;
      }
      tryUnlock(l);
   }
   static private String getTS() {
      if (!withTimestamp)
         return "";
      // try { Thread.sleep(1000); } catch (InterruptedException e) {/* */ }
      int ms=(int) (System.currentTimeMillis() - start);
      int s=ms / 1000;
      int m=s / 60;
      int h=m / 60; // ms%=1000; s%=60; m%=60; h%=24;
      return String.format("[%2d%2d%2d.%3d] ", h % 24, m % 60, s % 60, ms % 1000);
   }
   /* ----------------------------------------------------------------------------- */
   final static private String CR="\r";
   final static private String LF="\n";
   static public void crLog(StringBuilder sb, boolean err) {
      if (withTimestamp)
         sb.insert(0, getTS());
      log(sb.insert(0, CR), err);
   }
   static public void lfLog(StringBuilder sb, boolean err) {
      if (withTimestamp)
         sb.insert(0, getTS());
      log(sb.insert(0, LF), err);
   }
   static public void log(StringBuilder sb, boolean err) {
      if (err)
         System.err.print(compress(sb));
      else
         System.err.print(compress(sb));
   }
   static ArrayList<Map.Entry<String, String>> compressor=new ArrayList<>();
   static public void tr(String from, String to) {
      compressor.add(Map.entry(from, to));
   }
   static private String compress(StringBuilder sb) {
      String erg=sb.toString();
      for (Entry<String, String> e:compressor)
         erg=erg.replaceAll(e.getKey(), e.getValue());
      return erg;
   }
}
