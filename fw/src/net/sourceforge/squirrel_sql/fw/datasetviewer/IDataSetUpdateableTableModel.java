package net.sourceforge.squirrel_sql.fw.datasetviewer;
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
import java.util.ArrayList;

/**
 * @author gwg
 *
 * The data type representing an (application) updateable table object.
 * See the explanation in IDataSetUpdateableModel for why this
 * delcaration is in fw but used by the application code.
 */
public interface IDataSetUpdateableTableModel extends IDataSetUpdateableModel
{
	/**
	 * Get warning message about unusual conditions, if any, in the current data
	 * that the user needs to be aware of before proceeding.
	 */
	public String getWarningOnCurrentData(Object[] values, ColumnDisplayDefinition[] colDefs, int col, Object oldValue);
	
	/**
	 * Get warning message about unusual conditions, if any, that will occur
	 * if we proceed with the update as expected.
	 */
	public String getWarningOnProjectedUpdate(Object[] values, ColumnDisplayDefinition[] colDefs, int col, Object newValue);
	
	/**
	 * Update the data underlying the table.
	 */
	public String updateTableComponent(Object[] values, ColumnDisplayDefinition[] colDefs,
		int col, Object oldValue, Object newValue);
	
	/**
	 * Get the column number containing the rowID for this table, if any.
	 * If there is no rowID in this table (e.g. because the DB does not
	 * support the rowID concept), then this will be -1.
	 * The name of the column might be something other than "rowID", e.g. "oid".
	 */
	public int getRowidCol();
	
	/**
	 * Delete a set of rows from the DB.
	 * If the delete succeeded this returns a null string.
	 * The deletes are done within a transaction
	 * so they are either all done or all not done.
	 */
	public String deleteRows(Object[][] rowData, ColumnDisplayDefinition[] colDefs);
}
