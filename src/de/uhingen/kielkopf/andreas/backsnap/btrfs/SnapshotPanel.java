/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.*;

/**
 * @author Andreas Kielkopf
 *
 */
public class SnapshotPanel extends JPanel implements ComponentListener {
   private static final long             serialVersionUID=-3405881652038164771L;
   private JPanel                        panelView;
   private JPanel                        panelDetail;
   private JPanel                        panelVolumeName;
   private JLabel                        volumeName;
   private JPanel                        panel;
   private JScrollPane                   scrollPane;
   public ConcurrentSkipListMap<String, SnapshotLabel> labelTree_UUID  =new ConcurrentSkipListMap<>();
   public ConcurrentSkipListMap<String, SnapshotLabel> labelTree_Parent=new ConcurrentSkipListMap<>();
   public TreeMap<String, Snapshot>      sTree;
   public SnapshotPanel() {
      initialize();
      getPanelView().add(new SnapshotLabel(null));
      add(getPanel(), BorderLayout.CENTER);
   }
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getPanelDetail(), BorderLayout.SOUTH);
      add(getPanelVolumeName(), BorderLayout.NORTH);
   }
   private JPanel getPanelView() {
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
                  // TreeMap<String, Snapshot> tr=sTree;
                  int                               w =getWidth();
                  Set<Entry<String, SnapshotLabel>> lt=labelTree_UUID.entrySet();
                  for (Entry<String, SnapshotLabel> en:lt) {
                     SnapshotLabel sn         =en.getValue();
                     // Snapshot s =sn.snapshot;
                     String        parent_uuid=sn.snapshot.parent_uuid();
                     SnapshotLabel parent     =labelTree_UUID.get(parent_uuid);
                     if (parent != null) {
                        Rectangle sb  =sn.getBounds();
                        Rectangle pb  =parent.getBounds();
                        double    abst=pb.getMaxX() - sb.getMinX();
                        boolean   t   =(abst + sb.getWidth() > w);
                        boolean   m   =(Math.abs(abst) < sb.getWidth());
                        if (t) {
                           g2d.setColor(Color.BLACK);
                           int y=(int) ((pb.getCenterY() + sb.getCenterY()) / 2);
                           g2d.drawLine((int) pb.getMaxX(), (int) pb.getCenterY(), w, y);
                           g2d.drawLine(0, y, (int) sb.getMinX(), (int) sb.getCenterY());
                        } else
                           if (m) {
                              g2d.setColor(Color.BLACK);
                              g2d.drawLine((int) pb.getMaxX(), (int) pb.getCenterY(), (int) sb.getMinX(),
                                       (int) sb.getCenterY());
                           } else {
                              g2d.setColor(Color.BLUE.darker());
                              g2d.fillOval((int) pb.getMaxX() - 10, (int) pb.getMaxY() - 10, 10, 10);
                              g2d.drawLine((int) pb.getMaxX(), (int) pb.getMaxY() - 5, (int) sb.getMinX(),
                                       (int) sb.getMinY() + 5);
                              g2d.drawOval((int) sb.getMinX(), (int) sb.getMinY(), 10, 10);
                           }
                     }
                  }
               }
            }
            @Override
            protected void paintComponent(Graphics g) {
               if (g instanceof Graphics2D g2d) {
                  // g2d.setColor(Color.BLUE);
                  // g2d.drawLine(10, 10, 1000, 1000);
                  super.paintComponent(g);
                  g2d.setColor(Color.GREEN);
                  g2d.drawLine(10, 10, 500, 500);
               }
            }
         };
      }
      return panelView;
   }
   private JPanel getPanelDetail() {
      if (panelDetail == null) {
         panelDetail=new SnapshotDetail();
      }
      return panelDetail;
   }
   private JPanel getPanelVolumeName() {
      if (panelVolumeName == null) {
         panelVolumeName=new JPanel();
         panelVolumeName.setLayout(new BorderLayout(0, 0));
         panelVolumeName.add(getVolumeName(), BorderLayout.NORTH);
      }
      return panelVolumeName;
   }
   /**
    * @param receivedSnapshots
    * @param srcVolume
    */
   public void setVolume(Subvolume subVolume, TreeMap<String, Snapshot> tree) {
      String        extern=subVolume.extern();
      String        mount =subVolume.mountPoint();
      String        device=subVolume.device();
      StringBuilder sb    =new StringBuilder(mount).append("(").append(device).append(")");
      if (!extern.isBlank())
         sb.insert(0, ":").insert(0, extern);
      getVolumeName().setText(sb.toString());
      repaint();
      JPanel pv=getPanelView();
      pv.removeAll();
      labelTree_UUID.clear();
      labelTree_Parent.clear();
      sTree=tree;
      pv.revalidate();
      for (Snapshot snapshot:tree.values()) {
         SnapshotLabel snapshotLabel=new SnapshotLabel(snapshot);
         labelTree_UUID.put(snapshot.uuid(), snapshotLabel);
         labelTree_Parent.put(snapshot.parent_uuid(), snapshotLabel);
         pv.add(snapshotLabel);
         pv.revalidate();
      }
      componentResized(null);
   }
   private JLabel getVolumeName() {
      if (volumeName == null) {
         volumeName=new JLabel("Vname");
         volumeName.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return volumeName;
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
      final JPanel pv=getPanelView();
      final int    w =getScrollPane().getWidth();
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
   public void componentMoved(ComponentEvent e) {/* */ }
   @Override
   public void componentShown(ComponentEvent e) {/* */ }
   @Override
   public void componentHidden(ComponentEvent e) {/* */ }
}
