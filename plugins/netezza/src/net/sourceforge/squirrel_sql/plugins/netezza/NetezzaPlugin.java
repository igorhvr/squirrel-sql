/*
 * Copyright (C) 2009 Rob Manning
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

package net.sourceforge.squirrel_sql.plugins.netezza;

import net.sourceforge.squirrel_sql.client.plugin.DefaultSessionPlugin;
import net.sourceforge.squirrel_sql.client.plugin.PluginException;
import net.sourceforge.squirrel_sql.client.plugin.PluginQueryTokenizerPreferencesManager;
import net.sourceforge.squirrel_sql.client.plugin.PluginSessionCallback;
import net.sourceforge.squirrel_sql.client.plugin.gui.PluginGlobalPreferencesTab;
import net.sourceforge.squirrel_sql.client.plugin.gui.PluginQueryTokenizerPreferencesPanel;
import net.sourceforge.squirrel_sql.client.preferences.IGlobalPreferencesPanel;
import net.sourceforge.squirrel_sql.client.session.IObjectTreeAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.expanders.SchemaExpander;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.DatabaseObjectInfoTab;
import net.sourceforge.squirrel_sql.fw.dialects.DialectFactory;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.plugins.netezza.exp.NetezzaSequenceInodeExpanderFactory;
import net.sourceforge.squirrel_sql.plugins.netezza.prefs.NetezzaPreferenceBean;
import net.sourceforge.squirrel_sql.plugins.netezza.tab.ProcedureSourceTab;
import net.sourceforge.squirrel_sql.plugins.netezza.tab.ViewSourceTab;

/**
 * The main controller class for the Netezza plugin.
 */
public class NetezzaPlugin extends DefaultSessionPlugin
{

	/**
	 * Internationalized strings for this class.
	 */
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(NetezzaPlugin.class);

	/** manages our query tokenizing preferences */
	private PluginQueryTokenizerPreferencesManager _prefsManager = null;	
	
	static interface i18n
	{
		// i18n[NetezzaPlugin.viewSourceTabHint=Shows the source of the selected view]
		String VIEW_SOURCE_TAB_HINT = s_stringMgr.getString("NetezzaPlugin.viewSourceTabHint");
		
		// i18n[NetezzaPlugin.prefsHint=Preferences for Netezza]
		String PREFS_HINT = s_stringMgr.getString("NetezzaPlugin.prefsHint");
		
	}

	@Override
	public String getAuthor()
	{
		return "Rob Manning";
	}

	@Override
	public String getDescriptiveName()
	{
		return "Netezza Plugin";
	}

	@Override
	public String getInternalName()
	{
		return "netezza";
	}

	@Override
	public String getVersion()
	{
		return "0.01";
	}

	@Override
	public boolean allowsSessionStartedInBackground()
	{
		return true;
	}

	@Override
	protected boolean isPluginSession(ISession session)
	{
		return DialectFactory.isNetezza(session.getMetaData());
	}

	/**
	 * Create panel for the Global Properties dialog.
	 * 
	 * @return properties panel.
	 */
	public IGlobalPreferencesPanel[] getGlobalPreferencePanels()
	{
		PluginQueryTokenizerPreferencesPanel _prefsPanel =
			new PluginQueryTokenizerPreferencesPanel(_prefsManager, "Netezza");

		PluginGlobalPreferencesTab tab = new PluginGlobalPreferencesTab(_prefsPanel);

		tab.setHint(i18n.PREFS_HINT);
		tab.setTitle("Netezza");

		return new IGlobalPreferencesPanel[] { tab };
	}
	
	
	@Override
	public PluginSessionCallback sessionStarted(final ISession session)
	{
		if (!DialectFactory.isNetezza(session.getMetaData())) {
			return null;
		}
		GUIUtils.processOnSwingEventThread(new Runnable() {

			@Override
			public void run()
			{
				updateObjectTree(session.getObjectTreeAPIOfActiveSessionWindow());
			}
			
		});
		
		return null;
	}

	private void updateObjectTree(final IObjectTreeAPI objTree)
	{
		objTree.addDetailTab(DatabaseObjectType.PROCEDURE, new DatabaseObjectInfoTab());
		objTree.addDetailTab(DatabaseObjectType.PROCEDURE, new ProcedureSourceTab(i18n.VIEW_SOURCE_TAB_HINT, ";"));
		objTree.addDetailTab(DatabaseObjectType.SEQUENCE, new DatabaseObjectInfoTab());
		objTree.addDetailTab(DatabaseObjectType.VIEW, new DatabaseObjectInfoTab());
		objTree.addDetailTab(DatabaseObjectType.VIEW, new ViewSourceTab(i18n.VIEW_SOURCE_TAB_HINT, ";"));
		
		
		// ////// Object Tree Expanders ////////
		// Schema Expanders - sequence
		objTree.addExpander(DatabaseObjectType.SCHEMA, 
			new SchemaExpander(new NetezzaSequenceInodeExpanderFactory(), DatabaseObjectType.SEQUENCE));
				
	}

	/**
	 * @see net.sourceforge.squirrel_sql.client.plugin.DefaultPlugin#initialize()
	 */
	@Override
	public void initialize() throws PluginException
	{
		_prefsManager = new PluginQueryTokenizerPreferencesManager();
		_prefsManager.initialize(this, new NetezzaPreferenceBean());
	}

}
