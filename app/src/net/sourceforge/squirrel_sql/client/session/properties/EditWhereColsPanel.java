package net.sourceforge.squirrel_sql.client.session.properties;
/*
 *
 * Adapted from WhereClausePanel.java by Maury Hammel.
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
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.JOptionPane;


/**
 * This panel allows the user to select specific columns from a specific table for use in
 * the WHERE clause when editing a cell in a table.  This is useful if the table has a large number
 * of columns and the WHERE clause generated using all the columns exceeds the DBMS limit.
 */
public class EditWhereColsPanel extends JPanel
{
	/** The name of the database table the Where clause applies to. */
	private String _tableName;
	
	/** The name of the table including the URL **/
	private String _unambiguousTableName;
	
	/** The list of all possible columns in the table **/
	private SortedSet _columnList;
	
	/** The list of "to use" column names as seen by the user **/
	private JList useColsList;
	
	/** The list of "to NOT use" column names as seen by the user **/
	private JList notUseColsList;
	
	/** The list of column names to use as calculated when window is created **/
	private Object[] initalUseColsArray;
	
	/** The list of column names to NOT use as calculated when window is created **/
	private Object[] initalNotUseColsArray;

	/**
	 * ?? this should be changed to use the I18N file mechanism.
	 */
	interface EditWhereColsPanelI18N {
		String TITLE = "Limit Columns in Cell Edit";
		String HINT = "Limit columns used in WHERE clause when editing table ";
	}
	
	/**
	 * Create a new instance of a WhereClausePanel.
	 *
	 * @param	columnList	A list of column names for the database table.
	 * @param	tableName	The name of the database table that the filter
	 * 						information will apply to.
	 * @param unambiguousTableName The name of the table including the URL 
	 * 				to the specific DBMS
	 *
	 * @throws	IllegalArgumentException
	 *			The exception thrown if invalid arguments are passed.
	 */
	public EditWhereColsPanel(SortedSet columnList, 
							String tableName, String unambiguousTableName)
		throws IllegalArgumentException
	{
		super();

		// save the input for use later
		_columnList = columnList;
		_tableName = tableName;
		_unambiguousTableName = unambiguousTableName;
		
		// look up the table in the EditWhereCols list
		HashMap colsTable = EditWhereCols.get(unambiguousTableName);
		
		if (colsTable == null) {
			// use all of the columns
			initalUseColsArray = _columnList.toArray();
			initalNotUseColsArray = new Object[0];
		}
		else {
			// use just the columns listed in the table, and set the not-used cols to the ones
			// that are not mentioned in the table
			SortedSet initialUseColsSet = new TreeSet( );
			SortedSet initialNotUseColsList = new TreeSet();
			
			Iterator it = _columnList.iterator();
			while (it.hasNext()) {
				Object colName = it.next();
				if (colsTable.get(colName) != null)
					initialUseColsSet.add(colName);
				else initialNotUseColsList.add(colName);
			}
			initalUseColsArray = initialUseColsSet.toArray();
			initalNotUseColsArray = initialNotUseColsList.toArray();
		}

		// create all of the gui objects now
		createGUI();
	}


	/**
	 * Get the title of the panel.
	 *
	 * @return	Return a string containing the title of the panl.
	 */
	public String getTitle()
	{
		return EditWhereColsPanelI18N.TITLE;
	}

	/**
	 * Get the hint text associated with the panel.
	 *
	 * @return A String value containing the hint text associated with the panel.
	 */
	public String getHint()
	{
		return EditWhereColsPanelI18N.HINT;
	}

	/**
	 * Reset the panel to the contents at the time we started editing
	 * (as set in initialize).
	 * 
	 */
	public void reset() {	
		useColsList.setListData(initalUseColsArray);
		notUseColsList.setListData(initalNotUseColsArray);
	}
	
	/**
	 * Put the current data into the EditWhereCols table.
	 */
	public boolean ok() {
		
		// if all cols are in the "to use" side, delete from EditWhereCols
		if (notUseColsList.getModel().getSize() == 0) {
			EditWhereCols.put(_unambiguousTableName, null);
		}
		else {
			// some cols are not to be used
			ListModel useColsModel = useColsList.getModel();
			
			// do not let user remove everything from the list
			if (useColsModel.getSize() == 0) {
				JOptionPane.showMessageDialog(this,
					"You cannot remove all of the fields from the 'use columns' list.");
				return false;
			}
			
			// create the HashMap of names to use and put it in EditWhereCols
			HashMap useColsMap = new HashMap(useColsModel.getSize());
			
			for (int i=0; i< useColsModel.getSize(); i++) {
				useColsMap.put(useColsModel.getElementAt(i), useColsModel.getElementAt(i));
			}
			
			EditWhereCols.put(_unambiguousTableName, useColsMap);
		}
		return true;
	}
	
	/**
	 * Move selected fields from "used" to "not used"
	 */
	private void moveToNotUsed() {
		
		// get the values from the "not use" list and convert to sorted set
		ListModel notUseColsModel = notUseColsList.getModel();
		SortedSet notUseColsSet = new TreeSet();
		for (int i=0; i<notUseColsModel.getSize(); i++)
			notUseColsSet.add(notUseColsModel.getElementAt(i));
		
		// get the values from the "use" list
		ListModel useColsModel = useColsList.getModel();
		
		// create an empty set for the "use" list
		SortedSet useColsSet = new TreeSet();

		// for each element in the "use" set, if selected then add to "not use",
		// otherwise add to new "use" set
		for (int i=0; i<useColsModel.getSize(); i++) {
			Object colName = useColsModel.getElementAt(i);
			if (useColsList.isSelectedIndex(i))
				notUseColsSet.add(colName);
			else useColsSet.add(colName);
		}
		
		useColsList.setListData(useColsSet.toArray());
		notUseColsList.setListData(notUseColsSet.toArray());
	}
	
	/**
	 * Move selected fields from "not used" to "used"
	 */
	private void moveToUsed() {
		// get the values from the "use" list and convert to sorted set
		ListModel useColsModel = useColsList.getModel();
		SortedSet useColsSet = new TreeSet();
		for (int i=0; i<useColsModel.getSize(); i++)
			useColsSet.add(useColsModel.getElementAt(i));
		
		// get the values from the "not use" list
		ListModel notUseColsModel = notUseColsList.getModel();
		
		// create an empty set for the "not use" list
		SortedSet notUseColsSet = new TreeSet();

		// for each element in the "not use" set, if selected then add to "use",
		// otherwise add to new "not use" set
		for (int i=0; i<notUseColsModel.getSize(); i++) {
			Object colName = notUseColsModel.getElementAt(i);
			if (notUseColsList.isSelectedIndex(i))
				useColsSet.add(colName);
			else notUseColsSet.add(colName);
		}
		
		useColsList.setListData(useColsSet.toArray());
		notUseColsList.setListData(notUseColsSet.toArray());
	}
	
	
	/**
	 * Create the GUI elements for the panel.
	 */
	private void createGUI()
	{

		JPanel useColsPanel = new JPanel(new BorderLayout());
		useColsPanel.add(new JLabel("Use Columns"), BorderLayout.NORTH);
		useColsList = new JList(initalUseColsArray);
		JScrollPane scrollPane = new JScrollPane(useColsList);
		scrollPane.setPreferredSize(new Dimension(200, 200));
		useColsPanel.add(scrollPane, BorderLayout.SOUTH);
		add(useColsPanel);

		JPanel moveButtonsPanel = new JPanel();
		JPanel buttonPanel = new JPanel(new BorderLayout());
//????? if desired, get fancy and use icons in buttons instead of text ?????????
		JButton moveToNotUsedButton = new JButton("=>");
		moveToNotUsedButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				moveToNotUsed();
			}
 		});
		buttonPanel.add(moveToNotUsedButton, BorderLayout.NORTH);
		JButton moveToUsedButton = new JButton("<=");
		moveToUsedButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				moveToUsed();
			}
			});
		buttonPanel.add(moveToUsedButton, BorderLayout.SOUTH);

		moveButtonsPanel.add(buttonPanel, BorderLayout.CENTER);
		add(moveButtonsPanel);
	  
		JPanel notUseColsPanel = new JPanel(new BorderLayout());
		notUseColsPanel.add(new JLabel("Not Use Columns"), BorderLayout.NORTH);
		notUseColsList = new JList(initalNotUseColsArray);
 		JScrollPane notUseScrollPane = new JScrollPane(notUseColsList);
		notUseScrollPane.setPreferredSize(new Dimension(200, 200));
		notUseColsPanel.add(notUseScrollPane, BorderLayout.SOUTH);
		add(notUseColsPanel);
	}
}