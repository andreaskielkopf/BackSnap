/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.util.ArrayList;

/**
 * @author Andreas Kielkopf
 *
 */
public class Worker {
   public static boolean      withTimestamp=true;
   final private static long  start        =System.currentTimeMillis();
   final public static Worker stdInp       =new Worker() {
                                              @Override
                                              public void processLine(String line) {
                                                 System.out.println(withTimestamp ? withTS(line) : line);
                                              }
                                              @Override
                                              public String toString() {
                                                 return "stdInp-Worker";
                                              }
                                           };
   final public static Worker stdErr       =new Worker() {
                                              @Override
                                              public void processLine(String line) {
                                                 System.err.println(withTimestamp ? withTS(line) : line);
                                              }
                                              @Override
                                              public String toString() {
                                                 return "stdErr-Worker";
                                              }
                                           };
   final public static Worker nullWorker   =new Worker();
   /**
    * @param line
    */
   public void processLine(String line) {/* do nothing */}
   public ArrayList<String> get() {
      if (erg == null)
         erg=new ArrayList<>();
      return erg;
   }
   public String getFirst() {
      if (erg instanceof ArrayList<String> al && (!al.isEmpty()))
         return al.getFirst();
      return "";
   }
   private ArrayList<String> erg;
   final public static Worker collectInp() {
      return new Worker() {
         @Override
         public void processLine(String line) {
            get().add(withTimestamp ? withTS(line) : line);
         }
         @Override
         public String toString() {
            return "collectInp-Worker";
         }
      };
   }
   final public static Worker collectErr() {
      return new Worker() {
         @Override
         public void processLine(String line) {
            get().add(withTimestamp ? withTS(line) : line);
         }
         @Override
         public String toString() {
            return "collectErr()-Worker";
         }
      };
   }
   static private String withTS(String line) {
      // try { Thread.sleep(1000); } catch (InterruptedException e) {/* */ }
      int ms=(int) (System.currentTimeMillis() - start);
      int s=ms / 1000;
      int m=s / 60;
      int h=m / 60; // ms%=1000; s%=60; m%=60; h%=24;
      return new StringBuilder(line).insert(0, //
               String.format("[%2d%2d%2d.%3d] ", h % 24, m % 60, s % 60, ms % 1000)).toString();
   }
   public void clean() {
      erg=null;
   }
}
