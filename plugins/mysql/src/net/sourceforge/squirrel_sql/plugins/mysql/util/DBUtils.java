package net.sourceforge.squirrel_sql.plugins.mysql.util;
/*
 * Copyright (C) 2003 Arun Kapilan.P
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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.fw.sql.SQLConnection;
import net.sourceforge.squirrel_sql.fw.sql.SQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.session.IObjectTreeAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;

import net.sourceforge.squirrel_sql.plugins.mysql.MysqlPlugin;
/*
 * DBUtils.java
 *
 * Created on June 14, 2003, 9:07 AM
 *
 * @author Arun Kapilan.P
 */
public class DBUtils
{
	/** Logger for this class. */
	private final static ILogger s_log =
		LoggerController.createLogger(DBUtils.class);

	/** Current session. */
	private ISession _session;

	/** Current plugin. */
	private final MysqlPlugin _plugin;

	/**
	 * Ctor specifying the current session.
	 */
	public DBUtils(ISession session, MysqlPlugin plugin)
	{
		super();
		_session = session;
		_plugin = plugin;

	}

	//To get the TableInfo for the selected object in the tree
	public ITableInfo getTableInfo()
	{

		SQLConnection conn = _session.getSQLConnection();
		SQLDatabaseMetaData dmd = conn.getSQLMetaData();
		IObjectTreeAPI treeAPI = _session.getObjectTreeAPI(_plugin);
		IDatabaseObjectInfo[] dbInfo = treeAPI.getSelectedDatabaseObjects();

		if (dbInfo[0] instanceof ITableInfo)
		{
			return (ITableInfo)dbInfo[0];
		}
		return null;
	}

	public String[] getColumnNames()
	{
		Vector dadaSet = new Vector();
		String[] columnNames = null;
		ResultSetMetaData rsmd;
		try
		{
			final SQLConnection conn = _session.getSQLConnection();
			SQLDatabaseMetaData dmd = conn.getSQLMetaData();
			Statement stmt = conn.createStatement();
			ResultSet rs =
				stmt.executeQuery("SELECT * FROM " + getTableInfo() + ";");
			ResultSetMetaData md = rs.getMetaData();
			columnNames = new String[md.getColumnCount()];
			for (int i = 0; i < columnNames.length; i++)
			{
				columnNames[i] = md.getColumnLabel(i + 1);
			}
		}
		catch (SQLException ex)
		{
			_session.getMessageHandler().showErrorMessage(ex);
		}
		return columnNames;
	}

	public String[] getFieldDataTypes()
	{
		Vector dataSet = new Vector();
		String[] dataTypes = null;

		try
		{
			final SQLConnection conn = _session.getSQLConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs =
				stmt.executeQuery("SELECT * FROM " + getTableInfo() + ";");
			ResultSetMetaData md = rs.getMetaData();

			dataTypes = new String[md.getColumnCount()];
			for (int i = 0; i < dataTypes.length; i++)
			{
				dataTypes[i] = md.getColumnTypeName(i + 1);
			}
		}
		catch (SQLException ex)
		{
			_session.getMessageHandler().showErrorMessage(ex);
		}
		return dataTypes;
	}

	public void execute(String SQLQuery)
	{
		try
		{

			SQLConnection conn = _session.getSQLConnection();
			Statement stmt = conn.createStatement();
			stmt.execute(SQLQuery);
		}
		catch (SQLException ex)
		{
			_session.getMessageHandler().showErrorMessage(ex);
		}
	}

	//Get all the data types available
	public Vector getDataTypes()
	{

		Vector dataTypes = new Vector();
		try
		{
			final SQLConnection conn = _session.getSQLConnection();
			SQLDatabaseMetaData dmd = conn.getSQLMetaData();
			ResultSet rs = dmd.getTypeInfo();
			ResultSetMetaData md = rs.getMetaData();
			while (rs.next())
			{
				dataTypes.add(rs.getString(1));
			}
		}
		catch (SQLException ex)
		{
			_session.getMessageHandler().showErrorMessage(ex);
		}
		return dataTypes;
	}

	public String getPrimaryKeyColumn()
	{

		String primaryKey = "";
		try
		{
			SQLConnection con = _session.getSQLConnection();
			SQLDatabaseMetaData db = con.getSQLMetaData();
			ResultSet keys = db.getPrimaryKeys(getTableInfo());
			while (keys.next())
			{
				primaryKey = keys.getString(4);
			}
		}
		catch (SQLException ex)
		{
			_session.getMessageHandler().showErrorMessage(ex);
		}

		return primaryKey;
	}
}
