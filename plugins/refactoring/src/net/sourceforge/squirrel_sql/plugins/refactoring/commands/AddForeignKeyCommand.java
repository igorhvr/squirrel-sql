package net.sourceforge.squirrel_sql.plugins.refactoring.commands;
/*
 * Copyright (C) 2007 Daniel Regli & Yannick Winiger
 * http://sourceforge.net/projects/squirrel-sql
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

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.SQLExecuterTask;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.fw.sql.TableColumnInfo;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.plugins.refactoring.gui.AddForeignKeyDialog;
import net.sourceforge.squirrel_sql.plugins.refactoring.hibernate.DatabaseObjectQualifier;
import net.sourceforge.squirrel_sql.plugins.refactoring.hibernate.IHibernateDialectExtension;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.TreeSet;


public class AddForeignKeyCommand extends AbstractRefactoringCommand {
    /**
     * Logger for this class.
     */
    private final static ILogger s_log = LoggerController.createLogger(AddForeignKeyCommand.class);

    /**
     * Internationalized strings for this class.
     */
    private static final StringManager s_stringMgr = StringManagerFactory.getStringManager(AddForeignKeyCommand.class);

    static interface i18n {
        String SHOWSQL_DIALOG_TITLE = s_stringMgr.getString("AddForeignKeyCommand.sqlDialogTitle");
    }

    protected AddForeignKeyDialog customDialog;


    public AddForeignKeyCommand(ISession session, IDatabaseObjectInfo[] info) {
        super(session, info);
    }


    @Override
    protected void onExecute() throws SQLException {
        if (!(_info[0] instanceof ITableInfo)) return;

        showCustomDialog();
    }


    protected void showCustomDialog() throws SQLException {
        final ITableInfo selectedTable = (ITableInfo) _info[0];
        String schema = selectedTable.getSchemaName();
        String catalog = selectedTable.getCatalogName();
        ITableInfo[] tables = _session.getSchemaInfo().getITableInfos(catalog, schema);

        TableColumnInfo[] tableColumnInfos = _session.getMetaData().getColumnInfo(selectedTable);
        if (tableColumnInfos == null || tableColumnInfos.length == 0) {
            _session.showErrorMessage(s_stringMgr.getString("AddForeignKeyCommand.noColumns",
                    selectedTable.getSimpleName()));
            return;
        }

        final TreeSet<String> localColumns = new TreeSet<String>();
        for (TableColumnInfo columns : tableColumnInfos) {
            localColumns.add(columns.getColumnName());
        }

        final HashMap<String, TableColumnInfo[]> allTables = new HashMap<String, TableColumnInfo[]>();
        for (ITableInfo table : tables) {
            if (table.getDatabaseObjectType() == DatabaseObjectType.TABLE) {
                TableColumnInfo[] columnInfos = _session.getMetaData().getColumnInfo(table);
                if (columnInfos != null && columnInfos.length > 0) {
                    allTables.put(table.getSimpleName(), _session.getMetaData().getColumnInfo(table));
                }
            }
        }

        _session.getApplication().getThreadPool().addTask(new Runnable() {
            public void run() {
                GUIUtils.processOnSwingEventThread(new Runnable() {
                    public void run() {
                        customDialog = new AddForeignKeyDialog(selectedTable.getSimpleName(), localColumns.toArray(new String[]{}), allTables);
                        customDialog.addExecuteListener(new ExecuteListener());
                        customDialog.addEditSQLListener(new EditSQLListener(customDialog));
                        customDialog.addShowSQLListener(new ShowSQLListener(i18n.SHOWSQL_DIALOG_TITLE, customDialog));

                        customDialog.setLocationRelativeTo(_session.getApplication().getMainFrame());
                        customDialog.setVisible(true);
                    }
                });
            }
        });
    }


    @Override
    protected String[] generateSQLStatements() throws Exception {
        DatabaseObjectQualifier qualifier = new DatabaseObjectQualifier(_info[0].getCatalogName(), _info[0].getSchemaName());

        String result = _dialect.getAddForeignKeyConstraintSQL(_info[0].getSimpleName(), customDialog.getReferencedTable(),
                customDialog.getConstraintName(), customDialog.isDeferrable(), customDialog.isDeferred(),
                customDialog.isMatchFull(), customDialog.isAutoFKIndex(), customDialog.getFKIndexName(),
                customDialog.getReferencedColumns(), customDialog.getOnUpdateAction(), customDialog.getOnDeleteAction(),
                qualifier, _sqlPrefs);
        return new String[]{result};
    }


    @Override
    protected void executeScript(String script) {
        CommandExecHandler handler = new CommandExecHandler(_session);

        SQLExecuterTask executer = new SQLExecuterTask(_session, script, handler);
        executer.run(); // Execute the sql synchronously

        _session.getApplication().getThreadPool().addTask(new Runnable() {
            public void run() {
                GUIUtils.processOnSwingEventThread(new Runnable() {
                    public void run() {
                        customDialog.setVisible(false);
                        _session.getSchemaInfo().reloadAll();
                    }
                });
            }
        });
    }


	/**
	 * Returns a boolean value indicating whether or not this refactoring is supported for the specified 
	 * dialect. 
	 * 
	 * @param dialectExt the IHibernateDialectExtension to check
	 * @return true if this refactoring is supported; false otherwise.
	 */
	@Override
	protected boolean isRefactoringSupportedForDialect(IHibernateDialectExtension dialectExt)
	{
		return dialectExt.supportsAddForeignKeyConstraint();
	}
}