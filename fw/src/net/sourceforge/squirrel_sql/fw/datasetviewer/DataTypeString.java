package net.sourceforge.squirrel_sql.fw.datasetviewer;
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
import java.awt.event.*;

import javax.swing.JTextField;
import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
/**
 * @author gwg
 *
 * This class provides the display components for handling Integer data types,
 * specifically SQL types INTEGER, SMALLINT, and TINYINT.
 * The display components are for:
 * <UL>
 * <LI> read-only display within a table cell
 * <LI> editing within a table cell
 * <LI> read-only display within a separate window
 * <LI> editing within a separate window
 * </UL>
 * The class also contains a function to compare two display values
 * to see if they are equal.  This is needed because the display format
 * may not be the same as the internal format, and all internal object
 * types may not provide an appropriate equals() function.
 * <P>
 * The components may be derived from this class or may be
 * retrieved from elsewhere extends JTextField for use in editing table cells that
 * contain Integer values.  It provides the special behavior for null
 * handling and resetting the cell to the original value.
 */
public class DataTypeString extends JTextField
	implements IDataTypeComponent
{

	/* Original value of the JTextField */
	String _originalValue = null;

	/* whether nulls are allowed or not */
	private boolean _isNullable;

	/* the number of characters allowed in this field */
	private int _columnSize;

	/* table of which we are part (needed for creating popup dialog) */
	private JTable _table;

	/**
	 * Method to retrieve an editor for within a table cell.
	 */
	public static DefaultCellEditor getInCellEditor(
		JTable table,
		boolean isNullable,
		int columnSize)
	{
		DefaultCellEditor ed = new DefaultCellEditor(new DataTypeString(table, isNullable, columnSize));
		ed.setClickCountToStart(1);
		return ed;
	}

	/**
	 * No one outside of this class should need to create an instance.
	 */
	private DataTypeString(JTable table, boolean isNullable, int columnSize) {
		super();
		_table = table;
		_isNullable = isNullable;
		_columnSize = columnSize;

		// special handling of operations while editing Integers
		super.addKeyListener(
			new KeyAdapter() {
				public void keyTyped(KeyEvent e) {
					char c = e.getKeyChar();
					String text = DataTypeString.this.getText();

					//?? Is there any way to check for invalid input?  Valid input includes
					//?? at least any printable character, but could it also include unprintable
					//?? characters?

					// check for max size reached (only works when DB provides non-zero scale info
					if (DataTypeString.this._columnSize > 0 &&
						text.length() == DataTypeString.this._columnSize &&
						c != KeyEvent.VK_BACK_SPACE &&
						c != KeyEvent.VK_DELETE) {
						// max size reached
						e.consume();
						getToolkit().beep();
					}

					// handle cases of null
					// The processing is different when nulls are allowed and when they are not.
					//

					if ( DataTypeString.this._isNullable) {

						// user enters something when field is null
						if (text.equals("<null>")) {
							if ((c==KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)) {
								// delete when null => original value
								DataTypeString.this.updateText(DataTypeString.this._originalValue);
								e.consume();
							}
							else {
								// non-delete when null => clear field and add text
								DataTypeString.this.updateText("");
								// fall through to normal processing of this key stroke
							}
						}
						else {
							// for strings, a "blank" field is allowed, so only
							// switch to null when there is nothing left in the field
							// and user does delete
							if ((c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)) {
								if (text.length() == 0 ) {
									// about to delete last thing in field, so replace with null
									DataTypeString.this.updateText("<null>");
									e.consume();
								}
							}
						}
					}
					else {
						// field is not nullable
						//
						// if the field is not allowed to have nulls, we need to let the
						// user erase the entire contents of the field so that they can enter
						// a brand-new value from scratch.  While the empty field is not a legal
						// value, we cannot avoid allowing it.  This is the normal editing behavior,
						// so we do not need to add anything special here except for the cyclic
						// re-entering of the original data if user hits delete when field is empty
						if (DataTypeString.this.getText().length() == 0 &&
							(c==KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)) {
							// delete when null => original value
							DataTypeString.this.updateText(DataTypeString.this._originalValue);
							e.consume();
						}
					}
				}
			}
		);		// end of keyListener for null handling

		//
		// handle mouse events for double-click creation of popup dialog
		//
		addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent evt)
			{
				if (evt.getClickCount() == 2)
				{
					MouseEvent tableEvt = SwingUtilities.convertMouseEvent(
						DataTypeString.this, evt, DataTypeString.this._table);
					CellDataPopup.showDialog(DataTypeString.this._table, tableEvt);
				}
			}
		});	// end of mouse listener

	}

	// when external callers set this field, remember the value as the
	// original value of the field
	public void setText(String originalValue) {
		if (originalValue == null)
			_originalValue = "<null>";
		else _originalValue = originalValue;
		super.setText(_originalValue);
	}

	// used by internal operations to set textField value without
	// changing the original text saved in the class
	private void updateText(String newText) {
		super.setText(newText);
	}

	/**
	 * Implement the interface for validating and converting to Integer object.
	 * Null is a valid successful return, so errors are indicated only by
	 * existance or not of a message in the messageBuffer.
	 */
	public Object validateAndConvert(String value, StringBuffer messageBuffer) {
		// handle null, which is shown as the special string "<null>"
		if (value.equals("<null>"))
			return null;

		// Do the conversion into the object in a safe manner
		return value;	// Special case: the input is exactly the output
	}
}
