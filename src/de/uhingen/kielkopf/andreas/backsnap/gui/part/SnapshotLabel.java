/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.part;

import java.awt.Color;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.*;
import javax.swing.border.*;

import org.eclipse.jdt.annotation.NonNull;

import de.uhingen.kielkopf.andreas.backsnap.btrfs.Snapshot;

/**
 * @author Andreas Kielkopf
 */
public class SnapshotLabel extends JLabel {
   static private final long                                   serialVersionUID=5111240176198425385L;
   static private ConcurrentSkipListMap<String, SnapshotLabel> cache           =new ConcurrentSkipListMap<>();
   public final Snapshot                                       snapshot;
   private STATUS                                              status;
   private SnapshotLabel() throws IOException {
      this(null);
   }
   /**
    * Ein Label das einen Snapshot oder ein Backup von einem Snapshot grafisch repräsentiert
    * 
    * @param snapshot
    * @throws IOException
    */
   private SnapshotLabel(Snapshot snapshot1) throws IOException {
      initialize();
      snapshot=snapshot1;
      if (snapshot instanceof Snapshot s) {
         if (s.dirName() instanceof String name)
            setText(name);
         else
            throw new IOException("Could not find snapshot: " + snapshot1.toString());
         // setStatus(snapshot.isBackup() ? STATUS.SICHERUNG : STATUS.NEU);
      } else
         setText("null");
      setStatus(STATUS.NEU);
   }
   public enum STATUS {
      NEU(Color.LIGHT_GRAY), // noch nicht zugeordnet
      // Source-Snapshots
      UNGESICHERT(Color.YELLOW), // kein Backup vorhanden
      INPROGRESS(Color.CYAN), // BACKUP läuft gerade
      GESICHERT(Color.GREEN.brighter()), // Es existiert bereits ein Backup
      // Auf dem Backuzpmedium
      FIXIERT(Color.GREEN.darker()), // Darf nicht gelöscht werden, weil es auf dem Source-Laufwerk noch vorhanden ist
      ALT(Color.RED), // Ist alt genug um gelöscht zu werden
      SPAM(Color.ORANGE), // Ist auch zum löschen vorgemerkt
      UNGENUTZT(Color.ORANGE.brighter()), // ist nicht in der aktuellen Kette enthalten
      GENUTZT(Color.BLUE), // IST in der aktuellen Kette enthalten
      NAHE(Color.ORANGE.brighter()),
      // ALLESOK(Color.GREEN.brighter()),
      SONSTIGE(Color.MAGENTA);
      public final Color color;
      /** @param color */
      STATUS(Color c) {
         color=c;
      }
   }
   private void initialize() {
      setBorder(new CompoundBorder(new MatteBorder(1, 1, 1, 1, (Color) new Color(0, 0, 0)),
               new EmptyBorder(1, 4, 1, 4)));
      setOpaque(true);
      // setBackground(unknownColor);
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
      // sl.setStatus(snapshot2.isBackup() ? STATUS.GESICHERT : STATUS.NEU);
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
   public STATUS getStatus() {
      return status;
   }
   public void setStatus(@NonNull STATUS s) {
      status=s;
      SwingUtilities.invokeLater(() -> setBackground(status.color));
      SwingUtilities.invokeLater(() -> {
         if (getParent() instanceof SnapshotPanel sp)
            sp.repaint(50);
      });
   }
}
