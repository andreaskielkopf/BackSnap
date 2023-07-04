/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.element;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * @author Andreas Kielkopf
 *
 */
public class Lbl extends JLabel {
   private static final long serialVersionUID=3776667855115291806L;

   /**
    * @param string
    */
   public Lbl(String string) {
      super(string);
      setHorizontalAlignment(SwingConstants.TRAILING);
//      setVerticalAlignment(SwingConstants.BOTTOM);
   }
}
