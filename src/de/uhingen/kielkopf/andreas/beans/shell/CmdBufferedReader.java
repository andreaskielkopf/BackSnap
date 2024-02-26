package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.*;
import java.nio.channels.AsynchronousCloseException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * @author Andreas Kielkopf
 * 
 *         Ein Reader, der zeilenweise Strings aus einem Inputstream liefert.
 * 
 *         Der Inhalt des Streams wird zwischengespeichert und kann mehrfach abgerufen werden
 */
public class CmdBufferedReader extends BufferedReader implements AutoCloseable {
   static final String                   UTF_8        ="UTF-8";                      // verwende UTF-8
   static final int                      bSize        =0x100000;                      // 1M Buffer für jeden Stream
   final String                          name;                                       // name nur fürs debugging
   private ConcurrentLinkedQueue<String> queue        =new ConcurrentLinkedQueue<>();
   /** Der erste Stream wurde angefangen zu lesen */
   private AtomicBoolean                 usedFirst    =new AtomicBoolean(false);
   /** Der erste Stream wurde komplett gelesen */
   private AtomicBoolean                 closedFirst  =new AtomicBoolean(false);
   /** Link auf den ersten Stream bis er closed() ist */
   private Stream<String>                firstStream;
   /** Die Anzahl, wie oft der Stream bisher angefordert wurde */
   AtomicInteger                         streamCounter=new AtomicInteger(0);
   /** Nur genau einen Hintergrund-Thread starten */
   AtomicBoolean                         virtual      =new AtomicBoolean(false);
   ReentrantLock                         line         =new ReentrantLock(true);
   /**
    * Erzeugt einen mehrfach lesbaren Stream aus einem InputStreamReader
    * 
    * @param in
    * @throws UnsupportedEncodingException
    */
   public CmdBufferedReader(String name0, InputStream in) throws UnsupportedEncodingException {
      super(new InputStreamReader(in, UTF_8), bSize);
      name=name0;
//      queue.add("");
   }
   /**
    * Beim ersten Mal den echten Stream liefern. Später den aus der queue
    * 
    * @return erg
    * @throws AsynchronousCloseException
    */
   @Override
   public Stream<String> lines() {
      line.lock();
      try {
         if (usedFirst.compareAndSet(false, true)) { // System.out.println(name + " first stream " + streamCounter.get());
            streamCounter.getAndIncrement();
            firstStream=super.lines().peek(queue::add).onClose(() -> closedFirst.set(true));
            // System.out.println(name + " first stream closed " + streamCounter.get());
            return firstStream;
         } // System.out.println(name + " queue stream " + streamCounter.get());
         streamCounter.getAndIncrement(); // Replay stream
         return queue.stream().onClose(() -> closedFirst.set(true));// System.out.println(name + " queue stream closed");
      } finally {
         line.unlock();
      }
   }
   /**
    * startet einen Hintergrund Thread um den Stream zu lesen
    * 
    * @return
    */
   public Thread fetchVirtual() {
      if (!usedFirst.get())
         if (virtual.compareAndSet(false, true)) { // System.out.println(name + " virtual start");
            return Thread.ofVirtual().name("fetch " + name).start(() -> {
               AtomicLong n=new AtomicLong(0);
               lines().forEachOrdered(w -> n.incrementAndGet());
               closedFirst.set(true); // System.out.println(name + " virtual ends " + n.get());
            }); // alles lesen und in peek() verarbeiten
         }
      return null;
   }
   public void waitFor() {
      if (fetchVirtual() instanceof Thread t)
         try {
            t.join();
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      while (!closedFirst.get())
         Thread.onSpinWait();
   }
   @Override
   public void close() throws IOException {
      if (!usedFirst.get())
         waitFor();
      if (firstStream != null) { // stelle sicher dass der reale Stream genau 1x closed() wird
         firstStream.close();
         firstStream=null;
      }
   }
}
