/**
 * @author Andreas Kielkopf
 *
 */
module backsnap {
   exports de.uhingen.kielkopf.andreas.backsnap.btrfs;
   exports de.uhingen.kielkopf.andreas.backsnap;
   exports de.uhingen.kielkopf.andreas.backsnap.gui;
   exports de.uhingen.kielkopf.andreas.backsnap.gui.element;
   exports de.uhingen.kielkopf.andreas.backsnap.gui.part;
   requires transitive Beans;
   requires transitive java.desktop;
   requires transitive java.prefs;
   requires org.eclipse.jdt.annotation;
}
