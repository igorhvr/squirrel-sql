package net.sourceforge.squirrel_sql.plugins.hibernate.configuration;

import net.sourceforge.squirrel_sql.fw.gui.MultipleLineLabel;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ProcessDetailsDialog extends JDialog
{
   private static final StringManager s_stringMgr =
      StringManagerFactory.getStringManager(ProcessDetailsDialog.class);
   JButton btnOk;
   JButton btnCancel;
   JEditorPane txtCommand;
   JCheckBox chkEndProcessOnDisconnect;
   JButton btnRestoreDefault;
   JButton btnCopyCmndToClip;

   public ProcessDetailsDialog(JFrame mainFrame)
   {
      super(mainFrame, s_stringMgr.getString("ProcessDetailsDialog.title"), true);

      getContentPane().setLayout(new GridBagLayout());

      GridBagConstraints gbc;

      MultipleLineLabel lbl;

      lbl = new MultipleLineLabel(s_stringMgr.getString("ProcessDetailsDialog.processDesc"));
      gbc = new GridBagConstraints(0,0,1,1,0,0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(5,5,5,5), 0,0);
      getContentPane().add(lbl, gbc);

      txtCommand = new JTextPane();
      gbc = new GridBagConstraints(0,1,1,1,1,1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(5,5,0,5), 0,0);
      getContentPane().add(new JScrollPane(txtCommand), gbc);

      gbc = new GridBagConstraints(0,2,1,1,0,0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0,5,5,5), 0,0);
      getContentPane().add(createCommandButtons(), gbc);


      lbl = new MultipleLineLabel(s_stringMgr.getString("ProcessDetailsDialog.endProcessDesc"));
      gbc = new GridBagConstraints(0,3,1,1,0,0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5,5,5,5), 0,0);
      getContentPane().add(lbl, gbc);

      chkEndProcessOnDisconnect= new JCheckBox(s_stringMgr.getString("ProcessDetailsDialog.chkEndProcess"));
      gbc = new GridBagConstraints(0,4,1,1,0,0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5,5,5,5), 0,0);
      getContentPane().add(chkEndProcessOnDisconnect, gbc);

      gbc = new GridBagConstraints(0,5,1,1,0,0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(5,5,5,5), 0,0);
      getContentPane().add(createButtonPanel(), gbc);


      setSize(600, 450);


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

   private JPanel createCommandButtons()
   {
      JPanel ret = new JPanel(new GridBagLayout());

      GridBagConstraints gbc;

      btnRestoreDefault = new JButton(s_stringMgr.getString("ProcessDetailsDialog.restoreDefault"));
      gbc = new GridBagConstraints(0,0,1,1,0,0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0,0,5,5), 0,0);
      ret.add(btnRestoreDefault, gbc);

      gbc = new GridBagConstraints(1,0,1,1,0,0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0,5,5,5), 0,0);
      btnCopyCmndToClip = new JButton(s_stringMgr.getString("ProcessDetailsDialog.RefreshRealCmndCopyToClip"));
      ret.add(btnCopyCmndToClip, gbc);

      return ret;
   }

   private JPanel createButtonPanel()
   {
      JPanel ret = new JPanel(new GridBagLayout());

      GridBagConstraints gbc;

      btnOk = new JButton(s_stringMgr.getString("ProcessDetailsDialog.ok"));
      gbc = new GridBagConstraints(0,0,1,1,0,0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(15,5,5,5), 0,0);
      ret.add(btnOk, gbc);

      btnCancel = new JButton(s_stringMgr.getString("ProcessDetailsDialog.cancel"));
      gbc = new GridBagConstraints(1,0,1,1,0,0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(15,5,5,5), 0,0);
      ret.add(btnCancel, gbc);

      gbc = new GridBagConstraints(2,0,1,1,1,1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(15,5,5,5), 0,0);
      ret.add(new JPanel(), gbc);

      return ret;
   }
}
