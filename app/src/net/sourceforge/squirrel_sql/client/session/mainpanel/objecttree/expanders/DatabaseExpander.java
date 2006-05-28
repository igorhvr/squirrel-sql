package net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.expanders;
/*
 * Copyright (C) 2002-2003 Colin Bell and Johan Compagner
 * colbell@users.sourceforge.net
 * jcompagner@j-com.nl
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
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.SQLConnection;
import net.sourceforge.squirrel_sql.fw.sql.SQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.session.IObjectTreeAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.INodeExpander;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.ObjectTreeNode;
/**
 * This class handles the expanding of database, catalog and schema nodes
 * in the object tree.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class DatabaseExpander implements INodeExpander
{
	/** Logger for this class. */
	private static ILogger s_log =
		LoggerController.createLogger(DatabaseExpander.class);

	/** Array of the different types of tables in this database. */
	private String[] _tableTypes = new String[] {};

	/**
	 * Ctor.
	 *
	 * @param	session	Current session.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>ISession</TT> passed.
	 */
	public DatabaseExpander(ISession session)
	{
		super();
		if (session == null)
		{
			throw new IllegalArgumentException("ISession == null");
		}
        GetTableTypes task = new GetTableTypes(session);
		if (SwingUtilities.isEventDispatchThread()) {
		    session.getApplication().getThreadPool().addTask(task);
        } else {
            task.run();
        }
    }

    private class GetTableTypes implements Runnable {
        
        ISession _session = null;
        
        public GetTableTypes(ISession session) {
            _session = session;
        }
        
        public void run() {
            try
            {
                _tableTypes = _session.getSQLConnection().getSQLMetaData().getTableTypes();
            }
            catch (SQLException ex)
            {
                s_log.debug("DBMS doesn't support 'getTableTypes()", ex);
            }
        }
        
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
		final IDatabaseObjectInfo parentDbinfo = parentNode.getDatabaseObjectInfo();
		final SQLConnection conn = session.getSQLConnection();
		final SQLDatabaseMetaData md = conn.getSQLMetaData();

		boolean supportsCatalogs = false;
		try
		{
			supportsCatalogs = md.supportsCatalogs();
		}
		catch (SQLException ex)
		{
			s_log.debug("DBMS doesn't support 'supportsCatalogs()", ex);
		}

		boolean supportsSchemas = false;
		try
		{
			supportsSchemas = md.supportsSchemas();
		}
		catch (SQLException ex)
		{
			s_log.debug("DBMS doesn't support 'supportsSchemas()", ex);
		}

		List childNodes = new ArrayList();

		if (parentDbinfo.getDatabaseObjectType() == DatabaseObjectType.SESSION)
		{
			// If a driver says it supports schemas/catalogs but doesn't
			// provide schema/catalog nodes, try to get other nodes.
			List addedChildren = new ArrayList();
			if (supportsCatalogs)
			{
				addedChildren = createCatalogNodes(session, md);
				childNodes.addAll(addedChildren);
			}
//			else if (supportsSchemas)
			if (addedChildren.size() == 0 && supportsSchemas)
			{
				addedChildren = createSchemaNodes(session, md, null);
				childNodes.addAll(addedChildren);
			}
//			else
			if (addedChildren.size() == 0)
			{
				childNodes.addAll(createObjectTypeNodes(session, null, null));
			}
		}
		else if (parentDbinfo.getDatabaseObjectType() == DatabaseObjectType.CATALOG)
		{
			// If a driver says it supports schemas but doesn't
			// provide schema nodes, try to get other nodes.
			final String catalogName = parentDbinfo.getSimpleName();
			List addedChildren = new ArrayList();
			if (supportsSchemas)
			{
				addedChildren = createSchemaNodes(session, md, catalogName);
				childNodes.addAll(addedChildren);
			}
			//else
			if (addedChildren.size() == 0)
			{
				childNodes.addAll(createObjectTypeNodes(session, catalogName, null));
			}
		}
		else if (parentDbinfo.getDatabaseObjectType() == DatabaseObjectType.SCHEMA)
		{
			final String catalogName = parentDbinfo.getCatalogName();
			final String schemaName = parentDbinfo.getSimpleName();
			childNodes.addAll(createObjectTypeNodes(session, catalogName, schemaName));
		}

		return childNodes;
	}

	private List createCatalogNodes(ISession session, SQLDatabaseMetaData md)
		throws SQLException
	{
		final List childNodes = new ArrayList();
		if (session.getProperties().getLoadSchemasCatalogs())
		{
			final String[] catalogs = md.getCatalogs();
			final String[] catalogPrefixArray =
						session.getProperties().getCatalogPrefixArray();
			for (int i = 0; i < catalogs.length; ++i)
			{
				boolean found = (catalogPrefixArray.length > 0) ? false : true;
				for (int j = 0; j < catalogPrefixArray.length; ++j)
				{
					if (catalogs[i].startsWith(catalogPrefixArray[j]))
					{
						found = true;
						break;
					}
				}
				if (found)
				{
					IDatabaseObjectInfo dbo = new DatabaseObjectInfo(null, null,
												catalogs[i],
												DatabaseObjectType.CATALOG,
												md);
					childNodes.add(new ObjectTreeNode(session, dbo));
				}
			}
		}
		return childNodes;
	}

	protected List createSchemaNodes(ISession session, 
                                     SQLDatabaseMetaData md,
								     String catalogName)
		throws SQLException
	{
		final List childNodes = new ArrayList();
		if (session.getProperties().getLoadSchemasCatalogs())
		{
         session.getSchemaInfo().waitTillSchemasAndCatalogsLoaded();
         final String[] schemas = session.getSchemaInfo().getSchemas();

         final String[] schemaPrefixArray =
							session.getProperties().getSchemaPrefixArray();
			for (int i = 0; i < schemas.length; ++i)
			{
				boolean found = (schemaPrefixArray.length > 0) ? false : true;
				for (int j = 0; j < schemaPrefixArray.length; ++j)
				{
					if (schemas[i].startsWith(schemaPrefixArray[j]))
					{
						found = true;
						break;
					}
				}
				if (found)
				{
					IDatabaseObjectInfo dbo = new DatabaseObjectInfo(catalogName, null,
													schemas[i],
													DatabaseObjectType.SCHEMA, md);
					childNodes.add(new ObjectTreeNode(session, dbo));
				}
			}
		}
		return childNodes;
	}

	private List createObjectTypeNodes(ISession session, String catalogName,
											String schemaName)
	{
		final List list = new ArrayList();

		if (session.getProperties().getLoadSchemasCatalogs())
		{
			final SQLConnection conn = session.getSQLConnection();
			final SQLDatabaseMetaData md = conn.getSQLMetaData();

			// Add table types to list.
			if (_tableTypes.length > 0)
			{
				for (int i = 0; i < _tableTypes.length; ++i)
				{
					IDatabaseObjectInfo dbo = new DatabaseObjectInfo(catalogName,
													schemaName, _tableTypes[i],
													DatabaseObjectType.TABLE_TYPE_DBO, md);
					ObjectTreeNode child = new ObjectTreeNode(session, dbo);
					list.add(child);
				}
			}
			else
			{
				s_log.debug("List of table types is empty so trying null table type to load all tables");
				IDatabaseObjectInfo dbo = new DatabaseObjectInfo(catalogName,
												schemaName, null,
												DatabaseObjectType.TABLE_TYPE_DBO, md);
				ObjectTreeNode child = new ObjectTreeNode(session, dbo);
				child.setUserObject("TABLE");
				list.add(child);
			}

			// Add stored proc parent node.
			boolean supportsStoredProcs = false;
			try
			{
				supportsStoredProcs = md.supportsStoredProcedures();
			}
			catch (SQLException ex)
			{
				s_log.debug("DBMS doesn't support 'supportsStoredProcedures()'", ex);
			}
			if (supportsStoredProcs)
			{
				IDatabaseObjectInfo dbo = new DatabaseObjectInfo(catalogName,
													schemaName, "PROCEDURE",
													DatabaseObjectType.PROC_TYPE_DBO, md);
				ObjectTreeNode child = new ObjectTreeNode(session, dbo);
				list.add(child);
			}

			// Add UDT parent node.
			{
				IDatabaseObjectInfo dbo = new DatabaseObjectInfo(catalogName,
											schemaName, "UDT",
											DatabaseObjectType.UDT_TYPE_DBO, md);
				ObjectTreeNode child = new ObjectTreeNode(session, dbo);
				list.add(child);
			}
		}

		return list;
	}

}
