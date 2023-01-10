/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.awt.*;

import javax.swing.*;

/**
 * @author Andreas Kielkopf
 *
 */
public class SnapshotDetail extends JPanel {
   private JPanel panel;
   private JLabel lblNewLabel;
   private JLabel lblNewLabel_1;
   private JPanel panel_1;
   public SnapshotDetail() {
      initialize();
   }
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getPanel_1());
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setLayout(new GridLayout(0, 2, 10, 0));
         panel.add(getLblNewLabel());
         panel.add(getLblNewLabel_1());
      }
      return panel;
   }
   private JLabel getLblNewLabel() {
      if (lblNewLabel == null) {
         lblNewLabel=new JLabel("name:");
         lblNewLabel.setFont(new Font("Noto Sans", Font.BOLD, 12));
         lblNewLabel.setHorizontalAlignment(SwingConstants.TRAILING);
      }
      return lblNewLabel;
   }
   private JLabel getLblNewLabel_1() {
      if (lblNewLabel_1 == null) {
         lblNewLabel_1=new JLabel("snapshot 5");
      }
      return lblNewLabel_1;
   }
   private JPanel getPanel_1() {
      if (panel_1 == null) {
         panel_1=new JPanel();
         panel_1.setLayout(new BorderLayout(0, 0));
         panel_1.add(getPanel());
      }
      return panel_1;
   }
}
