/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.gui2;

import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import javax.swing.JTabbedPane;
import java.awt.Color;

/**
 * @author Andreas Kielkopf
 *
 */
public class PanelMain extends JPanel {
   private static final long serialVersionUID=1L;
   private JTabbedPane tabbedPane;
   /**
    * Create the panel.
    */
   public PanelMain() {      initialize();
}
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getTabbedPane(), BorderLayout.CENTER);
      getTabbedPane().add("Muster",new PanelTab());
   }
   private JTabbedPane getTabbedPane() {
      if (tabbedPane == null) {
      	tabbedPane = new JTabbedPane(JTabbedPane.TOP);
      	tabbedPane.setBackground(Color.YELLOW);
      }
      return tabbedPane;
   }
}
