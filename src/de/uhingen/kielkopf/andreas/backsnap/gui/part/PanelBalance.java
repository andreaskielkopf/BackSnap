/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.part;

import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.GridLayout;

/**
 * @author Andreas Kielkopf
 *
 */
public class PanelBalance extends JPanel {
   private static final long serialVersionUID=-1691962412108609293L;
   private JPanel            panel;
   private JPanel            panel_1;
   private JPanel            panel_2;
//   private JPanel            panel_3;
   private JPanel            panel_4;
   /**
    * Create the panel.
    */
   public PanelBalance() {
      initialize();
   }
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getPanel());
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getPanel_1(), BorderLayout.CENTER);
      }
      return panel;
   }
   private JPanel getPanel_1() {
      if (panel_1 == null) {
         panel_1=new JPanel();
         panel_1.setLayout(new BorderLayout(0, 0));
         panel_1.add(getPanel_2(), BorderLayout.SOUTH);
         panel_1.add(getPanel_4());
         // panel_1.add(getPanel_3());
      }
      return panel_1;
   }
   private JPanel getPanel_2() {
      if (panel_2 == null) {
         panel_2=new BalanceSelect();
         panel_2.setEnabled(false);
         // panel_2.setBorder(new TitledBorder(null, "Balance Data", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         // panel_2.setLayout(new BorderLayout(0, 0));
      }
      return panel_2;
   }
   private JPanel getPanel_4() {
      if (panel_4 == null) {
         panel_4=new JPanel();
         panel_4.setLayout(new GridLayout(1, 0, 0, 0));
      }
      return panel_4;
   }
}
