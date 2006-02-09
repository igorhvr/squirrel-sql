package net.sourceforge.squirrel_sql.client.session;
/*
 * Copyright (C) 2002-2004 Colin Bell and Johan Compagner
 * colbell@users.sourceforge.net
 * jcompagner@j-com.nl
 *
 * Modifications Copyright (C) 2003-2004 Jason Height
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
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import net.sourceforge.squirrel_sql.client.action.ActionCollection;
import net.sourceforge.squirrel_sql.client.gui.session.ToolsPopupController;
import net.sourceforge.squirrel_sql.client.resources.SquirrelResources;
import net.sourceforge.squirrel_sql.client.session.action.FileSaveAction;
import net.sourceforge.squirrel_sql.client.session.action.ToolsPopupAction;
import net.sourceforge.squirrel_sql.client.session.action.ViewObjectAtCursorInObjectTreeAction;
import net.sourceforge.squirrel_sql.client.session.event.IResultTabListener;
import net.sourceforge.squirrel_sql.client.session.event.ISQLExecutionListener;
import net.sourceforge.squirrel_sql.client.session.event.ISQLPanelListener;
import net.sourceforge.squirrel_sql.client.session.event.ISQLResultExecuterTabListener;
import net.sourceforge.squirrel_sql.client.session.event.SessionAdapter;
import net.sourceforge.squirrel_sql.client.session.event.SessionEvent;
import net.sourceforge.squirrel_sql.client.session.mainpanel.ISQLResultExecuter;
import net.sourceforge.squirrel_sql.client.session.mainpanel.SQLHistoryItem;
import net.sourceforge.squirrel_sql.client.session.mainpanel.SQLPanel;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
/**
 * This class is the API through which plugins can work with the SQL Panel.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class SQLPanelAPI implements ISQLPanelAPI
{
	/** Internationalized strings for this class. */
    private static final StringManager s_stringMgr =
        StringManagerFactory.getStringManager(SQLPanelAPI.class);

	/** The SQL Panel. */
	private SQLPanel _panel;

   private ToolsPopupController _toolsPopupController;
   private FileManager _fileManager = new FileManager(this);

   private boolean fileOpened = false;
   private boolean fileSaved = false;
    private boolean unsavedEdits = false;
   
   /**
	 * Ctor specifying the panel.
	 *
	 * @param	panel	<TT>SQLPanel</TT> is the SQL Panel.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <T>null</TT> <TT>SQLPanel</TT> passed.
	 */
	public SQLPanelAPI(SQLPanel panel)
	{
		super();
		if (panel == null)
		{
			throw new IllegalArgumentException("SQLPanel == null");
		}
		_panel = panel;
        _panel.getSQLEntryPanel().addUndoableEditListener(new SQLEntryUndoListener());
      _toolsPopupController = new ToolsPopupController(getSession().getApplication(), _panel, getSession());
      getSession().getApplication().getSessionManager().addSessionListener(new MySessionListener());
      
      createStandardEntryAreaMenuItems();
	}


   private void createStandardEntryAreaMenuItems()
   {
      JMenuItem item;
      SquirrelResources resources = getSession().getApplication().getResources();


      Action toolsPopupAction = _panel.getSession().getApplication().getActionCollection().get(ToolsPopupAction.class);
      item = getSQLEntryPanel().addToSQLEntryAreaMenu(toolsPopupAction);
      resources.configureMenuItem(toolsPopupAction, item);

      if(_panel.isInMainSessionWindow())
      {
         Action vioAction = _panel.getSession().getApplication().getActionCollection().get(ViewObjectAtCursorInObjectTreeAction.class);
         item = getSQLEntryPanel().addToSQLEntryAreaMenu(vioAction);
         resources.configureMenuItem(vioAction, item);
      }

   }


   public void addToToolsPopUp(String selectionString, Action action)
   {
      _toolsPopupController.addAction(selectionString, action);
   }

   public void fileSave()
   {
      if (_fileManager.save()) {
          fileSaved = true;
          unsavedEdits = false;
          getSession().getActiveSessionWindow().setUnsavedEdits(false);
          ActionCollection actions = 
              getSession().getApplication().getActionCollection();
          actions.enableAction(FileSaveAction.class, false);
      }
   }
   
   /* (non-Javadoc)
    * @see net.sourceforge.squirrel_sql.client.session.ISQLPanelAPI#fileAppend()
    */
   public void fileAppend() {
       if (_fileManager.open(true)) {
           fileOpened = true;
           fileSaved = false;
           unsavedEdits = false;
           ActionCollection actions = 
               getSession().getApplication().getActionCollection();
           actions.enableAction(FileSaveAction.class, true);           
       }
   }
   
   
   /* (non-Javadoc)
    * @see net.sourceforge.squirrel_sql.client.session.ISQLPanelAPI#fileClose()
    */
   public void fileClose() {
       if (unsavedEdits) {
           showConfirmSaveDialog();
       }
       setEntireSQLScript("");
       getSession().getActiveSessionWindow().setSqlFile(null);
       fileOpened = true;
       fileSaved = false;
       unsavedEdits = false;
       ActionCollection actions = 
           getSession().getApplication().getActionCollection();
       actions.enableAction(FileSaveAction.class, false);
       _fileManager.clearCurrentFile();
   }
   
   
   /* (non-Javadoc)
    * @see net.sourceforge.squirrel_sql.client.session.ISQLPanelAPI#fileNew()
    */
   public void fileNew() {
       fileClose();
   }
   
   
   public void fileSaveAs()
   {
       if (_fileManager.saveAs()) {
           fileSaved = true;
           unsavedEdits = false;
           getSession().getActiveSessionWindow().setUnsavedEdits(false);
           ActionCollection actions = 
               getSession().getApplication().getActionCollection();
           actions.enableAction(FileSaveAction.class, false);         
       }
   }

   public void fileOpen()
   {
       if (unsavedEdits) {
           showConfirmSaveDialog();
       }       
       if (_fileManager.open(false)) {
           fileOpened = true;
           fileSaved = false;
           unsavedEdits = false;
           ActionCollection actions = 
               getSession().getApplication().getActionCollection();
           actions.enableAction(FileSaveAction.class, false);           
       }
   }

   public void showToolsPopup()
   {
      _toolsPopupController.showToolsPopup();
   }


   public void addExecutor(ISQLResultExecuter exec)
	{
		_panel.addExecutor(exec);
	}

	public void removeExecutor(ISQLResultExecuter exec)
	{
		_panel.removeExecutor(exec);
	}

	/**
	 * Add a listener listening for SQL Execution.
	 *
	 * @param	lis		Listener to add
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a null <TT>ISQLExecutionListener</TT> passed.
	 */
	public synchronized void addSQLExecutionListener(ISQLExecutionListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("null ISQLExecutionListener passed");
		}
		_panel.addSQLExecutionListener(lis);
	}

	/**
	 * Remove an SQL execution listener.
	 *
	 * @param	lis	 Listener
	 *
	 * @throws	IllegalArgumentException
	 *			If a null <TT>ISQLExecutionListener</TT> passed.
	 */
	public synchronized void removeSQLExecutionListener(ISQLExecutionListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("null ISQLExecutionListener passed");
		}
		_panel.removeSQLExecutionListener(lis);
	}

	/**
	 * Add a listener for events in this sessions result tabs.
	 *
	 * @param	lis		The listener.
	 */
// JASON: Do we need these?
	public synchronized void addResultTabListener(IResultTabListener lis)
	{
//		if (lis == null)
//		{
//			throw new IllegalArgumentException("null IResultTabListener passed");
//		}
//		_session.getSessionSheet().getSQLPanel().addResultTabListener(lis);
	}

	/**
	 * Remove a listener for events in this sessions result tabs.
	 *
	 * @param	lis		The listener.
	 */
//	 JASON: Do we need these?
	public synchronized void removeResultTabListener(IResultTabListener lis)
	{
//		if (lis == null)
//		{
//			throw new IllegalArgumentException("null IResultTabListener passed");
//		}
//		_session.getSessionSheet().getSQLPanel().removeResultTabListener(lis);
	}

	/**
	 * Add a listener for events in this sql panel executer tabs.
	 *
	 * @param	lis		The listener.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a null <TT>ISQLResultExecuterTabListener</TT> passed.
	 */
	public void addExecuterTabListener(ISQLResultExecuterTabListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("ISQLResultExecuterTabListener == null");
		}
		_panel.addExecuterTabListener(lis);
	}

	/**
	 * Remove a listener for events from this sql panel executer tabs.
	 *
	 * @param	lis		The listener.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a null <TT>ISQLResultExecuterTabListener</TT> passed.
	 */
	public void removeExecuterTabListener(ISQLResultExecuterTabListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("ISQLResultExecuterTabListener == null");
		}
		_panel.removeExecuterTabListener(lis);
	}

	/**
	 * Add a listener for events in this SQL Panel.
	 *
	 * @param	lis		Listener to add
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a null <TT>ISQLPanelListener</TT> passed.
	 */
	public synchronized void addSQLPanelListener(ISQLPanelListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("ISQLPanelListener == null");
		}
		_panel.addSQLPanelListener(lis);
	}

	/**
	 * Remove a listener.
	 *
	 * @param	lis		Listener to remove
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a null <TT>ISQLPanelListener</TT> passed.
	 */
	public synchronized void removeSQLPanelListener(ISQLPanelListener lis)
	{
		if (lis == null)
		{
			throw new IllegalArgumentException("ISQLPanelListener == null");
		}
		_panel.removeSQLPanelListener(lis);
	}

	public ISQLEntryPanel getSQLEntryPanel()
	{
		return _panel.getSQLEntryPanel();
	}

	/**
	 * Return the entire contents of the SQL entry area.
	 *
	 * @return	the entire contents of the SQL entry area.
	 */
	public synchronized String getEntireSQLScript()
	{
		return _panel.getSQLEntryPanel().getText();
	}

	/**
	 * Return the selected contents of the SQL entry area.
	 *
	 * @return	the selected contents of the SQL entry area.
	 */
	public String getSelectedSQLScript()
	{
		return _panel.getSQLEntryPanel().getSelectedText();
	}

	/**
	 * Return the SQL script to be executed.
	 *
	 * @return	the SQL script to be executed.
	 */
	public synchronized String getSQLScriptToBeExecuted()
	{
		return _panel.getSQLEntryPanel().getSQLToBeExecuted();
	}

	/**
	 * Append the passed SQL script to the SQL entry area but don't select
	 * it.
	 *
	 * @param	sqlScript	The script to be appended.
	 */
	public synchronized void appendSQLScript(String sqlScript)
	{
		_panel.getSQLEntryPanel().appendText(sqlScript);
	}

	/**
	 * Append the passed SQL script to the SQL entry area and specify
	 * whether it should be selected.
	 *
	 * @param	sqlScript	The script to be appended.
	 * @param	select		If <TT>true</TT> then select the passed script
	 * 						in the sql entry area.
	 */
	public synchronized void appendSQLScript(String sqlScript, boolean select)
	{
		_panel.getSQLEntryPanel().appendText(sqlScript, select);
	}

	/**
	 * Replace the contents of the SQL entry area with the passed
	 * SQL script without selecting it.
	 *
	 * @param	sqlScript	The script to be placed in the SQL entry area.
	 */
	public synchronized void setEntireSQLScript(String sqlScript)
	{
		_panel.getSQLEntryPanel().setText(sqlScript);
	}

	/**
	 * Replace the contents of the SQL entry area with the passed
	 * SQL script and specify whether to select it.
	 *
	 * @param	sqlScript	The script to be placed in the SQL entry area.
	 * @param	select		If <TT>true</TT> then select the passed script
	 * 						in the sql entry area.
	 */
	public synchronized void setEntireSQLScript(String sqlScript, boolean select)
	{
		_panel.getSQLEntryPanel().setText(sqlScript, select);
	}

	/**
	 * Replace the currently selected text in the SQL entry area
	 * with the passed text.
	 *
	 * @param	sqlScript	The script to be placed in the SQL entry area.
	 * @param	select		If <TT>true</TT> then select the passed script
	 * 						in the sql entry area.
	 */
	public synchronized void replaceSelectedSQLScript(String sqlScript, boolean select)
	{
		if (sqlScript == null)
		{
			sqlScript = "";
		}
		final ISQLEntryPanel pnl = _panel.getSQLEntryPanel();
		int selStart = -1;
		if (select)
		{
			selStart = pnl.getSelectionStart();
		}
		pnl.replaceSelection(sqlScript);
		if (select)
		{
			int entireLen = getEntireSQLScript().length();
			if (selStart == -1)
			{
				selStart = 0;
			}
			int selEnd = selStart + sqlScript.length() - 1;
			if (selEnd > entireLen - 1)
			{
				selEnd = entireLen - 1;
			}
			if (selStart <= selEnd)
			{
				pnl.setSelectionStart(selStart);
				pnl.setSelectionEnd(selEnd);
			}
		}
	}

	/**
	 * Return the offset into the SQL entry area where the current select
	 * starts.
	 *
	 * @return	the current selections start position.
	 */
	public synchronized int getSQLScriptSelectionStart()
	{
		return _panel.getSQLEntryPanel().getSelectionStart();
	}

	/**
	 * Return the offset into the SQL entry area where the current select
	 * ends.
	 *
	 * @return	the current selections end position.
	 */
	public synchronized int getSQLScriptSelectionEnd()
	{
		return _panel.getSQLEntryPanel().getSelectionEnd();
	}

	/**
	 * Set the offset into the SQL entry area where the current select
	 * starts.
	 *
	 * param	start	the new selections start position.
	 */
	public synchronized void setSQLScriptSelectionStart(int start)
	{
		_panel.getSQLEntryPanel().setSelectionStart(start);
	}

	/**
	 * Set the offset into the SQL entry area where the current select
	 * ends.
	 *
	 * param	start	the new selections start position.
	 */
	public synchronized void setSQLScriptSelectionEnd(int end)
	{
		_panel.getSQLEntryPanel().setSelectionEnd(end);
	}

	/**
	 * Execute the current SQL. Not <TT>synchronized</TT> as multiple SQL statements
	 * can be executed simultaneously.
	 */
	public void executeCurrentSQL()
	{
		_panel.runCurrentExecuter();
	}

   /**
	 * Execute the passed SQL. Not <TT>synchronized</TT> as multiple SQL statements
	 * can be executed simultaneously.
	 *
	 * @param	sql		SQL to be executed.
	 */
	public void executeSQL(String sql)
	{
		//JASON: Remove??
		//_session.getSessionSheet().getSQLPanel().executeSQL(sql);
	}

	/**
	 * Close all the SQL result tabs.
	 */
	public void closeAllSQLResultTabs()
	{
		_panel.getSQLExecPanel().closeAllSQLResultTabs();
	}

   public void closeAllButCurrentResultTabs()
   {
      _panel.getSQLExecPanel().closeAllButCurrentResultTabs();
   }

   public void closeCurrentResultTab()
   {
      _panel.getSQLExecPanel().closeCurrentResultTab();
   }

   public void toggleCurrentSQLResultTabSticky()
   {
      _panel.getSQLExecPanel().toggleCurrentSQLResultTabSticky();
   }

	/**
	 * Close all the "torn off" SQL result frames.
	 */
	public void closeAllSQLResultFrames()
	{
		_panel.getSQLExecPanel().closeAllSQLResultFrames();
	}

	/**
	 * Display the next tab in the SQL results.
	 */
	public synchronized void gotoNextResultsTab()
	{
		_panel.getSQLExecPanel().gotoNextResultsTab();
	}

	/**
	 * Display the previous tab in the SQL results.
	 */
	public void gotoPreviousResultsTab()
	{
		_panel.getSQLExecPanel().gotoPreviousResultsTab();
	}

	/**
	 * The passed SQL should be added to the SQL history.
	 *
	 * @param	sql		SQL to be added to history.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>sql</TT> passed.
	 */
	public synchronized void addSQLToHistory(String sql)
	{
		if (sql == null)
		{
			throw new IllegalArgumentException("sql == null");
		}

		final SQLHistoryItem shi = new SQLHistoryItem(sql);
		final ISession session = _panel.getSession();
		if (session.getProperties().getSQLShareHistory())
		{
			session.getApplication().getSQLHistory().add(shi);
		}
		_panel.addSQLToHistory(shi);
	}

	/**
	 * Add a hierarchical menu to the SQL Entry Area popup menu.
	 *
	 * @param	menu	The menu that will be added.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>Menu</TT> passed.
	 */
	public void addToSQLEntryAreaMenu(JMenu menu)
	{
		if (menu == null)
		{
			throw new IllegalArgumentException("Menu == null");
		}
		_panel.addToSQLEntryAreaMenu(menu);
	}

	/**
	 * Add an <TT>Action</TT> to the SQL Entry Area popup menu.
	 *
	 * @param	action	The action to be added.
	 *
	 * @return	The newly create menu item.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>Action</TT> passed.
	 */
	public JMenuItem addToSQLEntryAreaMenu(Action action)
	{
		if (action == null)
		{
			throw new IllegalArgumentException("Action == null");
		}
		return _panel.addToSQLEntryAreaMenu(action);
	}

	/** JASON: Remove once deprecated interface removed*/
	public ISession getSession()
	{
		return _panel.getSession();
	}

   public boolean isInMainSessionWindow()
   {
      return _panel.isInMainSessionWindow();
   }

   private void showConfirmSaveDialog() 
   {
       String msg = s_stringMgr.getString("SQLPanelAPI.unsavedchanges");
       String title = 
           s_stringMgr.getString("SQLPanelAPI.unsavedchangestitle");
       
       JFrame f = getSession().getApplication().getMainFrame();
       int option = 
           JOptionPane.showConfirmDialog(f, 
                                         msg, 
                                         title, 
                                         JOptionPane.YES_NO_OPTION);
       if (option == JOptionPane.YES_OPTION) {
           fileSave();
       }
   }   
   
   /**
    * A class to listen for events that indicate that the content in the 
    * SQLEntryPanel has changed and could be lost.
    */
   private class SQLEntryUndoListener implements UndoableEditListener {

    /* (non-Javadoc)
     * @see javax.swing.event.UndoableEditListener#undoableEditHappened(javax.swing.event.UndoableEditEvent)
     */
    public void undoableEditHappened(UndoableEditEvent e) {
        if (fileOpened || fileSaved) {
            unsavedEdits = true;
            getSession().getActiveSessionWindow().setUnsavedEdits(true);
            ActionCollection actions = 
                getSession().getApplication().getActionCollection();
            actions.enableAction(FileSaveAction.class, true);
        }        
    }
       
   }
   
   /**
    * A session listener that will detect unsaved edits and prompt the user to 
    * save them when the session is about to be closed.
    */
   private class MySessionListener extends SessionAdapter {
       
       /* (non-Javadoc)
        * @see net.sourceforge.squirrel_sql.client.session.event.SessionAdapter#sessionClosing(net.sourceforge.squirrel_sql.client.session.event.SessionEvent)
        */
       public void sessionClosing(SessionEvent evt) {
           if (evt.getSession().equals(_panel.getSession()) 
                   && unsavedEdits)
           {
               showConfirmSaveDialog();
           }
       }              
   }
   
}
