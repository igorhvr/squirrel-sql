package net.sourceforge.squirrel_sql.fw.gui;
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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;

import java.awt.print.PrinterJob;

import javax.swing.*;

import net.sourceforge.squirrel_sql.fw.gui.action.*;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.datasetviewer.IDataSetUpdateableModel;
import net.sourceforge.squirrel_sql.fw.datasetviewer.DataSetViewerTablePanel;


public class TablePopupMenu extends BasePopupMenu
{
	/** Internationalized strings for this class. */
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(TablePopupMenu.class);


	public interface IOptionTypes
	{
		int COPY = 0;
		int COPY_WITH_HEADERS = 1;
		int COPY_HTML = 2;
		int COPY_IN_STATEMENT = 3;
		int COPY_WHERE_STATEMENT = 4;
		int COPY_UPDATE_STATEMENT = 5;
      int COPY_INSERT_STATEMENT = 6;
		int EXPORT_CSV = 7;
		int SELECT_ALL = 8;
      int ADJUST_ALL_COL_WIDTHS_ACTION = 9;
      int ALWAYS_ADJUST_ALL_COL_WIDTHS_ACTION = 10;
      int SHOW_ROW_NUMBERS = 11;
		int LAST_ENTRY = 12;
   }

	private static final KeyStroke COPY_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK);

	private final JMenuItem[] _menuItems = new JMenuItem[IOptionTypes.LAST_ENTRY + 1];

	private JTable _table;

	private JCheckBoxMenuItem _alwaysAdjustAllColWidtshActionItem;
	private JCheckBoxMenuItem _showRowNumbersItem;


	private CopyAction _copy = new CopyAction();
	private CopyWithHeadersAction _copyWithHeaders = new CopyWithHeadersAction();
	private CopyHtmlAction _copyHtml = new CopyHtmlAction();
	private CopyInStatementAction _copyInStatement = new CopyInStatementAction();
	private CopyWhereStatementAction _copyWhereStatement = new CopyWhereStatementAction();
	private CopyUpdateStatementAction _copyUpdateStatement = new CopyUpdateStatementAction();
	private CopyInsertStatementAction _copyInsertStatement = new CopyInsertStatementAction();
	private ExportCsvAction _exportCvs = new ExportCsvAction();
   private AdjustAllColWidthsAction _adjustAllColWidthsAction = new AdjustAllColWidthsAction();
	private AlwaysAdjustAllColWidthsAction _alwaysAdjustAllColWidthsAction = new AlwaysAdjustAllColWidthsAction();
   private ShowRowNumbersAction _showRowNumbersAction = new ShowRowNumbersAction();

   private MakeEditableAction _makeEditable = new MakeEditableAction();
	private UndoMakeEditableAction _undoMakeEditable = new UndoMakeEditableAction();
	private DeleteRowsAction _deleteRows = new DeleteRowsAction();
	private InsertRowAction _insertRow = new InsertRowAction();
	private SelectAllAction _select = new SelectAllAction();
	private PrintAction _print = new PrintAction();


	// The following pointer is needed to allow the "Make Editable button
	// to tell the application to set up an editable display panel
	private IDataSetUpdateableModel _updateableModel = null;

	// pointer to the viewer
	// This is needed for insert and delete operations
	private DataSetViewerTablePanel _viewer = null;

	/**
	 * Constructor used when caller wants to be able to make table editable.
	 * We need both parameters because there is at least one case where the
	 * underlying data model is updateable, but we do not want to allow the
	 * user to enter editing mode because they are already in edit mode.
	 * The caller needs to determine whether or not to allow a request for edit mode.
	 */
	public TablePopupMenu(boolean allowEditing,
								 IDataSetUpdateableModel updateableModel,
								 DataSetViewerTablePanel viewer)
	{
		super();
		// save the pointer needed to enable editing of data on-demand
		_updateableModel = updateableModel;

		// save the pointer needed for insert and delete operations
		_viewer = viewer;

		// add the menu items to the menu
		_menuItems[IOptionTypes.COPY] = add(_copy);
		_menuItems[IOptionTypes.COPY].setAccelerator(COPY_STROKE);
		_menuItems[IOptionTypes.COPY_WITH_HEADERS] = add(_copyWithHeaders);
		_menuItems[IOptionTypes.COPY_HTML] = add(_copyHtml);
		_menuItems[IOptionTypes.COPY_IN_STATEMENT] = add(_copyInStatement);
		_menuItems[IOptionTypes.COPY_WHERE_STATEMENT] = add(_copyWhereStatement);
		_menuItems[IOptionTypes.COPY_UPDATE_STATEMENT] = add(_copyUpdateStatement);
		_menuItems[IOptionTypes.COPY_INSERT_STATEMENT] = add(_copyInsertStatement);
      addSeparator();
		_menuItems[IOptionTypes.EXPORT_CSV] = add(_exportCvs);
      addSeparator();
      _menuItems[IOptionTypes.ADJUST_ALL_COL_WIDTHS_ACTION] = add(_adjustAllColWidthsAction);

      _alwaysAdjustAllColWidtshActionItem = new JCheckBoxMenuItem();
		_alwaysAdjustAllColWidtshActionItem.setSelected(ButtonTableHeader.isAlwaysAdjustAllColWidths());
		_alwaysAdjustAllColWidtshActionItem.setAction(_alwaysAdjustAllColWidthsAction);
      _menuItems[IOptionTypes.ALWAYS_ADJUST_ALL_COL_WIDTHS_ACTION] = add(_alwaysAdjustAllColWidtshActionItem);

      addSeparator();
      
      _showRowNumbersItem = new JCheckBoxMenuItem();
		_showRowNumbersItem.setSelected(false);
		_showRowNumbersItem.setAction(_showRowNumbersAction);
		_menuItems[IOptionTypes.SHOW_ROW_NUMBERS] = add(_showRowNumbersItem);


		if (allowEditing)
		{
			addSeparator();
			add(_makeEditable);
		}
//		if  ( ! allowEditing )
		if (updateableModel != null && updateableModel.editModeIsForced())
		{
			add(_undoMakeEditable);
		}
		addSeparator();
		_menuItems[IOptionTypes.SELECT_ALL] = add(_select);

		// add entries for insert and delete rows
		// only if table is updateable and already editable (ie. allowEditing is false)
		if (_updateableModel != null && allowEditing==false) {
			addSeparator();
			add(_insertRow);
			add(_deleteRows);
		}

		addSeparator();
		add(_print);
	}

	/**
	 * Constructor used when creating menu for use in cell editor.
	 */
	public TablePopupMenu(IDataSetUpdateableModel updateableModel,
								 DataSetViewerTablePanel viewer, JTable table)
	{
		super();
		// save the pointer needed to enable editing of data on-demand
		_updateableModel = updateableModel;

		// save the pointer needed for insert and delete operations
		_viewer = viewer;

		_table = table;
		replaceStandardTableCopyAction();

// Cut and Paste need to be worked on, so for now do not include them
// Also, the copy operations do not seem to work right - we may need special
//    versions for the cellEditor menu.
//		add(_cut);
//		add(_copy);
//		add(_copyHtml);
//		add(_paste);
//		addSeparator();
		add(_select);
		addSeparator();
		add(_insertRow);
		add(_deleteRows);

		addSeparator();
		add(_print);
	}

	public void setTable(JTable value)
	{
		_table = value;
		replaceStandardTableCopyAction();
	}

	public void reset()
	{
		_showRowNumbersItem.setSelected(false);
	}


	private void replaceStandardTableCopyAction()
	{
		_table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(COPY_STROKE, "CopyAction");
		_table.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(COPY_STROKE, "CopyAction");
		_table.getInputMap(JComponent.WHEN_FOCUSED).put(COPY_STROKE, "CopyAction");
		_table.getActionMap().put("CopyAction", _copy);
	}

	/**
	 * Show the menu.
	 */
	public void show(Component invoker, int x, int y)
	{
		super.show(invoker, x, y);
	}

	public void show(MouseEvent evt)
	{
		super.show(evt);
	}


   private String getStatementSeparatorFromModel()
   {
      return _updateableModel.getDataModelImplementationDetails().getStatementSeparator();
   }


   private class CopyAction extends BaseAction
	{
		CopyAction()
		{
			super(s_stringMgr.getString("TablePopupMenu.copy"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (_table != null)
			{
				new TableCopyCommand(_table, false).execute();
			}
		}
	}

	private class CopyWithHeadersAction extends BaseAction
	{
		CopyWithHeadersAction()
		{
			super(s_stringMgr.getString("TablePopupMenu.copyWithHeaders"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (_table != null)
			{
				new TableCopyCommand(_table, true).execute();
			}
		}
	}



	private class CopyHtmlAction extends BaseAction
	{
		CopyHtmlAction()
		{
			super(s_stringMgr.getString("TablePopupMenu.copyashtml"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (_table != null)
			{
				new TableCopyHtmlCommand(_table).execute();
			}
		}
	}

	private class CopyInStatementAction extends BaseAction
	{
		CopyInStatementAction()
		{
			super(s_stringMgr.getString("TablePopupMenu.copyasinstatement"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (_table != null)
			{
				new TableCopyInStatementCommand(_table).execute();
			}
		}
	}

	private class CopyWhereStatementAction extends BaseAction
	{
		CopyWhereStatementAction()
		{
			super(s_stringMgr.getString("TablePopupMenu.copyaswherestatement"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (_table != null)
			{
				new TableCopyWhereStatementCommand(_table).execute();
			}
		}
	}

	private class CopyUpdateStatementAction extends BaseAction
	{
		CopyUpdateStatementAction()
		{
			super(s_stringMgr.getString("TablePopupMenu.copyasupdatestatement"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (_table != null)
			{
				new TableCopyUpdateStatementCommand(_table).execute();
			}
		}
	}

	private class CopyInsertStatementAction extends BaseAction
	{
		CopyInsertStatementAction()
		{
			super(s_stringMgr.getString("TablePopupMenu.copyasinsertstatement"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (_table != null)
			{
				new TableCopyInsertStatementCommand(_table, getStatementSeparatorFromModel()).execute();
			}
		}

   }

   private class ExportCsvAction extends BaseAction
   {
      ExportCsvAction()
      {
         // i18n[TablePopupMenu.export=Export CSV / MS Excel ...]
         super(s_stringMgr.getString("TablePopupMenu.export"));
      }

      public void actionPerformed(ActionEvent evt)
      {
         if (_table != null)
         {
            new TableExportCsvCommand(_table).execute();
         }
      }
   }


   private class AdjustAllColWidthsAction extends BaseAction
	{
		AdjustAllColWidthsAction()
		{
			super(s_stringMgr.getString("TablePopupMenu.adoptAllColWidthsAction"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (_table != null)
			{
            if(_table.getTableHeader() instanceof ButtonTableHeader)
            {
               ((ButtonTableHeader)_table.getTableHeader()).adjustAllColWidths(true);
            }
         }
		}
	}


   private class AlwaysAdjustAllColWidthsAction extends BaseAction
	{
		AlwaysAdjustAllColWidthsAction()
		{
			super(s_stringMgr.getString("TablePopupMenu.alwaysAdoptAllColWiths"));
		}

		public void actionPerformed(ActionEvent evt)
		{
         ButtonTableHeader.setAlwaysAdjustAllColWidths(_alwaysAdjustAllColWidtshActionItem.isSelected());
         if (_table != null && _alwaysAdjustAllColWidtshActionItem.isSelected())
			{
            ((ButtonTableHeader)_table.getTableHeader()).adjustAllColWidths(true);
			}
		}
	}

   private class ShowRowNumbersAction extends BaseAction
	{
		ShowRowNumbersAction()
		{
			super(s_stringMgr.getString("TablePopupMenu.showRowNumbers"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (_table != null)
			{
				JCheckBoxMenuItem mnu = (JCheckBoxMenuItem) evt.getSource();
				new ShowRowNumbersCommand(_viewer, mnu.isSelected()).execute();
			}
		}
	}



	private class MakeEditableAction extends BaseAction
	{
		MakeEditableAction()
		{
			super(s_stringMgr.getString("TablePopupMenu.makeeditable"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (_updateableModel != null)
			{
				new MakeEditableCommand(_updateableModel).execute();
			}
		}
	}


	private class UndoMakeEditableAction extends BaseAction
	{
		UndoMakeEditableAction()
		{
			super(s_stringMgr.getString("TablePopupMenu.undomakeeditable"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (_updateableModel != null)
			{
				new UndoMakeEditableCommand(_updateableModel).execute();
			}
		}
	}
	private class DeleteRowsAction extends BaseAction
	{
		DeleteRowsAction()
		{
			super(s_stringMgr.getString("TablePopupMenu.deleterows"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (_table != null)
			{
				int selectedRows[] = _table.getSelectedRows();

				// Tell the DataSetViewer to delete the rows
				// Note: rows are indexes in the SORTABLE model, not the ACTUAL model
				_viewer.deleteRows(selectedRows);
			}
		}
	}

	private class InsertRowAction extends BaseAction
	{
		InsertRowAction()
		{
			super(s_stringMgr.getString("TablePopupMenu.insertrow"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			_viewer.insertRow();
		}
	}

	private class SelectAllAction extends BaseAction
	{
		SelectAllAction()
		{
			super(s_stringMgr.getString("TablePopupMenu.selectall"));
		}

		public void actionPerformed(ActionEvent evt)
		{
			if (_table != null)
			{
				new TableSelectAllCellsCommand(_table).execute();
			}
		}
	}


   private class PrintAction extends BaseAction
   {
      PrintAction()
      {
         super(s_stringMgr.getString("TablePopupMenu.print"));
      }

      public void actionPerformed(ActionEvent evt)
      {
         if (_table != null)
         {
            try
            {

               PrinterJob printerJob = PrinterJob.getPrinterJob();

               printerJob.setPrintable(_viewer);

               if (printerJob.printDialog())
               {
                  printerJob.print();
               }
            }
            catch (Exception e)
            {
               throw new RuntimeException(e);
            }
         }
      }
   }
}

