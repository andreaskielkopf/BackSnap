package de.uhingen.kielkopf.andreas.backsnap;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import de.uhingen.kielkopf.andreas.beans.Version;

/**
 * @author Andreas Kielkopf
 */
public class Commandline {
   static final ProcessBuilder                                  processBuilder=new ProcessBuilder();
   static public final String                                   UTF_8         ="UTF-8";
   static public final ConcurrentSkipListMap<String, CmdStream> cache         =new ConcurrentSkipListMap<>();
   /** ExecutorService um den Errorstream im Hintergrund zu lesen */
   static public final ExecutorService                          background    =Version.getVx();
   /**
    * @param cmd
    * @return
    * @throws IOException
    */
   static public CmdStream executeCached(StringBuilder cmd, String key) throws IOException {
      return executeCached(cmd.toString(), key);
   }
   static public CmdStream executeCached(StringBuilder cmd1) throws IOException {
      String cmd=cmd1.toString();
      return executeCached(cmd, cmd);
   }
   /**
    * Einen Befehl ausführen, Fehlermeldungen direkt ausgeben, stdout als stream zurückgeben
    * 
    * @param cmd
    * @param key
    *           Unter diesem Schlüssel wird die Antwort im Cache abgelegt. Mit diesem Schlüssel kann sie auch wieder
    *           gelöscht werden. ist der key null, wird nicht gecached.
    * @return 2 x Stream<String>
    * @throws IOException
    */
   @SuppressWarnings("resource")
   static public CmdStream executeCached(String cmd, String key) throws IOException {
      if ((key != null) && cache.containsKey(key)) // aus dem cache antworten, wenn es im cache ist
         return cache.get(key); // ansonsten den Befehl neu ausführen und im cache ablegen
      Process process=processBuilder.command(List.of("/bin/bash", "-c", cmd)).start();
      return new CmdStream(process, new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8)),
               new BufferedReader(new InputStreamReader(process.getErrorStream(), UTF_8)), new ArrayList<>(),
               new ArrayList<>(), key);
   }
   /**
    * Zusammenfassung von beiden Streams (Ergebnis und Error) zu einem Objekt
    * 
    * @author Andreas Kielkopf
    */
   public record CmdStream(Process process, BufferedReader brErg, BufferedReader brErr, List<String> errList,
            List<String> ergList, String key) implements Closeable {
      public void backgroundErr() { // Fehler im Hintergrund ausgeben und ablegen // System.out.print("0");
         if ((key != null) && cache.containsKey(key))
            return; // ist schon im cache
         background.submit(new Runnable() {
            @Override
            public void run() {
               try { // System.out.print("1");
                  try (BufferedReader q=brErr()) {
                     errList.addAll(q.lines()/* .peek(System.err::println) */.toList());
                  } // System.out.print("2"); // System.out.println("3");
               } catch (IOException e) {
                  e.printStackTrace();
               }
            }
         }); // System.out.print("4");
      }
      /**
       * Schließt diesen Stream automatisch wenn alles gelesen wurde. Wenn ein cache-key vergeben wurde, wird der Inhalt
       * des Streams gecaches
       * 
       * @throws IOException
       */
      @SuppressWarnings("resource")
      @Override
      public void close() throws IOException { // waitFor();
         brErr.close(); // errlist ist komplett jetzt
         brErg.close(); // erg wurde gelesen
         process.destroy();
         if ((key != null) && (!cache.containsKey(key))) {
            // System.out.println("enable " + key + " in cache");
            cache.put(key, this);
         }
      }
      /**
       * Während der Prozess läuft gib den aktuellen Stream zurück. später den aus dem cache
       * 
       * @return err
       */
      public Stream<String> err() {
         if ((key != null) && cache.containsKey(key))
            return errList.stream(); // aus dem cache
         // if ((key == null) && errList.isEmpty())
         if (key == null) // den cache ignorieren
            return brErr.lines();
         if (errList.isEmpty())
            return brErr.lines().peek(ergList::add); // legt alle Zeilen im cache-Array ab
         return errList.stream(); // eigene Konserve
      }
      /**
       * Während der Prozess läuft gib den aktuellen Stream zurück. später den aus dem cache
       * 
       * @return erg
       */
      public Stream<String> erg() {
         if ((key != null) && cache.containsKey(key)) {
            // System.out.println("use " + key + " from cache");
            return ergList.stream(); // aus dem cache
         }
         if (key == null) // den cache ignorieren
            return brErg.lines();
         // System.out.println("save " + key + " into cache");
         if (ergList.isEmpty()) // wird nur hier gefüllt , also einmal wahr, dann falsch
            return brErg.lines().peek(ergList::add); // legt alle Zeilen im cache-Array ab
         // System.err.println("reuse " + key + " from cache");
         return ergList.stream(); // Konserve
      }
      /**
       * 
       */
      public void waitFor() {
         try {
            process.waitFor();
         } catch (InterruptedException ignore) {
            System.err.println(ignore.toString());
         }
      }
   }
   /**
    * Wenn die Errorstreams nicht mehr gebraucht werden, aufräumen
    */
   static public void cleanup() {
      background.shutdownNow();
   }
   @SuppressWarnings("resource")
   static public void removeFromCache(String cacheKey) {
      if (!cache.containsKey(cacheKey))
         return;
      try {
         CmdStream v=cache.get(cacheKey);
         v.close();
      } catch (IOException ignore) {
         System.err.println(ignore.getMessage());
      }
      cache.remove(cacheKey);
   }
}
