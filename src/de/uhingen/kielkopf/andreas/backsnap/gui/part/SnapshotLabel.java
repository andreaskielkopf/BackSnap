/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.part;

import java.awt.Color;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.*;

import de.uhingen.kielkopf.andreas.backsnap.btrfs.Snapshot;

/**
 * @author Andreas Kielkopf
 */
public class SnapshotLabel extends JLabel {
   static private final long                                   serialVersionUID=5111240176198425385L;
   static private ConcurrentSkipListMap<String, SnapshotLabel> cache           =new ConcurrentSkipListMap<>();
   public Snapshot                                             snapshot;
   public SnapshotLabel() {
      initialize();
   }
   /**
    * Ein Label das einen Snapshot oder ein Backup von einem Snapshot grafisch repräsentiert
    * 
    * @param snapshot
    * @throws IOException
    */
   public SnapshotLabel(Snapshot snapshot1) throws IOException {
      this();
      if (snapshot1 == null)
         return;
      snapshot=snapshot1;
      String name=snapshot.dirName();
      if (name == null)
         throw new IOException("Could not find snapshot: " + snapshot1.toString());
      setBackground(snapshot.isBackup() ? aktuellColor : snapshotColor);
      setText(name);
   }
   static final Color        unknownColor       =Color.GRAY.brighter();
   static final Color        snapshotColor      =Color.YELLOW.brighter();
   static public final Color aktuellColor       =Color.YELLOW.darker();
   static public final Color delete2Color       =Color.ORANGE;
   static public final Color allesOkColor       =Color.GREEN.brighter(); // muß bleiben
   static public final Color deleteOldColor     =Color.RED.brighter();   // darf weg
   static public final Color naheColor          =Color.ORANGE.brighter();
   static public final Color markInProgressColor=Color.CYAN;
   private void initialize() {
      setBorder(new CompoundBorder(new MatteBorder(1, 1, 1, 1, (Color) new Color(0, 0, 0)), new EmptyBorder(1, 4, 1, 4)));
      setOpaque(true);
      setBackground(unknownColor);
      setHorizontalAlignment(SwingConstants.CENTER);
   }
   /**
    * Wiederverwendung eines bereits erzeugten Labels aus dem cache
    * 
    * @param snapshot2
    * @return vorhandenes label oder neues
    * @throws IOException
    */
   static public SnapshotLabel getSnapshotLabel(Snapshot snapshot2) throws IOException {
      if (snapshot2 == null)
         return new SnapshotLabel(null);// exception
      if (!cache.containsKey(snapshot2.uuid()))
         cache.put(snapshot2.uuid(), new SnapshotLabel(snapshot2));
      SnapshotLabel sl=cache.get(snapshot2.uuid());
      sl.setBackground(snapshot2.isBackup() ? aktuellColor : snapshotColor);
      return sl;
   }
   @Override
   public String toString() {
      return new StringBuilder(snapshot.dirName()).toString();
   }
   @Override
   public synchronized void addMouseListener(MouseListener l) {
      MouseListener[] wl=getListeners(MouseListener.class);
      for (MouseListener mouseListener:wl)
         if (mouseListener == l)
            return;
      super.addMouseListener(l);
   }
}
