package net.sourceforge.squirrel_sql.client.session.action;
/*
 * Copyright (C) 2001-2002 Colin Bell
 * colbell@users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
import java.awt.event.ActionEvent;

import net.sourceforge.squirrel_sql.fw.gui.CursorChanger;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.SquirrelAction;
import net.sourceforge.squirrel_sql.client.session.ISession;

/**
 * This <CODE>Action</CODE> allows the user to close all the SQL
 * result windows for the current session.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class CloseAllSQLResultWindowsAction extends SquirrelAction implements ISessionAction {
	/** Current session. */
	private ISession _session;

	/**
	 * Ctor.
	 *
	 * @param	app		Application API.
	 */
	public CloseAllSQLResultWindowsAction(IApplication app) {
		super(app);
	}

	/**
	 * Set the current session.
	 * 
	 * @param	session		The current session.
	 */
	public void setSession(ISession session) {
		_session = session;
	}

	/**
	 * Perform this action. Uses the <TT>CloseAllSQLResultWindowsCommand</TT>.
	 *
	 * @param	evt	The current event.
	 */
	public void actionPerformed(ActionEvent evt) {
		IApplication app = getApplication();
		CursorChanger cursorChg = new CursorChanger(app.getMainFrame());
		cursorChg.show();
		try {
			new CloseAllSQLResultWindowsCommand(_session).execute();
		} finally {
			cursorChg.restore();
		}
	}
}
