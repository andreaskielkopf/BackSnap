/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.part;

import java.util.Dictionary;
import java.util.Hashtable;
import java.awt.BorderLayout;

import javax.swing.*;

import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.gui.BacksnapGui;
import java.awt.Dimension;

/**
 * @author Andreas Kielkopf
 *
 */
public class MaintenancePanel extends JPanel {
   private static final long serialVersionUID=-6113424454048132514L;
   private JPanel            panelWartung;
   private JTabbedPane       tabbedPane;
   private JPanel            panelSpace;
   private JPanel            panelScrub;
   private JPanel            panelBalance;
   // private JToggleButton tglPause;
   private JPanel            panelInfo;
   private JSlider           sliderSpace;
   private JCheckBox         chckSpace;
   private JCheckBox         chckMeta;
   private JButton           btnSpace;
   private JLabel            lblSpace;
   private JButton           btnMeta;
   public static int         DEFAULT_META    =249;
   public static int         DEFAULT_SPACE   =999;
   private JSlider           sliderMeta;
   private JLabel            lblMeta;
   private BacksnapGui       bsGui;
   private JPanel            panelMeta;
   /**
    * Create the panel.
    */
   public MaintenancePanel() {
      this(null);
   }
   /**
    * @param backsnapGui
    */
   public MaintenancePanel(BacksnapGui backsnapGui) {
      bsGui=backsnapGui;
      initialize();
   }
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getPanelWartung(), BorderLayout.CENTER);
   }
   private JPanel getPanelWartung() {
      if (panelWartung == null) {
         panelWartung=new JPanel();
         panelWartung.setPreferredSize(new Dimension(500, 125));
         // panelWartung.setMinimumSize(new Dimension(10, 250));
         panelWartung.setLayout(new BorderLayout(0, 0));
         panelWartung.add(getTabbedPane(), BorderLayout.CENTER);
      }
      return panelWartung;
   }
   private JTabbedPane getTabbedPane() {
      if (tabbedPane == null) {
         tabbedPane=new JTabbedPane(JTabbedPane.LEFT);
         tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
         tabbedPane.addTab("get space", null, getPanelSpace(),
                  "Remove some older backups to make more space for new backups");
         tabbedPane.addTab("clean up", null, getPanelMeta(), "Clean up some intermediate backups to clean up metadata");
         tabbedPane.addTab("backup info", null, getPanelInfo(), "After the backups, take a break to do maintenance");
         tabbedPane.addTab("balance", null, getPanelBalance(), null);
         tabbedPane.addTab("scrub", null, getPanelScrub(), null);
         tabbedPane.setEnabledAt(2, false);
         tabbedPane.setEnabledAt(3, false);
         tabbedPane.setEnabledAt(4, false);
      }
      return tabbedPane;
   }
   private JPanel getPanelSpace() {
      if (panelSpace == null) {
         panelSpace=new JPanel();
         panelSpace.setBorder(
                  new TitledBorder(null, "free some space", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panelSpace.setLayout(new BorderLayout(0, 0));
         panelSpace.add(getChckSpace(), BorderLayout.WEST);
         panelSpace.add(getSliderSpace(), BorderLayout.SOUTH);
         panelSpace.add(getBtnSpace(), BorderLayout.EAST);
         panelSpace.add(getLblSpace(), BorderLayout.CENTER);
      }
      return panelSpace;
   }
   private JPanel getPanelMeta() {
      if (panelMeta == null) {
         panelMeta=new JPanel();
         panelMeta.setBorder(
                  new TitledBorder(null, "free some metadata", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panelMeta.setLayout(new BorderLayout(0, 0));
         panelMeta.add(getChckMeta(), BorderLayout.WEST);
         panelMeta.add(getSliderMeta(), BorderLayout.SOUTH);
         panelMeta.add(getBtnMeta(), BorderLayout.EAST);
         panelMeta.add(getLblMeta(), BorderLayout.CENTER);
      }
      return panelMeta;
   }
   public JButton getBtnSpace() {
      if (btnSpace == null) {
         btnSpace=new JButton("Delete some old snapshots");
         if (bsGui != null)
            btnSpace.addActionListener(e -> bsGui.delete(getBtnSpace(), SnapshotLabel.deleteOldColor));
         btnSpace.setEnabled(false);
         btnSpace.setBackground(SnapshotLabel.deleteOldColor);
      }
      return btnSpace;
   }
   private JLabel getLblSpace() {
      if (lblSpace == null) {
         lblSpace=new JLabel("?");
         lblSpace.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return lblSpace;
   }
   public void flagSpace() {
      boolean s=getChckSpace().isSelected();
      Backsnap.logln(3, "--------------- getChckSpace() actionPerformed");
      Backsnap.DELETEOLD.set(s);
      getSliderSpace().setEnabled(s);
      getBtnSpace().setEnabled(s);
      if (s && bsGui != null && !bsGui.getTglPause().isSelected())
         SwingUtilities.invokeLater(() -> bsGui.getTglPause().doClick());
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
   public void flagMeta() {
      boolean s=getChckMeta().isSelected();
      Backsnap.log(3, "-------------- getChckMeta() actionPerformed");
      Backsnap.KEEP_MINIMUM.set(s);
      getSliderMeta().setEnabled(s);
      getBtnMeta().setEnabled(s);
      if (s && bsGui != null && !bsGui.getTglPause().isSelected())
         SwingUtilities.invokeLater(() -> bsGui.getTglPause().doClick());
   }
   public JButton getBtnMeta() {
      if (btnMeta == null) {
         btnMeta=new JButton("Delete some unneeded snapshots");
         if (bsGui != null)
            btnMeta.addActionListener(e -> bsGui.delete(getBtnMeta(), SnapshotLabel.delete2Color));
         btnMeta.setEnabled(false);
         btnMeta.setBackground(SnapshotLabel.delete2Color);
      }
      return btnMeta;
   }
   public JCheckBox getChckMeta() {
      if (chckMeta == null) {
         chckMeta=new JCheckBox("-m, --keepminimum");
         chckMeta.setHorizontalTextPosition(SwingConstants.LEADING);
         chckMeta.addActionListener(e -> flagMeta());
         chckMeta.setSelected(Backsnap.KEEP_MINIMUM.get());
      }
      return chckMeta;
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
                     bsGui.abgleich();
                  }
               }
            });
      }
      return sliderSpace;
   }
   public JSlider getSliderMeta() {
      if (sliderMeta == null) {
         sliderMeta=new JSlider();
         sliderMeta.setEnabled(false);
         sliderMeta.setMaximum(500);
         sliderMeta.setMajorTickSpacing(100);
         sliderMeta.setMinorTickSpacing(20);
         sliderMeta.setPaintLabels(true);
         sliderMeta.setPaintTicks(true);
         sliderMeta.setValue(DEFAULT_META - 1);
         if (bsGui != null)
            sliderMeta.addChangeListener(new ChangeListener() {
               @Override
               public void stateChanged(final ChangeEvent e) {
                  String text=Integer.toString(getSliderMeta().getValue());
                  getLblMeta().setText(text);
                  if (!getSliderMeta().getValueIsAdjusting()) {
                     Backsnap.KEEP_MINIMUM.setParameter(text);
                     bsGui.abgleich();
                  }
               }
            });
      }
      return sliderMeta;
   }
   private JLabel getLblMeta() {
      if (lblMeta == null) {
         lblMeta=new JLabel("?");
         lblMeta.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return lblMeta;
   }
   private JPanel getPanelScrub() {
      if (panelScrub == null) {
         panelScrub=new JPanel();
      }
      return panelScrub;
   }
   private JPanel getPanelBalance() {
      if (panelBalance == null) {
         panelBalance=new JPanel();
      }
      return panelBalance;
   }
   private JPanel getPanelInfo() {
      if (panelInfo == null) {
         panelInfo=new JPanel();
      }
      return panelInfo;
   }
}
