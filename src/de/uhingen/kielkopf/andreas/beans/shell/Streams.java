/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.shell;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Andreas Kielkopf
 *
 */
abstract public class Streams implements AutoCloseable {
   final static String        SHELL   =System.getenv("SHELL"); // get the used shell
   final static AtomicInteger streamNr=new AtomicInteger(0);
}
