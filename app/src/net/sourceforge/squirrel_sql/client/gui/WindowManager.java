package net.sourceforge.squirrel_sql.client.gui;
/*
 * Copyright (C) 2003-2004 Colin Bell
 * colbell@users.sourceforge.net
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
import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.ActionCollection;
import net.sourceforge.squirrel_sql.client.gui.db.*;
import net.sourceforge.squirrel_sql.client.gui.mainframe.MainFrame;
import net.sourceforge.squirrel_sql.client.gui.mainframe.MainFrameWindowState;
import net.sourceforge.squirrel_sql.client.gui.session.BaseSessionInternalFrame;
import net.sourceforge.squirrel_sql.client.gui.session.ObjectTreeInternalFrame;
import net.sourceforge.squirrel_sql.client.gui.session.SQLInternalFrame;
import net.sourceforge.squirrel_sql.client.gui.session.SessionInternalFrame;
import net.sourceforge.squirrel_sql.client.gui.util.ThreadCheckingRepaintManager;
import net.sourceforge.squirrel_sql.client.mainframe.action.*;
import net.sourceforge.squirrel_sql.client.preferences.SquirrelPreferences;
import net.sourceforge.squirrel_sql.client.session.IObjectTreeAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.SessionManager;
import net.sourceforge.squirrel_sql.client.session.event.SessionAdapter;
import net.sourceforge.squirrel_sql.client.session.event.SessionEvent;
import net.sourceforge.squirrel_sql.client.session.properties.EditWhereColsSheet;
import net.sourceforge.squirrel_sql.client.session.properties.SessionPropertiesSheet;
import net.sourceforge.squirrel_sql.client.session.sqlfilter.SQLFilterSheet;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.gui.WindowState;
import net.sourceforge.squirrel_sql.fw.gui.action.SelectInternalFrameAction;
import net.sourceforge.squirrel_sql.fw.gui.action.SelectInternalFrameCommand;
import net.sourceforge.squirrel_sql.fw.id.IIdentifier;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.ISQLAlias;
import net.sourceforge.squirrel_sql.fw.sql.ISQLDriver;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import java.awt.*;
import java.beans.PropertyVetoException;
/**
 * This class manages the windows for the application.
 *
 * TODO: Correct these notes
 * <p>When a session closes the window manager will ensure that
 * all of the windows for that sesion are closed.
 * <p>Similarily when a window is closed the windows manager will ensure that
 * references to the window are removed for the session.
 *
 * JASON: Prior to this patch there was some code movement from this class to
 * Sessionmanager. The idea being that Sessionmanager was the controller.
 * Do we still want to do this? Remember in the future there will probably be
 * an SDI as well as MDI version of the windows.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 * @author <A HREF="mailto:jmheight@users.sourceforge.net">Jason Height</A>
 */
public class WindowManager
{
	/** Logger for this class. */
	private static final ILogger s_log =
		LoggerController.createLogger(WindowManager.class);

	/** Internationalized strings for this class. */
//	private static final StringManager s_stringMgr =
//		StringManagerFactory.getStringManager(WindowManager.class);

	/**
	 * Key to client property stored in internal frame that udentifies the
	 * internal frame.
	 */
	private static final String MENU = WindowManager.class.getName() + ".menu";

	/** Application API. */
	private final IApplication _app;

	/** Window manager for driver windows. */
	private DriverWindowManager _driverWinMgr;

	/** Window manager for aliases windows. */
	private AliasWindowManager _aliasWinMgr;

	/** Applications main frame. */
	private MainFrame _mainFrame;

	/** Window containing list of database aliases. */
	private AliasesListInternalFrame _aliasesListWindow;

	/** Window containing list of JDBC driver definitions. */
	private DriversListInternalFrame _driversListWindow;

	/** Window Factory for alias maintenace windows. */
//	private final AliasWindowFactory _aliasWinFactory;

	/**
	 * Map of windows(s) that are currently open for a session, keyed by
	 * session ID.
	 */
	private final SessionWindowsHolder _sessionWindows = new SessionWindowsHolder();

	private final SessionWindowListener _windowListener = new SessionWindowListener();

//	private int _lastSessionIdx = 1;

	// JASON: Mow that multiple object trees exist storing the edit
	// where by objectInfo within session won't work. It needs to be objectinfo
	// within something else.
//	private final Map _editWhereColsSheets = new HashMap();

	private final SessionListener _sessionListener = new SessionListener();

	private EventListenerList _listenerList = new EventListenerList();

	private boolean _sessionClosing = false;

	/**
	 * Ctor.
	 *
	 * @param	app		Application API.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>IApplication</TT> passed.
	 */
	public WindowManager(IApplication app)
	{
		super();
		if (app == null)
		{
			throw new IllegalArgumentException("IApplication == null");
		}

		if (s_log.isDebugEnabled())
		{
			RepaintManager.setCurrentManager(new ThreadCheckingRepaintManager());
		}

		_app = app;

		_aliasWinMgr = new AliasWindowManager(_app);
		_driverWinMgr = new DriverWindowManager(_app);

		GUIUtils.processOnSwingEventThread(new Runnable()
		{
			public void run()
			{
				initialize();
			}
		}, true);
	}

	/**
	 * Retrieve applications main frame.
	 *
	 * @return	Applications main frame.
	 */
	public MainFrame getMainFrame()
	{
		return _mainFrame;
	}

	public AliasesListInternalFrame getAliasesListInternalFrame()
	{
		return _aliasesListWindow;
	}

	public DriversListInternalFrame getDriversListInternalFrame()
	{
		return _driversListWindow;
	}

	public WindowState getAliasesWindowState()
	{
		return new WindowState(_aliasesListWindow);
	}

	public WindowState getDriversWindowState()
	{
		return new WindowState(_driversListWindow);
	}

	public void showConnectionInternalFrame(final ISQLAlias alias,
							final ConnectionInternalFrame.IHandler handler)
	{
		if (alias == null)
		{
			throw new IllegalArgumentException("ISQLAlias == null");
		}

		GUIUtils.processOnSwingEventThread(new Runnable()
		{
			public void run()
			{
				ConnectionInternalFrame cif = new ConnectionInternalFrame(
													_app, alias, handler);
				_app.getMainFrame().addInternalFrame(cif, true, null);
				GUIUtils.centerWithinDesktop(cif);
				moveToFront(cif);
			}
		});
	}

	/**
	 * Get a maintenance sheet for the passed alias. If a maintenance sheet already
	 * exists it will be brought to the front. If one doesn't exist it will be
	 * created.
	 *
	 * @param	alias	The alias that user has requested to modify.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a <TT>null</TT> <TT>ISQLAlias</TT> passed.
	 */
	public void showModifyAliasInternalFrame(final ISQLAlias alias)
	{
		if (alias == null)
		{
			throw new IllegalArgumentException("ISQLAlias == null");
		}

		_aliasWinMgr.showModifyAliasInternalFrame(alias);
	}

	/**
	 * Create and show a new maintenance window to allow the user to create a
	 * new alias.
	 */
	public void showNewAliasInternalFrame()
	{
		_aliasWinMgr.showNewAliasInternalFrame();
	}

	/**
	 * Create and show a new maintenance sheet that will allow the user to create a
	 * new alias that is a copy of the passed one.
	 *
	 * @return	The new maintenance sheet.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a <TT>null</TT> <TT>ISQLAlias</TT> passed.
	 */
	public void showCopyAliasInternalFrame(final ISQLAlias alias)
	{
		if (alias == null)
		{
			throw new IllegalArgumentException("ISQLAlias == null");
		}

		_aliasWinMgr.showCopyAliasInternalFrame(alias);
	}

	/**
	 * Get a maintenance sheet for the passed driver. If a maintenance sheet
	 * already exists it will be brought to the front. If one doesn't exist
	 * it will be created.
	 *
	 * @param	driver	The driver that user has requested to modify.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a <TT>null</TT> <TT>ISQLDriver</TT> passed.
	 */
	public void showModifyDriverInternalFrame(final ISQLDriver driver)
	{
		if (driver == null)
		{
			throw new IllegalArgumentException("ISQLDriver == null");
		}

		_driverWinMgr.showModifyDriverInternalFrame(driver);
	}

	/**
	 * Create and show a new maintenance window to allow the user to create a
	 * new driver.
	 */
	public void showNewDriverInternalFrame()
	{
		_driverWinMgr.showNewDriverInternalFrame();
	}

	/**
	 * Create and show a new maintenance sheet that will allow the user to
	 * create a new driver that is a copy of the passed one.
	 *
	 * @return	The new maintenance sheet.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a <TT>null</TT> <TT>ISQLDriver</TT> passed.
	 */
	public void showCopyDriverInternalFrame(final ISQLDriver driver)
	{
		if (driver == null)
		{
			throw new IllegalArgumentException("ISQLDriver == null");
		}

		_driverWinMgr.showCopyDriverInternalFrame(driver);
	}

	/**
	 * Registers a sheet that is attached to a session. This sheet will
	 * be automatically closed when the session is closing.
	 * <p/><b>There is no need to call this method manually.</b> Any
	 * classes that properly extend BaseSessionInternalFrame will be registered.
	 */
	public synchronized void registerSessionSheet(BaseSessionInternalFrame sheet)
	{
		s_log.debug("Registering " + sheet.getClass().getName() + " in WindowManager");
		final IIdentifier sessionIdentifier = sheet.getSession().getIdentifier();

		// Store ptr to newly open window in list of windows per session.
   	final int idx = _sessionWindows.addFrame(sessionIdentifier, sheet);

		// For all windows (other than the first one opened) for a session
		// add a number on the end of the title to differentiate them in
		// menus etc.
		if ( idx > 1)
		{
			sheet.setTitle(sheet.getTitle() + " (" + idx + ")");
		}

		sheet.addInternalFrameListener(_windowListener);
	}

	/**
	 * Adds a listener to the sheets attached to this session <p/>When new
	 * sheets are constructed, they are automatically added to the session via
	 * the registerSessionSheet method. <p/>All other listener events fire due
	 * to interaction with the frame. <p/>The
	 * InternalFrameListener.internalFrameOpened is a good location to tailor
	 * the session sheets (ie internal frame) from a plugin. Examples can be
	 * found in the oracle plugin of how to modify how a session sheet.
	 */
	public void addSessionSheetListener(InternalFrameListener listener)
	{
		if (listener == null)
		{
			throw new IllegalArgumentException("InternalFrameListener == null");
		}

		_listenerList.add(InternalFrameListener.class, listener);
	}

	/**
	 * Create a new internal frame for the passed session.
	 *
	 * @param	session		Session we are creating internal frame for.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if ISession is passed as null.
	 */
	public synchronized SessionInternalFrame createInternalFrame(ISession session)
	{
		if (session == null)
		{
			throw new IllegalArgumentException("ISession == null");
		}

		final SessionInternalFrame sif = new SessionInternalFrame(session);

		session.setSessionInternalFrame(sif);
		_app.getPluginManager().sessionStarted(session);
		_app.getMainFrame().addInternalFrame(sif, true, null);

		// If we don't invokeLater here no Short-Cut-Key is sent
		// to the internal frame
		// seen under java version "1.4.1_01" and Linux
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				sif.setVisible(true);
			}
		});

		return sif;
	}

	/**
	 * Creates a new SQL View internal frame for the passed session.
	 *
	 * @param	session		Session we are creating internal frame for.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if ISession is passed as null.
	 */
	public synchronized SQLInternalFrame createSQLInternalFrame(ISession session)
	{
		if (session == null)
		{
			throw new IllegalArgumentException("ISession == null");
		}
		final SQLInternalFrame sif = new SQLInternalFrame(session);
		getMainFrame().addInternalFrame(sif, true, null);

		// If we don't invokeLater here no Short-Cut-Key is sent
		// to the internal frame
		// seen under java version "1.4.1_01" and Linux
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				sif.setVisible(true);
            sif.requestFocus();
			}
		});

		return sif;
	}

	/**
	 * Creates a new Object Tree internal frame for the passed session.
	 *
	 * @param	session		Session we are creating internal frame for.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if ISession is passed as null.
	 */
	public synchronized ObjectTreeInternalFrame createObjectTreeInternalFrame(ISession session)
	{
		if (session == null)
		{
			throw new IllegalArgumentException("ISession == null");
		}
		final ObjectTreeInternalFrame oif = new ObjectTreeInternalFrame(session);
		getMainFrame().addInternalFrame(oif, true, null);

		// If we don't invokeLater here no Short-Cut-Key is sent
		// to the internal frame
		// seen under java version "1.4.1_01" and Linux
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				oif.setVisible(true);
			}
		});

		return oif;
	}

	/**
	 * Get a properties dialog for the passed session. If one already
	 * exists it will be brought to the front. If one doesn't exist it will be
	 * created.
	 *
	 * @param	session		The session that user has request property dialog for.
    * @param tabNameToSelect The name (title) of the Tab to select. First Tab will be selected
    * if tabNameToSelect is null or doesnt match any tab.
    *
    * @param tabNameToSelect
    * @throws	IllegalArgumentException
    *			Thrown if a <TT>null</TT> <TT>ISession</TT> passed.
	 */
	public synchronized void showSessionPropertiesDialog(ISession session, String tabNameToSelect)
	{
		if (session == null)
		{
			throw new IllegalArgumentException("ISession == null");
		}

		SessionPropertiesSheet propsSheet = getSessionPropertiesDialog(session);
		if (propsSheet == null)
		{
			propsSheet = new SessionPropertiesSheet(session);
			_app.getMainFrame().addInternalFrame(propsSheet, true, null);
			positionSheet(propsSheet);
		}
		else
		{
			moveToFront(propsSheet);
		}

      propsSheet.selectTabByTitle(tabNameToSelect);
   }

	/**
	 * Get an SQL Filter sheet for the passed data. If one already exists it
	 * will be brought to the front. If one doesn't exist it will be created.
	 *
	 * @param	objectTree
	 * @param	objectInfo	An instance of a class containing information about
	 * 						the database metadata.
	 *
	 * @return	The filter dialog.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if <tt>null</tt> <tt>ContentsTab</tt>,
	 *			<tt>IObjectTreeAPI</tt>, or <tt>IDatabaseObjectInfo</tt> passed.
	 */
	public synchronized SQLFilterSheet showSQLFilterDialog(IObjectTreeAPI objectTree,
											IDatabaseObjectInfo objectInfo)
	{
		if (objectTree == null)
		{
			throw new IllegalArgumentException("IObjectTree == null");
		}
		if (objectInfo == null)
		{
			throw new IllegalArgumentException("IDatabaseObjectInfo == null");
		}

		SQLFilterSheet sqlFilterSheet = getSQLFilterSheet(objectTree, objectInfo);
		if (sqlFilterSheet == null)
		{
			sqlFilterSheet = new SQLFilterSheet(objectTree, objectInfo);
			_app.getMainFrame().addInternalFrame(sqlFilterSheet, true, null);
			positionSheet(sqlFilterSheet);
		}
		else
		{
			moveToFront(sqlFilterSheet);
		}

		return sqlFilterSheet;
	}

	/**
	 * Get a EditWhereCols sheet for the passed session. If one already exists it
	 * will be brought to the front. If one doesn't exist it will be created.
	 *
	 * @param	tree		Object tree containing the table.
	 * @param	objectInfo	An instance of a class containing information about
	 * 						the database metadata.
	 *
	 * @return	The maintenance sheet for the passed session.
	 */
	public synchronized EditWhereColsSheet showEditWhereColsDialog(IObjectTreeAPI tree,
											IDatabaseObjectInfo objectInfo)
	{
		if (tree == null)
		{
			throw new IllegalArgumentException("IObjectTreeAPI == null");
		}
		if (objectInfo == null)
		{
			throw new IllegalArgumentException("IDatabaseObjectInfo == null");
		}

		ISession session = tree.getSession();
		EditWhereColsSheet editWhereColsSheet = getEditWhereColsSheet(session, objectInfo);
		if (editWhereColsSheet == null)
		{
//			 JASON: Needs to be done same as the others
			editWhereColsSheet = new EditWhereColsSheet(session, objectInfo);
//			Map map = getAllEditWhereColsSheets(tree);
//			map.put(objectInfo.getQualifiedName(), editWhereColsSheet);
			_app.getMainFrame().addInternalFrame(editWhereColsSheet, true, null);
//			editWhereColsSheet.addInternalFrameListener(_editWhereColsDialogListener);
			positionSheet(editWhereColsSheet);
		}
		else
		{
			moveToFront(editWhereColsSheet);
		}

		return editWhereColsSheet;
	}

	public void moveToFront(final Window win)
	{
		if (win != null)
		{
			GUIUtils.processOnSwingEventThread(new Runnable()
			{
				public void run()
				{
					win.toFront();
					win.setVisible(true);
				}
			});
		}
	}

	public void moveToFront(final JInternalFrame fr)
	{
		if (fr != null)
		{
			GUIUtils.processOnSwingEventThread(new Runnable()
			{
				public void run()
				{
					fr.moveToFront();
					fr.setVisible(true);
					try
					{
						fr.setSelected(true);
					}
					catch (PropertyVetoException ex)
					{
						s_log.error("Error bringing internal frame to the front", ex);
					}
				}
			});
		}
	}

	public void activateNextSessionWindow()
	{
		final SessionManager sessMgr = _app.getSessionManager();
		final ISession sess = sessMgr.getActiveSession();

		if (sess == null)
		{
         return;
		}

      BaseSessionInternalFrame activeSessionWindow = sess.getActiveSessionWindow();

      if(null == activeSessionWindow)
      {
         throw new IllegalStateException("Active Session with no active window ???");
      }

      BaseSessionInternalFrame nextSessionWindow = _sessionWindows.getNextSessionWindow(activeSessionWindow);

		if (false == activeSessionWindow.equals(nextSessionWindow))
		{
			new SelectInternalFrameCommand(nextSessionWindow).execute();
		}
	}

	public void activatePreviousSessionWindow()
	{
      final SessionManager sessMgr = _app.getSessionManager();
      final ISession sess = sessMgr.getActiveSession();

      if (sess == null)
      {
         return;
      }

      BaseSessionInternalFrame activeSessionWindow = sess.getActiveSessionWindow();

      if(null == activeSessionWindow)
      {
         throw new IllegalStateException("Active Session with no active window ???");
      }

      BaseSessionInternalFrame previousSessionWindow = _sessionWindows.getPreviousSessionWindow(activeSessionWindow);

      if (false == activeSessionWindow.equals(previousSessionWindow))
      {
         new SelectInternalFrameCommand(previousSessionWindow).execute();
      }
	}

	protected void refireSessionSheetOpened(InternalFrameEvent evt)
	{
		// Guaranteed to return a non-null array
		Object[] listeners = _listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == InternalFrameListener.class)
			{
				((InternalFrameListener)listeners[i + 1]).internalFrameOpened(evt);
			}
		}
	}

	protected void refireSessionSheetClosing(InternalFrameEvent evt)
	{
		// Guaranteed to return a non-null array
		Object[] listeners = _listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == InternalFrameListener.class)
			{
				((InternalFrameListener)listeners[i + 1]).internalFrameClosing(evt);
			}
		}
	}

	protected void refireSessionSheetClosed(InternalFrameEvent evt)
	{
		// Guaranteed to return a non-null array
		Object[] listeners = _listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == InternalFrameListener.class)
			{
				((InternalFrameListener)listeners[i + 1]).internalFrameClosed(evt);
			}
		}
	}

	protected void refireSessionSheetIconified(InternalFrameEvent evt)
	{
		// Guaranteed to return a non-null array
		Object[] listeners = _listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == InternalFrameListener.class)
			{
				((InternalFrameListener)listeners[i + 1]).internalFrameIconified(evt);
			}
		}
	}

	protected void refireSessionSheetDeiconified(InternalFrameEvent evt)
	{
		// Guaranteed to return a non-null array
		Object[] listeners = _listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == InternalFrameListener.class)
			{
				((InternalFrameListener)listeners[i + 1])
						.internalFrameDeiconified(evt);
			}
		}
	}

	protected void refireSessionSheetActivated(InternalFrameEvent evt)
	{
		// Guaranteed to return a non-null array
		Object[] listeners = _listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == InternalFrameListener.class)
			{
				((InternalFrameListener)listeners[i + 1]).internalFrameActivated(evt);
			}
		}
	}

	protected void refireSessionSheetDeactivated(InternalFrameEvent evt)
	{
		// Guaranteed to return a non-null array
		Object[] listeners = _listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == InternalFrameListener.class)
			{
				((InternalFrameListener)listeners[i + 1]).internalFrameDeactivated(evt);
			}
		}
	}

	private SessionPropertiesSheet getSessionPropertiesDialog(ISession session)
	{

      BaseSessionInternalFrame[] framesOfSession = _sessionWindows.getFramesOfSession(session.getIdentifier());

      for (int i = 0; i < framesOfSession.length; i++)
      {
         if (framesOfSession[i] instanceof SessionPropertiesSheet)
         {
            return (SessionPropertiesSheet)framesOfSession[i];
         }
      }
		return null;
	}

	private SQLFilterSheet getSQLFilterSheet(IObjectTreeAPI tree,
												IDatabaseObjectInfo objectInfo)
	{
		final ISession session = tree.getSession();

      BaseSessionInternalFrame[] framesOfSession = _sessionWindows.getFramesOfSession(session.getIdentifier());

      for (int i = 0; i < framesOfSession.length; i++)
      {
         if (framesOfSession[i] instanceof SQLFilterSheet)
         {
            final SQLFilterSheet sfs = (SQLFilterSheet)framesOfSession[i];
            if (sfs.getObjectTree() == tree &&
                  objectInfo.equals(sfs.getDatabaseObjectInfo()))
            {
               return sfs;
            }
         }
      }

		return null;
	}

	private EditWhereColsSheet getEditWhereColsSheet(ISession session,
											IDatabaseObjectInfo objectInfo)
	{
//		final Map map = getAllEditWhereColsSheets(tree);
//		return (EditWhereColsSheet)map.get(objectInfo.getQualifiedName());

      BaseSessionInternalFrame[] framesOfSession = _sessionWindows.getFramesOfSession(session.getIdentifier());

      for (int i = 0; i < framesOfSession.length; i++)
      {
         if (framesOfSession[i] instanceof EditWhereColsSheet)
         {
            final EditWhereColsSheet sfs = (EditWhereColsSheet)framesOfSession[i];
//					if (sfs.getObjectTree() == tree &&
//							objectInfo.equals(sfs.getDatabaseObjectInfo()))
            if (objectInfo.equals(sfs.getDatabaseObjectInfo()))
            {
               return sfs;
            }
         }
      }
		return null;
	}

	// JASON: FIX THIS
//	private Map getAllEditWhereColsSheets(IObjectTreeAPI tree)
//	{
//		Map map = (Map)_editWhereColsSheets.get(tree.getIdentifier());
//		if (map == null)
//		{
//			map = new HashMap();
//			_editWhereColsSheets.put(session.getIdentifier(), map);
//		}
//		return map;
//	}

	private void positionSheet(JInternalFrame jif)
	{
		GUIUtils.centerWithinDesktop(jif);
		moveToFront(jif);
	}

	private void selectFrontWindow()
	{
		final JDesktopPane desktop = _app.getMainFrame().getDesktopPane();
		if (desktop != null)
		{
			final JInternalFrame[] jifs = desktop.getAllFrames();
			if (jifs != null && jifs.length > 0)
			{
				moveToFront(jifs[0]);
			}
		}
	}

	private void initialize()
	{
		createAliasesListUI();
		createDriversListUI();
		preLoadActions();
		_app.getSessionManager().addSessionListener(_sessionListener);
		createMainFrame();
		setupFromPreferences();
	}

	private void createMainFrame()
	{
		_mainFrame = new MainFrame(_app);
	}

	private void createAliasesListUI()
	{
		final AliasesList al = new AliasesList(_app);

		final ActionCollection actions = _app.getActionCollection();
		actions.add(new ModifyAliasAction(_app, al));
		actions.add(new DeleteAliasAction(_app, al));
		actions.add(new CopyAliasAction(_app, al));
		actions.add(new ConnectToAliasAction(_app, al));
		actions.add(new CreateAliasAction(_app));

		_aliasesListWindow = new AliasesListInternalFrame(_app, al);
	}

	private void createDriversListUI()
	{
		final DriversList dl = new DriversList(_app);

		final ActionCollection actions = _app.getActionCollection();
		actions.add(new ModifyDriverAction(_app, dl));
		actions.add(new DeleteDriverAction(_app, dl));
		actions.add(new CopyDriverAction(_app, dl));
		actions.add(new CreateDriverAction(_app));

		_driversListWindow = new DriversListInternalFrame(_app, dl);
	}

	private void preLoadActions()
	{
		final ActionCollection actions = _app.getActionCollection();
		if (actions == null)
		{
			throw new IllegalStateException("ActionCollection hasn't been created.");
		}

		actions.add(new ViewAliasesAction(_app, getAliasesListInternalFrame()));
		actions.add(new ViewDriversAction(_app, getDriversListInternalFrame()));

//		IAliasesList al = getAliasesListInternalFrame().getAliasesList();
	}

	private void setupFromPreferences()
	{
		final SquirrelPreferences prefs = _app.getSquirrelPreferences();
		final MainFrameWindowState ws = prefs.getMainFrameWindowState();

		_mainFrame.addInternalFrame(_driversListWindow, false, null);
		WindowState toolWs = ws.getDriversWindowState();
		_driversListWindow.setBounds(toolWs.getBounds().createRectangle());
		_driversListWindow.setVisible(toolWs.isVisible());
		try
		{
			_driversListWindow.setSelected(true);
		}
		catch (PropertyVetoException ex)
		{
			s_log.error("Error selecting window", ex);
		}

		_mainFrame.addInternalFrame(_aliasesListWindow, false, null);
		toolWs = ws.getAliasesWindowState();
		_aliasesListWindow.setBounds(toolWs.getBounds().createRectangle());
		if (toolWs.isVisible())
		{
			_aliasesListWindow.setVisible(true);
			try
			{
				_aliasesListWindow.setSelected(true);
			}
			catch (PropertyVetoException ex)
			{
				s_log.error("Error selecting window", ex);
			}
		}
		else
		{
			_aliasesListWindow.setVisible(false);
		}
		prefs.setMainFrameWindowState(new MainFrameWindowState(this));
	}

	/**
	 * Retrieve an internal frame for the passed session. Can be <TT>null</TT>
	 *
	 * @return	an internal frame for the passed session. Can be <TT>null</TT>.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if ISession is passed as null.
	 */
	private JInternalFrame getInternalFrameForSession(ISession session)
	{
		if (session == null)
		{
			throw new IllegalArgumentException("ISession == null");
		}

		JInternalFrame firstWindow = null;

      BaseSessionInternalFrame[] framesOfSession = _sessionWindows.getFramesOfSession(session.getIdentifier());
      for (int i = 0; i < framesOfSession.length; i++)
      {
         if (framesOfSession[i] instanceof BaseSessionInternalFrame)
         {
            firstWindow = (BaseSessionInternalFrame)framesOfSession[i];
         }
         if (framesOfSession[i] instanceof SessionInternalFrame)
         {
            final SessionInternalFrame sif = (SessionInternalFrame)framesOfSession[i];
            if (sif.getSession().equals(session))
            {
               return sif;
            }
         }
      }
		return firstWindow;
	}

   public BaseSessionInternalFrame[] getAllFramesOfSession(IIdentifier sessionIdentifier)
   {
      return _sessionWindows.getFramesOfSession(sessionIdentifier);
   }

	// JASON: Needs to be done elsewhere
//	private synchronized void editWhereColsDialogClosed(EditWhereColsSheet sfs)
//	{
//		if (sfs != null)
//		{
//			sfs.removeInternalFrameListener(_editWhereColsDialogListener);
//			Map map = getAllEditWhereColsSheets(sfs.getSession());
//			String key = sfs.getDatabaseObjectInfo().getQualifiedName();
//			if (map.remove(key) == null)
//			{
//				s_log.error("Unable to find EditWhereColsSheet for " + key);
//			}
//		}
//	}

	// JASON: Do this elsewhere
//	private final class EditWhereColsDialogListener extends InternalFrameAdapter
//	{
//		public void internalFrameClosed(InternalFrameEvent evt)
//		{
//			EditWhereColsSheet sfs = (EditWhereColsSheet)evt.getInternalFrame();
//			WindowManager.this.editWhereColsDialogClosed(sfs);
//		}
//	}

	private final class SessionWindowListener implements InternalFrameListener
	{
		public void internalFrameOpened(InternalFrameEvent evt)
		{
			final JInternalFrame jif = evt.getInternalFrame();

			// JASON: Make menu smarter. When second window for the same
			// session is added create a hierarchical menu for all windows
			// for the session.

			// Add an item to the Windows menu for this window and
			// store the menu item back in the internal frame.
			final JMenu menu = getMainFrame().getWindowsMenu();
			final Action action = new SelectInternalFrameAction(jif);
			final JMenuItem menuItem = menu.add(action);
			jif.putClientProperty(MENU, menuItem);

			// Enable/Disable actions that require open session frames.
			JInternalFrame[] frames = GUIUtils.getOpenNonToolWindows(getMainFrame().getDesktopPane().getAllFrames());
			_app.getActionCollection().internalFrameOpenedOrClosed(frames.length);

			refireSessionSheetOpened(evt);
		}

		public void internalFrameClosing(InternalFrameEvent evt)
		{
			refireSessionSheetClosing(evt);
		}

		public void internalFrameClosed(InternalFrameEvent evt)
		{
			final JInternalFrame jif = evt.getInternalFrame();

			// Only remove the frame if the entire session is not closing
			if (!_sessionClosing)
			{
				// Find the internal Frame in the list of internal frames
				// and remove it.
				if (jif instanceof BaseSessionInternalFrame)
				{
					final BaseSessionInternalFrame sessionJIF = (BaseSessionInternalFrame)jif;
					final IIdentifier sessionID = sessionJIF.getSession().getIdentifier();
               BaseSessionInternalFrame[] sessionSheets = _sessionWindows.getFramesOfSession(sessionID);

               for (int i = 0; i < sessionSheets.length; i++)
               {
                  if (sessionSheets[i] == sessionJIF)
                  {
                     _sessionWindows.removeWindow(sessionSheets[i]);
                     WindowManager.this.selectFrontWindow();
                     break;
                  }
               }
				}
			}

			// Remove menu item from Windows menu that relates to this
			// internal frame.
			final JMenuItem menuItem = (JMenuItem)jif.getClientProperty(MENU);
			if (menuItem != null)
			{
				final JMenu menu = getMainFrame().getWindowsMenu();
				if (menu != null)
				{
					menu.remove(menuItem);
				}
			}

			// Enable/Disable actions that require open session frames.
			JInternalFrame[] frames = GUIUtils.getOpenNonToolWindows(getMainFrame().getDesktopPane().getAllFrames());
			_app.getActionCollection().internalFrameOpenedOrClosed(frames.length);

			refireSessionSheetClosed(evt);
		}

		public void internalFrameIconified(InternalFrameEvent e)
		{
			refireSessionSheetIconified(e);
		}

		public void internalFrameDeiconified(InternalFrameEvent e)
		{
			refireSessionSheetDeiconified(e);
		}

		public void internalFrameActivated(InternalFrameEvent e)
		{
			refireSessionSheetActivated(e);
		}

		public void internalFrameDeactivated(InternalFrameEvent e)
		{
			refireSessionSheetDeactivated(e);
		}
	}

	/**
	 * Used to update the UI depending on various session events.
	 */
	private final class SessionListener extends SessionAdapter
	{
		/**
		 * Session has been connected to a database.
		 */
		public void sessionConnected(SessionEvent evt)
		{
			// Add the message handler to the session
			evt.getSession().setMessageHandler(_app.getMessageHandler());
		}

		/**
		 * A session has been activated.
		 */
		public void sessionActivated(SessionEvent evt)
		{
			final ISession newSession = evt.getSession();

			// Allocate the current session to the actions.
			_app.getActionCollection().setCurrentSession(newSession);

			// If the active window isn't for the currently selected session
			// then select the main window for the session.
			ISession currSession = null;
			JInternalFrame sif = getMainFrame().getDesktopPane().getSelectedFrame();
			if (sif instanceof BaseSessionInternalFrame)
			{
				currSession = ((BaseSessionInternalFrame)sif).getSession();
			}
			if (currSession != newSession)
			{
				sif = getInternalFrameForSession(newSession);
				if (sif != null)
				{
					moveToFront(sif);
				}
			}

			// Make sure that the session menu is enabled.
			GUIUtils.processOnSwingEventThread(new Runnable()
			{
				public void run()
				{
					getMainFrame().getSessionMenu().setEnabled(true);
				}
			});
		}

		/**
		 * A session is being closed.
		 *
		 * @param	evt		Current event.
		 */
		public void sessionClosing(SessionEvent evt)
		{
			getMainFrame().getSessionMenu().setEnabled(false);

			// Clear session info from all actions.
			_app.getActionCollection().setCurrentSession(null);

			// Close all sheets for the session.
			_sessionClosing = true;
			IIdentifier sessionId = evt.getSession().getIdentifier();

         BaseSessionInternalFrame[] framesOfSession = _sessionWindows.getFramesOfSession(sessionId);
         for (int i = 0; i < framesOfSession.length; i++)
         {
            framesOfSession[i].dispose();
         }

			_sessionWindows.removeAllWindows(sessionId);

			selectFrontWindow();

			_sessionClosing = false;
		}
	}
}
