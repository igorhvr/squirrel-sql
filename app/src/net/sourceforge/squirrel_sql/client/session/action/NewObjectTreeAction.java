package net.sourceforge.squirrel_sql.client.session.action;
/*
 * Copyright (C) 2003-2004 Jason Height
 * jmheight@users.sourceforge.net
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

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.SquirrelAction;
import net.sourceforge.squirrel_sql.client.session.ISession;
/**
 * This <CODE>Action</CODE> displays a new Object Tree sheet.
 *
 * @author <A HREF="mailto:jmheight@users.sourceforge.net">Jason Height</A>
 */
public class NewObjectTreeAction extends SquirrelAction
{
	/**
	 * Ctor.
	 *
	 * @param	app	 Application API.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a <TT>null</TT> <TT>IApplication</TT> passed.
	 */
	public NewObjectTreeAction(IApplication app)
	{
		super(app);
		if (app == null)
		{
			throw new IllegalArgumentException("Null IApplication passed");
		}
	}

	/**
	 * Display the tree.
	 *
	 * @param	evt	 The event being processed.
	 */
	public void actionPerformed(ActionEvent evt)
	{
		final IApplication app = getApplication();
		final ISession activeSession = app.getSessionManager().getActiveSession();
		if (activeSession == null)
		{
			throw new IllegalArgumentException(
					"This method should not be called with a null activeSession");
		}

		app.getWindowManager().createObjectTreeInternalFrame(activeSession);
	}
}
