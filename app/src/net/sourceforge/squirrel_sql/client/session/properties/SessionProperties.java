package net.sourceforge.squirrel_sql.client.session.properties;
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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.io.Serializable;

import javax.swing.SwingConstants;

import net.sourceforge.squirrel_sql.fw.datasetviewer.DataSetViewerEditableTablePanel;
import net.sourceforge.squirrel_sql.fw.datasetviewer.DataSetViewerTablePanel;
import net.sourceforge.squirrel_sql.fw.datasetviewer.DataSetViewerTextPanel;
import net.sourceforge.squirrel_sql.fw.gui.FontInfo;
import net.sourceforge.squirrel_sql.fw.util.PropertyChangeReporter;
import net.sourceforge.squirrel_sql.fw.util.Utilities;
/**
 * This class represents the settings for a session.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class SessionProperties implements Cloneable, Serializable
{
	public interface IDataSetDestinations
	{
		String TEXT = DataSetViewerTextPanel.class.getName();
		String READ_ONLY_TABLE = DataSetViewerTablePanel.class.getName();
		String EDITABLE_TABLE = DataSetViewerEditableTablePanel.class.getName();
	}

	public interface IPropertyNames
	{
		String ABORT_ON_ERROR = "abortOnError";
		String AUTO_COMMIT = "autoCommit";
		String CATALOG_PREFIX_LIST = "catalogPrefixList";
		String COMMIT_ON_CLOSING_CONNECTION = "commitOnClosingConnection";
		String CONTENTS_LIMIT_ROWS = "contentsLimitRows";
		String CONTENTS_NBR_ROWS_TO_SHOW = "contentsNbrOfRowsToShow";
		String FONT_INFO = "fontInfo";
		String LARGE_RESULT_SET_OBJECT_INFO = "largeResultSetObjectInfo";
		String LIMIT_SQL_ENTRY_HISTORY_SIZE = "limitSqlEntryHistorySize";
		String LOAD_SCHEMAS_CATALOGS = "loadCatalogsSchemas";
		String MAIN_TAB_PLACEMENT = "mainTabPlacement";
		String META_DATA_OUTPUT_CLASS_NAME = "metaDataOutputClassName";
		String OBJECT_TAB_PLACEMENT = "objectTabPlacement";
		String SCHEMA_PREFIX_LIST = "schemaPrefixList";
		String SQL_ENTRY_HISTORY_SIZE = "sqlEntryHistorySize";
		String SHOW_RESULTS_META_DATA = "showResultsMetaData";
		String SHOW_ROW_COUNT = "showRowCount";
		String SHOW_TOOL_BAR = "showToolBar";
		String SQL_SHARE_HISTORY = "sqlShareHistory";
		String SQL_EXECUTION_TAB_PLACEMENT = "sqlExecutionTabPlacement";
		String SQL_RESULTS_TAB_PLACEMENT = "sqlResultsTabPlacement";
		String SQL_LIMIT_ROWS = "sqlLimitRows";
		String SQL_NBR_ROWS_TO_SHOW = "sqlNbrOfRowsToShow";
		String SQL_RESULTS_OUTPUT_CLASS_NAME = "sqlResultsOutputClassName";
		String SQL_START_OF_LINE_COMMENT = "sqlStartOfLineComment";
		String SQL_STATEMENT_SEPARATOR_STRING = "sqlStatementSeparatorString";
		String TABLE_CONTENTS_OUTPUT_CLASS_NAME = "tableContentsOutputClassName";
	}

	private static final FontInfo DEFAULT_FONT_INFO =
									new FontInfo(new Font("Monospaced", 0, 12));

	/** Object to handle property change events. */
	private transient PropertyChangeReporter _propChgReporter;

	private boolean _autoCommit = true;
	private int _contentsNbrRowsToShow = 100;
	private int _sqlNbrRowsToShow = 100;

	/**
	 * If <CODE>true</CODE> then issue a commit when closing a connection
	 * else issue a rollback. This property is only valid if the
	 * connection is not in auto-commit mode.
	 */
	private boolean _commitOnClosingConnection = false;

	private boolean _contentsLimitRows = true;
	private boolean _sqlLimitRows = true;

	/**
	 * <CODE>true</CODE> if schemas and catalogs should be loaded in the object
	 * tree.
	 */
	private boolean _loadSchemasCatalogs = true;

	/** Limit schema objects to those in this comma-delimited list.	*/
	private String _schemaPrefixList = "";

	/** Limit catalog objects to those in this comma-delimited list. */
	private String _catalogPrefixList = "";

	/** <TT>true</TT> if sql result meta data should be shown. */
	private boolean _showResultsMetaData = true;

	/** Name of class to use for metadata output. */
	private String _metaDataOutputClassName = IDataSetDestinations.READ_ONLY_TABLE;

	/** Name of class to use for SQL results output. */
//	private String _sqlOutputMetaDataClassName = IDataSetDestinations.READ_ONLY_TABLE;

	/** Name of class to use for table contsnts output. */
	private String _tableContentsClassName = IDataSetDestinations.READ_ONLY_TABLE;

	/**
	 * The display class for the SQL results may be either editable or read-only.
	 * The functions accessing this must use the appropriate getter to be sure
	 * of getting either the selection made by the user in the Session Properties
	 * or the read-only or the editable version.
	 */
	private String _sqlResultsOutputClassName = IDataSetDestinations.READ_ONLY_TABLE;

	/**
	 * <TT>true</TT> if row count should be displayed for every table in object tree.
	 */
	private boolean _showRowCount = false;

	/** <TT>true</TT> if toolbar should be shown. */
	private boolean _showToolbar = true;

	/** Used to separate SQL multiple statements. */
	private String _sqlStmtSep = ";";

	/** Used to indicate a &quot;Start Of Line&quot; comment in SQL. */
	private String _solComment = "--";

	/** Font information for the SQL entry area. */
	private FontInfo _fi = (FontInfo)DEFAULT_FONT_INFO.clone();

	/** Should the number of SQL statements to save in execution history be limited?. */
	private boolean _limitSqlEntryHistorySize = false;

	/**
	 * Does this session share its SQL History with other sessions?
	 */
	private boolean _sqlShareHistory = true;

	/**
	 * Number of SQL statements to save in execution history. Only applicable
	 * if <TT>_limitSqlEntryHistorySize</TT> is true.
	 */
	private int _sqlEntryHistorySize = 100;

	/** Placement of main tabs. See javax.swing.SwingConstants for valid values. */
	private int _mainTabPlacement = SwingConstants.TOP;

	/**
	 * Placement of tabs displayed when an object selected in the object
	 * tree. See javax.swing.SwingConstants for valid values.
	 */
	private int _objectTabPlacement = SwingConstants.TOP;


	/**
	 * Placement of tabs displayed for SQL execution.
	 * See javax.swing.SwingConstants for valid values.
	 */
	private int _sqlExecutionTabPlacement = SwingConstants.TOP;

	/**
	 * Placement of tabs displayed for SQL execution results.
	 * See javax.swing.SwingConstants for valid values.
	 */
	private int _sqlResultsTabPlacement = SwingConstants.TOP;

	/**
	 * If <TT>true</TT> then don't execute any further SQL if an error occurs in one.
	 */
	private boolean _abortOnError = false;

	/**
	 * Default ctor.
	 */
	public SessionProperties()
	{
		super();
	}

	/**
	 * Return a copy of this object.
	 */
	public Object clone()
	{
		try
		{
			SessionProperties props = (SessionProperties)super.clone();
			props._propChgReporter = null;
			if (_fi != null)
			{
				props.setFontInfo((FontInfo)_fi.clone());
			}


			return props;
		}
		catch (CloneNotSupportedException ex)
		{
			throw new InternalError(ex.getMessage()); // Impossible.
		}
	}

	/**
	 * Normally we display data using the class selected by the user in the
	 * Session Preferences, but there are occasions in which the application
	 * needs to override the user selection and explicitly use either a read-only
	 * or an editable table. These functions provide access to those class names.
	 */
	public String getReadOnlyTableOutputClassName()
	{
		return IDataSetDestinations.READ_ONLY_TABLE;
	}

	public String getEditableTableOutputClassName()
	{
		return IDataSetDestinations.EDITABLE_TABLE;
	}

	/**
	 * Get the name of the read-only form of the user-selected preference,
	 * which may be TEXT or READ_ONLY_TABLE. The user may have selected
	 * EDITABLE_TABLE, but the caller wants to get the read-only version
	 * (because it does not know how to handle and changes the user makes,
	 * e.g. because the data represents a multi-table join).
	 */
	public String getReadOnlySQLResultsOutputClassName()
	{
		if (_sqlResultsOutputClassName.equals(IDataSetDestinations.EDITABLE_TABLE))
			return IDataSetDestinations.READ_ONLY_TABLE;
		return _sqlResultsOutputClassName;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		getPropertyChangeReporter().addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
		getPropertyChangeReporter().removePropertyChangeListener(listener);
	}

	public String getMetaDataOutputClassName()
	{
		return _metaDataOutputClassName;
	}

	public void setMetaDataOutputClassName(String value)
	{
		if (value == null)
		{
			value = "";
		}
		if (!_metaDataOutputClassName.equals(value))
		{
			final String oldValue = _metaDataOutputClassName;
			_metaDataOutputClassName = value;
			getPropertyChangeReporter().firePropertyChange(
				IPropertyNames.META_DATA_OUTPUT_CLASS_NAME,
				oldValue, _metaDataOutputClassName);
		}
	}

	public String getTableContentsOutputClassName()
	{
		return _tableContentsClassName;
	}

	public void setTableContentsOutputClassName(String value)
	{
		if (value == null)
		{
			value = "";
		}
		if (!_tableContentsClassName.equals(value))
		{
			final String oldValue = _tableContentsClassName;
			_tableContentsClassName= value;
			getPropertyChangeReporter().firePropertyChange(
				IPropertyNames.TABLE_CONTENTS_OUTPUT_CLASS_NAME,
				oldValue, _tableContentsClassName);
		}
	}

	/**
	 * Get the type of output display selected by the user in the
	 * Session Properties, which may be text, read-only table, or editable table;
	 * the caller must be able to handle any of those (especially editable).
	 */
	public String getSQLResultsOutputClassName()
	{
		return _sqlResultsOutputClassName;
	}

	/**
	 * Set the type of output display to user selection, which may be
	 * text, read-only table, or editable table. This is called
	 * when the user makes a selection, and also when loading the
	 * preferences object from the saved data during Squirrel startup.
	 */
	public void setSQLResultsOutputClassName(String value)
	{
		if (value == null)
		{
			value = "";
		}
		if (!_sqlResultsOutputClassName.equals(value))
		{
			final String oldValue = _sqlResultsOutputClassName;
			_sqlResultsOutputClassName = value;
			getPropertyChangeReporter().firePropertyChange(
				IPropertyNames.SQL_RESULTS_OUTPUT_CLASS_NAME,
				oldValue, _sqlResultsOutputClassName);
		}
	}

	/**
	 * Force a re-build of the GUI when a user makes a table
	 * temporarily editable. The situation is that the current
	 * SessionProperties _tableContentsClassName is a read-only
	 * class (either table or text) and the user has requested that
	 * the information be made editable.
	 * This can only be requested in the ContentsTab
	 * and only when the sqlResults is read-only.
	 * We make the table editable by:
	 *      - setting the underlying data model (e.g. ContentsTab) so that it
	 *	      internally overrides the SessionProperties when getting the
	 *	      output class name, and
	 *      - telling the listeners that the SessionProperties have changed.
	 * This function is called by the underlying data model to tell the
	 * listeners to update the GUI. This is done by pretending that the
	 * SessionPropertied have just changed from being EDITABLE_TABLE to
	 * some read-only class. (We know that the current value of the
	 * _tableContentsClassName is a read-only class.)
	 *
	 * This is not a very nice way to cause the interface to be updated,
	 * but it was the simplest one that I could find. GWG 10/30/02
	 *
	 * CB TODO: (Move this elsewhere).
	 */
	public void forceTableContentsOutputClassNameChange()
	{
		// We need the old value and the new value to be different, or the
		// listeners will ignore our property change request (and not rebuild
		// the GUI). We know that the current output class is a read-only one
		// because this function is only called when the user requests that a
		// single table be made editable.
		final String oldValue = _tableContentsClassName;
		getPropertyChangeReporter().firePropertyChange(
			IPropertyNames.TABLE_CONTENTS_OUTPUT_CLASS_NAME,
			IDataSetDestinations.EDITABLE_TABLE,
			oldValue);
	}

	public boolean getAutoCommit()
	{
		return _autoCommit;
	}

	public void setAutoCommit(boolean value)
	{
		if (_autoCommit != value)
		{
			_autoCommit = value;
			getPropertyChangeReporter().firePropertyChange(
				IPropertyNames.AUTO_COMMIT,
				!_autoCommit, _autoCommit);
		}
	}

	public boolean getAbortOnError()
	{
		return _abortOnError;
	}

	public void setAbortOnError(boolean value)
	{
		if (_abortOnError != value)
		{
			_abortOnError = value;
			getPropertyChangeReporter().firePropertyChange(
				IPropertyNames.ABORT_ON_ERROR,
				!_abortOnError, _abortOnError);
		}
	}

	public boolean getShowToolBar()
	{
		return _showToolbar;
	}

	public void setShowToolBar(boolean value)
	{
		if (_showToolbar != value)
		{
			_showToolbar = value;
			getPropertyChangeReporter().firePropertyChange(
				IPropertyNames.SHOW_TOOL_BAR,
				!_showToolbar, _showToolbar);
		}
	}

	public int getContentsNbrRowsToShow()
	{
		return _contentsNbrRowsToShow;
	}

	public void setContentsNbrRowsToShow(int value)
	{
		if (_contentsNbrRowsToShow != value)
		{
			final int oldValue = _contentsNbrRowsToShow;
			_contentsNbrRowsToShow = value;
			getPropertyChangeReporter().firePropertyChange(
				IPropertyNames.CONTENTS_NBR_ROWS_TO_SHOW,
				oldValue, _contentsNbrRowsToShow);
		}
	}

	public int getSQLNbrRowsToShow()
	{
		return _sqlNbrRowsToShow;
	}

	public void setSQLNbrRowsToShow(int value)
	{
		if (_sqlNbrRowsToShow != value)
		{
			final int oldValue = _sqlNbrRowsToShow;
			_sqlNbrRowsToShow = value;
			getPropertyChangeReporter().firePropertyChange(
				IPropertyNames.SQL_NBR_ROWS_TO_SHOW,
				oldValue, _sqlNbrRowsToShow);
		}
	}

	public boolean getContentsLimitRows()
	{
		return _contentsLimitRows;
	}

	public void setContentsLimitRows(boolean value)
	{
		if (_contentsLimitRows != value)
		{
			final boolean oldValue = _contentsLimitRows;
			_contentsLimitRows = value;
			getPropertyChangeReporter().firePropertyChange(
				IPropertyNames.CONTENTS_LIMIT_ROWS,
				oldValue, _contentsLimitRows);
		}
	}

	public boolean getSQLLimitRows()
	{
		return _sqlLimitRows;
	}

	public void setSQLLimitRows(boolean value)
	{
		if (_sqlLimitRows != value)
		{
			final boolean oldValue = _sqlLimitRows;
			_sqlLimitRows = value;
			getPropertyChangeReporter().firePropertyChange(
				IPropertyNames.SQL_LIMIT_ROWS,
				oldValue, _sqlLimitRows);
		}
	}


	/**
	 * Retrieve the string used to separate multiple SQL statements. Possible
	 * examples are ";" or "GO";
	 *
	 * @return		String used to separate SQL statements.
	 */
	public String getSQLStatementSeparator()
	{
		return _sqlStmtSep;
	}

	/**
	 * Set the string used to separate multiple SQL statements. Possible
	 * examples are ";" or "GO";
	 *
	 * @param	value	Separator string.
	 */
	public void setSQLStatementSeparator(String value)
	{
		// It causes a lot of pain in serveral places to cope with nulls or
		// emptys here.
		if(null == value || 0 == value.trim().length())
		{
			value =";";
		}

		if (!_sqlStmtSep.equals(value))
		{
			final String oldValue = _sqlStmtSep;
			_sqlStmtSep = value;
			getPropertyChangeReporter().firePropertyChange(
				IPropertyNames.SQL_STATEMENT_SEPARATOR_STRING,
				oldValue, _sqlStmtSep);
		}
	}

	public boolean getCommitOnClosingConnection()
	{
		return _commitOnClosingConnection;
	}

	public synchronized void setCommitOnClosingConnection(boolean data)
	{
		final boolean oldValue = _commitOnClosingConnection;
		_commitOnClosingConnection = data;
		getPropertyChangeReporter().firePropertyChange(
			IPropertyNames.COMMIT_ON_CLOSING_CONNECTION,
			oldValue, _commitOnClosingConnection);
	}

	/**
	 * Return <TT>true</TT> if row count should be displayed for every table in
	 * object tree.
	 */
	public boolean getShowRowCount()
	{
		return _showRowCount;
	}

	/**
	 * Specify whether row count should be displayed for every table in
	 * object tree.
	 *
	 * @param	data	<TT>true</TT> fi row count should be displayed
	 *					else <TT>false</TT>.
	 */
	public synchronized void setShowRowCount(boolean data)
	{
		final boolean oldValue = _showRowCount;
		_showRowCount = data;
		getPropertyChangeReporter().firePropertyChange(
			IPropertyNames.SHOW_ROW_COUNT,
			oldValue, _showRowCount);
	}

	/**
	 * Return the string used to represent a Start of Line Comment in SQL.
	 */
	public String getStartOfLineComment()
	{
		return _solComment;
	}

	/**
	 * Set the string used to represent a Start of Line Comment in SQL.
	 */
	public synchronized void setStartOfLineComment(String data)
	{
		final String oldValue = _solComment;
		_solComment = data;
		getPropertyChangeReporter().firePropertyChange(
									IPropertyNames.SQL_START_OF_LINE_COMMENT,
									oldValue, _solComment);
	}

	public FontInfo getFontInfo()
	{
		return _fi;
	}

	public void setFontInfo(FontInfo data)
	{
		if (_fi == null || !_fi.equals(data))
		{
			final FontInfo oldValue = _fi;
			_fi = data != null ? data : (FontInfo)DEFAULT_FONT_INFO.clone();
			getPropertyChangeReporter().firePropertyChange(
									IPropertyNames.FONT_INFO, oldValue, _fi);
		}
	}


	public boolean getLimitSQLEntryHistorySize()
	{
		return _limitSqlEntryHistorySize;
	}

	public void setLimitSQLEntryHistorySize(boolean data)
	{
		final boolean oldValue = _limitSqlEntryHistorySize;
		_limitSqlEntryHistorySize = data;
		getPropertyChangeReporter().firePropertyChange(IPropertyNames.LIMIT_SQL_ENTRY_HISTORY_SIZE,
									oldValue, _limitSqlEntryHistorySize);
	}

	/**
	 * Does this session share its SQL History with other sessions?
	 *
	 * @return	<TT>true</TT> if this session shares its history.
	 */
	public boolean getSQLShareHistory()
	{
		return _sqlShareHistory;
	}

	/**
	 * Set whether this session shares its SQL History with other sessions.
	 *
	 * @param	data	<TT>true</TT> if this session shares its history.
	 */
	public void setSQLShareHistory(boolean data)
	{
		final boolean oldValue = _sqlShareHistory;
		_sqlShareHistory = data;
		getPropertyChangeReporter().firePropertyChange(IPropertyNames.SQL_SHARE_HISTORY,
										oldValue, _sqlShareHistory);
	}

	public int getSQLEntryHistorySize()
	{
		return _sqlEntryHistorySize;
	}

	public void setSQLEntryHistorySize(int data)
	{
		final int oldValue = _sqlEntryHistorySize;
		_sqlEntryHistorySize = data;
		getPropertyChangeReporter().firePropertyChange(
			IPropertyNames.SQL_ENTRY_HISTORY_SIZE,
			oldValue, _sqlEntryHistorySize);
	}

	public int getMainTabPlacement()
	{
		return _mainTabPlacement;
	}

	public void setMainTabPlacement(int value)
	{
		if (_mainTabPlacement != value)
		{
			final int oldValue = _mainTabPlacement;
			_mainTabPlacement = value;
			getPropertyChangeReporter().firePropertyChange(
				IPropertyNames.MAIN_TAB_PLACEMENT,
				oldValue, _mainTabPlacement);
		}
	}

	public int getObjectTabPlacement()
	{
		return _objectTabPlacement;
	}

	public void setObjectTabPlacement(int value)
	{
		if (_objectTabPlacement != value)
		{
			final int oldValue = _objectTabPlacement;
			_objectTabPlacement = value;
			getPropertyChangeReporter().firePropertyChange(
				IPropertyNames.OBJECT_TAB_PLACEMENT,
				oldValue, _objectTabPlacement);
		}
	}

	public int getSQLExecutionTabPlacement()
	{
		return _sqlExecutionTabPlacement;
	}

	public void setSQLExecutionTabPlacement(int value)
	{
		if (_sqlExecutionTabPlacement != value)
		{
			final int oldValue = _sqlExecutionTabPlacement;
			_sqlExecutionTabPlacement = value;
			getPropertyChangeReporter().firePropertyChange(
				IPropertyNames.SQL_EXECUTION_TAB_PLACEMENT,
				oldValue, _sqlExecutionTabPlacement);
		}
	}

	public int getSQLResultsTabPlacement()
	{
		return _sqlResultsTabPlacement;
	}

	public void setSQLResultsTabPlacement(int value)
	{
		if (_sqlResultsTabPlacement != value)
		{
			final int oldValue = _sqlResultsTabPlacement;
			_sqlResultsTabPlacement = value;
			getPropertyChangeReporter().firePropertyChange(
				IPropertyNames.SQL_RESULTS_TAB_PLACEMENT,
				oldValue, _sqlResultsTabPlacement);
		}
	}

	/**
	 * Return comma-separated list of schema prefixes to display in the object
	 * tree.
	 */
	public String getSchemaPrefixList()
	{
		return _schemaPrefixList;
	}

	/**
	 * Return array of schema prefixes to display in the object tree.
	 */
	public String[] getSchemaPrefixArray()
	{
		return Utilities.splitString(_schemaPrefixList, ',', true);
	}

	/**
	 * Set the comma-separated list of schema prefixes to display in the object tree.
	 */
	public synchronized void setSchemaPrefixList(String data)
	{
		final String oldValue = _schemaPrefixList;
		_schemaPrefixList = data;
		getPropertyChangeReporter().firePropertyChange(
			IPropertyNames.SCHEMA_PREFIX_LIST,
			oldValue,
			_schemaPrefixList);
	}

	/**
	 * Return comma-separated catalog of schema prefixes to display in the
	 * object tree.
	 */
	public String getCatalogPrefixList()
	{
		return _catalogPrefixList;
	}

	/**
	 * Return array of catalog prefixes to display in the object tree.
	 */
	public String[] getCatalogPrefixArray()
	{
		return Utilities.splitString(_catalogPrefixList, ',', true);
	}

	/**
	 * Set the comma-separated list of catalog prefixes to display in the object tree.
	 */
	public synchronized void setCatalogPrefixList(String data)
	{
		final String oldValue = _catalogPrefixList;
		_catalogPrefixList = data;
		getPropertyChangeReporter().firePropertyChange(IPropertyNames.CATALOG_PREFIX_LIST,
												oldValue, _catalogPrefixList);
	}

	/**
	 * Return <CODE>true</CODE> if schemas and catalogs should be loaded into
	 * the object tree.
	 */
	public boolean getLoadSchemasCatalogs()
	{
		return _loadSchemasCatalogs;
	}

	/**
	 * Set <CODE>true</CODE> if schemas and catalogs should be loaded into the
	 * object tree.
	 */
	public synchronized void setLoadSchemasCatalogs(boolean data)
	{
		final boolean oldValue = _loadSchemasCatalogs;
		_loadSchemasCatalogs = data;
		getPropertyChangeReporter().firePropertyChange(
			IPropertyNames.LOAD_SCHEMAS_CATALOGS, oldValue, _loadSchemasCatalogs);
	}

	/**
	 * Set <CODE>true</CODE> if sql results meta data should be loaded.
	 */
	public synchronized void setShowResultsMetaData(boolean data)
	{
		final boolean oldValue = _showResultsMetaData;
		_showResultsMetaData = data;
		getPropertyChangeReporter().firePropertyChange(
							IPropertyNames.SHOW_RESULTS_META_DATA,
							oldValue, _showResultsMetaData);
	}

	/**
	 * Return <CODE>true</CODE> if sql results meta data should be loaded.
	 */
	public boolean getShowResultsMetaData()
	{
		return _showResultsMetaData;
	}

	private synchronized PropertyChangeReporter getPropertyChangeReporter()
	{
		if (_propChgReporter == null)
		{
			_propChgReporter = new PropertyChangeReporter(this);
		}
		return _propChgReporter;
	}
}
