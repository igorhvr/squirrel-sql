package net.sourceforge.squirrel_sql.fw.gui;
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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.util.Utilities;

public class ErrorDialog extends JDialog {
	/**
	 * This interface defines locale specific strings. This should be
	 * replaced with a property file.
	 */
	private interface i18n {
		String ERROR = "Error";
		String OK = "OK";
	}

	public ErrorDialog(Throwable th) {
		this((Frame)null, th);
	}

	public ErrorDialog(Frame owner, Throwable th) {
		super(owner, i18n.ERROR,true);
		commonCtor(th);
	}

	public ErrorDialog(Dialog owner, Throwable th) {
		super(owner, i18n.ERROR,true);
		commonCtor(th);
	}

	public ErrorDialog(Frame owner, String msg) {
		super(owner, i18n.ERROR,true);
		commonCtor(msg);
	}

	public ErrorDialog(Dialog owner, String msg) {
		super(owner, i18n.ERROR, true);
		commonCtor(msg);
	}

	private void commonCtor(Throwable ex) {
		String msg = ex.getMessage();
		if (msg == null || msg.length() == 0) {
			msg = ex.toString();
		}
		commonCtor(msg);
	}

	private void commonCtor(String msg) {
		createUserInterface(msg);
	}

	private void createUserInterface(String msg)
	{
		msg = "<html><body>" + msg + "</body></html>";
		int iDialogWidth = 350;
		int iDialogHeight = 150;

		JPanel mainPnl = new JPanel();
		mainPnl.setLayout(new GridLayout(0, 1));
		//		mainPnl.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		JLabel ta = new JLabel(msg);
		ta.setVerticalTextPosition(SwingConstants.TOP);
		//		ta.setEditable(false);
		Dimension dim = ta.getPreferredSize();
		if (dim.width > iDialogWidth)
		{
			int widthMinScrollbar = (iDialogWidth-20); // 20 should not be guessed
			dim.height = dim.height * (dim.width / widthMinScrollbar);
			dim.width = widthMinScrollbar;
		}
		ta.setPreferredSize(dim);
		JScrollPane scroller = new JScrollPane(ta,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
										JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		mainPnl.add(scroller);
		JPanel btnsPnl = new JPanel();
		//		btnsPnl.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		JButton okBtn = new JButton(i18n.OK);
		okBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				dispose();
			}
		});
		btnsPnl.add(okBtn);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(mainPnl, BorderLayout.CENTER);
		getContentPane().add(btnsPnl, BorderLayout.SOUTH);
		getRootPane().setDefaultButton(okBtn);
		setSize(iDialogWidth, iDialogHeight);
		GUIUtils.centerWithinParent(this);
		setResizable(false);
	}
}
