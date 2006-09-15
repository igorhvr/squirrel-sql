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
import java.sql.SQLException;

import net.sourceforge.squirrel_sql.fw.datasetviewer.DataSetException;
import net.sourceforge.squirrel_sql.fw.datasetviewer.IDataSet;
import net.sourceforge.squirrel_sql.fw.datasetviewer.JavabeanArrayDataSet;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.fw.sql.SQLConnection;
import net.sourceforge.squirrel_sql.fw.sql.SQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.sql.dbobj.BestRowIdentifier;
import net.sourceforge.squirrel_sql.fw.sql.dbobj.adapter.AdapterFactory;
/**
 * This tab shows the primary key info for the currently selected table.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class RowIDTab extends BaseTableTab
{
	/**
	 * This interface defines locale specific strings. This should be
	 * replaced with a property file.
	 */
	private interface i18n
	{
		String TITLE = "Row IDs";
		String HINT = "Show columns that uniquely identify a row";
	}

	/**
	 * Return the title for the tab.
	 *
	 * @return	The title for the tab.
	 */
	public String getTitle()
	{
		return i18n.TITLE;
	}

	/**
	 * Return the hint for the tab.
	 *
	 * @return	The hint for the tab.
	 */
	public String getHint()
	{
		return i18n.HINT;
	}

	/**
	 * Create the <TT>IDataSet</TT> to be displayed in this tab.
	 */
	protected IDataSet createDataSet() throws DataSetException
	{
		try
		{
			final SQLConnection conn = getSession().getSQLConnection();
			final SQLDatabaseMetaData md = conn.getSQLMetaData();
			final ITableInfo ti = getTableInfo();
			final BestRowIdentifier[] bris = md.getBestRowIdentifier(ti);
			return new JavabeanArrayDataSet(AdapterFactory.getInstance().createBestRowIdentifierAdapter(bris));
		}
		catch (SQLException ex)
		{
			throw new DataSetException(ex);
		}

//		try
//		{
//			final ResultSet rs = conn.getSQLMetaData().getBestRowIdentifier(getTableInfo());
//			try
//			{
//				final ResultSetDataSet rsds = new ResultSetDataSet();
//				rsds.setResultSet(rs);
//				return rsds;
//			}
//			finally
//			{
//				rs.close();
//			}
//		}
//		catch (SQLException ex)
//		{
//			throw new DataSetException(ex);
//		}
	}
}
