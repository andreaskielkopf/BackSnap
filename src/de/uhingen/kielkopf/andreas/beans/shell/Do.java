/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * Ausführen beliebiger Commands aus java heraus durch Benutzung der SHell ($SHELL)
 * 
 * @author Andreas Kielkopf
 *
 */
public class Do implements Runnable {
   /** Welche Shell soll verwendet werden */
   public static String SHELL;      // ="/bin/sh"; // ermitteln mit which $(cat /proc/$$/comm) ???
   public static String username="";
   static {
      shell();
      root();
   }
   /** Finde herraus welche Shell benutzt werden soll */
   static void shell() {
      if (SHELL == null) {
         SHELL="/bin/sh";
         SHELL=doGetFirstOr(List.of("-c", "echo ${SHELL}"), "/bin/sh");
         System.out.print("$SHELL=" + SHELL);
      }
   }
   /** bin ich root ? */
   static boolean root() {
      if (username.isBlank()) {
         username=doGetFirstOr(List.of("whoami"), "");
         System.out.println(", user=" + username);
      }
      return username.equals("root"); // System.out.println(SHELL);
   }
   /**
    * Liefert die erste Zeile die dieses Command ausspuckt, oder den Ersatz dafür
    * 
    * @param l
    *           Command
    * @param or
    *           Ersatz
    * @return
    */
   private static String doGetFirstOr(List<String> l, String or) {
      List<String> tmp=doGetList(l);
      return (tmp.isEmpty()) ? or : tmp.getFirst();
   }
   /**
    * Lifert den kompletten Output dieses Commands und gibt den err-stream an die Console weiter
    * 
    * @param l
    * @return
    */
   private static void doCmd(List<String> l) {
      new Do(l, Worker.stdInp, Worker.stdErr).executePlatform();
   }
   private static List<String> doGetList(List<String> l) {
      ArrayList<String> tmp=new ArrayList<>();
      return new Do(l, new Worker() {
         @Override
         public void processLine(String line) {
            tmp.add(line);
         }
      }) {
         @Override
         public List<String> get() {
            executePlatform();
            return tmp;
         }
      }.get();
   }
   public static void main(String[] args) {
      System.out.println(doGetFirstOr(List.of("whoami", "5"), "Fehlertest ;-)"));
      doGetFirstOr(List.of("whoami", "5"), "Nur den Fehlertext ausgeben");
      doCmd(List.of("ls", "-lA")); // alles über stdout ausgeben
      doCmd(List.of("ls", "-lA s")); // alles über sterr ausgeben
   }
   // public Do(String... s) { this(Arrays.asList(s)); }
   public Do(String... list1) {
      this(Worker.collectInp(), Worker.stdErr, list1);
   }
   public Do(Worker oWorker, String... list1) {
      this(oWorker, Worker.stdErr, list1);
   }
   public Do(List<String> list1) {
      this(list1, Worker.collectInp(), Worker.stdErr);
   }
   public Do(List<String> list1, Worker oWorker) {
      this(list1, oWorker, Worker.stdErr);
   }
   Worker               inpWorker;
   Worker               errWorker;
   final ProcessBuilder builder;
   Process              process;
   boolean              done=false;
   ArrayList<String>    list;
   public Do(Worker iWorker, Worker eWorker, String... list1) {
      this(new ArrayList<String>(Arrays.asList(list1)), iWorker, eWorker);
   }
   /**
    * @param oWorker
    * @param eWorker
    * @param list
    */
   public Do(List<String> l, Worker iWorker, Worker eWorker) {
      if (l.isEmpty())
         throw new NullPointerException("Es wurde kein Befehl übergeben");
      inpWorker=iWorker; // (iWorker != null) ? iWorker : Worker.collectInp();
      errWorker=eWorker;// (eWorker != null) ? eWorker : Worker.stdErr;
      list=new ArrayList<>(l);
      if (list.getFirst().equals("-c"))
         list.addFirst(SHELL);
      builder=new ProcessBuilder(list);
   }
   @Override
   public void run() {
      try {
         if (done)
            return;
         if (process == null)
            process=builder.start();
         try (BufferedReader err=process.errorReader(); BufferedReader inp=process.inputReader();) {
            while (process.isAlive())
               readAll(err, inp);
            readAll(err, inp);
            int x=process.exitValue();
            if (x != 0) {
               System.err.print(list);
               System.err.println(" exit with " + x);
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         done=true;
      }
   }
   void readAll(BufferedReader err, BufferedReader inp) throws IOException {
      try {
         Thread.sleep(1L);
         if (inpWorker instanceof Worker) // bei null ignorieren
            while (inp.ready())
               inpWorker.processLine(inp.readLine());
         if (errWorker instanceof Worker) // bei null ignorieren
            while (err.ready())
               errWorker.processLine(err.readLine());
      } catch (InterruptedException ignoree) { /* */ }
   }
   /** Warte bis der Prozess zu ende ist, aber verschwende keine Leistung dabei */
   private void waitFor() {
      try {
         while (done == false) {
            Thread.onSpinWait();
            Thread.sleep(1L);
         }
      } catch (InterruptedException ignore) {/* */ }
   }
   Worker getInpWorker() {
      return inpWorker;
   }
   Worker getErrWorker() {
      return errWorker;
   }
   public Do executePlatform() {
      if (process == null)
         Thread.ofPlatform().start(this);
      waitFor();
      return (Do) this;
   }
   static public Do executePlatform(List<String> l) {
      return new Do(l).executePlatform();
   }
   public List<String> get() {
      executePlatform();
      if (inpWorker instanceof Worker w)
         return w.get();
      return null;
   }
   static public Do toPipe(String... list) {
      return toPipe(new ArrayList<String>(Arrays.asList(list)));
   }
   static public Do toPipe(List<String> l) {
      return new Do(l, null, null) {
         @Override
         public void run() {/* wird automatisch an die Pipe angehängt */}
      };
   }
   static public Do toWorker(String... list) {
      return toWorker(new ArrayList<String>(Arrays.asList(list)));
   }
   static public Do toWorker(List<String> l) {
      return new Do(l, Worker.collectInp(), Worker.collectErr());
   }
   static public Do toConsole(List<String> l) {
      return new Do(l, Worker.stdInp, Worker.stdErr);
   }
   public Do clean() {
      done=false;
      process=null;
      if (inpWorker instanceof Worker w)
         w.clean();
      if (inpWorker instanceof Worker w)
         w.clean();
      return this;
   }
   @Override
   public String toString() {
      return new StringBuilder(" Do").append(list).append(" ").append(inpWorker).append(" ").append(errWorker)
               .append(" ").toString();
   }
}
