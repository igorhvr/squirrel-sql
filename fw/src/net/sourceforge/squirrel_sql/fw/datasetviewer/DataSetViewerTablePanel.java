package net.sourceforge.squirrel_sql.fw.datasetviewer;
/*
 * Copyright (C) 2001-2002 Colin Bell
 * colbell@users.sourceforge.net
 * Modifications copyright (C) 2001-2002 Johan Compagner
 * jcompagner@j-com.nl
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
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultCellEditor;	//?? May not be needed??
import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellEditor;	//?? May not be needed??
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

//??import javax.swing.event.TableModelListener;
//??import javax.swing.event.TableModelEvent;

import net.sourceforge.squirrel_sql.fw.gui.BaseMDIParentFrame;
import net.sourceforge.squirrel_sql.fw.gui.ButtonTableHeader;
import net.sourceforge.squirrel_sql.fw.gui.SortableTableModel;
import net.sourceforge.squirrel_sql.fw.gui.TablePopupMenu;
import net.sourceforge.squirrel_sql.fw.gui.TextPopupMenu;
import net.sourceforge.squirrel_sql.fw.gui.action.BaseAction;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.CellComponentFactory;

public class DataSetViewerTablePanel extends BaseDataSetViewerDestination
										implements IDataSetTableControls
{
	private ILogger s_log = LoggerController.createLogger(DataSetViewerTablePanel.class);

	private MyJTable _comp = null;
	private MyTableModel _typedModel;
	private IDataSetUpdateableModel _updateableModel;

	public DataSetViewerTablePanel()
	{
		super();
	}

	public void init(IDataSetUpdateableModel updateableModel)
	{
		_comp = new MyJTable(this, updateableModel);
		_updateableModel = updateableModel;
	}
	
	public IDataSetUpdateableModel getUpdateableModel()
	{
		return _updateableModel;
	}

	public void clear()
	{
		_typedModel.clear();
		_typedModel.fireTableDataChanged();
	}
	

	public void setColumnDefinitions(ColumnDisplayDefinition[] colDefs)
	{
		super.setColumnDefinitions(colDefs);
		_comp.setColumnDefinitions(colDefs);
	}

	public void moveToTop()
	{
		if (_comp.getRowCount() > 0)
		{
			_comp.setRowSelectionInterval(0, 0);
		}
	}

	/**
	 * Get the component for this viewer.
	 *
	 * @return	The component for this viewer.
	 */
	public Component getComponent()
	{
		return _comp;
	}

	/*
	 * @see BaseDataSetViewerDestination#addRow(Object[])
	 */
	protected void addRow(Object[] row)
	{
		_typedModel.addRow(row);
	}
	
	/*
	 * @see BaseDataSetViewerDestination#getRow(row)
	 */
	protected Object[] getRow(int row)
	{
		Object values[] = new Object[_typedModel.getColumnCount()];
		for (int i=0; i < values.length; i++)
			values[i] = _typedModel.getValueAt(row, i);
		return values;
	}

	/*
	 * @see BaseDataSetViewerDestination#allRowsAdded()
	 */
	protected void allRowsAdded()
	{
		_typedModel.fireTableStructureChanged();
	}

	/*
	 * @see IDataSetViewer#getRowCount()
	 */
	public int getRowCount()
	{
		return _typedModel.getRowCount();
	}

	protected final static class MyTableModel extends AbstractTableModel
	{
		private List _data = new ArrayList();
		private ColumnDisplayDefinition[] _colDefs = new ColumnDisplayDefinition[0];
		private IDataSetTableControls _creator = null;

		MyTableModel(IDataSetTableControls creator)
		{
			super();
			_creator = creator;
		}

		/**
		 * Determine whether the cell is editable by asking the creator whether
		 * the table is editable or not
		 */
		public boolean isCellEditable(int row, int col)
		{
			return _creator.isColumnEditable(col);
		}

		public Object getValueAt(int row, int col)
		{
			return ((Object[])_data.get(row))[col];
		}

		public int getRowCount()
		{
			return _data.size();
		}

		public int getColumnCount()
		{
			return _colDefs != null ? _colDefs.length : 0;
		}

		public String getColumnName(int col)
		{
			return _colDefs != null ? _colDefs[col].getLabel() : super.getColumnName(col);
		}

		public Class getColumnClass(int col)
		{
			try
			{
				// if no columns defined, return a generic class
				// to avoid anything throwing an exception.
				if (_colDefs == null)
				{
					return Object.class;
				}
			
				return Class.forName(_colDefs[col].getClassName());
			}
			catch (Exception e)
			{
				return null;
			}
		}

		void setHeadings(ColumnDisplayDefinition[] hdgs)
		{
			_colDefs = hdgs;
		}

		public void addRow(Object[] row)
		{
			_data.add(row);
		}

		void clear()
		{
			_data.clear();
		}

		public void allRowsAdded()
		{
			fireTableStructureChanged();
		}

		/**
		 * Let creator handle saving the data, if anything is to be done with it.
		 * If the creator succeeds in changing the underlying data,
		 * then update the JTable as well.
		 */
		public void setValueAt(Object aValue, int row, int col) {
			if ( _creator.changeUnderlyingValueAt(row, col, aValue, getValueAt(row, col)))
			{
				((Object[])_data.get(row))[col] = aValue;
			}
		}
	}

	protected final class MyJTable extends JTable
	{
		private final int _multiplier;
		private static final String data = "THE QUICK BROWN FOX JUMPED OVER THE LAZY DOG";

		private TablePopupMenu _tablePopupMenu;
		private ButtonTableHeader _bth;
		private IDataSetTableControls _creator;

		MyJTable(IDataSetTableControls creator, 
			IDataSetUpdateableModel updateableObject)
		{
			super(new SortableTableModel(new MyTableModel(creator)));
			_creator = creator;
			_typedModel = (MyTableModel) ((SortableTableModel) getModel()).getActualModel();
			_multiplier =
				Toolkit.getDefaultToolkit().getFontMetrics(getFont()).stringWidth(data) / data.length();
			boolean allowUpdate = false;
			// we want to allow editing of read-only tables on-demand, but
			// it would be confusing to include the "Make Editable" option
			// when we are already in edit mode, so only allow that option when
			// the background model is updateable AND we are not already editing
			if (updateableObject != null && ! creator.isTableEditable())
				allowUpdate = true;
			createGUI(allowUpdate, updateableObject);
			
			// just in case table is editable, call creator to set up cell editors
			_creator.setCellEditors(this);
		}
		
		public IDataSetTableControls getCreator() {
			return _creator;
		}

		// JTable is inconsistant with passing events such as key strokes
		// into the components of CellEditors depending on whether you enter
		// the cell with a tab or by clicking the mouse on it.  In one case
		// JTable passes key events in, and in the other case it handles them
		// itself in a pseudo-edit mode.  This makes it difficult to grab key
		// strokes to do our own special behavior (e.g. null handling).
		// There are several solutions proposed on the internet,but this way
		// seems to be the easiest.  Whenever the JTable goes into edit mode on
		// a cell, we force all events to go to that cell by having it get the
		// focus.  The cell component is our own special class that implements
		// the necessary behavior.
		// Note that the special component classes use AWT event handling for
		// processing key strokes.  There are recommendations that say that mixing
		// AWT and Swing event processing is not recommended.  However, there is a
		// document at the Sun Java site that recommends processing key presses in
		// this way, so it should work.
		public boolean editCellAt(int row, int col) {
			boolean result = super.editCellAt(row, col);
			DefaultCellEditor ed = (DefaultCellEditor)super.getCellEditor(row, col);
			if (ed != null)
				ed.getComponent().requestFocus();
			return result;
		}

		/*
		 * override the JTable method so that whenever something asks for
		 * the cellEditor, we save a reference to that cell editor.
		 * Our ASSUMPTION is that the cell editor is only requested
		 * when it is about to be activated.
		 */
		public TableCellEditor getCellEditor(int row, int col)
		{
			TableCellEditor cellEditor = super.getCellEditor(row, col);
			currentCellEditor = (DefaultCellEditor)cellEditor;
			return cellEditor;
		}

		public void setColumnDefinitions(ColumnDisplayDefinition[] colDefs)
		{
			TableColumnModel tcm = createColumnModel(colDefs);
			setColumnModel(tcm);
			_typedModel.setHeadings(colDefs);

			// just in case table is editable, call creator to set up cell editors
			_creator.setCellEditors(this);
		}

		MyTableModel getTypedModel()
		{
			return _typedModel;
		}

		/**
		 * Display the popup menu for this component.
		 */
		private void displayPopupMenu(MouseEvent evt)
		{
			_tablePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
		}


		private TableColumnModel createColumnModel(ColumnDisplayDefinition[] colDefs)
		{

			//_colDefs = hdgs;
			TableColumnModel cm = new DefaultTableColumnModel();
			for (int i = 0; i < colDefs.length; ++i)
			{
				ColumnDisplayDefinition colDef = colDefs[i];
				int colWidth = colDef.getDisplayWidth() * _multiplier;
				if (colWidth > MAX_COLUMN_WIDTH * _multiplier)
				{
					colWidth = MAX_COLUMN_WIDTH * _multiplier;
				}

				TableColumn col = new TableColumn(i, colWidth,
					CellComponentFactory.getTableCellRenderer(colDefs[i]), null);			
				col.setHeaderValue(colDef.getLabel());
				cm.addColumn(col);
			}
			return cm;
		}

		private void createGUI(boolean allowUpdate, 
			IDataSetUpdateableModel updateableObject)
		{
			setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			setRowSelectionAllowed(false);
			setColumnSelectionAllowed(false);
			setCellSelectionEnabled(true);
			getTableHeader().setResizingAllowed(true);
			getTableHeader().setReorderingAllowed(true);
			setAutoCreateColumnsFromModel(false);
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			_bth = new ButtonTableHeader();
			setTableHeader(_bth);

			_tablePopupMenu = new TablePopupMenu(allowUpdate, updateableObject);
			_tablePopupMenu.setTable(this);

			addMouseListener(new MouseAdapter()
			{
				public void mousePressed(MouseEvent evt)
				{
					if (evt.isPopupTrigger())
					{
						MyJTable.this.displayPopupMenu(evt);
					}
					else if (evt.getClickCount() == 2)
					{
						// figure out which column the user clicked on
						// so we can pass in the right column description

						Point pt = evt.getPoint();
						int col = MyJTable.this.columnAtPoint(pt);
						ColumnDisplayDefinition colDefs[] = getColumnDefinitions();
						CellDataPopup.showDialog(MyJTable.this, colDefs[col], evt);
					}
				}
				public void mouseReleased(MouseEvent evt)
				{
					if (evt.isPopupTrigger())
					{
						MyJTable.this.displayPopupMenu(evt);
					}
				}
			});

		}
	}


	
	
	
	
	/////////////////////////////////////////////////////////////////////////
	//
	// Implement the IDataSetTableControls interface,
	// functions needed to support table operations
	//
	// These functions are called from within MyJTable and MyTable to tell
	// those classes how to operate.  The code in these functions will be
	// different depending on whether the table is read-only or editable.
	//
	// The definitions below are for read-only operation.  The editable
	// table panel overrides these functions with the versions that tell the
	// tables how to set up for editing operations.
	//
	//
	/////////////////////////////////////////////////////////////////////////
	
	/**
	 * Tell the table that it is editable.  This is called from within
	 * MyTable.isCellEditable().  We do not bother to distinguish between
	 * editable and non-editable cells within the same table.
	 */
	public boolean isTableEditable() {
		return false;
	}
	
	/**
	 * Tell the table whether particular columns are editable.
	 */
	public boolean isColumnEditable(int col) {
		return false;
	}
	
	/**
	 * Function to set up CellEditors.  Null for read-only tables.
	 */
	public void setCellEditors(JTable table) {}
	
	/**
	 * Change the data in the permanent store that is represented by the JTable.
	 * Does nothing in read-only table.
	 */
	public boolean changeUnderlyingValueAt(int row, int col, Object newValue, Object oldValue)
	{
		return false;	// underlaying data cannot be changed
	}
	
	//?? Other functions??
	/////////////////////////////////////////////////////////////////////////
}
