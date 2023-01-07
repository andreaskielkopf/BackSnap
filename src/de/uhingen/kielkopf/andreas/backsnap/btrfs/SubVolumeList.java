/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static java.lang.System.out;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;

/**
 * Ene Liste der Subvolumes eines Rechners
 * 
 * @author Andreas Kielkopf
 *
 */
public record SubVolumeList(String extern, TreeMap<String, Subvolume> subvTree) {
   /**
    * @param string
    *           mit Zugang zum PC
    * @param snapTree
    *           Liste mit Snapshots dieses PCs
    * @throws IOException
    */
   public SubVolumeList(String extern) throws IOException {
      this(extern, new TreeMap<>());
      populate();
   }
   /**
    * Ermittle alle Subvolumes die Snapshots enthalten indem "mount" gefragt wird
    * 
    * @param snapTree
    * @throws IOException
    */
   private void populate() throws IOException {
      StringBuilder mountCmd=new StringBuilder("mount|grep btrfs");
      if (!extern.isBlank())
         if (extern.startsWith("sudo"))
            mountCmd.insert(0, extern);
         else
            mountCmd.insert(0, "ssh " + extern + " '").append("'");
      System.out.println(mountCmd);
      try (CmdStream subvolumeList=Commandline.execute(mountCmd)) {
         subvolumeList.backgroundErr();
         for (String line:subvolumeList.erg().toList()) {
            Subvolume subvolume=new Subvolume(line, extern);
            subvTree.put(subvolume.key(), subvolume);
         }
         for (String line:subvolumeList.errList())
            if (line.contains("No route to host") || line.contains("Connection closed")
                     || line.contains("connection unexpectedly closed"))
               throw new IOException(line);
         out.println();
      }
   }
   /**
    * @param vorschlag
    * @return
    */
   public Subvolume getBackupVolume(String vorschlag) {
      if (subvTree.containsKey(vorschlag))
         return subvTree.get(vorschlag);
      List<Subvolume> treffer=new ArrayList<>();
      for (Entry<String, Subvolume> e:subvTree.entrySet())
         if (e.getKey().contains(vorschlag))
            treffer.add(e.getValue());
      if (treffer.size() == 1)
         return treffer.get(0);
      Path p =Paths.get(vorschlag);
      Path pp=p.getParent();
      if (pp.getParent() == null)
         return null;
      return getBackupVolume(pp.toString());
   }
}
