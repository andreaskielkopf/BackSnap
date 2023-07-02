/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.part;

import javax.swing.JPanel;

import de.uhingen.kielkopf.andreas.backsnap.btrfs.Usage;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import java.awt.*;

/**
 * @author Andreas Kielkopf
 *
 */
public class PanelInfo extends JPanel {
   private static final long serialVersionUID=-6513777293474216762L;
   private JPanel            panel;
   private JLabel            lblSize;
   private JPanel            panel_1;
   private JTextField        textSize;
   private JLabel            lblUnallocated;
   private JTextField        textUnallocated;
   private JLabel            lblFree;
   private JTextField        textFree;
   private JLabel            lblWarning;
   private JPanel            panel_2;
   /**
    * Create the panel.
    */
   public PanelInfo() {
      initialize();
   }
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getPanel_1());
   }
   /**
    * @param usage
    * 
    */
   public void setUsage(Usage usage) {
      getTextSize().setText(usage.size());
      getTextFree().setText(usage.free());
      getTextUnallocated().setText(usage.unallcoated());
      getLblWarning()
               .setText(usage.isFull() ? "There doesn't seem to be more than 10GiB unallocated. Please clean up first."
                        : usage.needsBalance() ? "It seems urgently advisable to balance the backup volume" : "");
      getLblWarning().setBackground(
               usage.isFull() ? Color.RED : usage.needsBalance() ? Color.ORANGE : getLblWarning().getBackground());
      revalidate();
      repaint(50);
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setLayout(new GridLayout(0, 6, 0, 0));
         panel.add(getLblSize());
         panel.add(getTextSize());
         panel.add(getLblUnallocated());
         panel.add(getTextUnallocated());
         panel.add(getLblFree());
         panel.add(getTextFree());
      }
      return panel;
   }
   private JLabel getLblSize() {
      if (lblSize == null) {
         lblSize=new JLabel("size:");
         lblSize.setHorizontalAlignment(SwingConstants.TRAILING);
      }
      return lblSize;
   }
   private JPanel getPanel_1() {
      if (panel_1 == null) {
         panel_1=new JPanel();
         panel_1.setLayout(new BorderLayout(0, 0));
         panel_1.add(getPanel(), BorderLayout.NORTH);
         panel_1.add(getPanel_2(), BorderLayout.CENTER);
      }
      return panel_1;
   }
   private JTextField getTextSize() {
      if (textSize == null) {
         textSize=new JTextField();
         textSize.setFont(SnapshotPanel.FONT_INFO);
         textSize.setHorizontalAlignment(SwingConstants.CENTER);
         textSize.setEditable(false);
         textSize.setColumns(10);
      }
      return textSize;
   }
   private JLabel getLblUnallocated() {
      if (lblUnallocated == null) {
         lblUnallocated=new JLabel("unallocated:");
         lblUnallocated.setHorizontalAlignment(SwingConstants.TRAILING);
      }
      return lblUnallocated;
   }
   private JTextField getTextUnallocated() {
      if (textUnallocated == null) {
         textUnallocated=new JTextField();
         textUnallocated.setFont(SnapshotPanel.FONT_INFO);
         textUnallocated.setHorizontalAlignment(SwingConstants.CENTER);
         textUnallocated.setEditable(false);
         textUnallocated.setColumns(10);
      }
      return textUnallocated;
   }
   private JLabel getLblFree() {
      if (lblFree == null) {
         lblFree=new JLabel("free:");
         lblFree.setHorizontalAlignment(SwingConstants.TRAILING);
      }
      return lblFree;
   }
   private JTextField getTextFree() {
      if (textFree == null) {
         textFree=new JTextField();
         textFree.setFont(SnapshotPanel.FONT_INFO);
         textFree.setHorizontalAlignment(SwingConstants.CENTER);
         textFree.setEditable(false);
         textFree.setColumns(10);
      }
      return textFree;
   }
   private JLabel getLblWarning() {
      if (lblWarning == null) {
         lblWarning=new JLabel("-");
         lblWarning.setOpaque(true);
         lblWarning.setFont(SnapshotPanel.FONT_INFO_B);
         lblWarning.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return lblWarning;
   }
   private JPanel getPanel_2() {
      if (panel_2 == null) {
         panel_2=new JPanel();
         panel_2.setLayout(new BorderLayout(0, 0));
         panel_2.add(getLblWarning(), BorderLayout.NORTH);
      }
      return panel_2;
   }
}
