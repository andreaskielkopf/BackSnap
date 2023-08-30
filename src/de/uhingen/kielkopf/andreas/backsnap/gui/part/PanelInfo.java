package de.uhingen.kielkopf.andreas.backsnap.gui.part;

import java.awt.*;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import de.uhingen.kielkopf.andreas.backsnap.btrfs.Usage;
import de.uhingen.kielkopf.andreas.backsnap.gui.element.Lbl;
import de.uhingen.kielkopf.andreas.backsnap.gui.element.TxtFeld;

/**
 * @author Andreas Kielkopf
 */
public class PanelInfo extends JPanel {
   static private final long serialVersionUID=-6513777293474216762L;
   private JPanel            panel;
   private Lbl               lblSize;
   private JPanel            panel_1;
   private TxtFeld           textSize;
   private Lbl               lblUnallocated;
   private TxtFeld           textUnallocated;
   private Lbl               lblFree;
   private TxtFeld           textFree;
   private TxtFeld           textWarning;
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
    *           Setzt die durch usage gewonnenen Infos in die passendne Felder
    */
   public void setUsage(Usage usage) {
      SwingUtilities.invokeLater(() -> {
         getTextSize().setText(usage.size());
         getTextFree().setText(usage.free());
         getTextUnallocated().setText(usage.unallcoated());
         getLblWarning().setText(
                  usage.isFull() ? "There doesn't seem to be more than 10GiB unallocated. Please clean up first."
                           : usage.needsBalance() ? "It seems urgently advisable to balance the backup volume" : "");
         getLblWarning().setBackground(
                  usage.isFull() ? Color.RED : usage.needsBalance() ? Color.ORANGE : getLblWarning().getBackground());
         revalidate();
         repaint(50);
      });
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
   private Lbl getLblSize() {
      if (lblSize == null) {
         lblSize=new Lbl("size:");
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
   private TxtFeld getTextSize() {
      if (textSize == null) {
         textSize=new TxtFeld();
      }
      return textSize;
   }
   private Lbl getLblUnallocated() {
      if (lblUnallocated == null) {
         lblUnallocated=new Lbl("unallocated:");
      }
      return lblUnallocated;
   }
   private TxtFeld getTextUnallocated() {
      if (textUnallocated == null) {
         textUnallocated=new TxtFeld();
      }
      return textUnallocated;
   }
   private Lbl getLblFree() {
      if (lblFree == null) {
         lblFree=new Lbl("free:");
      }
      return lblFree;
   }
   private TxtFeld getTextFree() {
      if (textFree == null) {
         textFree=new TxtFeld();
      }
      return textFree;
   }
   private TxtFeld getLblWarning() {
      if (textWarning == null) {
         textWarning=new TxtFeld(" ");
         textWarning.setOpaque(true);
         textWarning.setFont(SnapshotPanel.FONT_INFO_B);
      }
      return textWarning;
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
