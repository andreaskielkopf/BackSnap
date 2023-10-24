/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import de.uhingen.kielkopf.andreas.backsnap.btrfs.Mount;

/**
 * @author Andreas Kielkopf
 *
 */
public class Subvol extends JPanel {
   static private final long serialVersionUID=-4206237186411548874L;
   private JPanel            panel;
   private JTextField        svName;
   private JPanel            panel_1;
   private JPanel            panel_3;
   private JPanel            panel_2;
   private Mount             mount;
   private JLabel            mountpoint;
   private JLabel            device;
   private JPanel            panel_4;
   private JPanel            panel_5;
   private JLabel            subvolume;
   private JLabel            count;
   private JLabel            options;
   private String            label;
   /**
    * Create the panel.
    */
   public Subvol() {
      initialize();
   }
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getPanel(), BorderLayout.NORTH);
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setBorder(new TitledBorder(null, "name on backup", TitledBorder.LEADING, TitledBorder.TOP, null,
                  new Color(59, 59, 59)));
         panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
         panel.add(getPanel_1());
         panel.add(getPanel_2());
         panel.add(getPanel_3());
         panel.add(getPanel_4());
         panel.add(getPanel_5());
      }
      return panel;
   }
   private JTextField getSvName() {
      if (svName == null) {
         svName=new JTextField();
         svName.setEnabled(false);
         svName.setText("manjaro18");
         svName.setColumns(10);
      }
      return svName;
   }
   private JPanel getPanel_1() {
      if (panel_1 == null) {
         panel_1=new JPanel();
         panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));
         panel_1.add(getSvName());
      }
      return panel_1;
   }
   private JPanel getPanel_3() {
      if (panel_3 == null) {
         panel_3=new JPanel();
         panel_3.setBorder(new TitledBorder(null, "mounted", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panel_3.setLayout(new BorderLayout(0, 0));
         panel_3.add(getMountpoint(), BorderLayout.CENTER);
         panel_3.add(getOptions(), BorderLayout.EAST);
      }
      return panel_3;
   }
   private JPanel getPanel_2() {
      if (panel_2 == null) {
         panel_2=new JPanel();
         panel_2.setBorder(new TitledBorder(null, "device", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panel_2.setLayout(new BorderLayout(0, 0));
         panel_2.add(getDevice());
      }
      return panel_2;
   }
   private JLabel getMountpoint() {
      if (mountpoint == null) {
         mountpoint=new JLabel("nowhere mounted");
      }
      return mountpoint;
   }
   private JLabel getDevice() {
      if (device == null) {
         device=new JLabel("/dev/???");
      }
      return device;
   }
   private JPanel getPanel_4() {
      if (panel_4 == null) {
         panel_4=new JPanel();
         panel_4.setBorder(new TitledBorder(null, "SubVolume", TitledBorder.LEADING, TitledBorder.TOP, null,
                  new Color(59, 59, 59)));
         panel_4.setLayout(new BorderLayout(0, 0));
         panel_4.add(getSubvolume(), BorderLayout.NORTH);
      }
      return panel_4;
   }
   private JPanel getPanel_5() {
      if (panel_5 == null) {
         panel_5=new JPanel();
         panel_5.setBorder(new TitledBorder(null, "count of snapshots", TitledBorder.LEADING, TitledBorder.TOP, null,
                  new Color(59, 59, 59)));
         panel_5.setLayout(new BorderLayout(0, 0));
         panel_5.add(getCount(), BorderLayout.NORTH);
      }
      return panel_5;
   }
   private JLabel getSubvolume() {
      if (subvolume == null) {
         subvolume=new JLabel("?");
      }
      return subvolume;
   }
   private JLabel getCount() {
      if (count == null) {
         count=new JLabel("?");
      }
      return count;
   }
   private JLabel getOptions() {
      if (options == null) {
         options=new JLabel("New label");
      }
      return options;
   }
   public Mount getMount() {
      return mount;
   }
   public void setMount(Mount mount1) {
      if (mount1 == null)
         return;
      if (mount == mount1)
         return;
      mount=mount1;
      getDevice().setText(mount.devicePath().toString());
      getMountpoint().setText(mount.mountPath().toString());
      getSubvolume().setText(mount.btrfsPath().toString());
      getOptions().setText(mount.options());
      getCount().setText(Integer.toString(mount.btrfsMap().size() ));
   }
   public String getLabel() {
      return label;
   }
   public void setLabel(String label1) {
      this.label=(label1 != null) ? label1 : "?";
   }
}
