package net.sourceforge.squirrel_sql.fw.sql;
/*
 * Copyright (C) 2001-2004 Colin Bell
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
import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;

import net.sourceforge.squirrel_sql.fw.util.PropertyChangeReporter;
import net.sourceforge.squirrel_sql.fw.util.StringUtilities;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
/**
 * This represents a connection to an SQL server. it is basically a wrapper
 * around <TT>java.sql.Connection</TT>.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class SQLConnection
{
	public interface IPropertyNames
	{
		String AUTO_COMMIT = "autocommit";
		String CATALOG = "catalog";
	}

	/** Logger for this class. */
	private final static ILogger s_log =
		LoggerController.createLogger(SQLConnection.class);

	/** The <TT>java.sql.Connection</TT> this object is wrapped around. */
	private Connection _conn;

	/** Connectiopn properties specified when connection was opened. */
	private final SQLDriverPropertyCollection _connProps;

	/** MetaData for this connection. */
	private SQLDatabaseMetaData _metaData;

	private boolean _autoCommitOnClose = false;

	private Date _timeOpened;
	private Date _timeClosed;

	/** Object to handle property change events. */
	private transient PropertyChangeReporter _propChgReporter;

	public SQLConnection(Connection conn, SQLDriverPropertyCollection connProps)
	{
		super();
		if (conn == null)
		{
			throw new IllegalArgumentException("SQLConnection == null");
		}
		_conn = conn;
		_connProps = connProps;
		_timeOpened = Calendar.getInstance().getTime();
	}

	public void close() throws SQLException
	{
		SQLException savedEx = null;
		if (_conn != null)
		{
			s_log.debug("Closing connection");
			try
			{
				if (!_conn.getAutoCommit())
				{
					if (_autoCommitOnClose)
					{
						_conn.commit();
					}
					else
					{
						_conn.rollback();
					}
				}
			}
			catch (SQLException ex)
			{
				savedEx = ex;
			}
			_conn.close();
			_conn = null;
			_timeClosed = Calendar.getInstance().getTime();
			if (savedEx != null)
			{
				s_log.debug("Connection close failed", savedEx);
				throw savedEx;
			}
			s_log.debug("Connection closed successfully");
		}
	}

	public void commit() throws SQLException
	{
		validateConnection();
		_conn.commit();
	}

	public void rollback() throws SQLException
	{
		validateConnection();
		_conn.rollback();
	}

	/**
	 * Retrieve the properties specified when connection was opened. This can
	 * be <TT>null</TT>.
	 * 
	 * @return	Connection properties.
	 */
	public SQLDriverPropertyCollection getConnectionProperties()
	{
		return _connProps;
	}

	public boolean getAutoCommit() throws SQLException
	{
		validateConnection();
		return _conn.getAutoCommit();
	}

	public void setAutoCommit(boolean value) throws SQLException
	{
		validateConnection();
		final Connection conn = getConnection();
		final boolean oldValue = conn.getAutoCommit();
		if (oldValue != value)
		{
			_conn.setAutoCommit(value);
			getPropertyChangeReporter().firePropertyChange(IPropertyNames.AUTO_COMMIT,
												oldValue, value);
		}
	}

	public boolean getCommitOnClose()
	{
		return _autoCommitOnClose;
	}

	public int getTransactionIsolation()
		throws SQLException
	{
		validateConnection();
		return _conn.getTransactionIsolation();
	}

	public void setTransactionIsolation(int value)
		throws SQLException
	{
		validateConnection();
		_conn.setTransactionIsolation(value);
	}

	public void setCommitOnClose(boolean value)
	{
		_autoCommitOnClose = value;
	}

	public Statement createStatement() throws SQLException
	{
		validateConnection();
		return _conn.createStatement();
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException
	{
		validateConnection();
		return _conn.prepareStatement(sql);
	}

	/**
	 * Retrieve the time that this connection was opened. Note that this time
	 * is the time that this <TT>SQLConnection</TT> was created, not the time
	 * that the <TT>java.sql.Connection</TT> object that it is wrapped around
	 * was opened.
	 * 
	 * @return	Time connection opened.
	 */
	public Date getTimeOpened()
	{
		return _timeOpened;
	}

	/**
	 * Retrieve the time that this connection was closed. If this connection
	 * is still opened then <TT>null</TT> will be returned..
	 * 
	 * @return	Time connection closed.
	 */
	public Date getTimeClosed()
	{
		return _timeClosed;
	}
	
	/**
	 * Retrieve the metadata for this connection.
	 * 
	 * @return	The <TT>SQLMetaData</TT> object.
	 */
	public synchronized SQLDatabaseMetaData getSQLMetaData()
	{
		if (_metaData == null)
		{
			_metaData = new SQLDatabaseMetaData(this);
		}
		return _metaData;
	}

	public Connection getConnection()
	{
		return _conn;
	}

	public String getCatalog() throws SQLException
	{
		validateConnection();
		return getConnection().getCatalog();
	}

	public void setCatalog(String catalogName)
		throws SQLException
	{
		validateConnection();
		final Connection conn = getConnection();
		final String oldValue = conn.getCatalog();
		if (!StringUtilities.areStringsEqual(oldValue, catalogName))
		{
			try
			{
				conn.setCatalog(catalogName);
			}
			catch (SQLException ex)
			{
				// MS SQL Server throws an exception if the catalog name
				// contains a period without it being quoted.
				conn.setCatalog(quote(catalogName));
			}
			getPropertyChangeReporter().firePropertyChange(IPropertyNames.CATALOG,
												oldValue, catalogName);
		}
	}

	public SQLWarning getWarnings() throws SQLException
	{
		validateConnection();
		return _conn.getWarnings();
	}

	/**
	 * Add a listener for property change events.
	 *
	 * @param	lis		The new listener.
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		if (listener != null)
		{
			getPropertyChangeReporter().addPropertyChangeListener(listener);
		}
		else
		{
			s_log.debug("Attempted to add a null PropertyChangeListener");
		}
	}

	/**
	 * Remove a property change listener.
	 *
	 * @param	lis		The listener to be removed.
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
		if (listener != null)
		{
			getPropertyChangeReporter().removePropertyChangeListener(listener);
		}
		else
		{
			s_log.debug("Attempted to remove a null PropertyChangeListener");
		}
	}

	protected void validateConnection() throws SQLException
	{
		if (_conn == null)
		{
			throw new SQLException("No connection");
		}
	}

	/**
	 * Retrieve the object that reports on property change events. If it
	 * doesn't exist then create it.
	 *
	 * @return	PropertyChangeReporter object.
	 */
	private synchronized PropertyChangeReporter getPropertyChangeReporter()
	{
		if (_propChgReporter == null)
		{
			_propChgReporter = new PropertyChangeReporter(this); 
		}
		return _propChgReporter;
	}
	
	private String quote(String str)
	{
		String identifierQuoteString = "";
		try
		{
			identifierQuoteString = getSQLMetaData().getIdentifierQuoteString();
		}
		catch (SQLException ex)
		{
			s_log.debug(
				"DBMS doesn't supportDatabasemetaData.getIdentifierQuoteString",
				ex);
		}
		if (identifierQuoteString != null
				&& !identifierQuoteString.equals(" "))
		{
			return identifierQuoteString + str + identifierQuoteString;
		}
		return str;
	}
}
