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
 * Zusammenfassung von 2 Streams (out und err) zu einem Objekt
 * 
 * Der cache soll genutzt werden, wenn die Commands Infos liefern, die wieder verwendet werden dürfen
 * 
 * @author Andreas Kielkopf
 */
public class CmdStreams implements AutoCloseable {
   /** Cache mit den bisher abgefragten Commands */
   static final ConcurrentSkipListMap<String, CmdStreams> cmdCache   =new ConcurrentSkipListMap<>();
   /** Wie oft wurde irgendein neues Cmd abgerufen */
   static final AtomicInteger                             readCounter=new AtomicInteger(0);
   static final ReentrantLock                             getLock    =new ReentrantLock();
   /** Der Process der cmd0 ausführt */
   private final Process                                  cmdProcess;
   private final AtomicBoolean                            cmdProcessed;
   private final AtomicBoolean                            firstCmdClosed;
   private final CmdBufferedReader                        cmdOut;
   private final CmdBufferedReader                        cmdErr;
   /** fortlaufende Nummer dess cmd */
   private final int                                      nr;
   @SuppressWarnings("resource")
   private CmdStreams(String cmd) throws IOException {
      firstCmdClosed=new AtomicBoolean(false);
      nr=readCounter.incrementAndGet(); // System.out.println(nr + " " + cmd);
      ProcessBuilder builder=new ProcessBuilder(List.of("/bin/bash", "-c", cmd));
      cmdProcess=builder.start();
      cmdProcessed=new AtomicBoolean(false);
      cmdOut=new CmdBufferedReader(nr + " out", cmdProcess.getInputStream());
      cmdErr=new CmdBufferedReader(nr + " err", cmdProcess.getErrorStream());
   }
   /**
    * @param cmd0
    *           String cmd0 ausführen und das Ergebniss cachen
    * @return Antwort des cmd0
    * @throws IOException
    */
   static public CmdStreams getCachedStream(String cmd0) throws IOException {
      if (!(cmd0 instanceof String cmd))
         return null;
      return getCachedStream(cmd, cmd);
   }
   /**
    * @param subvolumeListCmd
    * @param keyD
    * @return
    * @throws IOException
    */
   @SuppressWarnings("resource")
   public static CmdStreams getCachedStream(String cmd0, String key) throws IOException {
      getLock.lock();
      try {
         if (!(cmd0 instanceof String cmd))
            return null;
         if (cmdCache.containsKey(key)) // aus dem cache antworten, wenn es im cache ist
            cmdCache.get(key).waitFor(); // der Befehl muß aber vorher fertig geworden sein !
         else
            cmdCache.putIfAbsent(key, new CmdStreams(cmd));
         return cmdCache.get(key);
      } finally {
         getLock.unlock();
      }
   }
   /**
    * Cmd0 Ausführen ohne den Cache zu benutzen
    * 
    * @param cmd0
    * @return
    * @throws IOException
    */
   static public CmdStreams getDirectStream(String cmd0) throws IOException {
      getLock.lock();
      try {
         if (!(cmd0 instanceof String cmd))
            return null;
         if (cmdCache.containsKey(cmd)) // aus dem cache entfernen, wenn es im cache ist
            cmdCache.remove(cmd).close();
         return new CmdStreams(cmd);
      } finally {
         getLock.unlock();
      }
   }
   /**
    * Beim ersten mal den echten Stream liefern. später den aus dem cache
    * 
    * @return erg
    * @throws AsynchronousCloseException
    */
   public Stream<String> outLines() {
      return cmdOut.lines();
   }
   public Stream<String> errLines() {
      return cmdErr.lines();
   }
   /**
    * Den err-Stream im Background lesen, out übergeben
    * 
    * @return out
    * @throws AsynchronousCloseException
    */
   public Stream<String> outBGerr() throws AsynchronousCloseException {
      cmdErr.fetchVirtual();
      return outLines();
   }
   /**
    * Den out-Stream im Background lesen, err übergeben
    * 
    * @return err
    * @throws AsynchronousCloseException
    */
   public Stream<String> errBGout() throws AsynchronousCloseException {
      cmdOut.fetchVirtual();
      return errLines();
   }
   public void errPrintln() {
      errLines().forEach(System.err::println);
   }
   /** Infos fürs debugging */
   @Override
   public String toString() {
      Info info=cmdProcess.info();
      return new StringBuilder("CStream").append(nr).append("(id=").append(cmdProcess.pid()).append(")")//
               .append(cmdProcess.isAlive() ? "r+" : "c=")// running/completed
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
      if (cmdProcessed.get())
         return 0;
      while (cmdProcess.isAlive() && !firstCmdClosed.get())
         Thread.onSpinWait();
      try {
         int x=cmdProcess.waitFor();
         cmdErr.waitFor();
         cmdOut.waitFor();
         cmdProcessed.set(true);
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
   public void close() { // waitFor();
      try {
         cmdOut.close();
      } catch (IOException e) {/* erg wurde gelesen */
         System.err.println(e);
      }
      try {
         cmdErr.close();
      } catch (IOException e) { /* errlist ist komplett jetzt */
         System.err.println(e);
      }
      firstCmdClosed.set(false); // oder erst am ende ?
      cmdProcess.destroy();
   }
   /**
    * Einzelenes Cmd sauber aus dem cache entfernen und close() ausführen
    * 
    * @param cmd
    */
   static public void removeFromCache(String cmd) {
      if (cmdCache.containsKey(cmd)) {
         getLock.lock();
         try {
            cmdCache.remove(cmd).close();
         } finally {
            getLock.unlock();
         }
      }
   }
   /**
    * Wenn die Streams nicht mehr gebraucht werden, alle aufräumen
    */
   static public void cleanup() {
      for (String cmd:cmdCache.keySet())
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
