package net.sourceforge.squirrel_sql.fw.datasetviewer;
/*
 * Copyright (C) 2001 Colin Bell
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
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;


import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;

import net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.CellComponentFactory;
import net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.RestorableJTextArea;
import net.sourceforge.squirrel_sql.fw.gui.TextPopupMenu;
import net.sourceforge.squirrel_sql.fw.gui.action.BaseAction;
import net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent.BinaryDisplayConverter;


/**
 * @author gwg
 *
 * Class to handle IO between user and editable text, text and object,
 * and text/object to/from file.
 */
public class PopupEditableIOPanel extends JPanel
	implements ActionListener {
	
	// The text area displaying the object contents
	private final JTextArea _ta;
	
	// the scroll pane that holds the text area
	private final JScrollPane scrollPane;
	
	// Description needed to handle conversion of data to/from Object
	private final ColumnDisplayDefinition _colDef;
	
	private MouseAdapter _lis;
	
	private final TextPopupMenu _popupMenu;
	
	// name of file to do export/import/process on
	private JTextField fileNameField;

	// command to use when processing data with an external program
	private JComboBox externalCommandCombo;
	
	// save the original value for re-use by CLOB/BLOB types in conversion
	private Object originalValue;
	
	// Binary data viewing option: which radix to use
	// This object is only non-null when the data is binary data
	private JComboBox radixList = null;
	private String previousRadixListItem = null;
	// Binary data viewing option: view ascii as char rather than as numeric value
	private JCheckBox showAscii = null;
	private boolean previousShowAscii;
	
	class BinaryOptionActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			
			// user asked to see binary data in a different format
			int base = 16;	// default to hex
			if (previousRadixListItem.equals("Decimal")) base = 10;
			else if (previousRadixListItem.equals("Octal")) base = 8;
			else if (previousRadixListItem.equals("Binary")) base = 2;
			
			Byte[] bytes = BinaryDisplayConverter.convertToBytes(_ta.getText(),
				base, previousShowAscii);
		
			// return the expected format for this data
			base = 16;	// default to hex
			if (radixList.getSelectedItem().equals("Decimal")) base = 10;
			else if (radixList.getSelectedItem().equals("Octal")) base = 8;
			else if (radixList.getSelectedItem().equals("Binary")) base = 2;
		
			((RestorableJTextArea)_ta).updateText(			
				BinaryDisplayConverter.convertToString(bytes,
				base, showAscii.isSelected()));
			
			previousRadixListItem = (String)radixList.getSelectedItem();
			previousShowAscii = showAscii.isSelected();

			return;
		}
	}
	private BinaryOptionActionListener optionActionListener =
		new BinaryOptionActionListener();
	
	// text put in file name field to indicate that we should
	// create a temp file for export
	private final String TEMP_FILE_FLAG = "<temp file>";
	
	// Symbol used by user in Command field to indicate
	// "Put the file name here" when the command is executed.
	private final String FILE_REPLACE_FLAG = "%f";

	/**
	 * Constructor
	 */
	public PopupEditableIOPanel(ColumnDisplayDefinition colDef,
		Object value, boolean isEditable) {
			
		originalValue = value;	// save for possible future use
		
		_popupMenu = new TextPopupMenu();
		
		_colDef = colDef;
		_ta = CellComponentFactory.getJTextArea(colDef, value);
		
		if (isEditable) {
			_ta.setEditable(true);
			_ta.setBackground(Color.yellow);	// tell user it is editable
		}
		else {
			_ta.setEditable(false);
		}

		
		_ta.setLineWrap(true);
		_ta.setWrapStyleWord(true);
		
		setLayout(new BorderLayout());
		
		// add a panel containing binary data editing options, if needed
		JPanel displayPanel = new JPanel();
		displayPanel.setLayout(new BorderLayout());
		scrollPane = new JScrollPane(_ta);
		scrollPane.setWheelScrollingEnabled(true);
		displayPanel.add(scrollPane, BorderLayout.CENTER);
		if (CellComponentFactory.useBinaryEditingPanel(colDef)) {
			// this is a binary field, so allow for multiple viewing options
			
			String[] radixListData = { "Hex", "Decimal", "Octal", "Binary" };
			radixList = new JComboBox(radixListData);
			radixList.addActionListener(optionActionListener);
			previousRadixListItem = "Hex";
			
			showAscii = new JCheckBox();
			previousShowAscii = false;
			showAscii.addActionListener(optionActionListener);
			
			JPanel displayControlsPanel = new JPanel();
			// use default sequential layout
			displayControlsPanel.add(new JLabel("Number Base:"));
			displayControlsPanel.add(radixList);
			displayControlsPanel.add(new JLabel("    "));	// add some space
			displayControlsPanel.add(showAscii);
			displayControlsPanel.add(new JLabel("Show ASCII as chars"));
			displayPanel.add(displayControlsPanel, BorderLayout.SOUTH);
		}
		add(displayPanel, BorderLayout.CENTER);

		// add controls for file handling, but only if DataType
		// can do File operations
		if (CellComponentFactory.canDoFileIO(colDef)) {
			// yes it can, so add controls
			add(exportImportPanel(isEditable), BorderLayout.SOUTH);
		}
		
		_popupMenu.add(new LineWrapAction());
		_popupMenu.add(new WordWrapAction());
		_popupMenu.setTextComponent(_ta);
		
	}
	
	/**
	 * build the user interface for export/import operations
	 */
	private JPanel exportImportPanel(boolean isEditable) {
		JPanel eiPanel = new JPanel();
		eiPanel.setLayout(new GridBagLayout());
		
		final GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.insets = new Insets(4,4,4,4);
		gbc.gridx = 0;
		gbc.gridy = 0;
		
		// File handling controls
		eiPanel.add(new JLabel("Use File: "), gbc);


		fileNameField = new JTextField(TEMP_FILE_FLAG, 19);
		gbc.gridx++;
		eiPanel.add(fileNameField, gbc);

		
		// add button for Brows
		JButton browseButton = new JButton("Browse");
		browseButton.setActionCommand("browse");
		browseButton.addActionListener(this);


		gbc.gridx++;
		eiPanel.add(browseButton, gbc);
		

		JButton exportButton = new JButton("Export");
		exportButton.setActionCommand("export");
		exportButton.addActionListener(this);

		gbc.gridx++;
		eiPanel.add(exportButton, gbc);
		
		// import and external processing can only be done if
		// panel is editable
		if ( isEditable == false)
			return eiPanel;

		// Add import control
		JButton importButton = new JButton("Import");
		importButton.setActionCommand("import");
		importButton.addActionListener(this);

		gbc.gridx++;
		eiPanel.add(importButton, gbc);
		
		// add external processing command field and button
		gbc.gridy++;
		gbc.gridx = 0;
		eiPanel.add(new JLabel("With command:"), gbc);

		// add combo box for command to execute
		gbc.gridx++;
		externalCommandCombo = new JComboBox(
			CellImportExportInfoSaver.getInstance().getCmdList());
		externalCommandCombo.setSelectedIndex(-1);	// no entry selected
		externalCommandCombo.setEditable(true);
		externalCommandCombo.setPrototypeDisplayValue("xxxxxxxxxxxxxxxxxxxxxxxxxx");
		eiPanel.add(externalCommandCombo, gbc);
		
		// add button to execute external command
		JButton externalCommandButton = new JButton("Execute");
		externalCommandButton.setActionCommand("execute");
		externalCommandButton.addActionListener(this);

		gbc.gridx++;
		eiPanel.add(externalCommandButton, gbc);
		
		// add button for applying file & cmd info without doing anything else
		JButton applyButton = new JButton("Apply File & Cmd");
		applyButton.setActionCommand("apply");
		applyButton.addActionListener(this);

		gbc.gridx++;
		gbc.gridwidth = 2;
		eiPanel.add(applyButton, gbc);	
		gbc.gridwidth = 1;	// reset width to normal	
	
		// add note to user about including file name in command
		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		eiPanel.add(new JLabel("(In command, the string "+FILE_REPLACE_FLAG+
			" is replaced by the file name when Executed.)"), gbc);

		// load filename and command with previously entered info
		// if not the default
		CellImportExportInfo info = 
			CellImportExportInfoSaver.getInstance().get(_colDef.getFullTableColumnName());
		if (info != null) {
			// load the info into the text fields
			fileNameField.setText(info.getFileName());
			externalCommandCombo.getEditor().setItem(info.getCommand());
		}
		
		return eiPanel;
	}
	
	
	/**
	 * Return the contents of the editable text area as an Object
	 * converted by the correct DataType function.  Errors in converting
	 * from the text form into the Object are reported
	 * through the messageBuffer.
	 */
	public Object getObject(StringBuffer messageBuffer) {
		String text = null;
		try {
			text = getTextAreaCannonicalForm();
		}
		catch (Exception e) {
			messageBuffer.append(
				"Failed to convert binary text; error was:\n"+e.getMessage());
			return null;
		}
		return CellComponentFactory.validateAndConvertInPopup(_colDef,
					originalValue, text, messageBuffer);
	}
	
	/**
	 * When focus is passed to this panel, automatically pass it
	 * on to the text area.
	 */
	public void requestFocus() {
		_ta.requestFocus();
	}
	
	/**
	 * Handle actions on the buttons in the file operations panel
	 */
	public void actionPerformed(ActionEvent e) {
		
		//File object for doing IO
		File file;		

		if (e.getActionCommand().equals("browse")) {
			JFileChooser chooser = new JFileChooser();

			int returnVal = chooser.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				//	System.out.println("You chose to open this file: " +
				//		chooser.getSelectedFile().getName());
				try {
					fileNameField.setText(chooser.getSelectedFile().getCanonicalPath());
				}
				catch (Exception ex) {
					// should not happen since the file that was selected was
					// just being shown in the Chooser dialog, but just to be safe...
					JOptionPane.showMessageDialog(this,
						"Error getting full path name for selected file",
						"File Chooser Error",JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		
		else if (e.getActionCommand().equals("apply")) {
			// If file name default and cmd is null or empty,
			// make sure this entry is not being held in CellImportExportInfoSaver
			if ( (fileNameField.getText() != null &&
					fileNameField.getText().equals(TEMP_FILE_FLAG)) &&
				(externalCommandCombo.getEditor().getItem() == null ||
					((String)externalCommandCombo.getEditor().getItem()).length() == 0)) {
				// user has not entered anything or has reset to defaults,
				// so make sure there is no entry for this column in the
				// saved info
				CellImportExportInfoSaver.remove(_colDef.getFullTableColumnName());
			}
			else {
				// user has entered some non-default info, so save it
				CellImportExportInfoSaver.getInstance().save(
					_colDef.getFullTableColumnName(),fileNameField.getText(),
					((String)externalCommandCombo.getEditor().getItem()));
			}
			
		}
 
 		else if (e.getActionCommand().equals("import")) {
 			
 			// IMPORT OBJECT FROM FILE
 			
 			if (fileNameField.getText() == null ||
 				fileNameField.getText().equals(TEMP_FILE_FLAG)) {
 				// not allowed - must have existing file for import
 				JOptionPane.showMessageDialog(this,
						"You must select an existing file to import data from.",
						"No File Selected",JOptionPane.ERROR_MESSAGE);
				return;	// do not do import
 			}

 			// get name of file, which must exist
			if (fileNameField.getText() == null)
				fileNameField.setText("");	// guard against something really stupid
			file = new File(fileNameField.getText());
			if ( ! file.exists() || ! file.isFile() || ! file.canRead()) {
				// not something we can read
				JOptionPane.showMessageDialog(this,
						"File "+fileNameField.getText()+" does not exist,\n"+
						"or is not a readable, normal file.\n"+
						"Please enter a valid file name or use Browse to select a file.",
						"File Name Error",JOptionPane.ERROR_MESSAGE);
				return;	// do not do import
			}
			
			// Now that we have the file, do the import.
			//
			// Note: the sequence of operations is divided into two sections
			// at this point.  The preceeding code ensures that we have
			// a readable file, and the code in the following method call
			// does the import.  The reason for splitting at this point is
			// that the Execute operation needs to do an import, and it will
			// already have the file to do the import from (which is the same
			// as the file it exported into).
			importData(file);

			// save the user options - we know that it is not the default
			// because we do not allow importing from "temp file"
			CellImportExportInfoSaver.getInstance().save(
				_colDef.getFullTableColumnName(), fileNameField.getText(),
				((String)externalCommandCombo.getEditor().getItem()));
	
 		}

		else {
			
			// GET FILE FOR EXPORT & EXTERNAL PROCESSING
			
			String canonicalFilePathName = fileNameField.getText();
			
			// file name verification operations are the same for both
			// export and execute, so do that work here for both.
			//
			// If file name is null or empty, do not proceed
			if (fileNameField.getText() == null ||
				fileNameField.getText().equals("")) {
					
				JOptionPane.showMessageDialog(this,
					"No file name given for export.\n"+
					"Please enter a file name  or use Browse before clicking Export.",
					"Export Error",JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			// create the file to open
			if (fileNameField.getText().equals(TEMP_FILE_FLAG)) {
				// user wants us to create a temp file
				try {
					file = File.createTempFile("squirrel", ".tmp");
					
					// get the real name for use later
					canonicalFilePathName = file.getCanonicalPath();
				}
				catch (Exception ex) {
					JOptionPane.showMessageDialog(this,
						"Cannot create temp file..\n"+
						"Error was:\n"+ex.getMessage(),
						"Export Error",JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
			else {
				//user must have supplied a file name.
				file = new File(fileNameField.getText());
				
				try {
					canonicalFilePathName = file.getCanonicalPath();
				}
				catch (Exception ex) {
					// an error here may mean that the file cannot be
					// reached or has moved or some such.  In any case,
					// the file cannot be used for export.
					JOptionPane.showMessageDialog(this,
						"Cannot access file name "+ fileNameField.getText() +
						".\n"+
						"Aborting export.",
						"Export Error",JOptionPane.ERROR_MESSAGE);
						return;
				}
					
				// see if file exists
				if (file.exists()) {
					if ( ! file.isFile()) {
						JOptionPane.showMessageDialog(this,
							"File is not a normal file.\n"+
							"Cannot do export to a directory or system file.",
							"Export Error",JOptionPane.ERROR_MESSAGE);
						return;
					}
					if ( ! file.canWrite()) {
						JOptionPane.showMessageDialog(this,
							"File is not writeable.\n"+
							"Change file permissions or select a differnt file for export.",
							"Export Error",JOptionPane.ERROR_MESSAGE);
						return;
					}
					// file exists, is normal and is writable, so see if user
					// wants to overwrite contents of file
					int option = JOptionPane.showConfirmDialog(this,
							"File "+ canonicalFilePathName +
							" already exists.\n\nDo you wish to overwrite this file?",
							"File Overwrite Warning", JOptionPane.YES_NO_OPTION);
					if (option != JOptionPane.YES_OPTION) {
						// user does not want to overwrite the file
						
						// we could tell user here that export was canceled,
						// but I don't think its necessary, and that avoids
						// forcing user to do yet another annoying mouse click.
						return;
					}
					// user is ok with overwriting file
					// We do not need to do anything special to overwrite
					// (as opposed to appending) since the OutputString
					// starts at the beginning of the file by default.
				}
				else {
					// file does not already exist, so try to create it
					try {
						if ( ! file.createNewFile()) {
							JOptionPane.showMessageDialog(this,
								"Failed to create file "+canonicalFilePathName+
								".\n"+
								"Change file name or select a differnt file for export.",
								"Export Error",JOptionPane.ERROR_MESSAGE);
							return;
						}
					}
					catch (Exception ex) {
						JOptionPane.showMessageDialog(this,
							"Cannot open file "+canonicalFilePathName+
							".\nError was:\n"+ex.getMessage(),
							"Export Error",JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
			}
			
			// at this point we have an actual file that we can output to,
			// so create the output stream
			// (so that data type objects do not have to).

			// We have done everything we can prior to this point
			// to ensure that the the file is accessible, but it is
			// still possible that an existing file was removed
			// at a bad moment.  Also, the compiler insists that we
			// put this in a try statement
			FileOutputStream outStream;
			try {
				outStream = new FileOutputStream(file);
			}
			catch (Exception ex) {
				JOptionPane.showMessageDialog(this,
					"Cannot find file "+canonicalFilePathName+"\n"+
					"Check file name and re-try export.",
					"Export Error",JOptionPane.ERROR_MESSAGE);
				return;
			}

			// if user did anything other than default, then save
			// their options
			if ( ! fileNameField.getText().equals(TEMP_FILE_FLAG) ||
				(((String)externalCommandCombo.getEditor().getItem()) != null &&
				((String)externalCommandCombo.getEditor().getItem()).length() > 0)) {	
						
				CellImportExportInfoSaver.getInstance().save(
					_colDef.getFullTableColumnName(), fileNameField.getText(),
					((String)externalCommandCombo.getEditor().getItem()));
			}
			
			if (e.getActionCommand().equals("export")) {
				
				// EXPORT OBJECT TO FILE

				if (exportData(file, outStream, canonicalFilePathName) == true) {
								
					// if we get here, then everything worked correctly, so
					// tell user that data was put into file.
					// This is different from the Import strategy
					// because the user may not know the name of the file
					// that was used if they selected the automatic temp file
					// operation, or they may not know what directory the file
					// was actually put into, so this tells them the full file path.
					JOptionPane.showMessageDialog(this,
						"Data Successfully exported to file "+
						canonicalFilePathName+ ".\n",
						"Export Success",JOptionPane.INFORMATION_MESSAGE);			
				}
			}
		
			else if (e.getActionCommand().equals("execute")) {
				
				// EXPORT OBJECT TO FILE, EXECUTE PROGRAM ON IT, IMPORT IT BACK
				
				if (((String)externalCommandCombo.getEditor().getItem()) == null ||
					((String)externalCommandCombo.getEditor().getItem()).length() == 0) {
					// cannot execute a null command
					JOptionPane.showMessageDialog(this,
						"Cannot execute a null command.\n"+
						"Please enter a command in the Command field before clicking on Execute.",
						"Execute Error",JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				// replace any instance of flag in command with file name
				String command = ((String)externalCommandCombo.getEditor().getItem());

				int index;
				while ((index = command.indexOf(FILE_REPLACE_FLAG)) >= 0) {
					command = command.substring(0, index) +
						canonicalFilePathName +
						command.substring(index + FILE_REPLACE_FLAG.length());
				}
				
				// export data to file
				if (exportData(file, outStream, canonicalFilePathName) == false) {
					// bad export - do not proceed with command
					// The exportData() method has already put up a message
					// to the user saying the export failed.
					return;
				}
				
				int commandResult;
				try {
					// execute command
					Process cmdProcess = Runtime.getRuntime().exec(command);
				
					// wait for command to complete
					commandResult = cmdProcess.waitFor();
					
					// check the error stream for a problem
					//
					// This is a bit questionable since it is possible
					// for processes to output something on stderr
					// but continue processing.  But without this, some
					// problems are not seen (e.g. "bad argument" type
					// messages from the process).
					BufferedReader err = new BufferedReader(
						new InputStreamReader(cmdProcess.getErrorStream()));
					
					String errMsg = err.readLine();
					if (errMsg != null) 
						throw new IOException(
							"text on error stream from command starting with:\n"+errMsg);
				}
				catch (Exception ex) {
					JOptionPane.showMessageDialog(this,
						"Error while executing command.\n"+
						"The command was:\n  "+command +
						"\nThe error was:\n "+ex.getMessage(),
						"Execute Error",JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				// check for possibly bad return from child
				if (commandResult != 0) {
					// command returned non-standard value.
					// ask user before proceeding.
					int option = JOptionPane.showConfirmDialog(this,
							"The convention for command returns is that 0 means success,"+
							" but this command returned "+ commandResult +
							".\nDo you wish to import the file contents anyway?",
							"File Overwrite Warning", JOptionPane.YES_NO_OPTION);
					if (option != JOptionPane.YES_OPTION) {
						return;
					}
				}
				
				//import the data back from the same file
				importData(file);

				// If the file was a temp file, delete it now.
				// We assume that Export-only operations want to leave the
				// file in place, but Execute operations just want a temp
				// space to work with and do not want it lying around afterwards.
				file.delete();
			}
		} // end of combined export and execute operations
	}
	
	/**
	 * Function to import data from a file.
	 * This is a separate function because it is called from two places.
	 * One is when the user asks to do an import, and the other is when they
	 * ask to run an external command, which involves importing from the file
	 * after the command is completed.
	 */
	private void importData(File file) {
		// create the imput stream
		// (so that DataType objects don't have to)
		FileInputStream inStream;
		String canonicalFilePathName = fileNameField.getText();
		try {
			inStream = new FileInputStream(file);
				
			// it is handy to have the cannonical path name
			// to show user in error messages.  Since getting
			// that name might involve an IOException, we need
			// to put it inside a try statement.  However,
			// since the file does exist there is no good reason
			// for getting an IOException at this point, but
			// if we get one there is something seriously wrong
			// and we want to abort.  Therefore it make sense
			// to get that name here and save it for later use.
			canonicalFilePathName = file.getCanonicalPath();
		}
		catch (Exception ex) {
			JOptionPane.showMessageDialog(this,
				"There was an error opening file "+canonicalFilePathName+
				".\nThe error was:\n"+ex.getMessage(),
				"File Open Error",JOptionPane.ERROR_MESSAGE);
			return;
		}

		// hand file input stream to DataType object for import
		// Also, handle File IO errors here so that DataType objects
		// do not have to.
		try {
				
			// The DataObject returns a string to put into the
			// popup which can later be converted to the appropriate
			// object type.
			String replacementText =
				CellComponentFactory.importObject(_colDef, inStream);
				
			// since the above did not throw an exception,
			// we now have a good new data object, so
			// change the text area to reflect that new object.
			//
			
			// If the user has selected a non-cannonical Binary format, we need
			// to convert the text appropriately
			if (radixList != null &&
				! (radixList.getSelectedItem().equals("Hex") &&
					showAscii.isSelected() == false) ) {
				// we need to convert to a different format
				int base = 16;	// default to hex
				if (radixList.getSelectedItem().equals("Decimal")) base = 10;
				else if (radixList.getSelectedItem().equals("Octal")) base = 8;
				else if (radixList.getSelectedItem().equals("Binary")) base = 2;
		
				Byte[] bytes = BinaryDisplayConverter.convertToBytes(replacementText,
					16, false);
		
				// return the expected format for this data
				replacementText = BinaryDisplayConverter.convertToString(bytes,
					base, showAscii.isSelected());				
			}

			((RestorableJTextArea)_ta).updateText(replacementText);
		}
		catch (Exception ex) {
			JOptionPane.showMessageDialog(this,
				"There was an error while reading file "+canonicalFilePathName+
				".\nThe error was:\n"+ex.getMessage(),
				"Import Error",JOptionPane.ERROR_MESSAGE);
			return;
		}
			
		// cleanup resources used
		try {
			inStream.close();
		}
		catch (Exception ex) {
			// I cannot think of any reason for doing anything
			// at all here
		}
			
		/*
		 * Issue: After importing the data, we could tell the user that
		 * it has been successfully imported.  This would give good
		 * positive feedback.  However, it would mean that they need
		 * to click on yet another button to dismiss that message.
		 * On the other hand, since the operation is synchronous,
		 * the user cannot proceed to do anything until it is done.
		 * If we assume that import usually works correctly unless
		 * they are shown an error message, then we do not need to
		 * display anything here.
		 */
	}
	
	
	/**
	 * Function to export data to a file.
	 * This is a separate function because it is called from two places.
	 * One is when the user asks to export, and the other is when they
	 * as to run an external command, which involves exporting to file first.
	 */
	private boolean exportData(File file, FileOutputStream outStream,
		String canonicalFilePathName){
		
		// hand file output stream to DataType object for export
		// Also, handle File IO errors here so that DataType objects
		// do not have to.
		try {
				
			// Hand the current text to the DataType object.
			// DataType object is responsible for validating
			// that the text makes sense for this type of object
			// and converting it to the proper form for output.
			// All errors are handled as IOExceptions
			CellComponentFactory.exportObject(_colDef, outStream,
				getTextAreaCannonicalForm());
				
		}
		catch (Exception ex) {
			JOptionPane.showMessageDialog(this,
				"There was an error while writing file "+canonicalFilePathName+
				".\nThe error was:\n"+ex.getMessage(),
				"Export Error",JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		return true;
	}
	
	/**
	 * catch and handle mouse events to put up a menu
	 */
	public void addNotify()
	{
		super.addNotify();
		if (_lis == null)
		{
			_lis = new MouseAdapter()
			{
				public void mousePressed(MouseEvent evt)
				{
					if (evt.isPopupTrigger())
					{
						_popupMenu.show(evt);
					}
				}
				public void mouseReleased(MouseEvent evt)
				{
						if (evt.isPopupTrigger())
					{
						_popupMenu.show(evt);
					}
				}
			};
			_ta.addMouseListener(_lis);
		}
	}

	public void removeNotify()
	{
		super.removeNotify();
		if (_lis != null)
		{
			_ta.removeMouseListener(_lis);
			_lis = null;
		}
	}

	private class LineWrapAction extends BaseAction
	{
		LineWrapAction()
		{
			super("Wrap Lines on/off");
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (_ta != null)
			{
				_ta.setLineWrap(!_ta.getLineWrap());
			}
		}
	}

	private class WordWrapAction extends BaseAction
	{
		WordWrapAction()
		{
			super("Wrap on Word on/off");
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (_ta != null)
			{
				_ta.setWrapStyleWord(!_ta.getWrapStyleWord());
			}
		}
	}
		
		
	/**
	 * Helper function that ensures that the data is acceptable to the DataType object.
	 * The issue addressed here is that Binary data can be represented in multiple
	 * formats (Hex, Octal, etc), and to keep the DataTypes simple we assume that
	 * they get only Hex data with ASCII chars shown as their numeric value.
	 * This makes sense from the point of view that the different formats are
	 * temporary views handled by this class rather than permanent settings
	 * applied to either the column or the DataType.
	 * Therefore, when we pass the data from the TextArea into the DataType,
	 * we may need to do a conversion on the way.
	 */
	private String getTextAreaCannonicalForm() {
		// handle null
		if (_ta.getText() == null ||
			_ta.getText().equals("<null>") ||
			_ta.getText().length() == 0)
			return _ta.getText();
		
		// if the data is not binary, then there is no need for conversion.
		// if the data is Hex with ASCII not shown as chars, then no conversion needed.
		if (radixList == null ||
			(radixList.getSelectedItem().equals("Hex") && ! showAscii.isSelected()) ) {
			// no need for conversion
			return _ta.getText();
		}
			
		// The field is binary and not in the format expected by the DataType
		int base = 16;	// default to hex
		if (radixList.getSelectedItem().equals("Decimal")) base = 10;
		else if (radixList.getSelectedItem().equals("Octal")) base = 8;
		else if (radixList.getSelectedItem().equals("Binary")) base = 2;
		
		// the following can cause and exception if the text is not formatted correctly
		Byte[] bytes = BinaryDisplayConverter.convertToBytes(_ta.getText(),
			base, showAscii.isSelected());
		
		// return the expected format for this data
		return BinaryDisplayConverter.convertToString(bytes, 16, false);
	}
}
