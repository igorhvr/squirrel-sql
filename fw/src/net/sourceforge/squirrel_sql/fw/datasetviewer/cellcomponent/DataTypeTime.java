package net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent;
/*
 * Copyright (C) 2001-2004 Colin Bell
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
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.Insets;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.BorderFactory;

import javax.swing.text.JTextComponent;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;

import java.text.DateFormat;

import net.sourceforge.squirrel_sql.fw.datasetviewer.CellDataPopup;
import net.sourceforge.squirrel_sql.fw.datasetviewer.ColumnDisplayDefinition;
import net.sourceforge.squirrel_sql.fw.gui.OkJPanel;
import net.sourceforge.squirrel_sql.fw.gui.RightLabel;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;

/**
 * @author gwg
 *
 * This class provides the display components for handling Time data types,
 * specifically SQL type TIME.
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

public class DataTypeTime
	implements IDataTypeComponent
{

	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(DataTypeTime.class);

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
	 * Name of this class, which is needed because the class name is needed
	 * by the static method getControlPanel, so we cannot use something
	 * like getClass() to find this name.
	 */
	private static final String thisClassName =
		"net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.DataTypeTime";


	/** Default date format */
	private static int DEFAULT_LOCALE_FORMAT = DateFormat.SHORT;

	/*
	 * Properties settable by the user
	 */
	 // flag for whether we have already loaded the properties or not
	 private static boolean propertiesAlreadyLoaded = false;

	 // flag for whether to use the default Java format (true)
	 // or the Locale-dependent format (false)
	 private static boolean useJavaDefaultFormat = true;

	 // which locale-dependent format to use; short, medium, long, or full
	 private static int localeFormat = DEFAULT_LOCALE_FORMAT;

	 // Whether to force user to enter dates in exact format or use heuristics to guess it
	 private static boolean lenient = true;

	 // The DateFormat object to use for all locale-dependent formatting.
	 // This is reset each time the user changes the previous settings.
	 private static DateFormat dateFormat = DateFormat.getTimeInstance(localeFormat);

	/**
	 * Constructor - save the data needed by this data type.
	 */
	public DataTypeTime(JTable table, ColumnDisplayDefinition colDef) {
		_table = table;
		_colDef = colDef;
		_isNullable = colDef.isNullable();

		loadProperties();
	}

	/** Internal function to get the user-settable properties from the DTProperties,
	 * if they exist, and to ensure that defaults are set if the properties have
	 * not yet been created.
	 * <P>
	 * This method may be called from different places depending on whether
	 * an instance of this class is created before the user brings up the Session
	 * Properties window.  In either case, the data is static and is set only
	 * the first time we are called.
	 */
	private static void loadProperties() {

		//set the property values
		// Note: this may have already been done by another instance of
		// this DataType created to handle a different column.
		if (propertiesAlreadyLoaded == false) {
			// get parameters previously set by user, or set default values
			useJavaDefaultFormat =true;	// set to use the Java default
			String useJavaDefaultFormatString = DTProperties.get(
				thisClassName, "useJavaDefaultFormat");
			if (useJavaDefaultFormatString != null && useJavaDefaultFormatString.equals("false"))
				useJavaDefaultFormat =false;

			// get which locale-dependent format to use
			localeFormat =DateFormat.SHORT;	// set to use the Java default
			String localeFormatString = DTProperties.get(
				thisClassName, "localeFormat");
			if (localeFormatString != null)
				localeFormat = Integer.parseInt(localeFormatString);

			// use lenient input or force user to enter exact format
			lenient = true;	// set to allow less stringent input
			String lenientString = DTProperties.get(
				thisClassName, "lenient");
			if (lenientString != null && lenientString.equals("false"))
				lenient =false;
		}
	}

	/**
	 * Return the name of the java class used to hold this data type.
	 */
	public String getClassName() {
		return "java.sql.Time";
	}

	/**
	 * Determine if two objects of this data type contain the same value.
	 * Neither of the objects is null
	 */
	public boolean areEqual(Object obj1, Object obj2) {
		return ((Time)obj1).equals(obj2);
	}

	/*
	 * First we have the methods for in-cell and Text-table operations
	 */

	/**
	 * Render a value into text for this DataType.
	 */
	public String renderObject(Object value) {
		// use the Java default date-to-string
		if (useJavaDefaultFormat == true || value == null)
			return (String)_renderer.renderObject(value);

		// use a date formatter
		if (value == null)
			return (String)_renderer.renderObject(value);
		else return (String)_renderer.renderObject(dateFormat.format(value));
	}

	/**
	 * This Data Type can be edited in a table cell.
	 */
	public boolean isEditableInCell(Object originalValue) {
		return true;
	}

	/**
	 * See if a value in a column has been limited in some way and
	 * needs to be re-read before being used for editing.
	 * For read-only tables this may actually return true since we want
	 * to be able to view the entire contents of the cell even if it was not
	 * completely loaded during the initial table setup.
	 */
	public boolean needToReRead(Object originalValue) {
		// this DataType does not limit the data read during the initial load of the table,
		// so there is no need to re-read the complete data later
		return false;
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
						(RestorableJTextField)DataTypeTime.this._textComponent,
						evt, DataTypeTime.this._table);
					CellDataPopup.showDialog(DataTypeTime.this._table,
						DataTypeTime.this._colDef, tableEvt, true);
				}
			}
		});	// end of mouse listener

		return (JTextField)_textComponent;
	}

	/**
	 * Implement the interface for validating and converting to internal object.
	 * Null is a valid successful return, so errors are indicated only by
	 * existance or not of a message in the messageBuffer.
	 */
	public Object validateAndConvert(String value, Object originalValue, StringBuffer messageBuffer) {
		// handle null, which is shown as the special string "<null>"
		if (value.equals("<null>") || value.equals(""))
			return null;

		// Do the conversion into the object in a safe manner
		try {
			if (useJavaDefaultFormat) {
				// allow the user to enter just the hour or just hour and minute
				// and assume the un-entered values are 0
				int firstColon = value.indexOf(":");
				if (firstColon == -1) {
					// user just entered the hour, so append min & sec
					value = value + ":0:0";
				}
				else {
					// user entered hour an min. See if they also entered secs
					if (value.indexOf(":", firstColon + 1) == -1) {
						// user did not enter seconds
						value = value + ":0";
					}
				}
				Object obj = Time.valueOf(value);
				return obj;
			}
			else {
				// use the DateFormat to parse
				java.util.Date javaDate = dateFormat.parse(value);
				return new Time(javaDate.getTime());
			}
		}
		catch (Exception e) {
			messageBuffer.append(e.toString()+"\n");
			//?? do we need the message also, or is it automatically part of the toString()?
			//messageBuffer.append(e.getMessage());
			return null;
		}
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
		return false;
	}


	/*
	 * Now the functions for the Popup-related operations.
	 */

	/**
	 * Returns true if data type may be edited in the popup,
	 * false if not.
	 */
	public boolean isEditableInPopup(Object originalValue) {
		return true;
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
				JTextComponent _theComponent = (JTextComponent)DataTypeTime.this._textComponent;
				String text = _theComponent.getText();

				// tabs and newlines get put into the text before this check,
				// so remove them
				// This only applies to Popup editing since these chars are
				// not passed to this level by the in-cell editor.
				if (c == KeyEvent.VK_TAB || c == KeyEvent.VK_ENTER) {
					// remove all instances of the offending char
					int index = text.indexOf(c);
					if (index == text.length() -1) {
						text = text.substring(0, text.length()-1);	// truncate string
					}
					else {
						text = text.substring(0, index) + text.substring(index+1);
					}
					((IRestorableTextComponent)_theComponent).updateText( text);
					_theComponent.getToolkit().beep();
					e.consume();
				}


				// handle cases of null
				// The processing is different when nulls are allowed and when they are not.
				//

				if ( DataTypeTime.this._isNullable) {

					// user enters something when field is null
					if (text.equals("<null>")) {
						if ((c==KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)) {
							// delete when null => original value
							DataTypeTime.this._textComponent.restoreText();
							e.consume();
						}
						else {
							// non-delete when null => clear field and add text
							DataTypeTime.this._textComponent.updateText("");
							// fall through to normal processing of this key stroke
						}
					}
					else {
						// check for user deletes last thing in field
						if ((c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)) {
							if (text.length() <= 1 ) {
								// about to delete last thing in field, so replace with null
								DataTypeTime.this._textComponent.updateText("<null>");
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
						DataTypeTime.this._textComponent.restoreText();
						e.consume();
					}
				}
			}
		}


	/*
	 * DataBase-related functions
	 */

	 /**
	  * On input from the DB, read the data from the ResultSet into the appropriate
	  * type of object to be stored in the table cell.
	  */
	public Object readResultSet(ResultSet rs, int index, boolean limitDataRead)
		throws java.sql.SQLException {

		Time data = rs.getTime(index);
		if (rs.wasNull())
			return null;
		else return data;
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
	public String getWhereClauseValue(Object value, String databaseProductName) {
		if (value == null || value.toString() == null || value.toString().length() == 0)
			return _colDef.getLabel() + " IS NULL";
		else
			return _colDef.getLabel() + "={t '" + value.toString() +"'}";
	}


	/**
	 * When updating the database, insert the appropriate datatype into the
	 * prepared statment at the given variable position.
	 */
	public void setPreparedStatementValue(PreparedStatement pstmt, Object value, int position)
		throws java.sql.SQLException {
		if (value == null) {
			pstmt.setNull(position, _colDef.getSqlType());
		}
		else {
			pstmt.setTime(position, ((Time)value));
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
		return new Time(new java.util.Date().getTime());
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

		 InputStreamReader inReader = new InputStreamReader(inStream);

		 int fileSize = inStream.available();

		 char charBuf[] = new char[fileSize];

		 int count = inReader.read(charBuf, 0, fileSize);

		 if (count != fileSize)
			 throw new IOException(
				 "Could read only "+ count +
				 " chars from a total file size of " + fileSize +
				 ". Import failed.");

		 // convert file text into a string
		 // Special case: some systems tack a newline at the end of
		 // the text read.  Assume that if last char is a newline that
		 // we want everything else in the line.
		 String fileText;
		 if (charBuf[count-1] == KeyEvent.VK_ENTER)
			 fileText = new String(charBuf, 0, count-1);
		 else fileText = new String(charBuf);

		 // test that the string is valid by converting it into an
		 // object of this data type
		 StringBuffer messageBuffer = new StringBuffer();
		 validateAndConvertInPopup(fileText, null, messageBuffer);
		 if (messageBuffer.length() > 0) {
			 // convert number conversion issue into IO issue for consistancy
			 throw new IOException(
				 "Text does not represent data of type "+getClassName()+
				 ".  Text was:\n"+fileText);
		 }

		 // return the text from the file since it does
		 // represent a valid data value
		 return fileText;
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

		 OutputStreamWriter outWriter = new OutputStreamWriter(outStream);

		 // check that the text is a valid representation
		 StringBuffer messageBuffer = new StringBuffer();
		 validateAndConvertInPopup(text, null, messageBuffer);
		 if (messageBuffer.length() > 0) {
			 // there was an error in the conversion
			 throw new IOException(new String(messageBuffer));
		 }

		 // just send the text to the output file
		outWriter.write(text);
		outWriter.flush();
		outWriter.close();
	 }


	/*
	 * Property change control panel
	 */

	 /**
	  * Generate a JPanel containing controls that allow the user
	  * to adjust the properties for this DataType.
	  * All properties are static accross all instances of this DataType.
	  * However, the class may choose to apply the information differentially,
	  * such as keeping a list (also entered by the user) of table/column names
	  * for which certain properties should be used.
	  * <P>
	  * This is called ONLY if there is at least one property entered into the DTProperties
	  * for this class.
	  * <P>
	  * Since this method is called by reflection on the Method object derived from this class,
	  * it does not need to be included in the Interface.
	  * It would be nice to include this in the Interface for consistancy, documentation, etc,
	  * but the Interface does not seem to like static methods.
	  */
	 public static OkJPanel getControlPanel() {

		/*
		 * If you add this method to one of the standard DataTypes in the
		 * fw/datasetviewer/cellcomponent directory, you must also add the name
		 * of that DataType class to the list in CellComponentFactory, method
		 * getControlPanels, variable named initialClassNameList.
		 * If the class is being registered with the factory using registerDataType,
		 * then you should not include the class name in the list (it will be found
		 * automatically), but if the DataType is part of the case statement in the
		 * factory method getDataTypeObject, then it does need to be explicitly listed
		 * in the getControlPanels method also.
		 */

		 // if this panel is called before any instances of the class have been
		 // created, we need to load the properties from the DTProperties.
		 loadProperties();

		return new BlobOkJPanel();
	 }

	// Class that displays the various formats available for dates
	public static class DateFormatTypeCombo extends JComboBox
	{
		public DateFormatTypeCombo()
		{
			// i18n[dataTypeTime.full=Full ({0})]
			addItem(s_stringMgr.getString("dataTypeTime.full", DateFormat.getTimeInstance(DateFormat.FULL).format(new java.util.Date())));
			// i18n[dataTypeTime.long=Long ({0})]
			addItem(s_stringMgr.getString("dataTypeTime.long", DateFormat.getTimeInstance(DateFormat.LONG).format(new java.util.Date())));
			// i18n[dataTypeTime.medium=Medium ({0})]
			addItem(s_stringMgr.getString("dataTypeTime.medium", DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new java.util.Date())));
			// i18n[dataTypeTime.short=Short ({0})]
			addItem(s_stringMgr.getString("dataTypeTime.short", DateFormat.getTimeInstance(DateFormat.SHORT).format(new java.util.Date())));
		}

		public void setSelectedIndex(int option) {
			if (option == DateFormat.SHORT)
				super.setSelectedIndex(3);
			else if (option == DateFormat.MEDIUM)
				super.setSelectedIndex(2);
			else if (option == DateFormat.LONG)
				super.setSelectedIndex(1);
			else super.setSelectedIndex(0);
		}

		public int getValue() {
			if (getSelectedIndex() == 3)
				return DateFormat.SHORT;
			else if (getSelectedIndex() == 2)
				return DateFormat.MEDIUM;
			else if (getSelectedIndex() == 1)
				return DateFormat.LONG;
			else return DateFormat.FULL;
		}
	}


	 /**
	  * Inner class that extends OkJPanel so that we can call the ok()
	  * method to save the data when the user is happy with it.
	  */
	 private static class BlobOkJPanel extends OkJPanel
	 {
		 /*
		 * GUI components - need to be here because they need to be
		 * accessible from the event handlers to alter each other's state.
		 */
		 // check box for whether to use Java Default or a Locale-dependent format


		 private JCheckBox useJavaDefaultFormatChk =
			 // i18n[dataTypeTime.useDefaultFormat=Use default format ({0})]
			 new JCheckBox(s_stringMgr.getString("dataTypeTime.useDefaultFormat", new Time(new java.util.Date().getTime()).toString()));

		 // label for the date format combo, used to enable/disable text
	    // i18n[dataTypeTime.useDefaultFormat2= or locale-dependent format:]
		 private RightLabel dateFormatTypeDropLabel = new RightLabel(s_stringMgr.getString("dataTypeTime.useDefaultFormat2"));

		 // Combo box for read-all/read-part of blob
		 private DateFormatTypeCombo dateFormatTypeDrop = new DateFormatTypeCombo();

		 // checkbox for whether to interpret input leniently or not
		 // i18n[dataTypeTime.inexact=allow inexact format on input]
		 private JCheckBox lenientChk = new JCheckBox(s_stringMgr.getString("dataTypeTime.inexact"));


		 public BlobOkJPanel()
		 {

			 /* set up the controls */
			 // checkbox for Java default/non-default format
			 useJavaDefaultFormatChk.setSelected(useJavaDefaultFormat);
			 useJavaDefaultFormatChk.addChangeListener(new ChangeListener()
			 {
				 public void stateChanged(ChangeEvent e)
				 {
					 dateFormatTypeDrop.setEnabled(! useJavaDefaultFormatChk.isSelected());
					 dateFormatTypeDropLabel.setEnabled(! useJavaDefaultFormatChk.isSelected());
					 lenientChk.setEnabled(! useJavaDefaultFormatChk.isSelected());
				 }
			 });

			 // Combo box for read-all/read-part of blob
			 dateFormatTypeDrop = new DateFormatTypeCombo();
			 dateFormatTypeDrop.setSelectedIndex(localeFormat);

			 // lenient checkbox
			 lenientChk.setSelected(lenient);

			 // handle cross-connection between fields
			 dateFormatTypeDrop.setEnabled(! useJavaDefaultFormatChk.isSelected());
			 dateFormatTypeDropLabel.setEnabled(! useJavaDefaultFormatChk.isSelected());
			 lenientChk.setEnabled(! useJavaDefaultFormatChk.isSelected());

			 /*
			  * Create the panel and add the GUI items to it
			 */

			 setLayout(new GridBagLayout());


			 // i18n[dataTypeTime.typeTime=Time   (SQL type 92)]
			 setBorder(BorderFactory.createTitledBorder(s_stringMgr.getString("dataTypeTime.typeTime")));
			 final GridBagConstraints gbc = new GridBagConstraints();
			 gbc.fill = GridBagConstraints.HORIZONTAL;
			 gbc.insets = new Insets(4, 4, 4, 4);
			 gbc.anchor = GridBagConstraints.WEST;

			 gbc.gridx = 0;
			 gbc.gridy = 0;

			 gbc.gridwidth = GridBagConstraints.REMAINDER;
			 add(useJavaDefaultFormatChk, gbc);

			 gbc.gridwidth = 1;
			 gbc.gridx = 0;
			 ++gbc.gridy;
			 add(dateFormatTypeDropLabel, gbc);

			 ++gbc.gridx;
			 add(dateFormatTypeDrop, gbc);

			 gbc.gridx = 0;
			 ++gbc.gridy;
			 add(lenientChk, gbc);

		 } // end of constructor for inner class


		 /**
		  * User has clicked OK in the surrounding JPanel,
		  * so save the current state of all variables
		  */
		 public void ok()
		 {
			 // get the values from the controls and set them in the static properties
			 useJavaDefaultFormat = useJavaDefaultFormatChk.isSelected();
			 DTProperties.put(
				 thisClassName,
				 "useJavaDefaultFormat", new Boolean(useJavaDefaultFormat).toString());


			 localeFormat = dateFormatTypeDrop.getValue();
			 dateFormat = DateFormat.getTimeInstance(localeFormat);	// lenient is set next
			 DTProperties.put(
				 thisClassName,
				 "localeFormat", Integer.toString(localeFormat));

			 lenient = lenientChk.isSelected();
			 dateFormat.setLenient(lenient);
			 DTProperties.put(
				 thisClassName,
				 "lenient", new Boolean(lenient).toString());
		 }

	 } // end of inner class

}
