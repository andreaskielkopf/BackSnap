/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.part;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import org.eclipse.jdt.annotation.NonNull;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.config.Log;
import de.uhingen.kielkopf.andreas.backsnap.config.Log.LEVEL;
import de.uhingen.kielkopf.andreas.backsnap.gui.BacksnapGui;
import de.uhingen.kielkopf.andreas.backsnap.gui.element.Lbl;
import de.uhingen.kielkopf.andreas.backsnap.gui.element.TxtFeld;
import de.uhingen.kielkopf.andreas.beans.Version;

/**
 * @author Andreas Kielkopf
 *
 */
public class PanelSpace extends JPanel {
   static private ExecutorService     virtual         =Version.getVx();
   static private final long          serialVersionUID=-8473404478127990644L;
   private JPanel            panel;
   private JPanel            panel_c;
   private Lbl               lblSpace;
   private TxtFeld           txtDisabled;
   private JSlider           sliderSpace;
   private JCheckBox         chckSpace;
   private JButton           btnSpace;
   static public int                  DEFAULT_SPACE   =1999;
   AtomicBoolean                      needsAbgleich   =new AtomicBoolean(false);
   @NonNull final private BacksnapGui bsGui;
   public PanelSpace(@NonNull BacksnapGui b) {
      // if (!Beans.isDesignTime())
      // if (b == null)
      // throw new NullPointerException("BacksnapGui ist null");
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
         txtDisabled=new TxtFeld("deleting of backups is disabled while btrfs is busy ");
      }
      return txtDisabled;
   }
   public JButton getBtnSpace() {
      if (btnSpace == null) {
         btnSpace=new JButton("Delete some old snapshots");
         btnSpace.addActionListener(e -> bsGui.delete(getBtnSpace(), getChckSpace(), SnapshotLabel.STATUS.ALT));
         btnSpace.setEnabled(false);
         btnSpace.setBackground(SnapshotLabel.STATUS.ALT.color);
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
         sliderSpace.addChangeListener(e -> {
                  String text=Integer.toString(getSliderSpace().getValue());
                  getLblSpace().setText(text);
                     Backsnap.DELETEOLD.setParameter(text);
            if (needsAbgleich.compareAndSet(false, true))
               virtual.execute(() -> {
                     try {
                     Thread.sleep(50);
                     if (needsAbgleich.compareAndSet(true, false))
                        bsGui.abgleich();
                  } catch (IOException | InterruptedException ignore) {
                     ignore.printStackTrace();
                     }
            });
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
      Log.logln("--------------- getChckSpace() actionPerformed", LEVEL.DEBUG);
      Backsnap.DELETEOLD.set(s);
      getSliderSpace().setEnabled(s);
      getBtnSpace().setEnabled(PanelMeta.testLock(s));
      if (s && !bsGui.getTglPause().isSelected())
         SwingUtilities.invokeLater(() -> bsGui.getTglPause().doClick());
      SwingUtilities.invokeLater(() -> updateButtons());
   }
   public void updateButtons() {
      getBtnSpace().setEnabled(PanelMeta.testLock(getChckSpace().isSelected()));
      getTxtDisabled().setVisible(PanelMeta.testLock(true));
      // revalidate();
      repaint(50);
   }
}
