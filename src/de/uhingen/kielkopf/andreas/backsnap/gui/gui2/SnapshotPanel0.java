/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.gui2;

import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListMap;

import org.eclipse.jdt.annotation.NonNull;

import de.uhingen.kielkopf.andreas.backsnap.btrfs.Mount;
import de.uhingen.kielkopf.andreas.backsnap.btrfs.Snapshot;
import de.uhingen.kielkopf.andreas.backsnap.gui.part.SnapshotLabel;

/**
 * @author Andreas Kielkopf
 */
public interface SnapshotPanel0 {
   static public final Font FONT_INFO  =new Font("Noto Sans", Font.PLAIN, 16);
   static public final Font FONT_INFO_B=new Font("Noto Sans", Font.BOLD, 16);
   public abstract ConcurrentSkipListMap<String, SnapshotLabel> labelTree_DirNameS();
   public abstract ConcurrentSkipListMap<String, SnapshotLabel> labelTree_UUID();
   public abstract ConcurrentSkipListMap<String, SnapshotLabel> labelTree_KeyO();
   public abstract ConcurrentSkipListMap<String, SnapshotLabel> labelTree_R_UUID();
   /**
    * @param dimension
    */
   public abstract void setMinimumSize(@NonNull Dimension dimension);
   /**
    * @param string
    */
   public abstract void setTitle(String string);
   /**
    * @param volumeMount
    */
   public abstract void setInfo(Mount volumeMount);
   /**
    * @param values
    * @return
    * @throws IOException 
    */
   public abstract ConcurrentSkipListMap<String, Snapshot> setVolume(Collection<Snapshot> values) throws IOException;
   /**
    * @return
    */
   public abstract ArrayList<SnapshotLabel> mixedList();
  
   
}
