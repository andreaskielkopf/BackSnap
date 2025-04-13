/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.util.Arrays;

/**
 * @author Andreas Kielkopf
 *
 */
public class toErr {
   /**
    * @param args
    */
   public static void main(String[] args) {
      String LF=System.lineSeparator();
      System.err.println(Arrays.toString(args).replaceAll("\\[/n", LF+"["));
   }
}
