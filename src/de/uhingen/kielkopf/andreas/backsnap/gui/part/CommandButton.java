/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.gui.part;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;

/**
 * @author Andreas Kielkopf
 *
 */
public class CommandButton extends JPanel {
   private static final long serialVersionUID=-917266146931312618L;
   private JPanel            panel;
   private JLabel            lblOk;
   private JTextArea         textArea;
   private JScrollPane       scrollPane;
   private JButton           btnCommand;
   private JPanel            panel_1;
   private TitledBorder      tBorder;
   /**
    * Create the panel.
    */
   public CommandButton() {
      initialize();
   }
   /**
    * @param string
    */
   public CommandButton(String string) {
      setTitle(string);
   }
   /**
    * @param string
    */
   public void setTitle(String string) {
      getTitledBorder().setTitle(string);
   }
   private void initialize() {
      setLayout(new BorderLayout(0, 0));
      add(getPanel(), BorderLayout.CENTER);
   }
   private JPanel getPanel() {
      if (panel == null) {
         panel=new JPanel();
         // panel.setBorder(new TitledBorder(null, "x. Command", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panel.setBorder(getTitledBorder());
         panel.setLayout(new BorderLayout(0, 0));
         panel.add(getScrollPane(), BorderLayout.CENTER);
         panel.add(getPanel_1(), BorderLayout.NORTH);
      }
      return panel;
   }
   /**
    * @return
    */
   private TitledBorder getTitledBorder() {
      if (tBorder == null) {
         tBorder=new TitledBorder(null, "x. Command", TitledBorder.LEADING, TitledBorder.TOP, null, null);
      }
      return tBorder;
   }
   private JLabel getLblOk() {
      if (lblOk == null) {
         lblOk=new JLabel("detailed command");
      }
      return lblOk;
   }
   private JTextArea getTextArea() {
      if (textArea == null) {
         textArea=new JTextArea();
         textArea.setText("...");
         textArea.setEditable(false);
      }
      return textArea;
   }
   private JScrollPane getScrollPane() {
      if (scrollPane == null) {
         scrollPane=new JScrollPane(getTextArea());
         scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
         scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
      }
      return scrollPane;
   }
   private JButton getBtnCommand() {
      if (btnCommand == null) {
         btnCommand=new JButton("execute");
      }
      return btnCommand;
   }
   private JPanel getPanel_1() {
      if (panel_1 == null) {
         panel_1=new JPanel();
         panel_1.setLayout(new BorderLayout(0, 0));
         panel_1.add(getLblOk(), BorderLayout.CENTER);
         panel_1.add(getBtnCommand(), BorderLayout.WEST);
      }
      return panel_1;
   }
}
