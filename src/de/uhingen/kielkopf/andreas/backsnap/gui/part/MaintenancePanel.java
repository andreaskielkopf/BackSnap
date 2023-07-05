/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.part;

import java.awt.BorderLayout;

import javax.swing.*;

import de.uhingen.kielkopf.andreas.backsnap.btrfs.Usage;
import de.uhingen.kielkopf.andreas.backsnap.gui.BacksnapGui;

import java.awt.Dimension;
import java.beans.Beans;

/**
 * @author Andreas Kielkopf
 *
 */
public class MaintenancePanel extends JPanel {
   private static final long serialVersionUID=-6113424454048132514L;
   private JPanel            panelWartung;
   private JTabbedPane       tabbedPane;
   private PanelSpace        panelSpace;
   private PanelScrub        panelScrub;
   private PanelBalance      panelBalance;
   private PanelInfo         panelInfo;
   private BacksnapGui       bsGui;
   private PanelMeta         panelMeta;
   /**
    * Create the panel.
    */
   @SuppressWarnings("unused")
   private MaintenancePanel() {
      this(null);
   }
   /**
    * @param backsnapGui
    */
   public MaintenancePanel(BacksnapGui b) {
      if (!Beans.isDesignTime())
         if (b == null)
            throw new NullPointerException("BacksnapGui ist null");
      bsGui=b;
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
         panelWartung.setLayout(new BorderLayout(0, 0));
         panelWartung.add(getTabbedPane(), BorderLayout.CENTER);
      }
      return panelWartung;
   }
   private JTabbedPane getTabbedPane() {
      if (tabbedPane == null) {
         tabbedPane=new JTabbedPane(SwingConstants.LEFT);
         tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
         tabbedPane.addTab("backup info", null, getPanelInfo(), "After the backups, take a break to do maintenance");
         tabbedPane.addTab("get space", null, getPanelSpace(),
                  "Remove some older backups to make more space for new backups");
         tabbedPane.addTab("clean up", null, getPanelMeta(), "Clean up some intermediate backups to clean up metadata");
         tabbedPane.setEnabledAt(2, true);
         tabbedPane.addTab("balance", null, getPanelBalance(), null);
         tabbedPane.addTab("scrub", null, getPanelScrub(), null);
         tabbedPane.setEnabledAt(3, false);
         tabbedPane.setEnabledAt(4, false);
      }
      return tabbedPane;
   }
   public PanelSpace getPanelSpace() {
      if (panelSpace == null) {
         panelSpace=new PanelSpace(bsGui);
      }
      return panelSpace;
   }
   public PanelMeta getPanelMeta() {
      if (panelMeta == null) {
         panelMeta=new PanelMeta(bsGui);
      }
      return panelMeta;
   }
   private PanelScrub getPanelScrub() {
      if (panelScrub == null) {
         panelScrub=new PanelScrub();
      }
      return panelScrub;
   }
   private PanelBalance getPanelBalance() {
      if (panelBalance == null) {
         panelBalance=new PanelBalance();
      }
      return panelBalance;
   }
   private PanelInfo getPanelInfo() {
      if (panelInfo == null) {
         panelInfo=new PanelInfo();
      }
      return panelInfo;
   }
   public void updateButtons() {
      getPanelSpace().updateButtons();
      getPanelMeta().updateButtons();
   }
   /**
    * @param usage
    */
   public void setUsage(Usage usage) {
      getPanelInfo().setUsage(usage);
   }
}
