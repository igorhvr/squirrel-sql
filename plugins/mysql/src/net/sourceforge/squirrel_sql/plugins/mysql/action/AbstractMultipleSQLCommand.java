package net.sourceforge.squirrel_sql.plugins.mysql.action;
/*
 * Copyright (C) 2003 Colin Bell
 * colbell@users.sourceforge.net
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
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.util.BaseException;
import net.sourceforge.squirrel_sql.fw.util.ICommand;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.session.IObjectTreeAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;

import net.sourceforge.squirrel_sql.plugins.mysql.MysqlPlugin;
/**
 * This abstract command is a MySQL command that takes a table
 * as a parameter.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
abstract class AbstractMultipleSQLCommand implements ICommand
{
	/** Logger for this class. */
	private final static ILogger s_log =
		LoggerController.createLogger(AbstractMultipleSQLCommand.class);

	/** Current session. */
	private ISession _session;

	/** Current plugin. */
	private final MysqlPlugin _plugin;

	/**
	 * Ctor specifying the current session.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if a�<TT>null</TT> <TT>ISession</TT>,
	 * 			<TT>Resources</TT> or <TT>MysqlPlugin</TT> passed.
	 */
	public AbstractMultipleSQLCommand(ISession session, MysqlPlugin plugin)
	{
		super();
		if (session == null)
		{
			throw new IllegalArgumentException("ISession == null");
		}
		if (plugin == null)
		{
			throw new IllegalArgumentException("MysqlPlugin == null");
		}

		_session = session;
		_plugin = plugin;
	}

	/**
	 * Execute this command.
	 */
	public void execute() throws BaseException
	{
		final StringBuffer buf = new StringBuffer(2048);
		final String sep = " " + _session.getProperties().getSQLStatementSeparator();

		final IObjectTreeAPI api = _session.getObjectTreeAPI(_plugin);
		final IDatabaseObjectInfo[] dbObjs = api.getSelectedDatabaseObjects();

		for (int i = 0; i < dbObjs.length; ++i)
		{
			final String cmd = getMySQLCommand(dbObjs[i]);
			if (cmd != null && cmd.length() > 0)
			{
				buf.append(cmd).append(sep).append('\n');
			}
		}

		// Execute the SQL command in the SQL tab and then display the SQL tab.
		if (buf.length() > 0)
		{
			_session.getSQLPanelAPI(_plugin).appendSQLScript(buf.toString(), true);
			_session.getSQLPanelAPI(_plugin).executeCurrentSQL();
			_session.selectMainTab(ISession.IMainPanelTabIndexes.SQL_TAB);
		}
	}

	/**
	 * Retrieve the MySQL command to run.
	 *
	 * @return	the MySQL command to run.
	 */
	protected abstract String getMySQLCommand(IDatabaseObjectInfo dbObj);
}
