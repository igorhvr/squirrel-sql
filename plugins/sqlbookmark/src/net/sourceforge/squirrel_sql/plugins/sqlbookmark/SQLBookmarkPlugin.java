/*
 * Copyright (C) 2003 Joseph Mocker
 * mock-sf@misfit.dhs.org
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

package net.sourceforge.squirrel_sql.plugins.sqlbookmark;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Iterator;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import net.sourceforge.squirrel_sql.client.plugin.PluginManager;
import net.sourceforge.squirrel_sql.client.plugin.PluginException;
import net.sourceforge.squirrel_sql.client.plugin.DefaultSessionPlugin;
import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.ActionCollection;
import net.sourceforge.squirrel_sql.client.plugin.PluginResources;
import net.sourceforge.squirrel_sql.client.action.SquirrelAction;

import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.preferences.IGlobalPreferencesPanel;
import net.sourceforge.squirrel_sql.client.session.ISession;

/**
 * Main entry into the SQL Bookmark plugin. 
 *
 * This plugin allows you to maintain a set of frequently used SQL 
 * scripts for easy playback. There is also a parameter replacement
 * syntax available for the SQL files.
 *
 * @author      Joseph Mocker
 **/
public class SQLBookmarkPlugin extends DefaultSessionPlugin {
    private interface IMenuResourceKeys {
	String BOOKMARKS = "bookmarks";
    }

    private static String RESOURCE_PATH =
	"net.sourceforge.squirrel_sql.plugins.sqlbookmark.sqlbookmark";
    
    private static ILogger logger = 
	LoggerController.createLogger(SQLBookmarkPlugin.class);

    /** The app folder for this plugin. */
    private File pluginAppFolder;
    
    /** Folder to store user settings in. */
    protected File userSettingsFolder;
    
    private PluginResources resources;

    /** The bookmark menu */
    private JMenu menu;

    /** All the current bookmarks */
    private BookmarkManager bookmarks;

    /**
     * Returns the plugin version.
     *
     * @return  the plugin version.
     */
    public String getVersion() {
	return "0.42";
    }
    
    /**
     * Returns the authors name.
     *
     * @return  the authors name.
     */
    public String getAuthor() {
	return "Joseph Mocker";
    }
    
    /**
     * Return the internal name of this plugin.
     *
     * @return  the internal name of this plugin.
     */
    public String getInternalName() {
	return "sqlbookmark";
    }
    
    /**
     * Return the descriptive name of this plugin.
     *
     * @return  the descriptive name of this plugin.
     */
    public String getDescriptiveName() {
	return "SQL Bookmark Plugin";
    }

    /**
     * Returns the name of the Help file for the plugin. 
     *
     * @return	the help file name.
     */
    public String getHelpFileName() {
	return "readme.html";
    }

    /**
     * Returns the name of the Help file for the plugin. 
     *
     * @return	the license file name.
     */
	public String getLicenceFileName() {
	return "license.txt";
    }

    /**
     * Return the plugin resources. Used by other classes.
     *
     * @return	plugin resources.
     */
    protected PluginResources getResources() {
	return resources;
    }
    
    /**
     * Get and return a string from the plugin resources. 
     *
     * @param name name of the resource string to return.
     * @return	resource string.
     */
    protected String getResourceString(String name) {
	return resources.getString(name);
    }

    /**
     * Returns a handle to the current bookmark manager.
     *
     * @return	the bookmark manager.
     */
    protected BookmarkManager getBookmarkManager() {
	return bookmarks;
    }

    /**
     * Set the bookmark manager.
     *
     * @param bookmarks new manager to register.
     */
    protected void setBookmarkManager(BookmarkManager bookmarks) {
	this.bookmarks = bookmarks;
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
	    pluginAppFolder = getPluginAppSettingsFolder();
	} catch (IOException ex) {
	    throw new PluginException(ex);
	}
	
	// Folder to store user settings.
	try {
	    userSettingsFolder = getPluginUserSettingsFolder();
	} catch (IOException ex) {
	    throw new PluginException(ex);
	}

	// Load resources such as menu items, etc...
	resources = new SQLBookmarkResources(RESOURCE_PATH, this);
	
	bookmarks = new BookmarkManager(userSettingsFolder);
	// Load plugin preferences.
	try {
	    bookmarks.load();
	}
	catch (IOException e) {
	    if (!(e instanceof FileNotFoundException))
		logger.error("Problem loading bookmarks", e);
	}
	
	ActionCollection coll = app.getActionCollection();
	coll.add(new AddBookmarkAction(app, resources, this));
	coll.add(new RunBookmarkAction(app, resources, this));
	createMenu();

	rebuildMenu();
    }

    /**
     * Rebuild the Sessions->Bookmarks menu
     *
     */
    protected void rebuildMenu() {
	ActionCollection coll = getApplication().getActionCollection();

	menu.removeAll();
	resources.addToMenu(coll.get(AddBookmarkAction.class), menu);
	menu.add(new JSeparator());

	for (Iterator i = bookmarks.iterator(); i.hasNext(); ) {
	    Object o = i.next();
	    logger.error(o.getClass().getName());
	    Bookmark bookmark = (Bookmark) o;

	    addBookmarkItem(bookmark);
	}
    }

    /**
     * Create the initial Sessions->Bookmark menu
     *
     */
    private void createMenu() {
	IApplication app = getApplication();

	menu = resources.createMenu(IMenuResourceKeys.BOOKMARKS);
	
	app.addToMenu(IApplication.IMenuIDs.SESSION_MENU, menu);
    }

    /**
     * Add new bookmark to Sessions->Bookmark menu
     *
     * @param bookmark the bookmark to add.
     */
    protected void addBookmarkItem(Bookmark bookmark) {
	IApplication app = getApplication();
	ActionCollection coll = app.getActionCollection();

	SquirrelAction action = 
	    (SquirrelAction) coll.get(RunBookmarkAction.class);

	JMenuItem item = new JMenuItem(action);
	item.setText(bookmark.getName());
	
	menu.add(item);
    }

    /**
     * Create and return a preferences object.
     *
     * @return The global preferences object.
     */
    public IGlobalPreferencesPanel[] getGlobalPreferencePanels() {
	return new IGlobalPreferencesPanel[] {
	    new SQLBookmarkPreferencesPanel(this)
		};
    }
}
    
