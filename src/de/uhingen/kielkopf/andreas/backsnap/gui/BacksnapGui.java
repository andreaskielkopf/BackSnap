/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.*;

import javax.swing.*;

import de.uhingen.kielkopf.andreas.backsnap.Backsnap;
import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.btrfs.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.time.Instant;

import javax.swing.border.TitledBorder;

import de.uhingen.kielkopf.andreas.backsnap.gui.element.Lbl;
import de.uhingen.kielkopf.andreas.backsnap.gui.element.TxtFeld;
import de.uhingen.kielkopf.andreas.backsnap.gui.part.*;
import de.uhingen.kielkopf.andreas.beans.cli.Flag;

/**
 * @author Andreas Kielkopf
 * 
 *         Eine GUI um BackSnap zu beobachten und Zusatzfunktionen auszulösen
 *
 */
public class BacksnapGui implements MouseListener {
   static private BacksnapGui                          backSnapGui;
   public JFrame                                       frame;
   private SnapshotPanel                               panelSrc;
   private SnapshotPanel                               panelBackup;
   private JSplitPane                                  splitPaneSnapshots;
   public ConcurrentSkipListMap<String, SnapshotLabel> manualDelete=new ConcurrentSkipListMap<>();
   private JPanel                                      panelProgress;
   private JPanel                                      panelEnde;
   private JProgressBar                                progressBar;
   private TxtFeld                                     textPv;
   static public final String                          BLUE        ="<font size=+1 color=\"3333ff\">";
   static public final String                          NORMAL      ="</font>";
   static public final String                          IGEL1       ="<=>";
   static public final String                          IGEL2       =BLUE + "=O=" + NORMAL;
   private MaintenancePanel                            panelMaintenance;
   private JSplitPane                                  splitPaneMaintenance;
   private JPanel                                      panelParameter;
   private Lbl                                         labelParameterInfo;
   private TxtFeld                                     lblArgs;
   private JToggleButton                               tglPause;
   /**
    * @param args
    */
   static public void main2(String[] args) {
      if (backSnapGui == null)
         try {
            backSnapGui=new BacksnapGui();
         } catch (IOException e) {
            e.printStackTrace();
         } // leere GUI erzeugen ;-) nur für Designer
      if (backSnapGui != null)
         EventQueue.invokeLater(() -> {
            try {
               backSnapGui.frame.setVisible(true);
               backSnapGui.getSplitPaneSnapshots().setDividerLocation(1d / 3d);
            } catch (final Exception e) {
               e.printStackTrace();
            }
         });
   }
   /**
    * Create the application.
    * 
    * @throws IOException
    * 
    * @wbp.parser.entryPoint
    */
   public BacksnapGui() throws IOException {
      UIManager.put("ProgressBar.selectionForeground", Color.black);
      UIManager.put("ProgressBar.selectionBackground", Color.black);
      initialize();
      getPanelMaintenance().getPanelMeta().getSliderMeta()
               .setValue(parseIntOrDefault(Backsnap.KEEP_MINIMUM.getParameter(), PanelMeta.DEFAULT_META));
      if (Backsnap.KEEP_MINIMUM.get())
         getPanelMaintenance().getPanelMeta().flagMeta();
      getPanelMaintenance().getPanelSpace().getSliderSpace()
               .setValue(parseIntOrDefault(Backsnap.DELETEOLD.getParameter(), PanelSpace.DEFAULT_SPACE));
      if (Backsnap.DELETEOLD.get())
         getPanelMaintenance().getPanelSpace().flagSpace();
   }
   /**
    * @param snapConfigs
    * @param srcVolume
    * @param srcSubVolumes
    * @param backupVolume
    * @param backupSubVolumes
    * @throws IOException
    */
   public BacksnapGui(List<SnapConfig> snapConfigs, Mount srcVolume, SubVolumeList srcSubVolumes, Mount backupVolume,
            SubVolumeList backupSubVolumes) throws IOException {
      this();
   }
   /**
    * @throws IOException
    * 
    */
   private void initialize() throws IOException {
      frame=new JFrame(Backsnap.BACK_SNAP_VERSION);
      // if (Beans.isDesignTime()) {
      frame.getContentPane().add(getPanelOben(), BorderLayout.CENTER);
      frame.getContentPane().add(getPanelUnten(), BorderLayout.SOUTH);
      Dimension screenSize=Toolkit.getDefaultToolkit().getScreenSize();
      final int START_W=(3840 * 40) / 100;
      final int START_H=(2160 * 40) / 100;
      int width=Math.min(screenSize.width, START_W);
      int height=Math.min(screenSize.height, START_H);
      int x=(screenSize.width <= START_W) ? 0 : screenSize.width - START_W;
      frame.setBounds(x, 0, width, height);
      // }
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
   }
   /**
    * @param usage 
    * @param backupDir 
    * @param backupTree 
    * @param srcConfig 
    * @param bsGui
    * @throws IOException 
    */
//   static public void setGui(BacksnapGui bsGui) {
//      backSnapGui=bsGui;
//   }
   static public BacksnapGui getGui(SnapConfig srcConfig, SnapTree backupTree, String backupLabel, Usage usage) throws IOException {
      if (backSnapGui==null)
      {
         backSnapGui=new BacksnapGui();
//         BacksnapGui.setGui(bsGui);
         main2(null); // zeigt die GUI an
         backSnapGui.setArgs(Flag.getArgs());
         backSnapGui.setSrc(srcConfig);
         backSnapGui.setBackup(backupTree, backupLabel);
         backSnapGui.setUsage(usage);
         SwingUtilities.invokeLater(() -> {
            try {
               backSnapGui.getSplitPaneSnapshots().setDividerLocation(1d / 3d);
            } catch (IOException ignore) { /* ignore */ }
         });
         SwingUtilities.invokeLater(() -> backSnapGui.getPanelMaintenance().updateButtons());
      }
      backSnapGui.getProgressBar().setMaximum(srcConfig.volumeMount().otimeKeyMap().size());
      return backSnapGui;
   }
private SnapshotPanel getPanelSrc() throws IOException {
      if (panelSrc == null) {
         panelSrc=new SnapshotPanel();
      }
      return panelSrc;
   }
   /**
    * @param srcConfig.original()
    * @throws IOException
    */
   public void setSrc(SnapConfig srcConfig) throws IOException {
      int linefeeds=0;
      StringBuilder sb=new StringBuilder("Src:");
      ConcurrentSkipListMap<String, Snapshot> neuList=getPanelSrc()
               .setVolume(srcConfig.volumeMount().otimeKeyMap().values());
      for (Snapshot snap:neuList.values()) {
         sb.append(" ").append(snap.dirName());
         if ((sb.length() / 120) > linefeeds) {
            sb.append("\n");
            linefeeds++;
         }
      }
      Backsnap.logln(2, sb.toString());
      abgleich();
      getPanelSrc().setInfo(srcConfig.volumeMount());
   }
   /**
    * Bereite das einfärben vor
    * 
    * @throws IOException
    */
   public void abgleich() throws IOException {
      ConcurrentSkipListMap<String, SnapshotLabel> snapshotLabels_Uuid=getPanelSrc().labelTree_UUID;
      ConcurrentSkipListMap<String, SnapshotLabel> backupLabels_Uuid=getPanelBackup().labelTree_UUID;
      ConcurrentSkipListMap<String, SnapshotLabel> backupLabels_KeyO=getPanelBackup().labelTree_KeyO;
      ConcurrentSkipListMap<String, SnapshotLabel> backupLabels_DirName=getPanelBackup().labelTree_DirName;
      // SINGLESNAPSHOT make or delete only one(1) snapshot per call
      // for DELETEOLD get all old snapshots that are "o=999" older than the newest one
      ConcurrentNavigableMap<String, SnapshotLabel> toDeleteOld=new ConcurrentSkipListMap<>();
      if (Backsnap.DELETEOLD.get()) {
         int deleteOld=parseIntOrDefault(Backsnap.DELETEOLD.getParameter(), PanelSpace.DEFAULT_SPACE);
         if (getPanelSrc().labelTree_DirName.lastEntry() instanceof Entry<String, SnapshotLabel> lastEntry) {
            Backsnap.logln(8, "delOld: " + deleteOld);
            SnapshotLabel last=lastEntry.getValue();
            Snapshot ls=last.snapshot;
            Instant q=ls.stunden();
            if (q != null) {
               Instant grenze=q.minusSeconds(deleteOld * 3600l);// 1 Snapshot pro Stunde rechnen wie bei snapper üblich
               for (Entry<String, SnapshotLabel> entry:backupLabels_DirName.descendingMap().entrySet()) {
                  Instant instant=entry.getValue().snapshot.stunden();
                  if (instant.isBefore(grenze))
                     toDeleteOld.put(entry.getKey(), entry.getValue());
               }
            } else
               if (ls.btrfsPath().toString().startsWith("/timeshift-btrfs")) {
                  if (backupLabels_DirName.lastEntry() instanceof Entry<String, SnapshotLabel> lastBackupLabel) {
                     SnapshotLabel lastB=lastBackupLabel.getValue();
                     int firstId=lastB.snapshot.id() - deleteOld;
                     for (Entry<String, SnapshotLabel> entry:backupLabels_DirName.descendingMap().entrySet()) {
                        int id=entry.getValue().snapshot.id();
                        if (id <= firstId)
                           toDeleteOld.put(entry.getKey(), entry.getValue());
                     }
                  }
               } else {
                  int firstNr=parseIntOrDefault(last.snapshot.dirName(), deleteOld) - deleteOld;
                  if (firstNr > 0) {
                     toDeleteOld=backupLabels_DirName.headMap(Integer.toString(firstNr));
                  }
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
            keineSackgasse.put(s.keyO(), child);
            String parent_uuid=s.parent_uuid();
            SnapshotLabel parent=backupLabels_Uuid.get(parent_uuid);
            child=parent;
         }
      }
      // KEEP_MINIMUM
      int minimum=PanelMeta.DEFAULT_META;
      if (Backsnap.KEEP_MINIMUM.get())
         minimum=parseIntOrDefault(Backsnap.KEEP_MINIMUM.getParameter(), PanelMeta.DEFAULT_META);
      ArrayList<SnapshotLabel> mixedList2;
      synchronized (getPanelBackup().mixedList) {
         mixedList2=new ArrayList<>(getPanelBackup().mixedList);
      }
      int deletable=mixedList2.size() - minimum;
      for (SnapshotLabel sl:backupLabels_DirName.values()) { // GrundFarbe setzen
         if (snapshotLabels_Uuid.containsKey(sl.snapshot.received_uuid())) {
            sl.setBackground(SnapshotLabel.allesOkColor);// Das ist ein aktuelles Backup ! (unlöschbar)
            snapshotLabels_Uuid.get(sl.snapshot.received_uuid()).setBackground(SnapshotLabel.allesOkColor);
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
         sl.setBackground(SnapshotLabel.deleteOldColor);// rot
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
   public void delete(final JButton jButton, Color deleteColor) throws IOException {
      ConcurrentSkipListMap<String, SnapshotLabel> alle=getPanelBackup().labelTree_KeyO;
      final ArrayList<Snapshot> toRemove=new ArrayList<>();
      for (Entry<String, SnapshotLabel> entry:alle.entrySet())
         if (entry.getValue() instanceof SnapshotLabel label)
            if (label.getBackground() == deleteColor) {
               Backsnap.logln(6, "to remove " + label.getText());
               toRemove.add(label.snapshot);
            }
      try {
         if (Backsnap.BTRFS_LOCK.tryLock(1, TimeUnit.SECONDS))
            try {
               Commandline.background.submit(new Runnable() {
                  @Override
                  public void run() {
                     jButton.setEnabled(false);
                     for (Snapshot snapshot:toRemove) {
                        if (jButton == getPanelMaintenance().getPanelMeta().getBtnMeta())
                           if (!getPanelMaintenance().getPanelMeta().getChckMeta().isSelected())
                              continue;
                        if (jButton == getPanelMaintenance().getPanelSpace().getBtnSpace())
                           if (!getPanelMaintenance().getPanelSpace().getChckSpace().isSelected())
                              continue;
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
                     Backsnap.logln(1, "");
                     jButton.setEnabled(true);
                  }
               });
            } finally {
               Backsnap.BTRFS_LOCK.unlock();
            }
      } catch (InterruptedException ignore) { /* */ }
   }
   static public final int parseIntOrDefault(String s, int def) {
      if (s != null)
         try {
            return Integer.parseInt(s);
         } catch (NumberFormatException ignore) {
            System.err.println(ignore.getMessage() + ":" + s);
         }
      return def;
   }
   private SnapshotPanel getPanelBackup() throws IOException {
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
    * @throws IOException
    * 
    */
   public void setBackup(SnapTree backupTree, String backupLabel) throws IOException {
      ConcurrentSkipListMap<String, Snapshot> passendBackups=new ConcurrentSkipListMap<>();
      Path rest=Path.of(Backsnap.MNT_BACKSNAP, backupLabel);
      for (Snapshot snapshot:backupTree.dateMap().values()) { // sortiert nach otime
         Path pfad=snapshot.getBackupMountPath();
         if (pfad == null)
            continue;
         if (pfad.startsWith(rest))
            passendBackups.put(snapshot.keyB(), snapshot);
      }
      ConcurrentSkipListMap<String, Snapshot> neuList=getPanelBackup().setVolume(passendBackups.values());
      for (SnapshotLabel label:getPanelBackup().getLabels().values())
         label.addMouseListener(this);
      int linefeeds=0;
      StringBuilder sb=new StringBuilder("Backup:");
      for (Snapshot snap:neuList.values()) {
         sb.append(" ").append(snap.dirName());
         if ((sb.length() / 120) > linefeeds) {
            sb.append("\n");
            linefeeds++;
         }
      }
      Backsnap.logln(2, sb.toString());
      abgleich();
      getPanelBackup().setTitle("Backup to Label " + backupLabel);
      getPanelBackup().setInfo(backupTree.mount());
   }
   public JSplitPane getSplitPaneSnapshots() throws IOException {
      if (splitPaneSnapshots == null) {
         splitPaneSnapshots=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, getPanelSrc(), getPanelBackup());
         splitPaneSnapshots.setDividerLocation(.3d);
      }
      return splitPaneSnapshots;
   }
   @Override
   public void mouseClicked(MouseEvent e) {
      if (e.getSource() instanceof SnapshotLabel sl) {
         Backsnap.log(8, "click-" + sl);
         if (!manualDelete.containsValue(sl))
            manualDelete.put(sl.getText(), sl);
         else
            manualDelete.remove(sl.getText());
         Backsnap.log(8, manualDelete.containsValue(sl) ? "del" : "keep");
         try {
            abgleich();
         } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         }
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
   private JPanel getPanelProgress() {
      if (panelProgress == null) {
         panelProgress=new JPanel();
         panelProgress.setLayout(new BorderLayout(10, 0));
         panelProgress.add(getProgressBar(), BorderLayout.WEST);
         panelProgress.add(getPanelWork(), BorderLayout.CENTER);
         panelProgress.add(getPanelLive(), BorderLayout.EAST);
      }
      return panelProgress;
   }
   private JPanel getPanelEnde() {
      if (panelEnde == null) {
         panelEnde=new JPanel();
         panelEnde.setLayout(new BorderLayout(10, 0));
         panelEnde.add(getTglPause(), BorderLayout.WEST);
         panelEnde.add(getSpeedBar(), BorderLayout.CENTER);
      }
      return panelEnde;
   }
   public JProgressBar getProgressBar() {
      if (progressBar == null) {
         progressBar=new JProgressBar();
         progressBar.setPreferredSize(new Dimension(200, 30));
         progressBar.setForeground(SnapshotLabel.allesOkColor);
         progressBar.setBackground(SnapshotLabel.naheColor);
         progressBar.setMaximum(1000);
         progressBar.setValue(1);
         progressBar.setStringPainted(true);
         progressBar.setFont(SnapshotPanel.FONT_INFO);
      }
      return progressBar;
   }
   public void lblPvSetText(final String s0) {
      EventQueue.invokeLater(() -> {
         final PvInfo pv=new PvInfo(s0);
         if (!pv.progress().isEmpty()) {
            getTxtSize().setText(pv.size());
            getTxtTime().setText(pv.time());
            getTxtSpeed().setText(pv.speed());
            getTxtWork().setText(pv.progress());
            getPanelWork().revalidate();
            getTxtWork().repaint(50);
            getPanelWork().repaint(50);
            return;
         }
         if (s0.contains("<")) {
            String[] s1=s0.trim().split("\\] \\[");
            if (s1.length == 2) {
               String[] s3=s1[0].replace(" B", "_B").replace("[ ", "[_").split(" ");
               if (s3.length == 3) {
                  getTxtSize().setText(s3[0].replace('_', ' '));
                  getTxtTime().setText(s3[1]);
                  getTxtSpeed().setText(s3[2].replace("[", ""));
                  getTxtWork().setText(" " + s1[1].replace(' ', '.').replace("]", " "));
                  getPanelWork().revalidate();
                  getPanelWork().repaint(50);
                  return;
               }
               System.out.println("s3=" + s3.length);
               System.out.println(s3);
            }
            System.out.println("s1=" + s1.length);
            System.out.println(s1);
         }
         getTextPv().setText(""); // System.out.println(s2);
         getTextPv().repaint(50);
      });
   }
   private TxtFeld getTextPv() {
      if (textPv == null) {
         textPv=new TxtFeld("Info");
         // textPv.setPreferredSize(new Dimension(200, 30));
      }
      return textPv;
   }
   /**
    * @param s
    * @throws IOException
    */
   public void mark(Snapshot s) throws IOException {
      for (SnapshotLabel sl1:getPanelBackup().labelTree_UUID.values())
         if (sl1.snapshot == s) {
            sl1.setBackground(SnapshotLabel.markInProgressColor);
            getPanelBackup().repaint(100);
         }
      for (SnapshotLabel sl2:getPanelSrc().labelTree_UUID.values())
         if (sl2.snapshot == s) {
            sl2.setBackground(SnapshotLabel.markInProgressColor);
            getPanelSrc().repaint(100);
         }
   }
   public MaintenancePanel getPanelMaintenance() {
      if (panelMaintenance == null) {
         panelMaintenance=new MaintenancePanel(this);
         panelMaintenance.setPreferredSize(new Dimension(1000, 125));
      }
      return panelMaintenance;
   }
   private JSplitPane getSplitPaneMaintenance() {
      if (splitPaneMaintenance == null) {
         // Das Maintenance-Panel erst bei Bedarf eintragen ???
         splitPaneMaintenance=new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, getPanelEnde(), getPanelMaintenance());
         splitPaneMaintenance.setToolTipText("show maintenance tools");
         splitPaneMaintenance.setDividerSize((splitPaneMaintenance.getDividerSize() * 3) / 2);
         splitPaneMaintenance.setOneTouchExpandable(true);
         splitPaneMaintenance.setResizeWeight(1d);
         splitPaneMaintenance.setDividerLocation(100000);
      }
      return splitPaneMaintenance;
   }
   private JPanel getPanelParameter() {
      if (panelParameter == null) {
         panelParameter=new JPanel();
         panelParameter.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
         panelParameter.add(getLabelParameterInfo());
         panelParameter.add(getLblArgs());
      }
      return panelParameter;
   }
   private Lbl getLabelParameterInfo() {
      if (labelParameterInfo == null) {
         labelParameterInfo=new Lbl("Args : ");
      }
      return labelParameterInfo;
   }
   private TxtFeld getLblArgs() {
      if (lblArgs == null) {
         lblArgs=new TxtFeld("?");
      }
      return lblArgs;
   }
   /**
    * @param argLine
    */
   public void setArgs(String argLine) {
      getLblArgs().setText(argLine);
   }
   public JToggleButton getTglPause() {
      if (tglPause == null) {
         tglPause=new JToggleButton("pause for maintenance work");
         tglPause.addActionListener(e -> showMaintenance());
         tglPause.setPreferredSize(new Dimension(200, 30));
      }
      return tglPause;
   }
   public void showMaintenance() {
      if (getTglPause().isSelected()) {
         getPanelUnten().setPreferredSize(PANEL_UNTEN_DIM2);
         getSplitPaneMaintenance().setDividerLocation(1d);
      } else {
         getPanelUnten().setPreferredSize(PANEL_UNTEN_DIM);
         getSplitPaneMaintenance().setDividerLocation(100000);
      }
      getPanelUnten().revalidate();
      getSplitPaneMaintenance().resetToPreferredSizes();
   }
   private JProgressBar speedBar;
   private JPanel       panelOben;
   private JPanel       panelUnten;
   public JProgressBar getSpeedBar() {
      if (speedBar == null) {
         speedBar=new JProgressBar();
         speedBar.setEnabled(false);
         speedBar.setPreferredSize(new Dimension(200, 25));
         speedBar.setForeground(SnapshotLabel.allesOkColor);
         speedBar.setBackground(SnapshotLabel.markInProgressColor);
         speedBar.setMaximum(100);
         speedBar.setValue(0);
         speedBar.setStringPainted(true);
         speedBar.setString("backup is running");
      }
      return speedBar;
   }
   private JPanel getPanelOben() throws IOException {
      if (panelOben == null) {
         panelOben=new JPanel();
         panelOben.setBorder(new TitledBorder(null, "backup", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panelOben.setLayout(new BorderLayout(0, 0));
         panelOben.add(getPanelParameter(), BorderLayout.NORTH);
         panelOben.add(getSplitPaneSnapshots(), BorderLayout.CENTER);
         panelOben.add(getPanelProgress(), BorderLayout.SOUTH);
      }
      return panelOben;
   }
   private final Dimension PANEL_UNTEN_DIM =new Dimension(10, 80);
   private final Dimension PANEL_UNTEN_DIM2=new Dimension(10, 180);
   private JPanel          panelWork;
   private Lbl             lblParent;
   private TxtFeld         txtSnapshot;
   private Lbl             lblSnapshot;
   private TxtFeld         txtParent;
   private JPanel          panelLive;
   private TxtFeld         txtSpeed;
   private TxtFeld         txtWork;
   private Lbl             lblSpeed;
   private Lbl             lblWork;
   private Lbl             lblTime;
   private TxtFeld         txtTime;
   private Lbl             lblSize;
   private TxtFeld         txtSize;
   private JPanel getPanelUnten() {
      if (panelUnten == null) {
         panelUnten=new JPanel();
         panelUnten.setPreferredSize(PANEL_UNTEN_DIM);
         panelUnten
                  .setBorder(new TitledBorder(null, "maintenance", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panelUnten.setLayout(new BorderLayout(0, 0));
         panelUnten.add(getSplitPaneMaintenance());
      }
      return panelUnten;
   }
   public JPanel getPanelWork() {
      if (panelWork == null) {
         panelWork=new JPanel();
         panelWork.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
         panelWork.add(getLblSnapshot());
         panelWork.add(getTxtSnapshot());
         panelWork.add(getLblParent());
         panelWork.add(getTxtParent());
         panelWork.add(getLblWork());
         panelWork.add(getTxtWork());
         panelWork.add(getLblSpeed());
         panelWork.add(getTxtSpeed());
         panelWork.add(getLblTime());
         panelWork.add(getTxtTime());
         panelWork.add(getLblSize());
         panelWork.add(getTxtSize());
      }
      return panelWork;
   }
   public Lbl getLblParent() {
      if (lblParent == null) {
         lblParent=new Lbl(":");
         lblParent.setVerticalAlignment(SwingConstants.BOTTOM);
      }
      return lblParent;
   }
   public TxtFeld getTxtSnapshot() {
      if (txtSnapshot == null) {
         txtSnapshot=new TxtFeld("-");
      }
      return txtSnapshot;
   }
   public Lbl getLblSnapshot() {
      if (lblSnapshot == null) {
         lblSnapshot=new Lbl(":");
         lblSnapshot.setVerticalAlignment(SwingConstants.BOTTOM);
      }
      return lblSnapshot;
   }
   public TxtFeld getTxtParent() {
      if (txtParent == null) {
         txtParent=new TxtFeld("-");
      }
      return txtParent;
   }
   private JPanel getPanelLive() {
      if (panelLive == null) {
         panelLive=new JPanel();
         panelLive.setLayout(new BorderLayout(0, 0));
         panelLive.add(getTextPv());
      }
      return panelLive;
   }
   private TxtFeld getTxtSpeed() {
      if (txtSpeed == null) {
         txtSpeed=new TxtFeld("-");
      }
      return txtSpeed;
   }
   private TxtFeld getTxtWork() {
      if (txtWork == null) {
         txtWork=new TxtFeld("-");
         txtWork.setFont(SnapshotPanel.FONT_INFO_B);
         txtWork.setOpaque(true);
         txtWork.setBackground(SnapshotLabel.markInProgressColor);
      }
      return txtWork;
   }
   private Lbl getLblSpeed() {
      if (lblSpeed == null) {
         lblSpeed=new Lbl("speed:");
         lblSpeed.setVerticalAlignment(SwingConstants.BOTTOM);
      }
      return lblSpeed;
   }
   private Lbl getLblWork() {
      if (lblWork == null) {
         lblWork=new Lbl("");
         lblWork.setVerticalAlignment(SwingConstants.BOTTOM);
      }
      return lblWork;
   }
   private Lbl getLblTime() {
      if (lblTime == null) {
         lblTime=new Lbl("time:");
      }
      return lblTime;
   }
   private TxtFeld getTxtTime() {
      if (txtTime == null) {
         txtTime=new TxtFeld("-");
      }
      return txtTime;
   }
   private Lbl getLblSize() {
      if (lblSize == null) {
         lblSize=new Lbl("size:");
      }
      return lblSize;
   }
   private TxtFeld getTxtSize() {
      if (txtSize == null) {
         txtSize=new TxtFeld("-");
      }
      return txtSize;
   }
   /**
    * @param usage
    */
   public void setUsage(Usage usage) {
      if (usage.isFull() || usage.needsBalance()) {
         getTglPause().setSelected(false);
         getTglPause().doClick();
      }
      getPanelMaintenance().setUsage(usage);
   }
   /**
    * @param srcSnapshot
    * @param parentSnapshot
    */
   public void setBackupInfo(Snapshot srcSnapshot, Snapshot parentSnapshot) {
      Backsnap.logln(7, "<html>" + srcSnapshot.getSnapshotMountPath().toString());
      getLblSnapshot().setText("backup of : ");
      getTxtSnapshot().setText(srcSnapshot.dirName());
      getLblParent().setText((parentSnapshot == null) ? " " : "based on:");
      getTxtParent().setText((parentSnapshot == null) ? " " : parentSnapshot.dirName());
      getPanelWork().repaint(50);
   }
}
