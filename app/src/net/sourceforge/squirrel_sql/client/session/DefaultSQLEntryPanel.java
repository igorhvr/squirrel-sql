package net.sourceforge.squirrel_sql.client.session;
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
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JTextArea;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;

import net.sourceforge.squirrel_sql.fw.gui.FontInfo;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.session.properties.SessionProperties;

public class DefaultSQLEntryPanel extends BaseSQLEntryPanel
{
	/** Logger for this class. */
	private static ILogger s_log =
		LoggerController.createLogger(DefaultSQLEntryPanel.class);

	/** Current session. */
	private ISession _session;

	/** Text area control. */
	private MyTextArea _comp;

	/** Popup menu for this component. */
	private SessionTextEditPopupMenu _textPopupMenu;

	/** Listener for displaying the popup menu. */
	private MouseListener _sqlEntryMouseListener = new MyMouseListener();

	public DefaultSQLEntryPanel(ISession session)
	{
		super();
		if (session == null)
		{
			throw new IllegalArgumentException("ISession == null");
		}

		_session = session;
		_textPopupMenu = new SessionTextEditPopupMenu(session);
		_comp = new MyTextArea(session, this);
	}

	/**
	 * Retrieve the text area component. Normally this would be a subclass
	 * of <TT>javax.swing.text.JTextComponent</TT> but a plugin may use a
	 * class otehr than a Swing text control.
	 * 
	 * @return	The text area component.
	 */
	public JComponent getTextComponent()
	{
		return _comp;
	}

	/**
	 * If the component returned by <TT>getTextComponent</TT> contains
	 * its own scroll bars return <TT>true</TT> other wise this component
	 * will be wrapped in the scroll pane when added to the SQL panel.
	 * 
	 * @return	<TT>true</TT> if text component already handles scrolling.
	 */
	public boolean getDoesTextComponentHaveScroller()
	{
		return false;
	}

	public void addUndoableEditListener(UndoableEditListener lis)
	{
		_comp.getDocument().addUndoableEditListener(lis);
	}

	public void removeUndoableEditListener(UndoableEditListener lis)
	{
		_comp.getDocument().removeUndoableEditListener(lis);
	}

	/*
	 * @see ISQLEntryPanel#hasOwnUndoableManager()
	 */
	public boolean hasOwnUndoableManager()
	{
		return false;
	}

	/**
	 * @see ISQLEntryPanel#setUndoActions(Action, Action)
	 */
	public void setUndoActions(Action undo, Action redo)
	{
		_textPopupMenu.addSeparator();
		final IApplication app = _session.getApplication();
		app.getResources().addToPopupMenu(undo, _textPopupMenu);
		app.getResources().addToPopupMenu(redo, _textPopupMenu);
	}

	/**
	 * @see ISQLEntryPanel#getText()
	 */
	public String getText()
	{
		return _comp.getText();
	}

	/**
	 * @see ISQLEntryPanel#getSelectedText()
	 */
	public String getSelectedText()
	{
		return _comp.getSelectedText();
	}

	/**
	 * Replace the contents of the SQL entry area with the passed
	 * SQL script without selecting it.
	 * 
	 * @param	sqlScript	The script to be placed in the SQL entry area..
	 */
	public void setText(String sqlScript)
	{
		setText(sqlScript, false);
	}

	/**
	 * Replace the contents of the SQL entry area with the passed
	 * SQL script and specify whether to select it.
	 * 
	 * @param	sqlScript	The script to be placed in the SQL entry area..
	 * @param	select		If <TT>true</TT> then select the passed script
	 * 						in the sql entry area.
	 */
	public void setText(String sqlScript, boolean select)
	{
		_comp.setText(sqlScript);
		if (select)
		{
			setSelectionEnd(getText().length());
			setSelectionStart(0);
			_comp.setCaretPosition(0);
		}
	}

	/**
	 * Append the passed SQL script to the SQL entry area but don't select
	 * it.
	 * 
	 * @param	sqlScript	The script to be appended.
	 */
	public void appendText(String sqlScript)
	{
		appendText(sqlScript, false);
	}

	/**
	 * Append the passed SQL script to the SQL entry area and specify
	 * whether it should be selected.
	 * 
	 * @param	sqlScript	The script to be appended.
	 * @param	select		If <TT>true</TT> then select the passed script
	 * 						in the sql entry area.
	 */
	public void appendText(String sqlScript, boolean select)
	{
		if (!getText().endsWith("\n") && !sqlScript.startsWith("\n"))
		{
			_comp.append("\n");
		}
		int start = 0;
		if (select)
		{
			start = getText().length();
		}
		_comp.append(sqlScript);
		if (select)
		{
			setSelectionEnd(getText().length());
			setSelectionStart(start);
			_comp.setCaretPosition(start);
		}
	}

	/**
	 * Replace the currently selected text in the SQL entry area
	 * with the passed text.
	 * 
	 * @param	sqlScript	The script to be placed in the SQL entry area.
	 */
	public void replaceSelection(String sqlScript)
	{
		_comp.replaceSelection(sqlScript);
	}

	/**
	 * @see ISQLEntryPanel#getCaretPosition()
	 */
	public int getCaretPosition()
	{
		return _comp.getCaretPosition();
	}

	/**
	 * @see ISQLEntryPanel#setTabSize(int)
	 */
	public void setTabSize(int tabSize)
	{
		_comp.setTabSize(tabSize);
	}

	public void setFont(Font font)
	{
		_comp.setFont(font);
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

		_textPopupMenu.add(menu);
	}

	/**
	 * Add an <TT>Action</TT> to the SQL Entry Area popup menu.
	 *
	 * @param	action	The action to be added.
	 * 
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>Action</TT> passed.
	 */
	public void addToSQLEntryAreaMenu(Action action)
	{
		if (action == null)
		{
			throw new IllegalArgumentException("Action == null");
		}

		_textPopupMenu.add(action);
	}

	/**
	 * @see ISQLEntryPanel#addMouseListener(MouseListener)
	 */
	public void addMouseListener(MouseListener lis)
	{
		_comp.addMouseListener(lis);
	}

	/**
	 * @see ISQLEntryPanel#removeListener(MouseListener)
	 */
	public void removeMouseListener(MouseListener lis)
	{
		_comp.removeMouseListener(lis);
	}

	/**
	 * @see ISQLEntryPanel#setCaretPosition(int)
	 */
	public void setCaretPosition(int pos)
	{
		_comp.setCaretPosition(pos);
	}

	/*
	 * @see ISQLEntryPanel#getCaretLineNumber()
	 */
	public int getCaretLineNumber()
	{
		try
		{
			return _comp.getLineOfOffset(_comp.getCaretPosition());
		}
		catch (BadLocationException ex)
		{
			return 0;
		}
	}

	public int getCaretLinePosition()
	{
		int caretPos = _comp.getCaretPosition();
		int caretLineOffset = caretPos;
		try
		{
			caretLineOffset = _comp.getLineStartOffset(getCaretLineNumber());
		}
		catch (BadLocationException ignore)
		{
		}
		return caretPos - caretLineOffset;
	}

	/**
	 * @see ISQLEntryPanel#getSelectionStart()
	 */
	public int getSelectionStart()
	{
		return _comp.getSelectionStart();
	}

	/**
	 * @see ISQLEntryPanel#setSelectionStart(int)
	 */
	public void setSelectionStart(int pos)
	{
		_comp.setSelectionStart(pos);
	}

	/**
	 * @see ISQLEntryPanel#getSelectionEnd()
	 */
	public int getSelectionEnd()
	{
		return _comp.getSelectionEnd();
	}

	/**
	 * @see ISQLEntryPanel#setSelectionEnd(int)
	 */
	public void setSelectionEnd(int pos)
	{
		_comp.setSelectionEnd(pos);
	}

	/**
	 * @see ISQLEntryPanel#hasFocus()
	 */
	public boolean hasFocus()
	{
		return _comp.hasFocus();
	}

	/**
	 * @see ISQLEntryPanel#requestFocus()
	 */
	public void requestFocus()
	{
		_comp.requestFocus();
	}

	/*
	 * @see ISQLEntryPanel#addCaretListener(CaretListener)
	 */
	public void addCaretListener(CaretListener lis)
	{
		_comp.addCaretListener(lis);
	}

	/*
	 * @see ISQLEntryPanel#removeCaretListener(CaretListener)
	 */
	public void removeCaretListener(CaretListener lis)
	{
		_comp.removeCaretListener(lis);
	}

	private final class MyMouseListener extends MouseAdapter
	{
		public void mousePressed(MouseEvent evt)
		{
			if (evt.isPopupTrigger())
			{
				displayPopupMenu(evt);
			}
		}
		public void mouseReleased(MouseEvent evt)
		{
			if (evt.isPopupTrigger())
			{
				displayPopupMenu(evt);
			}
		}
		private void displayPopupMenu(MouseEvent evt)
		{
			_textPopupMenu.setTextComponent(_comp);
			_textPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
		}
	}

	private static class MyTextArea extends JTextArea
	{
		private ISession _session;
		private DefaultSQLEntryPanel _pnl;

		private MyTextArea(ISession session, DefaultSQLEntryPanel pnl)
		{
			super();
			_session = session;
			_pnl = pnl;
			SessionProperties props = session.getProperties();
			FontInfo fi = props.getFontInfo();
			if (fi != null)
			{
				this.setFont(props.getFontInfo().createFont());
			}
		}

		public void addNotify()
		{
			super.addNotify();
			_pnl.addMouseListener(_pnl._sqlEntryMouseListener);
		}

		public void removeNotify()
		{
			super.removeNotify();
			_pnl.removeMouseListener(_pnl._sqlEntryMouseListener);
		}
	}

}
