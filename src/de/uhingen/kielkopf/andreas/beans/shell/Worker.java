/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author Andreas Kielkopf
 * 
 *         Objekt um die Auswertung von stdout und stderr zu erleichtern
 */
public class Worker {
   /** Startzeit des Programms in ms */
   final private static long  start     =System.currentTimeMillis();
   /** Ein Worker der alles abholt und verwirft */
   final public static Worker nullWorker=new Worker();
   /**
    * Verarbeite eine Zeile des Output
    * 
    * @param line
    * 
    */
   public void processLine(String line) {/* do nothing */}
   /** Gib die ArrayList mit den Ergebnissen her */
   public ConcurrentLinkedDeque<String> get() {
      if (erg == null)
         erg=new ConcurrentLinkedDeque<>();
      return erg;
   }
   public String getFirst() {
      if (erg instanceof ConcurrentLinkedDeque<String> al && (!al.isEmpty()))
         return al.getFirst();
      return "";
   }
   private ConcurrentLinkedDeque<String> erg;
   /** Ein Worker der alles auf die Standardausgabe schreibt */
   final public static Worker stdOut() {
      if (sharedStdOut == null)
         sharedStdOut=new Worker() {
            @Override
            public void processLine(String line) {
               System.out.println(withTimestamp ? withTS(line) : line);
            }
            @Override
            public String toString() {
               return "stdOut-Worker";
            }
         };
      return sharedStdOut;
   }
   private static Worker sharedStdOut;
   /** Ein Worker der alles auf die Errorausgabe schreibt (shared) */
   final public static Worker stdErr() {
      if (sharedStdErr == null)
         sharedStdErr=new Worker() {
            @Override
            public void processLine(String line) {
               System.err.println(withTimestamp ? withTS(line) : line);
            }
            @Override
            public String toString() {
               return "stdErr-Worker";
            }
         };
      return sharedStdErr;
   }
   private static Worker sharedStdErr;
   /** Sammle die Ausgaben des Programms um sie später als Arraylist zu erhalten */
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
   /** Sammle die Fehler des Programms um sie später als Arraylist zu erhalten */
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
   /** Soll jeweils ein Timestamp eingefügt werden ? */
   public static boolean withTimestamp=true;
   /* Füge einen Timestamp ein */
   static private String withTS(String line) {
      // try { Thread.sleep(1000); } catch (InterruptedException e) {/* */ }
      int ms=(int) (System.currentTimeMillis() - start);
      int s=ms / 1000;
      int m=s / 60;
      int h=m / 60; // ms%=1000; s%=60; m%=60; h%=24;
      return new StringBuilder(line).insert(0, //
               String.format("[%2d%2d%2d.%3d] ", h % 24, m % 60, s % 60, ms % 1000)).toString();
   }
   /** Lösche das Array, dmait der Worker erneut genutzt werden kann */
   public void clean() {
      erg=null;
   }
}
