package net.sourceforge.squirrel_sql.client.gui.db.aliasproperties;

import net.sourceforge.squirrel_sql.client.gui.BaseInternalFrame;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

public class AliasPropertiesInternalFrame extends BaseInternalFrame
{
   private static final String PREF_KEY_ALIAS_PROPS_SHEET_WIDTH = "Squirrel.aliasPropsSheetWidth";
   private static final String PREF_KEY_ALIAS_PROPS_SHEET_HEIGHT = "Squirrel.aliasPropsSheetHeight";

   private static final StringManager s_stringMgr =
      StringManagerFactory.getStringManager(AliasPropertiesInternalFrame.class);


   JTabbedPane tabPane;
   JButton btnOk;
   JButton btnClose;

   AliasPropertiesInternalFrame()
   {
      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
      GUIUtils.makeToolWindow(this, true);
      setSize(getDimension());
      setResizable(true);

      getContentPane().setLayout(new GridBagLayout());

      GridBagConstraints gbc;

      tabPane = new JTabbedPane();
      gbc = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0);
      getContentPane().add(tabPane, gbc);

      JPanel pnlButtons = new JPanel();
      gbc = new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0);
      getContentPane().add(pnlButtons, gbc);

      // i18n[AliasPropertiesInternalFrame.ok=OK]
      btnOk = new JButton(s_stringMgr.getString("AliasPropertiesInternalFrame.ok"));
      pnlButtons.add(btnOk);

      // i18n[AliasPropertiesInternalFrame.close=Close]
      btnClose = new JButton(s_stringMgr.getString("AliasPropertiesInternalFrame.close"));
      pnlButtons.add(btnClose);
   }

   private Dimension getDimension()
   {
      return new Dimension(
         Preferences.userRoot().getInt(PREF_KEY_ALIAS_PROPS_SHEET_WIDTH, 500),
         Preferences.userRoot().getInt(PREF_KEY_ALIAS_PROPS_SHEET_HEIGHT, 600)
      );
   }


   public void dispose()
   {
      Dimension size = getSize();
      Preferences.userRoot().putInt(PREF_KEY_ALIAS_PROPS_SHEET_WIDTH, size.width);
      Preferences.userRoot().putInt(PREF_KEY_ALIAS_PROPS_SHEET_HEIGHT, size.height);

      super.dispose();
   }

}
