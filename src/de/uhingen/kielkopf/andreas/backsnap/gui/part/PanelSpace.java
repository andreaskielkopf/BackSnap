/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.part;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.gui.BacksnapGui;
import de.uhingen.kielkopf.andreas.backsnap.gui.element.Lbl;
import de.uhingen.kielkopf.andreas.backsnap.gui.element.TxtFeld;

import java.beans.Beans;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.BorderLayout;

/**
 * @author Andreas Kielkopf
 *
 */
public class PanelSpace extends JPanel {
   static private final long serialVersionUID=-8473404478127990644L;
   private JPanel            panel;
   private JPanel            panel_c;
   private Lbl               lblSpace;
   private TxtFeld           txtDisabled;
   private JSlider           sliderSpace;
   private JCheckBox         chckSpace;
   private JButton           btnSpace;
   static public int         DEFAULT_SPACE   =1999;
   final private BacksnapGui bsGui;
   /**
    * Create the panel.
    */
   @SuppressWarnings("unused")
   private PanelSpace() {
      this(null);
   }
   public PanelSpace(BacksnapGui b) {
      if (!Beans.isDesignTime())
         if (b == null)
            throw new NullPointerException("BacksnapGui ist null");
      bsGui=b;
      initialize();
   }
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getPanel());
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setBorder(new TitledBorder(null, "free some space", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getChckSpace(), BorderLayout.WEST);
         panel.add(getSliderSpace(), BorderLayout.SOUTH);
         panel.add(getBtnSpace(), BorderLayout.EAST);
         panel.add(getPanel_c(), BorderLayout.CENTER);
      }
      return panel;
   }
   private JPanel getPanel_c() {
      if (panel_c == null) {
         panel_c=new JPanel();
         panel_c.setLayout(new BorderLayout(0, 0));
         panel_c.add(getLblSpace(), BorderLayout.WEST);
         panel_c.add(getTxtDisabled(), BorderLayout.EAST);
      }
      return panel_c;
   }
   private Lbl getLblSpace() {
      if (lblSpace == null) {
         lblSpace=new Lbl("?");
      }
      return lblSpace;
   }
   private TxtFeld getTxtDisabled() {
      if (txtDisabled == null) {
         txtDisabled=new TxtFeld("deleting of backups is disabled while backups are running");
      }
      return txtDisabled;
   }
   public JButton getBtnSpace() {
      if (btnSpace == null) {
         btnSpace=new JButton("Delete some old snapshots");
         if (bsGui != null)
            btnSpace.addActionListener(e -> {
               try {
                  bsGui.delete(getBtnSpace(), SnapshotLabel.deleteOldColor);
               } catch (IOException e1) {
                  e1.printStackTrace();
               }
            });
         btnSpace.setEnabled(false);
         btnSpace.setBackground(SnapshotLabel.deleteOldColor);
      }
      return btnSpace;
   }
   public JSlider getSliderSpace() {
      if (sliderSpace == null) {
         sliderSpace=new JSlider();
         sliderSpace.setEnabled(Backsnap.DELETEOLD.get());
         sliderSpace.setMaximum(5000);
         sliderSpace.setMajorTickSpacing(1000);
         sliderSpace.setMinorTickSpacing(200);
         sliderSpace.setPaintTicks(true);
         sliderSpace.setPaintLabels(true);
         sliderSpace.setValue(DEFAULT_SPACE - 1);
         Dictionary<Integer, JLabel> labelTable=new Hashtable<>();
         for (int i=0; i <= 5; i++)
            labelTable.put(Integer.valueOf(i * 1000), new JLabel((i == 0) ? "0" : Integer.toString(i) + "T"));
         sliderSpace.setLabelTable(labelTable);
         if (bsGui != null)
            sliderSpace.addChangeListener(new ChangeListener() {
               @Override
               public void stateChanged(final ChangeEvent e) {
                  String text=Integer.toString(getSliderSpace().getValue());
                  getLblSpace().setText(text);
                  if (!getSliderSpace().getValueIsAdjusting()) {
                     Backsnap.DELETEOLD.setParameter(text);
                     try {
                        bsGui.abgleich();
                     } catch (IOException e1) {
                        e1.printStackTrace();
                     }
                  }
               }
            });
      }
      return sliderSpace;
   }
   public JCheckBox getChckSpace() {
      if (chckSpace == null) {
         chckSpace=new JCheckBox("-o, --deleteold");
         chckSpace.setHorizontalTextPosition(SwingConstants.LEADING);
         chckSpace.addActionListener(e -> flagSpace());
         chckSpace.setSelected(Backsnap.DELETEOLD.get());
      }
      return chckSpace;
   }
   public void flagSpace() {
      boolean s=getChckSpace().isSelected();
      Backsnap.logln(3, "--------------- getChckSpace() actionPerformed");
      Backsnap.DELETEOLD.set(s);
      getSliderSpace().setEnabled(s);
      getBtnSpace().setEnabled(s & !Backsnap.BTRFS_LOCK.isLocked());
      if (s && bsGui != null && !bsGui.getTglPause().isSelected())
         SwingUtilities.invokeLater(() -> bsGui.getTglPause().doClick());
   }
   public void updateButtons() {
      getBtnSpace().setEnabled(getChckSpace().isSelected() & !Backsnap.BTRFS_LOCK.isLocked());
      getTxtDisabled().setVisible(Backsnap.BTRFS_LOCK.isLocked());
      // revalidate();
      repaint(50);
   }
}
