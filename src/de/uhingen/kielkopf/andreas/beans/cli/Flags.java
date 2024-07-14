/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andreas Kielkopf
 * 
 *         Vom statischen Objhekt zu einzelnen Objekten umbauen
 */
public class Flags {
   public class F {
      // final private String name;
      final private Character kurz;          // kurz
      final private String    lang;          // name
      private String          param;         // parameter
      private Boolean         flag    =null;
      private boolean         standard=false;
      private F(Character kurz1, String lang1, String parameter1) {
         kurz=kurz1;
         lang=lang1;
         param=parameter1;
      }
      public F(String lang1) {
         this(null, lang1, null);
      }
      public F(char kurz1) {
         this(Character.valueOf(kurz1), "" + kurz1, null);
      }
      public F(char kurz1, String lang1) {
         this(Character.valueOf(kurz1), lang1, null);
      }
      public F(char kurz1, String lang1, String parameter1) {
         this(Character.valueOf(kurz1), lang1, parameter1);
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
       * Manuelles setzen oder löschen von Flags
       *
       * @param b
       * @return
       */
      public F setDefault(boolean b1) {
         standard=b1;
         return this;
      }
      /**
       * @param string
       */
      public void setParameter(String p) {
         param=p;
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
   }
   private String                           args         =" ";
   private List<String>                     parameterList=null;
   private ConcurrentSkipListMap<String, F> flagList     =new ConcurrentSkipListMap<>();
   /**
    * @return
    */
   public String getArgs() {
      return args;
   }
   /**
    * Lazy get Parameters from commandline
    * 
    * @param nr
    * @return
    */
   public String getParameter(int nr) {
      return getParameterOrDefault(nr, "");
   }
   public String getParameterOrDefault(int nr, String standard) {
      if (parameterList == null)
         getParameterList();
      return (nr + 1 > parameterList.size()) ? standard : parameterList.get(nr);
   }
   public List<String> getParameterList() {
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
    * @param args
    *           vom System übergeben
    * @param standard_args
    *           (wenn keine anderen übergeben wurden)
    */
   public void setArgs(String[] args1, String standard_args) {
      args=String.join(" ", args1);
      if (args.isBlank())
         args=standard_args;
      args=" " + args + " ";
      parameterList=null;
      for (F flag:flagList.values()) {
         flag.param=null;
         flag.flag=null;
         flag.standard=false;
      }
   }
   /**
    * Main um tests zu fahren
    * 
    * @param argumente
    */
   public static void main(String[] argumente) {
      Flags flags=new Flags();
      flags.setArgs(argumente, "-a test test1 -c /home /usr/local/bin");
      for (String p:flags.getParameterList())
         System.out.println(p);
   }
   public final int parseIntOrDefault(String s, int def) {
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
      return new StringBuilder("Flags[")//
               // .append(kurz).append(" ")//
               // .append(lang).append(" ")//
               // .append(param).append(" ")//
               .append("]").toString();
   }
   /**
    * @param c
    * @param string
    * @return
    * @return
    */
   public Flags create(char c, String name) {
      F f=new F(c, name);
      if (flagList.putIfAbsent(f.lang, f) != null)
         throw new UnsupportedOperationException("Unable to create Flag(" + f.lang + ") twice");
      return this;
   }
   public F f(String name) {
      return flagList.get(name);
   }
   public boolean get(String name) {
      return f(name).get();
   }
}
