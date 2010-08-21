package net.sourceforge.squirrel_sql.plugins.mysql.expander;
/*
 * Copyright (C) 2003 Colin Bell
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
import net.sourceforge.squirrel_sql.fw.sql.ISQLConnection;
import net.sourceforge.squirrel_sql.fw.sql.SQLDatabaseMetaData;
//import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
//import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.INodeExpander;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.ObjectTreeNode;

import net.sourceforge.squirrel_sql.plugins.mysql.MysqlPlugin;
/**
 * This class handles the expanding of the "user type"
 * node. It will give a list of all the users.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class UserParentExpander implements INodeExpander
{
	/** SQL used to load info. */
	private static final String SQL = "select concat(user, '@', host) from mysql.user";

	/** Logger for this class. */
//	private static final ILogger s_log =
//		LoggerController.createLogger(UserParentExpander.class);

	/** The plugin. */
	@SuppressWarnings("unused")
    private final MysqlPlugin _plugin;

	/**
	 * Ctor.
	 *
	 * @param	plugin	The controlling plugin.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>MysqlPlugin</TT> passed.
	 */
	public UserParentExpander(MysqlPlugin plugin)
	{
		super();
		if (plugin == null)
		{
			throw new IllegalArgumentException("MysqlPlugin == null");
		}

		_plugin = plugin;
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
		final ISQLConnection conn = session.getSQLConnection();
		final SQLDatabaseMetaData md = session.getSQLConnection().getSQLMetaData();
		final IDatabaseObjectInfo parentDbinfo = parentNode.getDatabaseObjectInfo();
		final String schemaName = parentDbinfo.getSchemaName();

		PreparedStatement pstmt = conn.prepareStatement(SQL);
		try
		{
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{
				IDatabaseObjectInfo doi = new DatabaseObjectInfo(null, schemaName,
											rs.getString(1), DatabaseObjectType.USER, md);
				childNodes.add(new ObjectTreeNode(session, doi));
			}
		}
		finally
		{
			pstmt.close();
		}
		return childNodes;
	}
}
