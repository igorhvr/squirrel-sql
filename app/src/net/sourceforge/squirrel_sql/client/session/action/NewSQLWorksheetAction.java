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
import javax.swing.SwingUtilities;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.SquirrelAction;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.SQLInternalFrame;
/**
 * This <CODE>Action</CODE> displays a new SQL Worksheet.
 *
 * @author  <A HREF="mailto:jmheight@users.sourceforge.net">Jason Height</A>
 */
public class NewSQLWorksheetAction extends SquirrelAction
{
	/**
	 * Ctor.
	 *
	 * @param   app	 Application API.
	 *
	 * @throws  IllegalArgumentException
	 *			Thrown if a <TT>null</TT> <TT>IApplication</TT> passed.
	 */
	public NewSQLWorksheetAction(IApplication app)
	{
		super(app);
		if (app == null)
		{
			throw new IllegalArgumentException("Null IApplication passed");
		}
	}

	/**
	 * Display a new worksheet.
	 *
	 * @param   evt	 The event being processed.
	 */
	public void actionPerformed(ActionEvent evt)
	{
		ISession activeSession = getApplication().getSessionManager().getActiveSession();
		if (activeSession == null)
		{
			throw new IllegalArgumentException(
					"This method should not be called with a null activeSession");
		}

		final SQLInternalFrame sif = new SQLInternalFrame(activeSession);
		getApplication().getMainFrame().addInternalFrame(sif, true, null);

		// If we don't invokeLater here no Short-Cut-Key is sent
		// to the internal frame
		// seen under java version "1.4.1_01" and Linux
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				sif.setVisible(true);
			}
		});
	}
}