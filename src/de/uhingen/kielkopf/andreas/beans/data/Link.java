/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.data;

import java.lang.ref.SoftReference;

/**
 * @author Andreas Kielkopf
 */
public class Link<T> {
   private SoftReference<T> sr=null;
   final private String     name;
   // private @NonNull Class<? extends @NonNull Object> clazz;
   @SuppressWarnings("null")
   public Link(String n) {
      this(n, null);
   }
   public Link(String n, T t) {
      name=n;
      set(t);
   }
   @SuppressWarnings("null")
   public T get() {
      if (sr == null)
         return null;
      return sr.get();
   }
   public T set(T t) {
      sr=(t == null) ? null : new SoftReference<T>(t);
      return t;
   }
   public void clear() {
      sr=null;
   }
   @SuppressWarnings("null")
   @Override
   public String toString() {
      StringBuilder sb=new StringBuilder(name).append("[");
      T l=get();
      if (l == null)
         sb.append("null");
      else
         sb.append(get().toString());
      sb.append("]");
      return sb.toString();
   }
}
