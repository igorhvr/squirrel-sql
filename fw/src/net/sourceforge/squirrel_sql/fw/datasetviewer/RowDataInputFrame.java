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
import java.awt.Container;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Component;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Point;

import javax.swing.JInternalFrame;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.CellComponentFactory;
import net.sourceforge.squirrel_sql.fw.gui.ButtonTableHeader;

/**
 * @author gwg
 *
 * This is the frame that gets data from the user for creating
 * a new row in a table.
 */
public class RowDataInputFrame extends JInternalFrame
	implements ActionListener {
	
	// object that called us and that we want to return data to when done
	DataSetViewerEditableTablePanel _caller;
	
	// the table containing the user's input
	RowDataJTable table;
	
	/**
	 * ctor.
	 */
	public RowDataInputFrame(ColumnDisplayDefinition[] colDefs,
		Object[] initialValues,
		DataSetViewerEditableTablePanel caller) {
		
		super("Input New Row Data", true, true, false, true);
		
		// get the ConentPane into a variable for convenience
		Container pane = getContentPane();
		
		// save data passed in to us	
		_caller = caller;
		
		// set layout
		pane.setLayout(new BorderLayout());
		
		// create the JTable for input and put in the top of window
		table = new RowDataJTable(colDefs, initialValues);
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setSize(800, 150);
		pane.add(scrollPane, BorderLayout.NORTH);
		
		// create the buttons for input done and cancel
		JPanel buttonPanel = new JPanel();
		
		JButton insertButton = new JButton("Insert");
		buttonPanel.add(insertButton);
		insertButton.setActionCommand("insert");
		insertButton.addActionListener(this);
		
		JButton cancelButton = new JButton("Cancel");
		buttonPanel.add(cancelButton);
		cancelButton.setActionCommand("cancel");
		cancelButton.addActionListener(this);

		pane.add(buttonPanel, BorderLayout.SOUTH);

		// this frame should really go away when done
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		// display the frame
		pack();
		show();
		
	}

	/**
	 * Handle actions on the buttons 
	 */
	public void actionPerformed(ActionEvent e) {

		if (e.getActionCommand().equals("cancel")) {
			setVisible(false);
			dispose();
			return;
		}
		else if ( ! e.getActionCommand().equals("insert")) {
			return;	// do not recognize this button request
		}
		
		// user said to insert, so collect all the data from the
		// JTable and send it to the DataSetViewer for insertion
		// into DB and on-screen tables
		
		Object[] rowData = new Object[table.getModel().getColumnCount()];
		for (int i=0; i< table.getModel().getColumnCount(); i++) {
			rowData[i] = table.getValueAt(0, i);
		}

		// put the data into the DB and the on-screen JTable.
		// If there was a failure, do not make this form
		// go away since the user may be able to fix the problem
		// by changing the data.
		if (_caller.insertRow(rowData) == null) {
			// the insert worked, so make this input form go away
			setVisible(false);
			dispose();
		}
	}
	
	/**
	 * JTable for use in creating data for insertion.
	 */
	class RowDataJTable extends JTable {
		
		private ColumnDisplayDefinition[] _colDefs = null;
		
		/**
		 * constructor
		 */
		protected  RowDataJTable(ColumnDisplayDefinition[] colDefs, Object[] initalValues) {
			
			super(new RowDataModel(colDefs, initalValues));
			
			// create column model

			final String data = "THE QUICK BROWN FOX JUMPED OVER THE LAZY DOG";
			final int _multiplier =
				getFontMetrics(getFont()).stringWidth(data) / data.length();
			
			TableColumnModel cm = new DefaultTableColumnModel();
			for (int i = 0; i < colDefs.length; ++i)
			{
				ColumnDisplayDefinition colDef = colDefs[i];
				int colWidth = colDef.getDisplayWidth() * _multiplier;
				if (colWidth > _caller.MAX_COLUMN_WIDTH * _multiplier)
				{
					colWidth = _caller.MAX_COLUMN_WIDTH * _multiplier;
				}

				TableColumn col = new TableColumn(i, colWidth,
					CellComponentFactory.getTableCellRenderer(colDefs[i]), null);			
				col.setHeaderValue(colDef.getLabel());
				cm.addColumn(col);
			}

			setColumnModel(cm);
			
			// set up column headers
			ButtonTableHeader _bth = new ButtonTableHeader();
			_bth.setTable(this);
			setTableHeader(_bth);
			
			_colDefs = colDefs;
			
			setRowHeight(1, 60);

			setRowSelectionAllowed(false);
			setColumnSelectionAllowed(false);
			setCellSelectionEnabled(true);
			getTableHeader().setResizingAllowed(true);
			getTableHeader().setReorderingAllowed(true);
			setAutoCreateColumnsFromModel(false);
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			
//?? Future: may want to create TablePopupMenu to allow cut/copy/paste operations
		
			// add mouse listener for Popup
			addMouseListener(new MouseAdapter()
			{
				public void mousePressed(MouseEvent evt)
				{
					if (evt.isPopupTrigger())
					{
						// for now, ignore popup request
						//RowDataJTable.this.displayPopupMenu(evt);
					}
					else if (evt.getClickCount() == 2)
					{
						// figure out which column the user clicked on
						// so we can pass in the right column description

						Point pt = evt.getPoint();
						int col = RowDataJTable.this.columnAtPoint(pt);
						CellDataPopup.showDialog(RowDataJTable.this, _colDefs[col], evt, true);
					}
				}
				public void mouseReleased(MouseEvent evt)
				{
					if (evt.isPopupTrigger())
					{
						// for now, ignore popup request
						//RowDataJTable.this.displayPopupMenu(evt);
					}
				}
			});
		}
		
		public boolean isCellEditable(int row, int col) {
			if (row > 0)
				return false;	// only the first row (containing data) is editable
			return CellComponentFactory.isEditableInCell(_colDefs[row], getValueAt(row,col));
		}
			
		// set up editors and renderers
		public TableCellEditor getCellEditor(int row, int column) {
			// assume this is only called for the first row in the table
			return CellComponentFactory.getInCellEditor(this, _colDefs[column]);
		}
			
		public TableCellRenderer getCellRenderer(int row, int column) {
			if (row == 0)
				return CellComponentFactory.getTableCellRenderer(_colDefs[column]);
			// for entries past the first one, use the default renderer
			return new RowDataDescriptionRenderer();
		}

// Set up to allow popup editor window
//????

		// set up to validate data when user finishes editing
		public void setValueAt(Object newValueString, int row, int col) {
		 	StringBuffer messageBuffer = new StringBuffer();
		 	ColumnDisplayDefinition colDef = _colDefs[col];
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
			
	
	}
	
	/**
	 * Model for use by JTable in creating data for insertion.
	 */
	class RowDataModel extends DefaultTableModel {
		
		/**
		 * ctor
		 */
		protected RowDataModel(ColumnDisplayDefinition[] colDefs, Object[] initalValues) {
			super();
			
			// set up the list of column names and the data for the rows
			String[] colNames = new String[colDefs.length];
			Object[][] rowData = new Object[2][colDefs.length];
			for (int i=0; i<colDefs.length; i++) {
				colNames[i] = colDefs[i].getLabel();	// set column heading
				rowData[0][i] = initalValues[i];	// set data in first row
				
				// put a description of the field in the following rows
				rowData[1][i] = colDefs[i].getSqlTypeName() + "\n" +
					((colDefs[i].isNullable()) ? "nullable" : "not nullable") + "\n" +
					"mx size="+ colDefs[i].getPrecision() + "\n" +
					"scale=" + colDefs[i].getScale();
			}
			
			// put the data and header names into the model
			setDataVector(rowData, colNames);
		}
	}
	
	/**
	 * renderer to display multiple lines in one table cell
	 */
	class RowDataDescriptionRenderer implements TableCellRenderer {
		
		public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus, int row, int column) {

				JTextArea ta = new JTextArea((String)value, 4, 20);
				ta.setBackground(Color.CYAN);
				return ta;
			}
	}

}
