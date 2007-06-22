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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.HashMap;
import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.undo.UndoManager;

import net.sourceforge.squirrel_sql.fw.gui.FontInfo;
import net.sourceforge.squirrel_sql.fw.gui.IntegerField;
import net.sourceforge.squirrel_sql.fw.gui.MemoryComboBox;
import net.sourceforge.squirrel_sql.fw.sql.ISQLConnection;
import net.sourceforge.squirrel_sql.fw.util.Resources;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.gui.builders.UIFactory;
import net.sourceforge.squirrel_sql.client.resources.SquirrelResources;
import net.sourceforge.squirrel_sql.client.session.*;
import net.sourceforge.squirrel_sql.client.session.action.RedoAction;
import net.sourceforge.squirrel_sql.client.session.action.UndoAction;
import net.sourceforge.squirrel_sql.client.session.action.OpenSqlHistoryAction;
import net.sourceforge.squirrel_sql.client.session.event.*;
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

    /** Internationalized strings for this class. */
    private static final StringManager s_stringMgr =
        StringManagerFactory.getStringManager(SQLPanel.class);   
    
	/** Used to separate lines in the SQL entry area. */
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
	private JCheckBox _limitRowsChk;
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

   /**
    * Is the bottom component of the split.
    * Holds the _simpleExecuterPanel if there is just one entry in _executors,
    * holds the _tabbedExecuterPanel if there is more that one element in _executors, 
    */
   private JPanel _executerPanleHolder;

	private JTabbedPane _tabbedExecuterPanel;
	private JPanel _simpleExecuterPanel;

	private boolean _hasBeenVisible = false;
	private JSplitPane _splitPane;


	/** Listeners */
	private EventListenerList _listeners = new EventListenerList();

	private UndoManager _undoManager = new SquirrelDefaultUndoManager();

	/** Factory for generating unique IDs for new <TT>ResultTab</TT> objects. */
//	private IntegerIdentifierFactory _idFactory = new IntegerIdentifierFactory();

	private final List _executors = new ArrayList();

	private SQLResultExecuterPanel _sqlExecPanel;

	private ISQLPanelAPI _panelAPI;

   private static final String PREFS_KEY_SPLIT_DIVIDER_LOC = "squirrelSql_sqlPanel_divider_loc";
   private UndoAction _undoAction;
   private RedoAction _redoAction;

   /**
    * true if this panel is within a SessionInternalFrame
    * false if this panle is within a SQLInternalFrame
    */
   private boolean _inMainSessionWindow;
	private SQLPanel.SQLExecutorHistoryListener _sqlExecutorHistoryListener = new SQLExecutorHistoryListener();
   private ArrayList<SqlPanelListener> _sqlPanelListeners = new ArrayList<SqlPanelListener>();


   /**
	 * Ctor.
	 *
	 * @param	session	 Current session.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a <TT>null</TT> <TT>ISession</TT> passed.
	 */
	public SQLPanel(ISession session, boolean isInMainSessionWindow)
	{
		super();
		_inMainSessionWindow = isInMainSessionWindow;
		setSession(session);
		createGUI();
		propertiesHaveChanged(null);
		_sqlExecPanel = new SQLResultExecuterPanel(session);
		_sqlExecPanel.addSQLExecutionListener(_sqlExecutorHistoryListener);
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
		_executors.add(exec);

      if(1 == _executors.size())
      {
         _executerPanleHolder.remove(_tabbedExecuterPanel);
         _executerPanleHolder.add(_simpleExecuterPanel);
      }
      else if(2 == _executors.size())
      {
         _executerPanleHolder.remove(_simpleExecuterPanel);
         _executerPanleHolder.add(_tabbedExecuterPanel);
         _executors.get(0);
         ISQLResultExecuter buf = (ISQLResultExecuter) _executors.get(0);
         _tabbedExecuterPanel.addTab(buf.getTitle(), null, buf.getComponent(), buf.getTitle());
      }


      if( 1 < _executors.size())
      {
         _tabbedExecuterPanel.addTab(exec.getTitle(), null, exec.getComponent(), exec.getTitle());
      }
      else
      {
         _simpleExecuterPanel.add(exec.getComponent());
      }

		this.fireExecuterTabAdded(exec);
	}

	public void removeExecutor(ISQLResultExecuter exec)
	{
		_executors.remove(exec);
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
		//_listeners.add(ISQLExecutionListener.class, lis);
      _sqlExecPanel.addSQLExecutionListener(lis);
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
		//_listeners.remove(ISQLExecutionListener.class, lis);
      _sqlExecPanel.removeSQLExecutionListener(lis);
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


	public synchronized void removeExecuterTabListener(ISQLResultExecuterTabListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("ISQLResultExecuterTabListener == null");
		}
		_listeners.remove(ISQLResultExecuterTabListener.class, lis);
	}


	public ISQLEntryPanel getSQLEntryPanel()
	{
		return _sqlEntry;
	}


	public void runCurrentExecuter()
	{
      if(1 == _executors.size())
      {
         ISQLResultExecuter exec = (ISQLResultExecuter) _executors.get(0);
         exec.execute(_sqlEntry);
      }
      else
      {
         int selectedIndex = _tabbedExecuterPanel.getSelectedIndex();
         ISQLResultExecuter exec = (ISQLResultExecuter)_executors.get(selectedIndex);
         exec.execute(_sqlEntry);
      }
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
	}

   public void sessionWindowClosing()
   {

      fireSQLEntryAreaClosed();

      if(_hasBeenVisible)
      {
         int dividerLoc = _splitPane.getDividerLocation();
         Preferences.userRoot().putInt(PREFS_KEY_SPLIT_DIVIDER_LOC, dividerLoc);
      }

		_sqlCombo.removeActionListener(_sqlComboListener);
		_sqlCombo.dispose();
		_sqlExecPanel.removeSQLExecutionListener(_sqlExecutorHistoryListener);
		



      for (SqlPanelListener l : _sqlPanelListeners)
      {
         l.panelParentWindowClosing();
      }

      _sqlEntry.dispose();


   }


	private void installSQLEntryPanel(ISQLEntryPanel pnl)
	{
		if (pnl == null)
		{
			throw new IllegalArgumentException("Null ISQLEntryPanel passed");
		}

		_sqlEntry = pnl;

		final int pos = _splitPane.getDividerLocation();
		if (!_sqlEntry.getDoesTextComponentHaveScroller())
		{
			_sqlEntryScroller = new JScrollPane(_sqlEntry.getTextComponent());
			_sqlEntryScroller.setBorder(BorderFactory.createEmptyBorder());
			_splitPane.add(_sqlEntryScroller);
		}
		else
		{
			_splitPane.add(_sqlEntry.getTextComponent(), JSplitPane.LEFT);
		}
		_splitPane.setDividerLocation(pos);

		if (!_sqlEntry.hasOwnUndoableManager())
		{
			IApplication app = _session.getApplication();
			Resources res = app.getResources();
			_undoAction = new UndoAction(app, _undoManager);
			_redoAction = new RedoAction(app, _undoManager);

			JComponent comp = _sqlEntry.getTextComponent();
			comp.registerKeyboardAction(_undoAction, res.getKeyStroke(_undoAction),
							WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
			comp.registerKeyboardAction(_redoAction, res.getKeyStroke(_redoAction),
							WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
			_sqlEntry.setUndoActions(_undoAction, _redoAction);

			_sqlEntry.setUndoManager(_undoManager);
		}

      fireSQLEntryAreaInstalled();
	}



   public void setVisible(boolean value)
	{
      super.setVisible(value);
      if (!_hasBeenVisible && value == true)
      {
         final int dividerLoc = Preferences.userRoot().getInt(PREFS_KEY_SPLIT_DIVIDER_LOC, _splitPane.getMaximumDividerLocation() / 4);

         SwingUtilities.invokeLater(new Runnable()
         {
            public void run()
            {
               _splitPane.setDividerLocation(dividerLoc);
            }
         });

         _hasBeenVisible = true;
      }
	}


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
         _sqlCombo.repaint();
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

	private void fireSQLEntryAreaInstalled()
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


   private void fireSQLEntryAreaClosed()
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
            ((ISQLPanelListener)listeners[i + 1]).sqlEntryAreaClosed(evt);
         }
      }
   }


   private void fireTabTornOffEvent(IResultTab tab)
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

	private void fireTornOffResultTabReturned(IResultTab tab)
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

   private void fireExecuterTabAdded(ISQLResultExecuter exec)
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

	private void fireExecuterTabActivated(ISQLResultExecuter exec)
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

	private void openSQLHistory()
	{
      new SQLHistoryController(_session, getSQLPanelAPI(), ((SQLHistoryComboBoxModel)_sqlCombo.getModel()).getItems());
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
            SetAutoCommitTask task = new SetAutoCommitTask();
            if (SwingUtilities.isEventDispatchThread()) {
                _session.getApplication().getThreadPool().addTask(task);
            } else {
                task.run();
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

	}

   public void addSqlPanelListener(SqlPanelListener sqlPanelListener)
   {
      _sqlPanelListeners.add(sqlPanelListener);
   }

   public ArrayList<SQLHistoryItem> getSQLHistoryItems()
   {
      return ((SQLHistoryComboBoxModel)_sqlCombo.getModel()).getItems();
   }

   private class SetAutoCommitTask implements Runnable {
        
        public void run() {
            final ISQLConnection conn = _session.getSQLConnection();
            final SessionProperties props = _session.getProperties();
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
                    _session.showErrorMessage(ex);
                }
                try
                {
                    conn.setAutoCommit(props.getAutoCommit());
                }
                catch (SQLException ex)
                {
                    props.setAutoCommit(auto);
                    _session.showErrorMessage(ex);
                }
            }        
        }
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
			box.add(new ShowHistoryButton(app));
			box.add(Box.createHorizontalStrut(10));
            // i18n[SQLPanel.limitrowscheckbox.hint=Limit rows: ]
            String hint = 
                s_stringMgr.getString("SQLPanel.limitrowscheckbox.label");
            _limitRowsChk = new JCheckBox(hint);
			box.add(_limitRowsChk);
			box.add(Box.createHorizontalStrut(5));
			box.add(_nbrRows);
			pnl.add(box, BorderLayout.EAST);
			add(pnl, BorderLayout.NORTH);
		}

		_splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		_splitPane.setOneTouchExpandable(true);

		installSQLEntryPanel(app.getSQLEntryPanelFactory().createSQLEntryPanel(_session, new HashMap()));

      _executerPanleHolder = new JPanel(new GridLayout(1,1));
      _simpleExecuterPanel = new JPanel(new GridLayout(1,1));
      _executerPanleHolder.add(_simpleExecuterPanel);
      _splitPane.add(_executerPanleHolder, JSplitPane.RIGHT);

		add(_splitPane, BorderLayout.CENTER);

		_sqlCombo.addActionListener(_sqlComboListener);
		_limitRowsChk.addChangeListener(new LimitRowsCheckBoxListener());
		_nbrRows.getDocument().addDocumentListener(new LimitRowsTextBoxListener());

		// Set focus to the SQL entry panel.
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				_sqlEntry.getTextComponent().requestFocus();
			}
		});
	}

   public Action getUndoAction()
   {
      return _undoAction;
   }

   public Action getRedoAction()
   {
      return _redoAction;
   }

   public boolean isInMainSessionWindow()
   {
      return _inMainSessionWindow;
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
				fireExecuterTabActivated((ISQLResultExecuter)_executors.get(index));
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


	private class CopyLastButton extends JButton
	{
		CopyLastButton(IApplication app)
		{
			super();
			final SquirrelResources rsrc = app.getResources();
			final ImageIcon icon = rsrc.getIcon(SquirrelResources.IImageNames.COPY_SELECTED);
			setIcon(icon);
            // i18n[SQLPanel.copylastbutton.hint=Copy current SQL history to entry area]
			String hint = s_stringMgr.getString("SQLPanel.copylastbutton.hint");
            setToolTipText(hint);
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

	private class ShowHistoryButton extends JButton
	{
		ShowHistoryButton(IApplication app)
		{
         final SquirrelResources rsrc = app.getResources();
         final ImageIcon icon = rsrc.getIcon(SquirrelResources.IImageNames.SQL_HISTORY);
         setIcon(icon);
         // i18n[SQLPanel.openSqlHistory.hint=Open SQL History]
         String hint = s_stringMgr.getString("SQLPanel.openSqlHistory.hint");
         setToolTipText(hint);
         Dimension dm = getPreferredSize();
         dm.setSize(dm.height, dm.height);
			setPreferredSize(dm);
         addActionListener(_session.getApplication().getActionCollection().get(OpenSqlHistoryAction.class));
		}
	}

	/**
	 * This class is responsible for listening for sql that executes
	 * for a SQLExecuterPanel and adding it to the SQL history.
	 */
	private class SQLExecutorHistoryListener extends SQLExecutionAdapter
	{
      public void statementExecuted(String sql)
      {
         _panelAPI.addSQLToHistory(sql);
      }
	}
}
