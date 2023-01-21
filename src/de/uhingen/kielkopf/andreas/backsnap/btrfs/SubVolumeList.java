/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static java.lang.System.out;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import de.uhingen.kielkopf.andreas.backsnap.Commandline;
import de.uhingen.kielkopf.andreas.backsnap.Commandline.CmdStream;

/**
 * Ene Liste der Subvolumes eines Rechners
 * 
 * @author Andreas Kielkopf
 *
 */
public record SubVolumeList(String extern, ConcurrentSkipListMap<String, Mount> mountTree) {
   /**
    * @param string
    *           mit Zugang zum PC
    * @param snapTree
    *           Liste mit Snapshots dieses PCs
    * @throws IOException
    */
   public SubVolumeList(String extern) throws IOException {
      this(extern, new ConcurrentSkipListMap<>());
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
      String key="mount " + extern;
      try (CmdStream mountList=Commandline.executeCached(mountCmd, key)) {
         mountList.backgroundErr();
         for (String line:mountList.erg().toList()) {
            Mount mount=new Mount(this, line, extern);
            mountTree.put(mount.key(), mount);
         }
         mountList.waitFor();
         for (String line:mountList.errList())
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
   public Mount getBackupVolume(String vorschlag) {
      if (mountTree.containsKey(vorschlag))
         return mountTree.get(vorschlag);
      Entry<String, Mount> test=null;
      for (Entry<String, Mount> e:mountTree.entrySet())
         if (vorschlag.startsWith(e.getKey())) // find the longest matching key
            if ((test == null) || (test.getKey().length() < e.getKey().length()))
               test=e;
      if (test != null)
         return test.getValue();
      List<Mount> treffer=new ArrayList<>();
      for (Entry<String, Mount> e:mountTree.entrySet())
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
