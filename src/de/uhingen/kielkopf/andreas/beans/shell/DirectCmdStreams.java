/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;

/**
 * 
 * Hilfkonstrukt um einen Prozess und dessen Streams zu managen
 * 
 * @author Andreas Kielkopf
 *
 */
public class DirectCmdStreams extends Streams implements AutoCloseable {
   final private @NonNull BufferedCmdReader outReader;
   final private @NonNull BufferedCmdReader errReader;
   final private Process                    process;
   final private String                     cmd;
   final private int                        nr;
   @SuppressWarnings("resource")
   public DirectCmdStreams(String cmd0) throws IOException {
      cmd=cmd0;
      process=new ProcessBuilder(List.of(SHELL, "-c", cmd)).start();
      nr=streamNr.incrementAndGet();
      outReader=new BufferedCmdReader(nr + " out", process, process.getInputStream());
      errReader=new BufferedCmdReader(nr + " err", process, process.getErrorStream());
   }
   public BufferedCmdReader out() {
      return outReader;
   }
   public BufferedCmdReader err() {
      return errReader;
   }
   public Process process() {
      return process;
   }
   /** Close wartet, bis der Prozess beendet ist. erst dann werden auch die Teilprozesse closed */
   @Override
   public void close() throws Exception {
      System.out.print("¹");
      process.waitFor();
      System.out.print("²");
      outReader.close();
      errReader.close();
      if (!process.waitFor(1, TimeUnit.SECONDS))
         System.err.println(Class.class.getSimpleName() + " " + nr + " " + cmd + " is running after close() ");
      System.out.print("³");
   }
   public void print2Err() {
      final Stream<String> l=errReader.lines();
      Thread.ofVirtual().start(() -> l.forEach(System.err::println));
   }
   public void print2Out() {
      final Stream<String> l=errReader.lines();
      Thread.ofVirtual().start(() -> l.forEach(System.out::println));
   }
}
