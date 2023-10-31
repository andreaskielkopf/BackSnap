/**
 * Andreas Kielkopf 
 */
package de.uhingen.kielkopf.andreas.beans.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JLabel;

import de.uhingen.kielkopf.andreas.beans.Version;

/**
 * @author Andreas Kielkopf
 *
 */
public class Rotator extends JLabel {
   private static final long serialVersionUID=7534694490229028604L;
   ExecutorService       virtual=Version.getVx();
   private ReentrantLock running=new ReentrantLock();
   /**
   * 
   */
   public Rotator() {
      // TODO Auto-generated constructor stub
   }
   @Override
   protected void paintComponent(Graphics g1) {
      if (g1 instanceof Graphics2D g) {
         int w=getWidth();
         int h=getHeight();
         int n=(int) System.currentTimeMillis();
         int b=((n * 360) / 60_000) % 360;
         g.setColor(getBackground());
         g.fillArc(0, 0, w, h, -b, 18);
         int f=((n * 360) / 5_000) % 360;
         g.setColor(getForeground());
         g.fillArc(0, 0, w, h, -f, 18);
      }
      if (!running.isLocked())
         virtual.execute(() -> {
            running.lock();
            try {
               for (int i=200; i > 0; i--)
                  try {
                     repaint(25);
                     Thread.sleep(25);
                  } catch (InterruptedException ignore) {/* */ }
            } finally {
               running.unlock();
               repaint(10);
            }
         });
   }
}
