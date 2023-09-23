/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.part;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import org.eclipse.jdt.annotation.NonNull;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.btrfs.Btrfs;
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
public class PanelMeta extends JPanel {
   static private ExecutorService     virtual         =Version.getVx();
   static private final long          serialVersionUID=-8829953253542936677L;
   private JCheckBox                  chckMeta;
   private JButton                    btnMeta;
   static public int                  DEFAULT_META    =499;
   private JSlider                    sliderMeta;
   private Lbl                        lblMeta;
   private JPanel                     panel;
   private JPanel                     panel_c;
   private TxtFeld                    xtxDisabled;
   AtomicBoolean                      needsAbgleich   =new AtomicBoolean(false);
   @NonNull private final BacksnapGui bsGui;
   /**
    * @param bsGui
    */
   public PanelMeta(@NonNull BacksnapGui b) {
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
         panel.setBorder(
                  new TitledBorder(null, "free some metadata", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getChckMeta(), BorderLayout.WEST);
         panel.add(getSliderMeta(), BorderLayout.SOUTH);
         panel.add(getBtnMeta(), BorderLayout.EAST);
         panel.add(getPanel_c(), BorderLayout.CENTER);
      }
      return panel;
   }
   public void flagMeta() {
      boolean s=getChckMeta().isSelected();
      Log.log("-------------- getChckMeta() actionPerformed", LEVEL.DEBUG);
      Backsnap.KEEP_MINIMUM.set(s);
      getSliderMeta().setEnabled(s);
      getBtnMeta().setEnabled(s & !Btrfs.LOCK.isLocked());
      if (s && !bsGui.getTglPause().isSelected())
         SwingUtilities.invokeLater(() -> bsGui.getTglPause().doClick());
      SwingUtilities.invokeLater(() -> updateButtons());
   }
   public JButton getBtnMeta() {
      if (btnMeta == null) {
         btnMeta=new JButton("Delete some unneeded snapshots");
         btnMeta.addActionListener(e -> bsGui.delete(getBtnMeta(), getChckMeta(), SnapshotLabel.STATUS.SPAM));
         btnMeta.setEnabled(false);
         btnMeta.setBackground(SnapshotLabel.STATUS.SPAM.color);
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
   public JSlider getSliderMeta() {
      if (sliderMeta == null) {
         sliderMeta=new JSlider();
         sliderMeta.setEnabled(false);
         sliderMeta.setMaximum(1000);
         sliderMeta.setMajorTickSpacing(100);
         sliderMeta.setMinorTickSpacing(20);
         sliderMeta.setPaintLabels(true);
         sliderMeta.setPaintTicks(true);
         sliderMeta.setValue(DEFAULT_META - 1);
         sliderMeta.addChangeListener(e -> {
            String text=Integer.toString(getSliderMeta().getValue());
            getLblMeta().setText(text);
            Backsnap.KEEP_MINIMUM.setParameter(text);
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
      return sliderMeta;
   }
   private Lbl getLblMeta() {
      if (lblMeta == null) {
         lblMeta=new Lbl("?");
      }
      return lblMeta;
   }
   public void updateButtons() {
      getBtnMeta().setEnabled(getChckMeta().isSelected() & !Btrfs.LOCK.isLocked());
      getLblDisabled_1().setVisible(Btrfs.LOCK.isLocked());
      repaint(50);
   }
   private JPanel getPanel_c() {
      if (panel_c == null) {
         panel_c=new JPanel();
         panel_c.setLayout(new BorderLayout(0, 0));
         panel_c.add(getLblMeta(), BorderLayout.WEST);
         panel_c.add(getLblDisabled_1(), BorderLayout.EAST);
      }
      return panel_c;
   }
   private TxtFeld getLblDisabled_1() {
      if (xtxDisabled == null) {
         xtxDisabled=new TxtFeld("deleting of backups is disabled while btrfs is busy ");
      }
      return xtxDisabled;
   }
}
