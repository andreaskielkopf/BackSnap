/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.TreeMap;

import javax.swing.*;

/**
 * @author Andreas Kielkopf
 *
 */
public class SnapshotPanel extends JPanel implements ComponentListener {
   private static final long serialVersionUID=-3405881652038164771L;
   private JPanel            panelView;
   private JPanel            panelDetail;
   private JPanel            panelVolumeName;
   private JLabel            volumeName;
   private JPanel            panel;
   private JScrollPane       scrollPane;
   public SnapshotPanel() {
      initialize();
      getPanelView().add(new SnapshotLabel(null));
      add(getPanel(), BorderLayout.CENTER);
   }
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getPanelDetail(), BorderLayout.SOUTH);
      add(getPanelVolumeName(), BorderLayout.NORTH);
      // add(getPanelView(), BorderLayout.CENTER);
   }
   private JPanel getPanelView() {
      if (panelView == null) {
         panelView=new JPanel() {
            private static final long serialVersionUID=479127839751209072L;
            // boolean reRun=false;
            // @Override
            // public void revalidate() {
            // final int w=getScrollPane().getWidth();
            // final int c=getComponentCount() * 3;
            // if (!reRun)
            // SwingUtilities.invokeLater(new Runnable() {
            // @Override
            // public void run() {
            // reRun=true;
            // pref
            // setPreferredSize(new Dimension(w, c));
            // revalidate();
            // reRun=false;
            // }
            // });
            // super.revalidate();
            // }
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
      pv.revalidate();
      for (Snapshot snapshot:tree.values()) {
         pv.add(new SnapshotLabel(snapshot));
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
