/*
 * Copyright (C) 2007 Rob Manning
 * manningr@users.sourceforge.net
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
package net.sourceforge.squirrel_sql.plugins.postgres.types;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.BaseDataTypeComponent;
import net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.IDataTypeComponent;
import net.sourceforge.squirrel_sql.fw.sql.ISQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

/**
 * A custom DatatType implementation of IDataTypeComponent that can handle Postgres' xml type (DataType
 * value of 1111 and type name of 'xml'). 
 * 
 * @author manningr
 */
public class PostgreSqlXmlTypeDataTypeComponent extends BaseDataTypeComponent implements IDataTypeComponent
{

	/** Logger for this class. */
	private static ILogger s_log = LoggerController.createLogger(PostgreSqlXmlTypeDataTypeComponent.class);

	/**
	 * Internationalized strings for this class.
	 */
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(PostgreSqlXmlTypeDataTypeComponent.class);

	/**
	 * I18n messages
	 */
	static interface i18n
	{
		// i18n[PostgreSqlXmlTypeDataTypeComponent.cellErrorMsg=<Error: see log file>]
		String CELL_ERROR_MSG = s_stringMgr.getString("PostgreSqlXmlTypeDataTypeComponent.cellErrorMsg");
	}

	/* IDataTypeComponent interface methods 
	
	/**
	 * @see net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.IDataTypeComponent#canDoFileIO()
	 */
	public boolean canDoFileIO()
	{
		return true;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.IDataTypeComponent#getClassName()
	 */
	public String getClassName()
	{
		return "java.lang.String";
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.IDataTypeComponent#getDefaultValue(java.lang.String)
	 */
	public Object getDefaultValue(String dbDefaultValue)
	{
		// At the moment, no default value
		if (s_log.isInfoEnabled())
		{
			s_log.info("getDefaultValue: not yet implemented");
		}
		return null;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.IDataTypeComponent#getWhereClauseValue(java.lang.Object,
	 *      net.sourceforge.squirrel_sql.fw.sql.ISQLDatabaseMetaData)
	 */
	public String getWhereClauseValue(Object value, ISQLDatabaseMetaData md)
	{
		if (value == null || value.toString() == null)
		{
			return _colDef.getLabel() + " IS NULL";
		} else
		{
			// This results in "ERROR: operator does not exist: xml = unknown"
			// So don't use xml type column in where clauses for now.
			//return _colDef.getLabel() + "='" + SQLUtilities.escapeLine(value.toString(), md) + "'";
			return "";
		}
	}

	/**
	 * This Data Type can be edited in a table cell as long as there are no issues displaying the data. If we
	 * detect our error message in the cell, then we should prevent the user from editing the cell (our error
	 * message is not meant to be valid XML data; further, we don't want to let the user whack their data with
	 * our tool accidentally)
	 * 
	 * @see net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.IDataTypeComponent#isEditableInCell(java.lang.Object)
	 */
	public boolean isEditableInCell(Object originalValue)
	{
		return !i18n.CELL_ERROR_MSG.equals(originalValue);
	}

	/**
	 * This Data Type can be edited in a popup as long as there are no issues displaying the data. If we
	 * detect our error message in the cell, then we should prevent the user from editing the cell (our error
	 * message is not meant to be valid XML data; further, we don't want to let the user whack their data with
	 * our tool accidentally)
	 * 
	 * @see net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.IDataTypeComponent#isEditableInPopup(java.lang.Object)
	 */
	public boolean isEditableInPopup(Object originalValue)
	{
		return !i18n.CELL_ERROR_MSG.equals(originalValue);
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.IDataTypeComponent#needToReRead(java.lang.Object)
	 */
	public boolean needToReRead(Object originalValue)
	{
		return false;
	}

	/**
	 * This class relies on reflection to get a handle to Oracle's XMLType which is made available separately
	 * from the JDBC driver, so we cannot just assume the user will always have, nor can we depend on it to
	 * compile SQuirreL code. So we remove this dependency here by using reflection which doesn't require this
	 * library in order to just compile the code.
	 * 
	 * @see net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.IDataTypeComponent#readResultSet(java.sql.ResultSet,
	 *      int, boolean)
	 */
	public Object readResultSet(ResultSet rs, int idx, boolean limitDataRead) throws SQLException
	{
		Object result = null;
		try
		{
			result = rs.getString(idx);
			if (result == null)
			{
				return NULL_VALUE_PATTERN;
			} 
		} catch (Exception e) {
			s_log.error("Unexpected exception while attempting to read PostgreSQL XML column", e);
		}
		if (result == null)
		{
			result = i18n.CELL_ERROR_MSG;
		}
		return result;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.IDataTypeComponent#setPreparedStatementValue(java.sql.PreparedStatement,
	 *      java.lang.Object, int)
	 */
	public void setPreparedStatementValue(PreparedStatement pstmt, Object value, int position)
		throws SQLException
	{
		if (value == null)
		{
			// Throws an exception claiming that 2007 isn't a valid type - go
			// figure.
			// pstmt.setNull(position, _colDef.getSqlType());

			// Both of these throw an exception claiming that it got a clob
			// and expected a number (inconsistent data types):
			//
			// pstmt.setClob(position, null);
			// pstmt.setNull(position, java.sql.Types.CLOB);
			//

			// This seems to work for both Oracle 9i and 10g
			//pstmt.setObject(position, null);
			pstmt.setNull(position, java.sql.Types.OTHER, "xml");
		} else
		{
			try
			{				
				pstmt.setString(position, value.toString());
			} catch (Exception e)
			{
				s_log.error("setPreparedStatementValue: Unexpected exception - " + e.getMessage(), e);
			}

		}
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.IDataTypeComponent#useBinaryEditingPanel()
	 */
	public boolean useBinaryEditingPanel()
	{
		return false;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.IDataTypeComponent#areEqual(java.lang.Object,
	 *      java.lang.Object)
	 */
	public boolean areEqual(Object obj1, Object obj2)
	{
		return ((String) obj1).equals(obj2);
	}

}
