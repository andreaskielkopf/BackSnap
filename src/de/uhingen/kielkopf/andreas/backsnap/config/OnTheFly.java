/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.config;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javax.swing.*;

import org.eclipse.jdt.annotation.NonNull;

import de.uhingen.kielkopf.andreas.backsnap.btrfs.Pc;
import de.uhingen.kielkopf.andreas.backsnap.btrfs.Volume;
import de.uhingen.kielkopf.andreas.backsnap.gui.dialog.ConfigDialog;
import de.uhingen.kielkopf.andreas.beans.minijson.Etc;

/**
 * @author Andreas Kielkopf
 *
 */
public class OnTheFly extends JPanel {
   /**
    * 
    */
   private static final @NonNull Font FONT=new Font("Noto Sans", Font.PLAIN, 20);
   private JPanel                     panel;
   private JPanel                     panel_1;
   private JCheckBox                  cb_1;
   private JPanel                     panel_2;
   private JCheckBox                  cb_2;
   private JCheckBox                  cb_3;
   /**
    * Create the panel.
    */
   public OnTheFly() {
      initialize();
   }
   void createConfig() throws IOException {
      createEtcBacksnap();
      createEtcBackSnapLocal();
      selectBackupId();
      createBackupSubvolume();
   }
   /**
    * @throws IOException
    * 
    */
   private Volume createBackupSubvolume() throws IOException {
      return ConfigDialog.prepareBackupVolume(null, getFocusTraversalKeysEnabled());
   }
   private JCheckBox showSelection(JCheckBox cb, boolean b) {
      cb.setEnabled(!b);
      cb.setSelected(b);
      return cb;
   }
   private void initialize() {
      add(getPanel());
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getPanel_1(), BorderLayout.WEST);
         panel.add(getPanel_2(), BorderLayout.CENTER);
      }
      return panel;
   }
   private JPanel getPanel_1() {
      if (panel_1 == null) {
         panel_1=new JPanel();
         panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.Y_AXIS));
         panel_1.add(getCb_1());
         panel_1.add(getCb_2());
         panel_1.add(getCb_3());
      }
      return panel_1;
   }
   private JPanel getPanel_2() {
      if (panel_2 == null) {
         panel_2=new JPanel();
         panel_2.setLayout(new BorderLayout(0, 0));
      }
      return panel_2;
   }
   /**
    * @return
    * 
    */
   private boolean createEtcBacksnap() {
      JCheckBox cb=getCb_1();
      if (cb.isEnabled() && cb.isSelected())
         try {
            System.out.println(cb.getText());
            Etc.createConfigDir("backsnap");
         } catch (IOException e) { /* */
            System.err.println(e.getMessage());
         }
      return hasEtcBacksnap();
   }
   private boolean hasEtcBacksnap() {
      JCheckBox cb=showSelection(getCb_1(), false);
      try {
         return showSelection(cb, (Etc.hasConfigDir("backsnap") != null)).isSelected();
      } catch (IOException e) { /* */
         System.err.println(e.getMessage());
      }
      return cb.isSelected();
   }
   private JCheckBox getCb_1() {
      if (cb_1 == null) {
         cb_1=new JCheckBox("create /etc/backsnap.d");
         cb_1.setFont(FONT);
         hasEtcBacksnap();
         cb_1.addActionListener(a -> createEtcBacksnap());
      }
      return cb_1;
   }
   /**
    * @return
    * 
    */
   private boolean createEtcBackSnapLocal() {
      JCheckBox cb=getCb_2();
      if (cb.isEnabled() && cb.isSelected())
         try {
            Path local=Path.of("/etc/backsnap.d/local.conf");
            System.out.println("create " + local.toString());
            Files.createFile(local);
            if (Etc.getConfig("backsnap") instanceof Etc etc) {
               List<String> lines=etc.conf.get(local);
               lines.add("# backup local pc per sudo");
               lines.add("#pc = localhost");
               lines.add("# backup local pc per ssh");
               lines.add("pc = root@localhost");
               lines.add("");
               lines.add("# backuplabel = manjaro18 for snapshots of /");
               lines.add("manjaro18 = /");
               lines.add("# backuplabel = manjaro18.home for snapshots of /home");
               lines.add("manjaro18.home = /home");
               lines.add("");
               etc.save();
            }
         } catch (IOException e) { /* */
            System.err.println(e.getMessage());
         }
      return hasEtcBackSnapLocal();
   }
   private boolean hasEtcBackSnapLocal() {
      JCheckBox cb=showSelection(getCb_2(), false);
      try {
         if (Etc.getConfig("backsnap") instanceof Etc etc) {
            return showSelection(cb, etc.conf.entrySet().stream()
                     .filter(e -> e.getKey().getFileName().toString().startsWith("local")).findAny().isPresent())
                              .isSelected();
         }
      } catch (IOException e) { /* */
         System.err.println(e.getMessage());
      }
      return cb.isSelected();
   }
   private JCheckBox getCb_2() {
      if (cb_2 == null) {
         cb_2=new JCheckBox("create local.conf");
         cb_2.setFont(FONT);
         hasEtcBackSnapLocal();
         cb_2.addActionListener(a -> createEtcBackSnapLocal());
      }
      return cb_2;
   }
   /**
    * @return
    * 
    */
   private boolean selectBackupId() {
      ConfigDialog.getBackupVolume(null);
      return hasBackupId();
   }
   private boolean hasBackupId() {
      JCheckBox cb=showSelection(getCb_3(), false);
      try {
         if (Etc.getConfig("backsnap") instanceof Etc etc) // ergibt eine config mit der ID
            return showSelection(cb, etc.conf.entrySet().stream()
                     .filter(e -> e.getValue().stream().filter(l -> l.startsWith("backup_id")).findAny().isPresent())
                     .findFirst().isPresent()).isSelected();
      } catch (IOException e) { /* */
         System.err.println(e.getMessage());
      }
      return cb.isSelected();
   }
   private JCheckBox getCb_3() {
      if (cb_3 == null) {
         cb_3=new JCheckBox("select UUID of BackupVolume");
         cb_3.setFont(FONT);
         hasBackupId();
         cb_3.addActionListener(a -> selectBackupId());
      }
      return cb_3;
   }
   /**
    * Bereitet die Konfiguration vor, und liefert sie bei Erfolg zurück
    * 
    * @param etc
    * @throws IOException
    */
   public static Etc prepare() throws IOException {
      // 1. /etc/backsnap.d erstellen
      if (Etc.hasConfigDir("backsnap") == null)
         Etc.createConfigDir("backsnap");
      if (Etc.getConfig("backsnap") instanceof Etc etc) {
         // 2. /etc/backsnap.d/local erstellen
         boolean hasLocal=etc.conf.entrySet().stream()
                  .filter(e -> e.getKey().getFileName().toString().startsWith("local")).findAny().isPresent();
         Path local=Path.of("/etc/backsnap.d/local.conf");
         if (!hasLocal) {
            System.out.println("create " + local.toString());
            Files.createFile(local);
            etc=Etc.getConfig("backsnap"); // neu einlesen
            List<String> lines=etc.conf.get(local);
            lines.add("# backup local pc per sudo");
            lines.add("# pc = localhost");
            lines.add("# backup local pc per ssh");
            lines.add("pc = root@localhost");
            lines.add("");
            lines.add("# backuplabel = manjaro18 for snapshots of /");
            lines.add("manjaro18 = /");
            lines.add("# backuplabel = manjaro18.home for snapshots of /home");
            lines.add("manjaro18.home = /home");
            lines.add("");
            etc.save();
         }
         // 3. Backup-UUID ermitteln und eintragen
         boolean hasId=etc.conf.entrySet().stream()
                  .filter(e -> e.getValue().stream().filter(l -> l.startsWith("backup_id")).findAny().isPresent())
                  .findAny().isPresent();
         if (!hasId) {
            Volume v=ConfigDialog.getBackupVolume(Pc.getPc("root@localhost"));
            // Volume v=ConfigDialog.prepareBackupVolume(null, false);
            if (v.uuid() instanceof String uuid && uuid.length() > 10) {
               List<String> lines=etc.conf.get(local);
               int nr=lines.isEmpty() ? 0
                        : lines.indexOf(lines.stream().filter(l -> (l.startsWith("pc ") || l.startsWith("pc=")))
                                 .findFirst().orElse(lines.getLast()));
               lines.add(++nr, "");
               lines.add(++nr, "# detect and mount backupvolume by scanning for this id (as part of the uuid)");
               lines.add(++nr, "backup_id = " + uuid);
               lines.add(++nr, "");
               etc.save();
            }
         }
         return etc;
      }
      return null;
   }
}
