package net.sourceforge.squirrel_sql.plugins.mssql.action;

/*
 * Copyright (C) 2004 Ryan Walberg <generalpf@yahoo.com>
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

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.fw.util.ICommand;

import net.sourceforge.squirrel_sql.plugins.mssql.MssqlPlugin;

public class IndexDefragCommand implements ICommand {
	private ISession _session;
	private final MssqlPlugin _plugin;

	private final ITableInfo _tableInfo;
    private final String _indexName;

	public IndexDefragCommand(ISession session, MssqlPlugin plugin, ITableInfo tableInfo, String indexName) {
		super();
		if (session == null)
			throw new IllegalArgumentException("ISession == null");
		if (tableInfo == null)
			throw new IllegalArgumentException("ITableInfo == null");

		_session = session;
		_plugin = plugin;
		_tableInfo = tableInfo;
        _indexName = indexName;
	}

	public void execute() {
        StringBuffer sqlBuffer = new StringBuffer();
        final String sqlSep = _session.getProperties().getSQLStatementSeparator();
        sqlBuffer.append("DBCC INDEXDEFRAG(" + _tableInfo.getCatalogName() + "," + _tableInfo.getSimpleName() + "," + _indexName + ")");
        sqlBuffer.append(" " + sqlSep + " \n");
        
        _session.getSQLPanelAPI(_plugin).appendSQLScript(sqlBuffer.toString(), true);
		_session.getSQLPanelAPI(_plugin).executeCurrentSQL();
		_session.selectMainTab(ISession.IMainPanelTabIndexes.SQL_TAB);
	}
}
