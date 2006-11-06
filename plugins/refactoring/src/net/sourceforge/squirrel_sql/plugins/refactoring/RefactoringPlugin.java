package net.sourceforge.squirrel_sql.plugins.refactoring;
/*
 * Copyright (C) 2006 Rob Manning
 * manningr@users.sourceforge.net
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
import javax.swing.JMenuItem;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.ActionCollection;
import net.sourceforge.squirrel_sql.client.gui.session.ObjectTreeInternalFrame;
import net.sourceforge.squirrel_sql.client.gui.session.SQLInternalFrame;
import net.sourceforge.squirrel_sql.client.plugin.DefaultSessionPlugin;
import net.sourceforge.squirrel_sql.client.plugin.PluginException;
import net.sourceforge.squirrel_sql.client.plugin.PluginManager;
import net.sourceforge.squirrel_sql.client.plugin.PluginResources;
import net.sourceforge.squirrel_sql.client.plugin.PluginSessionCallback;
import net.sourceforge.squirrel_sql.client.session.IObjectTreeAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.action.DropSelectedTablesAction;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.plugins.refactoring.actions.AddColumnAction;
import net.sourceforge.squirrel_sql.plugins.refactoring.actions.AddPrimaryKeyAction;
import net.sourceforge.squirrel_sql.plugins.refactoring.actions.ModifyColumnAction;
import net.sourceforge.squirrel_sql.plugins.refactoring.actions.RemoveColumnAction;

/**
 * The Refactoring plugin class.
 */
public class RefactoringPlugin extends DefaultSessionPlugin {
   private interface IMenuResourceKeys {
      String REFACTORING = "refactoring";
   }

   /** Logger for this class. */
   private static ILogger s_log = LoggerController.createLogger(RefactoringPlugin.class);

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
      return "refactoring";
   }

   /**
    * Return the descriptive name of this plugin.
    *
    * @return  the descriptive name of this plugin.
    */
   public String getDescriptiveName() {
      return "Refactoring Plugin";
   }

   /**
    * Returns the current version of this plugin.
    *
    * @return  the current version of this plugin.
    */
   public String getVersion() {
      return "0.1";
   }

   /**
    * Returns the authors name.
    *
    * @return  the authors name.
    */
   public String getAuthor() {
      return "Rob Manning";
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
      return "";
   }

   /**
    * Create preferences panel for the Global Preferences dialog.
    *
    * @return  Preferences panel.
    */
   /*
   public IGlobalPreferencesPanel[] getGlobalPreferencePanels() {
       SQLScriptPreferencesTab tab = new SQLScriptPreferencesTab();
       return new IGlobalPreferencesPanel[] {tab};
   }
   */
   
   /**
    * Initialize this plugin.
    */
   public synchronized void initialize() throws PluginException
   {
      super.initialize();
      IApplication app = getApplication();

      PluginManager pmgr = app.getPluginManager();

      // Folder within plugins folder that belongs to this
      // plugin.
      try
      {
         _pluginAppFolder = getPluginAppSettingsFolder();
      }
      catch (IOException ex)
      {
         throw new PluginException(ex);
      }

      // Folder to store user settings.
      try
      {
         _userSettingsFolder = getPluginUserSettingsFolder();
      }
      catch (IOException ex)
      {
         throw new PluginException(ex);
      }

      _resources =
         new SQLPluginResources(
            "net.sourceforge.squirrel_sql.plugins.refactoring.refactoring",
            this);


      ActionCollection coll = app.getActionCollection();
      coll.add(new AddColumnAction(app, _resources));
      coll.add(new ModifyColumnAction(app, _resources));
      coll.add(new RemoveColumnAction(app, _resources));
      coll.add(new AddPrimaryKeyAction(app, _resources));
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
               //sqlInternalFrame.addSeparatorToToolbar();
               //sqlInternalFrame.addToToolbar(coll.get(CreateTableOfCurrentSQLAction.class));

               //sqlInternalFrame.addToToolsPopUp("sql2table", coll.get(CreateTableOfCurrentSQLAction.class));
               //sqlInternalFrame.addToToolsPopUp("sql2ins", coll.get(CreateDataScriptOfCurrentSQLAction.class));
           }

           public void objectTreeInternalFrameOpened(ObjectTreeInternalFrame objectTreeInternalFrame, ISession sess)
           {
               //ActionCollection coll = sess.getApplication().getActionCollection();
               //objectTreeInternalFrame.getObjectTreeAPI().addToPopup(DatabaseObjectType.TABLE, coll.get(CreateTableScriptAction.class));
               //objectTreeInternalFrame.getObjectTreeAPI().addToPopup(DatabaseObjectType.TABLE, coll.get(CreateSelectScriptAction.class));
               //objectTreeInternalFrame.getObjectTreeAPI().addToPopup(DatabaseObjectType.TABLE, coll.get(DropTableScriptAction.class));
               //objectTreeInternalFrame.getObjectTreeAPI().addToPopup(DatabaseObjectType.TABLE, coll.get(CreateDataScriptAction.class));
               //objectTreeInternalFrame.getObjectTreeAPI().addToPopup(DatabaseObjectType.TABLE, coll.get(CreateTemplateDataScriptAction.class));
           }
       };

       return ret;
   }

    private void addActionsToPopup(ISession session) {
        ActionCollection coll = getApplication().getActionCollection();

        IObjectTreeAPI api = session.getObjectTreeAPIOfActiveSessionWindow();

        //session.getApplication().addToMenu(MainFrameMenuBar, menu)
        JMenu tableObjectMenu = _resources.createMenu(IMenuResourceKeys.REFACTORING);
        JMenu columnMenu = new JMenu("Column"); 
        JMenuItem addColItem = new JMenuItem("Add Column");
        addColItem.setAction(coll.get(AddColumnAction.class));
        JMenuItem removeColItem = new JMenuItem("Drop Column");
        removeColItem.setAction(coll.get(RemoveColumnAction.class));
        JMenuItem modifyMenuItem = new JMenuItem("Modify Column");
        modifyMenuItem.setAction(coll.get(ModifyColumnAction.class));
        
        columnMenu.add(addColItem);
        columnMenu.add(modifyMenuItem);
        columnMenu.add(removeColItem);
        
        JMenuItem dropTableItem = new JMenuItem("Drop Table");
        dropTableItem.setAction(coll.get(DropSelectedTablesAction.class));
        JMenuItem addIndexItem = new JMenuItem("Add Index");
        JMenuItem dropIndexItem = new JMenuItem("Drop Index");
        JMenuItem addPrimaryKeyItem = new JMenuItem("Add Primary Key");
        addPrimaryKeyItem.setAction(coll.get(AddPrimaryKeyAction.class));
        JMenuItem dropPrimaryKeyItem = new JMenuItem("Drop Primary Key");
        JMenuItem addForeignKeyItem = new JMenuItem("Add Foreign Key");
        JMenuItem dropForeignKeyItem = new JMenuItem("Drop Foreign Key");
        JMenuItem enableConstraintsItem = new JMenuItem("Enable Constraints");
        JMenuItem disableConstraintsItem = new JMenuItem("Disable Constraints");
        
        
        JMenu tableMenu = new JMenu("Table");
        
        tableMenu.add(dropTableItem);
        tableMenu.add(addIndexItem);
        tableMenu.add(dropIndexItem);
        tableMenu.add(addPrimaryKeyItem);
        tableMenu.add(dropPrimaryKeyItem);
        tableMenu.add(addForeignKeyItem);
        tableMenu.add(dropForeignKeyItem);
        tableMenu.add(enableConstraintsItem);
        tableMenu.add(disableConstraintsItem);
        
        tableObjectMenu.add(tableMenu);
        tableObjectMenu.add(columnMenu);
        
        api.addToPopup(DatabaseObjectType.TABLE, tableObjectMenu);
        /*
        api.addToPopup(DatabaseObjectType.TABLE, coll.get(CreateTableScriptAction.class));
        api.addToPopup(DatabaseObjectType.TABLE, coll.get(CreateSelectScriptAction.class));
        api.addToPopup(DatabaseObjectType.TABLE, coll.get(DropTableScriptAction.class));
        api.addToPopup(DatabaseObjectType.TABLE, coll.get(CreateDataScriptAction.class));
        api.addToPopup(DatabaseObjectType.TABLE, coll.get(CreateTemplateDataScriptAction.class));
        api.addToPopup(DatabaseObjectType.VIEW, coll.get(CreateTableScriptAction.class));
        api.addToPopup(DatabaseObjectType.VIEW, coll.get(CreateSelectScriptAction.class));
        api.addToPopup(DatabaseObjectType.VIEW, coll.get(CreateDataScriptAction.class));
        api.addToPopup(DatabaseObjectType.VIEW, coll.get(CreateTemplateDataScriptAction.class));
    

        session.addSeparatorToToolbar();
        session.addToToolbar(coll.get(CreateTableOfCurrentSQLAction.class));

        session.getSessionInternalFrame().addToToolsPopUp("sql2table", coll.get(CreateTableOfCurrentSQLAction.class));
        session.getSessionInternalFrame().addToToolsPopUp("sql2ins", coll.get(CreateDataScriptOfCurrentSQLAction.class));
        */
    }

   private void createMenu(ISession session)
   {
      IApplication app = getApplication();
      ActionCollection coll = app.getActionCollection();

      JMenu menu = _resources.createMenu(IMenuResourceKeys.REFACTORING);
      app.addToMenu(IApplication.IMenuIDs.PLUGINS_MENU, menu);

      //_resources.addToMenu(coll.get(CreateDataScriptAction.class), menu);
      //_resources.addToMenu(coll.get(CreateTemplateDataScriptAction.class), menu);
      //_resources.addToMenu(coll.get(CreateTableScriptAction.class), menu);
      //_resources.addToMenu(coll.get(CreateSelectScriptAction.class), menu);
      //_resources.addToMenu(coll.get(DropTableScriptAction.class), menu);
      //_resources.addToMenu(coll.get(CreateDataScriptOfCurrentSQLAction.class), menu);
      //_resources.addToMenu(coll.get(CreateTableOfCurrentSQLAction.class), menu);

      //app.addToMenu(IApplication.IMenuIDs.SESSION_MENU, menu);
   }

}
