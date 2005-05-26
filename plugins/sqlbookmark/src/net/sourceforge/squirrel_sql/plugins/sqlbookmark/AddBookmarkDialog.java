package net.sourceforge.squirrel_sql.plugins.sqlbookmark;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AddBookmarkDialog extends JDialog
{
   private static final String BM_TITLE = "dialog.add.title";
   private static final String BM_NAME = "dialog.add.name";
   private static final String BM_DESCRIPTION = "dialog.add.description";
   private static final String BM_ENTER_NAME = "dialog.add.entername";
   public static final String BM_ACCESS_HINT = "dialog.add.accesshint";

   private JTextField txtName = new JTextField();
   private JTextField txtDescription = new JTextField();
   private JButton btnOK;
   private JButton btnCancel;
   private static final String BM_CANCEL = "dialog.add.cancel";
   private static final String BM_OK = "dialog.add.ok";
   private SQLBookmarkPlugin plugin;
   private boolean ok;


   public AddBookmarkDialog(Frame frame, SQLBookmarkPlugin plugin)
   {
      super(frame, plugin.getResourceString(BM_TITLE),true);
      this.plugin = plugin;

      createUI();

      btnOK.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            onOK();
         }
      });

      btnCancel.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            onCancel();
         }
      });
   }

   private void onCancel()
   {
      closeDialog();
   }

   private void onOK()
   {
      if(null == txtName.getText() || 0 == txtName.getText().trim().length())
      {
         JOptionPane.showMessageDialog(this, plugin.getResourceString(BM_ENTER_NAME));
         return;
      }
      ok = true;

      closeDialog();
   }

   private void closeDialog()
   {
      setVisible(false);
      dispose();
   }

   private void createUI()
   {
      getContentPane().setLayout(new GridBagLayout());

      GridBagConstraints gbc;

      gbc = new GridBagConstraints(0,0,1,1,0.0,0.0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5,5,0,0),0,0);
      getContentPane().add(new JLabel(plugin.getResourceString(BM_NAME)), gbc);

      gbc = new GridBagConstraints(1,0,1,1,1.0,0.0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5,5,0,5),0,0);
      getContentPane().add(txtName, gbc);

      gbc = new GridBagConstraints(0,1,1,1,0.0,0.0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5,5,0,0),0,0);
      getContentPane().add(new JLabel(plugin.getResourceString(BM_DESCRIPTION)), gbc);

      gbc = new GridBagConstraints(1,1,2,1,1.0,0.0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5,5,0,5),0,0);
      getContentPane().add(txtDescription, gbc);


      gbc = new GridBagConstraints(0,2,3,1,1.0,0.0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5,5,0,5),0,0);
      JLabel lblAccesshint = new JLabel(plugin.getResourceString(BM_ACCESS_HINT));
      lblAccesshint.setForeground(Color.red);
      getContentPane().add(lblAccesshint, gbc);


      JPanel pnlButtons = new JPanel(new GridBagLayout());

      btnOK = new JButton(plugin.getResourceString(BM_OK));
      gbc = new GridBagConstraints(0,0,1,1,0.0,0.0,GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(0,0,0,5),0,0);
      pnlButtons.add(btnOK, gbc);

      btnCancel = new JButton(plugin.getResourceString(BM_CANCEL));
      gbc = new GridBagConstraints(1,0,1,1,0.0,0.0,GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(0,0,0,0),0,0);
      pnlButtons.add(btnCancel, gbc);


      gbc = new GridBagConstraints(2,3,1,1,1.0,0.0,GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(5,5,5,5),0,0);
      getContentPane().add(pnlButtons, gbc);

      getRootPane().setDefaultButton(btnOK);

      txtName.requestFocus();

      setSize(430, 130);
   }

   public boolean isOK()
   {
      return ok;
   }

   public String getDescription()
   {
      return txtDescription.getText().trim();
   }

   public String getBookmarkName()
   {
      return txtName.getText().trim();
   }
}
