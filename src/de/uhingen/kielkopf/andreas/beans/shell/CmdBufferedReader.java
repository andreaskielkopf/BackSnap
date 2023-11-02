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
   static final String                   UTF_8      ="UTF-8";
   static final int                      bSize      =0x10000;                      // 64k Buffer für jeden Stream
   private ConcurrentLinkedQueue<String> queue      =new ConcurrentLinkedQueue<>();
   AtomicBoolean                         usedFirst  =new AtomicBoolean(false);
   AtomicBoolean                         closedFirst=new AtomicBoolean(false);
   AtomicBoolean                         virtual    =new AtomicBoolean(false);
   AtomicInteger                         counter    =new AtomicInteger(0);
   ReentrantLock                         line       =new ReentrantLock(true);
   Stream<String>                        realStream;
   // private Status status;
   final String                          name;
   public enum Status {
      CREATED, STARTED, CLOSED, QUEUE;
   }
   /**
    * @param in
    * @throws UnsupportedEncodingException
    */
   public CmdBufferedReader(String name0, InputStream in) throws UnsupportedEncodingException {
      super(new InputStreamReader(in, UTF_8), bSize);
      // status=Status.CREATED;
      name=name0;
   }
   /**
    * Beim ersten mal den echten Stream liefern. später den aus der queue
    * 
    * @return erg
    * @throws AsynchronousCloseException
    */
   @Override
   public Stream<String> lines() {
      line.lock();
      try {
         if (usedFirst.compareAndSet(false, true)) { // open.lock();
            // status=Status.STARTED;
            // System.out.println(name + " first stream " + counter.get());
            counter.getAndIncrement();
            realStream=super.lines().peek(queue::add).onClose(() -> {
               closedFirst.set(true);
               // System.out.println(name + " first stream closed " + counter.get());
               // status=Status.CLOSED; // open.unlock();
            });
            return realStream;
         }
         // if (closedFirst.get() || usedFirst.get()) {
         // status=Status.QUEUE;
         // System.out.println(name + " queue stream " + counter.get());
         counter.getAndIncrement();
         Stream<String> v=queue.stream().onClose(() -> {
            closedFirst.set(true);
            // System.out.println(name + " queue stream closed");
         });// Replay stream
         return v;
         // }
         // throw new UncheckedIOException(new AsynchronousCloseException());
      } finally {
         line.unlock();
      }
   }
   Thread fetchVirtual() {
      if (!usedFirst.get())
         if (virtual.compareAndSet(false, true)) {
            // System.out.println(name + " virtual start");
            Thread v=Thread.ofVirtual().name("fetch " + name).start(() -> {
               AtomicLong n=new AtomicLong(0);
               lines().forEachOrdered(w -> {
                  n.incrementAndGet();
               });
               closedFirst.set(true);
               // System.out.println(name + " virtual ends " + n.get());
            }); // alles lesen und in peek() verarbeiten
            return v;
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
      // if (closedFirst.compareAndSet(false, true)) {
      if (realStream != null) {
         realStream.close();
         realStream=null;
      }
      // super.close();
      // }
   }
   // public Status getStatus() {
   // return status;
   // }
}
