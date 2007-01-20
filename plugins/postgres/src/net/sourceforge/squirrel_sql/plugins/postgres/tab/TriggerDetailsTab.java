package net.sourceforge.squirrel_sql.plugins.postgres.tab;
/*
 * Copyright (C) 2007 Rob Manning
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
 * This class will display the details for an PostgreSQL trigger.
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
        "select condition_timing AS trigger_time, " +
        "       v.manip AS triggering_event, " +
        "       action_orientation AS granularity, " +
        "       event_object_table AS table_name " +
        "FROM information_schema.triggers t, " +
        "(select  trigger_schema, " +
        "        trigger_name, " +
        "        rtrim( " +
        "            max(case when pos=0 then manip else '' end)|| " +
        "            max(case when pos=1 then manip else '' end)|| " +
        "            max(case when pos=2 then manip else '' end), ' or ' " +
        "            ) as manip " +
        "from ( " +
        "    select a.trigger_schema, " +
        "           a.trigger_name, " +
        "           a.event_manipulation||' or ' as manip, " +
        "           d.cnt, " +
        "           a.rnk as pos " +
        "    from (  select trigger_name, " +
        "                   trigger_schema, " +
        "                   event_manipulation, " +
        "                   (select count(distinct is1.event_manipulation) " +
        "                    from information_schema.triggers is1 " +
        "                    where is2.event_manipulation < is1.event_manipulation) as rnk " +
        "            from information_schema.triggers is2 " +
        "          ) a, " +
        "         (select trigger_schema, trigger_name, count(event_manipulation) as cnt " +
        "          from ( " +
        "                select trigger_schema, " +
        "                       trigger_name, " +
        "                       event_manipulation, " +
        "                       (select count(distinct is3.event_manipulation) " +
        "                        from information_schema.triggers is3 " +
        "                        where is4.event_manipulation < is3.event_manipulation) as rnk " +
        "                from information_schema.triggers is4 " +
        "                ) y " +
        "          group by trigger_schema, trigger_name) d " +
        "    where d.trigger_name = a.trigger_name " +
        "    and d.trigger_schema = a.trigger_schema " +
        ") x " +
        "group by trigger_schema, trigger_name " +
        "order by 1) v " +
        "where t.trigger_schema = v.trigger_schema " +
        "and t.trigger_name = v.trigger_name " +
        "and t.trigger_schema = ? " +
        "and t.trigger_name = ? ";
    
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
            s_log.debug("Trigger schema: "+doi.getSchemaName());
            s_log.debug("Trigger name: "+doi.getSimpleName());
        }
		PreparedStatement pstmt = session.getSQLConnection().prepareStatement(SQL);
        pstmt.setString(1, doi.getSchemaName());
		pstmt.setString(2, doi.getSimpleName());
		return pstmt;
	}
}
