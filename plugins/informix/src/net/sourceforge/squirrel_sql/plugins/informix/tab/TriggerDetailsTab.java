package net.sourceforge.squirrel_sql.plugins.informix.tab;
/*
 * Copyright (C) 2006 Rob Manning
 * manningr@users.sourceforge.net
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

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.BasePreparedStatementTab;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
/**
 * This class will display the details for an Informix trigger.
 *
 * @author manningr
 */
public class TriggerDetailsTab extends BasePreparedStatementTab
{
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(TriggerDetailsTab.class);


	/**
	 * This interface defines locale specific strings. This should be
	 * replaced with a property file.
	 */
	private interface i18n
	{
		// i18n[TriggerDetailsTab.title=Details]
		String TITLE = s_stringMgr.getString("TriggerDetailsTab.title");
		// i18n[TriggerDetailsTab.hint=Display trigger details]
		String HINT = s_stringMgr.getString("TriggerDetailsTab.hint");
	}

	/** SQL that retrieves the data. */
	private static String SQL =
        "SELECT  T1.owner     AS trigger_owner, " +
        "       T1.trigname  AS trigger_name, " +
        "       case T1.event  " +
        "         when 'I' then 'INSERT' " +
        "         when 'U' then 'UPDATE' " +
        "         when 'D' then 'DELETE' " +
        "         when 'S' then 'SELECT' " +
        "         else T1.event " +
        "       end AS triggering_event, " +
        "       T2.owner     AS table_owner, " +
        "       T2.tabname   AS table_name, " +
        "       case T2.tabtype " +
        "         when 'T' then 'TABLE' " +
        "         when 'V' then 'VIEW' " +
        "         else T2.tabtype " +
        "       end AS table_type, " +
        "       T1.old       AS reference_before, " +
        "       T1.new       AS reference_after " +
        "FROM    systriggers  AS T1, " +
        "       systables    AS T2 " +
        "WHERE   T2.tabid     = T1.tabid " +
        "and T1.trigname = ? ";
    
	/** Logger for this class. */
	private final static ILogger s_log =
		LoggerController.createLogger(TriggerDetailsTab.class);

	public TriggerDetailsTab()
	{
		super(i18n.TITLE, i18n.HINT, true);
	}

	protected PreparedStatement createStatement() throws SQLException
	{
		ISession session = getSession();
        IDatabaseObjectInfo doi = getDatabaseObjectInfo();
        if (s_log.isDebugEnabled()) {
            s_log.debug("Trigger details SQL: "+SQL);
            s_log.debug("Trigger name: "+doi.getSimpleName());
        }
		PreparedStatement pstmt = session.getSQLConnection().prepareStatement(SQL);
		pstmt.setString(1, doi.getSimpleName());
		return pstmt;
	}
}
