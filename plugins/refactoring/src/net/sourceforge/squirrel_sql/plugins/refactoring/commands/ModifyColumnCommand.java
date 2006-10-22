package net.sourceforge.squirrel_sql.plugins.refactoring.commands;

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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;

import net.sourceforge.squirrel_sql.client.db.dialects.DialectFactory;
import net.sourceforge.squirrel_sql.client.gui.db.ColumnDetailDialog;
import net.sourceforge.squirrel_sql.client.gui.db.ColumnListDialog;
import net.sourceforge.squirrel_sql.client.gui.mainframe.MainFrame;
import net.sourceforge.squirrel_sql.client.session.DefaultSQLExecuterHandler;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect;
import net.sourceforge.squirrel_sql.fw.dialects.UserCancelledOperationException;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.fw.sql.SQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.sql.TableColumnInfo;
import net.sourceforge.squirrel_sql.fw.util.ICommand;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

/**
 * Implements showing a list of columns for a selected table to the 
 * user and dropping the ones that are selected when the user presses the 
 * drop column(s) button.
 * 
 * @author rmmannin
 *
 */
public class ModifyColumnCommand implements ICommand
{
    /**
     * Current session.
     */
    private ISession _session;
    
    /**
     * Currently selected 
     */
    private final IDatabaseObjectInfo _info;
    
    /** Logger for this class. */
    private final static ILogger log = 
                       LoggerController.createLogger(RemoveColumnCommand.class);
    
    /** Internationalized strings for this class. */
    private static final StringManager s_stringMgr =
        StringManagerFactory.getStringManager(RemoveColumnCommand.class);
    
    private ColumnListDialog listDialog = null;
    
    private ColumnDetailDialog detailDialog = null;
    
    private MainFrame mainFrame = null;    
    
    private TableColumnInfo[] columns = null;
    
    private HibernateDialect dialect = null;    
    /**
     * Ctor specifying the current session.
     */
    public ModifyColumnCommand(ISession session, IDatabaseObjectInfo info)
    {
        super();
        _session = session;
        _info = info;
    }
    
    /**
     * Execute this command. Save the session and selected objects in the plugin
     * for use in paste command.
     */
    public void execute()
    {
        if (! (_info instanceof ITableInfo)) {
            return;
        }
        try {
            ITableInfo ti = (ITableInfo)_info;
            columns = 
                _session.getSQLConnection().getSQLMetaData().getColumnInfo(ti);
            
            //Show the user a dialog with a list of columns and ask them to select
            // one or more columns to drop
            ArrayList tmp = new ArrayList();
            for (int i = 0; i < columns.length; i++) {
                TableColumnInfo info = columns[i];
                tmp.add(info.getColumnName());
            }
            if (listDialog == null) {
                String[] tableColumns = 
                    (String[])tmp.toArray(new String[tmp.size()]);
                listDialog = 
                    new ColumnListDialog(tableColumns, 
                                         ColumnListDialog.MODIFY_COLUMN_MODE);
                listDialog.addColumnSelectionListener(new ColumnListSelectionActionListener());
                mainFrame = _session.getApplication().getMainFrame();
                listDialog.setLocationRelativeTo(mainFrame);
                listDialog.setSingleSelection();
            }
            listDialog.setTableName(ti.getSimpleName());
            listDialog.setVisible(true);
        } catch (SQLException e) {
            log.error("Unexpected exception "+e.getMessage(), e);
        }
        
        
    }

    private class ColumnListSelectionActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (listDialog == null) {
                System.err.println("dialog was null");
                return;
            }
            ModifyColumnExecHandler handler = new ModifyColumnExecHandler(_session);
            String tableName = listDialog.getTableName();
            Object[] columnNames = listDialog.getSelectedColumnList();
            if (columnNames == null || columnNames.length != 1) {
                System.err.println("Exactly one column must be selected to modify");
                return;
            }
            String selectedColumn = (String)columnNames[0];
            int foundIdx = -1;
            for (int i = 0; i < columns.length; i++) {
                TableColumnInfo tcinfo = columns[i];
                if (tcinfo.getColumnName().equals(selectedColumn)) {
                    foundIdx = i;
                    break;
                }
            }
            if (foundIdx != -1) {
                try {
                    dialect =  
                        DialectFactory.getDialect(_session, DialectFactory.DEST_TYPE);
                    String dbName = dialect.getDisplayName();                
                    detailDialog = 
                        new ColumnDetailDialog(ColumnDetailDialog.MODIFY_MODE);
                    detailDialog.setExistingColumnInfo(columns[foundIdx]);
                    
                    mainFrame = _session.getApplication().getMainFrame();
                    detailDialog.setLocationRelativeTo(mainFrame);
                    detailDialog.setSelectedDialect(dbName);
                    detailDialog.setVisible(true);
                } catch (UserCancelledOperationException ex) {
                    ex.printStackTrace();
                }
            } else {
                System.err.println("Didn't find TableColumnInfo");
            }
            //TableColumnInfo colInfo = 
            /*
            for (int i = 0; i < columnNames.length; i++) {
                String columnName = (String)columnNames[i];
                String dropSQL = 
                    DialectUtils.getColumnDropSQL(tableName, columnName);
                log.info("AddColumnCommand: executing SQL - "+dropSQL);
                SQLExecuterTask executer = 
                    new SQLExecuterTask(_session, 
                                        dropSQL, 
                                        handler);
    
                // Execute the sql synchronously
                executer.run();                
                
                if (handler.exceptionEncountered) {
                    // Stop processing statements
                    break;
                }
                
            }
            */
            listDialog.setVisible(false);
        }
        
    }
    
    private class ModifyColumnExecHandler extends DefaultSQLExecuterHandler {
        private boolean exceptionEncountered = false;
        
        public ModifyColumnExecHandler(ISession session) {
            super(session);
        }

        /* (non-Javadoc)
         * @see net.sourceforge.squirrel_sql.client.session.DefaultSQLExecuterHandler#sqlExecutionException(java.lang.Throwable, java.lang.String)
         */
        public void sqlExecutionException(Throwable th, String postErrorString) {
            super.sqlExecutionException(th, postErrorString);
            exceptionEncountered = true;
        }
    }
    
}