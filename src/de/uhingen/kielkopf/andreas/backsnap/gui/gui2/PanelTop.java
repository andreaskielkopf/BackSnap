/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.gui2;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.border.TitledBorder;
import javax.swing.JLabel;
import java.awt.Color;

/**
 * @author Andreas Kielkopf
 *
 */
public class PanelTop extends JPanel {
   private static final long serialVersionUID=1L;
   private JPanel panel_1;
   private JLabel lblNewLabel;
   private JPanel panel_2;
   private JLabel lblNewLabel_1;
   private JPanel panel_3;
   private JLabel lblNewLabel_2;
   /**
    * Create the panel.
    */
   public PanelTop() {
      initialize();
   }
   private void initialize() {
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
      add(getPanel_1());
      add(getPanel_2());
      add(getPanel_3());
   }
   private JPanel getPanel_1() {
      if (panel_1 == null) {
      	panel_1 = new JPanel();
      	panel_1.setBorder(new TitledBorder(null, "Args", TitledBorder.LEADING, TitledBorder.TOP, null, null));
      	panel_1.setLayout(new BorderLayout(0, 0));
      	panel_1.add(getLblNewLabel(), BorderLayout.NORTH);
      }
      return panel_1;
   }
   private JLabel getLblNewLabel() {
      if (lblNewLabel == null) {
      	lblNewLabel = new JLabel("-gt -v=1 -a=12");
      }
      return lblNewLabel;
   }
   private JPanel getPanel_2() {
      if (panel_2 == null) {
      	panel_2 = new JPanel();
      	panel_2.setBorder(new TitledBorder(null, "OneBackup", TitledBorder.LEADING, TitledBorder.TOP, null, null));
      	panel_2.setLayout(new BorderLayout(0, 0));
      	panel_2.add(getLblNewLabel_1(), BorderLayout.NORTH);
      }
      return panel_2;
   }
   private JLabel getLblNewLabel_1() {
      if (lblNewLabel_1 == null) {
      	lblNewLabel_1 = new JLabel("New label");
      }
      return lblNewLabel_1;
   }
   private JPanel getPanel_3() {
      if (panel_3 == null) {
      	panel_3 = new JPanel();
      	panel_3.setBorder(new TitledBorder(null, "Args2", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(59, 59, 59)));
      	panel_3.setLayout(new BorderLayout(0, 0));
      	panel_3.add(getLblNewLabel_2(), BorderLayout.NORTH);
      }
      return panel_3;
   }
   private JLabel getLblNewLabel_2() {
      if (lblNewLabel_2 == null) {
      	lblNewLabel_2 = new JLabel("New label");
      }
      return lblNewLabel_2;
   }
}
