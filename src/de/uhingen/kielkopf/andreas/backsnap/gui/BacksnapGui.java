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
import javax.swing.border.EmptyBorder;

/**
 * @author Andreas Kielkopf
 * 
 *         Eine GUI um BackSnap zu beobachten und Zusatzfunktionen auszulösen
 *
 */
public class BacksnapGui implements MouseListener {
   private static BacksnapGui                          backSnapGui;
   public JFrame                                       frame;
   private JPanel                                      panel;
   private JLabel                                      lblNewLabel;
   private JPanel                                      panelMain;
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
   private JPanel                                      panelInfo;
   private JPanel                                      panelProgress;
   private JPanel                                      panelPv;
   private JProgressBar                                progressBar;
   private JLabel                                      lblPv;
   private JPanel                                      panelSpeed;
   private JProgressBar                                speedBar;
   private JLabel                                      SnapshotName;
   private JToggleButton                               tglPause;
   public final static String                          BLUE        ="<font size=+1 color=\"3333ff\">";
   public final static String                          NORMAL      ="</font>";
   public final static String                          IGEL1       ="<=>";
   public final static String                          IGEL2       =BLUE + IGEL1 + NORMAL;
   private JLabel                                      lblSpace;
   private JLabel                                      lblMeta;
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
            } catch (final Exception e) {
               e.printStackTrace();
            }
         });
   }
   /**
    * Create the application.
    * 
    * @wbp.parser.entryPoint
    */
   public BacksnapGui() {
      UIManager.put("ProgressBar.selectionForeground", Color.black);
      UIManager.put("ProgressBar.selectionBackground", Color.black);
      initialize();
      if (Backsnap.DELETEOLD.get())
         getSliderSpace().setValue(parseIntOrDefault(Backsnap.DELETEOLD.getParameter(), 999));
      if (Backsnap.MINIMUMSNAPSHOTS.get())
         getSliderMeta().setValue(parseIntOrDefault(Backsnap.MINIMUMSNAPSHOTS.getParameter(), 249));
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
      frame.setBounds(100, 100, 1280, 960);
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      frame.getContentPane().add(getPanel(), BorderLayout.NORTH);
      frame.getContentPane().add(getPanelMain(), BorderLayout.CENTER);
      frame.getContentPane().add(getPanelInfo(), BorderLayout.SOUTH);
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
         lblNewLabel=new JLabel(Backsnap.BACK_SNAP_VERSION);
         lblNewLabel.setFont(new Font("Noto Sans", Font.PLAIN, 14));
         lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return lblNewLabel;
   }
   /**
    * @param bsGui
    */
   public static void setGui(BacksnapGui bsGui) {
      backSnapGui=bsGui;
   }
   private JPanel getPanelMain() {
      if (panelMain == null) {
         panelMain=new JPanel();
         panelMain.setLayout(new BorderLayout(0, 0));
         panelMain.add(getSplitPane(), BorderLayout.CENTER);
      }
      return panelMain;
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
      int                                     linefeeds=0;
      StringBuilder                           sb       =new StringBuilder("Src:");
      ConcurrentSkipListMap<String, Snapshot> neuList  =getPanelSrc().setVolume(srcConfig.original(),
               srcConfig.original().otimeKeyMap().values());
      for (Snapshot snap:neuList.values()) {
         sb.append(" ").append(snap.dirName());
         if ((sb.length() / 120) > linefeeds) {
            sb.append("\n");
            linefeeds++;
         }
      }
      System.out.println(sb.toString());
      abgleich();
      sb.setLength(0);
      sb.append("<html>Snapshots of ").append(BLUE).append(srcConfig.original().mountList().extern()).append(NORMAL);
      sb.append(": ").append(srcConfig.original().devicePath());
      sb.append(" subvolume->").append(srcConfig.original().btrfsPath());
      sb.append(" (mounted as ").append(BLUE).append(srcConfig.original().mountPath()).append(NORMAL).append(")");
      getPanelSrc().setTitle(sb.toString());
      getPanelSrc().repaint();
      // System.out.println(sb.toString());
   }
   /**
    * Bereite das einfärben vor
    */
   private void abgleich() {
      ConcurrentSkipListMap<String, SnapshotLabel>  snapshotLabels_Uuid =getPanelSrc().labelTree_UUID;
      ConcurrentSkipListMap<String, SnapshotLabel>  backupLabels_Uuid   =getPanelBackup().labelTree_UUID;
      ConcurrentSkipListMap<String, SnapshotLabel>  backupLabels_KeyO   =getPanelBackup().labelTree_KeyO;
      ConcurrentSkipListMap<String, SnapshotLabel>  backupLabels_DirName=getPanelBackup().labelTree_DirName;
      // ConcurrentSkipListMap<String, SnapshotLabel> deleteLabels =new ConcurrentSkipListMap<>();
      // SINGLESNAPSHOT make or delete only one(1) snapshot per call
      // for DELETEOLD get all old snapshots that are "o=999" older than the newest one
      ConcurrentNavigableMap<String, SnapshotLabel> toDeleteOld         =new ConcurrentSkipListMap<>();
      if (Backsnap.DELETEOLD.get()) {
         if (getPanelSrc().labelTree_DirName.lastEntry() instanceof Entry<String, SnapshotLabel> lastEntry) {
            int           deleteOld=parseIntOrDefault(Backsnap.DELETEOLD.getParameter(), 999);
            // System.out.println("delOld: "+deleteOld);
            SnapshotLabel last     =lastEntry.getValue();
            int           firstNr  =parseIntOrDefault(last.snapshot.dirName(), deleteOld) - deleteOld;
            if (firstNr > 0) {
               toDeleteOld=backupLabels_DirName.headMap(Integer.toString(firstNr));
            }
         }
      }
      ConcurrentSkipListMap<String, SnapshotLabel> keineSackgasse=new ConcurrentSkipListMap<>();
      // suche Sackgassen um sie bevorzugt zu löschen
      if (!backupLabels_KeyO.isEmpty()) {
         SnapshotLabel child=backupLabels_KeyO.lastEntry().getValue();
         while (child != null) {
            if (keineSackgasse.containsValue(child))
               break; // Bloß keine Endlosschleife !
            Snapshot s=child.snapshot;
            keineSackgasse.put(s.key(), child);
            String        parent_uuid=s.parent_uuid();
            SnapshotLabel parent     =backupLabels_Uuid.get(parent_uuid);
            child=parent;
         }
      }
      // MINIMUMSNAPSHOTS
      int minimum=0;
      if (Backsnap.MINIMUMSNAPSHOTS.get())
         minimum=parseIntOrDefault(Backsnap.MINIMUMSNAPSHOTS.getParameter(), 499);
      ArrayList<SnapshotLabel> mixedList2;
      synchronized (getPanelBackup().mixedList) {
         mixedList2=new ArrayList<>(getPanelBackup().mixedList);
      }
      int deletable=mixedList2.size() - minimum;
      for (SnapshotLabel sl:backupLabels_DirName.values()) { // GrundFarbe setzen
         if (snapshotLabels_Uuid.containsKey(sl.snapshot.received_uuid())) {
            sl.setBackground(SnapshotLabel.backupColor);// Das ist ein aktuelles Backup ! (unlöschbar)
            snapshotLabels_Uuid.get(sl.snapshot.received_uuid()).setBackground(SnapshotLabel.backupColor);
         } else
            sl.setBackground(SnapshotLabel.naheColor);
         panelBackup.repaint(100);
      }
      ArrayList<SnapshotLabel> deleteList=new ArrayList<>();
      // Schauen was gelöscht werden könnte von den alten Backups
      for (SnapshotLabel sl:toDeleteOld.values()) {
         if (snapshotLabels_Uuid.containsKey(sl.snapshot.received_uuid()))
            continue;// aktuell, unlöschbar
         deleteList.add(sl);
         sl.setBackground(SnapshotLabel.deleteColor);// rot
         deletable--;
         panelBackup.repaint(100);
      }
      for (SnapshotLabel sl:mixedList2) { // manuell gelöschte anbieten
         if (deletable < 1)
            break;
         if (snapshotLabels_Uuid.containsKey(sl.snapshot.received_uuid()))
            continue;// aktuell, unlöschbar
         if (deleteList.contains(sl))
            continue;// wird schon gelöscht
         if (!manualDelete.containsValue(sl))
            continue; // nicht angeklickt
         deleteList.add(sl);
         sl.setBackground(SnapshotLabel.delete2Color); // orange
         deletable--;
         panelBackup.repaint(100);
      }
      for (SnapshotLabel sl:mixedList2) { // dann Sackgassen anbieten
         if (deletable < 1)
            break;
         if (snapshotLabels_Uuid.containsKey(sl.snapshot.received_uuid()))
            continue;// aktuell, unlöschbar
         if (deleteList.contains(sl))
            continue;
         if (keineSackgasse.containsValue(sl))
            continue;
         deleteList.add(sl);
         sl.setBackground(SnapshotLabel.delete2Color);
         deletable--;
         panelBackup.repaint(100);
      }
      for (SnapshotLabel sl:mixedList2) { // zuletzt reguläre Snapshots anbieten
         if (deletable < 1)
            break;
         if (snapshotLabels_Uuid.containsKey(sl.snapshot.received_uuid()))
            continue;// aktuell, unlöschbar
         if (deleteList.contains(sl))
            continue;
         deleteList.add(sl);
         sl.setBackground(SnapshotLabel.delete2Color);
         deletable--;
         panelBackup.repaint(100);
      }
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
    * Erstelle oder Erneuere die Anzeige der Backups
    * 
    * @param backupTree
    * @param backupDir
    * 
    */
   public void setBackup(SnapTree backupTree, String backupDir) {
      ConcurrentSkipListMap<String, Snapshot> passendBackups=new ConcurrentSkipListMap<>();
      Path                                    rest          =Path.of("/", backupDir);
      for (Snapshot snapshot:backupTree.dateMap().values()) { // sortiert nach otime
         Path pfad=snapshot.getMountPath();
         if (pfad == null)
            continue;
         if (pfad.startsWith(rest))
            passendBackups.put(snapshot.keyO(), snapshot);
      }
      ConcurrentSkipListMap<String, Snapshot> neuList=getPanelBackup().setVolume(backupTree.mount(),
               passendBackups.values());
      for (SnapshotLabel label:getPanelBackup().getLabels().values())
         label.addMouseListener(this);
      int           linefeeds=0;
      StringBuilder sb       =new StringBuilder("Backup:");
      for (Snapshot snap:neuList.values()) {
         sb.append(" ").append(snap.dirName());
         if ((sb.length() / 120) > linefeeds) {
            sb.append("\n");
            linefeeds++;
         }
      }
      System.out.println(sb.toString());
      abgleich();
      sb.setLength(0);
      sb.append("<html>Backup to ").append(BLUE).append(rest.getFileName()).append(NORMAL);
      sb.append(" on ").append(backupTree.mount().mountList().extern());
      sb.append(": ").append(backupTree.mount().devicePath());
      sb.append(" subvolume->").append(backupTree.mount().btrfsPath());
      sb.append(" (mounted as ").append(BLUE).append(backupTree.mount().mountPath()).append(NORMAL).append(")");
      getPanelBackup().setTitle(sb.toString());
      getPanelBackup().repaint();
      // System.out.println(sb.toString());
   }
   private JSplitPane getSplitPane() {
      if (splitPane == null) {
         splitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, getPanelSrc(), getPanelBackup());
         splitPane.setDividerLocation(.3d);
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
         sliderSpace.setMaximum(5000);
         sliderSpace.setMajorTickSpacing(1000);
         sliderSpace.setMinorTickSpacing(200);
         sliderSpace.setPaintTicks(true);
         sliderSpace.setPaintLabels(true);
         sliderSpace.setValue(999);
         Dictionary<Integer, JLabel> labelTable=new Hashtable<>();
         for (int i=0; i <= 5; i++)
            labelTable.put(Integer.valueOf(i * 1000), new JLabel((i == 0) ? "0" : Integer.toString(i) + "T"));
         sliderSpace.setLabelTable(labelTable);
         sliderSpace.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
               if (!getSliderSpace().getValueIsAdjusting()) {
                  int v=getSliderSpace().getValue();
                  Backsnap.DELETEOLD.setParameter(Integer.toString(v));
                  getLblSpace().setText(Integer.toString(v));
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
         sliderMeta.setMaximum(500);
         sliderMeta.setMajorTickSpacing(100);
         sliderMeta.setMinorTickSpacing(20);
         sliderMeta.setPaintLabels(true);
         sliderMeta.setPaintTicks(true);
         sliderMeta.setValue(249);
         sliderMeta.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
               if (!getSliderMeta().getValueIsAdjusting()) {
                  int v=getSliderMeta().getValue();
                  Backsnap.MINIMUMSNAPSHOTS.setParameter(Integer.toString(v));
                  getLblMeta().setText(Integer.toString(v));
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
   private JPanel getPanelInfo() {
      if (panelInfo == null) {
         panelInfo=new JPanel();
         panelInfo.setBorder(
                  new TitledBorder(null, "Backup Progress:", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panelInfo.setLayout(new BorderLayout(0, 0));
         panelInfo.add(getPanelProgress(), BorderLayout.WEST);
         panelInfo.add(getPanelPv());
         panelInfo.add(getPanelSpeed(), BorderLayout.EAST);
      }
      return panelInfo;
   }
   private JPanel getPanelProgress() {
      if (panelProgress == null) {
         panelProgress=new JPanel();
         panelProgress.setBorder(new EmptyBorder(0, 10, 0, 5));
         panelProgress.setLayout(new BorderLayout(0, 0));
         panelProgress.add(getProgressBar(), BorderLayout.CENTER);
      }
      return panelProgress;
   }
   private JPanel getPanelPv() {
      if (panelPv == null) {
         panelPv=new JPanel();
         panelPv.setBorder(new EmptyBorder(2, 5, 2, 5));
         panelPv.setLayout(new BoxLayout(panelPv, BoxLayout.X_AXIS));
         panelPv.add(getSnapshotName());
         panelPv.add(getLblPv());
      }
      return panelPv;
   }
   public JProgressBar getProgressBar() {
      if (progressBar == null) {
         progressBar=new JProgressBar();
         progressBar.setForeground(SnapshotLabel.backupColor);
         progressBar.setBackground(SnapshotLabel.naheColor);
         progressBar.setMaximum(1000);
         progressBar.setValue(1);
         progressBar.setStringPainted(true);
      }
      return progressBar;
   }
   public void getLblPvSetText(String s1) {
      String s2=s1.replace(IGEL1, IGEL2);
      if (s2.contentEquals(getLblPv().getText()))
         return;
      getLblPv().setText(s2);
      getLblPv().repaint(50);
   }
   private JLabel getLblPv() {
      if (lblPv == null) {
         lblPv=new JLabel("- Infozeile <=>");
         lblPv.setBorder(new EmptyBorder(0, 10, 0, 0));
      }
      return lblPv;
   }
   private JPanel getPanelSpeed() {
      if (panelSpeed == null) {
         panelSpeed=new JPanel();
         panelSpeed.setBorder(new EmptyBorder(0, 5, 0, 10));
         panelSpeed.setLayout(new BorderLayout(0, 0));
         panelSpeed.add(getSpeedBar(), BorderLayout.CENTER);
         panelSpeed.add(getTglPause(), BorderLayout.EAST);
      }
      return panelSpeed;
   }
   public JProgressBar getSpeedBar() {
      if (speedBar == null) {
         speedBar=new JProgressBar();
         speedBar.setForeground(SnapshotLabel.naheColor);
         speedBar.setBackground(SnapshotLabel.deleteColor);
         speedBar.setMaximum(100);
         speedBar.setValue(100);
         speedBar.setStringPainted(true);
         speedBar.setString(" running ");
      }
      return speedBar;
   }
   public JLabel getSnapshotName() {
      if (SnapshotName == null) {
         SnapshotName=new JLabel("this Snapshot ;-)");
      }
      return SnapshotName;
   }
   public JToggleButton getTglPause() {
      if (tglPause == null) {
         tglPause=new JToggleButton("pause");
      }
      return tglPause;
   }
   private JLabel getLblSpace() {
      if (lblSpace == null) {
         lblSpace=new JLabel("?");
         lblSpace.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return lblSpace;
   }
   private JLabel getLblMeta() {
      if (lblMeta == null) {
         lblMeta=new JLabel("?");
         lblMeta.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return lblMeta;
   }
}
