package net.sourceforge.squirrel_sql.client.session;
/*
 * Copyright (C) 2004 Colin Bell
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
import net.sourceforge.squirrel_sql.client.session.event.ISessionListener;
import net.sourceforge.squirrel_sql.client.session.event.SessionEvent;
import net.sourceforge.squirrel_sql.fw.gui.Dialogs;
import net.sourceforge.squirrel_sql.fw.id.IIdentifier;
import net.sourceforge.squirrel_sql.fw.id.IntegerIdentifierFactory;
import net.sourceforge.squirrel_sql.fw.sql.ISQLAlias;
import net.sourceforge.squirrel_sql.fw.sql.ISQLDriver;
import net.sourceforge.squirrel_sql.fw.sql.SQLConnection;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import javax.swing.event.EventListenerList;
import javax.swing.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
/**
 * This class manages sessions.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class SessionManager
{
	/** Logger for this class. */
	private static final ILogger s_log =
		LoggerController.createLogger(SessionManager.class);

	/** Internationalized strings for this class. */
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(SessionManager.class);

	/** Application API. */
	private final IApplication _app;

	private ISession _activeSession;

	/** Linked list of sessions. */
	private final LinkedList _sessionsList = new LinkedList();

	/** Map of sessions keyed by session ID. */
	private final Map _sessionsById = new HashMap();

	private EventListenerList listenerList = new EventListenerList();

	/** Factory used to generate session IDs. */
	private final IntegerIdentifierFactory _idFactory = new IntegerIdentifierFactory(1);

	/**
	 * Ctor.
	 *
	 * @param	app		Application API.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>IApplication</TT> passed.
	 */
	public SessionManager(IApplication app)
	{
		super();
		if (app == null)
		{
			throw new IllegalArgumentException("IApplication == null");
		}

		_app = app;
	}

	/**
	 * Create a new session.
	 *
	 * @param	app			Application API.
	 * @param	driver		JDBC driver for session.
	 * @param	alias		Defines URL to database.
	 * @param	conn		Connection to database.
	 * @param	user		User name connected with.
	 * @param	password	Password for <TT>user</TT>
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if IApplication, ISQLDriver, ISQLAlias,
	 * 			or SQLConnection is passed as null.
	 */
	public synchronized ISession createSession(IApplication app,
									ISQLDriver driver, ISQLAlias alias,
									SQLConnection conn, String user,
									String password)
	{
		if (app == null)
		{
			throw new IllegalArgumentException("null IApplication passed");
		}
		if (driver == null)
		{
			throw new IllegalArgumentException("null ISQLDriver passed");
		}
		if (alias == null)
		{
			throw new IllegalArgumentException("null ISQLAlias passed");
		}
		if (conn == null)
		{
			throw new IllegalArgumentException("null SQLConnection passed");
		}

		final Session sess = new Session(app, driver, alias, conn, user,
										password, _idFactory.createIdentifier());
		_sessionsList.addLast(sess);
		_sessionsById.put(sess.getIdentifier(), sess);

		fireSessionAdded(sess);
		setActiveSession(sess);

		return sess;
	}

	public void setActiveSession(ISession session)
	{
		if (session != _activeSession)
		{
			_activeSession = session;
			fireSessionActivated(session);
		}
	}

	/**
	 * Retrieve an array of all the sessions currently connected.
	 *
	 * @return array of all connected sessions.
	 */
	public synchronized ISession[] getConnectedSessions()
	{
		final ISession[] ar = new ISession[_sessionsList.size()];
		return (ISession[])_sessionsList.toArray(ar);
	}

	/**
	 * Retrieve the session that is currently activated within the
	 * session manager. Any new sql worksheets etc will be created
	 * against this session
	 */
	public synchronized ISession getActiveSession()
	{
		return _activeSession;
	}

	/**
	 * Get the next session opened after the passed one.
	 *
	 * @return	The next session or the first one if the passed one is
	 * 			the last session.
	 */
	public synchronized ISession getNextSession(ISession session)
	{
		final int sessionCount = _sessionsList.size();
		int idx = _sessionsList.indexOf(session);
		if (idx != -1)
		{
			++idx;
			if (idx >= sessionCount)
			{
				idx = 0;
			}
			return (ISession)_sessionsList.get(idx);
		}

		s_log.error("SessionManager.getNextSession()-> Session " +
					session.getIdentifier() + " not found in _sessionsList");
		if (sessionCount > 0)
		{
			s_log.error("SessionManager.getNextSession()-> Returning first session");
			return (ISession)_sessionsList.getFirst();
		}
		s_log.error("SessionManager.getNextSession()-> List empty so returning passed session");
		return session;
	}

	/**
	 * Get the next session opened before the passed one.
	 *
	 * @return	The previous session or the last one if the passed one is
	 * 			the first session.
	 */
	public synchronized ISession getPreviousSession(ISession session)
	{
		final int sessionCount = _sessionsList.size();
		int idx = _sessionsList.indexOf(session);
		if (idx != -1)
		{
			--idx;
			if (idx < 0)
			{
				idx = sessionCount - 1;
			}
			return (ISession)_sessionsList.get(idx);
		}

		s_log.error("SessionManager.getPreviousSession()-> Session " +
					session.getIdentifier() + " not found in _sessionsList");
		if (sessionCount > 0)
		{
			s_log.error("SessionManager.getPreviousSession()-> Returning last session");
			return (ISession)_sessionsList.getLast();
		}
		s_log.error("SessionManager.getPreviousSession()-> List empty so returning passed session");
		return session;
	}

	/**
	 * Retrieve the session for the passed identifier.
	 *
	 * @param	sessionID	ID of session we are trying to retrieve.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>IIdentifier</TT> passed.
	 */
	public ISession getSession(IIdentifier sessionID)
	{
		return (ISession)_sessionsById.get(sessionID);
	}

	/**
	 * Close a session.
	 *
	 * @param	session		Session to close.
	 *
	 * @return	<tt>true</tt> if session was closed else <tt>false</tt>.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if <TT>null</TT>ISession passed.
	 */
	public synchronized boolean closeSession(ISession session)
	{
		if (session == null)
		{
			throw new IllegalArgumentException("ISession == null");
		}

		try
		{
			if (confirmClose(session))
			{
				// TODO: Should have session listeners instead of these calls.
				session.getApplication().getPluginManager().sessionEnding(session);

				fireSessionClosing(session);
				session.close();
				fireSessionClosed(session);

				final IIdentifier sessionId = session.getIdentifier();
				if (!_sessionsList.remove(session))
				{
					s_log.error("SessionManager.closeSession()-> Session " +
							sessionId +
							" not found in _sessionsList when trying to remove it.");
				}
				if (_sessionsById.remove(sessionId) == null)
				{
					s_log.error("SessionManager.closeSession()-> Session " +
							sessionId +
							" not found in _sessionsById when trying to remove it.");
				}

				if (_sessionsList.isEmpty())
				{
					fireAllSessionsClosed();
				}

				// Activate another session since the current
				// active session has closed.
				if (session == _activeSession)
				{
					// JASON: This isn't right? Next/last?
		//			ISession nextSession = null;
					if (!_sessionsList.isEmpty())
					{
						setActiveSession((ISession)_sessionsList.getLast());
					}
					else
					{
						_activeSession = null;
					}
				}

				return true;
			}
		}
		catch (Throwable ex)
		{
			s_log.error(ex);
			session.getMessageHandler().showErrorMessage(s_stringMgr.getString("SessionManager.ErrorClosingSession", ex));
		}

		return false;
	}

	/**
	 * Closes all currently open sessions.
	 *
	 * @return	<tt>true</tt> if all sessions closed else <tt>false</tt>.
	 *
	 * @throws	SQLException
	 * 			Thrown if an error closing the SQL connection. The session
	 * 			will still be closed even though the connection may not have
	 *			been.
	 */
	synchronized public boolean closeAllSessions()
	{
		// Get an array since we dont want trouble with the sessionsList when
		// we remove the sessions from it.
		final ISession[] sessions = getConnectedSessions();
		for (int i = sessions.length - 1; i >= 0; i--)
		{
			if (!closeSession(sessions[i]))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Adds a session listener
	 *
	 * @param	lis		The listener to add.
	 */
	public void addSessionListener(ISessionListener lis)
	{
		if (lis != null)
		{
			listenerList.add(ISessionListener.class, lis);
		}
		else
		{
			s_log.error("Attempted to add null listener: SessionManager.addSessionListener");
		}
	}

	/**
	 * Removes a session listener
	 *
	 * @param	lis		The listener to remove.
	 */
	public void removeSessionListener(ISessionListener lis)
	{
		if (lis != null)
		{
			listenerList.remove(ISessionListener.class, lis);
		}
		else
		{
			s_log.error("Attempted to remove null listener: SessionManager.addSessionListener");
		}
	}

	/**
	 * Fired when a session is connected (added) to the session
	 * manager
	 */
	protected void fireSessionAdded(ISession session)
	{
		Object[] listeners = listenerList.getListenerList();
		SessionEvent evt = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == ISessionListener.class)
			{
				// Lazily create the event:
				if (evt == null)
					evt = new SessionEvent(session);
				((ISessionListener)listeners[i + 1]).sessionConnected(evt);
			}
		}
	}

	/**
	 * Fired when a session is closed (removed) from the session manager
	 */
	protected void fireSessionClosed(ISession session)
	{
		Object[] listeners = listenerList.getListenerList();
		SessionEvent evt = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == ISessionListener.class)
			{
				// Lazily create the event:
				if (evt == null)
					evt = new SessionEvent(session);
				((ISessionListener)listeners[i + 1]).sessionClosed(evt);
			}
		}
	}

	/**
	 * Fired when a session is about to close from the session manager
	 */
	protected void fireSessionClosing(ISession session)
	{
		Object[] listeners = listenerList.getListenerList();
		SessionEvent evt = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == ISessionListener.class)
			{
				// Lazily create the event:
				if (evt == null)
				{
					evt = new SessionEvent(session);
				}
				((ISessionListener)listeners[i + 1]).sessionClosing(evt);
			}
		}
	}

	/**
	 * Fired when all the session have been closed (removed) from the
	 * session manager
	 */
	protected void fireAllSessionsClosed()
	{
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == ISessionListener.class)
			{
				((ISessionListener)listeners[i + 1]).allSessionsClosed();
			}
		}
	}

	/**
	 * Fired when the active session changed
	 */
	protected void fireSessionActivated(ISession session)
	{
		Object[] listeners = listenerList.getListenerList();
		SessionEvent evt = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == ISessionListener.class)
			{
				// Lazily create the event:
				if (evt == null)
					evt = new SessionEvent(session);
				((ISessionListener)listeners[i + 1]).sessionActivated(evt);
			}
		}
	}

	/**
	 * Confirm whether session is to be closed.
	 *
	 * @param	session		Session being closed.
	 *
	 * @return	<tt>true</tt> if confirmed to close session.
	 */
	private boolean confirmClose(ISession session)
	{
		if (!_app.getSquirrelPreferences().getConfirmSessionClose())
		{
            return session.confirmClose();
        }

		final String msg = s_stringMgr.getString("SessionManager.confirmClose",
							session.getTitle());
		if (!Dialogs.showYesNo(_app.getMainFrame(), msg)) {
            return session.confirmClose();
        } else {
            return true;
        }
	}

	protected void fireConnectionClosedForReconnect(Session session)
	{
		Object[] listeners = listenerList.getListenerList();
		SessionEvent evt = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == ISessionListener.class)
			{
				// Lazily create the event:
				if (evt == null)
					evt = new SessionEvent(session);
				((ISessionListener)listeners[i + 1]).connectionClosedForReconnect(evt);
			}
		}
	}

	protected void fireReconnected(Session session)
	{
		Object[] listeners = listenerList.getListenerList();
		SessionEvent evt = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == ISessionListener.class)
			{
				// Lazily create the event:
				if (evt == null)
					evt = new SessionEvent(session);
				((ISessionListener)listeners[i + 1]).reconnected(evt);
			}
		}
	}

	protected void fireReconnectFailed(Session session)
	{
		Object[] listeners = listenerList.getListenerList();
		SessionEvent evt = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2)
		{
			if (listeners[i] == ISessionListener.class)
			{
				// Lazily create the event:
				if (evt == null)
					evt = new SessionEvent(session);
				((ISessionListener)listeners[i + 1]).reconnectFailed(evt);
			}
		}
	}


	protected void fireSessionFinalized(final IIdentifier sessionIdentifier)
	{
		// invokeLater to make the call synchronto the event queue 
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				Object[] listeners = listenerList.getListenerList();
				for (int i = listeners.length - 2; i >= 0; i -= 2)
				{
					if (listeners[i] == ISessionListener.class)
					{
						((ISessionListener)listeners[i + 1]).sessionFinalized(sessionIdentifier);
					}
				}
			}
		});

	}
}
