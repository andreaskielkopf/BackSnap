/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.shell;

/**
 * @author Andreas Kielkopf
 *
 */
public class Worker {
   final public static Worker stdInp    =new Worker() {
                                           @Override
                                           public void processLine(String line) {
                                              System.out.println(line);
                                           }
                                        };
   final public static Worker stdErr    =new Worker() {
                                           @Override
                                           public void processLine(String line) {
                                              System.err.println(line);
                                           }
                                        };
   final public static Worker nullWorker=new Worker();
   /**
    * @param line
    */
   public void processLine(String line) {/* do nothing */}
}
