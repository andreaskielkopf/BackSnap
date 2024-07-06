/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.gui2;

import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.border.TitledBorder;
import javax.swing.JProgressBar;

/**
 * @author Andreas Kielkopf
 *
 */
public class PanelBottom extends JPanel {
   private static final long serialVersionUID=1L;
   private JPanel panel;
   private JProgressBar progressBar;
   /**
    * Create the panel.
    */
   public PanelBottom() {      initialize();
}
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getPanel());
   }
   private JPanel getPanel() {
      if (panel == null) {
      	panel = new JPanel();
      	panel.setBorder(new TitledBorder(null, "Progress", TitledBorder.LEADING, TitledBorder.TOP, null, null));
      	panel.setBackground(Color.ORANGE);
      	panel.setLayout(new BorderLayout(0, 0));
      	panel.add(getProgressBar(), BorderLayout.SOUTH);
      }
      return panel;
   }
   private JProgressBar getProgressBar() {
      if (progressBar == null) {
      	progressBar = new JProgressBar();
      }
      return progressBar;
   }
}
