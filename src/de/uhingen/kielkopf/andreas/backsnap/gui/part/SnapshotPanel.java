/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.part;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import de.uhingen.kielkopf.andreas.backsnap.btrfs.Mount;
import de.uhingen.kielkopf.andreas.backsnap.btrfs.Snapshot;
import de.uhingen.kielkopf.andreas.backsnap.gui.element.Lbl;
import de.uhingen.kielkopf.andreas.backsnap.gui.element.TxtFeld;

/**
 * @author Andreas Kielkopf
 */
public class SnapshotPanel extends JPanel implements ComponentListener, MouseListener {
   static private final long                           serialVersionUID    =-3405881652038164771L;
   static public final Font                            FONT_INFO           =new Font("Noto Sans", Font.PLAIN, 16);
   static public final Font                            FONT_INFO_B         =new Font("Noto Sans", Font.BOLD, 16);
   private JPanel                                      panelView;
   private SnapshotDetail                              panelDetail;
   private JPanel                                      panelSnapshots;
   private JScrollPane                                 scrollPane;
   public ConcurrentSkipListMap<String, SnapshotLabel> labelTree_UUID      =new ConcurrentSkipListMap<>();
   public ConcurrentSkipListMap<String, SnapshotLabel> labelTree_R_UUID    =new ConcurrentSkipListMap<>();
   public ConcurrentSkipListMap<String, SnapshotLabel> labelTree_ParentUuid=new ConcurrentSkipListMap<>();
   public ConcurrentSkipListMap<String, SnapshotLabel> labelTree_KeyO      =new ConcurrentSkipListMap<>();
   public ConcurrentSkipListMap<String, SnapshotLabel> labelTree_DirNameS  =new ConcurrentSkipListMap<>();
   public ArrayList<SnapshotLabel>                     mixedList           =new ArrayList<>();
   private TitledBorder                                tBorder             =new TitledBorder(null, "Snapshots of ...",
            TitledBorder.LEADING, TitledBorder.TOP, null, null);
   private JPanel                                      panelInfo;
   private Lbl                                         lblPc;
   private Lbl                                         lblVolume;
   private Lbl                                         lblSubvolume;
   private Lbl                                         lblMountPoint;
   private TxtFeld                                     infoPc;
   private TxtFeld                                     infoVolume;
   private TxtFeld                                     infoSubvolume;
   private TxtFeld                                     infoMountPoint;
   private JSplitPane                                  splitPane;
   public SnapshotPanel() throws IOException {
      setBorder(tBorder);
      initialize();
   }
   private void initialize() throws IOException {
      setLayout(new BorderLayout(0, 0));
      add(getPanelInfo(), BorderLayout.NORTH);
      getPanelView().add(SnapshotLabel.getSnapshotLabel(null));
      add(getSplitPane(), BorderLayout.CENTER);
      setMinimumSize(new Dimension(200, 200));
   }
   public JPanel getPanelView() {
      if (panelView == null) {
         panelView=new JPanel() {
            BasicStroke               stroke          =new BasicStroke(1.5f, BasicStroke.CAP_ROUND,
                     BasicStroke.JOIN_ROUND);
            static private final long serialVersionUID=-8623737829256524456L;
            @Override
            public void paint(Graphics g) {
               final int bolla2=6;
               final int bolla1=bolla2 / 2;
               if (g instanceof Graphics2D g2d) {
                  g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                  super.paint(g);
                  g2d.setColor(Color.BLUE);
                  g2d.setStroke(stroke);
                  int w=getWidth();
                  for (SnapshotLabel snap:labelTree_UUID.values())
                     if (labelTree_UUID.get(snap.snapshot.parent_uuid()) instanceof SnapshotLabel parent) {
                        Rectangle s=snap.getBounds();
                        Rectangle p=parent.getBounds();
                        int px=p.x;
                        int pw=p.width;
                        int py=p.y;
                        int ph=p.height;
                        int sx=s.x;
                        int sy=s.y;
                        int sh=s.height;
                        int abstand=px + pw - sx;
                        boolean t=(abstand + s.getWidth() * 2 > w);
                        boolean m=(Math.abs(abstand) < s.getWidth());
                        if (t) {
                           g2d.setColor(Color.BLACK);
                           int y=(int) ((p.getCenterY() + s.getCenterY()) / 2);
                           g2d.drawLine(px + pw, py + ph / 2, w, y);
                           g2d.drawLine(0, y, sx, sy + sh / 2);
                        } else
                           if (m) {
                              g2d.setColor(Color.BLACK);
                              g2d.drawLine(px + pw, py + ph / 2, sx, sy + sh / 2);
                           } else {
                              g2d.setColor(Color.BLUE);
                              g2d.fillOval(px + pw - bolla2 - 2, py + ph - bolla2 - 2, bolla2, bolla2);
                              g2d.drawLine(px + pw - bolla1 - 2, py + ph - bolla1 - 2, sx + bolla1 + 2,
                                       sy + bolla1 + 2);
                              g2d.drawOval(sx + 2, sy + 2, bolla2, bolla2);
                           }
                     }
               }
            }
         };
         FlowLayout flowLayout=(FlowLayout) panelView.getLayout();
         flowLayout.setVgap(3);
         flowLayout.setHgap(3);
         flowLayout.setAlignment(FlowLayout.LEFT);
      }
      return panelView;
   }
   private SnapshotDetail getPanelDetail() {
      if (panelDetail == null) {
         panelDetail=new SnapshotDetail();
      }
      return panelDetail;
   }
   /**
    * @param receivedSnapshots
    * @param srcVolume
    * @return
    * @throws IOException
    */
   public ConcurrentSkipListMap<String, Snapshot> setVolume(Collection<Snapshot> list) throws IOException {
      ConcurrentSkipListMap<String, Snapshot> neuList=new ConcurrentSkipListMap<>();
      for (Snapshot snap:list)
         if (!labelTree_UUID.containsKey(snap.uuid()))
            neuList.put(snap.keyB(), snap); // mit sortierter Reihenfolge für die Labels
      labelTree_UUID.clear();
      labelTree_ParentUuid.clear();
      labelTree_KeyO.clear();
      labelTree_DirNameS.clear();
      final ArrayList<SnapshotLabel> pvList=new ArrayList<>();
      List<Component> aktuell;
      final JPanel pv=getPanelView();
      synchronized (mixedList) {
         boolean doShuffle=(mixedList.size() < list.size() / 2);
         aktuell=Arrays.asList(pv.getComponents());
         for (Snapshot snapshot:list) {
            SnapshotLabel snapshotLabel=SnapshotLabel.getSnapshotLabel(snapshot);// gespeichertes Label holen
            snapshotLabel.addMouseListener(this);
            labelTree_UUID.put(snapshot.uuid(), snapshotLabel);// nach UUID sortiert
            if (snapshot.isBackup())
               labelTree_R_UUID.put(snapshot.received_uuid(), snapshotLabel);
            labelTree_ParentUuid.put(snapshot.parent_uuid(), snapshotLabel);// parent sortiert (keine doppelten !)
            labelTree_KeyO.put(snapshot.keyO(), snapshotLabel);// nach Key sortiert (keine doppelten !)
            labelTree_DirNameS.put(snapshot.sortableDirname(), snapshotLabel);// nach Key sortiert (keine doppelten !)
            if (!mixedList.contains(snapshotLabel))
               mixedList.add(snapshotLabel);
            pvList.add(snapshotLabel);
         }
         if (doShuffle)
            Collections.shuffle(mixedList);
      }
      for (Component c:aktuell) // nur entfernen, was weg muss
         if (c instanceof SnapshotLabel sl)
            if (!pvList.contains(sl))
               SwingUtilities.invokeLater(() -> pv.remove(sl));
      for (SnapshotLabel sl:pvList) // neue hinzufügen
         if (!aktuell.contains(sl))
            SwingUtilities.invokeLater(() -> pv.add(sl));
      SwingUtilities.invokeLater(() -> {
         pv.revalidate();
         pv.repaint(100);
         componentResized(null);
         repaint(100);
      });
      return neuList;
   }
   private JPanel getPanelSnapshots() {
      if (panelSnapshots == null) {
         panelSnapshots=new JPanel();
         panelSnapshots.addComponentListener(this);
         panelSnapshots.setLayout(new BorderLayout(0, 0));
         panelSnapshots.add(getScrollPane(), BorderLayout.CENTER);
      }
      return panelSnapshots;
   }
   private JScrollPane getScrollPane() {
      if (scrollPane == null) {
         scrollPane=new JScrollPane(getPanelView());
         scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
         scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
         scrollPane.getVerticalScrollBar().setUnitIncrement(10);
      }
      return scrollPane;
   }
   @Override
   public void componentResized(ComponentEvent e) {
      final int w=getScrollPane().getWidth();
      if (w == 0)
         return;
      final JPanel pv=getPanelView();
      long v=pv.getComponentCount() * 2000L;
      final int c=(int) (v / w);
      SwingUtilities.invokeLater(() -> {
         pv.setPreferredSize(new Dimension(w, c));
         pv.revalidate();
      });
   }
   /**
    * @return
    */
   public ConcurrentSkipListMap<String, SnapshotLabel> getLabels() {
      return labelTree_KeyO;
   }
   @Override
   public void componentMoved(ComponentEvent e) { /* noop */ }
   @Override
   public void componentShown(ComponentEvent e) { /* noop */ }
   @Override
   public void componentHidden(ComponentEvent e) { /* noop */ }
   @Override
   public void mouseClicked(MouseEvent e) { /* noop */ }
   @Override
   public void mousePressed(MouseEvent e) { /* noop */ }
   @Override
   public void mouseReleased(MouseEvent e) { /* noop */ }
   @Override
   public void mouseEntered(MouseEvent e) {
      if (e.getSource() instanceof SnapshotLabel sl) {
         Snapshot sn=sl.snapshot;
         SnapshotDetail pd=getPanelDetail();
         JSplitPane sp=getSplitPane();
         pd.setInfo("Snapshot " + sn.dirName() + ":", sn.getInfo());
         if (sp.getBottomComponent() == null) {
            pd.setMinimumSize(new Dimension(300, 160));
            sp.setBottomComponent(pd);
            sp.setDividerLocation(0.85d);
         }
      }
   }
   @Override
   public void mouseExited(MouseEvent e) { /* noop */ }
   /**
    * @param string
    */
   public void setTitle(String string) {
      tBorder.setTitle(string);
   }
   private JPanel getPanelInfo() {
      if (panelInfo == null) {
         panelInfo=new JPanel();
         FlowLayout fl_panelInfo=new FlowLayout(FlowLayout.LEFT, 2, 1);
         fl_panelInfo.setAlignOnBaseline(true);
         panelInfo.setLayout(fl_panelInfo);
         panelInfo.add(getLblPc());
         panelInfo.add(getInfoPc());
         panelInfo.add(getLblVolume());
         panelInfo.add(getInfoVolume());
         panelInfo.add(getLblSubvolume());
         panelInfo.add(getInfoSubvolume());
         panelInfo.add(getLblMountPoint());
         panelInfo.add(getInfoMountPoint());
      }
      return panelInfo;
   }
   private Lbl getLblPc() {
      if (lblPc == null) {
         lblPc=new Lbl("Pc:");
      }
      return lblPc;
   }
   private Lbl getLblVolume() {
      if (lblVolume == null) {
         lblVolume=new Lbl("Vol:");
      }
      return lblVolume;
   }
   private Lbl getLblSubvolume() {
      if (lblSubvolume == null) {
         lblSubvolume=new Lbl("Subvol:");
      }
      return lblSubvolume;
   }
   private Lbl getLblMountPoint() {
      if (lblMountPoint == null) {
         lblMountPoint=new Lbl("at:");
      }
      return lblMountPoint;
   }
   public TxtFeld getInfoPc() {
      if (infoPc == null) {
         infoPc=new TxtFeld("local");
      }
      return infoPc;
   }
   public TxtFeld getInfoVolume() {
      if (infoVolume == null) {
         infoVolume=new TxtFeld("/dev/sdz");
      }
      return infoVolume;
   }
   public TxtFeld getInfoSubvolume() {
      if (infoSubvolume == null) {
         infoSubvolume=new TxtFeld("/@");
      }
      return infoSubvolume;
   }
   public TxtFeld getInfoMountPoint() {
      if (infoMountPoint == null) {
         infoMountPoint=new TxtFeld("/");
      }
      return infoMountPoint;
   }
   /**
    * @param srcConfig
    */
   public void setInfo(Mount mount) {
      SwingUtilities.invokeLater(() -> {
         getInfoPc().setText(mount.pc().extern());
         getInfoVolume().setText(mount.devicePath().toString());
         getInfoSubvolume().setText(mount.btrfsPath().toString());
         getInfoMountPoint().setText(mount.mountPath().toString());
         getPanelInfo().revalidate();
         getPanelInfo().repaint(100);
      });
   }
   private JSplitPane getSplitPane() {
      if (splitPane == null) {
         // Das 2.Panel erst eintragen, wenn es dargestellt werden muss !
         splitPane=new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, getPanelSnapshots(), null);
         splitPane.setOneTouchExpandable(true);
         splitPane.setResizeWeight(1d);
         splitPane.setDividerSize((splitPane.getDividerSize() * 3) / 2);
      }
      return splitPane;
   }
}
