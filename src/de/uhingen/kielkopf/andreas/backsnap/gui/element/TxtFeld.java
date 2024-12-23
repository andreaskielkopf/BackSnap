/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.element;

import javax.swing.JTextField;
import javax.swing.SwingConstants;

import de.uhingen.kielkopf.andreas.backsnap.gui.gui2.SnapshotPanel0;

/**
 * @author Andreas Kielkopf
 *
 */
public class TxtFeld extends JTextField {
   static private final long serialVersionUID=7311777720376081379L;
   public TxtFeld() {
      super();
      setFont(SnapshotPanel0.FONT_INFO);
      setHorizontalAlignment(SwingConstants.CENTER);
      setEditable(false);
      // setColumns(10);
   }
   /**
    * @param string
    */
   public TxtFeld(String string) {
      this();
      setText(string);
   }
}
