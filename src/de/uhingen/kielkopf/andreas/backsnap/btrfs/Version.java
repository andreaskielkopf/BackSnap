/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.util.regex.Pattern;

/**
 * @author Andreas Kielkopf *
 */
public record Version(String version, String major, String minor, String patch) {
   final static Pattern VERSION=Pattern.compile("[^0-9]*([-0-9.]+)");
   final static Pattern MAYOR=Pattern.compile("[^0-9]*([0-9]+)");
   final static Pattern MINOR=Pattern.compile("[^0-9]*[0-9]+[.]([0-9]+)");
   final static Pattern PATCH=Pattern.compile("[^0-9]*[0-9]+[.][0-9]+[.]([0-9]+)");
   public Version(String t) {
      this(Snapshot.getString(VERSION.matcher(t)), Snapshot.getString(MAYOR.matcher(t)),
               Snapshot.getString(MINOR.matcher(t)), Snapshot.getString(PATCH.matcher(t)));
   }
   public int getMayor() {
      return Integer.parseInt(major);
   }
   public int getMinor() {
      return Integer.parseInt(minor);
   }
   public int getPatch() {
      return Integer.parseInt(patch);
   }
   public float getVersion() {
      try {
         return Float.parseFloat(version);
      } catch (NumberFormatException e) {
         return Float.parseFloat(Integer.toString(getMayor()) + "." + Integer.toString(getMinor()));
      }
   }
}
