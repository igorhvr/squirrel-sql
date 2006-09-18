package net.sourceforge.squirrel_sql.fw.sql;
/*
 * Copyright (C) 2001-2002 Colin Bell
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.squirrel_sql.fw.id.IIdentifier;
import net.sourceforge.squirrel_sql.fw.persist.ValidationException;
import net.sourceforge.squirrel_sql.fw.util.PropertyChangeReporter;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.beanwrapper.StringWrapper;
/**
 * This represents a JDBC driver.
 * This class is a <CODE>JavaBean</CODE>.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class SQLDriver implements ISQLDriver, Cloneable, Serializable
{
	/** Internationalized strings for this class. */
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(SQLDriver.class);

	private interface IStrings
	{
		String ERR_BLANK_NAME = s_stringMgr.getString("SQLDriver.error.blankname");
		String ERR_BLANK_DRIVER = s_stringMgr.getString("SQLDriver.error.blankdriver");
		String ERR_BLANK_URL = s_stringMgr.getString("SQLDriver.error.blankurl");
	}

	/** The <CODE>IIdentifier</CODE> that uniquely identifies this object. */
	private IIdentifier _id;

	/** The name of this driver. */
	private String _name;

	/**
	 * File name associated with <CODE>_jarFileURL</CODE>.
	 */
	private String _jarFileName = null;

	/** Names for driver jar files. */
	private List _jarFileNamesList = new ArrayList();

	/** The class name of the JDBC driver. */
	private String _driverClassName;

	/** Default URL required to access the database. */
	private String _url;

	/** Is the JDBC driver class for this object loaded? */
	private boolean _jdbcDriverClassLoaded;

	/** Object to handle property change events. */
	private transient PropertyChangeReporter _propChgReporter;

    /** Default Website URL for more info about the JDBC driver */
    private String _websiteUrl;
    
    /** 
     * Default statement separator.  If the database server has a command line
     * utility which can be used to execute scripts, then this would be the 
     * character or character sequence that is accepted by the utility to 
     * separate different SQL statements from each other. 
     */
    private String _statementSeparator;
    
	/**
	 * Ctor specifying the identifier.
	 *
	 * @param	id	Uniquely identifies this object.
	 */
	public SQLDriver(IIdentifier id)
	{
		super();
		_id = id;
		_name = "";
		_jarFileName = null;
		_driverClassName = null;
		_url = "";
        _websiteUrl = "";
        _statementSeparator = "";
	}

	/**
	 * Default ctor.
	 */
	public SQLDriver()
	{
		super();
	}

	/**
	 * Assign data from the passed <CODE>ISQLDriver</CODE> to this one. This
	 * does <B>not</B> copy the identifier.
	 *
	 * @param	rhs	 <CODE>ISQLDriver</CODE> to copy data from.
	 *
	 * @exception	ValidationException
	 *				Thrown if an error occurs assigning data from
	 *				<CODE>rhs</CODE>.
	 */
	public synchronized void assignFrom(ISQLDriver rhs)
		throws ValidationException
	{
		setName(rhs.getName());
		setJarFileNames(rhs.getJarFileNames());
		setDriverClassName(rhs.getDriverClassName());
		setUrl(rhs.getUrl());
		setJDBCDriverClassLoaded(rhs.isJDBCDriverClassLoaded());
        setWebSiteUrl(rhs.getWebSiteUrl());
        setStatementSeparator(rhs.getStatementSeparator());
	}

	/**
	 * Returns <TT>true</TT> if this objects is equal to the passed one. Two
	 * <TT>ISQLDriver</TT> objects are considered equal if they have the same
	 * identifier.
	 */
	public boolean equals(Object rhs)
	{
		boolean rc = false;
		if (rhs != null && rhs.getClass().equals(getClass()))
		{
			rc = ((ISQLDriver) rhs).getIdentifier().equals(getIdentifier());
		}
		return rc;
	}

	/**
	 * Returns a hash code value for this object.
	 */
	public synchronized int hashCode()
	{
		return getIdentifier().hashCode();
	}

	/**
	 * Returns the name of this <TT>ISQLDriver</TT>.
	 */
	public String toString()
	{
		return getName();
	}

	/**
	 * Return a clone of this object.
	 */
	public Object clone()
	{
		try
		{
			final SQLDriver driver = (SQLDriver)super.clone();
			driver._propChgReporter = null;
			return driver;
		}
		catch (CloneNotSupportedException ex)
		{
			throw new InternalError(ex.getMessage()); // Impossible.
		}
	}

	/**
	 * Compare this <TT>ISQLDriver</TT> to another object. If the passed object
	 * is a <TT>ISQLDriver</TT>, then the <TT>getName()</TT> functions of the two
	 * <TT>ISQLDriver</TT> objects are used to compare them. Otherwise, it throws a
	 * ClassCastException (as <TT>ISQLDriver</TT> objects are comparable only to
	 * other <TT>ISQLDriver</TT> objects).
	 */
	public int compareTo(Object rhs)
	{
		return _name.compareTo(((ISQLDriver)rhs).getName());
	}

	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		getPropertyChangeReporter().addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
		getPropertyChangeReporter().removePropertyChangeListener(listener);
	}

	public void setReportPropertyChanges(boolean report)
	{
		getPropertyChangeReporter().setNotify(report);
	}

	public IIdentifier getIdentifier()
	{
		return _id;
	}

	public void setIdentifier(IIdentifier id)
	{
		_id = id;
	}

	public String getDriverClassName()
	{
		return _driverClassName;
	}

	public void setDriverClassName(String driverClassName)
		throws ValidationException
	{
		String data = getString(driverClassName);
		if (data.length() == 0)
		{
			throw new ValidationException(IStrings.ERR_BLANK_DRIVER);
		}
		if (_driverClassName != data)
		{
			final String oldValue = _driverClassName;
			_driverClassName = data;
			getPropertyChangeReporter().firePropertyChange(
				ISQLDriver.IPropertyNames.DRIVER_CLASS,
				oldValue,
				_driverClassName);
		}
	}

	/**
	 * @deprecated	Replaced by getJarFileNames().
	 */
	public String getJarFileName()
	{
		return _jarFileName;
	}

	public void setJarFileName(String value)
	{
		if (value == null)
		{
			value = "";
		}
		if (_jarFileName == null || !_jarFileName.equals(value))
		{
			final String oldValue = _jarFileName;
			_jarFileName = value;
			getPropertyChangeReporter().firePropertyChange(
				ISQLDriver.IPropertyNames.JARFILE_NAME,
				oldValue,
				_jarFileName);
		}
	}

	public synchronized String[] getJarFileNames()
	{
		return (String[]) _jarFileNamesList.toArray(
			new String[_jarFileNamesList.size()]);
	}

	public synchronized void setJarFileNames(String[] values)
	{
		String[] oldValue =
			(String[]) _jarFileNamesList.toArray(
				new String[_jarFileNamesList.size()]);
		_jarFileNamesList.clear();

		if (values == null)
		{
			values = new String[0];
		}

		for (int i = 0; i < values.length; ++i)
		{
			_jarFileNamesList.add(values[i]);
		}

		getPropertyChangeReporter().firePropertyChange(
			ISQLDriver.IPropertyNames.JARFILE_NAMES,
			oldValue,
			values);
	}
	public String getUrl()
	{
		return _url;
	}

	public void setUrl(String url) throws ValidationException
	{
		String data = getString(url);
		if (data.length() == 0)
		{
			throw new ValidationException(IStrings.ERR_BLANK_URL);
		}
		if (_url != data)
		{
			final String oldValue = _url;
			_url = data;
			getPropertyChangeReporter().firePropertyChange(
				ISQLDriver.IPropertyNames.URL,
				oldValue,
				_url);
		}
	}

	public String getName()
	{
		return _name;
	}

	public void setName(String name) throws ValidationException
	{
		String data = getString(name);
		if (data.length() == 0)
		{
			throw new ValidationException(IStrings.ERR_BLANK_NAME);
		}
		if (_name != data)
		{
			final String oldValue = _name;
			_name = data;
			getPropertyChangeReporter().firePropertyChange(
				ISQLDriver.IPropertyNames.NAME,
				oldValue,
				_name);
		}
	}

	public boolean isJDBCDriverClassLoaded()
	{
		return _jdbcDriverClassLoaded;
	}

	public void setJDBCDriverClassLoaded(boolean cl)
	{
		_jdbcDriverClassLoaded = cl;
		//TODO: Decide whether this should be a bound property or not.
		//		getPropertyChangeReporter().firePropertyChange(ISQLDriver.IPropertyNames.NAME, _name, _name);
	}

	public synchronized StringWrapper[] getJarFileNameWrappers()
	{
		StringWrapper[] wrappers = new StringWrapper[_jarFileNamesList.size()];
		for (int i = 0; i < wrappers.length; ++i)
		{
			wrappers[i] = new StringWrapper((String) _jarFileNamesList.get(i));
		}
		return wrappers;
	}

	public StringWrapper getJarFileNameWrapper(int idx)
		throws ArrayIndexOutOfBoundsException
	{
		return new StringWrapper((String) _jarFileNamesList.get(idx));
	}

	public void setJarFileNameWrappers(StringWrapper[] value)
	{
		_jarFileNamesList.clear();
		if (value != null)
		{
			for (int i = 0; i < value.length; ++i)
			{
				_jarFileNamesList.add(value[i].getString());
			}
		}
	}

	public void setJarFileNameWrapper(int idx, StringWrapper value)
		throws ArrayIndexOutOfBoundsException
	{
		_jarFileNamesList.set(idx, value);
	}

	private String getString(String data)
	{
		return data != null ? data.trim() : "";
	}

	private synchronized PropertyChangeReporter getPropertyChangeReporter()
	{
		if (_propChgReporter == null)
		{
			_propChgReporter = new PropertyChangeReporter(this);
		}
		return _propChgReporter;
	}

    /* (non-Javadoc)
     * @see net.sourceforge.squirrel_sql.fw.sql.ISQLDriver#getWebSiteUrl()
     */
    public String getWebSiteUrl() {
        return _websiteUrl;
    }

    /* (non-Javadoc)
     * @see net.sourceforge.squirrel_sql.fw.sql.ISQLDriver#setWebSiteUrl(java.lang.String)
     */
    public void setWebSiteUrl(String url) throws ValidationException { 
        String data = getString(url);
        if (!data.equals(_websiteUrl)) {
            final String oldValue = _websiteUrl;
            _websiteUrl = data;
            PropertyChangeReporter pcr = getPropertyChangeReporter();
            pcr.firePropertyChange(ISQLDriver.IPropertyNames.WEBSITE_URL,
                                   oldValue,
                                   _websiteUrl);            
        }
    }

    /**
	 * If the database server has a command line utility which can be used to 
	 * execute scripts, then this would be the character or character sequence 
	 * that is accepted by the utility to separate different SQL statements 
	 * from each other.  For example, Oracle has SQL-Plus which accepts ";".
	 * However, Sybase uses "GO" as the statement separator.
	 *  
     * @return the SQL statement separator.
     */
	public String getStatementSeparator() {
		return _statementSeparator;
	}

	/**
	 * Sets the SQL statement separator. 
	 * 
	 * @param separator a string of chars to interpret as indicating where one
	 *                  statement ends and another begins.
	 *                  
	 * @throws ValidationException if the input value failed validation
	 */
	public void setStatementSeparator(String separator) 
		throws ValidationException 
	{
        String data = getString(separator);
        if (!data.equals(_statementSeparator)) {
            final String oldValue = _statementSeparator;
            _statementSeparator = data;
            PropertyChangeReporter pcr = getPropertyChangeReporter();
            pcr.firePropertyChange(ISQLDriver.IPropertyNames.STATEMENT_SEPARATOR,
                                   oldValue,
                                   _statementSeparator);            
        }		
	}
    
    
}
