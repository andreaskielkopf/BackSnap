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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
    */
   public SubVolumeList(String extern) {
      this(extern, new TreeMap<>());
      populate();
   }
   /**
    * Ermittle alle Subvolumes die Snapshots enthalten indem "mount" gefragt wird
    * 
    * @param snapTree
    */
   private void populate() {
      StringBuilder mountCmd=new StringBuilder("mount|grep btrfs");
      if (!extern.isBlank())
         if (extern.startsWith("sudo"))
            mountCmd.insert(0, extern);
         else
            mountCmd.insert(0, "ssh " + extern + " '").append("'");
      System.out.println(mountCmd);
      try (CmdStream subvolumeList=Commandline.execute(mountCmd)) {
         Future<?> errorHandling=Commandline.background.submit(() -> {// Some Error handling in background
            if (subvolumeList.err().peek(System.err::println).anyMatch(line -> {
               return line.contains("No route to host") || line.contains("Connection closed")
                        || line.contains("connection unexpectedly closed");
            }))
               throw new IOException("connection unexpectedly closed");
            return "";
         });
         subvolumeList.erg().forEachOrdered(line -> {
            Subvolume subvolume=new Subvolume(line, extern);
            subvTree.put(subvolume.key(), subvolume);
         });
         errorHandling.get();
         out.println();
      } catch (IOException | ExecutionException | InterruptedException e) {
         e.printStackTrace();
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
