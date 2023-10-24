/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui;

import static javax.swing.SwingUtilities.invokeLater;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.uhingen.kielkopf.andreas.backsnap.btrfs.Mount;
import de.uhingen.kielkopf.andreas.backsnap.btrfs.Pc;
import de.uhingen.kielkopf.andreas.backsnap.config.Log;
import de.uhingen.kielkopf.andreas.backsnap.config.Log.LEVEL;

/**
 * @author Andreas Kielkopf
 *
 */
public class Computer extends JPanel {
   static private final long       serialVersionUID=-1777560551058489693L;
   private JPanel                  panel;
   private JPanel                  connection;
   private JPanel                  subvolumes;
   private JLabel                  lblNewLabel;
   private JTextField              computerName;
   private JLabel                  lblNewLabel_1;
   private JList<Mount>            list;
   private Subvol                  subvol;
   private JComboBox<String>       benutzer;
   private JPanel                  panel_1;
   private JLabel                  TestInfo;
   private JPanel                  panel_2;
   private JPanel                  panel_3;
   private String                  titel           ="?";
   private TitledBorder            titelBorder;
   private JButton                 btnNewButton;

   private DefaultListModel<Mount> listModel;
   /**
    * Create the panel.
    */
   public Computer() {
      initialize();
   }
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getPanel());
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         setTitelBorder(new TitledBorder(null, "Computer:", TitledBorder.LEADING, TitledBorder.TOP, null,
                  new Color(59, 59, 59)));
         panel.setBorder(getTitelBorder());
         panel.setBackground(Color.CYAN);
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getSubvolumes());
         panel.add(getPanel_3(), BorderLayout.NORTH);
      }
      return panel;
   }
   private JPanel getConnection() {
      if (connection == null) {
         connection=new JPanel();
         connection.setBorder(new TitledBorder(null, "Connection", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         connection.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
         connection.add(getBenutzer());
         connection.add(getLblNewLabel());
         connection.add(getComputerName());
         connection.add(getBtnNewButton());
         connection.add(getLblNewLabel_1());
      }
      return connection;
   }
   private JPanel getSubvolumes() {
      if (subvolumes == null) {
         subvolumes=new JPanel();
         subvolumes.setBorder(new TitledBorder(null, "Subvolumes", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         subvolumes.setLayout(new BoxLayout(subvolumes, BoxLayout.X_AXIS));
         subvolumes.add(getPanel_1());
         subvolumes.add(getSubvol());
      }
      return subvolumes;
   }
   private JLabel getLblNewLabel() {
      if (lblNewLabel == null) {
         lblNewLabel=new JLabel("@");
      }
      return lblNewLabel;
   }
   private JTextField getComputerName() {
      if (computerName == null) {
         computerName=new JTextField();
         computerName.setEnabled(false);
         computerName.setText("localhost");
         computerName.setColumns(10);
      }
      return computerName;
   }
   private JLabel getLblNewLabel_1() {
      if (lblNewLabel_1 == null) {
         lblNewLabel_1=new JLabel(
                  "<html>Examples: root@192.168.178.4  / root@localhost  / <br>  sudo  / root@notebook ...  </html>");
         lblNewLabel_1.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return lblNewLabel_1;
   }
   private JList<Mount> getList() {
      if (list == null) {
         list=new JList<Mount>(getListModell());
         list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
               updateSubvolumeInfo(e);
            }
         });
      }
      return list;
   }
   /**
    * @param e
    */
   protected void updateSubvolumeInfo(ListSelectionEvent ev) {
      if (ev.getSource() instanceof JList<?> l)
         if (l.getSelectedValue() instanceof Mount mount) {
            Log.logln(mount.toString(), LEVEL.BTRFS);
            getSubvol().setMount(mount);
         }
   }
   private Subvol getSubvol() {
      if (subvol == null) {
         subvol=new Subvol();
      }
      return subvol;
   }
   private JComboBox<String> getBenutzer() {
      if (benutzer == null) {
         benutzer=new JComboBox<String>();
         benutzer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
               invokeLater(() -> calculateExtern());
            }
         });
         benutzer.setModel(new DefaultComboBoxModel<String>(new String[] {Pc.SUDO, Pc.ROOT}));
         benutzer.setSelectedIndex(0);
         benutzer.setEditable(true);
      }
      return benutzer;
   }
   private JPanel getPanel_1() {
      if (panel_1 == null) {
         panel_1=new JPanel();
         panel_1.setLayout(new BorderLayout(0, 0));
         panel_1.add(getList());
      }
      return panel_1;
   }
   private JLabel getTestInfo() {
      if (TestInfo == null) {
         TestInfo=new JLabel("?");
         TestInfo.setOpaque(true);
      }
      return TestInfo;
   }
   private JPanel getPanel_2() {
      if (panel_2 == null) {
         panel_2=new JPanel();
         panel_2.setLayout(new BorderLayout(0, 0));
         panel_2.add(getTestInfo(), BorderLayout.SOUTH);
      }
      return panel_2;
   }
   private JPanel getPanel_3() {
      if (panel_3 == null) {
         panel_3=new JPanel();
         panel_3.setLayout(new BorderLayout(0, 0));
         panel_3.add(getConnection(), BorderLayout.CENTER);
         panel_3.add(getPanel_2(), BorderLayout.SOUTH);
      }
      return panel_3;
   }
   public String getTitel() {
      return titel;
   }
   public void setTitel(String titel1) {
      this.titel=(titel1 != null) ? titel1 : "";
      getTitelBorder().setTitle(titel);
   }
   private JButton getBtnNewButton() {
      if (btnNewButton == null) {
         btnNewButton=new JButton("test");
         btnNewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
               // invokeLater(() -> test());
            }
         });
      }
      return btnNewButton;
   }
   /**
    * @return
    */
   private DefaultListModel<Mount> getListModell() {
      if (listModel == null)
         listModel=new DefaultListModel<Mount>();
      return listModel;
   }
   /** recalculate the connectionString */
   protected void calculateExtern() {
      if (benutzer.getSelectedItem() instanceof String b) {
         boolean s=(b.equals(Pc.SUDO));
         getComputerName().setEnabled(!s);
         // extern=s ? b : b + "@" + getComputerName().getText();
         // System.out.println(extern);
      }
   }
   public TitledBorder getTitelBorder() {
      return titelBorder;
   }
   public void setTitelBorder(TitledBorder titelBorder1) {
      this.titelBorder=titelBorder1;
   }
}
