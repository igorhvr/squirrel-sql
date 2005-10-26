package net.sourceforge.squirrel_sql.plugins.favs;
/*
 * Copyright (C) 2001 Colin Bell
 * colbell@users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;

import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;

import net.sourceforge.squirrel_sql.client.IApplication;

final class QueryTreeModel extends DefaultTreeModel {

	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(QueryTreeModel.class);


	/** Logger for this class. */
	private static ILogger s_log = LoggerController.createLogger(QueryTreeModel.class);

	private IApplication _app;
	private FoldersCache _cache;

	private JPanel _emptyPnl = new JPanel();

	private MyModelListener _modelListener = new MyModelListener();

	/**
	 * This interface defines locale specific strings. This should be
	 * replaced with a property file.
	 */
	private static class i18n {

		// i18n[favs.database=Database]
		String DATABASE = s_stringMgr.getString("favs.database");
		// i18n[favs.noCatalog=No Catalog]
		String NO_CATALOG = s_stringMgr.getString("favs.noCatalog");   // ?? Replace with md.getCatalogueTerm.
		// i18n[favs.mayNotReturn=May return a result]
		String MAY_RETURN = s_stringMgr.getString("favs.mayNotReturn");
		// i18n[favs.doesNotReturn=Does not return a result]
		String DOESNT_RETURN = s_stringMgr.getString("favs.doesNotReturn");
		// i18n[favs.returns=Returns a result]
		String DOES_RETURN = s_stringMgr.getString("favs.returns");
		// i18n[favs.unknown=Unknown]
		String UNKNOWN = s_stringMgr.getString("favs.unknown");
	}

	QueryTreeModel(IApplication app, FoldersCache cache) throws IllegalArgumentException {
		super(new FolderNode(new Folder(null, "Root")));
		if (app == null) {
			throw new IllegalArgumentException("Null IApplication passed");
		}
		if (cache == null) {
			throw new IllegalArgumentException("Null FoldersCache passed");
		}
		addTreeModelListener(_modelListener);
		_app = app;
		_cache = cache;
		loadTree();
	}

	private void loadTree() {
		_modelListener.stopListening();
		try {
			Folder rootFolder = _cache.getRootFolder();
			if (rootFolder != null) {
				FolderNode rootNode = new FolderNode(rootFolder);
				setRoot(rootNode);
				loadSubFolders(rootNode);
			} else {
				rootFolder = new Folder(null, "Root");
				setRoot(new FolderNode(rootFolder));
				_cache.setRootFolder(rootFolder);
			}
			reload();
		} finally {
			_modelListener.startListening();
		}
	}

	private void loadSubFolders(FolderNode node) {
		Folder[] subFolders = node.getFolder().getSubFolders();
		for (int i = 0; i < subFolders.length; ++i) {
			FolderNode childNode = new FolderNode(subFolders[i]);
			node.add(childNode);
			loadSubFolders(childNode);
		}
	}

	private static class MyModelListener implements TreeModelListener {
		private boolean _listening = true;

		public void treeStructureChanged(TreeModelEvent evt) {
			s_log.debug("treeStructureChanged");
		}

		public void treeNodesInserted(TreeModelEvent evt) {
			s_log.debug("treeNodesInserted");
		}

		public void treeNodesChanged(TreeModelEvent evt) {
			s_log.debug("treeNodesChanged");
			Object objs[] = evt.getChildren();
			if (objs != null) {
				for (int i = 0; i < objs.length; ++i) {
					FolderNode node = (FolderNode)objs[i];
					Folder folder = node.getFolder();
					String newName = node.toString();
					String oldName = folder.getName();
					s_log.debug(oldName);
					if (!newName.equals(oldName)) {
						try {
							folder.setName(newName);
						} catch (Exception ex) {
							s_log.error("Error", ex); // ??
						}
					}
				}
			}
		}

		public void treeNodesRemoved(TreeModelEvent evt) {
			s_log.debug("treeNodesRemoved");
		}

		void startListening() {
			_listening = true;
		}

		void stopListening() {
			_listening = false;
		}
	}
}
