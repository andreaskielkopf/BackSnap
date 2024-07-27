/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Andreas Kielkopf
 *
 */
public class DoPiped implements Runnable {
   static {
      Do.shell();
      Do.root();
   }
   public static void main(String[] args) {
      Do a=Do.toPipe(List.of("ls", "-la"));
      Do g=Do.toWorker(List.of("grep", "-E", "^drwx"));
      Do b=Do.toPipe(List.of("sort", "-nk 5"));
      // Do b=new Do(List.of("sort", "-k 9"));
      DoPiped p=DoPiped.executePlatform(List.of(a, b, g));
      for (String line:g.inpWorker.get())
         System.out.println(line);
      for (String line:g.errWorker.get())
         System.err.println(line);
      a=Do.toPipe(List.of("ls", "-la"));
      g=Do.toConsole(List.of("grep", "-E", "^drwx"));
      b=Do.toPipe(List.of("sort", "-nk 5"));
      p=DoPiped.executePlatform(List.of(a, b, g));
      List<String> l1=List.of("ls", "-la");
      List<String> l3=List.of("grep", "-E", "^drwx");
      List<String> l2=List.of("sort", "-nk 5");
      DoPiped.toWorker(l1,l2,l3);
   }
   /**
    * @param l1
    * @param l2
    * @param l3
    */
   private static void toWorker(List<String> ... l ) {
      List<String>[] n=l;
   }
   private List<Do>      pipedDos;
   List<ProcessBuilder>  builders=new ArrayList<>();
   private boolean       done    =false;
   private List<Process> processes;
   public DoPiped(List<Do> dos/* , Worker oWorker, Worker eWorker */) {
      if (dos.isEmpty())
         throw new NullPointerException("Es wurde kein Befehl übergeben");
      pipedDos=dos;
      for (Do do1:pipedDos) {
         if (do1.done)
            throw new NullPointerException("Der Befehl " + do1 + " ist schon fertig");
         if (do1.process != null)
            throw new NullPointerException("Der Befehl " + do1 + " wurde schon begonnen");
         builders.add(do1.builder);
      }
   }
   @Override
   public void run() {
      try {
         processes=ProcessBuilder.startPipeline(builders); // alle Prozesse gemeinsam starten
         int index=0;
         for (Process process:processes)
            pipedDos.get(index++).process=process;
         Do lastdo=pipedDos.getLast();
         lastdo.run(); // Daten auswerten
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         done=true;
         for (Do do1:pipedDos)
            do1.done=true;
      }
   }
   private void waitFor() {
      try {
         while (done == false) {
            Thread.onSpinWait();
            Thread.sleep(1L);
         }
      } catch (InterruptedException ignore) {/* */ }
   }
   public DoPiped executePlatform() {
      if (processes == null)
         Thread.ofPlatform().start(this);
      waitFor();
      return this;
   }
   static public DoPiped executePlatform(List<Do> dos) {
      return new DoPiped(dos).executePlatform();
   }
   public Object get() {
      executePlatform();
      return pipedDos.getLast().get();
   }
}
