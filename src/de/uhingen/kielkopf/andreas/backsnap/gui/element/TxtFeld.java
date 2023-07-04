/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.element;

import javax.swing.JTextField;
import javax.swing.SwingConstants;

import de.uhingen.kielkopf.andreas.backsnap.gui.part.SnapshotPanel;

/**
 * @author Andreas Kielkopf
 *
 */
public class TxtFeld extends JTextField {
   private static final long serialVersionUID=7311777720376081379L;
   public TxtFeld() {
      super();
      setFont(SnapshotPanel.FONT_INFO);
      setHorizontalAlignment(SwingConstants.CENTER);
      setEditable(false);
//      setColumns(10);
   }
   /**
    * @param string
    */
   public TxtFeld(String string) {
      this();
      setText(string);
   }
}
