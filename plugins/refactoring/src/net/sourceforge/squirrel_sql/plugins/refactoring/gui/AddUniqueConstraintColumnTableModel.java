/*
 * Copyright (C) 2008 Rob Manning
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
package net.sourceforge.squirrel_sql.plugins.refactoring.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.sourceforge.squirrel_sql.plugins.refactoring.gui.AddUniqueConstraintDialog.i18n;

class AddUniqueConstraintColumnTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 3668028756342251497L;

	private final ArrayList<String> rowData = new ArrayList<String>();

	private final String[] columnNames = new String[] { i18n.COLUMNS_LOCAL_COLUMN_HEADER };

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public int getRowCount()
	{
		return rowData.size();
	}

	public int getColumnCount()
	{
		return columnNames.length;
	}

	public Object getValueAt(int row, int col)
	{
		return rowData.get(row);
	}

	public boolean isCellEditable(int row, int col)
	{
		return false;
	}

	public void addColumn(String column)
	{
		rowData.add(column);
		fireTableDataChanged();
	}

	public String deleteRow(int row)
	{
		String removedRow = rowData.remove(row);
		fireTableDataChanged();
		return removedRow;
	}

	public List<String> getRowData()
	{
		return rowData;
	}
}