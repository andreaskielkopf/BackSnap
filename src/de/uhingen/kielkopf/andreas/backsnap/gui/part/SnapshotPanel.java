/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.part;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.*;

import de.uhingen.kielkopf.andreas.backsnap.btrfs.*;
import javax.swing.border.TitledBorder;

/**
 * @author Andreas Kielkopf
 */
public class SnapshotPanel extends JPanel implements ComponentListener, MouseListener {
   private static final long                           serialVersionUID    =-3405881652038164771L;
   private static final Font                           FONT_INFO           =new Font("Noto Sans", Font.PLAIN, 16);
   private JPanel                                      panelView;
   private SnapshotDetail                              panelDetail;
   private JPanel                                      panel;
   private JScrollPane                                 scrollPane;
   public ConcurrentSkipListMap<String, SnapshotLabel> labelTree_UUID      =new ConcurrentSkipListMap<>();
   public ConcurrentSkipListMap<String, SnapshotLabel> labelTree_ParentUuid=new ConcurrentSkipListMap<>();
   public ConcurrentSkipListMap<String, SnapshotLabel> labelTree_KeyO      =new ConcurrentSkipListMap<>();
   public ConcurrentSkipListMap<String, SnapshotLabel> labelTree_DirName   =new ConcurrentSkipListMap<>();
   public ArrayList<SnapshotLabel>                     mixedList           =new ArrayList<>();
   private TitledBorder                                tBorder             =new TitledBorder(null, "Snapshots of ...",
            TitledBorder.LEADING, TitledBorder.TOP, null, null);
   private JPanel                                      panelInfo;
   private JLabel                                      lblPc;
   private JLabel                                      lblVolume;
   private JLabel                                      lblSubvolume;
   private JLabel                                      lblMountPoint;
   private JLabel                                      infoPc;
   private JLabel                                      infoVolume;
   private JLabel                                      infoSubvolume;
   private JLabel                                      infoMountPoint;
   public SnapshotPanel() {
      setBorder(tBorder);
      initialize();
   }
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getPanelDetail(), BorderLayout.SOUTH);
      // add(getPanelVolumeName(), BorderLayout.NORTH);
      getPanelView().add(new SnapshotLabel(null));
      add(getPanel(), BorderLayout.CENTER);
      add(getPanelInfo(), BorderLayout.NORTH);
   }
   public JPanel getPanelView() {
      if (panelView == null) {
         panelView=new JPanel() {
            BasicStroke               stroke          =new BasicStroke(3f, BasicStroke.CAP_ROUND,
                     BasicStroke.JOIN_ROUND);
            private static final long serialVersionUID=-8623737829256524456L;
            @Override
            public void paint(Graphics g) {
               if (g instanceof Graphics2D g2d) {
                  super.paint(g);
                  g2d.setColor(Color.BLUE);
                  g2d.setStroke(stroke);
                  int                               w        =getWidth();
                  Set<Entry<String, SnapshotLabel>> labelTree=labelTree_UUID.entrySet();
                  for (Entry<String, SnapshotLabel> entry:labelTree) {
                     SnapshotLabel snap       =entry.getValue();
                     String        parent_uuid=snap.snapshot.parent_uuid();
                     SnapshotLabel parent     =labelTree_UUID.get(parent_uuid);
                     if (parent != null) {
                        Rectangle snapBounds  =snap.getBounds();
                        Rectangle parentBounds=parent.getBounds();
                        double    abstand     =parentBounds.getMaxX() - snapBounds.getMinX();
                        boolean   t           =(abstand + snapBounds.getWidth() * 2 > w);
                        boolean   m           =(Math.abs(abstand) < snapBounds.getWidth());
                        if (t) {
                           g2d.setColor(Color.BLACK);
                           int y=(int) ((parentBounds.getCenterY() + snapBounds.getCenterY()) / 2);
                           g2d.drawLine((int) parentBounds.getMaxX(), (int) parentBounds.getCenterY(), w, y);
                           g2d.drawLine(0, y, (int) snapBounds.getMinX(), (int) snapBounds.getCenterY());
                        } else
                           if (m) {
                              g2d.setColor(Color.BLACK);
                              g2d.drawLine((int) parentBounds.getMaxX(), (int) parentBounds.getCenterY(),
                                       (int) snapBounds.getMinX(), (int) snapBounds.getCenterY());
                           } else {
                              g2d.setColor(Color.BLUE.darker());
                              g2d.fillOval((int) parentBounds.getMaxX() - 10, (int) parentBounds.getMaxY() - 10, 10,
                                       10);
                              g2d.drawLine((int) parentBounds.getMaxX(), (int) parentBounds.getMaxY() - 5,
                                       (int) snapBounds.getMinX(), (int) snapBounds.getMinY() + 5);
                              g2d.drawOval((int) snapBounds.getMinX(), (int) snapBounds.getMinY(), 10, 10);
                           }
                     }
                  }
               }
            }
            @Override
            protected void paintComponent(Graphics g) {
               if (g instanceof Graphics2D g2d) {
                  super.paintComponent(g);
                  g2d.setColor(Color.GREEN);
                  g2d.drawLine(10, 10, 500, 500);
               }
            }
         };
         FlowLayout flowLayout=(FlowLayout) panelView.getLayout();
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
    */
   public ConcurrentSkipListMap<String, Snapshot> setVolume(Mount subVolume, Collection<Snapshot> list) {
      ConcurrentSkipListMap<String, Snapshot> neuList=new ConcurrentSkipListMap<>();
      for (Snapshot snap:list)
         if (!labelTree_UUID.containsKey(snap.uuid()))
            neuList.put(snap.keyO(), snap);
      labelTree_UUID.clear();
      labelTree_ParentUuid.clear();
      labelTree_KeyO.clear();
      labelTree_DirName.clear();
      synchronized (mixedList) {
         boolean doShuffle=(mixedList.size() < list.size() / 2);
         JPanel  pv       =getPanelView();
         pv.removeAll(); // alle Labels entfernen
         for (Snapshot snapshot:list) {
            SnapshotLabel snapshotLabel=SnapshotLabel.getSnapshotLabel(snapshot);// gespeichertes Label holen
            snapshotLabel.addMouseListener(this);
            labelTree_UUID.put(snapshot.uuid(), snapshotLabel);// nach UUID sortiert
            labelTree_ParentUuid.put(snapshot.parent_uuid(), snapshotLabel);// parent sortiert (keine doppelten !)
            labelTree_KeyO.put(snapshot.keyO(), snapshotLabel);// nach Key sortiert (keine doppelten !)
            labelTree_DirName.put(snapshot.dirName(), snapshotLabel);// nach Key sortiert (keine doppelten !)
            if (!mixedList.contains(snapshotLabel))
               mixedList.add(snapshotLabel);
            pv.add(snapshotLabel);
         }
         pv.revalidate();
         if (doShuffle)
            Collections.shuffle(mixedList);
      }
      componentResized(null);
      repaint(100);
      return neuList;
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.addComponentListener(this);
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getScrollPane(), BorderLayout.CENTER);
      }
      return panel;
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
      long         v =pv.getComponentCount() * 2000L;
      final int    c =(int) (v / w);
      SwingUtilities.invokeLater(new Runnable() {
         @Override
         public void run() {
            pv.setPreferredSize(new Dimension(w, c));
            pv.revalidate();
         }
      });
   }
   @Override
   public void componentMoved(ComponentEvent e) { /* noop */ }
   @Override
   public void componentShown(ComponentEvent e) { /* noop */ }
   @Override
   public void componentHidden(ComponentEvent e) { /* noop */ }
   /**
    * @return
    */
   public ConcurrentSkipListMap<String, SnapshotLabel> getLabels() {
      return labelTree_KeyO;
   }
   @Override
   public void mouseClicked(MouseEvent e) { /* noop */ }
   @Override
   public void mousePressed(MouseEvent e) { /* noop */ }
   @Override
   public void mouseReleased(MouseEvent e) { /* noop */ }
   @Override
   public void mouseEntered(MouseEvent e) {
      if (e.getSource() instanceof SnapshotLabel sl) {
         Snapshot       sn=sl.snapshot;
         SnapshotDetail pd=getPanelDetail();
         pd.setInfo("Snapshot " + sn.dirName() + ":", sn.getInfo());
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
         FlowLayout fl_panelInfo = new FlowLayout(FlowLayout.LEFT, 5, 5);
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
   private JLabel getLblPc() {
      if (lblPc == null) {
         lblPc=new JLabel("Pc:");
      }
      return lblPc;
   }
   private JLabel getLblVolume() {
      if (lblVolume == null) {
         lblVolume=new JLabel("   Volume:");
      }
      return lblVolume;
   }
   private JLabel getLblSubvolume() {
      if (lblSubvolume == null) {
         lblSubvolume=new JLabel("   Subvolume:");
      }
      return lblSubvolume;
   }
   private JLabel getLblMountPoint() {
      if (lblMountPoint == null) {
         lblMountPoint=new JLabel("   mounted as: ");
      }
      return lblMountPoint;
   }
   public JLabel getInfoPc() {
      if (infoPc == null) {
         infoPc=new JLabel("local");
         infoPc.setFont(FONT_INFO);
         infoPc.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return infoPc;
   }
   public JLabel getInfoVolume() {
      if (infoVolume == null) {
         infoVolume=new JLabel("/dev/sdz");
         infoVolume.setFont(FONT_INFO);
         infoVolume.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return infoVolume;
   }
   public JLabel getInfoSubvolume() {
      if (infoSubvolume == null) {
         infoSubvolume=new JLabel("/@");
         infoSubvolume.setFont(FONT_INFO);
         infoSubvolume.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return infoSubvolume;
   }
   public JLabel getInfoMountPoint() {
      if (infoMountPoint == null) {
         infoMountPoint=new JLabel("/");
         infoMountPoint.setFont(FONT_INFO);
         infoMountPoint.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return infoMountPoint;
   }
   /**
    * @param srcConfig
    */
   public void setInfo(Mount mount) {
      getInfoPc().setText(mount.pc().extern());
      getInfoVolume().setText(mount.devicePath().toString());
      getInfoSubvolume().setText(mount.btrfsPath().toString());
      getInfoMountPoint().setText(mount.mountPath().toString());
      repaint(100);
   }
  
}
