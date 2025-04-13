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
      // Do a=Do.toPipe(List.of("ls", "-la"));
      // Do b=Do.toPipe(List.of("sort", "-nk 5"));
      // Do g=Do.toWorker(List.of("grep", "-E", "^drwx"));
      // // Do b=new Do(List.of("sort", "-k 9"));
      // DoPiped p=DoPiped.executePlatform(List.of(a, b, g));
      // for (String line:g.inpWorker.get())
      // System.out.println(line);
      // for (String line:g.errWorker.get())
      // System.err.println(line);
      // a=Do.toPipe(List.of("ls", "-la"));
      // b=Do.toPipe(List.of("sort", "-nk 5"));
      // g=Do.toConsole(List.of("grep", "-E", "^drwx"));
      // p=DoPiped.executePlatform(List.of(a, b, g));
      // List<String> l1=List.of("ls", "-la");
      // List<String> l3=List.of("grep", "-E", "^drwx");
      // List<String> l2=List.of("sort", "-nk 5");
      // DoPiped.toWorker(l1,l2,l3);
      // System.err.println("Fehlerausgabe" + Arrays.toString(args));
      System.out.println("Standardausgabe");
      // Do.doCmd(List.of("-c", "time ~/bin/src|pv -pteabfW -i 1|~/bin/dst"));
      Do t1=Do.toPipe(List.of("/home/andreas/bin/src"));
      Do t2=Do.toPipe(List.of("pv", "-pteabfW", "-i", "1"));
      Do t3=Do.toConsole(List.of("/home/andreas/bin/dst"));
      DoPiped t=DoPiped.executePlatform(List.of(t1, t2, t3));
   }// pv
   /**
    * @param l1
    * @param l2
    * @param l3
    */
   // private static void toWorker(List<String>... l) {
   // List<String>[] n=l;
   // }
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
   @SuppressWarnings("resource")
   @Override
   public void run() {
      try {
         processes=ProcessBuilder.startPipeline(builders); // alle Prozesse gemeinsam starten
         int index=0;
         for (Process process:processes) // Prozesse zuweisen
            pipedDos.get(index++).process=process;
         Do lastdo=pipedDos.getLast();
         index=0;
         for (Do d:pipedDos)
            if (d != lastdo)
               d.errWorker.withPlatform(d.process.errorReader(),
                        new StringBuilder().append(++index).append(":").toString());
            else
               d.run(); // Daten auswerten
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
