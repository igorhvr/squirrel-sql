package net.sourceforge.squirrel_sql.client.session.mainpanel;
/*
 * Copyright (C) 2001-2004 Colin Bell
 * colbell@users.sourceforge.net
 *
 * Modifications Copyright (C) 2001-2004 Johan Compagner
 * jcompagner@j-com.nl
 *
 * Modifications Copyright (C) 2003-2004 Jason Height
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
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.undo.UndoManager;

import net.sourceforge.squirrel_sql.fw.gui.FontInfo;
import net.sourceforge.squirrel_sql.fw.gui.IntegerField;
import net.sourceforge.squirrel_sql.fw.gui.MemoryComboBox;
import net.sourceforge.squirrel_sql.fw.sql.SQLConnection;
import net.sourceforge.squirrel_sql.fw.util.Resources;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.SquirrelAction;
import net.sourceforge.squirrel_sql.client.gui.builders.UIFactory;
import net.sourceforge.squirrel_sql.client.resources.SquirrelResources;
import net.sourceforge.squirrel_sql.client.session.ISQLEntryPanel;
import net.sourceforge.squirrel_sql.client.session.ISQLPanelAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.SQLPanelAPI;
import net.sourceforge.squirrel_sql.client.session.action.RedoAction;
import net.sourceforge.squirrel_sql.client.session.action.UndoAction;
import net.sourceforge.squirrel_sql.client.session.event.IResultTabListener;
import net.sourceforge.squirrel_sql.client.session.event.ISQLExecutionListener;
import net.sourceforge.squirrel_sql.client.session.event.ISQLPanelListener;
import net.sourceforge.squirrel_sql.client.session.event.ISQLResultExecuterTabListener;
import net.sourceforge.squirrel_sql.client.session.event.ResultTabEvent;
import net.sourceforge.squirrel_sql.client.session.event.SQLPanelEvent;
import net.sourceforge.squirrel_sql.client.session.event.SQLResultExecuterTabEvent;
import net.sourceforge.squirrel_sql.client.session.properties.SessionProperties;
/**
 * This is the panel where SQL scripts can be entered and executed.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class SQLPanel extends JPanel
{
	/** Logger for this class. */
	private static final ILogger s_log = LoggerController.createLogger(SQLPanel.class);

	/** Used to separate lines in teh SQL entry area. */
	private final static String LINE_SEPARATOR = "\n";

	/**
	 * Set to <TT>true</TT> once SQL history has been loaded from the file
	 * system.
	 */
	private static boolean s_loadedSQLHistory;

	/** Current session. */
	private ISession _session;

	private SQLHistoryComboBox _sqlCombo;
	private ISQLEntryPanel _sqlEntry;
	private JCheckBox _limitRowsChk = new JCheckBox("Limit rows: ");
	private IntegerField _nbrRows = new IntegerField();

	private JScrollPane _sqlEntryScroller;

	private SqlComboListener _sqlComboListener = new SqlComboListener();
	private MyPropertiesListener _propsListener;

	/** Each tab is a <TT>ResultTab</TT> showing the results of a query. */
//	private JTabbedPane _tabbedResultsPanel;

	/**
	 * Collection of <TT>ResultTabInfo</TT> objects for all
	 * <TT>ResultTab</TT> objects that have been created. Keyed
	 * by <TT>ResultTab.getIdentifier()</TT>.
	 */
//	private Map _allTabs = new HashMap();

	/**
	 * Pool of <TT>ResultTabInfo</TT> objects available for use.
	 */
//	private List _availableTabs = new ArrayList();

	/**
	 * Pool of <TT>ResultTabInfo</TT> objects currently being used.
	 */
//	private ArrayList _usedTabs = new ArrayList();

	/** Each tab is a <TT>ExecuterTab</TT> showing an installed executer. */
	private JTabbedPane _tabbedExecuterPanel;

	private boolean _hasBeenVisible = false;
	private JSplitPane _splitPane;

	/**
	 * Label added to session sheets statusbar sgowing row/col of the
	 * caret in the sql entryy area.
	 */
	private final RowColumnLabel _rowColLbl = new RowColumnLabel();

	/** Listeners */
	private EventListenerList _listeners = new EventListenerList();

	private UndoManager _undoManager = new SquirrelDefaultUndoManager();

	/** Factory for generating unique IDs for new <TT>ResultTab</TT> objects. */
//	private IntegerIdentifierFactory _idFactory = new IntegerIdentifierFactory();

	/** Listens to caret events in data entry area. */
	private final DataEntryAreaCaretListener _dataEntryCaretListener = new DataEntryAreaCaretListener();

	private final List executors = new ArrayList();

	private SQLResultExecuterPanel _sqlExecPanel;

	private ISQLPanelAPI _panelAPI;

	/**
	 * Ctor.
	 *
	 * @param	session	 Current session.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a <TT>null</TT> <TT>ISession</TT> passed.
	 */
	public SQLPanel(ISession session)
	{
		super();
		setSession(session);
		createGUI();
		propertiesHaveChanged(null);
		_sqlExecPanel = new SQLResultExecuterPanel(session);
		_sqlExecPanel.addSQLExecutionListener(new SQLExecutorHistoryListener());
		addExecutor(_sqlExecPanel);
		_panelAPI = new SQLPanelAPI(this);
	}

	/**
	 * Set the current session.
	 *
	 * @param	session	 Current session.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a <TT>null</TT> <TT>ISession</TT> passed.
	 */
	public synchronized void setSession(ISession session)
	{
		if (session == null)
		{
			throw new IllegalArgumentException("Null ISession passed");
		}
		sessionClosing();
		_session = session;
		_propsListener = new MyPropertiesListener();
		_session.getProperties().addPropertyChangeListener(_propsListener);
	}

	/**
	 * JASON: This method may go eventually if the SQLPanel implements the
	 * ISQLPanelAPI interface.
	 */
	public ISQLPanelAPI getSQLPanelAPI()
	{
		return _panelAPI;
	}

	/** Current session. */
	public ISession getSession()
	{
		return _session;
	}

	public void addExecutor(ISQLResultExecuter exec)
	{
		executors.add(exec);

		_tabbedExecuterPanel.addTab(exec.getTitle(), null, exec.getComponent(), exec.getTitle());
		this.fireExecuterTabAdded(exec);
	}

	public void removeExecutor(ISQLResultExecuter exec)
	{
		executors.remove(exec);
	}

	public SQLResultExecuterPanel getSQLExecPanel()
	{
		return _sqlExecPanel;
	}

	/**
	 * Add a listener listening for SQL Execution.
	 *
	 * @param	lis		Listener to add
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a null <TT>ISQLExecutionListener</TT> passed.
	 */
	public synchronized void addSQLExecutionListener(ISQLExecutionListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("null ISQLExecutionListener passed");
		}
		_listeners.add(ISQLExecutionListener.class, lis);
	}

	/**
	 * Remove an SQL execution listener.
	 *
	 * @param	lis	Listener
	 *
	 * @throws	IllegalArgumentException
	 *			If a null <TT>ISQLExecutionListener</TT> passed.
	 */
	public synchronized void removeSQLExecutionListener(ISQLExecutionListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("null ISQLExecutionListener passed");
		}
		_listeners.remove(ISQLExecutionListener.class, lis);
	}

	/**
	 * Add a listener to this panel.
	 *
	 * @param	lis	 Listener
	 *
	 * @throws	IllegalArgumentException
	 *			If a null <TT>ISQLPanelListener</TT> passed.
	 */
	public synchronized void addSQLPanelListener(ISQLPanelListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("null ISQLPanelListener passed");
		}
		_listeners.add(ISQLPanelListener.class, lis);
	}

	/**
	 * Remove a listener.
	 *
	 * @param	lis	Listener
	 *
	 * @throws	IllegalArgumentException
	 *			If a null <TT>ISQLPanelListener</TT> passed.
	 */
	public synchronized void removeSQLPanelListener(ISQLPanelListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("null ISQLPanelListener passed");
		}
		_listeners.remove(ISQLPanelListener.class, lis);
	}

	/**
	 * Add a listener listening for events on result tabs.
	 *
	 * @param	lis	Listener
	 *
	 * @throws	IllegalArgumentException
	 *			If a null <TT>IResultTabListener</TT> passed.
	 */
//	public synchronized void addResultTabListener(IResultTabListener lis)
//	{
//		if (lis == null)
//		{
//			throw new IllegalArgumentException("null IResultTabListener passed");
//		}
//		_listeners.add(IResultTabListener.class, lis);
//	}

	/**
	 * Remove a listener listening for events on result tabs.
	 *
	 * @param	lis	Listener
	 *
	 * @throws	IllegalArgumentException
	 *			If a null <TT>IResultTabListener</TT> passed.
	 */
//	public synchronized void removeResultTabListener(IResultTabListener lis)
//	{
//		if (lis == null)
//		{
//			throw new IllegalArgumentException("null IResultTabListener passed");
//		}
//		_listeners.remove(IResultTabListener.class, lis);
//	}

	/**
	 * Add a listener for events in this sql panel executer tabs.
	 *
	 * @param	lis	Listener
	 *
	 * @throws	IllegalArgumentException
	 *			If a null <TT>ISQLResultExecuterTabListener</TT> passed.
	 */
	public void addExecuterTabListener(ISQLResultExecuterTabListener lis)
	{
 		if (lis == null)
 		{
 			throw new IllegalArgumentException("ISQLExecutionListener == null");
 		}
 		_listeners.add(ISQLResultExecuterTabListener.class, lis);
	}

	/**
	 * Remove a listener for events in this sql panel executer tabs.
	 *
	 * @param	lis		Listener
	 *
	 * @throws	IllegalArgumentException
	 *			If a null <TT>IResultTabListener</TT> passed.
	 */
//	public synchronized void removeResultTabListener(ISQLResultExecuterTabListener lis)
//	{
//		if (lis == null)
//		{
//			throw new IllegalArgumentException("ISQLResultExecuterTabListener == null");
//		}
//		_listeners.remove(ISQLResultExecuterTabListener.class, lis);
//	}

	public synchronized void removeExecuterTabListener(ISQLResultExecuterTabListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("ISQLResultExecuterTabListener == null");
		}
		_listeners.remove(ISQLResultExecuterTabListener.class, lis);
	}

//	public ISQLEntryPanel getSQLEntryPanel()
//	{
//		return _sqlEntry;
//	}

//	public void executeCurrentSQL()
//	{
//		String sql = getSQLEntryPanel().getSQLToBeExecuted();
//		if (sql != null && sql.length() > 0)
//		{
//			executeSQL(sql);
//		}
//		else
//		{
//			_session.getMessageHandler().showErrorMessage("No SQL selected for execution.");
//		}
//	}

//	public void executeSQL(String sql)
//	{
//		if (sql != null && sql.trim().length() > 0)
//		{
//			SQLExecuterTask task = new SQLExecuterTask(this, _session, sql);
//			_session.getApplication().getThreadPool().addTask(task);
//		}
//	}

	/**
	 * Close all the Results frames.
	 */
//	public synchronized void closeAllSQLResultFrames()
//	{
//		List tabs = (List)_usedTabs.clone();
//		for (Iterator it = tabs.iterator(); it.hasNext();)
//		{
//			ResultTabInfo ti = (ResultTabInfo) it.next();
//			if (ti._resultFrame != null)
//			{
//				ti._resultFrame.dispose();
//				ti._resultFrame = null;
//			}
//		}
//	}

	/**
	 * Close all the Results tabs.
	 */
//	public synchronized void closeAllSQLResultTabs()
//	{
//		List tabs = (List)_usedTabs.clone();
//		for (Iterator it = tabs.iterator(); it.hasNext();)
//		{
//			ResultTabInfo ti = (ResultTabInfo) it.next();
//			if (ti._resultFrame == null)
//			{
//				closeTab(ti._tab);
//			}
//		}
//	}

	public ISQLEntryPanel getSQLEntryPanel()
	{
		return _sqlEntry;
	}

//	void selected()
//	{
//		// Empty body.
//	}

	public void runCurrentExecuter()
	{
		int selectedIndex = _tabbedExecuterPanel.getSelectedIndex();
		ISQLResultExecuter exec = (ISQLResultExecuter)executors.get(selectedIndex);
		exec.execute(_sqlEntry);
	}

	/**
	 * Sesssion is ending.
	 * Remove all listeners that this component has setup. Close all
	 * torn off result tab windows.
	 */
	void sessionClosing()
	{
		if (_propsListener != null)
		{
			_session.getProperties().removePropertyChangeListener(_propsListener);
			_propsListener = null;
		}

//		closeAllSQLResultFrames();
	}

	public void installSQLEntryPanel(ISQLEntryPanel pnl)
	{
		if (pnl == null)
		{
			throw new IllegalArgumentException("Null ISQLEntryPanel passed");
		}

		SQLEntryState state = new SQLEntryState(this, _sqlEntry);
		final int pos = _splitPane.getDividerLocation();
		if (_sqlEntry != null)
		{
			_sqlEntry.removeUndoableEditListener(_undoManager);
			_sqlEntry.removeCaretListener(_dataEntryCaretListener);
			if (_sqlEntryScroller != null)
			{
				_splitPane.remove(_sqlEntryScroller);
				_sqlEntryScroller = null;
			}
			else
			{
				_splitPane.remove(_sqlEntry.getTextComponent());
			}
		}
		if (!pnl.getDoesTextComponentHaveScroller())
		{
			_sqlEntryScroller = new JScrollPane(pnl.getTextComponent());
			_sqlEntryScroller.setBorder(BorderFactory.createEmptyBorder());
			_splitPane.add(_sqlEntryScroller);
		}
		else
		{
			_splitPane.add(pnl.getTextComponent(), JSplitPane.LEFT);
		}
		_splitPane.setDividerLocation(pos);
		state.restoreState(pnl);
		_sqlEntry = pnl;

		_sqlEntry.addCaretListener(_dataEntryCaretListener);

		if (!_sqlEntry.hasOwnUndoableManager())
		{
			IApplication app = _session.getApplication();
			Resources res = app.getResources();
			UndoAction undo = new UndoAction(app, _undoManager);
			RedoAction redo = new RedoAction(app, _undoManager);

			JComponent comp = _sqlEntry.getTextComponent();
			comp.registerKeyboardAction(undo, res.getKeyStroke(undo),
							WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
			comp.registerKeyboardAction(redo, res.getKeyStroke(redo),
							WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
			_sqlEntry.setUndoActions(undo, redo);

			_sqlEntry.addUndoableEditListener(_undoManager);
		}

		fireSQLEntryAreaInstalled();
	}

	/**
	 * Close the passed <TT>ResultTab</TT>. This is done by clearing
	 * all data from the tab, removing it from the tabbed panel
	 * and adding it to the list of available tabs.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a <TT>null</TT> <TT>ResultTab</TT> passed.
	 */
//	public void closeTab(ResultTab tab)
//	{
//		if (tab == null)
//		{
//			throw new IllegalArgumentException("Null ResultTab passed");
//		}
//		s_log.debug("SQLPanel.closeTab(" + tab.getIdentifier().toString() + ")");
//		tab.clear();
//		_tabbedResultsPanel.remove(tab);
//		ResultTabInfo tabInfo = (ResultTabInfo) _allTabs.get(tab.getIdentifier());
//		_availableTabs.add(tabInfo);
//		_usedTabs.remove(tabInfo);
//		tabInfo._resultFrame = null;
//		fireTabRemovedEvent(tab);
//	}

	/**
	 * Create an internal frame for the specified tab and
	 * display the tab in the internal frame after removing
	 * it from the tabbed pane.
	 *
	 * @param	tab	<TT>ResultTab</TT> to be displayed in
	 *				an internal frame.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a <TT>null</TT> <TT>ResultTab</TT> passed.
	 */
//	public void createWindow(ResultTab tab)
//	{
//		if (tab == null)
//		{
//			throw new IllegalArgumentException("Null ResultTab passed");
//		}
//		s_log.debug("SQLPanel.createWindow(" + tab.getIdentifier().toString() + ")");
//		_tabbedResultsPanel.remove(tab);
//		ResultFrame frame = new ResultFrame(_session, tab);
//		ResultTabInfo tabInfo = (ResultTabInfo) _allTabs.get(tab.getIdentifier());
//		tabInfo._resultFrame = frame;
//		_session.getApplication().getMainFrame().addInternalFrame(frame, true, null);
//		fireTabTornOffEvent(tab);
//		frame.setVisible(true);
//
//		// There used to be a frame.pack() here but it resized the frame
//		// to be very wide if text output was used.
//
//		frame.toFront();
//		frame.requestFocus();
//	}

	/**
	 * Return the passed tab back into the tabbed pane.
	 *
	 * @param	tab	<TT>Resulttab</TT> to be returned
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a <TT>null</TT> <TT>ResultTab</TT> passed.
	 */
//	public void returnToTabbedPane(ResultTab tab)
//	{
//		if (tab == null)
//		{
//			throw new IllegalArgumentException("Null ResultTab passed");
//		}
//
//		s_log.debug("SQLPanel.returnToTabbedPane(" + tab.getIdentifier().toString() + ")");
//
//		ResultTabInfo tabInfo = (ResultTabInfo) _allTabs.get(tab.getIdentifier());
//		if (tabInfo._resultFrame != null)
//		{
//			addResultsTab(tab);
//			fireTornOffResultTabReturned(tab);
//			tabInfo._resultFrame = null;
//		}
//	}

	public void setVisible(boolean value)
	{
		super.setVisible(value);
		if (!_hasBeenVisible && value == true)
		{
			_splitPane.setDividerLocation(0.2d);
			_hasBeenVisible = true;
		}
	}

	/**
	 * Display the next tab in the SQL results.
	 */
//	public void gotoNextResultsTab()
//	{
//		final int tabCount = _tabbedResultsPanel.getTabCount();
//		if (tabCount > 1)
//		{
//			int nextTabIdx = _tabbedResultsPanel.getSelectedIndex() + 1;
//			if (nextTabIdx >= tabCount)
//			{
//				nextTabIdx = 0;
//			}
//			_tabbedResultsPanel.setSelectedIndex(nextTabIdx);
//		}
//	}

	/**
	 * Display the previous tab in the SQL results.
	 */
//	public void gotoPreviousResultsTab()
//	{
//		final int tabCount = _tabbedResultsPanel.getTabCount();
//		if (tabCount > 1)
//		{
//			int prevTabIdx = _tabbedResultsPanel.getSelectedIndex() - 1;
//			if (prevTabIdx < 0)
//			{
//				prevTabIdx = tabCount - 1;
//			}
//			_tabbedResultsPanel.setSelectedIndex(prevTabIdx);
//		}
//	}

	/**
	 * Add the passed item to end of the SQL history. If the item
	 * at the end of the history is the same as the passed one
	 * then don't add it.
	 *
	 * @param	sql		SQL item to add.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> sql passed.
	 */
	public void addSQLToHistory(SQLHistoryItem sql)
	{
		if (sql == null)
		{
			throw new IllegalArgumentException("SQLHistoryItem == null");
		}

		_sqlComboListener.stopListening();
		try
		{
			int beforeSize = 0;
			int afterSize = _sqlCombo.getItemCount();
			do
			{
				beforeSize = afterSize;
				_sqlCombo.removeItem(sql);
				afterSize = _sqlCombo.getItemCount();
			} while (beforeSize != afterSize);
			_sqlCombo.insertItemAt(sql, afterSize);
			_sqlCombo.setSelectedIndex(afterSize);
		}
		finally
		{
			_sqlComboListener.startListening();
		}
	}

	/**
	 * Add a hierarchical menu to the SQL Entry Area popup menu.
	 *
	 * @param	menu	The menu that will be added.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>Menu</TT> passed.
	 */
	public void addToSQLEntryAreaMenu(JMenu menu)
	{
		if (menu == null)
		{
			throw new IllegalArgumentException("Menu == null");
		}
		getSQLEntryPanel().addToSQLEntryAreaMenu(menu);
	}

	/**
	 * Add an <TT>Action</TT> to the SQL Entry Area popup menu.
	 *
	 * @param	action	The action to be added.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>Action</TT> passed.
	 */
	public JMenuItem addToSQLEntryAreaMenu(Action action)
	{
		if (action == null)
		{
			throw new IllegalArgumentException("Action == null");
		}
		return getSQLEntryPanel().addToSQLEntryAreaMenu(action);
	}

	protected void fireSQLEntryAreaInstalled()
	{
		// Guaranteed to be non-null.
		Object[] listeners = _listeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event.
		SQLPanelEvent evt = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == ISQLPanelListener.class)
			{
				// Lazily create the event:
				if (evt == null)
				{
					evt = new SQLPanelEvent(_session, this);
				}
				((ISQLPanelListener)listeners[i + 1]).sqlEntryAreaInstalled(evt);
			}
		}
	}

//	protected void fireTabAddedEvent(ResultTab tab)
//	{
//		// Guaranteed to be non-null.
//		Object[] listeners = _listeners.getListenerList();
//		// Process the listeners last to first, notifying
//		// those that are interested in this event.
//		ResultTabEvent evt = null;
//		for (int i = listeners.length - 2; i >= 0; i -= 2)
//		{
//			if (listeners[i] == IResultTabListener.class)
//			{
//				// Lazily create the event:
//				if (evt == null)
//				{
//					evt = new ResultTabEvent(_session, tab);
//				}
//				((IResultTabListener)listeners[i + 1]).resultTabAdded(evt);
//			}
//		}
//	}

//	protected void fireTabRemovedEvent(ResultTab tab)
//	{
//		// Guaranteed to be non-null.
//		Object[] listeners = _listeners.getListenerList();
//		// Process the listeners last to first, notifying
//		// those that are interested in this event.
//		ResultTabEvent evt = null;
//		for (int i = listeners.length - 2; i >= 0; i -= 2)
//		{
//			if (listeners[i] == IResultTabListener.class)
//			{
//				// Lazily create the event:
//				if (evt == null)
//				{
//					evt = new ResultTabEvent(_session, tab);
//				}
//				((IResultTabListener) listeners[i + 1]).resultTabRemoved(evt);
//			}
//		}
//	}

//	protected void fireTabRemovedEvent(ResultTab tab)
//	{
//		// Guaranteed to be non-null.
//		Object[] listeners = _listeners.getListenerList();
//		// Process the listeners last to first, notifying
//		// those that are interested in this event.
//		ResultTabEvent evt = null;
//		for (int i = listeners.length - 2; i >= 0; i -= 2)
//		{
//			if (listeners[i] == IResultTabListener.class)
//			{
//				// Lazily create the event:
//				if (evt == null)
//				{
//					evt = new ResultTabEvent(_session, tab);
//				}
//				((IResultTabListener) listeners[i + 1]).resultTabRemoved(evt);
//			}
//		}
//	}

	protected void fireTabTornOffEvent(ResultTab tab)
	{
		// Guaranteed to be non-null.
		Object[] listeners = _listeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event.
		ResultTabEvent evt = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == IResultTabListener.class)
			{
				// Lazily create the event:
				if (evt == null)
				{
					evt = new ResultTabEvent(_session, tab);
				}
				((IResultTabListener) listeners[i + 1]).resultTabTornOff(evt);
			}
		}
	}

	protected void fireTornOffResultTabReturned(ResultTab tab)
	{
		// Guaranteed to be non-null.
		Object[] listeners = _listeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event.
		ResultTabEvent evt = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == IResultTabListener.class)
			{
				// Lazily create the event:
				if (evt == null)
				{
					evt = new ResultTabEvent(_session, tab);
				}
				((IResultTabListener) listeners[i + 1]).tornOffResultTabReturned(evt);
			}
		}
	}

	protected List fireAllSQLToBeExecutedEvent(List sql)
	{
		// Guaranteed to be non-null.
		Object[] listeners = _listeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event.
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == ISQLExecutionListener.class)
			{
				((ISQLExecutionListener)listeners[i + 1]).allStatementsExecuting(sql);
				if (sql.size() == 0)
				{
					break;
				}
			}
		}
		return sql;
	}

	protected String fireSQLToBeExecutedEvent(String sql)
	{
		// Guaranteed to be non-null.
		Object[] listeners = _listeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event.
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == ISQLExecutionListener.class)
			{
				sql = ((ISQLExecutionListener)listeners[i + 1]).statementExecuting(sql);
				if (sql == null)
				{
					break;
				}
			}
		}
		return sql;
	}

	protected void fireExecuterTabAdded(ISQLResultExecuter exec)
	{
		// Guaranteed to be non-null.
		Object[] listeners = _listeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event.
		SQLResultExecuterTabEvent evt = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == ISQLResultExecuterTabListener.class)
			{
				// Lazily create the event:
				if (evt == null)
				{
					evt = new SQLResultExecuterTabEvent(_session, exec);
				}
				((ISQLResultExecuterTabListener)listeners[i + 1]).executerTabAdded(evt);
			}
		}
	}

	protected void fireExecuterTabActivated(ISQLResultExecuter exec)
	{
		// Guaranteed to be non-null.
		Object[] listeners = _listeners.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event.
		SQLResultExecuterTabEvent evt = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == ISQLResultExecuterTabListener.class)
			{
				// Lazily create the event:
				if (evt == null)
				{
					evt = new SQLResultExecuterTabEvent(_session, exec);
				}
				((ISQLResultExecuterTabListener)listeners[i + 1]).executerTabActivated(evt);
			}
		}
	}

//	void setCancelPanel(final JPanel panel)
//	{
//		SwingUtilities.invokeLater(new Runnable()
//		{
//			public void run()
//			{
//				_tabbedResultsPanel.addTab("Executing SQL", null, panel, "Press Cancel to Stop");
//				_tabbedResultsPanel.setSelectedComponent(panel);
//			}
//		});
//	}

//	void addResultsTab(SQLExecutionInfo exInfo, ResultSetDataSet rsds,
//						ResultSetMetaDataDataSet mdds, final JPanel cancelPanel,
//						IDataSetUpdateableTableModel creator)
//	{
//		final ResultTab tab;
//		if (_availableTabs.size() > 0)
//		{
//			ResultTabInfo ti = (ResultTabInfo)_availableTabs.remove(0);
//			_usedTabs.add(ti);
//			tab = ti._tab;
//			s_log.debug("Using tab " + tab.getIdentifier().toString() + " for results.");
//		}
//		else
//		{
//			tab = new ResultTab(_session, this, _idFactory.createIdentifier(), exInfo, creator);
//			ResultTabInfo ti = new ResultTabInfo(tab);
//			_allTabs.put(tab.getIdentifier(), ti);
//			_usedTabs.add(ti);
//			s_log.debug("Created new tab " + tab.getIdentifier().toString() + " for results.");
//		}
//
//		try
//		{
//			tab.showResults(rsds, mdds, exInfo);
//			SwingUtilities.invokeLater(new Runnable()
//			{
//				public void run()
//				{
//					_tabbedResultsPanel.remove(cancelPanel);
//					addResultsTab(tab);
//					_tabbedResultsPanel.setSelectedComponent(tab);
//					fireTabAddedEvent(tab);
//				}
//			});
//		}
//		catch (DataSetException dse)
//		{
//			_session.getMessageHandler().showErrorMessage(dse);
//		}
//	}

//	void removeCancelPanel(final JPanel cancelPanel)
//	{
//		SwingUtilities.invokeLater(new Runnable()
//		{
//			public void run()
//			{
//				_tabbedResultsPanel.remove(cancelPanel);
//			}
//		});
//	}

//	private void addResultsTab(ResultTab tab)
//	{
//		_tabbedResultsPanel.addTab(tab.getTitle(), null, tab, tab.getViewableSqlString());
//	}

//	private String modifyIndividualScript(String sql)
//	{
//		// Guaranteed to be non-null.
//		Object[] listeners = _listeners.getListenerList();
//		// Process the listeners last to first, notifying
//		// those that are interested in this event.
//		for (int i = listeners.length - 2; i >= 0; i -= 2)
//		{
//			if (listeners[i] == ISQLExecutionListener.class)
//			{
//				sql = ((ISQLExecutionListener) listeners[i]).statementExecuting(sql);
//				if (sql == null)
//				{
//					break;
//				}
//			}
//		}
//
//		return sql;
//	}

	private void appendSQL(String sql)
	{
		if (_sqlEntry.getText().length() > 0)
		{
			_sqlEntry.appendText(LINE_SEPARATOR + LINE_SEPARATOR);
		}
		_sqlEntry.appendText(sql, true);
		_sqlEntry.requestFocus();
	}

	private void copySelectedItemToEntryArea()
	{
		SQLHistoryItem item = (SQLHistoryItem)_sqlCombo.getSelectedItem();
		if (item != null)
		{
			appendSQL(item.getSQL());
		}
	}

	private void propertiesHaveChanged(String propName)
	{
		final SessionProperties props = _session.getProperties();
		if (propName == null || propName.equals(
				SessionProperties.IPropertyNames.SQL_SHARE_HISTORY))
		{
			_sqlCombo.setUseSharedModel(props.getSQLShareHistory());
		}

		if (propName == null || propName.equals(SessionProperties.IPropertyNames.AUTO_COMMIT))
		{
			final SQLConnection conn = _session.getSQLConnection();
			if (conn != null)
			{
				boolean auto = true;
				try
				{
					auto = conn.getAutoCommit();
				}
				catch (SQLException ex)
				{
					s_log.error("Error with transaction control", ex);
					_session.getMessageHandler().showErrorMessage(ex);
				}
				try
				{
					conn.setAutoCommit(props.getAutoCommit());
				}
				catch (SQLException ex)
				{
					props.setAutoCommit(auto);
					_session.getMessageHandler().showErrorMessage(ex);
				}
			}
		}

		if (propName == null || propName.equals(SessionProperties.IPropertyNames.SQL_LIMIT_ROWS))
		{
			_limitRowsChk.setSelected(props.getSQLLimitRows());
		}

		if (propName == null
			|| propName.equals(SessionProperties.IPropertyNames.SQL_NBR_ROWS_TO_SHOW))
		{
			_nbrRows.setInt(props.getSQLNbrRowsToShow());
		}

		if (propName == null || propName.equals(SessionProperties.IPropertyNames.FONT_INFO))
		{
			FontInfo fi = props.getFontInfo();
			if (fi != null)
			{
				_sqlEntry.setFont(fi.createFont());
			}
		}

		if (propName == null || propName.equals(SessionProperties.IPropertyNames.SQL_ENTRY_HISTORY_SIZE)
							|| propName.equals(SessionProperties.IPropertyNames.LIMIT_SQL_ENTRY_HISTORY_SIZE))
		{
			if (props.getLimitSQLEntryHistorySize())
			{
				_sqlCombo.setMaxMemoryCount(props.getSQLEntryHistorySize());
			}
			else
			{
				_sqlCombo.setMaxMemoryCount(MemoryComboBox.NO_MAX);
			}
		}

//		if (propName == null
//			|| propName.equals(SessionProperties.IPropertyNames.SQL_EXECUTION_TAB_PLACEMENT))
//		{
//			_tabbedResultsPanel.setTabPlacement(props.getSQLExecutionTabPlacement());
//		}
	}

	private void createGUI()
	{
		final IApplication app = _session.getApplication();
		synchronized (getClass())
		{
			if (!s_loadedSQLHistory)
			{
				final SQLHistory sqlHistory = app.getSQLHistory();
				SQLHistoryComboBoxModel.initializeSharedInstance(sqlHistory.getData());
				s_loadedSQLHistory = true;
			}
		}

//		_tabbedResultsPanel = UIFactory.getInstance().createTabbedPane();
		_tabbedExecuterPanel = UIFactory.getInstance().createTabbedPane();
		_tabbedExecuterPanel.addChangeListener(new MyExecuterPaneListener());

		setLayout(new BorderLayout());

		_nbrRows.setColumns(8);

		final SessionProperties props = _session.getProperties();
		_sqlCombo = new SQLHistoryComboBox(props.getSQLShareHistory());
		_sqlCombo.setEditable(false);
		if (_sqlCombo.getItemCount() > 0)
		{
			_sqlCombo.setSelectedIndex(_sqlCombo.getItemCount() - 1);
		}

		{
			JPanel pnl = new JPanel();
			pnl.setLayout(new BorderLayout());
			pnl.add(_sqlCombo, BorderLayout.CENTER);

			Box box = Box.createHorizontalBox();
			box.add(new CopyLastButton(app));
			box.add(Box.createHorizontalStrut(10));
			box.add(_limitRowsChk);
			box.add(Box.createHorizontalStrut(5));
			box.add(_nbrRows);
			pnl.add(box, BorderLayout.EAST);
			add(pnl, BorderLayout.NORTH);
		}

		_splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		_splitPane.setOneTouchExpandable(true);

		installSQLEntryPanel(app.getSQLEntryPanelFactory().createSQLEntryPanel(_session));
//		_splitPane.add(_tabbedResultsPanel, JSplitPane.RIGHT);
		_splitPane.add(_tabbedExecuterPanel, JSplitPane.RIGHT);

		add(_splitPane, BorderLayout.CENTER);

		_sqlCombo.addActionListener(_sqlComboListener);
		_limitRowsChk.addChangeListener(new LimitRowsCheckBoxListener());
		_nbrRows.getDocument().addDocumentListener(new LimitRowsTextBoxListener());

		// Add a label to the session sheets statusbar to show the current
		// row/col of the caret in the sql entry area.
		_session.addToStatusBar(_rowColLbl);

		// Set focus to the SQL entry panel.
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				_sqlEntry.getTextComponent().requestFocus();
			}
		});
	}

	/**
	 * Listens for changes in the execution jtabbedpane and then fires
	 * activation events
	 */
	private class MyExecuterPaneListener implements ChangeListener
	{
		public void stateChanged(ChangeEvent e)
		{
			JTabbedPane pane = (JTabbedPane)e.getSource();
			int index = pane.getSelectedIndex();
			if (index != -1)
			{
				fireExecuterTabActivated((ISQLResultExecuter)executors.get(index));
			}
		}
	}

	private class MyPropertiesListener implements PropertyChangeListener
	{
		private boolean _listening = true;

		void stopListening()
		{
			_listening = false;
		}

		void startListening()
		{
			_listening = true;
		}

		public void propertyChange(PropertyChangeEvent evt)
		{
			if (_listening)
			{
				propertiesHaveChanged(evt.getPropertyName());
			}
		}
	}

	private class SqlComboListener implements ActionListener
	{
		private boolean _listening = true;

		void stopListening()
		{
			_listening = false;
		}

		void startListening()
		{
			_listening = true;
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (_listening)
			{
				// Because the datamodel for the combobox may be shared
				// between sessions we only want to update the sql entry area
				// if this is actually the combox box that a new item has been
				// selected in.
//				SessionWindowManager winMgr = _session.getApplication().getSessionWindowManager();
//				if (winMgr.getInternalFrame(_session).isSelected())
//				{
					copySelectedItemToEntryArea();
				}
			}
//		}

//		private void copySelectedItemToEntryArea()
//		{
//			SQLHistoryItem item = (SQLHistoryItem)_sqlCombo.getSelectedItem();
//			if (item != null)
//			{
//				appendSQL(item.getSQL());
//			}
//		}
	}

	private class LimitRowsCheckBoxListener implements ChangeListener
	{
		public void stateChanged(ChangeEvent evt)
		{
			if (_propsListener != null)
			{
				_propsListener.stopListening();
			}
			try
			{
				final boolean limitRows = ((JCheckBox)evt.getSource()).isSelected();
				_nbrRows.setEnabled(limitRows);
				_session.getProperties().setSQLLimitRows(limitRows);
			}
			finally
			{
				if (_propsListener != null)
				{
					_propsListener.startListening();
				}
			}
		}
	}

	private class LimitRowsTextBoxListener implements DocumentListener
	{
		public void insertUpdate(DocumentEvent evt)
		{
			updateProperties(evt);
		}

		public void changedUpdate(DocumentEvent evt)
		{
			updateProperties(evt);
		}

		public void removeUpdate(DocumentEvent evt)
		{
			updateProperties(evt);
		}

		private void updateProperties(DocumentEvent evt)
		{
			if (_propsListener != null)
			{
				_propsListener.stopListening();
			}
			try
			{
				_session.getProperties().setSQLNbrRowsToShow(_nbrRows.getInt());
			}
			finally
			{
				if (_propsListener != null)
				{
					_propsListener.startListening();
				}
			}
		}
	}

//	private final static class ResultTabInfo
//	{
//		final ResultTab _tab;
//		ResultFrame _resultFrame;
//
//		ResultTabInfo(ResultTab tab)
//		{
//			if (tab == null)
//			{
//				throw new IllegalArgumentException("Null ResultTab passed");
//			}
//			_tab = tab;
//		}
//	}

	private final class SQLEntryState
	{
		private SQLPanel _sqlPnl;
		private boolean _saved = false;
		private String _text;
		private int _caretPos;
		private int _selStart;
		private int _selEnd;
		private boolean _hasFocus;

		SQLEntryState(SQLPanel sqlPnl, ISQLEntryPanel pnl)
		{
			super();
			_sqlPnl = sqlPnl;
			if (pnl != null)
			{
				_saved = true;
				_text = pnl.getText();
				_selStart = pnl.getSelectionStart();
				_selEnd = pnl.getSelectionEnd();
				_caretPos = pnl.getCaretPosition();
				//??
				//				_hasFocus = SwingUtilities.findFocusOwner(sqlPnl) == pnl.getComponent();
				//				_hasFocus = pnl.hasFocus();
				//				_hasFocus = sqlPnl.f pnl.hasFocus();
				_hasFocus = true;
			}
		}

		void restoreState(final ISQLEntryPanel pnl)
		{
			if (_saved && pnl != null)
			{
				pnl.setText(_text);
				pnl.setSelectionStart(_selStart);
				pnl.setSelectionEnd(_selEnd);
				//pnl.setCaretPosition(_caretPos);
				//				if (_hasFocus) {
				//					SwingUtilities.invokeLater(new Runnable() {
				//						public void run() {
				//							pnl.requestFocus();
				//						}
				//					});
				//				}
			}
		}
	}

	private final class DataEntryAreaCaretListener implements CaretListener
	{
		public void caretUpdate(CaretEvent evt)
		{
			final StringBuffer msg = new StringBuffer();
			msg.append(_sqlEntry.getCaretLineNumber() + 1)
				.append(",").append(_sqlEntry.getCaretLinePosition() + 1);
			SQLPanel.this._rowColLbl.setText(msg.toString());
		}
	}

	private final class GetLastSQLAction extends SquirrelAction
	{
		GetLastSQLAction(IApplication app, Resources rsrc)
		{
			super(app, rsrc);
		}

		public void actionPerformed(ActionEvent evt)
		{
			int idx = _sqlCombo.getItemCount() - 1;
			if (idx > -1)
			{
				final SQLHistoryItem hi = (SQLHistoryItem)_sqlCombo.getItemAt(idx);
				appendSQL(hi.getSQL());
			}
		}
	}

	private final class RowColumnLabel extends JLabel
	{
		RowColumnLabel()
		{
			super(" ", JLabel.CENTER);
		}

		/**
		 * Return the preferred size of this component.
		 *
		 * @return	the preferred size of this component.
		 */
		public Dimension getPreferredSize()
		{
			Dimension dim = super.getPreferredSize();
			FontMetrics fm = getFontMetrics(getFont());
			dim.width = fm.stringWidth("000,000");
			Border border = getBorder();
			if (border != null)
			{
				Insets ins = border.getBorderInsets(this);
				if (ins != null)
				{
					dim.width += (ins.left + ins.right);
				}
			}
			Insets ins = getInsets();
			if (ins != null)
			{
				dim.width += (ins.left + ins.right);
			}
			return dim;
		}
	}

	private class CopyLastButton extends JButton
	{
		CopyLastButton(IApplication app)
		{
			super();
			final SquirrelResources rsrc = app.getResources();
			final ImageIcon icon = rsrc.getIcon(SquirrelResources.IImageNames.COPY_SELECTED);
			setIcon(icon);
			setToolTipText("Copy current SQL history to entry area");
			Dimension dm = getPreferredSize();
			dm.setSize(dm.height, dm.height);
			setPreferredSize(dm);
			addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					copySelectedItemToEntryArea();
				}
			});
		}
	}

	/**
	 * This class is responsible for listening for sql that executes
	 * for a SQLExecuterPanel and adding it to the SQL history.
	 */
	private class SQLExecutorHistoryListener implements ISQLExecutionListener
	{
		public void allStatementsExecuting(List sql)
		{
		}
		public String statementExecuting(String sql)
		{
			_panelAPI.addSQLToHistory(sql);
			return sql;
		}
	}
}
