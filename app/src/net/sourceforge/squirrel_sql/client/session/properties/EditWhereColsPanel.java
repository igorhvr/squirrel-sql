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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.SortedSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.sourceforge.squirrel_sql.client.session.ISession;

/**
 * This panel allows the user to select specific columns from a specific table for use in
 * the WHERE clause when editing a cell in a table.  This is useful if the table has a large number
 * of columns and the WHERE clause generated using all the columns exceeds the DBMS limit.
 */
public class EditWhereColsPanel
{
	/** The actual GUI panel that allows user to do the maintenance. */
	private EditWhereColsSubPanel _myPanel;
	
	/**
	 * Create a new instance of a WhereClausePanel.
	 *
	 * @param	columnList	A list of column names for the database table.
	 * @param	textColumns	A collection of column names that are "text"
	 * 						columns.
	 * @param	tableName	The name of the database table that the filter
	 * 						information will apply to.
	 *
	 * @throws	IllegalArgumentException
	 *			The exception thrown if invalid arguments are passed.
	 */
	public EditWhereColsPanel(SortedSet columnList, Map textColumns, 
							String tableName)
		throws IllegalArgumentException
	{
		super();
		_myPanel = new EditWhereColsSubPanel(columnList, textColumns, tableName);
	}

	/**
	 * Initialize the components of the WhereClausePanel.
	 *
	 * @param	sqlFilterClauses	An instance of a class containing information
	 *								about SQL filters already in place for the table.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if an invalid argument is passed.
	 */
	public void initialize(ISession session)
		throws IllegalArgumentException
	{
//????????????
//????		_myPanel.loadData(_sqlFilterClauses);
	}

	/**
	 * Returns the panel created by the class.
	 *
	 * @return Return an instance of a WhereClauseSubPanel.
	 */
	public Component getPanelComponent()
	{
		return _myPanel;
	}

	/**
	 * Get the title of the panel.
	 *
	 * @return	Return a string containing the title of the panl.
	 */
	public String getTitle()
	{
		return EditWhereColsSubPanel.EditWhereColsSubPanelI18n.WHERE_CLAUSE;
	}

	/**
	 * Get the hint text associated with the panel.
	 *
	 * @return A String value containing the hint text associated with the panel.
	 */
	public String getHint()
	{
		return EditWhereColsSubPanel.EditWhereColsSubPanelI18n.HINT;
	}

	/**
	 * Update the current session with any changes to the SQL filter
	 * information.
	 */
	public void applyChanges()
	{
		_myPanel.applyChanges();
	}

	/**
	 * A private class that makes up the bulk of the GUI for the panel.
	 */
	private static final class EditWhereColsSubPanel extends JPanel
	{
		/**
		 * This interface defines locale specific strings. This should be
		 * replaced with a property file.
		 */
		interface EditWhereColsSubPanelI18n
		{
			String COLUMNS = "Columns";
			String OPERATORS = "Operators";
			String VALUE = "Value";
			String WHERE_CLAUSE = "Where Clause";
			String HINT = "Where clause for the selected table";
			String ADD = "Add";
			String AND = "AND";
			String OR = "OR";
			String LIKE = "LIKE";
			String IN = "IN";
			String IS_NULL = "IS NULL";
			String IS_NOT_NULL = "IS NOT NULL";
		}

		/**
		 * A JComboBox component containing a list of the names of the
		 * columns for the current table.
		 */
		private JComboBox _columnCombo;

		/** A label to identify the column combo box. */
		private JLabel _columnLabel = new JLabel(EditWhereColsSubPanelI18n.COLUMNS);

		/**
		 * A JComboBox containing a list of valid operators used in SQL Where clause
		 * expressions.
		 */
		private OperatorTypeCombo _operatorCombo = new OperatorTypeCombo();

		/** A label to identify the operator combo box. */
		private JLabel _operatorLabel = new JLabel(EditWhereColsSubPanelI18n.OPERATORS);

		/** A field used to enter the right-hand side of a WhereClause expression. */
		private JTextField _valueField = new JTextField(10);

		/** A label to identify the valueField text area. */
		private JLabel _valueLabel = new JLabel(EditWhereColsSubPanelI18n.VALUE);

		/** A JComboBox used to list Where clause connectors. */
		private AndOrCombo _andOrCombo = new AndOrCombo();

		/** A label to identify the andor combo box. */
		private JLabel _andOrLabel = new JLabel(" ");

		/** A text area used to contain all of the information for the Where clause. */
		private JTextArea _whereClauseArea = new JTextArea(10, 40);

		/**
		 * A button used to add information from the combo boxes and text fields into the
		 * Where clause text area.
		 */
		private JButton _addTextButton = new JButton(EditWhereColsSubPanelI18n.ADD);

		/** The name of the database table the Where clause applies to. */
		private String _tableName;

		/** A List containing the names of the text columns */
		private Map _textColumns;

		/**
		 * A JPanel used for a bulk of the GUI elements of the panel.
		 *
		 * @param	columnList	A list of the column names for the table.
		 * @param	tableName	The name of the database table.
		 */
		EditWhereColsSubPanel(SortedSet columnList, Map textColumns,
								String tableName)
		{
			super();
			_tableName = tableName;
			_columnCombo = new JComboBox(columnList.toArray());
			_textColumns = textColumns;
			createGUI();
		}

		/**
		 * Load existing clause information into the panel.
		 *
		 * @param	sqlFilterClauses	An instance of a class containing
		 * 								SQL Filter information for the current table.
		 *
		 */
		void loadData()
		{
//?????			_whereClauseArea.setText(
//?????				sqlFilterClauses.get(getClauseIdentifier(), _tableName));
		}

		/** Update the current SQuirreL session with any changes to the SQL filter
		 * information.
		 * @param sqlFilterClauses An instance of a class containing SQL Filter information for the current table.
		 *
		 */
		void applyChanges()
		{
//????			sqlFilterClauses.put(
//????				getClauseIdentifier(),
//????				_tableName,
//?????				_whereClauseArea.getText());
		}

		/**
		 * Create the GUI elements for the panel.
		 */
		private void createGUI()
		{
			setLayout(new GridBagLayout());
			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;

			gbc.gridx = 0;
			gbc.gridy = 0;
			add(createGeneralPanel(), gbc);
		}

		/**
		 * Create a JPanel with GUI components.
		 *
		 * @return Returns a JPanel
		 */
		private JPanel createGeneralPanel()
		{
			final JPanel pnl = new JPanel(new GridBagLayout());

			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new Insets(4, 4, 4, 4);
			gbc.weightx = 1.0;

			gbc.fill = GridBagConstraints.NONE;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 1;
			JPanel andOrPanel = new JPanel();
			andOrPanel.setLayout(new BoxLayout(andOrPanel, BoxLayout.Y_AXIS));
			_andOrLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			andOrPanel.add(_andOrLabel);
			_andOrCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
			andOrPanel.add(_andOrCombo);
			pnl.add(andOrPanel, gbc);

			gbc.gridx++;
			gbc.gridwidth = 5;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			JPanel columnPanel = new JPanel();
			columnPanel.setLayout(new BoxLayout(columnPanel, BoxLayout.Y_AXIS));
			_columnLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			columnPanel.add(_columnLabel);
			_columnCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
			columnPanel.add(_columnCombo);
			pnl.add(columnPanel, gbc);

			gbc.gridx += 5;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.NONE;
			JPanel operatorPanel = new JPanel();
			operatorPanel.setLayout(
				new BoxLayout(operatorPanel, BoxLayout.Y_AXIS));
			_operatorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			operatorPanel.add(_operatorLabel);
			_operatorCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
			operatorPanel.add(_operatorCombo);
			pnl.add(operatorPanel, gbc);

			gbc.gridx++;
			gbc.gridwidth = 1;
			JPanel valuePanel = new JPanel();
			valuePanel.setLayout(new BoxLayout(valuePanel, BoxLayout.Y_AXIS));
			_valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			valuePanel.add(_valueLabel);
			valuePanel.add(Box.createRigidArea(new Dimension(5, 5)));
			_valueField.setAlignmentX(Component.LEFT_ALIGNMENT);
			valuePanel.add(_valueField);
			pnl.add(valuePanel, gbc);

			gbc.gridx++;
			_addTextButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					addTextToClause();
				}
			});
			pnl.add(_addTextButton, gbc);

			gbc.gridy++; // new line
			gbc.gridx = 0;
			gbc.gridwidth = 9;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.ipady = 4;
			_whereClauseArea.setBorder(BorderFactory.createEtchedBorder());
			_whereClauseArea.setLineWrap(true);
			JScrollPane sp = new JScrollPane(_whereClauseArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
												JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			pnl.add(sp, gbc);

			return pnl;
		}

		private static final class OperatorTypeCombo extends JComboBox
		{
			OperatorTypeCombo()
			{
				addItem("=");
				addItem("<>");
				addItem(">");
				addItem("<");
				addItem(">=");
				addItem("<=");
				addItem(EditWhereColsSubPanelI18n.IN);
				addItem(EditWhereColsSubPanelI18n.LIKE);
				addItem(EditWhereColsSubPanelI18n.IS_NULL);
				addItem(EditWhereColsSubPanelI18n.IS_NOT_NULL);
			}
		}

		private static final class AndOrCombo extends JComboBox
		{
			AndOrCombo()
			{
				addItem(EditWhereColsSubPanelI18n.AND);
				addItem(EditWhereColsSubPanelI18n.OR);
			}
		}

		/**
		 * Combine the information entered in the combo boxes
		 * and the text field and add it to the Where clause information.
		 */
		private void addTextToClause()
		{
			String value = (String)_valueField.getText();
			String operator = (String)_operatorCombo.getSelectedItem();
			if (((value != null) && (value.length() > 0))
					|| ((operator.equals(EditWhereColsSubPanelI18n.IS_NULL))
					|| 	(operator.equals(EditWhereColsSubPanelI18n.IS_NOT_NULL))))
			{
				String andOr = (String)_andOrCombo.getSelectedItem();
				String column = (String)_columnCombo.getSelectedItem();

				// Put the 'AND' or the 'OR' in front of the clause if
				// there are already values in the text area.
				if (_whereClauseArea.getText().length() > 0)
				{
					_whereClauseArea.append("\n" + andOr + " ");
				}

				// If the operator is 'IN' and there are no parenthesis
				// around the value, put them there.
				if (operator.equals(EditWhereColsSubPanelI18n.IN)
					&& (!value.trim().startsWith("(")))
				{
					value = "(" + value + ")";
				}

				// If the column is a text column, and there aren't single quotes around the value, put them there.

				else if ((value != null) && (value.length() > 0)) 
				{
					if (_textColumns.containsKey(column)
							&& (!value.trim().startsWith("'")))
					{
						value = "'" + value + "'";
					}
				}
				_whereClauseArea.append(column + " " + operator);

				if ((value != null) && (value.length() > 0)) 
				{
					_whereClauseArea.append(" " + value);
				}
			}
			_valueField.setText("");
		}

		/**
		 * Erase all information for the current filter.
		 */
		public void clearFilter()
		{
//?? currently ignored; this should reset the lists of fields to the original values
		}
	}

	/**
	 * Erase any information for the appropriate filter.
	 */
	public void clearFilter()
	{
		_myPanel.clearFilter();
	}

	/**
	 * Get a value that uniquely identifies this SQL filter clause.
	 *
	 * @return Return a String value containing an identifing value.
	 */
	public static String getClauseIdentifier()
	{
		return EditWhereColsSubPanel.EditWhereColsSubPanelI18n.WHERE_CLAUSE;
	}
}