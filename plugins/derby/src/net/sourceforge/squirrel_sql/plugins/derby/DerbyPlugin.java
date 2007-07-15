package net.sourceforge.squirrel_sql.plugins.derby;
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

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.gui.session.ObjectTreeInternalFrame;
import net.sourceforge.squirrel_sql.client.gui.session.SQLInternalFrame;
import net.sourceforge.squirrel_sql.client.plugin.DefaultSessionPlugin;
import net.sourceforge.squirrel_sql.client.plugin.PluginException;
import net.sourceforge.squirrel_sql.client.plugin.PluginSessionCallback;
import net.sourceforge.squirrel_sql.client.session.IObjectTreeAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.expanders.TableWithTriggersExpander;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.DatabaseObjectInfoTab;
import net.sourceforge.squirrel_sql.fw.dialects.DialectFactory;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType;
import net.sourceforge.squirrel_sql.fw.sql.IQueryTokenizer;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.plugins.derby.exp.DerbyTableTriggerExtractorImpl;
import net.sourceforge.squirrel_sql.plugins.derby.tab.TriggerDetailsTab;
import net.sourceforge.squirrel_sql.plugins.derby.tab.TriggerSourceTab;
import net.sourceforge.squirrel_sql.plugins.derby.tab.ViewSourceTab;
import net.sourceforge.squirrel_sql.plugins.derby.tokenizer.DerbyQueryTokenizer;


/**
 * The main controller class for the Derby plugin.
 * 
 * @author manningr
 */
public class DerbyPlugin extends DefaultSessionPlugin {
    
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(DerbyPlugin.class);

    /** Logger for this class. */
    private final static ILogger s_log = 
        LoggerController.createLogger(DerbyPlugin.class);

    /** API for the Obejct Tree. */
    private IObjectTreeAPI _treeAPI;

    static interface i18n {
        //i18n[DerbyPlugin.showViewSource=Show view source]
        String SHOW_VIEW_SOURCE = 
            s_stringMgr.getString("DerbyPlugin.showViewSource");
        
        //i18n[DerbyPlugin.showProcedureSource=Show procedure source]
        String SHOW_PROCEDURE_SOURCE =
            s_stringMgr.getString("DerbyPlugin.showProcedureSource");
    }
    
    /**
     * Return the internal name of this plugin.
     *
     * @return  the internal name of this plugin.
     */
    public String getInternalName()
    {
        return "derby";
    }

    /**
     * Return the descriptive name of this plugin.
     *
     * @return  the descriptive name of this plugin.
     */
    public String getDescriptiveName()
    {
        return "Derby Plugin";
    }

    /**
     * Returns the current version of this plugin.
     *
     * @return  the current version of this plugin.
     */
    public String getVersion()
    {
        return "0.11";
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
        return "";
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
       s_log.info("Installing Derby query tokenizer");
       IQueryTokenizer orig = session.getQueryTokenizer();
       DerbyQueryTokenizer tokenizer = 
           new DerbyQueryTokenizer(orig.getSQLStatementSeparator(),
                                   orig.getLineCommentBegin(),
                                   orig.isRemoveMultiLineComment());
       session.setQueryTokenizer(tokenizer);
       GUIUtils.processOnSwingEventThread(new Runnable() {
           public void run() {
               updateTreeApi(session);
           }
       });

       return new PluginSessionCallback()
       {
           public void sqlInternalFrameOpened(SQLInternalFrame sqlInternalFrame, ISession sess)
           {
               // Supports Session main window only
           }

           public void objectTreeInternalFrameOpened(ObjectTreeInternalFrame objectTreeInternalFrame, ISession sess)
           {
               // Supports Session main window only
           }
       };


   }

    @Override
    protected boolean isPluginSession(ISession session) {
        return DialectFactory.isDerby(session.getMetaData());
    }
    
    private void updateTreeApi(ISession session) {
        
        _treeAPI = session.getSessionInternalFrame().getObjectTreeAPI();
        
        //_treeAPI.addDetailTab(DatabaseObjectType.PROCEDURE, 
        //        new ProcedureSourceTab(i18n.SHOW_PROCEDURE_SOURCE));
        
        _treeAPI.addDetailTab(DatabaseObjectType.VIEW, 
                              new ViewSourceTab(i18n.SHOW_VIEW_SOURCE));
        
        
        //_treeAPI.addDetailTab(DatabaseObjectType.INDEX, new DatabaseObjectInfoTab());
        //_treeAPI.addDetailTab(DatabaseObjectType.INDEX, new IndexDetailsTab());
        _treeAPI.addDetailTab(DatabaseObjectType.TRIGGER, new DatabaseObjectInfoTab());
        _treeAPI.addDetailTab(DatabaseObjectType.TRIGGER_TYPE_DBO, new DatabaseObjectInfoTab());
        //_treeAPI.addDetailTab(DatabaseObjectType.SEQUENCE, new DatabaseObjectInfoTab());
        //_treeAPI.addDetailTab(DatabaseObjectType.SEQUENCE, new SequenceDetailsTab());        
        
        // Expanders - trigger and index expanders are added inside the table
        // expander
        //_treeAPI.addExpander(DatabaseObjectType.SCHEMA, new SchemaExpander());
        TableWithTriggersExpander trigExp = new TableWithTriggersExpander();
        trigExp.setTableTriggerExtractor(new DerbyTableTriggerExtractorImpl());
        _treeAPI.addExpander(DatabaseObjectType.TABLE, trigExp);
        
        
        _treeAPI.addDetailTab(DatabaseObjectType.TRIGGER, new TriggerDetailsTab());
        _treeAPI.addDetailTab(DatabaseObjectType.TRIGGER, new TriggerSourceTab("The source of the trigger"));
        
    }
    
}
