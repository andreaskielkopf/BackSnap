/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.*;

/**
 * @author Andreas Kielkopf
 *
 */
public class SnapshotLabel extends JLabel {
   private static final long serialVersionUID=5111240176198425385L;
   public Snapshot           snapshot;
   public SnapshotLabel() {
      initialize();
   }
   /**
    * @param snapshot
    */
   public SnapshotLabel(Snapshot snapshot1) {
      this();
      if (snapshot1 == null)
         return;
      snapshot=snapshot1;
      String name=snapshot.dirName();
      setBackground(snapshot.isBackup() ? aktuellColor : snapshotColor);
      setText(name);
   }
   final static Color unknownColor =Color.RED.darker();
   final static Color snapshotColor=Color.YELLOW.brighter();
   final static Color aktuellColor =Color.YELLOW.darker();
   final static Color        missingColor =Color.ORANGE.darker();
   public final static Color backupColor  =Color.GREEN.brighter();
   private void initialize() {
      setBorder(new CompoundBorder(new LineBorder(new Color(0, 0, 0), 2, true), new EmptyBorder(5, 5, 5, 5)));
      setOpaque(true);
      setBackground(unknownColor);
      setHorizontalAlignment(SwingConstants.CENTER);
      setText("name");
   }
}
