package net.sourceforge.squirrel_sql.plugins.dbcopy.commands;

/*
 * Copyright (C) 2005 Rob Manning
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ProgressMonitor;

import net.sourceforge.squirrel_sql.client.gui.ProgessCallBackDialog;
import net.sourceforge.squirrel_sql.client.session.IObjectTreeAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.fw.sql.SQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.sql.SQLUtilities;
import net.sourceforge.squirrel_sql.fw.util.ICommand;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.plugins.dbcopy.DBCopyPlugin;
import net.sourceforge.squirrel_sql.plugins.dbcopy.util.Compat;

public class CopyTableCommand implements ICommand
{
    /**
     * Current session.
     */
    private ISession _session;
    
    /**
     * Current plugin.
     */
    private final DBCopyPlugin _plugin;
    
    /** Logger for this class. */
    private final static ILogger log = 
                         LoggerController.createLogger(CopyTableCommand.class);
    
    /** Internationalized strings for this class */
    private static final StringManager s_stringMgr =
        StringManagerFactory.getStringManager(CopyTableCommand.class);   
    
    static interface i18n {
        
        //i18n[CopyTablesCommand.progressDialogTitle=Analyzing FKs in Tables to Copy]
        String PROGRESS_DIALOG_TITLE = 
            s_stringMgr.getString("CopyTablesCommand.progressDialogTitle");
        
        //i18n[CopyTablesCommand.loadingPrefix=Analyzing table:]
        String LOADING_PREFIX = 
            s_stringMgr.getString("CopyTablesCommand.loadingPrefix");        
        
    }
    
    
    /** When analyzing FK dependencies for the selected tables, show the
     *  user the progress since this can take a while.
     */
    ProgressMonitor pm = null;
    
    int progressCount = 0;
    
    /**
     * Ctor specifying the current session.
     */
    public CopyTableCommand(ISession session, DBCopyPlugin plugin)
    {
        super();
        _session = session;
        _plugin = plugin;
    }
    
    /**
     * Execute this command. Save the session and selected objects in the plugin
     * for use in paste command.
     */
    public void execute()
    {
        IObjectTreeAPI api = Compat.getIObjectTreeAPI(_session, _plugin);
        if (api != null) {
            IDatabaseObjectInfo[] dbObjs = api.getSelectedDatabaseObjects();
            if (Compat.isTableTypeDBO(dbObjs[0].getDatabaseObjectType())) {
            	String catalog = dbObjs[0].getCatalogName();
            	String schema = dbObjs[0].getSchemaName();
            	if (log.isDebugEnabled()) {
	            	log.debug("CopyTableCommand.execute: catalog="+catalog);
	            	log.debug("CopyTableCommand.execute: schema="+schema);
            	}	
            	dbObjs = Compat.getTables(_session, catalog, schema, null);
            	for (int i = 0; i < dbObjs.length; i++) {
            		ITableInfo info = (ITableInfo)dbObjs[i];
            		if (log.isDebugEnabled()) {
            			log.debug("dbObj["+i+"] = "+info.getSimpleName());
            		}
				}
            }

            _plugin.setCopySourceSession(_session);
            final IDatabaseObjectInfo[] fdbObjs = dbObjs;
            final SQLDatabaseMetaData md = 
                _session.getSQLConnection().getSQLMetaData();
            _session.getApplication().getThreadPool().addTask(new Runnable() {
                public void run() {
                    try {
                        getInsertionOrder(fdbObjs, md);
                        _plugin.setPasteMenuEnabled(true);
                    } catch (SQLException e) {
                        log.error("Unexected exception: ", e);
                    }
                }
            });
            
        } 
    }
    
    private void getInsertionOrder(IDatabaseObjectInfo[] dbObjs,
                                                    SQLDatabaseMetaData md) 
        throws SQLException
    {
        List<ITableInfo> selectedTables = new ArrayList<ITableInfo>();
        for (int i = 0; i < dbObjs.length; i++) {
            selectedTables.add((ITableInfo)dbObjs[i]);
        }
        ProgessCallBackDialog cb = 
            new ProgessCallBackDialog(_session.getApplication().getMainFrame(), 
                                       i18n.PROGRESS_DIALOG_TITLE,
                                       dbObjs.length);
        
        cb.setLoadingPrefix(i18n.LOADING_PREFIX);
        
        selectedTables = 
            SQLUtilities.getInsertionOrder(selectedTables, 
                                           md, 
                                           cb);
        
        _plugin.setSelectedDatabaseObjects(
                selectedTables.toArray(new IDatabaseObjectInfo[dbObjs.length]));
    }
    
}