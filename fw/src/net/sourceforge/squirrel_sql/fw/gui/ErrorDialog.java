package net.sourceforge.squirrel_sql.fw.gui;
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
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import net.sourceforge.squirrel_sql.fw.util.Utilities;

public class ErrorDialog extends JDialog
{
	/**
	 * This interface defines locale specific strings. This should be
	 * replaced with a property file.
	 */
	private interface ErrorDialog_i18n
	{
		String ERROR = "Error";
		String CLOSE = "Close";
		String MORE = "More";
		String STACK_TRACE = "Stack Trace";
		String UNKNOWN_ERROR = "Unknown error";
	}

	/** Preferred width of the message area. TODO: remove magic number*/
	private static final int PREFERRED_WIDTH = 400;

	/** Close button. */
	private JButton _closeBtn;

	/** Display stack trace button. */
	private JButton _stackTraceBtn;

	/** Display more errors. */
	private JButton _moreBtn;

	/** Panel to display the stack trace in. */
	private JScrollPane _stackTraceScroller;

	/** Panel to display more errors in. */
	private JScrollPane _moreErrorsScroller;

	/** Handler for Stack Trace button. */
	private ActionListener _stackTraceHandler = new StackTraceButtonHandler();

	/** Handler for Close button. */
	private ActionListener _closeHandler = new CloseButtonHandler();

	/** Handler for More button. */
	private ActionListener _moreHandler = new MoreButtonHandler();

	public ErrorDialog(Throwable th)
	{
		this((Frame) null, th);
	}

	public ErrorDialog(Frame owner, Throwable th)
	{
		super(owner, ErrorDialog_i18n.ERROR, true);
		createUserInterface(null, th);
	}

	public ErrorDialog(Dialog owner, Throwable th)
	{
		super(owner, ErrorDialog_i18n.ERROR, true);
		createUserInterface(null, th);
	}

	public ErrorDialog(Frame owner, String msg)
	{
		super(owner, ErrorDialog_i18n.ERROR, true);
		createUserInterface(msg, null);
	}

	public ErrorDialog(Frame owner, String msg, Throwable th)
	{
		super(owner, ErrorDialog_i18n.ERROR, true);
		createUserInterface(msg, th);
	}

	public ErrorDialog(Dialog owner, String msg)
	{
		super(owner, ErrorDialog_i18n.ERROR, true);
		createUserInterface(msg, null);
	}

	/**
	 * Dispose of this dialog after cleaning up all listeners.
	 */
	public void dispose()
	{
		if (_closeBtn != null && _closeHandler != null)
		{
			_closeBtn.removeActionListener(_closeHandler);
		}
		if (_stackTraceBtn != null && _stackTraceHandler != null)
		{
			_stackTraceBtn.removeActionListener(_stackTraceHandler);
		}
		if (_moreBtn != null && _moreHandler != null)
		{
			_moreBtn.removeActionListener(_moreHandler);
		}
		super.dispose();
	}

	/**
	 * Create user interface.
	 * 
	 * @param	msg		Message to be displayed. Can be null.
	 * @param	th		Exception to be shown. Can be null.
	 */
	private void createUserInterface(String msg, Throwable th)
	{
		if (msg == null || msg.length() == 0)
		{
			if (th != null)
			{
				msg = th.getMessage();
				if (msg == null || msg.length() == 0)
				{
					msg = th.toString();
				}
			}
		}
		if (msg == null || msg.length() == 0)
		{
			msg = ErrorDialog_i18n.UNKNOWN_ERROR;
		}

		_stackTraceScroller = new JScrollPane(new StackTracePanel(th));
		_stackTraceScroller.setVisible(false);

		final MoreErrorsPanel moreErrPnl = createMoreErrorsPanel(th);
		if (moreErrPnl != null)
		{
			_moreErrorsScroller = new JScrollPane(moreErrPnl);
			_moreErrorsScroller.setVisible(false);
		}

		Container content = getContentPane();
		content.setLayout(new GridBagLayout());
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(4, 4, 4, 4);

		gbc.gridx = 0;
		gbc.gridy = 0;
		content.add(createMessagePanel(msg, th), gbc);

		++gbc.gridy;
		content.add(createButtonsPanel(th), gbc);

		++gbc.gridy;
		content.add(_stackTraceScroller, gbc);

		if (_moreErrorsScroller != null)
		{
			++gbc.gridy;
			content.add(_moreErrorsScroller, gbc);
		}

		getRootPane().setDefaultButton(_closeBtn);
		setResizable(false);

		pack();
		GUIUtils.centerWithinParent(ErrorDialog.this);
	}

	/**
	 * Create the message panel.
	 * 
	 * @param	msg		The message to be displayed.
	 * @param	th		The exception to be displayed.
	 * 
	 * @return	The newly created message panel.
	 */
	private JComponent createMessagePanel(String msg, Throwable th)
	{
		if (msg == null || msg.length() == 0)
		{
			if (th != null)
			{
				msg = th.getMessage();
				if (msg == null || msg.length() == 0)
				{
					msg = th.toString();
				}
			}
		}
		if (msg == null || msg.length() == 0)
		{
			msg = ErrorDialog_i18n.UNKNOWN_ERROR;
		}
		JScrollPane sp = new JScrollPane(new MessagePanel(msg));
		Dimension dim = sp.getPreferredSize();
		dim.width = PREFERRED_WIDTH;
		sp.setPreferredSize(dim);

		return sp;
	}

	/**
	 * Create the buttons panel.
	 * 
	 * @param	th		The exception.
	 * 
	 * @return	The newly created buttons panel.
	 */
	private JPanel createButtonsPanel(Throwable th)
	{
		JPanel btnsPnl = new JPanel();
		if (th != null)
		{
			_stackTraceBtn = new JButton(ErrorDialog_i18n.STACK_TRACE);
			_stackTraceBtn.addActionListener(_stackTraceHandler);
			btnsPnl.add(_stackTraceBtn);
			if (_moreErrorsScroller != null)
			{
				_moreBtn = new JButton(ErrorDialog_i18n.MORE);
				_moreBtn.addActionListener(_moreHandler);
				btnsPnl.add(_moreBtn);
			}
		}
		_closeBtn = new JButton(ErrorDialog_i18n.CLOSE);
		_closeBtn.addActionListener(_closeHandler);
		btnsPnl.add(_closeBtn);

		return btnsPnl;
	}

	private static Color getTextAreaBackgroundColor()
	{
		return (Color)UIManager.get("TextArea.background");
	}

	private MoreErrorsPanel createMoreErrorsPanel(Throwable th)
	{
		if (th instanceof SQLException)
		{
			SQLException ex = ((SQLException)th).getNextException();
			if (ex != null)
			{
				return new MoreErrorsPanel(ex);
			}
		}
		return null;
	}

	/**
	 * Panel to display the message in.
	 */
	private final class MessagePanel extends MultipleLineLabel
	{
		MessagePanel(String msg)
		{
			super();
			setText(msg);
			setBackground(ErrorDialog.getTextAreaBackgroundColor());
//			Dimension dim = getPreferredSize();
//			dim.width = PREFERRED_WIDTH;
//			setPreferredSize(dim);
			setRows(3);
		}
	}

	/**
	 * Panel to display the stack trace in.
	 */
	private final class StackTracePanel extends MultipleLineLabel
	{
		StackTracePanel(Throwable th)
		{
			super();
			setBackground(ErrorDialog.getTextAreaBackgroundColor());
			if (th != null)
			{
				setText(Utilities.getStackTrace(th));
				setRows(10);
			}
		}
	}

	private final class MoreErrorsPanel extends MultipleLineLabel
	{
		MoreErrorsPanel(SQLException ex)
		{
			super();
			StringBuffer buf = new StringBuffer();
			setBackground(ErrorDialog.getTextAreaBackgroundColor());
			while (ex != null)
			{
				String msg = ex.getMessage();
				if (msg != null && msg.length() > 0)
				{
					buf.append(msg).append('\n');
				}
				else
				{
					buf.append(ex.toString()).append('\n');
				}
				ex = ex.getNextException();
			}
			setText(buf.toString());
			setRows(10);
		}
	}

	/**
	 * Handler for Close button. Disposes of this dialog.
	 */
	private final class CloseButtonHandler implements ActionListener
	{
		/**
		 * Disposes of this dialog.
		 */
		public void actionPerformed(ActionEvent evt)
		{
			ErrorDialog.this.dispose();
		}

	}

	/**
	 * Handler for Stack Trace button. Shows/hides the stack trace.
	 */
	private final class StackTraceButtonHandler implements ActionListener
	{
		/**
		 * Show/hide the stack trace.
		 */
		public void actionPerformed(ActionEvent evt)
		{
			boolean currentlyVisible = _stackTraceScroller.isVisible();
			if (!currentlyVisible)
			{
				if (_moreErrorsScroller != null)
				{
					_moreErrorsScroller.setVisible(false);
				}
			}
			_stackTraceScroller.setVisible(!currentlyVisible);
			ErrorDialog.this.pack();
		}
	}

	/**
	 * Handler for More button. Shows/hides more information about the error..
	 */
	private final class MoreButtonHandler implements ActionListener
	{
		/**
		 * Show/hide the extra errors.
		 */
		public void actionPerformed(ActionEvent evt)
		{
			boolean currentlyVisible = _moreErrorsScroller.isVisible();
			if (!currentlyVisible)
			{
				_stackTraceScroller.setVisible(false);
			}
			_moreErrorsScroller.setVisible(!currentlyVisible);
			ErrorDialog.this.pack();
		}
	}
}
