/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * @author Andreas Kielkopf *
 */
public record Version(String name, String version, String major, String minor, String patch) {
   static final Pattern VERSION=Pattern.compile("[^0-9]*([-ea0-9.]+)");// + early-access
   static final Pattern MAYOR=Pattern.compile("[^0-9]*([0-9]+)");
   static final Pattern MINOR=Pattern.compile("[^0-9]*[0-9]+[.]([0-9]+)");
   static final Pattern PATCH=Pattern.compile("[^0-9]*[0-9]+[.][0-9]+[.]([0-9]+)");
   static private Version java;
   static private ExecutorService vx;
   private static ExecutorService rx;
   static public final Version getJava() {
      if (java == null)
         java=new Version("java", System.getProperty("java.version"));
      return java;
   }
   static public final ExecutorService getVx() {
      if (vx == null) {
         if (getJava().getMayor() > 20)
            vx=Executors.newVirtualThreadPerTaskExecutor();
         else
            vx=getRx();
      }
      return vx;
   }
   static public final ExecutorService getRx() {
      if (rx == null) {
         rx=Executors.newWorkStealingPool();
      }
      return rx;
   }
   @SuppressWarnings("resource")
   static public final String getVxText() {
      return "using " + getVx().getClass().getSimpleName();
   }
   public Version(String name, String t) {
      this(name, RecordParser.getString(VERSION.matcher(t)), RecordParser.getString(MAYOR.matcher(t)),
               RecordParser.getString(MINOR.matcher(t)), RecordParser.getString(PATCH.matcher(t)));
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
   public String toShortString() {
      StringBuilder builder=new StringBuilder();
      if (name == null)
         builder.append("Version ");
      else
         builder.append(name).append(" ");
      builder.append(version);
      return builder.toString();
   }
   @Override
   public String toString() {
      StringBuilder builder=new StringBuilder();
      if (name == null)
         builder.append("Version [");
      else
         builder.append(name).append(" [");
      builder.append("version=").append(version);
      builder.append(", maj=").append(major);
      builder.append(", min=").append(minor);
      builder.append(", p=").append(patch);
      builder.append("]");
      return builder.toString();
   }
}
