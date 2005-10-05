package net.sourceforge.squirrel_sql.plugins.editextras;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;


public class EscapeDateFrame extends JDialog
{
    JTextField txtYear = new JTextField();
    JTextField txtMonth = new JTextField();
    JTextField txtDay = new JTextField();
    JTextField txtHour = new JTextField();
    JTextField txtMinute = new JTextField();
    JTextField txtSecond = new JTextField();
    JButton btnTimestamp = new JButton("Time stamp");
    JButton btnDate = new JButton("Date");
    JButton btnTime = new JButton("Time");

   public EscapeDateFrame(Frame owner)
   {
      super(owner, "Escape date");

      JPanel pnlEdit = new JPanel();

      pnlEdit.setLayout(new GridLayout(6,2));

      pnlEdit.add(new JLabel("Year"));
      pnlEdit.add(txtYear);
      pnlEdit.add(new JLabel("Month"));
      pnlEdit.add(txtMonth);
      pnlEdit.add(new JLabel("Day"));
      pnlEdit.add(txtDay);
      pnlEdit.add(new JLabel("Hour"));
      pnlEdit.add(txtHour);
      pnlEdit.add(new JLabel("Minute"));
      pnlEdit.add(txtMinute);
      pnlEdit.add(new JLabel("Second"));
      pnlEdit.add(txtSecond);

      JPanel pnlButtons = new JPanel(new GridLayout(3,1));
      pnlButtons.add(btnTimestamp);
      pnlButtons.add(btnDate);
      pnlButtons.add(btnTime);

      JPanel pnlMain = new JPanel();
      pnlMain.setLayout(new BorderLayout());

      pnlMain.add(pnlEdit, BorderLayout.CENTER);
      pnlMain.add(pnlButtons, BorderLayout.SOUTH);

      getContentPane().add(pnlMain);
      setSize(250, 250);

      getRootPane().setDefaultButton(btnTimestamp);

      AbstractAction closeAction = new AbstractAction()
      {
         public void actionPerformed(ActionEvent actionEvent)
         {
            setVisible(false);
            dispose();
         }
      };
      KeyStroke escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
      getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escapeStroke, "CloseAction");
      getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeStroke, "CloseAction");
      getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(escapeStroke, "CloseAction");
      getRootPane().getActionMap().put("CloseAction", closeAction);


   }
}

