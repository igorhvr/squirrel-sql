package net.sourceforge.squirrel_sql.plugins.sqlscript;
/*
 * Copyright (C) 2001 Johan Compagner
 * jcompagner@j-com.nl
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javax.swing.JMenu;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.ActionCollection;
import net.sourceforge.squirrel_sql.client.gui.session.ObjectTreeInternalFrame;
import net.sourceforge.squirrel_sql.client.gui.session.SQLInternalFrame;
import net.sourceforge.squirrel_sql.client.plugin.DefaultSessionPlugin;
import net.sourceforge.squirrel_sql.client.plugin.PluginException;
import net.sourceforge.squirrel_sql.client.plugin.PluginManager;
import net.sourceforge.squirrel_sql.client.plugin.PluginResources;
import net.sourceforge.squirrel_sql.client.plugin.PluginSessionCallback;
import net.sourceforge.squirrel_sql.client.preferences.IGlobalPreferencesPanel;
import net.sourceforge.squirrel_sql.client.session.IObjectTreeAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.plugins.sqlscript.table_script.CreateDataScriptAction;
import net.sourceforge.squirrel_sql.plugins.sqlscript.table_script.CreateDataScriptOfCurrentSQLAction;
import net.sourceforge.squirrel_sql.plugins.sqlscript.table_script.CreateTableOfCurrentSQLAction;
import net.sourceforge.squirrel_sql.plugins.sqlscript.table_script.CreateTableScriptAction;
import net.sourceforge.squirrel_sql.plugins.sqlscript.table_script.CreateTemplateDataScriptAction;
import net.sourceforge.squirrel_sql.plugins.sqlscript.table_script.DropTableScriptAction;
/**
 * The SQL Script plugin class.
 */
public class SQLScriptPlugin extends DefaultSessionPlugin {
	private interface IMenuResourceKeys {
		String SCRIPTS = "scripts";
	}

	/** Logger for this class. */
	private static ILogger s_log = LoggerController.createLogger(SQLScriptPlugin.class);

   /** The app folder for this plugin. */
	private File _pluginAppFolder;

	/** Folder to store user settings in. */
	private File _userSettingsFolder;

	private PluginResources _resources;

	private Hashtable _saveAndLoadDelegates = new Hashtable();

	/**
	 * Return the internal name of this plugin.
	 *
	 * @return  the internal name of this plugin.
	 */
	public String getInternalName() {
		return "sqlscript";
	}

	/**
	 * Return the descriptive name of this plugin.
	 *
	 * @return  the descriptive name of this plugin.
	 */
	public String getDescriptiveName() {
		return "SQL Scripts Plugin";
	}

	/**
	 * Returns the current version of this plugin.
	 *
	 * @return  the current version of this plugin.
	 */
	public String getVersion() {
		return "1.1";
	}

	/**
	 * Returns the authors name.
	 *
	 * @return  the authors name.
	 */
	public String getAuthor() {
		return "Johan Compagner";
	}



	/**
	 * Returns the name of the change log for the plugin. This should
	 * be a text or HTML file residing in the <TT>getPluginAppSettingsFolder</TT>
	 * directory.
	 *
	 * @return	the changelog file name or <TT>null</TT> if plugin doesn't have
	 * 			a change log.
	 */
	public String getChangeLogFileName()
	{
		return "changes.txt";
	}

	/**
	 * Returns the name of the Help file for the plugin. This should
	 * be a text or HTML file residing in the <TT>getPluginAppSettingsFolder</TT>
	 * directory.
	 *
	 * @return	the Help file name or <TT>null</TT> if plugin doesn't have
	 * 			a help file.
	 */
	public String getHelpFileName()
	{
		return "readme.html";
	}

	/**
	 * Returns the name of the Licence file for the plugin. This should
	 * be a text or HTML file residing in the <TT>getPluginAppSettingsFolder</TT>
	 * directory.
	 *
	 * @return	the Licence file name or <TT>null</TT> if plugin doesn't have
	 * 			a licence file.
	 */
	public String getLicenceFileName()
	{
		return "licence.txt";
	}

	/**
	 * @return	Comma separated list of contributors.
	 */
	public String getContributors()
	{
		return "Gerd Wagner, John Murga, Rob Manning";
	}

	/**
	 * Create preferences panel for the Global Preferences dialog.
	 *
	 * @return  Preferences panel.
	 */
	public IGlobalPreferencesPanel[] getGlobalPreferencePanels() {
		return new IGlobalPreferencesPanel[0];
	}

	/**
	 * Initialize this plugin.
	 */
	public synchronized void initialize() throws PluginException {
		super.initialize();
		IApplication app = getApplication();

		PluginManager pmgr = app.getPluginManager();

		// Folder within plugins folder that belongs to this
		// plugin.
		try {
			_pluginAppFolder = getPluginAppSettingsFolder();
		} catch (IOException ex) {
			throw new PluginException(ex);
		}

		// Folder to store user settings.
		try {
			_userSettingsFolder = getPluginUserSettingsFolder();
		} catch (IOException ex) {
			throw new PluginException(ex);
		}

		_resources =
			new SQLPluginResources(
				"net.sourceforge.squirrel_sql.plugins.sqlscript.sqlscript",
				this);


		ActionCollection coll = app.getActionCollection();
		coll.add(new CreateTableScriptAction(app, _resources, this));
        coll.add(new DropTableScriptAction(app, _resources, this));
        coll.add(new CreateDataScriptAction(app, _resources, this));
		coll.add(new CreateTemplateDataScriptAction(app, _resources, this));
		coll.add(new CreateDataScriptOfCurrentSQLAction(app, _resources, this));
		coll.add(new CreateTableOfCurrentSQLAction(app, _resources, this));
		createMenu();
	}

	/**
	 * Application is shutting down so save data.
	 */
	public void unload()
   {
		super.unload();
	}

   public boolean allowsSessionStartedInBackground()
   {
      return true;
   }

   /**
	 * Called when a session started. Add commands to popup menu
	 * in object tree.
	 *
	 * @param   session	 The session that is starting.
	 *
	 * @return  <TT>true</TT> to indicate that this plugin is
	 *		  applicable to passed session.
	 */
	public PluginSessionCallback sessionStarted(final ISession session)
	{
        GUIUtils.processOnSwingEventThread(new Runnable() {
           public void run() {
               addActionsToPopup(session);
           }
        });
	    
	    PluginSessionCallback ret = new PluginSessionCallback()
	    {
	        public void sqlInternalFrameOpened(SQLInternalFrame sqlInternalFrame, ISession sess)
	        {
	            ActionCollection coll = sess.getApplication().getActionCollection();
	            sqlInternalFrame.addSeparatorToToolbar();
	            sqlInternalFrame.addToToolbar(coll.get(CreateTableOfCurrentSQLAction.class));
	            
	            sqlInternalFrame.addToToolsPopUp("sql2table", coll.get(CreateTableOfCurrentSQLAction.class));
	            sqlInternalFrame.addToToolsPopUp("sql2ins", coll.get(CreateDataScriptOfCurrentSQLAction.class));
	        }
	        
	        public void objectTreeInternalFrameOpened(ObjectTreeInternalFrame objectTreeInternalFrame, ISession sess)
	        {
	            ActionCollection coll = sess.getApplication().getActionCollection();
	            objectTreeInternalFrame.getObjectTreeAPI().addToPopup(DatabaseObjectType.TABLE, coll.get(CreateTableScriptAction.class));
                objectTreeInternalFrame.getObjectTreeAPI().addToPopup(DatabaseObjectType.TABLE, coll.get(DropTableScriptAction.class));
	            objectTreeInternalFrame.getObjectTreeAPI().addToPopup(DatabaseObjectType.TABLE, coll.get(CreateDataScriptAction.class));
	            objectTreeInternalFrame.getObjectTreeAPI().addToPopup(DatabaseObjectType.TABLE, coll.get(CreateTemplateDataScriptAction.class));
	        }
	    };
	    
	    return ret;
	}

    private void addActionsToPopup(ISession session) {
        ActionCollection coll = getApplication().getActionCollection();
        
        //IObjectTreeAPI api = session.getObjectTreeAPI(this);
        IObjectTreeAPI api = FrameWorkAcessor.getObjectTreeAPI(session, this);
        
        api.addToPopup(DatabaseObjectType.TABLE, coll.get(CreateTableScriptAction.class));
        api.addToPopup(DatabaseObjectType.TABLE, coll.get(DropTableScriptAction.class));
        api.addToPopup(DatabaseObjectType.TABLE, coll.get(CreateDataScriptAction.class));
        api.addToPopup(DatabaseObjectType.TABLE, coll.get(CreateTemplateDataScriptAction.class));
        api.addToPopup(DatabaseObjectType.VIEW, coll.get(CreateTableScriptAction.class));
        api.addToPopup(DatabaseObjectType.VIEW, coll.get(CreateDataScriptAction.class));
        api.addToPopup(DatabaseObjectType.VIEW, coll.get(CreateTemplateDataScriptAction.class));
        
        
        session.addSeparatorToToolbar();
        session.addToToolbar(coll.get(CreateTableOfCurrentSQLAction.class));
        
        session.getSessionInternalFrame().addToToolsPopUp("sql2table", coll.get(CreateTableOfCurrentSQLAction.class));
        session.getSessionInternalFrame().addToToolsPopUp("sql2ins", coll.get(CreateDataScriptOfCurrentSQLAction.class));        
    }
    
   private void createMenu() {
		IApplication app = getApplication();
		ActionCollection coll = app.getActionCollection();

		JMenu menu = _resources.createMenu(IMenuResourceKeys.SCRIPTS);
		_resources.addToMenu(coll.get(CreateDataScriptAction.class), menu);
		_resources.addToMenu(coll.get(CreateTemplateDataScriptAction.class), menu);
		_resources.addToMenu(coll.get(CreateTableScriptAction.class), menu);
        _resources.addToMenu(coll.get(DropTableScriptAction.class), menu);
		_resources.addToMenu(coll.get(CreateDataScriptOfCurrentSQLAction.class), menu);
      _resources.addToMenu(coll.get(CreateTableOfCurrentSQLAction.class), menu);

		app.addToMenu(IApplication.IMenuIDs.SESSION_MENU, menu);
	}

   public Object getExternalService()
   {
      return new SQLScriptExternalService(this);
   }

}
