package net.sourceforge.squirrel_sql.plugins.exportconfig;
/*
 * Copyright (C) 2003 Colin Bell
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

import javax.swing.JMenu;

import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.ActionCollection;
import net.sourceforge.squirrel_sql.client.plugin.DefaultPlugin;
import net.sourceforge.squirrel_sql.client.plugin.PluginException;
import net.sourceforge.squirrel_sql.client.plugin.PluginResources;

import net.sourceforge.squirrel_sql.plugins.exportconfig.action.ExportAliasesAction;
import net.sourceforge.squirrel_sql.plugins.exportconfig.action.ExportDriversAction;
import net.sourceforge.squirrel_sql.plugins.exportconfig.action.ExportSettingsAction;
/**
 * Plugin controlling class.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class ExportConfigPlugin extends DefaultPlugin
{
	/** Logger for this class. */
	private final static ILogger s_log = LoggerController.createLogger(ExportConfigPlugin.class);

	/** Folder to store user settings in. */
	private File _userSettingsFolder;

	/** Plugin resources. */
	private PluginResources _resources;

	/** Export menu. */
	private JMenu _exportMenu;

	/**
	 * Return the internal name of this plugin.
	 *
	 * @return	the internal name of this plugin.
	 */
	public String getInternalName()
	{
		return "exportconfig";
	}

	/**
	 * Return the descriptive name of this plugin.
	 *
	 * @return	the descriptive name of this plugin.
	 */
	public String getDescriptiveName()
	{
		return "Export Configuration Plugin";
	}

	/**
	 * Returns the current version of this plugin.
	 *
	 * @return	the current version of this plugin.
	 */
	public String getVersion()
	{
		return "0.10";
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

		_resources = new ExportConfigResources(getClass().getName(), this);
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

		coll.add(new ExportAliasesAction(app, _resources, this));
		coll.add(new ExportDriversAction(app, _resources, this));
		coll.add(new ExportSettingsAction(app, _resources, this));

		_exportMenu = createExportMenu();
		app.addToMenu(IApplication.IMenuIDs.PLUGINS_MENU, _exportMenu);
	}

	/**
	 * Create menu containing actions relevant for the object tree.
	 *
	 * @return	The menu object.
	 */
	private JMenu createExportMenu()
	{
		final IApplication app = getApplication();
		final ActionCollection coll = app.getActionCollection();

		final JMenu exportMenu = _resources.createMenu(ExportConfigResources.IMenuResourceKeys.EXPORT);
		_resources.addToMenu(coll.get(ExportAliasesAction.class), exportMenu);
		_resources.addToMenu(coll.get(ExportDriversAction.class), exportMenu);
		_resources.addToMenu(coll.get(ExportSettingsAction.class), exportMenu);

		app.addToMenu(IApplication.IMenuIDs.SESSION_MENU, exportMenu);

		return exportMenu;
	}
}
