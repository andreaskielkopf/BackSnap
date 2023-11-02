/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andreas Kielkopf
 *
 */
public class Flag {
   static private List<Flag>   flagList     =new CopyOnWriteArrayList<>();
   static private String       args         =" ";
   static private List<String> parameterList=null;
   final private Character     kurz;                                      // kurz
   final private String        lang;                                      // name
   private String              param;                                     // parameter
   private Boolean             flag         =null;
   private boolean             standard     =false;
   public Flag(String lang1) {
      this(null, lang1, null);
   }
   public Flag(char kurz1) {
      this(Character.valueOf(kurz1), null, null);
   }
   public Flag(char kurz1, String lang1) {
      this(Character.valueOf(kurz1), lang1, null);
   }
   public Flag(char kurz1, String lang1, String parameter1) {
      this(Character.valueOf(kurz1), lang1, parameter1);
   }
   private Flag(Character kurz1, String lang1, String parameter1) {
      kurz=kurz1;
      lang=lang1;
      param=parameter1;
      flagList.add(this);
   }
   /**
    * @param args
    *           vom System übergeben
    * @param standard_args
    *           (wenn keine anderen übergeben wurden)
    */
   public static void setArgs(String[] args1, String standard_args) {
      args=String.join(" ", args1);
      if (args.isBlank())
         args=standard_args;
      args=" " + args + " ";
      parameterList=null;
      for (Flag flag:flagList) {
         flag.param=null;
         flag.flag=null;
         flag.standard=false;
      }
   }
   /**
    * @return
    */
   public static String getArgs() {
      return args;
   }
   /**
    * Lazy auswertung der Flags (optimiert)
    *
    * @return boolean ist dieses Flag gesetzt
    */
   public boolean get() {
      if (flag == null) {
         flag=false;
         if (lang != null) {
            final String findLong=".* --" + lang.toLowerCase().replaceAll("_", "-") + " .*";
            flag|=args.matches(findLong);
         }
         if (kurz != null) {
            final String findShort=".* -[a-z]*" + kurz + "[a-z]* .*";
            flag|=args.matches(findShort);
            final String findShortP=".* -" + kurz + "=.*";
            flag|=args.matches(findShortP);
         }
         flag|=standard;
      }
      return flag;
   }
   /**
    * Lazy auswertung der Parameter (optimiert)
    *
    * @return
    */
   public String getParameter() {
      if (param == null) {
         if (kurz != null) {
            final String findShort=" -" + kurz + "=([^- =]+)";
            final Matcher ma=Pattern.compile(findShort).matcher(args);
            if (ma.find())
               param=ma.group(1);
         }
         if (lang != null) {
            final String findLong=" --" + lang.toLowerCase().replaceAll("_", "-") + "=([^- =]+)";
            final Matcher ma2=Pattern.compile(findLong).matcher(args);
            if (ma2.find())
               param=ma2.group(1);
         }
      }
      return param;
   }
   public Object getParameterOrDefault(Object ersatz) {
      String parameter=getParameter();
      if ((parameter == null) || parameter.isBlank())
         return ersatz;
      if (ersatz instanceof Integer)
         try {
            return Integer.decode(parameter);
         } catch (Exception ignore) { /* Dann wars eben kein Integer */
            return ersatz;
         }
      return parameter;
   }
   /**
    * Lazy get Parameters from commandline
    * 
    * @param nr
    * @return
    */
   public static String getParameter(int nr) {
      return getParameterOrDefault(nr, "");
   }
   public static String getParameterOrDefault(int nr, String standard) {
      if (parameterList == null)
         getParameterList();
      return (nr + 1 > parameterList.size()) ? standard : parameterList.get(nr);
   }
   public static List<String> getParameterList() {
      if (parameterList == null) {
         parameterList=new ArrayList<>();
         final String findNonFlags=" [^ -][^ ]*";
         final Matcher ma3=Pattern.compile(findNonFlags).matcher(args);
         while (ma3.find())
            parameterList.add(ma3.group().trim());
      }
      return new ArrayList<>(parameterList); // gib das Original nicht aus der Hand
   }
   /**
    * Manuelles setzen oder löschen von Flags
    *
    * @param b
    * @return
    */
   public void set(boolean b1) {
      flag=b1;
   }
   /**
    * Manuelles setzen oder löschen von Flags
    *
    * @param b
    * @return
    */
   public Flag setDefault(boolean b1) {
      standard=b1;
      return this;
   }
   /**
    * Main um tests zu fahren
    * 
    * @param argumente
    */
   public static void main(String[] argumente) {
      Flag.setArgs(argumente, "-a test test1 -c /home /usr/local/bin");
      for (String p:getParameterList())
         System.out.println(p);
   }
   /**
    * @param string
    */
   public void setParameter(String p) {
      param=p;
   }
   static public final int parseIntOrDefault(String s, int def) {
      if (s != null)
         try {
            return Integer.parseInt(s);
         } catch (NumberFormatException ignore) {
            System.err.println(ignore.getMessage() + ":" + s);
         }
      return def;
   }
   @Override
   public String toString() {
      return new StringBuilder("Flag[")//
               .append(kurz).append(" ")//
               .append(lang).append(" ")//
               .append(param).append(" ")//
               .append("]").toString();
   }
}
