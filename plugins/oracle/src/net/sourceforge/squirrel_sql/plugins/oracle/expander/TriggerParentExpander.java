package net.sourceforge.squirrel_sql.plugins.oracle.expander;
/*
 * Copyright (C) 2002-2003 Colin Bell
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.SQLConnection;
import net.sourceforge.squirrel_sql.fw.sql.SQLDatabaseMetaData;

import net.sourceforge.squirrel_sql.client.session.IObjectTreeAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.INodeExpander;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.ObjectTreeNode;

import net.sourceforge.squirrel_sql.plugins.oracle.IObjectTypes;
/**
 * This class handles the expanding of the "Trigger Parent"
 * node. It will give a list of all the triggers available for the table.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class TriggerParentExpander implements INodeExpander
{
	private static String SQL =
		"select owner, trigger_name from sys.all_triggers where table_owner = ?" +
		" and table_name = ?";

	/**
	 * Default ctor.
	 */
	public TriggerParentExpander()
	{
		super();
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
	public List createChildren(ISession session, ObjectTreeNode parentNode)
		throws SQLException
	{
		final List childNodes = new ArrayList();
		final IDatabaseObjectInfo parentDbinfo = parentNode.getDatabaseObjectInfo();
		final SQLConnection conn = session.getSQLConnection();
		final SQLDatabaseMetaData md = session.getSQLConnection().getSQLMetaData();
		final IDatabaseObjectInfo tableInfo = ((TriggerParentInfo)parentDbinfo).getTableInfo();
		final PreparedStatement pstmt = conn.prepareStatement(SQL);
		try
		{
			pstmt.setString(1, tableInfo.getSchemaName());
			pstmt.setString(2, tableInfo.getSimpleName());
			ResultSet rs = pstmt.executeQuery();
			try
			{
				while (rs.next())
				{
					DatabaseObjectInfo doi = new DatabaseObjectInfo(null,
												rs.getString(1), rs.getString(2),
												DatabaseObjectType.TRIGGER, md);
					childNodes.add(new ObjectTreeNode(session, doi));
				}
			}
			finally
			{
				rs.close();
			}
		}
		finally
		{
			pstmt.close();
		}


		return childNodes;
	}
}
