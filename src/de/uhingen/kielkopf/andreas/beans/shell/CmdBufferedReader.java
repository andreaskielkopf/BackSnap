/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.*;
import java.nio.channels.AsynchronousCloseException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * @author Andreas Kielkopf
 * 
 *         Ein Reader, der zeilenweise Strings aus einem Inputstream liefert.
 * 
 *         Der Inhalt des Streams wird zwischengespeichert und kann mehrfach abgerufen werden
 */
public class CmdBufferedReader extends BufferedReader implements AutoCloseable {
   static final ExecutorService                  v          =Executors.newVirtualThreadPerTaskExecutor();
   static final String                           UTF_8      ="UTF-8";
   static final int                              bSize      =0x10000;                                    // 64k Buffer für jeden Stream
   private ConcurrentLinkedQueue<String>         queue      =new ConcurrentLinkedQueue<>();
   /** Flag for the first read that fills the queue */
   AtomicBoolean                                 wasReadF   =new AtomicBoolean(false);
   private AtomicBoolean                         wasClosedF =new AtomicBoolean(false);
   /** Zähle wie viele Streans noch offen sind */
   AtomicInteger                                 openStreams=new AtomicInteger(0);
   // AtomicBoolean used =new AtomicBoolean(false);
   private Stream<String>                        first;
   private ConcurrentLinkedQueue<Stream<String>> streams    =new ConcurrentLinkedQueue<>();
   private Future<Boolean>                       virtual;
   private final String                          name;
   /**
    * @param in
    * @throws UnsupportedEncodingException
    */
   public CmdBufferedReader(String name1, InputStream in) throws UnsupportedEncodingException {
      super(new InputStreamReader(in, UTF_8), bSize);
      this.name=name1;
   }
   /**
    * Beim ersten mal den echten Stream liefern. Abdem 2.mal dann den aus der queue
    * 
    * @return erg
    * @throws AsynchronousCloseException
    */
   @Override
   public Stream<String> lines() {
      openStreams.incrementAndGet();
//      System.out.println(name + " Read " + openStreams.get());
      if (!wasReadF.compareAndExchange(false, true)) {
         System.out.println(name + " ReadF " + openStreams.get());
         // System.out.println(name + " Read F");
         first=super.lines().peek(queue::add).onClose(() -> {
            wasClosedF.set(true);
            // System.out.println(name + " onClose F " + openStreams.get());
            openStreams.getAndDecrement();
         });
         streams.add(first);
         return first;
      }
      consume();
      System.out.println(name + " Read " + openStreams.get());
      // System.out.println(name + " Read N ");
      Stream<String> next=queue.stream().onClose(() -> {
         // System.out.println(name + " onClose N " + openStreams.get());
         openStreams.getAndDecrement();
      });
      streams.add(next);
      return next;
   }
   public void consume() {
      fetchVirtual();
      if (virtual != null)
         try {
            virtual.get();
         } catch (InterruptedException | ExecutionException e) { // TODO Auto-generated catch block
            e.printStackTrace();
         }
   }
   public Future<Boolean> fetchVirtual() {
      if (!wasReadF.get()) {
         // System.out.println(name + " B.fetchVirtual-a");
         Future<Boolean> q=v.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception { // TODO Auto-generated method stub
               // System.out.println(name + " B.virtual-b");
               @SuppressWarnings("unused")
               List<String> n=lines()/* .peek(w -> System.out.print(".")) */.toList();
               // System.out.println(name + " B.virtual-c " + openStreams.get() + " " + n.size());
               return true;
            }
         });
         virtual=q;
         return q;
      }
      return null;
   }
   /** Warte bis der erste Stream consumed ist */
   public void waitForF() {
      while (!wasClosedF.get())
         Thread.onSpinWait();
      // System.out.println(name + " B.waitFor()");
   }
   @Override
   public void close() throws IOException {
      while (!streams.isEmpty())
         streams.poll().close();
   }
}
