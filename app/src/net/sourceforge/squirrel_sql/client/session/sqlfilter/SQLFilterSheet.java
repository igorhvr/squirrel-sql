package net.sourceforge.squirrel_sql.client.session.sqlfilter;
/*
 * Copyright (C) 2003-2004 Maury Hammel
 * mjhammel@users.sourceforge.net
 *
 * Modifications Copyright (C) 2003-2004 Jason Height
 *
 * Adapted from SessionPropertiesSheet.java by Colin Bell.
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.squirrel_sql.fw.datasetviewer.DataSetException;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.fw.sql.SQLConnection;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.gui.builders.UIFactory;
import net.sourceforge.squirrel_sql.client.gui.session.BaseSessionInternalFrame;
import net.sourceforge.squirrel_sql.client.session.IObjectTreeAPI;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table.ContentsTab;
/**
 * SQLFilter dialog gui.
 * JASON: Rename to SQLFilterInternalFrame
 *
 * @author <A HREF="mailto:mjhammel@users.sourceforge.net">Maury Hammel</A>
 */
public class SQLFilterSheet extends BaseSessionInternalFrame
{
	/**
	 * This interface defines locale specific strings. This should be
	 * replaced with a property file.
	 */
	private interface i18n
	{
		/** Title of the filter */
		String TITLE = "SQL Filter";
	}

	/** Logger for this class. */
	private static final ILogger s_log =
		LoggerController.createLogger(SQLFilterSheet.class);

	/** The object tree we are filtering. */
	private final IObjectTreeAPI _objectTree;

	/** A reference to a class containing information about the database metadata. */
	private final IDatabaseObjectInfo _objectInfo;

	/** A list of panels that make up this sheet. */
	private List _panels = new ArrayList();

	/** A variable that contains a value that indicates which tab currently has focus. */
	private int _tabSelected;

	/** Frame title. */
	private JLabel _titleLbl = new JLabel();

	/** A button used to trigger the clearing of SQL Filter information. */
	private JButton _clearFilter = new JButton();

	/** A reference to a panel for the SQL Where Clause. */
	private WhereClausePanel _whereClausePanel = null;

	/** A reference to a panel for the SQL Order By Clause. */
	private OrderByClausePanel _orderByClausePanel = null;

	/**
	 * Creates a new instance of SQLFilterSheet
	 *
	 * @param	objectTree
	 * @param	objectInfo	The object we are filtering within the object
	 *						tree.
	 */
	public SQLFilterSheet(IObjectTreeAPI objectTree,
							IDatabaseObjectInfo objectInfo)
	{
		super(objectTree.getSession(), i18n.TITLE, true);
		if (objectInfo == null)
		{
			throw new IllegalArgumentException("IDatabaseObjectInfo == null");
		}
		_objectTree = objectTree;
		_objectInfo = objectInfo;

		createGUI();
	}

	/**
	 * Position and display the sheet.
	 *
	 * @param	show	A boolean that determines whether the sheet is shown
	 * 					or hidden.
	 */
	public synchronized void setVisible(boolean show)
	{
		boolean reallyShow = true;

		if (show)
		{
			if (!isVisible())
			{
				ContentsTab tab =(ContentsTab)_objectTree.getTabbedPaneIfSelected(
						DatabaseObjectType.TABLE,
						ContentsTab.TITLE);
				if (tab == null)
				{
					reallyShow = false;
					_objectTree.getSession().getMessageHandler().showMessage(
						"You must have the Contents Tab selected to activate the SQL Filter");
				}
				else
				{
					final boolean isDebug = s_log.isDebugEnabled();
					long start = 0;
					for (Iterator it = _panels.iterator(); it.hasNext();)
					{
						ISQLFilterPanel pnl = (ISQLFilterPanel)it.next();
						if (isDebug)
						{
							start = System.currentTimeMillis();
						}
	
						pnl.initialize(tab.getSQLFilterClauses());
						if (isDebug)
						{
							s_log.debug("Panel " + pnl.getTitle()
									+ " initialized in "
									+ (System.currentTimeMillis() - start) + "ms");
						}
					}
					pack();
					/*
					 * TODO: Find out why
					 * KLUDGE: For some reason, I am not able to get the sheet to
					 * size correctly. It always displays with a size that causes
					 * the sub-panels to have their scrollbars showing. Add a bit
					 * of an increase in the size of the panel so the scrollbars
					 * are not displayed.
					 */
					Dimension d = getSize();
					d.width += 5;
					d.height += 5;
					setSize(d);
					/*
					 * END-KLUDGE
					 */
					GUIUtils.centerWithinDesktop(this);
					moveToFront();
				}
			}
		}

		if (!show || reallyShow)
		{
			super.setVisible(show);
		}
	}

	/**
	 * Set title of this frame. Ensure that the title label matches the frame title.
	 *
	 * @param	title	New title text.
	 */
	public void setTitle(String title)
	{
		_titleLbl.setText(title + ": " + _objectInfo.getSimpleName());
	}

	/**
	 * Dispose of the sheet.
	 */
	private void performClose()
	{
		dispose();
	}

	public IDatabaseObjectInfo getDatabaseObjectInfo()
	{
		return _objectInfo;
	}

	public IObjectTreeAPI getObjectTree()
	{
		return _objectTree;
	}

	/**
	 * OK button pressed. Edit data and if ok save to aliases model and
	 * then close dialog.
	 */
	private void performOk()
	{
		final boolean isDebug = s_log.isDebugEnabled();
		long start = 0;
		for (Iterator it = _panels.iterator(); it.hasNext();)
		{
			ISQLFilterPanel pnl = (ISQLFilterPanel)it.next();
			if (isDebug)
			{
				start = System.currentTimeMillis();
			}
			pnl.applyChanges();
			if (isDebug)
			{
				s_log.debug("Panel " + pnl.getTitle() + " applied changes in "
						+ (System.currentTimeMillis() - start) + "ms");
			}
		}
		try
		{
//			ContentsTab cTab =
//				(ContentsTab)getSession()
//					.getSessionSheet()
//					.getObjectTreePanel()
//					.getTabbedPaneIfSelected(
//						DatabaseObjectType.TABLE,
//						ContentsTab.TITLE);
			ContentsTab cTab =(ContentsTab)_objectTree.getTabbedPaneIfSelected(
												DatabaseObjectType.TABLE,
												ContentsTab.TITLE);
			if (cTab != null)
			{
				cTab.refreshComponent();
			}
		}
		catch (DataSetException ex)
		{
			getSession().getMessageHandler().showErrorMessage(ex);
		}

		dispose();
	}

	/**
	 * Create the GUI elements for the sheet.
	 */
	private void createGUI()
	{
		SortedSet columnNames = new TreeSet();
		Map textColumns = new TreeMap();

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(getTitle());

		// This is a tool window.
		GUIUtils.makeToolWindow(this, true);

		try
		{
			SQLConnection sqlConnection = getSession().getSQLConnection();
			ResultSet rs =
				sqlConnection.getSQLMetaData().getColumns((ITableInfo)_objectInfo);
			while (rs.next())
			{
				columnNames.add(rs.getString("COLUMN_NAME"));
				int dataType = rs.getInt("DATA_TYPE");

				if ((dataType == Types.CHAR)
					|| (dataType == Types.CLOB)
					|| (dataType == Types.LONGVARCHAR)
					|| (dataType == Types.VARCHAR))
				{
					textColumns.put(
						rs.getString("COLUMN_NAME"),
						new Boolean(true));
				}
			}
		}
		catch (SQLException ex)
		{
			getSession().getApplication().showErrorDialog(
				"Unable to get list of columns, " + ex);
		}

		_whereClausePanel =
			new WhereClausePanel(columnNames, textColumns, _objectInfo.getQualifiedName());
		_orderByClausePanel =
			new OrderByClausePanel(columnNames, _objectInfo.getQualifiedName());
		_panels.add(_whereClausePanel);
		_panels.add(_orderByClausePanel);

		JTabbedPane tabPane = UIFactory.getInstance().createTabbedPane();
		for (Iterator it = _panels.iterator(); it.hasNext();)
		{
			ISQLFilterPanel pnl = (ISQLFilterPanel)it.next();
			String pnlTitle = pnl.getTitle();
			String hint = pnl.getHint();
			final JScrollPane sp = new JScrollPane(pnl.getPanelComponent());
			sp.setBorder(BorderFactory.createEmptyBorder());
			tabPane.addTab(pnlTitle, null, sp, hint);
		}

		tabPane.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent event)
			{
				setButtonLabel(
					((JTabbedPane)event.getSource()).getSelectedIndex());
			}
		});

		final JPanel contentPane = new JPanel(new GridBagLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		setContentPane(contentPane);

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridwidth = 1;

		gbc.gridx = 0;
		gbc.gridy = 0;

		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		contentPane.add(_titleLbl, gbc);

		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = GridBagConstraints.REMAINDER;
		setButtonLabel(0);
		_tabSelected = 0;
		_clearFilter.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				clearFilter();
			}
		});
		contentPane.add(_clearFilter);

		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 2;
		gbc.gridx = 0;
		++gbc.gridy;
		gbc.weighty = 1;
		contentPane.add(tabPane, gbc);

		++gbc.gridy;
		gbc.gridwidth = 2;
		gbc.weighty = 0;
		contentPane.add(createButtonsPanel(), gbc);
	}

	/**
	 * Clear out the SQL Filter information for the appropriate tab.
	 */
	private void clearFilter()
	{
		if (_tabSelected == 0)
		{
			_whereClausePanel.clearFilter();
		}
		else
		{
			_orderByClausePanel.clearFilter();
		}
	}

	/**
	 * Create a panel that contains the buttons that control the closing
	 * of the sheet.
	 *
	 * @return An instance of a JPanel.
	 */
	private JPanel createButtonsPanel()
	{
		JPanel pnl = new JPanel();

		JButton okBtn = new JButton("OK");
		okBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				performOk();
			}
		});
		JButton closeBtn = new JButton("Close");
		closeBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				performClose();
			}
		});

		pnl.add(okBtn);
		pnl.add(closeBtn);

		GUIUtils.setJButtonSizesTheSame(new JButton[] { okBtn, closeBtn });
		getRootPane().setDefaultButton(okBtn);

		return pnl;
	}

	/**
	 * Change the text of the 'clear' button depending on which
	 * clause panel has focus.
	 *
	 * @param	tabSelected	An integer indicating which panel has focus
	 */
	private void setButtonLabel(int tabSelected)
	{
		_clearFilter.setText(
			"Clear "
				+ ((tabSelected == 0)
					? _whereClausePanel.getTitle()
					: _orderByClausePanel.getTitle()));
		_tabSelected = tabSelected;
	}
}
