/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.part;

import java.awt.FlowLayout;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import org.eclipse.jdt.annotation.NonNull;

import java.awt.BorderLayout;
import java.awt.Color;

/**
 * @author Andreas Kielkopf
 *
 */
public class BalanceSelect extends JPanel {
   private static final long       serialVersionUID=1L;
   private JPanel                  panel;
   private JPanel                  panel_1;
   private JRadioButton            radioButton_1;
   private JRadioButton            radioButton_2;
   private JRadioButton            radioButton_3;
   private JRadioButton            radioButton_4;
   private JRadioButton            radioButton_5;
   private JRadioButton            radioButton_6;
   private JRadioButton            radioButton_7;
   private JRadioButton            radioButton_0;
   private JRadioButton            radioButton_8;
   private @NonNull ButtonGroup    bg              =new ButtonGroup();
   private ArrayList<JRadioButton> buttons         =new ArrayList<>();
   private JButton                 btnNewButton;
   /**
    * Create the panel.
    */
   public BalanceSelect() {
      initialize();
   }
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getPanel());
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         panel.setBorder(new TitledBorder(null, "Select a Balance and start it", TitledBorder.LEADING, TitledBorder.TOP,
                  null, null));
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getPanel_1(), BorderLayout.CENTER);
         panel.add(getBtnNewButton(), BorderLayout.EAST);
      }
      return panel;
   }
   private JPanel getPanel_1() {
      if (panel_1 == null) {
         panel_1=new JPanel();
         panel_1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
         panel_1.add(getRadioButton_0());
         panel_1.add(getRadioButton_1());
         panel_1.add(getRadioButton_2());
         panel_1.add(getRadioButton_3());
         panel_1.add(getRadioButton_4());
         panel_1.add(getRadioButton_5());
         panel_1.add(getRadioButton_6());
         panel_1.add(getRadioButton_7());
         panel_1.add(getRadioButton_8());
      }
      return panel_1;
   }
   private JRadioButton getRadioButton_0() {
      if (radioButton_0 == null) {
         radioButton_0=basisJRB("10%", Color.YELLOW);
      }
      return radioButton_0;
   }
   private JRadioButton getRadioButton_1() {
      if (radioButton_1 == null) {
         radioButton_1=basisJRB("20%", Color.YELLOW);
      }
      return radioButton_1;
   }
   private JRadioButton getRadioButton_2() {
      if (radioButton_2 == null) {
         radioButton_2=basisJRB("25%", Color.GREEN);
      }
      return radioButton_2;
   }
   private JRadioButton getRadioButton_3() {
      if (radioButton_3 == null) {
         radioButton_3=basisJRB("35%", Color.GREEN);
      }
      return radioButton_3;
   }
   private JRadioButton getRadioButton_4() {
      if (radioButton_4 == null) {
         radioButton_4=basisJRB("50%", Color.GREEN);
         radioButton_4.setSelected(true);
      }
      return radioButton_4;
   }
   private JRadioButton getRadioButton_5() {
      if (radioButton_5 == null) {
         radioButton_5=basisJRB("65%", Color.ORANGE);
      }
      return radioButton_5;
   }
   private JRadioButton getRadioButton_6() {
      if (radioButton_6 == null) {
         radioButton_6=basisJRB("75%", Color.ORANGE);
      }
      return radioButton_6;
   }
   private JRadioButton getRadioButton_7() {
      if (radioButton_7 == null) {
         radioButton_7=basisJRB("80%", Color.RED);
      }
      return radioButton_7;
   }
   private JRadioButton getRadioButton_8() {
      if (radioButton_8 == null) {
         radioButton_8=basisJRB("90%", Color.RED);
      }
      return radioButton_8;
   }
   private JRadioButton basisJRB(String text, Color c) {
      JRadioButton rb=new JRadioButton(text);
      rb.setOpaque(true);
      rb.setBackground(c);
      rb.setSelected(false);
      bg.add(rb);
      buttons.add(rb);
      return rb;
   }
   private JButton getBtnNewButton() {
      if (btnNewButton == null) {
         btnNewButton=new JButton("start balance");
         btnNewButton.setEnabled(false);
      }
      return btnNewButton;
   }
   public void enableUpTo(int max) {
      for (JRadioButton jr:buttons) {
         String nr=jr.getText().replaceAll("%", "");
         int i=Integer.parseInt(nr);
         jr.setEnabled(i <= max);
         if (jr.isSelected())
            jr.setSelected(i <= max);
      }
   }
   public int getSelected() {
      for (JRadioButton jr:buttons)
         if (jr.isSelected()) {
            String nr=jr.getText().replaceAll("%", "");
            int i=Integer.parseInt(nr);
            return i;
         }
      return 0;
   }
}
