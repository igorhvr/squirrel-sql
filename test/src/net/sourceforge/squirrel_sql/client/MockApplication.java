/*
 * Copyright (C) 2006 Rob Manning
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
package net.sourceforge.squirrel_sql.client;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;

import net.sourceforge.squirrel_sql.client.FontInfoStore;
import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.ActionCollection;
import net.sourceforge.squirrel_sql.client.gui.WindowManager;
import net.sourceforge.squirrel_sql.client.gui.db.DataCache;
import net.sourceforge.squirrel_sql.client.gui.mainframe.MainFrame;
import net.sourceforge.squirrel_sql.client.plugin.IPlugin;
import net.sourceforge.squirrel_sql.client.plugin.PluginManager;
import net.sourceforge.squirrel_sql.client.preferences.SquirrelPreferences;
import net.sourceforge.squirrel_sql.client.resources.SquirrelResources;
import net.sourceforge.squirrel_sql.client.session.ISQLEntryPanelFactory;
import net.sourceforge.squirrel_sql.client.session.SessionManager;
import net.sourceforge.squirrel_sql.client.session.mainpanel.SQLHistory;
import net.sourceforge.squirrel_sql.fw.sql.SQLDriverManager;
import net.sourceforge.squirrel_sql.fw.util.IMessageHandler;
import net.sourceforge.squirrel_sql.fw.util.TaskThreadPool;

public class MockApplication implements IApplication {

    TaskThreadPool threadPool = null;
    PluginManager pluginManager = null;
    ActionCollection actions = null;
    SquirrelResources resource = null;
    SquirrelPreferences prefs = null;
    
    public MockApplication() {
        resource = 
            new SquirrelResources("net.sourceforge.squirrel_sql.client.resources.squirrel");
        prefs = SquirrelPreferences.load();
        threadPool = new TaskThreadPool();
        pluginManager = new PluginManager(this);
        actions = new ActionCollection(this);
    }
    
    public IPlugin getDummyAppPlugin() {
        // TODO Auto-generated method stub
        return null;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public WindowManager getWindowManager() {
        // TODO Auto-generated method stub
        return null;
    }

    public ActionCollection getActionCollection() {
        return actions;
    }

    public SQLDriverManager getSQLDriverManager() {
        // TODO Auto-generated method stub
        return null;
    }

    public DataCache getDataCache() {
        // TODO Auto-generated method stub
        return null;
    }

    public SquirrelPreferences getSquirrelPreferences() {
        return prefs;
    }

    public SquirrelResources getResources() {
        return resource;
    }

    public IMessageHandler getMessageHandler() {
        // TODO Auto-generated method stub
        return null;
    }

    public SessionManager getSessionManager() {
        // TODO Auto-generated method stub
        return null;
    }

    public void showErrorDialog(String msg) {
        // TODO Auto-generated method stub

    }

    public void showErrorDialog(Throwable th) {
        // TODO Auto-generated method stub

    }

    public void showErrorDialog(String msg, Throwable th) {
        // TODO Auto-generated method stub

    }

    public MainFrame getMainFrame() {
        // TODO Auto-generated method stub
        return null;
    }

    public TaskThreadPool getThreadPool() {
        return threadPool;
    }

    public FontInfoStore getFontInfoStore() {
        // TODO Auto-generated method stub
        return null;
    }

    public ISQLEntryPanelFactory getSQLEntryPanelFactory() {
        // TODO Auto-generated method stub
        return null;
    }

    public SQLHistory getSQLHistory() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setSQLEntryPanelFactory(ISQLEntryPanelFactory factory) {
        // TODO Auto-generated method stub

    }

    public void addToMenu(int menuId, JMenu menu) {
        // TODO Auto-generated method stub

    }

    public void addToMenu(int menuId, Action action) {
        // TODO Auto-generated method stub

    }

    public void addToStatusBar(JComponent comp) {
        // TODO Auto-generated method stub

    }

    public void removeFromStatusBar(JComponent comp) {
        // TODO Auto-generated method stub

    }

    public void startup() {
        // TODO Auto-generated method stub

    }

    public boolean shutdown() {
        // TODO Auto-generated method stub
        return false;
    }

    public void openURL(String url) {
        // TODO Auto-generated method stub

    }

}
