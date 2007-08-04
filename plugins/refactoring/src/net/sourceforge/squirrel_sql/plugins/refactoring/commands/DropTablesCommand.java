package net.sourceforge.squirrel_sql.plugins.refactoring.commands;
/*
 * Copyright (C) 2006 Rob Manning
 * manningr@user.sourceforge.net
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.sourceforge.squirrel_sql.client.gui.ProgessCallBackDialog;
import net.sourceforge.squirrel_sql.client.session.DefaultSQLExecuterHandler;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.SQLExecuterTask;
import net.sourceforge.squirrel_sql.fw.dialects.DialectFactory;
import net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect;
import net.sourceforge.squirrel_sql.fw.dialects.UserCancelledOperationException;
import net.sourceforge.squirrel_sql.fw.gui.ErrorDialog;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.sql.ForeignKeyInfo;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.fw.sql.SQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.sql.SQLUtilities;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.StringUtilities;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;


public class DropTablesCommand extends AbstractRefactoringCommand
{
    /** Logger for this class. */
    private final ILogger s_log =
        LoggerController.createLogger(DropTablesCommand.class);
    
    /** Internationalized strings for this class */
    private static final StringManager s_stringMgr =
        StringManagerFactory.getStringManager(DropTablesCommand.class);
    
    private List<ITableInfo> orderedTables = null;
    
    private DropTableCommandExecHandler handler = null;
    
    ProgessCallBackDialog getOrderedTablesCallBack = null;
    
    private static interface i18n {
                
        //i18n[DropTablesCommand.progressDialogAnalyzeTitle=Analyzing tables to drop]
        String PROGRESS_DIALOG_ANALYZE_TITLE = 
            s_stringMgr.getString("DropTablesCommand.progressDialogAnalyzeTitle");
        
        //i18n[DropTablesCommand.progressDialogDropTitle=Dropping tables]
        String PROGRESS_DIALOG_DROP_TITLE = 
            s_stringMgr.getString("DropTablesCommand.progressDialogDropTitle");        
        
        //i18n[DropTablesCommand.loadingPrefix=Analyzing table:]
        String LOADING_PREFIX = 
            s_stringMgr.getString("DropTablesCommand.loadingPrefix");

        //i18n[DropTablesCommand.droppingConstraintPrefix=Dropping Constraint:]
        String DROPPING_CONSTRAINT_PREFIX = 
            s_stringMgr.getString("DropTablesCommand.droppingConstraintPrefix");

        //i18n[DropTablesCommand.droppingTablePrefix=Dropping table:]
        String DROPPING_TABLE_PREFIX = 
            s_stringMgr.getString("DropTablesCommand.droppingTablePrefix");
        

    }
    
    /** 
     * A set of materialized view names in the same schema as the table(s) 
     * being dropped
     */
    private HashSet<String> matViewLookup = null;
    
	/**
	 *
	 * @param	session		Current session..
	 * @param	tables		Array of <TT>IDatabaseObjectInfo</TT> objects
	 * 						representing the tables to be deleted.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a <TT>null</TT> <TT>ISession</TT> passed.
	 */
	public DropTablesCommand(ISession session, IDatabaseObjectInfo[] tables)
	{
		super(session, tables);
	}

	/**
	 * Drop selected tables in the object tree.
	 */
	public void execute()
	{
        try {
            super.showDropTableDialog(new DropTablesActionListener(), 
                                      new ShowSQLListener());
        } catch (Exception e) {
            s_log.error("Unexpected exception "+e.getMessage(), e);
        }
	}

    @Override
    protected void getSQLFromDialog(SQLResultListener listener) {
        HibernateDialect dialect = null; 
        List<ITableInfo> tables = dropTableDialog.getTableInfoList();
        boolean cascadeConstraints = dropTableDialog.getCascadeConstraints();
        
        ArrayList<String> result = new ArrayList<String>();
        try {
            orderedTables = getOrderedTables(tables);
            
            dialect = DialectFactory.getDialect(DialectFactory.DEST_TYPE, 
                                                _session.getApplication().getMainFrame(), 
                                                _session.getMetaData()); 
            String sep = _session.getQueryTokenizer().getSQLStatementSeparator();
            
            // Drop FK constraints before dropping any tables.  Otherwise, we 
            // may drop the child table prior to dropping it's FKs, which would
            // be an error.
            if (cascadeConstraints)  {
                for (ITableInfo info: orderedTables) {
                    List<String> dropFKSQLs = 
                        getDropChildFKConstraints(dialect, info);
                    for (String dropFKSQL : dropFKSQLs) {
                        StringBuilder dropSQL = new StringBuilder(); 
                        dropSQL.append(dropFKSQL);
                        dropSQL.append("\n");
                        dropSQL.append(sep);
                        result.add(dropSQL.toString());                        
                    }
                }
            }
            
            // Set cascadeConstraints to false, since we've already generated the
            // SQL for dropping these constraints above.
            for (ITableInfo info : orderedTables) {
                boolean isMaterializedView = isMaterializedView(info, _session);
                List<String> sqls = dialect.getTableDropSQL(info, 
                                                            false, // cascadeConstraints                      
                                                            isMaterializedView);
                for (String sql : sqls) {
                    StringBuilder dropSQL = new StringBuilder(); 
                    dropSQL.append(sql);
                    dropSQL.append("\n");
                    dropSQL.append(sep);
                    result.add(dropSQL.toString());
                }
            }            
        } catch (UnsupportedOperationException e2) {
            //i18n[DropTablesCommand.unsupportedOperationMsg=The {0} 
            //dialect doesn't support dropping tables]
            String msg = 
                s_stringMgr.getString("DropTablesCommand.unsupportedOperationMsg",
                                      dialect.getDisplayName());
            _session.showMessage(msg);
        } catch (UserCancelledOperationException e) {
            // user cancelled selecting a dialect. do nothing?
        }
        listener.finished(result.toArray(new String[result.size()]));
    }
    
    private List<String> getDropChildFKConstraints(HibernateDialect dialect, 
                                                   ITableInfo ti) {
        ArrayList<String> result = new ArrayList<String>();
        ForeignKeyInfo[] fks = ti.getExportedKeys();
        for (int i = 0; i < fks.length; i++) {
            ForeignKeyInfo info = fks[i];
            String fkName = info.getForeignKeyName();
            String fkTable = info.getForeignKeyTableName();
            result.add(dialect.getDropForeignKeySQL(fkName, fkTable));            
        }
        return result;
    }
    
    private List<ITableInfo> getOrderedTables(final List<ITableInfo> tables) {
        List<ITableInfo> result = tables;
        SQLDatabaseMetaData md = _session.getSQLConnection().getSQLMetaData();
        
        try {
            // Create the analysis dialog using the EDT, and wait for it to finish.
            GUIUtils.processOnSwingEventThread(new Runnable() {
                public void run() {
                    getOrderedTablesCallBack = 
                        new ProgessCallBackDialog(dropTableDialog,
                                                  i18n.PROGRESS_DIALOG_ANALYZE_TITLE,
                                                  tables.size());
                    
                    getOrderedTablesCallBack.setLoadingPrefix(i18n.LOADING_PREFIX);                    
                }
            }, true);
            
            // Now, get the drop order (same as delete) and update the dialog
            // status while doing so.
            result = SQLUtilities.getDeletionOrder(tables, 
                                                   md, 
                                                   getOrderedTablesCallBack);
        } catch (SQLException e) {
            s_log.error(
                "Encountered exception while attempting to order tables " +
                "according to constraints: "+e.getMessage(), e);
        }
        return result;
    }
    
    /**
     * Returns a boolean value indicating whether or not the specified table 
     * info is not only a table, but also a materialized view.
     * 
     * @param ti
     * @param session
     * @return
     */
    private boolean isMaterializedView(ITableInfo ti,
                                      ISession session)
    {
        if (!DialectFactory.isOracle(session.getMetaData())) {
            // Only Oracle supports materialized views directly.
            return false;
        }
        if (matViewLookup == null) {
            initMatViewLookup(session, ti.getSchemaName());
        }
        return matViewLookup.contains(ti.getSimpleName());
    }

    private void initMatViewLookup(ISession session, String schema) {
        matViewLookup = new HashSet<String>();
        // There is no good way using JDBC metadata to tell if the table is a 
        // materialized view.  So, we need to query the data dictionary to find
        // that out.  Get all table names whose comment indicates that they are
        // a materialized view.
        String sql = 
            "SELECT TABLE_NAME FROM ALL_TAB_COMMENTS " +
            "where COMMENTS like 'snapshot%' " +
            "and OWNER = ? ";
        
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = session.getSQLConnection().prepareStatement(sql);
            stmt.setString(1, schema);
            rs = stmt.executeQuery();
            if (rs.next()) {
                String tableName = rs.getString(1);
                matViewLookup.add(tableName);
            }
        } catch (SQLException e) {
            s_log.error(
                "Unexpected exception while attempting to find mat. views " +
                "in schema: "+schema, e);
        } finally {
            SQLUtilities.closeResultSet(rs);
            SQLUtilities.closeStatement(stmt);            
        }
        
    }
    
    private class ShowSQLListener implements ActionListener, SQLResultListener {
        
        
        /* (non-Javadoc)
         * @see net.sourceforge.squirrel_sql.plugins.refactoring.commands.DropTablesCommand.SQLResultListener#finished(java.lang.String[])
         */
        public void finished(String[] sql) {
            if (sql.length == 0) {
//              TODO: tell the user no changes
                return;
            }
            StringBuffer script = new StringBuffer();
            for (int i = 0; i < sql.length; i++) {
                script.append(sql[i]);
                script.append("\n\n");                
            }

            ErrorDialog sqldialog = 
                new ErrorDialog(dropTableDialog, script.toString());
            //i18n[DropTablesCommand.sqlDialogTitle=Drop Table SQL]
            String title = 
                s_stringMgr.getString("DropTablesCommand.sqlDialogTitle");
            sqldialog.setTitle(title);
            sqldialog.setVisible(true);                
        }

        public void actionPerformed( ActionEvent e) {
            _session.getApplication().getThreadPool().addTask(new GetSQLTask(this));
        }
    }
    
    private class DropTablesActionListener implements ActionListener, SQLResultListener {

        /* (non-Javadoc)
         * @see net.sourceforge.squirrel_sql.plugins.refactoring.commands.DropTablesCommand.SQLResultListener#finished(java.lang.String[])
         */
        public void finished(String[] sqls) {
            final StringBuilder script = new StringBuilder();
            for (int i = 0; i < sqls.length; i++) {
                String sql = sqls[i];
                if (s_log.isDebugEnabled()) {
                    s_log.debug("DropTablesCommand: adding SQL - "+sql);
                }
                script.append(sql);
                script.append("\n");
            }
            // Shows the user a dialog to let them know what's happening
            GUIUtils.processOnSwingEventThread(new Runnable() {
                public void run() {
                    DropTablesCommand.this.handler = 
                        new DropTableCommandExecHandler(_session);

                    final SQLExecuterTask executer = 
                        new SQLExecuterTask(_session, 
                                            script.toString(), 
                                            DropTablesCommand.this.handler);
                    executer.setSchemaCheck(false);
                    _session.getApplication().getThreadPool().addTask(new Runnable() {
                        public void run() {
                            executer.run();
                            
                            GUIUtils.processOnSwingEventThread(new Runnable() {
                                public void run() {
                                    dropTableDialog.setVisible(false);
                                    _session.getSchemaInfo().reloadAll();
                                }
                            });
                        }
                    });
                    
                    
                }
            });            
        }

        public void actionPerformed(ActionEvent e) {
            if (dropTableDialog == null) {
                return;
            }
            _session.getApplication().getThreadPool().addTask(new GetSQLTask(this));
        }
        
    }
    
    public class GetSQLTask implements Runnable {
        
        private SQLResultListener _listener;
        
        public GetSQLTask(SQLResultListener listener) {
            _listener = listener;
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            getSQLFromDialog(_listener);
        }
    }

    private class DropTableCommandExecHandler extends DefaultSQLExecuterHandler {
        
        ProgessCallBackDialog cb = null;
                
        /** This is used to track the number of tables seen so far, so that we
         *  can pick the right one from the ordered table list to display as the
         *  table name of the table currently being dropped - yes, a hack!
         */
        int tableCount = 0;
        
        public DropTableCommandExecHandler(ISession session)
        {
            super(session);
            cb = new ProgessCallBackDialog(dropTableDialog,
                                          i18n.PROGRESS_DIALOG_DROP_TITLE,
                                          DropTablesCommand.this.orderedTables.size());
        }

        
        /* (non-Javadoc)
         * @see net.sourceforge.squirrel_sql.client.session.DefaultSQLExecuterHandler#sqlStatementCount(int)
         */
        @Override
        public void sqlStatementCount(int statementCount) {
            cb.setTotalItems(statementCount);
        }


        /* (non-Javadoc)
         * @see net.sourceforge.squirrel_sql.client.session.DefaultSQLExecuterHandler#sqlToBeExecuted(java.lang.String)
         */
        @Override
        public void sqlToBeExecuted(String sql) {
            if (s_log.isDebugEnabled()) {
                s_log.debug("Statement to be executed: "+sql);
            }
            
            if (sql.startsWith("ALTER")) {
                cb.setLoadingPrefix(i18n.DROPPING_CONSTRAINT_PREFIX);
                // Hack!!! hopefully the FK name will always be the last token!
                String[] parts = StringUtilities.split(sql, ' ');
                cb.currentlyLoading(parts[parts.length - 1]);
            } else {         
                cb.setLoadingPrefix(i18n.DROPPING_TABLE_PREFIX);
                if (tableCount < DropTablesCommand.this.orderedTables.size()) {
                    ITableInfo ti = DropTablesCommand.this.orderedTables.get(tableCount);
                    cb.currentlyLoading(ti.getSimpleName());
                }
                tableCount++;
            }
        }
        
        
    }
    
}
