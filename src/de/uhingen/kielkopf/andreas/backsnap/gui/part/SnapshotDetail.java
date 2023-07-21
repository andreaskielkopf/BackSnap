/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.part;

import java.awt.*;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.swing.*;

import javax.swing.border.TitledBorder;

/**
 * @author Andreas Kielkopf
 *
 */
public class SnapshotDetail extends JPanel {
   static private final long serialVersionUID=-7659329578660282348L;
   private JPanel            panel_1;
   private JPanel            panelLbl;
   private TitledBorder      tBorder;
   private JPanel            panelInfo;
   public SnapshotDetail() {
      initialize();
   }
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getPanel_1());
      Map<String, String> infoMap=new TreeMap<>();
      infoMap.put("uuid", "1");
      infoMap.put("parent_uuid", "2");
      setInfo("Info:", infoMap.entrySet().parallelStream());
   }
   private JPanel getPanel_1() {
      if (panel_1 == null) {
         panel_1=new JPanel();
         panel_1.setBorder(getTBorder());
         panel_1.setLayout(new BorderLayout(0, 0));
         panel_1.add(getPanelLbl(), BorderLayout.WEST);
         panel_1.add(getPanelInfo(), BorderLayout.CENTER);
      }
      return panel_1;
   }
   private TitledBorder getTBorder() {
      if (tBorder == null)
         tBorder=new TitledBorder(null, "titel:", TitledBorder.LEADING, TitledBorder.TOP, null, null);
      return tBorder;
   }
   public String getTitle() {
      return getTBorder().getTitle();
   }
   public void setTitle(String title) {
      tBorder.setTitle(title);
   }
   private JPanel getPanelLbl() {
      if (panelLbl == null) {
         panelLbl=new JPanel();
         panelLbl.setLayout(new GridLayout(0, 1, 0, 0));
      }
      return panelLbl;
   }
   public void setInfo(String title, Stream<Entry<String, String>> infoStream) {
      setTitle(title);
      JPanel pLbl =getPanelLbl();
      JPanel pInfo=getPanelInfo();
      pLbl.removeAll();
      pInfo.removeAll();
      infoStream.forEachOrdered(e -> {
         JLabel label=new JLabel(e.getKey());
         label.setHorizontalAlignment(SwingConstants.TRAILING);
         pLbl.add(label);
         pInfo.add(new JLabel(e.getValue()));
      });
      revalidate();
      repaint(50);
   }
   private JPanel getPanelInfo() {
      if (panelInfo == null) {
         panelInfo=new JPanel();
         panelInfo.setLayout(new GridLayout(0, 1, 0, 0));
      }
      return panelInfo;
   }
}
