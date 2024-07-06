/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.gui2;

import javax.swing.JPanel;
import java.awt.Color;
import javax.swing.border.TitledBorder;

/**
 * @author Andreas Kielkopf
 *
 */
public class PanelCleanup extends JPanel {
   private static final long serialVersionUID=1L;
   /**
    * Create the panel.
    */
   public PanelCleanup() {      initialize();
}
   private void initialize() {
      setBorder(new TitledBorder(null, "Cleanup", TitledBorder.LEADING, TitledBorder.TOP, null, null));
      setBackground(Color.RED);
   }
}
