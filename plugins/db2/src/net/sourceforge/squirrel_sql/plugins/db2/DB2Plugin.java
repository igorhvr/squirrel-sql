package net.sourceforge.squirrel_sql.plugins.db2;
/*
 * Copyright (C) 2007 Rob Manning
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

import java.sql.SQLException;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.gui.session.ObjectTreeInternalFrame;
import net.sourceforge.squirrel_sql.client.gui.session.SQLInternalFrame;
import net.sourceforge.squirrel_sql.client.plugin.DefaultSessionPlugin;
import net.sourceforge.squirrel_sql.client.plugin.PluginException;
import net.sourceforge.squirrel_sql.client.plugin.PluginSessionCallback;
import net.sourceforge.squirrel_sql.client.session.IObjectTreeAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.expanders.ITableIndexExtractor;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.expanders.ITableTriggerExtractor;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.expanders.TableWithChildNodesExpander;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.DatabaseObjectInfoTab;
import net.sourceforge.squirrel_sql.fw.dialects.DialectFactory;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.plugins.db2.exp.DB2TableIndexExtractorImpl;
import net.sourceforge.squirrel_sql.plugins.db2.exp.DB2TableTriggerExtractorImpl;
import net.sourceforge.squirrel_sql.plugins.db2.exp.SchemaExpander;
import net.sourceforge.squirrel_sql.plugins.db2.tab.IndexDetailsTab;
import net.sourceforge.squirrel_sql.plugins.db2.tab.ProcedureSourceTab;
import net.sourceforge.squirrel_sql.plugins.db2.tab.SequenceDetailsTab;
import net.sourceforge.squirrel_sql.plugins.db2.tab.TriggerDetailsTab;
import net.sourceforge.squirrel_sql.plugins.db2.tab.TriggerSourceTab;
import net.sourceforge.squirrel_sql.plugins.db2.tab.ViewSourceTab;


/**
 * The main controller class for the DB2 plugin.
 * 
 * @author manningr
 */
public class DB2Plugin extends DefaultSessionPlugin {
    
    private static final String JCC_DRIVER_NAME = "IBM DB2 JDBC Universal Driver Architecture";
    
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(DB2Plugin.class);

    /** Logger for this class. */
    private final static ILogger s_log = 
        LoggerController.createLogger(DB2Plugin.class);

    /** API for the Obejct Tree. */
    private IObjectTreeAPI _treeAPI;

    static interface i18n {
        //i18n[DB2Plugin.showViewSource=Show view source]
        String SHOW_VIEW_SOURCE = 
            s_stringMgr.getString("DB2Plugin.showViewSource");
        
        //i18n[DB2Plugin.showProcedureSource=Show procedure source]
        String SHOW_PROCEDURE_SOURCE =
            s_stringMgr.getString("DB2Plugin.showProcedureSource");
        
        //i18n[DB2Plugin.showTriggerSource=Show trigger source]
        String SHOW_TRIGGER_SOURCE =
            s_stringMgr.getString("DB2Plugin.showTriggerSource");
        
    }
    
    /**
     * Return the internal name of this plugin.
     *
     * @return  the internal name of this plugin.
     */
    public String getInternalName()
    {
        return "db2";
    }

    /**
     * Return the descriptive name of this plugin.
     *
     * @return  the descriptive name of this plugin.
     */
    public String getDescriptiveName()
    {
        return "DB2 Plugin";
    }

    /**
     * Returns the current version of this plugin.
     *
     * @return  the current version of this plugin.
     */
    public String getVersion()
    {
        return "0.03";
    }

    /**
     * Returns the authors name.
     *
     * @return  the authors name.
     */
    public String getAuthor()
    {
        return "Rob Manning";
    }

    /**
     * Returns a comma separated list of other contributors.
     *
     * @return  Contributors names.
     */    
    public String getContributors() {
        return "Christoph Schmitz";
    }    
    
    /**
     * @see net.sourceforge.squirrel_sql.client.plugin.IPlugin#getChangeLogFileName()
     */
    public String getChangeLogFileName()
    {
        return "changes.txt";
    }

    /**
     * @see net.sourceforge.squirrel_sql.client.plugin.IPlugin#getHelpFileName()
     */
    public String getHelpFileName()
    {
        return "readme.html";
    }

    /**
     * @see net.sourceforge.squirrel_sql.client.plugin.IPlugin#getLicenceFileName()
     */
    public String getLicenceFileName()
    {
        return "licence.txt";
    }

	/**
	 * Load this plugin.
	 *
	 * @param	app	 Application API.
	 */
	public synchronized void load(IApplication app) throws PluginException
	{
		super.load(app);
	}

	/**
	 * Initialize this plugin.
	 */
	public synchronized void initialize() throws PluginException
	{
		super.initialize();
	}

    /**
     * Application is shutting down so save preferences.
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
     * Session has been started. Update the tree api in using the event thread
     *
     * @param   session     Session that has started.
     *
     * @return  <TT>true</TT> if session is Oracle in which case this plugin
     *                          is interested in it.
     */
    public PluginSessionCallback sessionStarted(final ISession session)
    {
       
       if (!isPluginSession(session)) {
           return null;
       }
       GUIUtils.processOnSwingEventThread(new Runnable() {
           public void run() {
               updateTreeApi(session);
           }
       });

       // Install DB2JCCExceptionFormatter iff we're using the JCC driver
       try {
           if (JCC_DRIVER_NAME.equals(session.getMetaData().getJDBCMetaData().getDriverName())) {
               session.setExceptionFormatter(new DB2JCCExceptionFormatter());
           }
       } catch (SQLException e) {
           s_log.error("Problem installing exception formatter: " + e.getMessage());
       }    
  
       return new PluginSessionCallback()
       {
          public void sqlInternalFrameOpened(SQLInternalFrame sqlInternalFrame, 
                                             ISession sess)
          {
             // Supports Session main window only
          }

          public void objectTreeInternalFrameOpened(ObjectTreeInternalFrame objectTreeInternalFrame, 
                                                    ISession sess)
          {
             // Supports Session main window only
          }
       };


    }

    @Override
    protected boolean isPluginSession(ISession session) {
        return DialectFactory.isDB2(session.getMetaData());
    }
    
    private void updateTreeApi(ISession session) {
        String stmtSep = session.getQueryTokenizer().getSQLStatementSeparator();
        
        
        _treeAPI = session.getSessionInternalFrame().getObjectTreeAPI();
        _treeAPI.addDetailTab(DatabaseObjectType.PROCEDURE, 
                new ProcedureSourceTab(i18n.SHOW_PROCEDURE_SOURCE));
        _treeAPI.addDetailTab(DatabaseObjectType.VIEW, 
                              new ViewSourceTab(i18n.SHOW_VIEW_SOURCE, stmtSep));
        
        
        _treeAPI.addDetailTab(DatabaseObjectType.INDEX, new DatabaseObjectInfoTab());
        _treeAPI.addDetailTab(DatabaseObjectType.INDEX, new IndexDetailsTab());

        _treeAPI.addDetailTab(DatabaseObjectType.TRIGGER, new DatabaseObjectInfoTab());
        _treeAPI.addDetailTab(DatabaseObjectType.TRIGGER_TYPE_DBO, new DatabaseObjectInfoTab());
        _treeAPI.addDetailTab(DatabaseObjectType.SEQUENCE, new DatabaseObjectInfoTab());
        _treeAPI.addDetailTab(DatabaseObjectType.SEQUENCE, new SequenceDetailsTab());        

        // Expanders - trigger and index expanders are added inside the table
        // expander
        _treeAPI.addExpander(DatabaseObjectType.SCHEMA, new SchemaExpander());        
        
        // Expanders - trigger and index expanders are added inside the table
        // expander        
        TableWithChildNodesExpander tableExpander = 
            new TableWithChildNodesExpander(); 
        
        //tableExpander.setTableIndexExtractor(extractor);
        ITableIndexExtractor indexExtractor = 
            new DB2TableIndexExtractorImpl();
        ITableTriggerExtractor triggerExtractor = 
            new DB2TableTriggerExtractorImpl();
        
        tableExpander.setTableTriggerExtractor(triggerExtractor);
        tableExpander.setTableIndexExtractor(indexExtractor);
        
        _treeAPI.addExpander(DatabaseObjectType.TABLE, tableExpander);
        
        
        _treeAPI.addDetailTab(DatabaseObjectType.TRIGGER, new TriggerDetailsTab());
        _treeAPI.addDetailTab(DatabaseObjectType.TRIGGER, 
                              new TriggerSourceTab("The source of the trigger", stmtSep));
        
    }
    
}
