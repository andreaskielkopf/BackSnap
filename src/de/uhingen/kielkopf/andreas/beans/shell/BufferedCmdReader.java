/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * @author Andreas Kielkopf
 *
 */
public class BufferedCmdReader extends BufferedReader implements AutoCloseable {
   static final String UTF_8="UTF-8";  // verwende immer UTF-8
   static final int    bSize=0x100000; // 1M Buffer für jeden Stream
   final String        name;           // name nur fürs debugging
   final Process       process;        // von diesem prozess stammen die Daten
   AtomicBoolean       isClosed;
   /**
    * @param in
    * @throws UnsupportedEncodingException
    */
   public BufferedCmdReader(String name0, Process p, InputStream in) throws UnsupportedEncodingException {
      super(new InputStreamReader(in, UTF_8), bSize);
      process=p;
      name=name0;
   }
   @Override
   public Stream<String> lines() {
      if (isClosed == null) { // nur einmalig erlauben
         isClosed=new AtomicBoolean(false);
         return super.lines().onClose(() -> isClosed.set(true));
      }
      return null;
   }
   @Override
   public void close() throws IOException {
      try {
         process.waitFor(); // warte auf jeden Fall bis der Prozess beendet ist
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      if (isClosed.compareAndSet(false, true))
         super.close();
   }
   public boolean isClosed() {
      return isClosed.get();
   }
   /** Lies den kompletten Stream, aber ignoriere den Inhalt */
   public void ignore() {
      final Stream<String> l=lines();
      Thread.ofVirtual().start(() -> {
         @SuppressWarnings("unused")
         long n=l.count();
      });
   }
}
