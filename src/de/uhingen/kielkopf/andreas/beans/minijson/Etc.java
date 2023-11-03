/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.minijson;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import org.eclipse.jdt.annotation.NonNull;

/**
 * @author Andreas Kielkopf
 *
 */
public class Etc {
   static final String                                    CONF=".conf";
   public final ConcurrentSkipListMap<Path, List<String>> conf;
   /**
    * @param directory
    * @param map
    */
   public Etc(@NonNull ConcurrentSkipListMap<Path, List<String>> conf0) {
      conf=conf0;
   }
   /**
    * @param string
    * @return
    * @throws IOException
    */
   public static Path hasConfigDir(String directory) throws IOException {
      if (directory instanceof String d && d.matches("[a-zA-Z0-9]{3,80}")) {
         Path p=Paths.get("/etc", directory + ".d");
         if (Files.isDirectory(p))
            return p;
      }
      return null;
   }
   public static Path createConfigDir(String directory) throws IOException {
      if (directory instanceof String d && d.matches("[a-zA-Z0-9]{3,80}")) {
         Path p=Paths.get("/etc", directory + ".d");
         if (Files.notExists(p))
            Files.createDirectory(p);
      }
      return hasConfigDir(directory);
   }
   // public static boolean hasConfig(String directory) {
   // if
   // if(Files.isDirectory(p))return p;}return Files.list(p).anyMatch(f->f.getFileName().toString().endsWith(".conf"));}
   /**
    * @param string
    * @return
    * @throws IOException
    */
   public static Etc getConfig(@NonNull String directory) throws IOException {
      ConcurrentSkipListMap<Path, List<String>> map=new ConcurrentSkipListMap<>();
      if (hasConfigDir(directory) instanceof Path p) {
         List<Path> list=Files.list(p).filter(f -> f.getFileName().toString().endsWith(".conf")).toList();
         for (Path path:list) 
            map.put(path, Files.readAllLines(path));
         
         return new Etc(map);
      }
      return null;
   }
   /**
    * @throws IOException
    * 
    */
   public void save() throws IOException {
      // Path p=Paths.get("/etc", directory + ".d");
      for (Entry<Path, List<String>> entry:conf.entrySet())
         if (entry.getKey() instanceof Path path)
            if (entry.getValue() instanceof List<String> ls)
               Files.write(path, ls, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE);
   }
}
