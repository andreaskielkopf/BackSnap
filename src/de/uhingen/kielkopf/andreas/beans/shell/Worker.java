/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author Andreas Kielkopf
 * 
 *         Objekt um die Auswertung von stdout und stderr zu erleichtern
 */
public abstract class Worker {
   /** Startzeit des Programms in ms */
   final private static long start=System.currentTimeMillis();
   /**
    * Verarbeite eine Zeile des Output
    * 
    * @param line
    */
   abstract public void processLine(String line);
   /** Gib die ArrayList mit den Ergebnissen her */
   public ConcurrentLinkedDeque<String> get() {
      if (erg == null)
         erg=new ConcurrentLinkedDeque<>();
      return erg;
   }
   public String getFirst() {
      return (erg instanceof ConcurrentLinkedDeque<String> al && (!al.isEmpty())) ? al.getFirst() : "";
   }
   public Worker withVirtual(BufferedReader br, String kennung) {
      busy=true;
      Thread.startVirtualThread(r(br, kennung));
      return this;
   }
   public Worker withPlatform(BufferedReader br, String kennung) {
      busy=true;
      Thread.ofPlatform().start(r(br, kennung));
      return this;
   }
   private Runnable r(BufferedReader br, String kennung) {
      return () -> {
         try {
            if (kennung == null)
               while (br.readLine() instanceof String line)
                  processLine(line);
            else
               while (br.readLine() instanceof String line)
                  processLine(kennung + line);
         } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
         } finally {
            busy=false;
         }
      };
   }
   private boolean busy=false;
   public Worker waitFor() {
      try {
         while (busy) {
            Thread.onSpinWait();
            Thread.sleep(1);
         }
      } catch (InterruptedException ignore) {/* */ }
      return this;
   }
   private ConcurrentLinkedDeque<String> erg;
   /** Ein Worker der alles abholt und verwirft (shared) */
   final public static Worker nullWorker() {
      if (sharedNullWorker == null)
         sharedNullWorker=new Worker() {
            @Override
            public void processLine(String line) { /* ignore */ }
            @Override
            public String toString() {
               return "null-Worker";
            }
         };
      return sharedNullWorker;
   }
   private static Worker sharedNullWorker;
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
   /** Ein Worker der auf die Errorausgabe schreibt (shared) */
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
   /** Sammle die Ausgaben des Programms um sie später als Queue zu erhalten */
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
   /** Sammle die Fehler des Programms um sie später als Queue zu erhalten */
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
