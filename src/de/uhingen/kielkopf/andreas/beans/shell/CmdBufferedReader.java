/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.*;
import java.nio.channels.AsynchronousCloseException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
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
   static final String                   UTF_8  ="UTF-8";
   static final int                      bSize  =0x10000;                      // 64k Buffer für jeden Stream
   private ConcurrentLinkedQueue<String> queue  =new ConcurrentLinkedQueue<>();
   AtomicBoolean                         used   =new AtomicBoolean(false);
   AtomicBoolean                         virtual=new AtomicBoolean(false);
   AtomicBoolean                         closed =new AtomicBoolean(false);
   ReentrantLock                         open   =new ReentrantLock(true);
   private Status                        status;
   public enum Status {
      CREATED, STARTED, CLOSED, QUEUE;
   }
   /**
    * @param in
    * @throws UnsupportedEncodingException
    */
   public CmdBufferedReader(InputStream in) throws UnsupportedEncodingException {
      super(new InputStreamReader(in, UTF_8), bSize);
      status=Status.CREATED;
   }
   /**
    * Beim ersten mal den echten Stream liefern. später den aus der queue
    * 
    * @return erg
    * @throws AsynchronousCloseException
    */
   @Override
   public Stream<String> lines() {
      if (closed.get() || used.get()) {
         status=Status.QUEUE;
         return queue.stream();// Replay stream
      }
      if (used.compareAndSet(false, true)) {
         open.lock();
         status=Status.STARTED;
         return super.lines().peek(queue::add).onClose(() -> {
            closed.set(true);
            status=Status.CLOSED;
            open.unlock();
         });
      }
      throw new UncheckedIOException(new AsynchronousCloseException());
   }
   Thread fetchVirtual() {
      if (!used.get())
         if (virtual.compareAndSet(false, true)) {
            return Thread.ofVirtual().name("fetch").start(() -> lines().count()); // alles lesen und in peek() verarbeiten
         }
      return null;
   }
   public void waitFor() {
      while (open.isLocked())
         Thread.yield();
      if (fetchVirtual() instanceof Thread t)
         try {
            t.join();
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
   }
   @Override
   public void close() throws IOException {
      if (!used.get())
         waitFor();
      if (closed.compareAndSet(false, true))
         super.close();
   }
   public Status getStatus() {
      return status;
   }
}
