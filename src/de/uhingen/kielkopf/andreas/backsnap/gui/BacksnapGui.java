/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.*;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.btrfs.*;
import de.uhingen.kielkopf.andreas.backsnap.gui.part.SnapshotLabel;
import de.uhingen.kielkopf.andreas.backsnap.gui.part.SnapshotPanel;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

/**
 * @author Andreas Kielkopf
 *
 */
public class BacksnapGui implements MouseListener {
   private static BacksnapGui                          backSnapGui;
   public JFrame                                       frame;
   private JPanel                                      panel;
   private JLabel                                      lblNewLabel;
   private JPanel                                      panel_1;
   private SnapshotPanel                               panelSrc;
   private SnapshotPanel                               panelBackup;
   private JSplitPane                                  splitPane;
   private JButton                                     btnMeta;
   private JButton                                     btnSpace;
   private JPanel                                      panelSpace;
   private JPanel                                      panelMeta;
   private JCheckBox                                   chckSpace;
   private JSlider                                     sliderSpace;
   private JCheckBox                                   chckMeta;
   private JSlider                                     sliderMeta;
   public ConcurrentSkipListMap<String, SnapshotLabel> manualDelete=new ConcurrentSkipListMap<>();
   /**
    * @param args
    */
   public static void main2(String[] args) {
      if (backSnapGui == null)
         backSnapGui=new BacksnapGui(); // leere GUI erzeugen ;-) nur für Designer
      if (backSnapGui != null)
         EventQueue.invokeLater(() -> {
            try {
               backSnapGui.frame.setVisible(true);
            } catch (final Exception e2) {
               e2.printStackTrace();
            }
         });
   }
   /**
    * Create the application.
    * 
    * @wbp.parser.entryPoint
    */
   public BacksnapGui() {
      initialize();
   }
   /**
    * @param snapConfigs
    * @param srcVolume
    * @param srcSubVolumes
    * @param backupVolume
    * @param backupSubVolumes
    */
   public BacksnapGui(List<SnapConfig> snapConfigs, Mount srcVolume, SubVolumeList srcSubVolumes, Mount backupVolume,
            SubVolumeList backupSubVolumes) {
      this();
   }
   /**
    * 
    */
   private void initialize() {
      frame=new JFrame();
      frame.setBounds(100, 100, 800, 650);
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      frame.getContentPane().add(getPanel(), BorderLayout.NORTH);
      frame.getContentPane().add(getPanel_1(), BorderLayout.CENTER);
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
         panel.add(getLblNewLabel());
         panel.add(getPanelSpace());
         panel.add(getPanelMeta());
      }
      return panel;
   }
   private JLabel getLblNewLabel() {
      if (lblNewLabel == null) {
         lblNewLabel=new JLabel("BacksnapGui");
         lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return lblNewLabel;
   }
   /**
    * @param bs
    */
   public static void setGui(BacksnapGui bs) {
      backSnapGui=bs;
   }
   private JPanel getPanel_1() {
      if (panel_1 == null) {
         panel_1=new JPanel();
         panel_1.setLayout(new BorderLayout(0, 0));
         panel_1.add(getSplitPane(), BorderLayout.CENTER);
      }
      return panel_1;
   }
   private SnapshotPanel getPanelSrc() {
      if (panelSrc == null) {
         panelSrc=new SnapshotPanel();
      }
      return panelSrc;
   }
   /**
    * @param srcConfig.original()
    */
   public void setSrc(SnapConfig srcConfig) {
      getPanelSrc().setVolume(srcConfig.original(), srcConfig.original().otimeMap().values());
      abgleich();
      getPanelSrc().repaint();
   }
   /**
    * 
    */
   private void abgleich() {
      ConcurrentSkipListMap<String, SnapshotLabel>  snapshotLabels_Uuid=getPanelSrc().labelTree_UUID;
      ConcurrentSkipListMap<String, SnapshotLabel>  backupLabels_Uuid  =getPanelBackup().labelTree_UUID;
      ConcurrentSkipListMap<String, SnapshotLabel>  backupLabels_KeyO  =getPanelBackup().labelTree_KeyO;
      ConcurrentSkipListMap<String, SnapshotLabel>  deleteLabels       =new ConcurrentSkipListMap<>();
      ConcurrentSkipListMap<String, SnapshotLabel>  keineSackgasse     =new ConcurrentSkipListMap<>();
      ConcurrentNavigableMap<String, SnapshotLabel> toDeleteOld        =new ConcurrentSkipListMap<>();
      ArrayList<SnapshotLabel>                      deleteList         =new ArrayList<>();
      // SINGLESNAPSHOT make or delete only one(1) snapshot per call
      // DELETEOLD delete all snapshots that are "o=999" older than the newest one
      if (Backsnap.DELETEOLD.get()) {
         if (getPanelSrc().labelTree_KeyO.lastEntry() instanceof Entry<String, SnapshotLabel> lastEntry) {
            int           deleteOld=parseIntOrDefault(Backsnap.DELETEOLD.getParameter(), 2999);
            SnapshotLabel last     =lastEntry.getValue();
            int           firstNr  =parseIntOrDefault(last.snapshot.dirName(), deleteOld) - deleteOld;
            if (firstNr > 0)
               toDeleteOld=backupLabels_KeyO.headMap(Snapshot.dir2key(Integer.toString(firstNr)));
         }
      }
      recolor(backupLabels_KeyO, deleteLabels, toDeleteOld, deleteList);
      // suche Sackgassen
      if (!backupLabels_KeyO.isEmpty()) {
         SnapshotLabel child=backupLabels_KeyO.lastEntry().getValue();
         while (child != null) {
            Snapshot s=child.snapshot;
            keineSackgasse.put(s.key(), child);
            String        parent_uuid=s.parent_uuid();
            SnapshotLabel parent     =backupLabels_Uuid.get(parent_uuid);
            child=parent;
         }
      }
      // MINIMUMSNAPSHOTS
      if (Backsnap.MINIMUMSNAPSHOTS.get()) {
         ArrayList<SnapshotLabel> mixedList2;
         synchronized (getPanelBackup().mixedList) {
            mixedList2=new ArrayList<>(getPanelBackup().mixedList);
         }
         int minimum  =parseIntOrDefault(Backsnap.MINIMUMSNAPSHOTS.getParameter(), 499);
         int deletable=mixedList2.size() - minimum - snapshotLabels_Uuid.size() - toDeleteOld.size();
         for (SnapshotLabel snapshotLabel:mixedList2) { // zuerst manuell gelöschte anbieten
            if (deletable < 1)
               break;
            if (!manualDelete.containsValue(snapshotLabel))
               continue;
            if (toDeleteOld.containsValue(snapshotLabel))
               continue; // wird eh schon gelöscht
            if (getPanelSrc().labelTree_UUID.containsKey(snapshotLabel.snapshot.received_uuid()))
               continue;// aktuell
            deleteList.add(snapshotLabel);
            snapshotLabel.setBackground(SnapshotLabel.delete2Color);
            deletable--;
         }
         for (SnapshotLabel snapshotLabel:mixedList2) { // zuerst Sackgassen anbieten
            if (deletable < 1)
               break;
            if (deleteList.contains(snapshotLabel))
               continue;
            if (keineSackgasse.containsValue(snapshotLabel))
               continue;
            if (toDeleteOld.containsValue(snapshotLabel))
               continue; // wird eh schon gelöscht
            if (getPanelSrc().labelTree_UUID.containsKey(snapshotLabel.snapshot.received_uuid()))
               continue; // aktuell
            deleteList.add(snapshotLabel);
            snapshotLabel.setBackground(SnapshotLabel.delete2Color);
            deletable--;
         }
         for (SnapshotLabel snapshotLabel:mixedList2) { // reguläre Snapshots anbieten
            if (deletable < 1)
               break;
            if (deleteList.contains(snapshotLabel))
               continue;
            if (toDeleteOld.containsValue(snapshotLabel))
               continue; // wird eh schon gelöscht
            if (getPanelSrc().labelTree_UUID.containsKey(snapshotLabel.snapshot.received_uuid()))
               continue;
            deleteList.add(snapshotLabel);
            snapshotLabel.setBackground(SnapshotLabel.delete2Color);
            deletable--;
         }
      }
      // Show status of snapshots
      recolor(backupLabels_KeyO, deleteLabels, toDeleteOld, deleteList);
      System.out.println("Show Backups");
   }
   private void delete(final JButton jButton, Color deleteColor) {
      ConcurrentSkipListMap<String, SnapshotLabel> alle    =getPanelBackup().labelTree_KeyO;
      final ArrayList<Snapshot>                    toRemove=new ArrayList<>();
      for (Entry<String, SnapshotLabel> entry:alle.entrySet())
         if (entry.getValue() instanceof SnapshotLabel label)
            if (label.getBackground() == deleteColor) {
               System.out.println("to remove " + label.getText());
               toRemove.add(label.snapshot);
            }
      Commandline.background.submit(new Runnable() {
         @Override
         public void run() {
            jButton.setEnabled(false);
            for (Snapshot snapshot:toRemove) {
               try {
                  Backsnap.removeSnapshot(snapshot);
               } catch (IOException e1) { /* */ }
               try {
                  EventQueue.invokeAndWait(() -> {
                     try {
                        Backsnap.refreshGUI();
                        abgleich();
                        getPanelBackup().repaint(50);
                     } catch (final Exception e2) {
                        e2.printStackTrace();
                     }
                  });
               } catch (InvocationTargetException e) {
                  e.printStackTrace();
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
               if (Backsnap.SINGLESNAPSHOT.get())
                  break;
            }
            jButton.setEnabled(true);
         }
      });
   }
   private void recolor(ConcurrentSkipListMap<String, SnapshotLabel> backupLabels_Key,
            ConcurrentSkipListMap<String, SnapshotLabel> deleteLabels,
            ConcurrentNavigableMap<String, SnapshotLabel> toDelete, ArrayList<SnapshotLabel> deleteList) {
      for (Entry<String, SnapshotLabel> entry:backupLabels_Key.entrySet()) {
         String        key          =entry.getKey();
         SnapshotLabel snl          =entry.getValue();
         String        received_uuid=snl.snapshot.received_uuid();
         boolean       istAktuell   =getPanelSrc().labelTree_UUID.containsKey(received_uuid);
         boolean       loeschen     =toDelete.containsKey(key);
         if (loeschen & !istAktuell)
            deleteLabels.put(key, snl);
         if (istAktuell) {
            getPanelSrc().labelTree_UUID.get(received_uuid).setBackground(SnapshotLabel.backupColor);
            snl.setBackground(SnapshotLabel.backupColor);
         } else
            if (loeschen)
               snl.setBackground(SnapshotLabel.deleteColor);
            else
               if (deleteList.contains(snl))
                  snl.setBackground(SnapshotLabel.delete2Color);
               else
                  snl.setBackground(SnapshotLabel.keepColor);
         getPanelBackup().repaint();
      }
   }
   final static public int parseIntOrDefault(String s, int or) {
      if (s != null)
         try {
            return Integer.parseInt(s);
         } catch (NumberFormatException ignore) {
            System.err.println(ignore.getMessage() + ":" + s);
         }
      return or;
   }
   private SnapshotPanel getPanelBackup() {
      if (panelBackup == null) {
         panelBackup=new SnapshotPanel();
      }
      return panelBackup;
   }
   /**
    * @param backupVolume
    * @param receivedSnapshots
    * @param backupDir
    */
   public void setBackup(SnapTree backupTree, String backupDir) {
      ConcurrentSkipListMap<String, Snapshot> passendBackups=new ConcurrentSkipListMap<>();
      Path                                    rest          =Path.of("/", backupDir);
      for (Snapshot snapshot:backupTree.dateMap().values()) { // sortiert nach datum
         Path pfad=snapshot.getMountPath();
         if (pfad == null)
            continue;
         if (pfad.startsWith(rest))
            passendBackups.put(snapshot.key(), snapshot);
      }
      getPanelBackup().setVolume(backupTree.mount(), passendBackups.values());
      abgleich();
      getPanelBackup().repaint();
      for (SnapshotLabel label:getPanelBackup().getLabels().values())
         label.addMouseListener(this);
   }
   private JSplitPane getSplitPane() {
      if (splitPane == null) {
         splitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, getPanelSrc(), getPanelBackup());
      }
      return splitPane;
   }
   private JButton getBtnMeta() {
      if (btnMeta == null) {
         btnMeta=new JButton("Delete some unneeded snapshots");
         btnMeta.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
               delete(getBtnMeta(), SnapshotLabel.delete2Color);
            }
         });
         btnMeta.setEnabled(false);
         btnMeta.setBackground(SnapshotLabel.delete2Color);
      }
      return btnMeta;
   }
   private JButton getBtnSpace() {
      if (btnSpace == null) {
         btnSpace=new JButton("Delete some old snapshots");
         btnSpace.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
               delete(getBtnSpace(), SnapshotLabel.deleteColor);
            }
         });
         btnSpace.setEnabled(false);
         btnSpace.setBackground(SnapshotLabel.deleteColor);
      }
      return btnSpace;
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
      }
      return panelMeta;
   }
   private void flagSpace() {
      boolean s=getChckSpace().isSelected();
      System.out.println("--------------- getChckSpace() actionPerformed");
      Backsnap.DELETEOLD.set(s);
      getSliderSpace().setEnabled(s);
      getBtnSpace().setEnabled(s);
   }
   private JCheckBox getChckSpace() {
      if (chckSpace == null) {
         chckSpace=new JCheckBox("-o, --deleteold");
         chckSpace.setHorizontalTextPosition(SwingConstants.LEADING);
         chckSpace.setSelected(Backsnap.DELETEOLD.get());
         chckSpace.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
               flagSpace();
            }
         });
         flagSpace();
      }
      return chckSpace;
   }
   private void flagMeta() {
      boolean s=getChckSpace().isSelected();
      System.out.println("-------------- getChckMeta() actionPerformed");
      Backsnap.MINIMUMSNAPSHOTS.set(s);
      getSliderMeta().setEnabled(s);
      getBtnMeta().setEnabled(s);
   }
   private JCheckBox getChckMeta() {
      if (chckMeta == null) {
         chckMeta=new JCheckBox("-m, --keepminimum");
         chckMeta.setHorizontalTextPosition(SwingConstants.LEADING);
         chckMeta.setSelected(Backsnap.MINIMUMSNAPSHOTS.get());
         chckMeta.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
               flagMeta();
            }
         });
         flagMeta();
      }
      return chckMeta;
   }
   private JSlider getSliderSpace() {
      if (sliderSpace == null) {
         sliderSpace=new JSlider();
         sliderSpace.setEnabled(Backsnap.DELETEOLD.get());
         sliderSpace.setMaximum(10000);
         sliderSpace.setMajorTickSpacing(1000);
         sliderSpace.setMinorTickSpacing(200);
         sliderSpace.setPaintTicks(true);
         sliderSpace.setPaintLabels(true);
         sliderSpace.setValue(2999);
         Dictionary<Integer, JLabel> labelTable=new Hashtable<>();
         for (int i=0; i <= 10; i++)
            labelTable.put(Integer.valueOf(i * 1000), new JLabel((i == 0) ? "0" : Integer.toString(i) + "T"));
         sliderSpace.setLabelTable(labelTable);
         sliderSpace.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
               if (!getSliderSpace().getValueIsAdjusting()) {
                  int v=getSliderSpace().getValue();
                  System.out.println("getSliderSpace() changed " + v);
                  Backsnap.DELETEOLD.setParameter(Integer.toString(v));
                  abgleich();
               }
            }
         });
      }
      return sliderSpace;
   }
   private JSlider getSliderMeta() {
      if (sliderMeta == null) {
         sliderMeta=new JSlider();
         sliderMeta.setEnabled(false);
         sliderMeta.setMaximum(1000);
         sliderMeta.setMajorTickSpacing(100);
         sliderMeta.setMinorTickSpacing(20);
         sliderMeta.setValue(499);
         sliderMeta.setPaintLabels(true);
         sliderMeta.setPaintTicks(true);
         sliderMeta.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
               if (!getSliderMeta().getValueIsAdjusting()) {
                  int v=getSliderMeta().getValue();
                  System.out.println("getSliderMeta() changed " + v);
                  Backsnap.MINIMUMSNAPSHOTS.setParameter(Integer.toString(v));
                  abgleich();
               }
            }
         });
      }
      return sliderMeta;
   }
   @Override
   public void mouseClicked(MouseEvent e) {
      if (e.getSource() instanceof SnapshotLabel sl) {
         System.out.print("click-" + sl);
         if (!manualDelete.containsValue(sl))
            manualDelete.put(sl.getText(), sl);
         else
            manualDelete.remove(sl.getText());
         System.out.print(manualDelete.containsValue(sl) ? "del" : "keep");
         abgleich();
         sl.repaint(100);
      }
   }
   @Override
   public void mousePressed(MouseEvent e) {/* */}
   @Override
   public void mouseReleased(MouseEvent e) {/* */}
   @Override
   public void mouseEntered(MouseEvent e) {/* */}
   @Override
   public void mouseExited(MouseEvent e) {/* */}
}
