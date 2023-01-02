/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap;

import java.io.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * @author Andreas Kielkopf
 */
public class Commandline {
   /**
    * 
    */
   final static ProcessBuilder  processBuilder=new ProcessBuilder();
   // final static List<Process> processList =new CopyOnWriteArrayList<>();
   public final static String   UTF_8         ="UTF-8";
   /**
    * ExecutorService um den Errorstream im Hintergrund zu lesen
    */
   final public static ExecutorService background    =Executors.newCachedThreadPool();
   /**
    * @param cmd
    * @return
    * @throws IOException
    */
   public static CmdStream execute(StringBuilder cmd) throws IOException {
      return execute(cmd.toString());
   }
   /**
    * Einen Befehl ausführen, Fehlermeldungen direkt ausgeben, stdout als stream zurückgeben
    * 
    * @param cmd
    * @return 2 x Stream<String>
    * @throws IOException
    */
   @SuppressWarnings("resource")
   static CmdStream execute(String cmd) throws IOException {
      Process process=processBuilder.command(List.of("/bin/bash", "-c", cmd)).start();
      // processList.add(process); // collect all Lines into streams
      return new CmdStream(process, new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8)).lines(),
               new BufferedReader(new InputStreamReader(process.getErrorStream(), UTF_8)).lines());
   }
   // public static void cleanup() {
   // for (Process process:new CopyOnWriteArrayList<>(processList))
   // if (!process.isAlive())
   // processList.remove(process);
   // else
   // try {
   // System.out.print(" wF");
   // process.waitFor(5, TimeUnit.SECONDS);
   // } catch (InterruptedException e) { /** */
   // }
   // }
   /**
    * Zusammenfassung von beiden Streams (Ergebnis und Error) zu einem Objekt
    * 
    * @author Andreas Kielkopf
    */
   record CmdStream(Process process, Stream<String> erg, Stream<String> err) implements Closeable {
      public void backgroundErr() { // Fehler im Hintergrund ausgeben
         background.submit(() -> bE());// err().forEach(System.err::println));
      }
      private void bE() {
         try (err) {
            err().forEach(System.err::println);
         }
      }
      /**
       * Wenn die Errorstreams nicht mehr gebraucht werden, aufräumen
       */
      public static void cleanup() {
         background.shutdownNow();
      }
      @Override
      public void close() {
         err.close();
         erg.close();
         process.destroy();
      }
   }
}
