package net.sourceforge.squirrel_sql.client.mainframe.action;
/*
 * Copyright (C) 2002-2003 Colin Bell
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
import java.beans.PropertyVetoException;
import java.net.URL;

import net.sourceforge.squirrel_sql.fw.gui.Dialogs;
import net.sourceforge.squirrel_sql.fw.util.BaseException;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.SquirrelAction;
import net.sourceforge.squirrel_sql.client.mainframe.DriversToolWindow;
/**
 * This <CODE>Action</CODE> will install the default drivers into the drivers
 * list.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class InstallDefaultDriversAction extends SquirrelAction
{
	/** Internationalized strings for this class. */
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(InstallDefaultDriversAction.class);

	/**
	 * Ctor.
	 *
	 * @param   app	 Application API.
	 */
	public InstallDefaultDriversAction(IApplication app)
	{
		super(app);
	}

	/**
	 * Perform this action.
	 *
	 * @param   evt	 The current event.
	 */
	public void actionPerformed(ActionEvent evt)
	{
		final IApplication app = getApplication();

		if (Dialogs.showYesNo(app.getMainFrame(),
								s_stringMgr.getString("InstallDefaultDriversAction.confirm")))
		{
			final DriversToolWindow tw = app.getMainFrame().getDriversToolWindow();
			tw.moveToFront();
			try
			{
				tw.setSelected(true);
			}
			catch (PropertyVetoException ignore)
			{
			}
			final URL url = app.getResources().getDefaultDriversUrl();
			try
			{
				new InstallDefaultDriversCommand(app, url).execute();
			}
			catch (BaseException ex)
			{
				app.showErrorDialog(s_stringMgr.getString("InstallDefaultDriversAction.error.install"), ex);
			}
		}
	}
}
