package net.sourceforge.squirrel_sql.plugins.db2.exp;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.INodeExpander;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.ObjectTreeNode;
import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.ISQLConnection;
import net.sourceforge.squirrel_sql.fw.sql.SQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.sql.SQLUtilities;
/**
 * This class handles the expanding of the "Sequence Group"
 * node. It will give a list of all the Sequences available in the schema.
 *
 * @author manningr
 */
public class UDFParentExpander implements INodeExpander
{
	/** SQL used to load UDF names  */
	private static final String SQL =
	    "SELECT name " +
	    "FROM SYSIBM.SYSFUNCTIONS " +
	    "WHERE schema = ? " +
	    "AND name like ? " +
	    "AND implementation is null";
	
	/** SQL used to load UDF names on OS/400 systems */
	private static final String OS_400_SQL = 
	    "select routine_name " +
	    "from QSYS2.SYSFUNCS " +
	    "where routine_schema = ? " +
	    "and routine_name like ? ";	    
	
	/** whether or not we are connected to OS/400 */
	private boolean isOS400 = false;
	
	/**
	 * Default ctor.
	 */
	public UDFParentExpander(boolean isOS400)
	{
		super();
		this.isOS400 = isOS400;
	}

	/**
	 * Create the child nodes for the passed parent node and return them. Note
	 * that this method should <B>not</B> actually add the child nodes to the
	 * parent node as this is taken care of in the caller.
	 *
	 * @param	session	Current session.
	 * @param	node	Node to be expanded.
	 *
	 * @return	A list of <TT>ObjectTreeNode</TT> objects representing the child
	 *			nodes for the passed node.
	 */
	public List<ObjectTreeNode> createChildren(ISession session, ObjectTreeNode parentNode)
		throws SQLException
	{
		final List<ObjectTreeNode> childNodes = new ArrayList<ObjectTreeNode>();
		final IDatabaseObjectInfo parentDbinfo = parentNode.getDatabaseObjectInfo();
		final ISQLConnection conn = session.getSQLConnection();
		final SQLDatabaseMetaData md = session.getSQLConnection().getSQLMetaData();
		final String catalogName = parentDbinfo.getCatalogName();
		final String schemaName = parentDbinfo.getSchemaName();
		final String objFilter =  session.getProperties().getObjectFilter();


		String sql = SQL;
		if (isOS400) {
		    sql = OS_400_SQL;
		}
		final PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = null;
		try
		{
			pstmt.setString(1, schemaName);
			pstmt.setString(2, objFilter != null && objFilter.length() > 0 ? objFilter :"%"); 
			rs = pstmt.executeQuery();
				while (rs.next())
				{
					IDatabaseObjectInfo si = new DatabaseObjectInfo(catalogName,
												schemaName, rs.getString(1),
												DatabaseObjectType.UDF, md);
					childNodes.add(new ObjectTreeNode(session, si));
				}
		}
		finally
		{
		    SQLUtilities.closeResultSet(rs);
            SQLUtilities.closeStatement(pstmt);
		}
		return childNodes;
	}
}
