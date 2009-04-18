/*
 * Copyright (C) 2005 Rob Manning
 * manningr@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package net.sourceforge.squirrel_sql.plugins.mssql.gui;

import net.sourceforge.squirrel_sql.client.plugin.PluginException;
import net.sourceforge.squirrel_sql.client.plugin.gui.AbstractPluginPreferencesUITest;
import net.sourceforge.squirrel_sql.client.plugin.gui.PluginQueryTokenizerPreferencesPanel;
import net.sourceforge.squirrel_sql.fw.preferences.IQueryTokenizerPreferenceBean;
import net.sourceforge.squirrel_sql.plugins.mssql.prefs.MSSQLPreferenceBean;

public class PreferencesPanelUITest extends AbstractPluginPreferencesUITest{


	/**
    * The main method is not used at all in the test - it is just here to allow for user interaction testing
    * with the graphical component, which doesn't require launching SQuirreL.
    * 
    * @param args
    */	
	public static void main(String[] args) throws Exception {
		new PreferencesPanelUITest().constructTestFrame().setVisible(true);
	}
	
	@Override
   protected IQueryTokenizerPreferenceBean getPreferenceBean()
   {
	   return new MSSQLPreferenceBean();
   }

	@Override
   protected PluginQueryTokenizerPreferencesPanel getPrefsPanelToTest() throws PluginException
   {
		return new PluginQueryTokenizerPreferencesPanel(prefsManager, 
         "MS SQL-Server",
         false);
   }

}
