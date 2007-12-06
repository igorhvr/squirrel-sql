package net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table;
/*
 * Copyright (C) 2001-2003 Colin Bell
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.sourceforge.squirrel_sql.fw.datasetviewer.DataSetException;
import net.sourceforge.squirrel_sql.fw.datasetviewer.IDataSet;
import net.sourceforge.squirrel_sql.fw.datasetviewer.ResultSetDataSet;
import net.sourceforge.squirrel_sql.fw.sql.ISQLConnection;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
/**
 * This tab shows the number of rows in the table.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class RowCountTab extends BaseTableTab
{
	
    /** Internationalized strings for this class. */
    private static final StringManager s_stringMgr =
        StringManagerFactory.getStringManager(RowCountTab.class);
    
	/**
	 * Return the title for the tab.
	 *
	 * @return	The title for the tab.
	 */
	public String getTitle()
	{
		//i18n[RowCountTab.title=Row Count]
		return s_stringMgr.getString("RowCountTab.title");
	}

	/**
	 * Return the hint for the tab.
	 *
	 * @return	The hint for the tab.
	 */
	public String getHint()
	{
		//i18n[RowCountTab.hint=Number of rows in the selected table] 
		return s_stringMgr.getString("RowCountTab.hint");
	}

	/**
	 * Create the <TT>IDataSet</TT> to be displayed in this tab.
	 */
	protected IDataSet createDataSet() throws DataSetException
	{
		final ISQLConnection conn = getSession().getSQLConnection();
		try
		{
			final Statement stmt = conn.createStatement();
			try
			{
				final ResultSet rs = stmt.executeQuery("select count(*) from "
												+ getTableInfo().getQualifiedName());
				try
				{
					final ResultSetDataSet rsds = new ResultSetDataSet();
					rsds.setResultSet(rs, getDialectType());
					return rsds;
				}
				finally
				{
					rs.close();
				}
			}
			finally
			{
				stmt.close();
			}
		}
		catch (SQLException ex)
		{
			throw new DataSetException(ex);
		}
	}
}
