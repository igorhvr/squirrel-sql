package net.sourceforge.squirrel_sql.plugins.oracle.tab;
/*
 * Copyright (C) 2002 Colin Bell
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
import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;

import net.sourceforge.squirrel_sql.client.session.ISession;
/**
 * This class will display the details for an Oracle instance.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class InstanceDetailsTab extends BasePreparedStatementTab
{
	/**
	 * This interface defines locale specific strings. This should be
	 * replaced with a property file.
	 */
	private interface i18n
	{
		String TITLE = "Details";
		String HINT = "Display instance details";
	}

	/** SQL that retrieves the data. */
	private static String SQL =
		"select instance_number, instance_name, host_name, version,"
			+ " startup_time, status, parallel, thread#, archiver, log_switch_wait,"
			+ " logins, shutdown_pending, database_status, instance_role"
			+ " from sys.v_$instance"
			+ " where instance_number = ?";

	public InstanceDetailsTab()
	{
		super(i18n.TITLE, i18n.HINT, true);
	}

	/**
	 */
	protected PreparedStatement createStatement() throws SQLException
	{
		ISession session = getSession();
		PreparedStatement pstmt = session.getSQLConnection().prepareStatement(SQL);
		IDatabaseObjectInfo doi = getDatabaseObjectInfo();
		pstmt.setLong(1, Long.parseLong(doi.getSimpleName()));
		return pstmt;
	}
}
