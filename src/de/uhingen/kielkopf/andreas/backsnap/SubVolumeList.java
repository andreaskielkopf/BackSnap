/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap;

import static java.lang.System.out;

import java.io.IOException;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
   public SubVolumeList(String extern, SnapTree snapTree) {
      this(extern, new TreeMap<>());
      populate(snapTree);
   }
   /**
    * Ermittle alle Subvolumes die Snapshots enthalten indem "mount" gefragt wird
    * 
    * @param snapTree
    */
   private void populate(SnapTree snapTree) {
      StringBuilder mountCmd=new StringBuilder("mount|grep btrfs");
      if ((extern instanceof String x) && (!x.isBlank()))
         if (x.startsWith("sudo"))
            mountCmd.insert(0, x);
         else
            mountCmd.insert(0, "ssh " + x + " '").append("'");
      System.out.println(mountCmd);
      try (CmdStream subvolumeList=Commandline.execute(mountCmd)) {
         Future<?> errorHandling=Commandline.background.submit(() -> {// Some Error handling in background
            if (subvolumeList.err().peek(System.err::println).anyMatch(line -> {
               return line.contains("Connection closed") || line.contains("connection unexpectedly closed");
            }))
               throw new IOException("connection unexpectedly closed");
            return "";
         });
         subvolumeList.erg().forEachOrdered(line -> {
            Subvolume subvolume=new Subvolume(line, extern, snapTree);
            if (!subvolume.snapshotTree().isEmpty()) // interessant sind nur die Subvolumes mit snapshots
               subvTree.put(subvolume.key(), subvolume);
         });
         errorHandling.get();
         out.println();
      } catch (IOException | ExecutionException | InterruptedException e) {
         e.printStackTrace();
      }
   }
}
