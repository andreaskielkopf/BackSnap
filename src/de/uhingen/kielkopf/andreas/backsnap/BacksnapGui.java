/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.*;

/**
 * @author Andreas Kielkopf
 *
 */
public class BacksnapGui {
   JFrame frame;
   private JPanel panel;
   private JLabel lblNewLabel;
   /**
    * @param args
    */
   public static void main(String[] args) {
      EventQueue.invokeLater(() -> {
         try {
            final BacksnapGui window=new BacksnapGui();
            window.frame.setVisible(true);
         } catch (final Exception e2) {
            e2.printStackTrace();
         }
      });
   }
   /**
    * Create the application.
    * 
    * @wbp.parser.entryPoint
    */
   public BacksnapGui() {
      initialize();
   }
   /**
    * 
    */
   private void initialize() {
      frame=new JFrame();
      frame.setBounds(100, 100, 800, 650);
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      frame.getContentPane().add(getPanel(), BorderLayout.NORTH);
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getLblNewLabel(), BorderLayout.NORTH);
      }
      return panel;
   }
   private JLabel getLblNewLabel() {
      if (lblNewLabel == null) {
         lblNewLabel=new JLabel("BacksnapGui");
         lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
      }
      return lblNewLabel;
   }
}
