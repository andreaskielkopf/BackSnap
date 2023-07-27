/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * @author Andreas Kielkopf *
 */
public record Version(String version, String major, String minor, String patch) {
   static final Pattern VERSION=Pattern.compile("[^0-9]*([-0-9.]+)");
   static final Pattern MAYOR=Pattern.compile("[^0-9]*([0-9]+)");
   static final Pattern MINOR=Pattern.compile("[^0-9]*[0-9]+[.]([0-9]+)");
   static final Pattern PATCH=Pattern.compile("[^0-9]*[0-9]+[.][0-9]+[.]([0-9]+)");
   static private Version java;
   static private ExecutorService x;
   final static public Version getJava() {
      if (java == null)
         java=new Version(System.getProperty("java.version"));
      System.out.println("java: " + java);
      return java;
   }
   final static public ExecutorService getExecutor() {
      if (x == null) {
         if (getJava().getMayor() > 20)
            x=Executors.newVirtualThreadPerTaskExecutor();
         else
            x=Executors.newWorkStealingPool();
         System.out.println("Verwende " + x.getClass().getSimpleName());
      }
      return x;
   }
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
