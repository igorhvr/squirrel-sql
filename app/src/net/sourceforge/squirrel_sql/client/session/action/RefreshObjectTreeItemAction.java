package net.sourceforge.squirrel_sql.client.session.action;
/*
 * Copyright (C) 2002 Colin Bell
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
import java.awt.event.ActionEvent;

import net.sourceforge.squirrel_sql.fw.gui.CursorChanger;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.SquirrelAction;
import net.sourceforge.squirrel_sql.client.session.IClientSession;

/**
 * This <CODE>Action</CODE> will refresh the selected nodes in the object tree.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class RefreshObjectTreeItemAction extends SquirrelAction
											implements IClientSessionAction
{
	/** Logger for this class. */
	private static ILogger s_log =
		LoggerController.createLogger(RefreshObjectTreeItemAction.class);

	/** Current session. */
	private IClientSession _session;

	/**
	 * Ctor specifying application API.
	 */
	public RefreshObjectTreeItemAction(IApplication app)
	{
		super(app);
	}

	public void setClientSession(IClientSession session)
	{
		_session = session;
	}

	public void actionPerformed(ActionEvent evt)
	{
		if (_session != null)
		{
			CursorChanger cursorChg = new CursorChanger(_session.getApplication().getMainFrame());
			cursorChg.show();
			try
			{
				new RefreshObjectTreeItemCommand(_session).execute();
			}
			finally
			{
				cursorChg.restore();
			}
		}
	}
}

