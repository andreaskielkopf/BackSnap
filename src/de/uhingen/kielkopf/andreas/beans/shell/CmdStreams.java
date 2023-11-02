package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.*;
import java.lang.ProcessHandle.Info;
import java.nio.channels.AsynchronousCloseException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Zusammenfassung von beiden Streams (out und err) zu einem Objekt
 * 
 * @author Andreas Kielkopf
 */
public class CmdStreams implements AutoCloseable {
   static final ConcurrentSkipListMap<String, CmdStreams> cache    =new ConcurrentSkipListMap<>();
   final String                                           key;
   final Process                                          process;
   final AtomicBoolean                                    processed;
   private final CmdBufferedReader                        out;
   private final CmdBufferedReader                        err;
   private AtomicBoolean                                  wasClosed=new AtomicBoolean(false);
   @SuppressWarnings("resource")
   private CmdStreams(String cmd) throws IOException {
      key=cmd;
      ProcessBuilder builder=new ProcessBuilder(List.of("/bin/bash", "-c", cmd));
      builder.environment().putIfAbsent("SSH_ASKPASS_REQUIRE", "prefer");
      process=builder.start();
      processed=new AtomicBoolean(false);
      out=new CmdBufferedReader("out", process.getInputStream());
      err=new CmdBufferedReader("err", process.getErrorStream());
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
      if (!(s instanceof String cmd))
         return null;
      if (cache.containsKey(key)) // aus dem cache antworten, wenn es im cache ist
         cache.get(key).waitFor(); // der Befehl muß aber vorher fertig geworden sein !
      else
         cache.putIfAbsent(key, new CmdStreams(cmd));
      return cache.get(key);
   }
   static public CmdStreams getDirectStream(String s) throws IOException {
      if (!(s instanceof String cmd))
         return null;
      if (cache.containsKey(cmd)) // aus dem cache entfernen, wenn es im cache ist
         cache.remove(cmd).close();
      return new CmdStreams(cmd);
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
      out.consume();
      err.consume();
      if (processed.get())
         return 0;
      try {
         int x=process.waitFor();
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
      // System.out.println("CmdStream.close()");
      if (wasClosed.compareAndSet(false, true)) {
         if (!out.wasReadF.get())
            if (out.fetchVirtual() instanceof Future<Boolean> f) {
               // System.out.println("out-Va");
               try {
                  f.get();
               } catch (InterruptedException | ExecutionException e) { // TODO Auto-generated catch block
                  e.printStackTrace();
               }
               // System.out.println("out-Vb");
            }
         if (!err.wasReadF.get())
            if (err.fetchVirtual() instanceof Future<Boolean> f) {
               // System.out.println("err-Va");
               try {
                  f.get();
               } catch (InterruptedException | ExecutionException e) { // TODO Auto-generated catch block
                  e.printStackTrace();
               }
               // System.out.println("err-Vb");
            }
         try {
            process.waitFor();
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
         try {
            // out.consume();
            // out.waitFor();
            if (out.openStreams.get() > 0)
               out.close();
         } catch (Exception e) {/* erg wurde gelesen */
            System.err.println(e);
         }
         try {
            // err.waitFor();
            if (err.openStreams.get() > 0)
               err.close();
         } catch (Exception e) { /* errlist ist komplett jetzt */
            System.err.println(e);
         }
         // System.out.println("CmdStream.closed()");
      }
      // process.destroy();
   }
   /**
    * Einzelenes Cmd sauber aus dem cache entfernen und close() ausführen
    * 
    * @param cmd
    */
   static public void removeFromCache(String cmd) {
      if (cache.containsKey(cmd))
         cache.remove(cmd).close();
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
         System.out.println("0----------------------------"); // don`t care about anything
         try (CmdStreams ccs1=getCachedStream(cmd)) {
            System.out.println("1----------------------------"); // don`t care about anything
         }
         try (CmdStreams ccs1=getCachedStream(cmd)) {
            System.out.println("2----------------------------");
            ccs1.outLines().filter(s -> s.contains("drwx-")).forEach(System.out::println); // don`t care about errors
         }
         try (CmdStreams ccs1=getCachedStream(cmd)) {
            System.out.println("2b---------------------------");
            ccs1.outLines().filter(s -> s.contains("drwx-")).forEach(System.out::println); // don`t care about errors
            ccs1.errLines().forEach(System.out::println); // don`t care about errors
         }
         try (CmdStreams ccs1=getCachedStream(cmd)) {
            System.out.println("3----------------------------");
            ccs1.outBGerr().filter(s -> s.contains("drwx-")).forEach(System.out::println);
         }
         try (CmdStreams ccs1=getCachedStream(cmd)) {
            System.out.println("4----------------------------");
            ccs1.outBGerr().filter(s -> s.contains("lrwx")).forEach(System.out::println);
            ccs1.errLines().forEach(System.err::println);
         }
         try (CmdStreams ccs1=getCachedStream(cmd)) {
            System.out.println("4b---------------------------");
            ccs1.errLines().forEach(System.err::println);
            ccs1.outBGerr().filter(s -> s.contains("lrwx")).forEach(System.out::println);
         }
         try (CmdStreams dcs=getDirectStream(cmd)) {
            System.out.println("5----------------------------");
            dcs.outBGerr().filter(s -> s.contains("-rwx")).forEach(System.out::println);
            dcs.errLines().forEach(System.err::println);
         }
         System.out.println("99----------------------------");
      } catch (IOException e) { // TODO Auto-generated catch block
         System.err.println(e);
      }
      removeFromCache(cmd);// optional
      cleanup();
   }
}
