package net.sourceforge.squirrel_sql.client.session.mainpanel;
/*
 * Copyright (C) 2001-2003 Johan Companger
 * jcompagner@j-com.nl
 *
 * Modification copyright (C) 2001-2003 Colin Bell
 * colbell@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terdims of the GNU Lesser General Public
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.squirrel_sql.fw.datasetviewer.DataSetException;
import net.sourceforge.squirrel_sql.fw.datasetviewer.ResultSetDataSet;
import net.sourceforge.squirrel_sql.fw.datasetviewer.ResultSetMetaDataDataSet;
import net.sourceforge.squirrel_sql.fw.sql.QueryTokenizer;
import net.sourceforge.squirrel_sql.fw.sql.SQLConnection;
import net.sourceforge.squirrel_sql.fw.util.Utilities;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.properties.SessionProperties;

public class SQLExecuterTask implements Runnable
{
	/** Logger for this class. */
	private static final ILogger s_log =
		LoggerController.createLogger(SQLExecuterTask.class);

	/**
	 * The <TT>SQLPanel</TT> that requested the execution. TODO: at some stage
	 * we need to abstract out the callbacks to this panel and have them handled
	 * as events which can be listened for. Also need to remove all UI code
	 * from this class.
	 */
	private SQLPanel _sqlPanel;

	/** Current session. */
	private ISession _session;

	/** SQL passed in to be executed. */
	private String _sql;
	private CancelPanel _cancelPanel = new CancelPanel();
	private Statement _stmt;
	private boolean _stopExecution = false;

	private int _currentQueryIndex = 0;

	private boolean _cancelPanelRemoved = false;

	/**
	 * Collection of <TT>ResultSetDataSet</TT> objects produced by SQL
	 * execution.
	 */
	private final List _results = Collections.synchronizedList(new ArrayList());

	public SQLExecuterTask(SQLPanel sqlPanel, ISession session, String sql)
	{
		super();
		_sqlPanel = sqlPanel;
		_session = session;
		_sql = sql;
	}

	public void run()
	{
		_sqlPanel.setCancelPanel(_cancelPanel);
		_cancelPanelRemoved = false;
		try
		{
			final SQLConnection conn = _session.getSQLConnection();
			final SessionProperties props = _session.getProperties();
			_stmt = conn.createStatement();
			try
			{
				final boolean correctlySupportsMaxRows = conn.getSQLMetaData().correctlySupportsSetMaxRows();
				if (correctlySupportsMaxRows && props.getSQLLimitRows())
				{
					try
					{
						_stmt.setMaxRows(props.getSQLNbrRowsToShow());
					}
					catch (Exception e)
					{
						s_log.error("Can't Set MaxRows", e);
					}
				}

				// Retrieve all the statements to execute.
				final QueryTokenizer qt = new QueryTokenizer(_sql,
										props.getSQLStatementSeparator(),
										props.getStartOfLineComment());
				List queryStrings = new ArrayList();
				while (qt.hasQuery())
				{
					queryStrings.add(qt.nextQuery());
				}

				// Allow plugins to modify the requested SQL prior to execution.
				queryStrings = _sqlPanel.fireAllSQLToBeExecutedEvent(queryStrings);


				_cancelPanel.setQueryCount(queryStrings.size());
				_currentQueryIndex = 0;

				// Process each individual query.
				boolean maxRowsHasBeenSet = correctlySupportsMaxRows;
				while (!queryStrings.isEmpty())
				{
					if (_cancelPanelRemoved)
					{
						_sqlPanel.setCancelPanel(_cancelPanel);
						_cancelPanelRemoved = false;
					}

					String querySql = (String)queryStrings.remove(0);
					if (querySql != null)
					{
						querySql = _sqlPanel.fireSQLToBeExecutedEvent(querySql);
						if (querySql != null)
						{
							// Some driver don't correctly support setMaxRows. In
							// these cases use setMaxRows only if this is a
							// SELECT.
							if (!correctlySupportsMaxRows && props.getSQLLimitRows())
							{
								if ("SELECT".length() < querySql.trim().length()
										&& "SELECT".equalsIgnoreCase(querySql.trim().substring(0, "SELECT".length())))
								{
									if (!maxRowsHasBeenSet)
									{
										try
										{
											_stmt.setMaxRows(props.getSQLNbrRowsToShow());
										}
										catch (Exception e)
										{
											s_log.error("Can't Set MaxRows", e);
										}
										maxRowsHasBeenSet = true;
									}
								}
								else if (maxRowsHasBeenSet)
								{
									_stmt.close();
									_stmt = conn.createStatement();
									maxRowsHasBeenSet = false;
								}
							}
							try
							{
								if (!processQuery(querySql))
								{
									break;
								}
							}
							catch (SQLException ex)
							{
								if (props.getAbortOnError())
								{
									throw ex;
								}
								displayError(ex);
							}
							catch (DataSetException ex)
							{
								if (props.getAbortOnError())
								{
									throw ex;
								}
								displayError(ex);
							}
						}
					}
				}

				if (_stopExecution || !_cancelPanelRemoved)
				{
					_sqlPanel.removeCancelPanel(_cancelPanel);
				}
			}
			finally
			{
				try
				{
					_stmt.close();
				}
				finally
				{
					_stmt = null;
				}
			}
		}
		catch (Throwable ex)
		{
			displayError(ex);
		}
		finally
		{
			if (_stopExecution || !_cancelPanelRemoved)
			{
				_sqlPanel.removeCancelPanel(_cancelPanel);
			}
			if (_stopExecution)
			{
				_session.getMessageHandler().showMessage("Query execution cancelled by user.");
			}
		}
	}

	private boolean processQuery(String querySql)
		throws SQLException, DataSetException
	{
		++_currentQueryIndex;

		_cancelPanel.setSQL(Utilities.cleanString(querySql));
		_cancelPanel.setStatusLabel("Executing SQL...");

		final SQLExecutionInfo exInfo = new SQLExecutionInfo(_currentQueryIndex, querySql);
		boolean rc = _stmt.execute(querySql);
		exInfo.sqlExecutionComplete();

		// If SQL executing produced warnings then write them out to the session
		// message handler. TODO: This is a pain. PostgreSQL sends "raise
		// notice" messages to the connection, not to the statment so they will
		// be mixed up with warnings from other statements.
		final SQLConnection conn = _session.getSQLConnection();
		synchronized (conn)
		{
			try
			{
				processWarnings(conn.getWarnings());
				conn.getConnection().clearWarnings();
			}
			catch (Throwable th)
			{
				s_log.debug("Driver doesn't handle Connection.getWarnings()/clearWarnings()", th);
			}
		}
		try
		{
			processWarnings(_stmt.getWarnings());
		}
		catch (Throwable th)
		{
			s_log.debug("Driver doesn't handle Statement.getWarnings()/clearWarnings()", th);
		}

		if (rc)
		{
			if (_stopExecution)
			{
				return false;
			}

			_session.getSQLPanelAPI(_session.getApplication().getDummyAppPlugin()).addSQLToHistory(querySql);
			ResultSet rs = _stmt.getResultSet();
			if (rs != null)
			{
				while (rs != null)
				{
					_cancelPanel.setStatusLabel("Building output...");
					ResultSetDataSet rsds = new ResultSetDataSet();
					_results.add(rsds);
					SessionProperties props = _session.getProperties();
					rsds.setResultSet(rs, props.getLargeResultSetObjectInfo());
					if (_stopExecution)
					{
						return false;
					}

					ResultSetMetaDataDataSet rsmdds = null;
					try
					{
						rsmdds = new ResultSetMetaDataDataSet(rs);
					}
					catch (DataSetException ex)
					{
						s_log.error("Cant retrieve metadata for ResultSet", ex);
						_session.getMessageHandler().showMessage(ex);
					}

					_sqlPanel.addResultsTab(exInfo, rsds, rsmdds, _cancelPanel);
					_cancelPanelRemoved = true;
					try
					{
						_session.getMessageHandler().showMessage(rs.getWarnings());
					}
					catch (Throwable th)
					{
						s_log.error("Can't get warnings ", th);
						_session.getMessageHandler().showMessage(th);
					}

					if (_stmt.getMoreResults())
					{
						rs = _stmt.getResultSet();
					}
					else
					{
						try
						{
							rs.close();
						}
						catch (Throwable th)
						{
							s_log.error("Error closing ResultSet", th);
						}
						finally
						{
							rs = null;
						}
					}
				}
			}
		}
		else
		{
			//_sqlPanel.addSQLToHistory(querySql);
			_session.getSQLPanelAPI(_session.getApplication().getDummyAppPlugin()).addSQLToHistory(querySql);
			_session.getMessageHandler().showMessage(
				_stmt.getUpdateCount() + " Rows Updated");
		}

		// i18n
		final NumberFormat nbrFmt = NumberFormat.getNumberInstance();
		double executionLength = exInfo.getSQLExecutionElapsedMillis() / 1000.0;
		double outputLength = exInfo.getResultsProcessingElapsedMillis() / 1000.0;
		StringBuffer buf = new StringBuffer();
		buf.append("Query ").append(nbrFmt.format(_currentQueryIndex))
			.append(" elapsed time (seconds) - Total: ")
			.append(nbrFmt.format(executionLength + outputLength))
			.append(", SQL query: ")
			.append(nbrFmt.format(executionLength))
			.append(", Building output: ")
			.append(nbrFmt.format(outputLength));
		_session.getMessageHandler().showMessage(buf.toString());
		return true;
	}

	private void processWarnings(SQLWarning sw)
	{
		try
		{
			while (sw != null)
			{
				_session.getMessageHandler().showMessage(sw);
				sw = sw.getNextWarning();
			}
		}
		catch (Throwable th)
		{
		}
	}

	private void displayError(Throwable th)
	{
		if (th instanceof SQLException)
		{
			_session.getMessageHandler().showErrorMessage("Error: " + th);
		}
		else
		{
			s_log.error("Error occured executing SQL", th);
			_session.getMessageHandler().showErrorMessage("Error: " + th);
		}
	}

	private final class CancelPanel extends JPanel implements ActionListener
	{
		private JLabel _sqlLbl = new JLabel();
		private JLabel _currentStatusLbl = new JLabel();

		/** Total number of queries that will be executed. */
		private int _queryCount;

		/** Number of the query currently being executed (starts from 1). */
		private int _currentQueryIndex = 0;

		private CancelPanel()
		{
			super(new GridBagLayout());

			JButton cancelBtn = new JButton("Cancel");
			cancelBtn.addActionListener(this);

			GridBagConstraints gbc = new GridBagConstraints();

			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new Insets(5, 10, 5, 10);

			gbc.gridx = 0;
			gbc.gridy = 0;
			add(new JLabel("SQL:"), gbc);

			gbc.weightx = 1;
			++gbc.gridx;
			add(_sqlLbl, gbc);

			gbc.weightx = 0;
			gbc.gridx = 0;
			++gbc.gridy;
			add(new JLabel("Status:"), gbc);

			++gbc.gridx;
			add(_currentStatusLbl, gbc);

			gbc.gridx = 0;
			++gbc.gridy;
			gbc.fill = GridBagConstraints.NONE;
			add(cancelBtn, gbc);
		}

		public void setSQL(String sql)
		{
			++_currentQueryIndex;
			StringBuffer buf = new StringBuffer();
			buf.append(String.valueOf(_currentQueryIndex)).append(" of ")
				.append(String.valueOf(_queryCount)).append(" - ").append(sql);
			_sqlLbl.setText(buf.toString());
		}

		public void setStatusLabel(String text)
		{
			_currentStatusLbl.setText(text);
		}

		public void setQueryCount(int value)
		{
			_queryCount = value;
			_currentQueryIndex = 0;
		}

		public void actionPerformed(ActionEvent event)
		{
			_stopExecution = true;
			try
			{
				if (_stmt != null)
				{
					_stmt.cancel();
				}
			}
			catch (Throwable th)
			{
				s_log.error("Error occured cancelling SQL", th);
			}

			synchronized (_results)
			{
				for (Iterator it = _results.iterator(); it.hasNext();)
				{
					((ResultSetDataSet)it.next()).cancelProcessing();
				}
			}
		}
	}
}
