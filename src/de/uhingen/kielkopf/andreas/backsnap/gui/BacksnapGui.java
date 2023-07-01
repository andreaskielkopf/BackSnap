/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui;

import static de.uhingen.kielkopf.andreas.backsnap.gui.part.SnapshotPanel.FONT_INFO;
import static de.uhingen.kielkopf.andreas.backsnap.gui.part.SnapshotPanel.FONT_INFO_B;
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
import de.uhingen.kielkopf.andreas.backsnap.gui.part.SnapshotLabel;
import de.uhingen.kielkopf.andreas.backsnap.gui.part.SnapshotPanel;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.time.Instant;

import javax.swing.border.TitledBorder;
import de.uhingen.kielkopf.andreas.backsnap.gui.part.MaintenancePanel;

/**
 * @author Andreas Kielkopf
 * 
 *         Eine GUI um BackSnap zu beobachten und Zusatzfunktionen auszulösen
 *
 */
public class BacksnapGui implements MouseListener {
   private static BacksnapGui                          backSnapGui;
   public JFrame                                       frame;
   private SnapshotPanel                               panelSrc;
   private SnapshotPanel                               panelBackup;
   private JSplitPane                                  splitPaneSnapshots;
   public ConcurrentSkipListMap<String, SnapshotLabel> manualDelete=new ConcurrentSkipListMap<>();
   private JPanel                                      panelProgress;
   private JPanel                                      panelEnde;
   private JProgressBar                                progressBar;
   private JLabel                                      lblPv;
   // private JLabel SnapshotName;
   public final static String                          BLUE        ="<font size=+1 color=\"3333ff\">";
   public final static String                          NORMAL      ="</font>";
   public final static String                          IGEL1       ="<=>";
   public final static String                          IGEL2       =BLUE + "=O=" + NORMAL;
   private MaintenancePanel                            panelMaintenance;
   private JSplitPane                                  splitPaneMaintenance;
   private JPanel                                      panelParameter;
   private JLabel                                      labelParameterInfo;
   private JLabel                                      lblArgs;
   private JToggleButton                               tglPause;
   /**
    * @param args
    */
   public static void main2(String[] args) {
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
      getPanelMaintenance().getSliderMeta()
               .setValue(parseIntOrDefault(Backsnap.KEEP_MINIMUM.getParameter(), MaintenancePanel.DEFAULT_META));
      if (Backsnap.KEEP_MINIMUM.get())
         getPanelMaintenance().flagMeta();
      getPanelMaintenance().getSliderSpace()
               .setValue(parseIntOrDefault(Backsnap.DELETEOLD.getParameter(), MaintenancePanel.DEFAULT_SPACE));
      if (Backsnap.DELETEOLD.get())
         getPanelMaintenance().flagSpace();
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
      frame.getContentPane().add(getPanelOben(), BorderLayout.CENTER);
      frame.getContentPane().add(getPanelUnten(), BorderLayout.SOUTH);
      Dimension screenSize=Toolkit.getDefaultToolkit().getScreenSize();
      int       width     =Math.min(screenSize.width, 3840 / 2);
      int       height    =Math.min(screenSize.height, 2160 / 2);
      int       x         =(screenSize.width <= (3840 / 2)) ? 0 : screenSize.width - (3840 / 2);
      frame.setBounds(x, 0, width, height);
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
   }
   /**
    * @param bsGui
    */
   public static void setGui(BacksnapGui bsGui) {
      backSnapGui=bsGui;
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
      int                                     linefeeds=0;
      StringBuilder                           sb       =new StringBuilder("Src:");
      ConcurrentSkipListMap<String, Snapshot> neuList  =getPanelSrc().setVolume(srcConfig.volumeMount(),
               srcConfig.volumeMount().otimeKeyMap().values());
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
      ConcurrentSkipListMap<String, SnapshotLabel>  snapshotLabels_Uuid =getPanelSrc().labelTree_UUID;
      ConcurrentSkipListMap<String, SnapshotLabel>  backupLabels_Uuid   =getPanelBackup().labelTree_UUID;
      ConcurrentSkipListMap<String, SnapshotLabel>  backupLabels_KeyO   =getPanelBackup().labelTree_KeyO;
      ConcurrentSkipListMap<String, SnapshotLabel>  backupLabels_DirName=getPanelBackup().labelTree_DirName;
      // ConcurrentSkipListMap<String, SnapshotLabel> deleteLabels =new ConcurrentSkipListMap<>();
      // SINGLESNAPSHOT make or delete only one(1) snapshot per call
      // for DELETEOLD get all old snapshots that are "o=999" older than the newest one
      ConcurrentNavigableMap<String, SnapshotLabel> toDeleteOld         =new ConcurrentSkipListMap<>();
      if (Backsnap.DELETEOLD.get()) {
         int deleteOld=parseIntOrDefault(Backsnap.DELETEOLD.getParameter(), MaintenancePanel.DEFAULT_SPACE);
         if (getPanelSrc().labelTree_DirName.lastEntry() instanceof Entry<String, SnapshotLabel> lastEntry) {
            Backsnap.logln(8, "delOld: " + deleteOld);
            SnapshotLabel last=lastEntry.getValue();
            Snapshot      ls  =last.snapshot;
            Instant       q   =ls.stunden();
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
                     SnapshotLabel lastB  =lastBackupLabel.getValue();
                     int           firstId=lastB.snapshot.id() - deleteOld;
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
            String        parent_uuid=s.parent_uuid();
            SnapshotLabel parent     =backupLabels_Uuid.get(parent_uuid);
            child=parent;
         }
      }
      // KEEP_MINIMUM
      int minimum=MaintenancePanel.DEFAULT_META;
      if (Backsnap.KEEP_MINIMUM.get())
         minimum=parseIntOrDefault(Backsnap.KEEP_MINIMUM.getParameter(), MaintenancePanel.DEFAULT_META);
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
      ConcurrentSkipListMap<String, SnapshotLabel> alle    =getPanelBackup().labelTree_KeyO;
      final ArrayList<Snapshot>                    toRemove=new ArrayList<>();
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
                        if (jButton == getPanelMaintenance().getBtnMeta())
                           if (!getPanelMaintenance().getChckMeta().isSelected())
                              continue;
                        if (jButton == getPanelMaintenance().getBtnSpace())
                           if (!getPanelMaintenance().getChckSpace().isSelected())
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
   final static public int parseIntOrDefault(String s, int def) {
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
   public void setBackup(SnapTree backupTree, String backupDir) throws IOException {
      ConcurrentSkipListMap<String, Snapshot> passendBackups=new ConcurrentSkipListMap<>();
      Path                                    rest          =Path.of("/", backupDir);
      for (Snapshot snapshot:backupTree.dateMap().values()) { // sortiert nach otime
         Path pfad=snapshot.getBackupMountPath();
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
      Backsnap.logln(2, sb.toString());
      abgleich();
      getPanelBackup().setTitle("Backup to Label " + rest.getFileName());
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
         getLblPv().setText(""); // System.out.println(s2);
         getLblPv().repaint(50);
      });
   }
   private JLabel getLblPv() {
      if (lblPv == null) {
         lblPv=new JLabel("Info");
         lblPv.setHorizontalAlignment(SwingConstants.CENTER);
         lblPv.setPreferredSize(new Dimension(200, 30));
      }
      return lblPv;
   }
   // public JLabel getSnapshotName() {
   // if (SnapshotName == null) {
   // SnapshotName=new JLabel("this Snapshot ;-)");
   // SnapshotName.setHorizontalAlignment(SwingConstants.CENTER);
   // SnapshotName.setPreferredSize(new Dimension(200, 30));
   // }
   // return SnapshotName;
   // }
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
   private JLabel getLabelParameterInfo() {
      if (labelParameterInfo == null) {
         labelParameterInfo=new JLabel("Args : ");
      }
      return labelParameterInfo;
   }
   private JLabel getLblArgs() {
      if (lblArgs == null) {
         lblArgs=new JLabel("?");
         lblArgs.setFont(FONT_INFO);
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
         tglPause=new JToggleButton("pause for maintenance");
         tglPause.addActionListener(e -> showMaintenance());
         tglPause.setPreferredSize(new Dimension(200, 30));
      }
      return tglPause;
   }
   public void showMaintenance() {
      // System.err.println("Manienance" + getTglPause().isSelected());
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
   private JLabel          lblParent;
   private JTextField      txtSnapshot;
   private JLabel          lblSnapshot;
   private JTextField      txtParent;
   private JPanel          panelLive;
   private JTextField      txtSpeed;
   private JTextField      txtWork;
   private JLabel          lblSpeed;
   private JLabel          lblWork;
   private JLabel          lblTime;
   private JTextField      txtTime;
   private JLabel          lblSize;
   private JTextField      txtSize;
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
         // panel.add(getSnapshotName());
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
   public JLabel getLblParent() {
      if (lblParent == null) {
         lblParent=new JLabel(":");
         lblParent.setVerticalAlignment(SwingConstants.BOTTOM);
      }
      return lblParent;
   }
   public JTextField getTxtSnapshot() {
      if (txtSnapshot == null) {
         txtSnapshot=new JTextField("-");
         txtSnapshot.setEditable(false);
         txtSnapshot.setFont(FONT_INFO);
      }
      return txtSnapshot;
   }
   public JLabel getLblSnapshot() {
      if (lblSnapshot == null) {
         lblSnapshot=new JLabel(":");
         lblSnapshot.setVerticalAlignment(SwingConstants.BOTTOM);
      }
      return lblSnapshot;
   }
   public JTextField getTxtParent() {
      if (txtParent == null) {
         txtParent=new JTextField("-");
         txtParent.setEditable(false);
         txtParent.setFont(FONT_INFO);
      }
      return txtParent;
   }
   private JPanel getPanelLive() {
      if (panelLive == null) {
         panelLive=new JPanel();
         panelLive.setLayout(new BorderLayout(0, 0));
         panelLive.add(getLblPv());
      }
      return panelLive;
   }
   private JTextField getTxtSpeed() {
      if (txtSpeed == null) {
         txtSpeed=new JTextField("-");
         txtSpeed.setFont(FONT_INFO);
         txtSpeed.setEditable(false);
      }
      return txtSpeed;
   }
   private JTextField getTxtWork() {
      if (txtWork == null) {
         txtWork=new JTextField("-");
         txtWork.setOpaque(true);
         txtWork.setBackground(SnapshotLabel.markInProgressColor);
         txtWork.setFont(FONT_INFO_B);
         txtWork.setEditable(false);
      }
      return txtWork;
   }
   private JLabel getLblSpeed() {
      if (lblSpeed == null) {
         lblSpeed=new JLabel("speed:");
         lblSpeed.setVerticalAlignment(SwingConstants.BOTTOM);
      }
      return lblSpeed;
   }
   private JLabel getLblWork() {
      if (lblWork == null) {
         lblWork=new JLabel("");
         lblWork.setVerticalAlignment(SwingConstants.BOTTOM);
      }
      return lblWork;
   }
   private JLabel getLblTime() {
      if (lblTime == null) {
         lblTime=new JLabel("time:");
      }
      return lblTime;
   }
   private JTextField getTxtTime() {
      if (txtTime == null) {
         txtTime=new JTextField("-");
         txtTime.setFont(FONT_INFO);
         txtTime.setEditable(false);
      }
      return txtTime;
   }
   private JLabel getLblSize() {
      if (lblSize == null) {
         lblSize=new JLabel("size:");
      }
      return lblSize;
   }
   private JTextField getTxtSize() {
      if (txtSize == null) {
         txtSize=new JTextField("-");
         txtSize.setFont(FONT_INFO);
         txtSize.setEditable(false);
      }
      return txtSize;
   }
}
