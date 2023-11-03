/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Andreas Kielkopf
 *
 */
public interface RecordParser {
   /**
    * @param Matcher
    * @return String
    */
  static public String getString(Matcher m) {
      return m.find() ? m.group(1) : null;
   }
   /**
    * @param Matcher
    * @return Path (ansolut)
    */
   static public Path getPath(Matcher m) { // absolut Path
      return m.find() ? Path.of("/", m.group(1)) : null;
   }
   /**
    * @param Matcher
    * @return Integer
    */
   static public Integer getInt(Matcher m) {
      return m.find() ? Integer.parseUnsignedInt(m.group(1)) : null;
   }
   static public String getString(Pattern p, List<String> lines) {
      return lines.stream().map(l -> getString(p.matcher(l))).filter(e -> e != null).findFirst().get();
   }
   static public List<String> getStringList(Pattern p, List<String> lines) {
      return lines.stream().map(l -> getString(p.matcher(l))).filter(e -> e != null).toList();
   }
   static public List<Path> getPathList(Pattern p, List<String> lines) {
      return lines.stream().map(l -> getPath(p.matcher(l))).filter(e -> e != null).toList();
   }
   static public List<Integer> getIntList(Pattern p, List<String> lines) {
      return lines.stream().map(l -> getInt(p.matcher(l))).filter(e -> e != null).toList();
   }
   static public Map<String, String> getStringMap(Pattern p, List<String> lines) {
      return lines.stream().map(l -> p.matcher(l)).filter(m -> m.matches())
               .collect(Collectors.toMap(m -> m.group("key"), m -> m.group("value")));
   }
   static public ConcurrentSkipListMap<String, Path> getPathMap(Pattern p, List<String> lines) {
      ConcurrentSkipListMap<String, Path> nm=new ConcurrentSkipListMap<>();
      nm.putAll(lines.stream().map(l -> p.matcher(l)).filter(m -> m.matches())
               .collect(Collectors.toMap(m -> m.group("key"), m -> Path.of(m.group("value")))));
      return nm;
   }
}
