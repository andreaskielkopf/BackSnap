/**
 * 
 */
package de.uhingen.kielkopf.andreas.beans.gui;

import java.awt.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JFrame;

import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Andreas Kielkopf
 *
 */
public class Prefs {
   @Nullable final private Preferences pref;
   /*
    *  
    */
   public Prefs(Class<?> c) {
      pref=Preferences.userNodeForPackage(c);
   }
   final String FRAME_X="frame_X";
   final String FRAME_Y="frame_Y";
   final String FRAME_W="frame_W";
   final String FRAME_H="frame_H";
   public void saveFramePos(JFrame frame) {
      if (frame instanceof JFrame f)
         if (pref instanceof Preferences p) {
            Rectangle b=f.getBounds();
            p.putInt(FRAME_X, b.x);
            p.putInt(FRAME_Y, b.y);
            p.putInt(FRAME_W, b.width);
            p.putInt(FRAME_H, b.height);
            try {
               p.flush();
            } catch (BackingStoreException ignore) { /* */ }
         }
   }
   public void restoreFramePos(JFrame frame) {
      if (frame instanceof JFrame f)
         if (pref instanceof Preferences p) {
            try {
               p.sync();
            } catch (BackingStoreException ignore) {/* */ }
            Dimension screenSize=Toolkit.getDefaultToolkit().getScreenSize();
            int width=Math.clamp(screenSize.width, 0, p.getInt(FRAME_W, (3840 * 40) / 100));
            int height=Math.clamp(screenSize.height, 0, p.getInt(FRAME_H, (2160 * 40) / 100));
            int restx=screenSize.width - width;
            int resty=screenSize.height - height;
            int x=Math.clamp(p.getInt(FRAME_X, restx / 2), 0, restx);
            int y=Math.clamp(p.getInt(FRAME_Y, resty / 2), 0, resty);
            f.setBounds(x, y, width, height);
         }
   }
}
