package net.sourceforge.squirrel_sql.plugins.oracle.expander;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.ISQLConnection;
import net.sourceforge.squirrel_sql.fw.sql.SQLDatabaseMetaData;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.INodeExpander;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.ObjectTreeNode;
/**
 * This class handles the expanding of an Oracle specific object type node.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class ObjectTypeExpander implements INodeExpander
{
	/** SQL that retrieves the objects for the object types. */
	private static String SQL =
		"select object_name from sys.all_objects where object_type = ?" +
		" and owner = ? and object_name like ? order by object_name";

	/** Type of the objects to be displayed in the child nodes. */
	private ObjectType _objectType;

	/**
	 * Ctor.
	 *
	 * @param	objectType	Object type to be displayed.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> objectType passed.
	 */
	ObjectTypeExpander(ObjectType objectType)
	{
		super();
		if (objectType == null)
		{
			throw new IllegalArgumentException("ObjectType == null");
		}
		_objectType = objectType;
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
		final String catalogName = parentDbinfo.getCatalogName();
		final String schemaName = parentDbinfo.getSchemaName();
		childNodes.addAll(createNodes(session, catalogName, schemaName));
		return childNodes;
	}

	private List<ObjectTreeNode> createNodes(ISession session, String catalogName,
											String schemaName)
		throws SQLException
	{
		final ISQLConnection conn = session.getSQLConnection();
		final SQLDatabaseMetaData md = conn.getSQLMetaData();
		final List<ObjectTreeNode> childNodes = new ArrayList<ObjectTreeNode>();
		String objFilter =  session.getProperties().getObjectFilter();

		// Add node for each object.
		PreparedStatement pstmt = conn.prepareStatement(SQL);
		try
		{	
			pstmt.setString(1, _objectType._objectTypeColumnData);
			pstmt.setString(2, schemaName);
			pstmt.setString(3, objFilter != null && objFilter.length() > 0 ? objFilter :"%");
			ResultSet rs = pstmt.executeQuery();
			try
			{
				while (rs.next())
				{
					IDatabaseObjectInfo dbinfo = new DatabaseObjectInfo(catalogName,
											schemaName, rs.getString(1),
											_objectType._childDboType, md);
					childNodes.add(new ObjectTreeNode(session, dbinfo));
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
