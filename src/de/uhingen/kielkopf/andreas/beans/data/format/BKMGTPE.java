/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.data.format;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andreas Kielkopf Formatiert beliebige Zahlen nach folgenden Regeln: 1 -> 1B 10 -> 10B 100 -> .1K 1000 -> 1K 10000 -> 10K 100000 -> .1M 9 ->
 *         9B 99 -> 99B 949 -> .9K 9499 -> 9K 99499 -> 99K 949999 -> .9M
 */
public class BKMGTPE {
   static String        NAMES="KMGTPE";
   static Pattern       SIZE =Pattern.compile("([0-9.,]+)([KMGTPEi]*)( ?B)");
   static DecimalFormat df31 =new DecimalFormat("  0");
   static DecimalFormat df32 =new DecimalFormat(" 00");
   static DecimalFormat df33 =new DecimalFormat("000");
   static DecimalFormat df3a =new DecimalFormat("0.0");
   static DecimalFormat df41 =new DecimalFormat("   0");
   static DecimalFormat df42 =new DecimalFormat("  00");
   static DecimalFormat df43 =new DecimalFormat(" 000");
   static DecimalFormat df44 =new DecimalFormat("0000");
   static DecimalFormat df4a =new DecimalFormat("0.00");
   static DecimalFormat df4b =new DecimalFormat("00.0");
   /**
    * Formatiert beliebige Zahlen nach folgenden Regeln mit 3 Ziffern K=1024
    * 
    * <pre>
    *       1 ->   1B 
    *      10 ->  10B 
    *     100 -> 100B
    *    1000 -> 1.0K 
    *   10000 ->  10K 
    *  100000 -> 100K
    * 1000000 -> 1.0M  
    *       9 ->   9B 
    *      99 ->  99B 
    *     999 -> 999B 
    *    9999 -> 9.9K 
    *   99999 ->  99K 
    *  999999 -> 999K
    * 9999999 -> 9.9M
    * </pre>
    * 
    * @throws DataFormatException
    */
   public static String drei1024(long zahl) {
      return drei(zahl, true);
   }
   public static String drei1000(long zahl) {
      return drei(zahl, false);
   }
   /**
    * 
    * @param zahl
    * @param k
    *           1024 oder 1000
    * @return
    */
   public static String drei(long zahl, boolean k) {
      StringBuilder sb=new StringBuilder();
      Double z=(double) zahl;
      int p=0;
      while (z > 995d) {
         p++;
         z/=k ? 1024L : 1000L;
      }
      sb.append((p == 0) ? switch (z) {
         case Double d when d < 9.5d:
            yield df31.format(z);
         case Double d when d < 99.5d:
            yield df32.format(z);
         default:
            yield df33.format(z);
      } : switch (z) {
         case Double d when d >= 99.5d:
            yield df33.format(z);
         case Double d when d >= 9.5d:
            yield df32.format(z);
         default:
            yield df3a.format(z);
      });
      // if (sb.length() != 3)
      // System.err.println("Zu lang: " + zahl);
      if (p == 0)
         sb.append(k ? "Byt" : "By");
      else
         sb.append(NAMES.charAt(p - 1)).append(k ? "iB" : "B");
      return sb.toString();
   }
   /**
    * Formatiert beliebige Zahlen nach folgenden Regeln mit 4 Ziffern
    * 
    * <pre>
    *        1 ->    1B 
    *       10 ->   10B 
    *      100 ->  100B
    *     1000 -> 1.00K 
    *    10000 -> 10.0K 
    *   100000 ->  100K
    *  1000000 -> 1.10M
    * 10000000 -> 10.0M  
    *        9 ->    9B 
    *       99 ->   99B 
    *      999 ->  999B 
    *     9999 -> 9.99K 
    *    99999 -> 99.9K 
    *   999999 ->  999K
    *  9999999 ->  9.9M
    * 99999999 -> 99,9M
    * </pre>
    * 
    * @throws DataFormatException
    */
   public static String vier1024(long zahl) {
      return vier(zahl, true);
   }
   public static String vier1000(long zahl) {
      return vier(zahl, false);
   }
   /**
    * 
    * @param zahl
    * @param k
    *           1024L oder 1000L
    * @return
    */
   public static String vier(long zahl, boolean k) {
      StringBuilder sb=new StringBuilder();
      Double z=(double) zahl;
      int p=0;
      while (z > 994d) {
         p++;
         z/=k ? 1024L : 1000L;
      }
      sb.append((p == 0) ? switch (z) {
         case Double d when d < 9.5d:
            yield df41.format(z);
         case Double d when d < 99.5d:
            yield df42.format(z);
         case Double d when d < 999.5d:
            yield df43.format(z);
         default:
            yield df44.format(z);
      } : switch (z) {
         case Double d when d < 9.99d:
            yield df4a.format(z);
         case Double d when d < 99.9d:
            yield df4b.format(z);
         case Double d when d < 999.5d:
            yield df43.format(z);
         default:
            yield df44.format(z);
      });
      // if (sb.length() != 4)
      // throw new DataFormatException("Zu lang: " + zahl);
      if (p == 0)
         sb.append(k ? "Byt" : "By");
      else
         sb.append(NAMES.charAt(p - 1)).append(k ? "iB" : "B");
      return sb.toString();
   }
   public static long getSize(String text) {
      Matcher s=SIZE.matcher(text.replaceAll(",", "."));
      if (s.find()) {
         try {
            double f=switch (s.group(2)) {
               case "Ei":
                  yield 1024L * 1024L * 1024L * 1024L * 1024L * 1024L;
               case "E":
                  yield 1000L * 1000L * 1000L * 1000L * 1000L * 1000L;
               case "Pi":
                  yield 1024L * 1024L * 1024L * 1024L * 1024L;
               case "P":
                  yield 1000L * 1000L * 1000L * 1000L * 1000L;
               case "Ti":
                  yield 1024L * 1024L * 1024L * 1024L;
               case "T":
                  yield 1000L * 1000L * 1000L * 1000L;
               case "Gi":
                  yield 1024L * 1024L * 1024L;
               case "G":
                  yield 1000L * 1000L * 1000L;
               case "Mi":
                  yield 1024L * 1024L;
               case "M":
                  yield 1000L * 1000L;
               case "Ki":
                  yield 1024L;
               case "K":
                  yield 1000L;
               default:
                  yield 1L;
            };
            return (long) (Double.parseDouble(s.group(1)) * f);
         } catch (NumberFormatException e) {
            e.printStackTrace(); // throw new DataFormatException(text);
         }
      }
      return 0;
   }
   // public static void main(String[] args) {
   // try {
   // for (double d=1.45; d < Long.MAX_VALUE; d=d * 1.01d + 1d) {
   // long l=(long) d;
   // System.out.print(drei1024(l) + " : " + vier1024(l) + " : ");
   // System.out.print(drei1000(l) + " : " + vier1000(l) + " : ");
   // System.out.print(getSize(drei1024(l)) + " - " + getSize(vier1024(l)) + " - ");
   // System.out.print(getSize(drei1000(l)) + " - " + getSize(vier1000(l)) + " - ");
   // System.out.println(l);
   // }
   // } catch (DataFormatException e) {
   // e.printStackTrace();
   // }
   // }
}
