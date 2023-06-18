/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui;

import java.awt.EventQueue;
import java.awt.BorderLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import java.awt.Color;

/**
 * @author Andreas Kielkopf
 *
 */
public class Config {
   private JFrame frame;
   private JPanel computers;
   private JPanel config;
   private Computer computer_1;
   private Computer backup;
   /**
    * Launch the application.
    */
   public static void main(String[] args) {
      EventQueue.invokeLater(new Runnable() {
         @Override
         public void run() {
            try {
               Config window=new Config();
               window.frame.setVisible(true);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
   }
   /**
    * Create the application.
    */
   public Config() {
      initialize();
   }
   /**
    * Initialize the contents of the frame.
    */
   private void initialize() {
      frame=new JFrame();
      frame.setBounds(100, 100, 800, 650);
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      frame.getContentPane().add(getConfig(), BorderLayout.CENTER);
   }
   private JPanel getComputers() {
      if (computers == null) {
      	computers = new JPanel();
      	computers.setBorder(new TitledBorder(null, "Backup from:", TitledBorder.LEADING, TitledBorder.TOP, null, null));
      	computers.setLayout(new BoxLayout(computers, BoxLayout.Y_AXIS));
      	computers.add(getComputer_1());
      }
      return computers;
   }
   private JPanel getConfig() {
      if (config == null) {
      	config = new JPanel();
      	config.setBorder(new TitledBorder(null, "Config for BackSnap", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(59, 59, 59)));
      	config.setLayout(new BorderLayout(0, 0));
      	config.add(getBackup(), BorderLayout.NORTH);
      	config.add(getComputers(), BorderLayout.CENTER);
      }
      return config;
   }
   private Computer getComputer_1() {
      if (computer_1 == null) {
      	computer_1 = new Computer();
      	
      }
      return computer_1;
   }
   private Computer getBackup() {
      if (backup == null) {
      	backup = new Computer();
      	backup.setTitel("Backup");
      }
      return backup;
   }
}
