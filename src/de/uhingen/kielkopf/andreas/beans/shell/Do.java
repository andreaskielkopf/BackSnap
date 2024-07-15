/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import javax.print.DocFlavor.STRING;

/**
 * Ausführen beliebiger Commands aus java herraus durch Benutzung der SHell ($SHELL)
 * 
 * @author Andreas Kielkopf
 *
 */
public class Do<V> implements Runnable {
   /** Welche Shell soll verwendet werden */
   public static String  SHELL  ="/bin/sh"; // ermitteln mit which $(cat /proc/$$/comm) ???
   public static Boolean iamroot=null;
   static {
      shell();
      root();
   }
   static void shell() {
      ArrayList<String> tmp=new ArrayList<>();
      String e=new Do<String>(List.of("-c", "echo ${SHELL}"), new Worker() {
         @Override
         public void processLine(String line) {
            tmp.add(line);
         }
      }) {
         @Override
         public String get() {
            executePlatform();
            return (tmp.isEmpty()) ? "" : tmp.getFirst();
         }
      }.get();
      SHELL=(e.isEmpty()) ? "/bin/sh" : e;
      System.out.print("$SHELL=" + SHELL);
   }
   static boolean root() {
      if (iamroot == null) {
         ArrayList<String> erg=new ArrayList<>();
         String e=new Do<String>(List.of("whoami"), new Worker() {
            @Override
            public void processLine(String line) {
               erg.add(line);
            }
         }) {
            @Override
            public String get() {
               executePlatform();
               return (erg.isEmpty()) ? "" : erg.getFirst();
            }
         }.get();
         System.out.println(", user=" + e);
         iamroot=Boolean.valueOf(e.equals("root")); // System.out.println(SHELL);
      }
      return iamroot;
   }
   public static void main(String[] args) {
      // String befehl="cat /etc/fstab|grep btrfs";
      // List<String> list=Arrays.asList(befehl.split(" "));
      // Do<String> d=new Do<String>(List.of("which $(cat /proc/$$/comm)"));
      // Thread.ofPlatform().start(d);
      // d.waitFor();
   }
   // public Do(String... s) { this(Arrays.asList(s)); }
   public Do(List<String> list) {
      this(list, Worker.stdInp, Worker.stdErr);
   }
   public Do(List<String> list, Worker oWorker) {
      this(list, oWorker, Worker.stdErr);
   }
   final private Worker         inpWorker;
   final private Worker         errWorker;
   final private ProcessBuilder builder;
   private Process              process;
   private boolean              done=false;
   private V                    v;
   /**
    * @param oWorker
    * @param eWorker
    * @param list
    */
   @SuppressWarnings("null")
   public Do(List<String> l, Worker iWorker, Worker eWorker) {
      if (l.isEmpty())
         throw new NullPointerException("Es wurde kein Befehl übergeben");
      ArrayList<String> list=new ArrayList<>(l);
      inpWorker=(iWorker != null) ? iWorker : Worker.nullWorker;
      errWorker=(eWorker != null) ? eWorker : Worker.nullWorker;
      if (list.getFirst().equals("-c"))
         list.addFirst(SHELL);
      builder=new ProcessBuilder(list);
   }
   /**
    * 
    */
   private void waitFor() {
      try {
         while (done == false) {
            Thread.onSpinWait();
            Thread.sleep(1L);
         }
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void run() {
      try {
         process=builder.start();
         try (BufferedReader err=process.errorReader(); BufferedReader inp=process.inputReader();) {
            while (process.isAlive())
               readAll(err, inp);
            readAll(err, inp);
         }
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         done=true;
      }
   }
   private void readAll(BufferedReader err, BufferedReader inp) throws IOException {
      try {
         Thread.sleep(1L);
         while (inp.ready())
            inpWorker.processLine(inp.readLine());
         while (err.ready())
            errWorker.processLine(err.readLine());
      } catch (InterruptedException ignoree) { /* */ }
   }
   public Do<V> executePlatform() {
      if (process == null)
         Thread.ofPlatform().start(this);
      waitFor();
      return (Do<V>) this;
   }
   @SuppressWarnings("null")
   public V get() {
      executePlatform();
      return (V) null;
   }
   // private V erg;
   // private V getErg() {
   // return erg;
   // }
}
