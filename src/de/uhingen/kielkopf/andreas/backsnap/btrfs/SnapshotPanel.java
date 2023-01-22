/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.*;

/**
 * @author Andreas Kielkopf
 *
 */
public class SnapshotPanel extends JPanel implements ComponentListener {
   private static final long                           serialVersionUID    =-3405881652038164771L;
   private JPanel                                      panelView;
   private JPanel                                      panelDetail;
   private JPanel                                      panelVolumeName;
   private JLabel                                      volumeName;
   private JPanel                                      panel;
   private JScrollPane                                 scrollPane;
   public ConcurrentSkipListMap<String, SnapshotLabel> labelTree_UUID      =new ConcurrentSkipListMap<>();
   public ConcurrentSkipListMap<String, SnapshotLabel> labelTree_ParentUuid=new ConcurrentSkipListMap<>();
   public ConcurrentSkipListMap<String, SnapshotLabel> labelTree_Key       =new ConcurrentSkipListMap<>();
   public ConcurrentSkipListMap<String, SnapshotLabel> labelTree_DirName   =new ConcurrentSkipListMap<>();
   public ArrayList<SnapshotLabel>                     mixedList           =new ArrayList<>();
   public ConcurrentSkipListMap<String, Object>        sTree;
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
   public void setVolume(Mount subVolume, ConcurrentSkipListMap<String, Object> tree) {
      String        extern=subVolume.mountList().extern();
      String        mount =subVolume.mountPoint();
      String        device=subVolume.device();
      StringBuilder sb    =new StringBuilder(mount).append(" (on ").append(device).append(")");
      if (!extern.isBlank())
         sb.insert(0, " : ").insert(0, extern);
      getVolumeName().setText(sb.toString());
      repaint(100);
      labelTree_UUID.clear();
      labelTree_ParentUuid.clear();
      labelTree_Key.clear();
      labelTree_DirName.clear();
      ConcurrentSkipListMap<String, Snapshot> sortedTree=new ConcurrentSkipListMap<>();
      for (Object o:tree.values())
         if (o instanceof Snapshot snapshot)
            sortedTree.put(snapshot.key(), snapshot);// sortierbare Nummern bis 8 Stellen
      synchronized (mixedList) {
         boolean doShuffle=(mixedList.size() < tree.size() / 2);
         JPanel  pv       =getPanelView();
         pv.removeAll(); // alle Labels entfernen
         pv.revalidate();
         for (Snapshot snapshot:sortedTree.values()) {
            SnapshotLabel snapshotLabel=SnapshotLabel.getSnapshotLabel(snapshot);// gespeichertes Label holen
            labelTree_UUID.put(snapshot.uuid(), snapshotLabel);// nach UUID sortiert
            labelTree_ParentUuid.put(snapshot.parent_uuid(), snapshotLabel);// parent sortiert (keine doppelten !)
            labelTree_Key.put(snapshot.key(), snapshotLabel);// nach Key sortiert (keine doppelten !)
            labelTree_DirName.put(snapshot.dirName(), snapshotLabel);// nach Key sortiert (keine doppelten !)
            if (!mixedList.contains(snapshotLabel))
               mixedList.add(snapshotLabel);
            pv.add(snapshotLabel);
            pv.revalidate();
         }
         if (doShuffle)
            Collections.shuffle(mixedList);
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
