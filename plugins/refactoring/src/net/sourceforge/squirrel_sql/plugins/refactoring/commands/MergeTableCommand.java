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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.SQLExecuterTask;
import net.sourceforge.squirrel_sql.fw.dialects.DatabaseObjectQualifier;
import net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect;
import net.sourceforge.squirrel_sql.fw.dialects.UserCancelledOperationException;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.ISQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.fw.sql.JDBCTypeMapper;
import net.sourceforge.squirrel_sql.fw.sql.SQLUtilities;
import net.sourceforge.squirrel_sql.fw.sql.TableColumnInfo;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.plugins.refactoring.gui.IMergeTableDialog;
import net.sourceforge.squirrel_sql.plugins.refactoring.gui.IMergeTableDialogFactory;

public class MergeTableCommand extends AbstractRefactoringCommand
{
	/**
	 * Logger for this class.
	 */
	private final static ILogger s_log = LoggerController.createLogger(MergeTableCommand.class);

	/**
	 * Internationalized strings for this class.
	 */
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(MergeTableCommand.class);

	static interface i18n
	{
		String SHOWSQL_DIALOG_TITLE = s_stringMgr.getString("MergeTableCommand.sqlDialogTitle");
	}

	/** The dialog implementation that gets displayed when we are executed */
	protected IMergeTableDialog customDialog;

	/** The factory that creates the custom dialog for us. */
	protected IMergeTableDialogFactory dialogFactory = null;

	protected HashMap<String, TableColumnInfo[]> _allTables;

	/**
	 * @param session
	 * @param info
	 * @param dialogFactory
	 */
	public MergeTableCommand(ISession session, IDatabaseObjectInfo[] info,
		IMergeTableDialogFactory dialogFactory)
	{
		super(session, info);
		this.dialogFactory = dialogFactory;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.plugins.refactoring.commands.AbstractRefactoringCommand#onExecute()
	 */
	@Override
	protected void onExecute() throws SQLException
	{
		if (!(_info[0] instanceof ITableInfo))
			return;

		showCustomDialog();
	}

	/**
	 * @see net.sourceforge.squirrel_sql.plugins.refactoring.commands.AbstractRefactoringCommand#generateSQLStatements()
	 */
	@Override
	protected String[] generateSQLStatements() throws UserCancelledOperationException, SQLException
	{
		ArrayList<String> results = new ArrayList<String>();
		ArrayList<String> updateResults = new ArrayList<String>();

		ISQLDatabaseMetaData md = _session.getMetaData();

		String catalog = _info[0].getCatalogName();
		String schema = _info[0].getSchemaName();
		DatabaseObjectQualifier qualifier = new DatabaseObjectQualifier(catalog, schema);
		String table = _info[0].getSimpleName();
		String mergedTable = customDialog.getReferencedTable();
		TableColumnInfo[] mergedTableColumnInfos = _allTables.get(mergedTable);
		if (s_log.isDebugEnabled())
		{
			s_log.debug("MergedTable = " + mergedTable);
			s_log.debug("Is Null " + (mergedTableColumnInfos == null));
			logDialogInfo();
		}

		// create new columns in table
		if (mergedTableColumnInfos != null)
		{
			for (TableColumnInfo mc : mergedTableColumnInfos)
			{

				if (customDialog.getMergeColumns().contains(mc.getColumnName()))
				{
					if (s_log.isDebugEnabled())
					{
						s_log.debug("Schema" + mc.getSchemaName());
					}
					TableColumnInfo columnInNewTable =
						new TableColumnInfo(	mc.getCatalogName(),
													mc.getSchemaName(),
													table,
													mc.getColumnName(),
													mc.getDataType(),
													JDBCTypeMapper.getJdbcTypeName(mc.getDataType()),
													mc.getColumnSize(),
													mc.getDecimalDigits(),
													mc.getRadix(),
													mc.isNullAllowed(),
													mc.getRemarks(),
													mc.getDefaultValue(),
													mc.getOctetLength(),
													mc.getOrdinalPosition(),
													"YES",
													md);
					String[] statements = _dialect.getAddColumnSQL(columnInNewTable, qualifier, _sqlPrefs);
					results.addAll(Arrays.asList(statements));
				}
			}
		}

		if (customDialog.isMergeData())
		{
			StringBuilder columnNamesSelectStmt = new StringBuilder();
			for (String columnNames : customDialog.getMergeColumns())
			{
				columnNamesSelectStmt.append("\"").append(columnNames).append("\"").append(", ");
			}
			columnNamesSelectStmt.delete(columnNamesSelectStmt.length() - 2, columnNamesSelectStmt.length()); // deletes
			// the
			// ", "

			StringBuilder columnIDSelect = new StringBuilder();
			for (String[] whereRow : customDialog.getWhereDataColumns())
			{
				columnIDSelect.append("\"").append(whereRow[1]).append("\"").append(", ");
			}
			columnIDSelect.delete(columnIDSelect.length() - 2, columnIDSelect.length()); // deletes the ", "

			final String[] mergeColumns =
				customDialog.getMergeColumns().toArray(new String[customDialog.getMergeColumns().size()]);

			// Selects the values from the merge table
			String dataQuery =
				"SELECT " + columnNamesSelectStmt.toString() + ", " + columnIDSelect.toString() + " FROM \""
					+ schema + "\".\"" + mergedTable + "\"";
			Statement stmt = null;
			ResultSet rs = null;
			try
			{
				stmt = _session.getSQLConnection().createStatement();
				if (s_log.isDebugEnabled()) {
					s_log.debug("generateSQLStatements - running dataQuery: "+dataQuery);
				}
				rs = stmt.executeQuery(dataQuery);

				ArrayList<String> whereColumns = new ArrayList<String>();
				ArrayList<String> whereValues = new ArrayList<String>();

				while (rs.next())
				{
					ArrayList<String> rowColumns = new ArrayList<String>();
					ArrayList<String> rowData = new ArrayList<String>();
					for (int i = 1; i <= customDialog.getMergeColumns().size(); i++)
					{
						String value = rs.getString(i);
						if (!rs.wasNull())
						{
							rowColumns.add(mergeColumns[i - 1]);
							rowData.add("'" + value + "'");
						}
					}

					if (customDialog.getWhereDataColumns().size() > 0)
					{
						// Selects the values for the where part
						whereColumns = new ArrayList<String>();
						whereValues = new ArrayList<String>();
						int count = 0;
						for (String[] whereRow : customDialog.getWhereDataColumns())
						{
							count++;
							// maybe with Inner join better performance
							StringBuilder whereColumn = new StringBuilder();
							StringBuilder whereValue = new StringBuilder();
							whereColumn.append(table).append(".").append(whereRow[0]);
							whereValue	.append("'")
											.append(rs.getString(customDialog.getMergeColumns().size() + count))
											.append("'");

							whereColumns.add(whereColumn.toString());
							whereValues.add(whereValue.toString());
						}
					}

					updateResults.add(_dialect.getUpdateSQL(table,
						rowColumns.toArray(new String[rowColumns.size()]),
						rowData.toArray(new String[rowData.size()]),
						new String[] { mergedTable },
						whereColumns.toArray(new String[whereColumns.size()]),
						whereValues.toArray(new String[whereValues.size()]),
						qualifier,
						_sqlPrefs));
					rowData.clear();
				}
			} catch (SQLException e)
			{
				s_log.error("generateSQLStatements: Unexpected exception: " + e.getMessage(), e);
			} finally
			{
				SQLUtilities.closeResultSet(rs);
				SQLUtilities.closeStatement(stmt);
			}
			results.addAll(updateResults);
		}

		return results.toArray(new String[] {});
	}
	
	/**
	 * @see net.sourceforge.squirrel_sql.plugins.refactoring.commands.AbstractRefactoringCommand#executeScript(java.lang.String)
	 */
	@Override
	protected void executeScript(String script)
	{
		CommandExecHandler handler = new CommandExecHandler(_session);

		SQLExecuterTask executer = new SQLExecuterTask(_session, script, handler);
		executer.run(); // Execute the sql synchronously

		_session.getApplication().getThreadPool().addTask(new Runnable()
		{
			public void run()
			{
				GUIUtils.processOnSwingEventThread(new Runnable()
				{
					public void run()
					{
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
	 * @param dialect
	 *           the HibernateDialect to check
	 * @return true if this refactoring is supported; false otherwise.
	 */
	@Override
	protected boolean isRefactoringSupportedForDialect(HibernateDialect dialect)
	{
		boolean result = true;
		// This refactoring depends on the following dialect API methods:
		// getUpdateSQL
		result = result && dialect.supportsUpdate();
		result = result && dialect.supportsAddColumn();

		return result;
	}

	private void showCustomDialog() throws SQLException
	{
		ITableInfo selectedTable = (ITableInfo) _info[0];
		ITableInfo[] tables =
			_session.getSchemaInfo().getITableInfos(selectedTable.getCatalogName(),
				selectedTable.getSchemaName());
		TableColumnInfo[] tableColumnInfos = _session.getMetaData().getColumnInfo(selectedTable);

		_allTables = new HashMap<String, TableColumnInfo[]>();
		for (ITableInfo table : tables)
		{
			if (table.getDatabaseObjectType() == DatabaseObjectType.TABLE && table != selectedTable)
			{
				_allTables.put(table.getSimpleName(), _session.getMetaData().getColumnInfo(table));
			}
		}
		customDialog = dialogFactory.createDialog(selectedTable.getSimpleName(), tableColumnInfos, _allTables);
		customDialog.addExecuteListener(new ExecuteListener());
		customDialog.addEditSQLListener(new EditSQLListener(customDialog));
		customDialog.addShowSQLListener(new ShowSQLListener(i18n.SHOWSQL_DIALOG_TITLE, customDialog));
		customDialog.setLocationRelativeTo(_session.getApplication().getMainFrame());
		customDialog.setVisible(true);
	}

	private void logDialogInfo() {
		if (s_log.isDebugEnabled()) {
			s_log.debug("getReferencedTable: "+customDialog.getReferencedTable());
			s_log.debug("getWhereDataColumns: ");
			for (String[] whereColumns : customDialog.getWhereDataColumns()) {
				s_log.debug("whereDataColumns Array: "+Arrays.toString(whereColumns));
			}
			for (String mergeColumn : customDialog.getMergeColumns()) {
				s_log.debug("mergeColumn: "+mergeColumn);
			}			
		}
	}
}
