package net.sourceforge.squirrel_sql.fw.sql;
/*
 * Copyright (C) 2003 Colin Bell
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
import java.sql.DriverPropertyInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
/**
 * A collection of <TT>SQLDriverDriverProperty</TT> objects.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class SQLDriverPropertyCollection
{
	/**
	 * JavaBean property names for this class.
	 */
	public interface IPropertyNames
	{
		String DRIVER_PROPERTIES = "driverProperties";
	}

	/** Collection of <TT></TT> objects keyed by the object name. */
	private final Map _objectsIndexMap = new TreeMap();

	/** Array of  <TT>SQLDriverProperty</TT> objects. */
	private final List _objectsList = new ArrayList();

	/**
	 * Default ctor. Creates an empty collection.
	 */
	public SQLDriverPropertyCollection()
	{
		super();
	}

	/**
	 * Clear all entries from this collection.
	 */
	public synchronized void clear()
	{
		_objectsIndexMap.clear();
		_objectsList.clear();
	}

	/**
	 * Retrieve the number of elements in this collection.
	 * 
	 * @return	the number of elements in this collection.
	 */
	public int size()
	{
		return _objectsList.size();
	}

	public synchronized void applyTo(Properties props)
	{
		for (int i = 0, limit = size(); i < limit; ++i)
		{
			SQLDriverProperty sdp = getDriverProperty(i);
			if (sdp.isSpecified())
			{
				final String value = sdp.getValue();
				if (value != null)
				{
					props.put(sdp.getName(), value);
				}
			}
		} 
	}

	/**
	 * Retrieve an array of the <TT>SQLDriverProperty</TT> objects contained
	 * in this collection.
	 * 
	 * @return	an array of the <TT>SQLDriverProperty</TT> objects contained
	 *			in this collection.
	 */
	public synchronized SQLDriverProperty[] getDriverProperties()
	{
		SQLDriverProperty[] ar = new SQLDriverProperty[_objectsList.size()];
		return (SQLDriverProperty[])_objectsList.toArray(ar);
	}

	public SQLDriverProperty getDriverProperty(int idx)
	{
		return (SQLDriverProperty)_objectsList.get(idx);
	}

	public synchronized void setDriverProperties(SQLDriverProperty[] values)
	{
		_objectsIndexMap.clear();
		_objectsList.clear();
		for (int i = 0; i < values.length; ++i)
		{
			_objectsList.add(values[i]);
			_objectsIndexMap.put(values[i].getName(), values[i]);
		}
	}

	/**
	 * Warning - should only be used when loading javabean from XML.
	 */
	public synchronized void setDriverProperty(int idx, SQLDriverProperty value)
	{
		_objectsList.add(idx, value);
		_objectsIndexMap.put(value.getName(), value);
	}

	public synchronized void applyDriverPropertynfo(DriverPropertyInfo[] infoAr)
	{
		for (int i = 0; i < infoAr.length; ++i)
		{
			SQLDriverProperty sdp = (SQLDriverProperty)_objectsIndexMap.get(infoAr[i].name);
			if (sdp == null)
			{
				sdp = new SQLDriverProperty(infoAr[i]); 
				_objectsIndexMap.put(sdp.getName(), sdp);
				_objectsList.add(sdp);
			}
			sdp.setDriverPropertyInfo(infoAr[i]);
		}
	}
}
