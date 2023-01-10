/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap;

import java.awt.*;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.*;

import de.uhingen.kielkopf.andreas.backsnap.btrfs.*;

/**
 * @author Andreas Kielkopf
 *
 */
public class BacksnapGui {
   private static BacksnapGui backSnapGui;
   JFrame                     frame;
   private JPanel             panel;
   private JLabel             lblNewLabel;
   private JPanel             panel_1;
   private SnapshotPanel      panelSrc;
   private SnapshotPanel      panelBackup;
   /**
    * @param args
    */
   public static void main(String[] args) {
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
   public BacksnapGui(List<SnapConfig> snapConfigs, Subvolume srcVolume, SubVolumeList srcSubVolumes,
            Subvolume backupVolume, SubVolumeList backupSubVolumes) {
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
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getLblNewLabel(), BorderLayout.NORTH);
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
         panel_1.setLayout(new GridLayout(0, 2, 0, 0));
         panel_1.add(getPanelSrc());
         panel_1.add(getPanelBackup());
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
    * @param srcVolume
    */
   public void setSrc(Subvolume srcVolume) {
      getPanelSrc().setVolume(srcVolume, srcVolume.snapshotTree());
      abgleich();
      getPanelSrc().repaint();
   }
   /**
    * 
    */
   private void abgleich() {
      ConcurrentSkipListMap<String, SnapshotLabel> snapshotLabels=getPanelSrc().labelTree_UUID;
      ConcurrentSkipListMap<String, SnapshotLabel> backupLabels  =panelBackup.labelTree_UUID;
      // Im Backup Farben bestimmen
      for (SnapshotLabel label:backupLabels.values()) {
         Snapshot snapshot    =label.snapshot;
         String   recievedUuid=snapshot.received_uuid();
         if (snapshotLabels.containsKey(recievedUuid)) {
            label.setBackground(SnapshotLabel.backupColor);
            snapshotLabels.get(recievedUuid).setBackground(SnapshotLabel.backupColor);
         }
      }
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
   public void setBackup(Subvolume backupVolume, TreeMap<String, Snapshot> receivedSnapshots, String backupDir) {
      TreeMap<String, Snapshot> passendBackups=new TreeMap<>();
      String                    mount         =backupVolume.mountPoint();
      if (!mount.endsWith("/"))
         mount+="/";
      String rest=backupDir.replaceFirst(mount, "");
      if (!rest.endsWith("/"))
         rest+="/";
      for (Snapshot snapshot:receivedSnapshots.values()) {
         String pfad=snapshot.path().toString();
         if (pfad.startsWith(rest))
            passendBackups.put(snapshot.key(), snapshot);
      }
      getPanelBackup().setVolume(backupVolume, passendBackups);
      abgleich();
      getPanelBackup().repaint();
   }
}
