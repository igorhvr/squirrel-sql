package net.sourceforge.squirrel_sql.fw.sql;
/*
 * Copyright (C) 2001-2003 Colin Bell
 * colbell@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANT_Y; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
import java.beans.PropertyChangeListener;
import java.io.Serializable;

import net.sourceforge.squirrel_sql.fw.id.IIdentifier;
import net.sourceforge.squirrel_sql.fw.persist.ValidationException;
import net.sourceforge.squirrel_sql.fw.util.PropertyChangeReporter;
/**
 * This represents a Database alias which is a description of the means
 * required to connect to a JDBC complient database.<P>
 * This class is a <CODE>JavaBean</CODE>.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class SQLAlias implements Cloneable, Serializable, ISQLAlias, Comparable
{
	/**
	 * This interface defines locale specific strings. This should be
	 * replaced with a property file.
	 */
	private interface i18n
	{
		String ERR_BLANK_NAME = "Name cannot be blank.";
		String ERR_BLANK_DRIVER = "JDBC Driver cannot be blank.";
		String ERR_BLANK_URL = "JDBC URL cannot be blank.";
	}

	/** The <CODE>IIdentifier</CODE> that uniquely identifies this object. */
	private IIdentifier _id;

	/** The name of this alias. */
	private String _name;

	/**
	 * The <CODE>IIdentifier</CODE> that identifies the <CODE>ISQLDriver</CODE>
	 * that this <CODE>ISQLAlias</CODE> uses.
	 */
	private IIdentifier _driverId;

	/** The URL required to access the database. */
	private String _url;

	/** Name of user for connection. */
	private String _userName;

	/** Password of user for connection. */
	private String _password;

	/**
	 * true if the user wants to have his password saved unencrypted.
	 * This flag is necessary if there is an empty password (standard in MSSQL)
	 */
	private boolean _passwordSaved;

	/** <TT>true</TT> if this alias should be logged on automatically. */
	private boolean _autoLogon;

	/** Should this alias be connected when the application is started up. */
	private boolean _connectAtStartup;

	/** If <TT>true</TT> then use drver properties. */
	private boolean _useDriverProperties = false;

	/** Collection of <TT>SQLDriverProperty</TT> objects for this alias. */
	private SQLDriverPropertyCollection _driverProps = new SQLDriverPropertyCollection();

	/** Object to handle property change events. */
	private transient PropertyChangeReporter _propChgReporter;

	/**
	 * Default ctor.
	 */
	public SQLAlias()
	{
		super();
	}

	/**
	 * Ctor specifying the identifier.
	 *
	 * @param   id  Uniquely identifies this object.
	 */
	public SQLAlias(IIdentifier id)
	{
		super();
		_id = id;
		_name = "";
		_driverId = null;
		_url = "";
		_userName = "";
		_password = "";
	}

	/**
	 * TODO: Replace with clone().
	 * Assign data from the passed <CODE>ISQLAlias</CODE> to this one.
	 *
	 * @param   rhs	 <CODE>ISQLAlias</CODE> to copy data from.
	 *
	 * @exception	ValidationException
	 *				Thrown if an error occurs assigning data from
	 *				<CODE>rhs</CODE>.
	 */
	public synchronized void assignFrom(ISQLAlias rhs)
		throws ValidationException
	{
		setName(rhs.getName());
		setDriverIdentifier(rhs.getDriverIdentifier());
		setUrl(rhs.getUrl());
		setUserName(rhs.getUserName());
		setPassword(rhs.getPassword());
		setPasswordSaved(rhs.isPasswordSaved());
		setAutoLogon(rhs.isAutoLogon());
		setUseDriverProperties(rhs.getUseDriverProperties());
		setDriverProperties(rhs.getDriverProperties());
	}

	/**
	 * Returns <TT>true</TT> if this objects is equal to the passed one. Two
	 * <TT>ISQLAlias</TT> objects are considered equal if they have the same
	 * identifier.
	 */
	public boolean equals(Object rhs)
	{
		boolean rc = false;
		if (rhs != null && rhs.getClass().equals(getClass()))
		{
			rc = ((ISQLAlias)rhs).getIdentifier().equals(getIdentifier());
		}
		return rc;
	}

	/**
	 * Return a clone of this object.
	 */
	public Object clone()
	{
		try
		{
			final SQLAlias alias = (SQLAlias)super.clone();
			alias._propChgReporter = null;
//			alias.setDriverProperties(getDriverProperties());
			return alias;
		}
		catch (CloneNotSupportedException ex)
		{
			throw new InternalError(ex.getMessage()); // Impossible.
		}
	}

	/**
	 * Returns a hash code value for this object.
	 */
	public synchronized int hashCode()
	{
		return getIdentifier().hashCode();
	}

	/**
	 * Returns the name of this <TT>ISQLAlias</TT>.
	 */
	public String toString()
	{
		return getName();
	}

	/**
	 * Compare this <TT>SQLAlias</TT> to another object. If the passed object
	 * is a <TT>SQLAlias</TT>, then the <TT>getName()</TT> functions of the two
	 * <TT>SQLAlias</TT> objects are used to compare them. Otherwise, it throws a
	 * ClassCastException (as <TT>SQLAlias</TT> objects are comparable only to
	 * other <TT>SQLAlias</TT> objects).
	 */
	public int compareTo(Object rhs)
	{
		return _name.compareTo(((ISQLAlias)rhs).getName());
	}

	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		getPropertyChangeReporter().addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
		getPropertyChangeReporter().removePropertyChangeListener(listener);
	}

	/**
	 * Returns <CODE>true</CODE> if this object is valid.<P>
	 * Implementation for <CODE>IPersistable</CODE>.
	 */
	public synchronized boolean isValid()
	{
		return _name.length() > 0 && _driverId != null && _url.length() > 0;
	}

	public IIdentifier getIdentifier()
	{
		return _id;
	}

	public String getName()
	{
		return _name;
	}

	public IIdentifier getDriverIdentifier()
	{
		return _driverId;
	}

	public String getUrl()
	{
		return _url;
	}

	public String getUserName()
	{
		return _userName;
	}

	/**
	 * Retrieve the saved password.
	 * 
	 * @return		The saved password.
	 */
	public String getPassword()
	{
		return _password;
	}

	/**
	 * Has a password been saved in this alias?
	 * 
	 * @return	<TT>true</TT> if a password has been saved
	 * 			else <TT>false</TT>.
	 */
	public boolean isPasswordSaved()
	{
		return _passwordSaved;
	}

	/**
	 * Set the password for this alias.
	 * 
	 * @param	password	The new password.
	 */
	public void setPassword(String password)
	{
		String data = getString(password);
		if (_password != data)
		{
			final String oldValue = _password;
			_password = data;
			getPropertyChangeReporter().firePropertyChange(ISQLAlias.IPropertyNames.PASSWORD,
												oldValue, _password);
		}
	}

	/**
	 * Set whether a password has been saved for his alias.
	 * 
	 * @param	passwordSaved	<TT>true</TT> if password saved else <TT>false</TT>.
	 */
	public void setPasswordSaved(boolean passwordSaved)
	{
		if (_passwordSaved != passwordSaved)
		{
			_passwordSaved = passwordSaved;
			getPropertyChangeReporter().firePropertyChange(ISQLAlias.IPropertyNames.PASSWORD_SAVED,
												!_passwordSaved, _passwordSaved);
		}
	}

	/**
	 * Should this alias be logged on automatically.
	 * 
	 * @return	<TT>true</TT> is this alias should be logged on automatically
	 * 			else <TT>false</TT>.
	 */
	public boolean isAutoLogon()
	{
		return _autoLogon;
	}

	/**
	 * Set whether this alias should be logged on automatically.
	 * 
	 * @param	value	<TT>true</TT> if alias should be autologged on
	 * 					else <TT>false</TT.
	 */
	public void setAutoLogon(boolean value)
	{
		if (_autoLogon != value)
		{
			_autoLogon = value;
			getPropertyChangeReporter().firePropertyChange(ISQLAlias.IPropertyNames.AUTO_LOGON,
												!_autoLogon, _autoLogon);
		}
	}

	/**
	 * Should this alias be connected when the application is started up.
	 * 
	 * @return	<TT>true</TT> if this alias should be connected when the
	 *			application is started up.
	 */
	public boolean isConnectAtStartup()
	{
		return _connectAtStartup;
	}

	/**
	 * Set whether alias should be connected when the application is started up.
	 * 
	 * @param	value	<TT>true</TT> if alias should be connected when the
	 *					application is started up.
	 */
	public void setConnectAtStartup(boolean value)
	{
		if (_connectAtStartup != value)
		{
			_connectAtStartup = value;
			getPropertyChangeReporter().firePropertyChange(ISQLAlias.IPropertyNames.CONNECT_AT_STARTUP,
												!_connectAtStartup, _connectAtStartup);
		}
	}

	/**
	 * Returns whether this alias uses driver properties.
	 */
	public boolean getUseDriverProperties()
	{
		return _useDriverProperties;
	}

	public void setIdentifier(IIdentifier id)
	{
		_id = id;
	}

	public void setName(String name) throws ValidationException
	{
		String data = getString(name);
		if (data.length() == 0)
		{
			throw new ValidationException(i18n.ERR_BLANK_NAME);
		}
		if (_name != data)
		{
			final String oldValue = _name;
			_name = data;
			getPropertyChangeReporter().firePropertyChange(ISQLAlias.IPropertyNames.NAME,
												oldValue, _name);
		}
	}

	public void setDriverIdentifier(IIdentifier data)
		throws ValidationException
	{
		if (data == null)
		{
			throw new ValidationException(i18n.ERR_BLANK_DRIVER);
		}
		if (_driverId != data)
		{
			final IIdentifier oldValue = _driverId;
			_driverId = data;
			getPropertyChangeReporter().firePropertyChange(ISQLAlias.IPropertyNames.DRIVER,
												oldValue, _driverId);
		}
	}

	public void setUrl(String url) throws ValidationException
	{
		String data = getString(url);
		if (data.length() == 0)
		{
			throw new ValidationException(i18n.ERR_BLANK_URL);
		}
		if (_url != data)
		{
			final String oldValue = _url;
			_url = data;
			getPropertyChangeReporter().firePropertyChange(ISQLAlias.IPropertyNames.URL,
													oldValue, _url);
		}
	}

	public void setUserName(String userName) throws ValidationException
	{
		String data = getString(userName);
		if (_userName != data)
		{
			final String oldValue = _userName;
			_userName = data;
			getPropertyChangeReporter().firePropertyChange(ISQLAlias.IPropertyNames.USER_NAME,
												oldValue, _userName);
		}
	}

	public void setUseDriverProperties(boolean value) throws ValidationException
	{
		if (_useDriverProperties != value)
		{
			final boolean oldValue = _useDriverProperties;
			_useDriverProperties = value;
			getPropertyChangeReporter().firePropertyChange(ISQLAlias.IPropertyNames.USE_DRIVER_PROPERTIES,
												oldValue, _useDriverProperties);
		}
	}

	/**
	 * Retrieve the SQL driver properties.
	 *
	 * @return	the SQL driver properties.
	 */
	public synchronized SQLDriverPropertyCollection getDriverProperties()
	{
		return _driverProps;
	}

	public void setDriverProperties(SQLDriverPropertyCollection value)
	{
		_driverProps = value != null ? value : new SQLDriverPropertyCollection();
	}

	private synchronized PropertyChangeReporter getPropertyChangeReporter()
	{
		if (_propChgReporter == null)
		{
			_propChgReporter = new PropertyChangeReporter(this);
		}
		return _propChgReporter;
	}

	private String getString(String data)
	{
		return data != null ? data.trim() : "";
	}
}
