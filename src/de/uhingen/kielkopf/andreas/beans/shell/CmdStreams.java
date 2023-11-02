package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.*;
import java.lang.ProcessHandle.Info;
import java.nio.channels.AsynchronousCloseException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * Zusammenfassung von beiden Streams (out und err) zu einem Objekt
 * 
 * @author Andreas Kielkopf
 */
public class CmdStreams implements AutoCloseable {
   static final ConcurrentSkipListMap<String, CmdStreams> cache  =new ConcurrentSkipListMap<>();
   final String                                           key;
   final Process                                          process;
   final AtomicBoolean                                    processed;
   private final CmdBufferedReader                        out;
   private final CmdBufferedReader                        err;
   static AtomicInteger                                   counter=new AtomicInteger(0);
   private final int                                      nr;
   static ReentrantLock                                   get    =new ReentrantLock();
   @SuppressWarnings("resource")
   private CmdStreams(String cmd) throws IOException {
      key=cmd;
      nr=counter.incrementAndGet();
      // System.out.println(nr + " " + cmd);
      ProcessBuilder builder=new ProcessBuilder(List.of("/bin/bash", "-c", cmd));
      builder.environment().putIfAbsent("SSH_ASKPASS_REQUIRE", "prefer");
      process=builder.start();
      processed=new AtomicBoolean(false);
      out=new CmdBufferedReader(nr + " out", process.getInputStream());
      err=new CmdBufferedReader(nr + " err", process.getErrorStream());
   }
   /**
    * 
    * @param o
    *           String oder
    * @return
    * @throws IOException
    */
   static public CmdStreams getCachedStream(String s) throws IOException {
      if (!(s instanceof String cmd))
         return null;
      return getCachedStream(s, s);
   }
   /**
    * @param subvolumeListCmd
    * @param keyD
    * @return
    * @throws IOException
    */
   @SuppressWarnings("resource")
   public static CmdStreams getCachedStream(String s, String key) throws IOException {
      get.lock();
      try {
         if (!(s instanceof String cmd))
            return null;
         if (cache.containsKey(key)) // aus dem cache antworten, wenn es im cache ist
            cache.get(key).waitFor(); // der Befehl muß aber vorher fertig geworden sein !
         else
            cache.putIfAbsent(key, new CmdStreams(cmd));
         return cache.get(key);
      } finally {
         get.unlock();
      }
   }
   static public CmdStreams getDirectStream(String s) throws IOException {
      get.lock();
      try {
         if (!(s instanceof String cmd))
            return null;
         if (cache.containsKey(cmd)) // aus dem cache entfernen, wenn es im cache ist
            cache.remove(cmd).close();
         return new CmdStreams(cmd);
      } finally {
         get.unlock();
      }
   }
   /**
    * Beim ersten mal den echten Stream liefern. später den aus dem cache
    * 
    * @return erg
    * @throws AsynchronousCloseException
    */
   public Stream<String> outLines() {
      return out.lines();
   }
   public Stream<String> errLines() {
      return err.lines();
   }
   /**
    * Den err-Stream im Background lesen, out übergeben
    * 
    * @return out
    * @throws AsynchronousCloseException
    */
   public Stream<String> outBGerr() throws AsynchronousCloseException {
      err.fetchVirtual();
      return outLines();
   }
   /**
    * Den out-Stream im Background lesen, err übergeben
    * 
    * @return err
    * @throws AsynchronousCloseException
    */
   public Stream<String> errBGout() throws AsynchronousCloseException {
      out.fetchVirtual();
      return errLines();
   }
   public void errPrintln() {
      errLines().forEach(System.err::println);
   }
   @Override
   public String toString() {
      Info info=process.info();
      return new StringBuilder("CStream(id=").append(process.pid()).append(")")//
               .append(process.isAlive() ? "r+" : "c=")// running/completed
               .append(info.totalCpuDuration().orElse(Duration.ZERO)).append(" ")//
               .append(info.commandLine().orElse("null")).toString();
   }
   /**
    * Warte bis der Process fertig ist, und vollstandig im cache.
    * 
    * @return
    * @throws AsynchronousCloseException
    */
   public int waitFor() {
      if (processed.get())
         return 0;
      try {
         int x=process.waitFor();
         err.waitFor();
         out.waitFor();
         processed.set(true);
         return x;
      } catch (InterruptedException ignore) {
         System.err.println(ignore);
      }
      return -1;
   }
   /**
    * Schließt diesen Stream automatisch nachdem alles gelesen wurde. Der Inhalt des Streams bleibt im cache
    * 
    * @throws IOException
    */
   @Override
   public void close() {
      // waitFor();
      try {
         out.close();
      } catch (IOException e) {/* erg wurde gelesen */
         System.err.println(e);
      }
      try {
         err.close();
      } catch (IOException e) { /* errlist ist komplett jetzt */
         System.err.println(e);
      }
      process.destroy();
   }
   /**
    * Einzelenes Cmd sauber aus dem cache entfernen und close() ausführen
    * 
    * @param cmd
    */
   static public void removeFromCache(String cmd) {
      if (cache.containsKey(cmd)) {
         get.lock();
         try {
            cache.remove(cmd).close();
         } finally {
            get.unlock();
         }
      }
   }
   /**
    * Wenn die Streams nicht mehr gebraucht werden, alle aufräumen
    */
   static public void cleanup() {
      for (String cmd:cache.keySet())
         removeFromCache(cmd);
   }
   /**
    * @author Andreas Kielkopf Demonstrator
    * 
    *         <pre>
    * Anleitung:
    *    1.) Erzeuge eine Commandozeile die auszuführen ist
    *       String cmd="ls -lA ~ ";        
    *    2.) Erzeuge einen CmdStreams der den Befehl ausführt
    *       try (CmdStreams ccs=getCachedCmdStream(cmd)) {
    *    3.) Verknüpfe die Standardstreams OUT und ERR mit einem Stream der die Daten verarbeitet
    *          ccs.outBgErr().forEach(System.out::println);
    *          ccs.err().forEach(System.err::println);
    *    4.) Beide Streams werden mit der schließenden Klammer geclosed ;-)
    *       }
    *    5. wiederhole das selbe mit einem anderen Filter
    *    6.) behandle die möglichen Fehler
    *         catch (IOException | InterruptedException e) {
    *          e.printStackTrace(); // TODO Auto-generated catch block
    *       }
    *    
    *    7. lösche die gecachten Daten (optional)
    *         </pre>
    * 
    * @param args
    */
   public static void main(String[] args) {
      String cmd="ls -lA ~";
      try {
         try (CmdStreams ccs1=getCachedStream(cmd)) {
            System.out.println("-----------------------------"); // don`t care about anything
         }
         try (CmdStreams ccs1=getCachedStream(cmd)) {
            ccs1.outLines().filter(s -> s.contains("drwx-")).forEach(System.out::println); // don`t care about errors
         }
         System.out.println("-----------------------------");
         try (CmdStreams ccs1=getCachedStream(cmd)) {
            ccs1.outBGerr().filter(s -> s.contains("drwx-")).forEach(System.out::println);
         }
         System.out.println("-----------------------------");
         try (CmdStreams ccs2=getCachedStream(cmd)) {
            ccs2.outBGerr().filter(s -> s.contains("lrwx")).forEach(System.out::println);
            ccs2.errLines().forEach(System.err::println);
         }
         System.out.println("-----------------------------");
         try (CmdStreams dcs=getDirectStream(cmd)) {
            dcs.errLines().forEach(System.err::println);
            dcs.outBGerr().filter(s -> s.contains("-rwx")).forEach(System.out::println);
         }
      } catch (IOException e) { // TODO Auto-generated catch block
         System.err.println(e);
      }
      removeFromCache(cmd);// optional
      cleanup();
   }
}
