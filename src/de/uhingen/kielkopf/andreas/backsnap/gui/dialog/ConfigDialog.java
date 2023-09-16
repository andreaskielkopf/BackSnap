/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.dialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.eclipse.jdt.annotation.NonNull;

import de.uhingen.kielkopf.andreas.backsnap.btrfs.*;

/**
 * @author Andreas Kielkopf Ein ConfigDialog um das Volume für Backups festzulegen
 */
public class ConfigDialog extends JDialog {
  
  
   private static final long           serialVersionUID=-993461584598902127L;
   /** Use a bigger Font for this Dialog */
   private static final @NonNull Font  FONT            =new Font("Monospaced", Font.PLAIN, 15);
   private JButton                     btnCancel;
   private JButton                     btnOK;
   private JButton                     btnScan;
   private JPanel                      panelMaster;
   private JTextPane                   infoBtrfs;
   private JList<Volume>               jlist;
   final private @NonNull ConfigDialog me;
   private JPanel                      panelSelect;
   private JPanel                      panelOK;
   private JPanel                      panelScan;
   private JPanel                      panelInfo;
   private JScrollPane                 scrollPane;
   private JTextPane                   textPaneMount;
   private Vector<Volume>              volumeVector    =new Vector<>();
   private final Pc                    pc;
   private JLabel                      lblNewLabel;
   private JPanel                      panelTitel;
   private JPanel                      panelButton;
   /**
    * Create the dialog.
    * 
    * @param pc0
    */
   public ConfigDialog(Pc pc0) {
      me=this;
      pc=pc0;
      init();
   }
   private void init() {
      setBounds(100, 100, 700, 550);
      getContentPane().setLayout(new BorderLayout());
      getContentPane().add(getPanelMaster(), BorderLayout.CENTER);
   }
   private JPanel getPanelMaster() {
      if (panelMaster == null) {
         panelMaster=new JPanel();
         panelMaster.setBorder(new EmptyBorder(5, 5, 5, 5));
         panelMaster.setLayout(new BorderLayout(0, 10));
         panelMaster.add(getPanelSelect(), BorderLayout.CENTER);
         panelMaster.add(getPanelTitel(), BorderLayout.NORTH);
         panelMaster.add(getPanelButton(), BorderLayout.SOUTH);
      }
      return panelMaster;
   }
   private JPanel getPanelButton() {
      if (panelButton == null) {
         panelButton=new JPanel();
         panelButton.setLayout(new BorderLayout(0, 0));
         panelButton.add(getPanelOK(), BorderLayout.EAST);
         panelButton.add(getPanelScan(), BorderLayout.WEST);
      }
      return panelButton;
   }
   private JButton getBtnCancel() {
      if (btnCancel == null) {
         btnCancel=new JButton("Cancel");
         btnCancel.addActionListener(e -> {
            getList().setSelectedValue(null, false);
            me.setVisible(false);
         });
         btnCancel.setActionCommand("Cancel");
      }
      return btnCancel;
   }
   private JButton getBtnOK() {
      if (btnOK == null) {
         btnOK=new JButton("OK");
         btnOK.setEnabled(false);
         btnOK.setActionCommand("OK");
         btnOK.addActionListener(e -> {
            me.setVisible(false);
         });
         getRootPane().setDefaultButton(btnOK);
      }
      return btnOK;
   }
   private JButton getBtnScan() {
      if (btnScan == null) {
         btnScan=new JButton("scan again");
         btnScan.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
               jlist.setListData(getData());
               getTextPaneBtrfs().setText("");
               getTextPaneMount().setText("");
            }
         });
      }
      return btnScan;
   }
   private Vector<Volume> getData() {
      volumeVector.removeAllElements();
      volumeVector.addAll(Btrfs.show(pc, false, true).values());
      return volumeVector;
   }
   private JList<Volume> getList() {
      if (jlist == null) {
         jlist=new JList<Volume>(getData());
         jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
         jlist.addListSelectionListener(e -> {
            String b="";
            String m="not jet mounted";
            if (getList().getSelectedValue() instanceof Volume volume) {
               b=String.join("\n", volume.lines());
               if (volume.mount() instanceof Mount mount)
                  m=mount.toString();
            }
            getTextPaneBtrfs().setText(b);
            getTextPaneMount().setText(m);
            getBtnOK().setEnabled(!b.isBlank());
         });
         jlist.setFont(FONT);
         jlist.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID=-6391227579401086998L;
            @Override
            public Component getListCellRendererComponent(JList<?> list0, Object value, int index, boolean isSelected,
                     boolean hasFocus) {
               Component comp=super.getListCellRendererComponent(jlist, value, index, isSelected, hasFocus);
               if (comp instanceof JLabel label && value instanceof Volume volume) {
                  label.setText((isSelected ? "==> " : "    ") + volume.listName());
                  label.setBackground(volume.listColor());
               }
               return this;
            }
         });
      }
      return jlist;
   }
   private JPanel getPanelSelect() {
      if (panelSelect == null) {
         panelSelect=new JPanel();
         panelSelect.setLayout(new BorderLayout(0, 10));
         panelSelect.add(getPanelInfo(), BorderLayout.SOUTH);
         panelSelect.add(getScrollPane(), BorderLayout.CENTER);
      }
      return panelSelect;
   }
   private JPanel getPanelOK() {
      if (panelOK == null) {
         panelOK=new JPanel();
         panelOK.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
         panelOK.add(getBtnOK());
         panelOK.add(getBtnCancel());
      }
      return panelOK;
   }
   private JPanel getPanelScan() {
      if (panelScan == null) {
         panelScan=new JPanel();
         panelScan.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
         panelScan.add(getBtnScan());
      }
      return panelScan;
   }
   private JPanel getPanelInfo() {
      if (panelInfo == null) {
         panelInfo=new JPanel();
         panelInfo.setLayout(new GridLayout(0, 1, 0, 10));
         panelInfo.add(getTextPaneBtrfs());
         panelInfo.add(getTextPaneMount());
      }
      return panelInfo;
   }
   private JScrollPane getScrollPane() {
      if (scrollPane == null) {
         scrollPane=new JScrollPane(getList());
         scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
         scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      }
      return scrollPane;
   }
   private JTextPane getTextPaneBtrfs() {
      if (infoBtrfs == null) {
         infoBtrfs=new JTextPane();
         infoBtrfs.setFont(FONT);
      }
      return infoBtrfs;
   }
   private JTextPane getTextPaneMount() {
      if (textPaneMount == null) {
         textPaneMount=new JTextPane();
         textPaneMount.setFont(FONT);
      }
      return textPaneMount;
   }
   public static Volume getBackupVolume(Pc pc) {
      ConfigDialog dialog=new ConfigDialog(pc);
      dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
      dialog.setVisible(true);
      while (dialog.isShowing())
         try {
            Thread.sleep(100);
         } catch (InterruptedException ignore) { /* */ }
      dialog.me.dispose();
      return dialog.getList().getSelectedValue();
   }
   static public Volume prepareBackupVolume(Volume v0, boolean keepMounted) throws IOException {
      if (v0 instanceof Volume volume) {
         volume.pc().mountBackupRoot(volume, true);// Mount
         if (!Btrfs.testSubvolume(volume.pc(), Pc.TMP_BACKSNAP))
            Btrfs.createSubvolume(volume.pc(), Pc.TMP_BACKSNAP); // Create Subvolume
         if (!keepMounted)
            volume.pc().mountBackupRoot(volume, false); // Unmount
      }
      return v0;
   }
   /**
    * Launch the application.
    */
   public static void main(String[] args) {
      try {
         if (getBackupVolume(Pc.getPc(Pc.ROOT_LOCALHOST)) instanceof Volume volume) {
            prepareBackupVolume(volume, false);
            System.out.println(volume.uuid());
         } else
            System.out.println("null");
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   private JLabel getLblNewLabel() {
      if (lblNewLabel == null) {
         lblNewLabel=new JLabel("Please choose a backup-volume");
         lblNewLabel.setFont(FONT);
      }
      return lblNewLabel;
   }
   private JPanel getPanelTitel() {
      if (panelTitel == null) {
         panelTitel=new JPanel();
         panelTitel.setLayout(new BorderLayout(0, 0));
         panelTitel.add(getLblNewLabel(), BorderLayout.CENTER);
      }
      return panelTitel;
   }
}
