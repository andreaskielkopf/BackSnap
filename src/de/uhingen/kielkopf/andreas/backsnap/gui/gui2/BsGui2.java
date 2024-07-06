/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.gui2;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.border.TitledBorder;

import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import javax.swing.JTextField;
import javax.swing.JLabel;

/**
 * @author Andreas Kielkopf
 *
 */
public class BsGui2 {
   public static BsGui2 bsGui2=null;
   private JFrame       frame;
   private PanelMain    panelMain;
   private PanelTop     panelTop;
   private PanelBottom  panelBottom;
   /**
    * Launch the application.
    */
   public static void main(String[] args) {
      EventQueue.invokeLater(new Runnable() {
         public void run() {
            try {
               if (bsGui2 == null)
                  bsGui2=new BsGui2();
               bsGui2.frame.setVisible(true);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
   }
   /**
    * Create the application.
    */
   public BsGui2() {
      initialize();
   }
   /**
    * Initialize the contents of the frame.
    */
   private void initialize() {
      frame=new JFrame();
      frame.setBounds(100, 100, 800, 650);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.getContentPane().add(getPanelMain(), BorderLayout.CENTER);
      frame.getContentPane().add(getPanelTop(), BorderLayout.NORTH);
      frame.getContentPane().add(getPanelBottom(), BorderLayout.SOUTH);
   }
   private PanelMain getPanelMain() {
      if (panelMain == null) {
         panelMain=new PanelMain();
         panelMain.setBackground(Color.YELLOW);
         // panelMain.setLayout(new BorderLayout(0, 0));
      }
      return panelMain;
   }
   private PanelTop getPanelTop() {
      if (panelTop == null) {
         panelTop=new PanelTop();
         panelTop.setBackground(Color.GREEN);
         // panelTop.setLayout(new BoxLayout(panelTop, BoxLayout.X_AXIS));
      }
      return panelTop;
   }
   private PanelBottom getPanelBottom() {
      if (panelBottom == null) {
         panelBottom=new PanelBottom();
         // panelBottom.setBackground(Color.ORANGE);
         // panelBottom.setLayout(new BorderLayout(0, 0));
      }
      return panelBottom;
   }
}
