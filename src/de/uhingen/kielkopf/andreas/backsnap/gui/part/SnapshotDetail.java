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
   private static final long serialVersionUID=-7659329578660282348L;
   private JPanel            panel;
   private JLabel            lblNewLabel;
   private JLabel            lblNewLabel_1;
   private JPanel            panel_1;
   private JPanel            panelInfo;
   private TitledBorder      tBorder;
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
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setLayout(new GridLayout(0, 2, 10, 0));
         panel.add(getLblNewLabel());
         panel.add(getLblNewLabel_1());
      }
      return panel;
   }
   private JLabel getLblNewLabel() {
      if (lblNewLabel == null) {
         lblNewLabel=new JLabel("name:");
         lblNewLabel.setFont(new Font("Noto Sans", Font.BOLD, 12));
         lblNewLabel.setHorizontalAlignment(SwingConstants.TRAILING);
      }
      return lblNewLabel;
   }
   private JLabel getLblNewLabel_1() {
      if (lblNewLabel_1 == null) {
         lblNewLabel_1=new JLabel("snapshot 5");
      }
      return lblNewLabel_1;
   }
   private JPanel getPanel_1() {
      if (panel_1 == null) {
         panel_1=new JPanel();
         panel_1.setBorder(getTBorder());
         panel_1.setLayout(new BorderLayout(0, 0));
         panel_1.add(getPanel(), BorderLayout.NORTH);
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
      return tBorder.getTitle();
   }
   public void setTitle(String title) {
      tBorder.setTitle(title);
   }
   private JPanel getPanelInfo() {
      if (panelInfo == null) {
         panelInfo=new JPanel();
         panelInfo.setLayout(new GridLayout(0, 2, 0, 0));
      }
      return panelInfo;
   }
   public void setInfo(String title, Stream<Entry<String, String>> infoStream) {
      setTitle(title);
      JPanel pi=getPanelInfo();
      pi.removeAll();
      infoStream.forEachOrdered(e -> {
         System.out.println(e);
         JLabel lk=new JLabel(e.getKey() + ":");
         pi.add(lk);
         JLabel lv=new JLabel(e.getValue());
         pi.add(lv);
      });
      pi.revalidate();
      pi.repaint(100);
   }
}
