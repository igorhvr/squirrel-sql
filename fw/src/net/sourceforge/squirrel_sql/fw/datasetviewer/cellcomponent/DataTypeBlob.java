package net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent;
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Blob;
import java.io.ByteArrayInputStream;

import net.sourceforge.squirrel_sql.fw.datasetviewer.CellDataPopup;
//??import net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.IDataTypeComponent;
import net.sourceforge.squirrel_sql.fw.datasetviewer.ColumnDisplayDefinition;
import net.sourceforge.squirrel_sql.fw.datasetviewer.LargeResultSetObjectInfo;

/**
 * @author gwg
 *
 * This class provides the display components for handling Blob data types,
 * specifically SQL type BLOB.
 * The display components are for:
 * <UL>
 * <LI> read-only display within a table cell
 * <LI> editing within a table cell
 * <LI> read-only or editing display within a separate window
 * </UL>
 * The class also contains 
 * <UL>
 * <LI> a function to compare two display values
 * to see if they are equal.  This is needed because the display format
 * may not be the same as the internal format, and all internal object
 * types may not provide an appropriate equals() function.
 * <LI> a function to return a printable text form of the cell contents,
 * which is used in the text version of the table.
 * </UL>
 * <P>
 * The components returned from this class extend RestorableJTextField
 * and RestorableJTextArea for use in editing table cells that
 * contain values of this data type.  It provides the special behavior for null
 * handling and resetting the cell to the original value.
 */

public class DataTypeBlob
	implements IDataTypeComponent
{
	/* the whole column definition */
	private ColumnDisplayDefinition _colDef;

	/* whether nulls are allowed or not */
	private boolean _isNullable;

	/* table of which we are part (needed for creating popup dialog) */
	private JTable _table;
	
	/* The JTextComponent that is being used for editing */
	private IRestorableTextComponent _textComponent;
	
	/* The CellRenderer used for this data type */
	//??? For now, use the same renderer as everyone else.
	//??
	//?? IN FUTURE: change this to use a new instance of renederer
	//?? for this data type.
	private DefaultColumnRenderer _renderer = DefaultColumnRenderer.getInstance();


	/**
	 * Constructor - save the data needed by this data type.
	 */
	public DataTypeBlob(JTable table, ColumnDisplayDefinition colDef) {
		_table = table;
		_colDef = colDef;
		_isNullable = colDef.isNullable();
	}
	
	/**
	 * Return the name of the java class used to hold this data type.
	 */
	public String getClassName() {
		return "net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.BlobDescriptor";
	}

	/**
	 * Determine if two objects of this data type contain the same value.
	 * Neither of the objects is null
	 */
	public boolean areEqual(Object obj1, Object obj2) {
		if (obj1 == obj2)
			return true;
		
		// if both objs are null, then they matched in the previous test,
		// so at this point we know that at least one of them (or both) is not null.
		// However, one of them may still be null, and we cannot call equals() on
		// the null object, so make sure that the one we call it on is not null.
		// The equals() method handles the other one being null, if it is.
		if (obj1 != null)
			return ((BlobDescriptor)obj1).equals((BlobDescriptor)obj2);
		else
			return ((BlobDescriptor)obj2).equals((BlobDescriptor)obj1);
	}

	/*
	 * First we have the methods for in-cell and Text-table operations
	 */
	 
	/**
	 * Render a value into text for this DataType.
	 */
	public String renderObject(Object value) {
		return (String)_renderer.renderObject(value);
	}
	
	/**
	 * This Data Type can be edited in a table cell.
	 * This function is not called during the initial table load, or during
	 * normal table operations.
	 * It is called only when the user enters the cell, either to examine
	 * or to edit the data.
	 * The user may have set the LargeResultSetObjectInfo parameters to
	 * minimize the data read during the initial table load (to speed it up),
	 * but when they enter this cell we would like to show them the entire
	 * contents of the BLOB.
	 * Therefore we use a call to this function as a trigger to make sure
	 * that we have all of the BLOB data, if that is possible.
	 */
	public boolean isEditableInCell(Object originalValue) {
		return wholeBlobRead((BlobDescriptor)originalValue);
	}
	
	/**
	 * Return a JTextField usable in a CellEditor.
	 */
	public JTextField getJTextField() {
		_textComponent = new RestorableJTextField();
		
		// special handling of operations while editing this data type
		((RestorableJTextField)_textComponent).addKeyListener(new KeyTextHandler());
				
		//
		// handle mouse events for double-click creation of popup dialog.
		// This happens only in the JTextField, not the JTextArea, so we can
		// make this an inner class within this method rather than a separate
		// inner class as is done with the KeyTextHandler class.
		//
		((RestorableJTextField)_textComponent).addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent evt)
			{
				if (evt.getClickCount() == 2)
				{
					MouseEvent tableEvt = SwingUtilities.convertMouseEvent(
						(RestorableJTextField)DataTypeBlob.this._textComponent,
						evt, DataTypeBlob.this._table);
					CellDataPopup.showDialog(DataTypeBlob.this._table,
						DataTypeBlob.this._colDef, tableEvt, true);
				}
			}
		});	// end of mouse listener

		return (JTextField)_textComponent;
	}

	/**
	 * Implement the interface for validating and converting to internal object.
	 * Null is a valid successful return, so errors are indicated only by
	 * existance or not of a message in the messageBuffer.
	 * If originalValue is null, then we are just checking that the data is
	 * in a valid format (for file import/export) and not actually converting
	 * the data.
	 */
	public Object validateAndConvert(String value, Object originalValue, StringBuffer messageBuffer) {
		// handle null, which is shown as the special string "<null>"
		if (value.equals("<null>"))
			return null;
			
		// Do the conversion into the object in a safe manner
		
		//First convert the string representation into the binary bytes it is describing
		Byte[] byteClassData;
		try {
					byteClassData = BinaryDisplayConverter.convertToBytes(value,
						BinaryDisplayConverter.HEX, false);
		}
		catch (Exception e) {
			messageBuffer.append(e.toString()+"\n");
			//?? do we need the message also, or is it automatically part of the toString()?
			//messageBuffer.append(e.getMessage());
			return null;
		}
		
		byte[] byteData = new byte[byteClassData.length];
		for (int i=0; i<byteClassData.length; i++)
			byteData[i] = byteClassData[i].byteValue();

		// if the original object is not null, then it contains a Blob object
		// that we need to re-use, since that is the DBs reference to the blob data area.
		// Otherwise, we set the original Blob to null, and the write method needs to
		// know to set the field to null.
		BlobDescriptor bdesc;
		if (originalValue == null) {
			// no existing blob to re-use
			bdesc = new BlobDescriptor(null, byteData, true, true, 0);
		}
		else {
			// for convenience, cast the existing object
			bdesc = (BlobDescriptor)originalValue;
			
			// create new object to hold the different value, but use the same internal BLOB pointer
			// as the original
			bdesc = new BlobDescriptor(bdesc.getBlob(), byteData, true, true, 0);
		}
		return bdesc;

	}

	/**
	 * If true, this tells the PopupEditableIOPanel to use the
	 * binary editing panel rather than a pure text panel.
	 * The binary editing panel assumes the data is an array of bytes,
	 * converts it into text form, allows the user to change how that
	 * data is displayed (e.g. Hex, Decimal, etc.), and converts
	 * the data back from text to bytes when the user editing is completed.
	 * If this returns false, this DataType class must
	 * convert the internal data into a text string that
	 * can be displayed (and edited, if allowed) in a TextField
	 * or TextArea, and must handle all
	 * user key strokes related to editing of that data.
	 */
	public boolean useBinaryEditingPanel() {
		return true;
	}
	 

	/*
	 * Now the functions for the Popup-related operations.
	 */
	
	/**
	 * Returns true if data type may be edited in the popup,
	 * false if not.
	 */
	public boolean isEditableInPopup(Object originalValue) {
		// If all of the data has been read, then the blob can be edited in the Popup,
		// otherwise it cannot
		return isEditableInCell(originalValue);
	}

	/*
	 * Return a JTextArea usable in the CellPopupDialog
	 * and fill in the value.
	 */
	 public JTextArea getJTextArea(Object value) {
		_textComponent = new RestorableJTextArea();
		
		
		// value is a simple string representation of the data,
		// the same one used in Text and in-cell operations.
		((RestorableJTextArea)_textComponent).setText(renderObject(value));
		
		// special handling of operations while editing this data type
		((RestorableJTextArea)_textComponent).addKeyListener(new KeyTextHandler());
		
		return (RestorableJTextArea)_textComponent;
	 }

	/**
	 * Validating and converting in Popup is identical to cell-related operation.
	 */
	public Object validateAndConvertInPopup(String value, Object originalValue, StringBuffer messageBuffer) {
		return validateAndConvert(value, originalValue, messageBuffer);
	}

	/*
	 * The following is used in both cell and popup operations.
	 */	
	
	/*
	 * Internal class for handling key events during editing
	 * of both JTextField and JTextArea.
	 */
	 private class KeyTextHandler extends KeyAdapter {
	 	public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				
				// as a coding convenience, create a reference to the text component
				// that is typecast to JTextComponent.  this is not essential, as we
				// could typecast every reference, but this makes the code cleaner
				JTextComponent _theComponent = (JTextComponent)DataTypeBlob.this._textComponent;
				String text = _theComponent.getText();


				// handle cases of null
				// The processing is different when nulls are allowed and when they are not.
				//

				if ( DataTypeBlob.this._isNullable) {

					// user enters something when field is null
					if (text.equals("<null>")) {
						if ((c==KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)) {
							// delete when null => original value
							DataTypeBlob.this._textComponent.restoreText();
							e.consume();
						}
						else {
							// non-delete when null => clear field and add text
							DataTypeBlob.this._textComponent.updateText("");
							// fall through to normal processing of this key stroke
						}
					}
					else {
						// check for user deletes last thing in field
						if ((c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)) {
							if (text.length() <= 1 ) {
								// about to delete last thing in field, so replace with null
								DataTypeBlob.this._textComponent.updateText("<null>");
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
					if (text.length() == 0 &&
						(c==KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)) {
						// delete when null => original value
						DataTypeBlob.this._textComponent.restoreText();
						e.consume();
					}
				}
			}
		}

	/*
	 * Make sure the entire BLOB data is read in.
	 * Return true if it has been read successfully, and false if not.
	 */
	private boolean wholeBlobRead(BlobDescriptor bdesc) {
		if (bdesc == null)
			return true;	// can use an empty blob for editing
			
		if (bdesc.getWholeBlobRead())
			return true;	// the whole blob has been previously read in

		// data was not fully read in before, so try to do that now
		try {
			byte[] data = bdesc.getBlob().getBytes(1, (int)bdesc.getBlob().length());
			
			// read succeeded, so reset the BlobDescriptor to match
			bdesc.setBlobRead(true);
			bdesc.setData(data);
			bdesc.setWholeBlobRead(true);
			bdesc.setUserSetBlobLimit(0);
			
			// we successfully read the whole thing
			 return true;
		}
		catch (Exception ex) {
			bdesc.setBlobRead(false);
			bdesc.setWholeBlobRead(false);
			bdesc.setData(null);
			//?? What to do with this error?
			//?? error message = "Could not read the complete data. Error was: "+ex.getMessage());
			return false;
		}	
	}

	/*
	 * DataBase-related functions
	 */
	 
	 /**
	  * On input from the DB, read the data from the ResultSet into the appropriate
	  * type of object to be stored in the table cell.
	  */
	public Object readResultSet(ResultSet rs, int index,
		LargeResultSetObjectInfo largeObjInfo)
		throws java.sql.SQLException {
		
		// We always get the BLOB.
		// Since the BLOB is just a pointer to the BLOB data rather than the
		// data itself, this operation should not take much time (as opposed
		// to getting all of the data in the blob).
		Blob blob = rs.getBlob(index);

		if (rs.wasNull())
			return null;
		
		// BLOB exists, so try to read the data from it
		// based on the user's directions
		if (largeObjInfo.getReadBlobs())
		{
			// User said to read at least some of the data from the blob
			byte[] blobData = null;
			if (blob != null)
			{
				int len = (int)blob.length();
				if (len > 0)
				{
					int charsToRead = len;
					if (!largeObjInfo.getReadCompleteBlobs())
					{
						charsToRead = largeObjInfo.getReadBlobsSize();
					}
					if (charsToRead > len)
					{
						charsToRead = len;
					}
					blobData = blob.getBytes(1, charsToRead);
				}
			}
			
			// determine whether we read all there was in the blob or not
			boolean wholeBlobRead = false;
			if (largeObjInfo.getReadCompleteBlobs() ||
				blobData.length < largeObjInfo.getReadBlobsSize())
				wholeBlobRead = true;
				
			return new BlobDescriptor(blob, blobData, true, wholeBlobRead,
				largeObjInfo.getReadBlobsSize());
		}
		else
		{
			// user said not to read any of the data from the blob
			return new BlobDescriptor(blob, null, false, false, 0);
		}

	}

	/**
	 * When updating the database, generate a string form of this object value
	 * that can be used in the WHERE clause to match the value in the database.
	 * A return value of null means that this column cannot be used in the WHERE
	 * clause, while a return of "null" (or "is null", etc) means that the column
	 * can be used in the WHERE clause and the value is actually a null value.
	 * This function must also include the column label so that its output
	 * is of the form:
	 * 	"columnName = value"
	 * or
	 * 	"columnName is null"
	 * or whatever is appropriate for this column in the database.
	 */
	public String getWhereClauseValue(Object value) {
		if (value == null || ((BlobDescriptor)value).getData() == null)
			return _colDef.getLabel() + " IS NULL";
		else
			return "";	// BLOB cannot be used in WHERE clause
	}
	
	
	/**
	 * When updating the database, insert the appropriate datatype into the
	 * prepared statment at the given variable position.
	 */
	public void setPreparedStatementValue(PreparedStatement pstmt, Object value, int position)
		throws java.sql.SQLException {
		if (value == null || ((BlobDescriptor)value).getData() == null) {
			pstmt.setNull(position, _colDef.getSqlType());
		}
		else {
			// for convenience cast the object to BlobDescriptor
			BlobDescriptor bdesc = (BlobDescriptor)value;
			
			// There are a couple of possible ways to update the data in the DB.
			// The first is to use setString like this:
			//		bdesc.getBlob().setString(0, bdesc.getData());
			// However, the DB2 driver throws an exception saying that that function
			// is not implemented, so we have to use the other method, which is to use a stream.		
			pstmt.setBinaryStream(position, new ByteArrayInputStream(bdesc.getData()), bdesc.getData().length);
		}
	}
	
	/**
	 * Get a default value for the table used to input data for a new row
	 * to be inserted into the DB.
	 */
	public Object getDefaultValue(String dbDefaultValue) {
		if (dbDefaultValue != null) {
			// try to use the DB default value
			StringBuffer mbuf = new StringBuffer();
			Object newObject = validateAndConvert(dbDefaultValue, null, mbuf);
			
			// if there was a problem with converting, then just fall through
			// and continue as if there was no default given in the DB.
			// Otherwise, use the converted object
			if (mbuf.length() == 0)
				return newObject;
		}
		
		// no default in DB.  If nullable, use null.
		if (_isNullable)
			return null;
		
		// field is not nullable, so create a reasonable default value
//????? for BLOB, do not know how to create a default BLOB for insertion.
//????? Is including a BLOB on an initial insertion possible?
		return null;
	}
	
	
	/*
	 * File IO related functions
	 */
	 
	 
	 /**
	  * Say whether or not object can be exported to and imported from
	  * a file.  We put both export and import together in one test
	  * on the assumption that all conversions can be done both ways.
	  */
	 public boolean canDoFileIO() {
	 	return true;
	 }
	 
	 /**
	  * Read a file and construct a valid object from its contents.
	  * Errors are returned by throwing an IOException containing the
	  * cause of the problem as its message.
	  * <P>
	  * DataType is responsible for validating that the imported
	  * data can be converted to an object, and then must return
	  * a text string that can be used in the Popup window text area.
	  * This object-to-text conversion is the same as is done by
	  * the DataType object internally in the getJTextArea() method.
	  * 
	  * <P>
	  * File is assumed to be and ASCII string of digits
	  * representing a value of this data type.
	  */
	public String importObject(FileInputStream inStream)
		throws IOException {
	 	

		int fileSize = inStream.available();
	 	
		byte[] buf = new byte[fileSize];
	 	
		int count = inStream.read(buf);
	 	
		if (count != fileSize)
			throw new IOException(
				"Could read only "+ count +
				" bytes from a total file size of " + fileSize +
				". Import failed.");
	 	
		// Convert bytes to Bytes
		Byte[] bBytes = new Byte[count];
		for (int i=0; i<count; i++)
			bBytes[i] = new Byte(buf[i]);
	 	
		// return the text converted from the file 
		return BinaryDisplayConverter.convertToString(bBytes,
			BinaryDisplayConverter.HEX, false);
	}

	 	 
	 /**
	  * Construct an appropriate external representation of the object
	  * and write it to a file.
	  * Errors are returned by throwing an IOException containing the
	  * cause of the problem as its message.
	  * <P>
	  * DataType is responsible for validating that the given text
	  * text from a Popup JTextArea can be converted to an object.
	  * This text-to-object conversion is the same as validateAndConvertInPopup,
	  * which may be used internally by the object to do the validation.
	  * <P>
	  * The DataType object must flush and close the output stream before returning.
	  * Typically it will create another object (e.g. an OutputWriter), and
	  * that is the object that must be flushed and closed.
	  * 
	  * <P>
	  * File is assumed to be and ASCII string of digits
	  * representing a value of this data type.
	  */
	public void exportObject(FileOutputStream outStream, String text)
	   throws IOException {
	 	
	   Byte[] bBytes = BinaryDisplayConverter.convertToBytes(text,
		   BinaryDisplayConverter.HEX, false);
	 	
	   // check that the text is a valid representation
	   StringBuffer messageBuffer = new StringBuffer();
	   validateAndConvertInPopup(text, null, messageBuffer);
	   if (messageBuffer.length() > 0) {
		   // there was an error in the conversion
		   throw new IOException(new String(messageBuffer));
	   }
	 	
	   // Convert Bytes to bytes
	   byte[] bytes = new byte[bBytes.length];
	   for (int i=0; i<bytes.length; i++)
		   bytes[i] = bBytes[i].byteValue();
	 	
	   // just send the text to the output file
	   outStream.write(bytes);
	   outStream.flush();
	   outStream.close();
	}
}
