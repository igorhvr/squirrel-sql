package net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree;
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
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import net.sourceforge.squirrel_sql.fw.id.IIdentifier;
import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.SQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.DatabaseObjectInfoTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.IObjectTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.database.CatalogsTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.database.ConnectionStatusTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.database.DataTypesTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.database.KeywordsTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.database.MetaDataTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.database.NumericFunctionsTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.database.SchemasTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.database.StringFunctionsTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.database.SystemFunctionsTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.database.TableTypesTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.database.TimeDateFunctionsTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.procedure.ProcedureColumnsTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table.ColumnPriviligesTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table.ColumnsTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table.ContentsTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table.ExportedKeysTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table.ImportedKeysTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table.IndexesTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table.PrimaryKeyTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table.RowCountTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table.RowIDTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table.TablePriviligesTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table.VersionColumnsTab;
import net.sourceforge.squirrel_sql.client.session.properties.SessionProperties;
/**
 * This is the panel for the Object Tree tab.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class ObjectTreePanel extends JPanel
{
	/** Logger for this class. */
	private static final ILogger s_log =
		LoggerController.createLogger(ObjectTreePanel.class);

	/** Current session. */
	private ISession _session;

	/** Tree of objects within the database. */
	private ObjectTree _tree;

	/** Split pane between the object tree and the data panel. */
	private final JSplitPane _splitPane =
		new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

	/**
	 * Empty data panel. Used if the object selected in the object
	 * tree doesn't require a panel.
	 */
	private final ObjectTreeTabbedPane _emptyTabPane;

	/**
	 * Contains instances of <TT>ObjectTreeTabbedPane</TT> objects keyed by
	 * the node type. I.E. the tabbed folders for each node type are kept here.
	 */
	private final Map _tabbedPanes = new HashMap();

	/** Listens to changes in session properties. */
	private SessionPropertiesListener _propsListener;

	/** Listens to changes in each of the tabbed folders. */
	private TabbedPaneListener _tabPnlListener;

	/**
	 * Collection of <TT>IObjectPanelTab</TT> objects to be displayed for all
	 * nodes in the object tree.
	 */
//	private List _tabsForAllNodes = new ArrayList();

	/**
	 * ctor specifying the current session.
	 *
	 * @param	session	Current session.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>ISession</TT> passed.
	 */
	public ObjectTreePanel(ISession session)
	{
		super();
		if (session == null)
		{
			throw new IllegalArgumentException("ISession == null");
		}
		_session = session;

		_emptyTabPane = new ObjectTreeTabbedPane(_session);

		CreateGUI();

		// Register tabs to display in the details panel for database nodes.
		addDetailTab(DatabaseObjectType.SESSION, new MetaDataTab());
		addDetailTab(DatabaseObjectType.SESSION, new ConnectionStatusTab());

		final SQLDatabaseMetaData md = session.getSQLConnection().getSQLMetaData();

		try
		{
			if (md.supportsCatalogs())
			{
				addDetailTab(DatabaseObjectType.SESSION, new CatalogsTab());
			}
		}
		catch (Throwable th)
		{
			s_log.error("Error in supportsCatalogs()", th);
		}

		try
		{
			if (md.supportsSchemas())
			{
				addDetailTab(DatabaseObjectType.SESSION, new SchemasTab());
			}
		}
		catch (Throwable th)
		{
			s_log.error("Error in supportsCatalogs()", th);
		}

		addDetailTab(DatabaseObjectType.SESSION, new TableTypesTab());
		addDetailTab(DatabaseObjectType.SESSION, new DataTypesTab());
		addDetailTab(DatabaseObjectType.SESSION, new NumericFunctionsTab());
		addDetailTab(DatabaseObjectType.SESSION, new StringFunctionsTab());
		addDetailTab(DatabaseObjectType.SESSION, new SystemFunctionsTab());
		addDetailTab(DatabaseObjectType.SESSION, new TimeDateFunctionsTab());
		addDetailTab(DatabaseObjectType.SESSION, new KeywordsTab());

		// Register tabs to display in the details panel for catalog nodes.
		addDetailTab(DatabaseObjectType.CATALOG, new DatabaseObjectInfoTab());

		// Register tabs to display in the details panel for schema nodes.
		addDetailTab(DatabaseObjectType.SCHEMA, new DatabaseObjectInfoTab());

		// Register tabs to display in the details panel for table nodes.
		addDetailTab(DatabaseObjectType.TABLE, new DatabaseObjectInfoTab());
		addDetailTab(DatabaseObjectType.TABLE, new ContentsTab());
		addDetailTab(DatabaseObjectType.TABLE, new RowCountTab());
		addDetailTab(DatabaseObjectType.TABLE, new ColumnsTab());
		addDetailTab(DatabaseObjectType.TABLE, new PrimaryKeyTab());
		addDetailTab(DatabaseObjectType.TABLE, new ExportedKeysTab());
		addDetailTab(DatabaseObjectType.TABLE, new ImportedKeysTab());
		addDetailTab(DatabaseObjectType.TABLE, new IndexesTab());
		addDetailTab(DatabaseObjectType.TABLE, new TablePriviligesTab());
		addDetailTab(DatabaseObjectType.TABLE, new ColumnPriviligesTab());
		addDetailTab(DatabaseObjectType.TABLE, new RowIDTab());
		addDetailTab(DatabaseObjectType.TABLE, new VersionColumnsTab());

		// Register tabs to display in the details panel for procedure nodes.
		addDetailTab(DatabaseObjectType.PROCEDURE, new DatabaseObjectInfoTab());
		addDetailTab(DatabaseObjectType.PROCEDURE, new ProcedureColumnsTab());

		// Register tabs to display in the details panel for UDT nodes.
		addDetailTab(DatabaseObjectType.UDT, new DatabaseObjectInfoTab());
	}

	public void addNotify()
	{
		super.addNotify();
		_tabPnlListener = new TabbedPaneListener();
		_propsListener = new SessionPropertiesListener();
		_session.getProperties().addPropertyChangeListener(_propsListener);

		Iterator it = _tabbedPanes.values().iterator();
		while (it.hasNext())
		{
			setupTabbedPane((ObjectTreeTabbedPane)it.next());
		}
	}

	public void removeNotify()
	{
		super.removeNotify();

		if (_propsListener != null)
		{
			_session.getProperties().removePropertyChangeListener(_propsListener);
			_propsListener = null;
		}

		Iterator it = _tabbedPanes.values().iterator();
		while (it.hasNext())
		{
			ObjectTreeTabbedPane pane = (ObjectTreeTabbedPane)it.next();
			pane.getTabbedPane().removeChangeListener(_tabPnlListener);
		}
		_tabPnlListener = null;
	}

	/**
	 * Add an expander for the specified database object type.
	 *
	 * @param	dboType		Database object type.
	 * @param	expander	Expander called to add children to a parent node.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if a <TT>null</TT> <TT>DatabaseObjectType</TT>
	 * 			or <TT>INodeExpander</TT> passed.
	 */
	public void addExpander(DatabaseObjectType dboType, INodeExpander expander)
	{
		if (dboType == null)
		{
			throw new IllegalArgumentException("Null DatabaseObjectType passed");
		}
		if (expander == null)
		{
			throw new IllegalArgumentException("Null INodeExpander passed");
		}
		_tree.getTypedModel().addExpander(dboType, expander);
	}

	/**
	 * Add a tab to be displayed in the detail panel for the passed
	 * database object type type.
	 *
	 * @param	dboType		Database Object type.
	 * @param	tab			Tab to be displayed.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown when a <TT>null</TT> <TT>DatabaseObjectType</TT> or
	 * 			<TT>IObjectPanelTab</TT> passed.
	 */
	public void addDetailTab(DatabaseObjectType dboType, IObjectTab tab)
	{
		if (dboType == null)
		{
			throw new IllegalArgumentException("Null DatabaseObjectType passed");
		}
		if (tab == null)
		{
			throw new IllegalArgumentException("IObjectPanelTab == null");
		}

		getOrCreateObjectPanelTabbedPane(dboType).addObjectPanelTab(tab);
	}

	/**
	 * Add a listener to the object tree for structure changes. I.E nodes
	 * added/removed.
	 *
	 * @param	lis		The <TT>TreeModelListener</TT> you want added.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>TreeModelListener</TT> passed.
	 */
	public void addTreeModelListener(TreeModelListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("TreeModelListener == null");
		}
		_tree.getModel().addTreeModelListener(lis);
	}

	/**
	 * Remove a structure changes listener from the object tree.
	 *
	 * @param	lis		The <TT>TreeModelListener</TT> you want removed.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>TreeModelListener</TT> passed.
	 */
	public void removeTreeModelListener(TreeModelListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("TreeModelListener == null");
		}
		_tree.getModel().removeTreeModelListener(lis);
	}

	/**
	 * Add a listener to the object tree for selection changes.
	 *
	 * @param	lis		The <TT>TreeSelectionListener</TT> you want added.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>TreeSelectionListener</TT> passed.
	 */
	public void addTreeSelectionListener(TreeSelectionListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("TreeSelectionListener == null");
		}
		_tree.addTreeSelectionListener(lis);
	}

	/**
	 * Remove a listener from the object tree for selection changes.
	 *
	 * @param	lis		The <TT>TreeSelectionListener</TT> you want removed.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>TreeSelectionListener</TT> passed.
	 */
	public void removeTreeSelectionListener(TreeSelectionListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("TreeSelectionListener == null");
		}
		_tree.removeTreeSelectionListener(lis);
	}

	/**
	 * Add a listener to the object tree.
	 *
	 * @param	lis		The <TT>ObjectTreeListener</TT> you want added.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>ObjectTreeListener</TT> passed.
	 */
	public void addObjectTreeListener(IObjectTreeListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("IObjectTreeListener == null");
		}
		_tree.addObjectTreeListener(lis);
	}

	/**
	 * Remove a listener from the object tree.
	 *
	 * @param	lis		The <TT>ObjectTreeListener</TT> you want removed.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>ObjectTreeListener</TT> passed.
	 */
	public void removeObjectTreeListener(IObjectTreeListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("IObjectTreeListener == null");
		}
		_tree.removeObjectTreeListener(lis);
	}

	/**
	 * Add an item to the popup menu for the specified database object type
	 * in the object tree.
	 *
	 * @param	dboType		Database Object type.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if a <TT>null</passed> <TT>DatabaseObjectType</TT>
	 * 			or <TT>Action</TT> passed.
	 */
	public void addToObjectTreePopup(DatabaseObjectType dboType, Action action)
	{
		if (dboType == null)
		{
			throw new IllegalArgumentException("Null DatabaseObjectType passed");
		}
		if (action == null)
		{
			throw new IllegalArgumentException("Null Action passed");
		}
		_tree.addToPopup(dboType, action);
	}

	/**
	 * Add an item to the popup menu for all node types in the object
	 * tree.
	 *
	 * @param	action		Action to add to menu.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if a <TT>null</TT> <TT>Action</TT> passed.
	 */
	public void addToObjectTreePopup(Action action)
	{
		if (action == null)
		{
			throw new IllegalArgumentException("Null Action passed");
		}
		_tree.addToPopup(action);
	}

	/**
	 * Add an hierarchical menu to the popup menu for the specified database
	 * object type.
	 *
	 * @param	dboType		Database object type.
	 * @param	menu		<TT>JMenu</TT> to add to menu.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if a <TT>null</TT> <TT>DatabaseObjectType</TT> or
	 * 			<TT>JMenu</TT> thrown.
	 */
	public void addToObjectTreePopup(DatabaseObjectType dboType, JMenu menu)
	{
		if (dboType == null)
		{
			throw new IllegalArgumentException("DatabaseObjectType == null");
		}
		if (menu == null)
		{
			throw new IllegalArgumentException("JMenu == null");
		}
		_tree.addToPopup(dboType, menu);
	}

	/**
	 * Add an hierarchical menu to the popup menu for all node types.
	 *
	 * @param	menu	<TT>JMenu</TT> to add to menu.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if a <TT>null</TT> <TT>JMenu</TT> thrown.
	 */
	public void addToObjectTreePopup(JMenu menu)
	{
		if (menu == null)
		{
			throw new IllegalArgumentException("JMenu == null");
		}
		_tree.addToPopup(menu);
	}

	/**
	 * Return an array of the currently selected nodes.
	 *
	 * @return	array of <TT>ObjectTreeNode</TT> objects.
	 */
	public ObjectTreeNode[] getSelectedNodes()
	{
		return _tree.getSelectedNodes();
	}

	/**
	 * Return an array of the currently selected database
	 * objects. This is guaranteed to be non-null.
	 *
	 * @return	array of <TT>ObjectTreeNode</TT> objects.
	 */
	public IDatabaseObjectInfo[] getSelectedDatabaseObjects()
	{
		return _tree.getSelectedDatabaseObjects();
	}

	/**
	 * Retrieve details about all object types that can be in this
	 * tree.
	 *
	 * @return	DatabaseObjectType[]	Array of object type info objects.
	 */
	public DatabaseObjectType[] getDatabaseObjectTypes()
	{
		return _tree.getTypedModel().getDatabaseObjectTypes();
	}

	/**
	 * Refresh object tree.
	 */
	public void refreshTree()
	{
		_tree.refresh();
	}

	/**
	 * Refresh the nodes currently selected in the object tree.
	 */
	public void refreshSelectedNodes()
	{
		_tree.refreshSelectedNodes();
	}

	/**
	 * Remove one or more nodes from the tree.
	 *
	 * @param	nodes	Array of nodes to be removed.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if a <TT>null</TT> <TT>ObjectTreeNode[]</TT> thrown.
	 */
	public void removeNodes(ObjectTreeNode[] nodes)
	{
		if (nodes == null)
		{
			throw new IllegalArgumentException("ObjectTreeNode[] == null");
		}
		ObjectTreeModel model = _tree.getTypedModel();
		for (int i = 0; i < nodes.length; ++i)
		{
			model.removeNodeFromParent(nodes[i]);
		}
	}

	public IObjectTab getTabbedPaneIfSelected(DatabaseObjectType dbObjectType, String title)
	{
		return getTabbedPane(dbObjectType).getTabIfSelected(title);
	}

	/**
	 * Add a known database object type to the object tree.
	 *
	 * @param	dboType		The new database object type.
	 */
	public void addKnownDatabaseObjectType(DatabaseObjectType dboType)
	{
		_tree.getTypedModel().addKnownDatabaseObjectType(dboType);
	}

	/**
	 * Set the panel to be shown in the data area for the passed
	 * path.
	 *
	 * @param	path	path of node currently selected.
	 */
	private void setSelectedObjectPanel(TreePath path)
	{
		ObjectTreeTabbedPane tabPane = null;
		if (path != null)
		{
			Object lastComp = path.getLastPathComponent();
			if (lastComp instanceof ObjectTreeNode)
			{
				ObjectTreeNode node = (ObjectTreeNode)lastComp;
				tabPane = getDetailPanel(node);
				tabPane.setDatabaseObjectInfo(node.getDatabaseObjectInfo());
				tabPane.selectCurrentTab();
			}
		}
		setSelectedObjectPanel(tabPane);
	}

	/**
	 * Set the panel in the data area to that passed.
	 *
	 * @param	comp	Component to be displayed. If <TT>null</TT> use an empty
	 * 					panel.
	 */
	private void setSelectedObjectPanel(ObjectTreeTabbedPane pane)
	{
		JTabbedPane comp = null;
		if (pane != null)
		{
			comp = pane.getTabbedPane();
		}
		if (comp == null)
		{
			comp = _emptyTabPane.getTabbedPane();
		}

		int divLoc = _splitPane.getDividerLocation();
		Component existing = _splitPane.getRightComponent();
		if (existing != null)
		{
			_splitPane.remove(existing);
		}
		_splitPane.add(comp, JSplitPane.RIGHT);
		_splitPane.setDividerLocation(divLoc);

		if (pane != null)
		{
			pane.selectCurrentTab();
		}
	}

	/**
	 * Get the detail panel to be displayed for the passed node.
	 *
	 * @param	node	Node to get details panel for.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if a <TT>null</TT> <TT>ObjectTreeNode</TT> passed.
	 */
	private ObjectTreeTabbedPane getDetailPanel(ObjectTreeNode node)
	{
		if (node == null)
		{
			throw new IllegalArgumentException("ObjectTreeNode == null");
		}

		ObjectTreeTabbedPane tabPane = getTabbedPane(node.getDatabaseObjectType());
		if (tabPane != null)
		{
			return tabPane;
		}

		return _emptyTabPane;
	}

	/**
	 * Return the tabbed pane for the passed object tree node type.
	 *
	 * @param	dboType		The database object type we are getting a tabbed
	 *						pane for.
	 *
	 * @return		the <TT>ObjectTreeTabbedPane</TT> for the passed database object
	 *				type.
	 */
	private ObjectTreeTabbedPane getTabbedPane(DatabaseObjectType dboType)
	{
		return (ObjectTreeTabbedPane)_tabbedPanes.get(dboType.getIdentifier());
	}

	/**
	 * Return the tabbed pane for the passed database object type. If one
	 * doesn't exist then create it.
	 *
	 * @param	dboType		The database object type we are getting a tabbed
	 *						pane for.
	 *
	 * @return	the <TT>List</TT> containing all the <TT>IObjectPanelTab</TT>
	 * 			instances for the passed object tree node type.
	 */
	private ObjectTreeTabbedPane getOrCreateObjectPanelTabbedPane(DatabaseObjectType dboType)
	{
		if (dboType == null)
		{
			throw new IllegalArgumentException("Null DatabaseObjectType passed");
		}

		final IIdentifier key = dboType.getIdentifier();
		ObjectTreeTabbedPane tabPane = (ObjectTreeTabbedPane)_tabbedPanes.get(key);
		if (tabPane == null)
		{
			tabPane = new ObjectTreeTabbedPane(_session);
			setupTabbedPane(tabPane);
			_tabbedPanes.put(key, tabPane);
		}
		return tabPane;
	}

	/**
	 * Create the user interface.
	 */
	private void CreateGUI()
	{
		setLayout(new BorderLayout());

		_tree = new ObjectTree(_session);

		_splitPane.setOneTouchExpandable(true);
		_splitPane.setContinuousLayout(true);
//		final JScrollPane sp = new JScrollPane();
//		sp.setBorder(BorderFactory.createEmptyBorder());
//		sp.setViewportView(_tree);
//		sp.setPreferredSize(new Dimension(200, 200));

		_splitPane.add(new LeftPanel(), JSplitPane.LEFT);
		add(_splitPane, BorderLayout.CENTER);
		_splitPane.setDividerLocation(200);

		_tree.addTreeSelectionListener(new ObjectTreeSelectionListener());

		_tree.setSelectionRow(0);
	}

	private synchronized void propertiesHaveChanged(String propName)
	{
		if (propName == null
			|| propName.equals(SessionProperties.IPropertyNames.META_DATA_OUTPUT_CLASS_NAME)
			|| propName.equals(SessionProperties.IPropertyNames.TABLE_CONTENTS_OUTPUT_CLASS_NAME)
			|| propName.equals(SessionProperties.IPropertyNames.SQL_RESULTS_OUTPUT_CLASS_NAME)
			|| propName.equals(SessionProperties.IPropertyNames.OBJECT_TAB_PLACEMENT))
		{
			final SessionProperties props = _session.getProperties();

			Iterator it = _tabbedPanes.values().iterator();
			while (it.hasNext())
			{
				ObjectTreeTabbedPane pane = (ObjectTreeTabbedPane)it.next();

				if (propName == null
					|| propName.equals(SessionProperties.IPropertyNames.META_DATA_OUTPUT_CLASS_NAME)
					|| propName.equals(SessionProperties.IPropertyNames.TABLE_CONTENTS_OUTPUT_CLASS_NAME)
					|| propName.equals(SessionProperties.IPropertyNames.SQL_RESULTS_OUTPUT_CLASS_NAME))
				{
					pane.rebuild();
				}
				if (propName == null
					|| propName.equals(SessionProperties.IPropertyNames.OBJECT_TAB_PLACEMENT))
				{
					pane.getTabbedPane().setTabPlacement(props.getObjectTabPlacement());
				}
			}
		}
	}

	private void setupTabbedPane(ObjectTreeTabbedPane pane)
	{
		final SessionProperties props = _session.getProperties();
		pane.rebuild();
		final JTabbedPane p = pane.getTabbedPane();
		p.setTabPlacement(props.getObjectTabPlacement());
		p.addChangeListener(_tabPnlListener);
	}

	private final class LeftPanel extends JPanel
	{
		LeftPanel()
		{
			super(new BorderLayout());
//			add(new TreeHeaderPanel(), BorderLayout.NORTH);
			final JScrollPane sp = new JScrollPane();
			sp.setBorder(BorderFactory.createEmptyBorder());
			sp.setViewportView(_tree);
			sp.setPreferredSize(new Dimension(200, 200));
			add(sp, BorderLayout.CENTER);
		}
	}

//	private final class TreeHeaderPanel extends JPanel
//	{
//		JPopupMenu _pop = new JPopupMenu("abc");
//		TreeHeaderPanel()
//		{
//			super(new FlowLayout());
//			JLabel lbl = new JLabel("DB Explorer");
//			add(lbl);
//
//			_pop.add("Filter...");
//			add(_pop);
//		}
//	}

	/**
	 * This class listens for changes in the node selected in the tree
	 * and displays the appropriate detail panel for the node.
	 */
	private final class ObjectTreeSelectionListener
		implements TreeSelectionListener
	{
		public void valueChanged(TreeSelectionEvent evt)
		{
			setSelectedObjectPanel(evt.getNewLeadSelectionPath());
		}
	}

	/**
	 * Listen for changes in session properties.
	 */
	private class SessionPropertiesListener implements PropertyChangeListener
	{
		public void propertyChange(PropertyChangeEvent evt)
		{
			propertiesHaveChanged(evt.getPropertyName());
		}
	}

	/**
	 * When a different tab is selected in one of the tabbed panels then
	 * refresh the newly selected tab.
	 */
	private class TabbedPaneListener implements ChangeListener
	{
		public void stateChanged(ChangeEvent evt)
		{
			final Object src = evt.getSource();
			if (!(src instanceof JTabbedPane))
			{
				StringBuffer buf = new StringBuffer();
				buf.append("Source object in TabbedPaneListener was not a JTabbedpane")
					.append(" - it was ")
					.append(src == null ? "null" : src.getClass().getName());
				s_log.error(buf.toString());
				return;
			}
			JTabbedPane tabPane = (JTabbedPane)src;

			Object prop = tabPane.getClientProperty(ObjectTreeTabbedPane.IClientPropertiesKeys.TABBED_PANE_OBJ);
			if (!(prop instanceof ObjectTreeTabbedPane))
			{
				StringBuffer buf = new StringBuffer();
				buf.append("Client property in JTabbedPane was not an ObjectTreeTabbedPane")
					.append(" - it was ")
					.append(prop == null ? "null" : prop.getClass().getName());
				s_log.error(buf.toString());
				return;
			}

			((ObjectTreeTabbedPane)prop).selectCurrentTab();
		}
	}

}
