package net.sourceforge.squirrel_sql.plugins.mysql;
/*
 * Copyright (C) 2002-2003 Colin Bell
 * colbell@users.sourceforge.net
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
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.swing.JMenu;

import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.ActionCollection;
import net.sourceforge.squirrel_sql.client.plugin.DefaultSessionPlugin;
import net.sourceforge.squirrel_sql.client.plugin.PluginException;
import net.sourceforge.squirrel_sql.client.plugin.PluginResources;
import net.sourceforge.squirrel_sql.client.session.IObjectTreeAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
/**
 * MySQL plugin class.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class MysqlPlugin extends DefaultSessionPlugin
{
	/** Logger for this class. */
	private final static ILogger s_log = LoggerController.createLogger(MysqlPlugin.class);

	/** Folder to store user settings in. */
	private File _userSettingsFolder;

	/** Plugin resources. */
	private PluginResources _resources;

	/** API for the Obejct Tree. */
	private IObjectTreeAPI _treeAPI;

	/**
	 * Return the internal name of this plugin.
	 *
	 * @return	the internal name of this plugin.
	 */
	public String getInternalName()
	{
		return "mysql";
	}

	/**
	 * Return the descriptive name of this plugin.
	 *
	 * @return	the descriptive name of this plugin.
	 */
	public String getDescriptiveName()
	{
		return "MySQL Plugin";
	}

	/**
	 * Returns the current version of this plugin.
	 *
	 * @return	the current version of this plugin.
	 */
	public String getVersion()
	{
		return "0.15";
	}

	/**
	 * Returns the authors name.
	 *
	 * @return	the authors name.
	 */
	public String getAuthor()
	{
		return "Colin Bell";
	}

	/**
	 * Load this plugin.
	 *
	 * @param	app	 Application API.
	 */
	public synchronized void load(IApplication app) throws PluginException
	{
		super.load(app);

		// Folder to store user settings.
		try
		{
			_userSettingsFolder = getPluginUserSettingsFolder();
		}
		catch (IOException ex)
		{
			throw new PluginException(ex);
		}

		_resources = new MysqlResources(getClass().getName(), this);
	}

	/**
	 * Retrieve the name of the change log.
	 *
	 * @return	The name of the change log.
	 */
	public String getChangeLogFileName()
	{
		return "changes.txt";
	}

	/**
	 * Retrieve the name of the help file.
	 *
	 * @return	The nane of the help file.
	 */
	public String getHelpFileName()
	{
		return "readme.html";
	}

	/**
	 * Retrieve the name of the licence file.
	 *
	 * @return	The nane of the licence file.
	 */
	public String getLicenceFileName()
	{
		return "licence.txt";
	}

	/**
	 * Initialize this plugin.
	 */
	public synchronized void initialize() throws PluginException
	{
		super.initialize();

		final IApplication app = getApplication();
		final ActionCollection coll = app.getActionCollection();

		coll.add(new CreateMysqlTableScriptAction(app, _resources, this));
		coll.add(new CheckTableAction.ChangedCheckTableAction(app, _resources, this));
		coll.add(new CheckTableAction.ExtendedCheckTableAction(app, _resources, this));
		coll.add(new CheckTableAction.FastCheckTableAction(app, _resources, this));
		coll.add(new CheckTableAction.MediumCheckTableAction(app, _resources, this));
		coll.add(new CheckTableAction.QuickCheckTableAction(app, _resources, this));
		coll.add(new OptimizeTableAction(app, _resources, this));

		app.addToMenu(IApplication.IMenuIDs.SESSION_MENU, createMysqlMenu());
	}

	/**
	 * Application is shutting down so save preferences.
	 */
	public void unload()
	{
		super.unload();
	}

	/**
	 * Session has been started. If this is a MySQL session
	 * then setup MySQL tabs etc.
	 * 
	 * @param	session		Session that has started.
	 * 
	 * @return	<TT>true</TT> if session is MySQL in which case this plugin
	 * 			is interested in it.
	 */
	public boolean sessionStarted(ISession session)
	{
		boolean isMysql = false;
		if( super.sessionStarted(session))
		{
			isMysql = isMysql(session);
			if (isMysql)
			{
				_treeAPI = session.getObjectTreeAPI(this);
				final ActionCollection coll = getApplication().getActionCollection();
				_treeAPI.addToPopup(DatabaseObjectType.TABLE, createMysqlMenu());

				// Tabs to add to the database node.
				_treeAPI.addDetailTab(DatabaseObjectType.SESSION, new DatabaseStatusTab());
				_treeAPI.addDetailTab(DatabaseObjectType.SESSION, new ProcessesTab());

				// Tabs to add to the catalog node.
				_treeAPI.addDetailTab(DatabaseObjectType.CATALOG, new OpenTablesTab());

				// Tabs to add to table nodes.
				_treeAPI.addDetailTab(DatabaseObjectType.TABLE, new AnalyzeTableTab());
			}
		}
		return isMysql;
	}

	/**
	 * Create menu containing actions relevant for the object tree.
	 *
	 * @return	The menu object.
	 */
	private JMenu createMysqlMenu()
	{
		final IApplication app = getApplication();
		final ActionCollection coll = app.getActionCollection();

		final JMenu menu = _resources.createMenu(MysqlResources.IMenuResourceKeys.MYSQL);
		_resources.addToMenu(coll.get(CreateMysqlTableScriptAction.class), menu);
		_resources.addToMenu(coll.get(CheckTableAction.ChangedCheckTableAction.class), menu);
		_resources.addToMenu(coll.get(CheckTableAction.ExtendedCheckTableAction.class), menu);
		_resources.addToMenu(coll.get(CheckTableAction.FastCheckTableAction.class), menu);
		_resources.addToMenu(coll.get(CheckTableAction.MediumCheckTableAction.class), menu);
		_resources.addToMenu(coll.get(CheckTableAction.QuickCheckTableAction.class), menu);
		_resources.addToMenu(coll.get(OptimizeTableAction.class), menu);

		app.addToMenu(IApplication.IMenuIDs.SESSION_MENU, menu);

		return menu;
	}

	/**
	 * Decide whether the passed session is a MySQL one.
	 *
	 * @param	session		Session we are checking.
	 *
	 * @return	<TT>true</TT> if <TT>session</TT> is a MySQL one.
	 */
	private boolean isMysql(ISession session)
	{
		final String MYSQL = "mysql";
		String dbms = null;
		try
		{
			dbms = session.getSQLConnection().getSQLMetaData().getDatabaseProductName();
		}
		catch (SQLException ex)
		{
			s_log.debug("Error in getDatabaseProductName()", ex);
		}
		return dbms != null && dbms.toLowerCase().startsWith(MYSQL);
	}
}
