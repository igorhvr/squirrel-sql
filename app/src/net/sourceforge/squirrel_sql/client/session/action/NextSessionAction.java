package net.sourceforge.squirrel_sql.client.session.action;
/*
 * Copyright (C) 2003 Colin Bell
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

import javax.swing.JInternalFrame;

import net.sourceforge.squirrel_sql.fw.gui.action.SelectInternalFrameCommand;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.SquirrelAction;
import net.sourceforge.squirrel_sql.client.session.IClientSession;
import net.sourceforge.squirrel_sql.client.session.ISession;
/**
 * This action will bring the window for the <EM>next</EM> session
 * to the front.
 *
 * @author Gerd Wagner
 */
public class NextSessionAction extends SquirrelAction
								implements IClientSessionAction
{
	private IClientSession _session;

	public NextSessionAction(IApplication app)
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
			final IApplication app = getApplication();
			final ISession nextSession = app.getSessionManager().getNextSession(_session);
			final JInternalFrame sif = app.getSessionWindowManager().getInternalFrame(nextSession);
			new SelectInternalFrameCommand(sif).execute();
		}
	}
}
