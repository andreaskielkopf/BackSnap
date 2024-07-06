/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.gui2;

import java.awt.*;
import java.io.IOException;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.TitledBorder; 

/**
 * @author Andreas Kielkopf
 *
 */
public class PanelTab extends JPanel {
   private static final long serialVersionUID=1L;
   private PanelCleanup      panelCleanup;
   private JSplitPane        splitPane;
   private SnapshotPanel2    panel_1;
   private SnapshotPanel2    panel_2;
   private JPanel            panelBackup;
   /**
    * Create the panel.
    */
   public PanelTab() {
      initialize();
   }
   private void initialize() {
      setBackground(Color.CYAN);
      setLayout(new BorderLayout(0, 0));
      add(getPanelCleanup(), BorderLayout.NORTH);
      // add(getPanel_2(), BorderLayout.WEST);
      // add(getPanel_1(), BorderLayout.NORTH);
      try {
         add(getPanelBackup(), BorderLayout.CENTER);
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }
   private PanelCleanup getPanelCleanup() {
      if (panelCleanup == null) {
         panelCleanup=new PanelCleanup();
         panelCleanup.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
      }
      return panelCleanup;
   }
   private JSplitPane getSplitPane() throws IOException {
      if (splitPane == null) {
         splitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, getPanel_1(), getPanel_2());
         splitPane.setOneTouchExpandable(true);
         splitPane.setDividerLocation(150);
      }
      return splitPane;
   }
   private SnapshotPanel2 getPanel_1() throws IOException {
      if (panel_1 == null) {
         panel_1=new SnapshotPanel2();
         panel_1.setMinimumSize(new Dimension(100, 50));
         panel_1.setTitle("Snapshots");
         // panel_1.setLayout(new BorderLayout(0, 0));
      }
      return panel_1;
   }
   private SnapshotPanel2 getPanel_2() throws IOException {
      if (panel_2 == null) {
         panel_2=new SnapshotPanel2();
         panel_2.setMinimumSize(new Dimension(100, 50));
         panel_2.setTitle("Backups");
         // panel_2.setLayout(new BorderLayout(0, 0));
      }
      return panel_2;
   }
   private JPanel getPanelBackup() throws IOException {
      if (panelBackup == null) {
         panelBackup=new JPanel();
         panelBackup.setBackground(Color.CYAN);
//         panelBackup.setBorder(new TitledBorder(null, "Backup", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panelBackup.setLayout(new BorderLayout(0, 0));
         panelBackup.add(getSplitPane(), BorderLayout.CENTER);
      }
      return panelBackup;
   }
}
