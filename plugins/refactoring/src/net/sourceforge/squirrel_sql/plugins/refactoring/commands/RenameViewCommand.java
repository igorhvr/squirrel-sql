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

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.SQLExecuterTask;
import net.sourceforge.squirrel_sql.fw.dialects.DatabaseObjectQualifier;
import net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect;
import net.sourceforge.squirrel_sql.fw.dialects.UserCancelledOperationException;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.SQLUtilities;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.plugins.refactoring.gui.RenameTableDialog;

public class RenameViewCommand extends AbstractRefactoringCommand
{
	/**
	 * Logger for this class.
	 */
	@SuppressWarnings("unused")
	private final ILogger s_log = LoggerController.createLogger(RenameViewCommand.class);

	/**
	 * Internationalized strings for this class
	 */
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(RenameViewCommand.class);

	static interface i18n
	{
		String SHOWSQL_DIALOG_TITLE = s_stringMgr.getString("RenameViewCommand.sqlDialogTitle");
	}

	protected RenameTableDialog customDialog;

	public RenameViewCommand(ISession session, IDatabaseObjectInfo[] dbInfo)
	{
		super(session, dbInfo);
	}

	/**
	 * @see net.sourceforge.squirrel_sql.plugins.refactoring.commands.AbstractRefactoringCommand#onExecute()
	 */
	@Override
	protected void onExecute()
	{
		showCustomDialog();
	}

	/**
	 * @see net.sourceforge.squirrel_sql.plugins.refactoring.commands.AbstractRefactoringCommand#generateSQLStatements()
	 */
	@Override
	protected String[] generateSQLStatements() throws UserCancelledOperationException
	{
		String[] result = null;
		try
		{
			DatabaseObjectQualifier qualifier =
				new DatabaseObjectQualifier(_info[0].getCatalogName(), _info[0].getSchemaName());
			
			String viewName = _info[0].getSimpleName();
			String newViewName = customDialog.getNewSimpleName();
			if (_dialect.supportsRenameView()) {
				result = 
					_dialect.getRenameViewSQL(viewName, newViewName, qualifier, _sqlPrefs);
			} else {
				String viewDefSql = _dialect.getViewDefinitionSQL(viewName, qualifier, _sqlPrefs);
				String viewDefinition = getViewDef(newViewName, viewDefSql);
				String dropOldViewSql = _dialect.getDropViewSQL(viewName, false, qualifier, _sqlPrefs);
				result = new String[] { viewDefinition, dropOldViewSql };
			}
		} catch (UnsupportedOperationException e2)
		{
			_session.showMessage(s_stringMgr.getString("RenameViewCommand.unsupportedOperationMsg",
				_dialect.getDisplayName()));
		}
		return result;
	}

	private String getViewDef (String newViewName, String viewDefQuery) {
		String result = null;
		ResultSet rs = null;
		Statement stmt = null;
		try {
			stmt = _session.getSQLConnection().createStatement();
			rs = stmt.executeQuery(viewDefQuery);
			if (rs.next()) {
				result = rs.getString(1);
				int asIndex = result.toUpperCase().indexOf("AS");
				if (asIndex != -1) {
					result = "CREATE VIEW " + newViewName + " AS " + result.substring(asIndex + 2);
				}
			}
		} catch (SQLException e) {
			s_log.error("getViewDef: unexpected exception - "+e.getMessage(), e);
		} finally {
			SQLUtilities.closeResultSet(rs);
			SQLUtilities.closeStatement(stmt);
		}
		return result;
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
	protected boolean isRefactoringSupportedForDialect(HibernateDialect dialect)
	{
		DatabaseObjectQualifier qualifier =
			new DatabaseObjectQualifier(_info[0].getCatalogName(), _info[0].getSchemaName());

		return dialect.supportsRenameView()
			|| dialect.getViewDefinitionSQL("test", qualifier, _sqlPrefs) != null;
	}

	private void showCustomDialog()
	{
		_session.getApplication().getThreadPool().addTask(new Runnable()
		{
			public void run()
			{
				GUIUtils.processOnSwingEventThread(new Runnable()
				{
					public void run()
					{
						customDialog = new RenameTableDialog(_info, RenameTableDialog.DIALOG_TYPE_VIEW);
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
}
