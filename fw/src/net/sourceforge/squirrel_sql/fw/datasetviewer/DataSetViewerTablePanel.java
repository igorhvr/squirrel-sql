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
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.JOptionPane;

//??import javax.swing.event.TableModelListener;
//??import javax.swing.event.TableModelEvent;

import net.sourceforge.squirrel_sql.fw.gui.ButtonTableHeader;
import net.sourceforge.squirrel_sql.fw.gui.SortableTableModel;
import net.sourceforge.squirrel_sql.fw.gui.TablePopupMenu;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.CellComponentFactory;
import net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.RestorableJTextField;

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


	/*
	 * The JTable used for displaying all DB ResultSet info.
	 */
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
				getFontMetrics(getFont()).stringWidth(data) / data.length();
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
			setSurrendersFocusOnKeystroke(true);
		}
		
		public IDataSetTableControls getCreator() {
			return _creator;
		}

/***********************************************************
 * I used to think that the following function was needed, but the problem does not
 * seem to occur now.  Also, I have added "setSurrendersFocusOnKeystroke(true)
 * in the constructor, so that should take care of the issue for which this function was
 * created.  I am leaving the code commented out for a little while in case someone sees a problem.
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
************************/

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
		
		
		/**
		 * There are two special cases where we need to override the default behavior
		 * when we begin cell editing.  For some reason, when you use the keyboard to
		 * enter a cell (tab, enter, arrow keys, etc), the first character that you type
		 * after entering the field is NOT passed through the KeyListener mechanism
		 * where we have the special handling in the DataTypes.  Instead, it is passed
		 * through the KeyMap and Action mechanism, and the default Action on the
		 * JTextField is to add the character to the end of the existing text, or if it is delete
		 * to delete the last character of the existing text.  In most cases, this is ok, but
		 * there are three special cases of which we only handle two here:
		 * 	- If the data field currently contains "<null>" and the user types a character,
		 * 	  we want that character to replace the string "<null>", which represents the
		 * 	  null value.  In this case we process the event normally, which usually adds
		 * 	  the char to the end of the string, then remove the char afterwards.
		 * 	  We take this approach rather than just immediately replacing the "<null>"
		 * 	  with the char because there are some chars that should not be put into
		 * 	  the editable text, such as control-characters.
		 * 	- If the data field contains "<null>" and the user types a delete, we do not
		 * 	  want to delete the last character from the string "<null>" since that string
		 * 	  represents the null value.  In this case we simply ignore the user input.
		 * 	- Whether or not the field initially contains null, we do not run the input validation
		 * 	  function for the DataType on the input character.  This means that the user
		 * 	  can type an illegal character into the field.  For example, after entering an
		 * 	  Integer field by typing a tab, the user can enter a letter (e.g. "a") into that
		 * 	  field.  The normal keyListener processing prevents that, but we cannot
		 * 	  call it from this point.  (More accurately, I cannot figure out how to do it
		 * 	  easilly.)  Thus the user may enter one character of invalid data into the field.
		 * 	  This is not too serious a problem, however, because the normal validation
		 * 	  is still done when the user leaves the field and it SQuirreL tries to convert
		 * 	  the text into an object of the correct type, so errors of this nature will still
		 * 	  be caught.  They just won't be prevented.
		 */
		public void processKeyEvent(KeyEvent e) {
			
				// handle special case of delete with <null> contents
				if (e.getKeyChar() == '\b' && getEditorComponent() != null &&
						((RestorableJTextField)getEditorComponent()).getText().equals("<null>") ) {
						//ignore the user input
						return;
				}

				// generally for KEY_TYPED this means add the typed char to the end of the text,
				// but there are some things (e.g. control chars) that are ignored, so let the
				// normal processing do its thing
				super.processKeyEvent(e);
                
				// now check to see if the original contents were <null>
				// and we have actually added the input char to the end of it                                                              
				if (getEditorComponent() != null) {
						if (e.getID() == KeyEvent.KEY_TYPED && ((RestorableJTextField)getEditorComponent()).getText().length() == 7) {
								// check that we did not just add a char to a <null>
								if (((RestorableJTextField)getEditorComponent()).getText().equals("<null>"+e.getKeyChar())) {
										// replace the null with just the char
										((RestorableJTextField)getEditorComponent()).updateText(""+e.getKeyChar());
								}
						}
				}

		}
	
		
		/*
		 * When user leaves a cell after editing it, the contents of
		 * that cell need to be converted from a string into an
		 * object of the appropriate type before updating the table.
		 * However, when the call comes from the Popup window, the data
		 * has already been converted and validated.
		 * We assume that a String being passed in here is a value from
		 * a text field that needs to be converted to an object, and
		 * a non-string object has already been validated and converted.
		 */
		 public void setValueAt(Object newValueString, int row, int col) {
		 	if (! (newValueString instanceof java.lang.String)) {
		 		// data is an object - assume already validated
		 		super.setValueAt(newValueString, row, col);
		 		return;
		 	}
		 	
		 	// data is a String, so we need to convert to real object
		 	StringBuffer messageBuffer = new StringBuffer();
		 	ColumnDisplayDefinition colDef = getColumnDefinitions()[col];
		 	Object newValueObject = CellComponentFactory.validateAndConvert(
		 		colDef, getValueAt(row, col), (String)newValueString, messageBuffer);
		 	if (messageBuffer.length() > 0) {
		 		// display error message and do not update the table
				messageBuffer.insert(0,
					"The given text cannot be converted into the internal object.\n"+
					"The database has not been changed.\n"+
					"The conversion error was:\n");
				JOptionPane.showMessageDialog(this,
					messageBuffer,
					"Conversion Error",
					JOptionPane.ERROR_MESSAGE);
		 	}
		 	else {
		 		// data converted ok, so update the table
		 		super.setValueAt(newValueObject, row, col);
		 	}
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

			_tablePopupMenu = new TablePopupMenu(allowUpdate, updateableObject,
				DataSetViewerTablePanel.this);
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
						CellDataPopup.showDialog(MyJTable.this, colDefs[col], evt,
							MyJTable.this._creator.isTableEditable());
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
	public boolean isColumnEditable(int col, Object originalValue) {
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
	
	/**
	 * Delete a set of rows from the table.
	 * The indexes are the row indexes in the SortableModel.
	 */
	public void deleteRows(int[] rows) {}	// cannot delete rows in read-only table
	
	/**
	 * Initiate operations to insert a new row into the table.
	 */
	public void insertRow() {}	// cannot insert row into read-only table
}
