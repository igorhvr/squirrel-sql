package net.sourceforge.squirrel_sql.plugins.mysql.action;
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
import javax.swing.JOptionPane;

import net.sourceforge.squirrel_sql.fw.util.ICommand;

import net.sourceforge.squirrel_sql.plugins.mysql.MysqlPlugin;

import net.sourceforge.squirrel_sql.client.session.IObjectTreeAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
/*
 * CreateDatabaseCommand.java
 *
 * Created on June 9, 2003, 11:16 AM
 *
 * @author Arun Kapilan.P
 */
public class CreateDatabaseCommand implements ICommand
{
	/** Current session. */
	private ISession _session;

	/** Current plugin. */
	private final MysqlPlugin _plugin;

	/**
	 * Ctor specifying the current session.
	 */
	public CreateDatabaseCommand(ISession session, MysqlPlugin plugin)
	{
		super();
		_session = session;
		_plugin = plugin;
	}

	public void execute()
	{
		String dbName = JOptionPane.showInputDialog("Enter database name");
		if (dbName != null)
		{
			final StringBuffer buf = new StringBuffer();
			buf.append("create database ").append(dbName);
			_session.getSessionInternalFrame().getSQLPanelAPI().executeSQL(buf.toString());
			IObjectTreeAPI api = _session.getSessionInternalFrame().getObjectTreeAPI();
			api.refreshTree();
		}

	}
}
