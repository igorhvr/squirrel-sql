package net.sourceforge.squirrel_sql.client.mainframe.action;
/*
 * Copyright (C) 2001-2002 Colin Bell
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

import net.sourceforge.squirrel_sql.fw.sql.ISQLDriver;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.SquirrelAction;
import net.sourceforge.squirrel_sql.client.mainframe.DriversList;
import net.sourceforge.squirrel_sql.client.mainframe.DriversToolWindow;

/**
 * This <CODE>Action</CODE> allows the user to delete a <TT>ISQLDriver</TT>.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class DeleteDriverAction extends SquirrelAction
{
	/**
	 * List of all the users drivers.
	 */
	private DriversList _drivers;

	/**
	 * Ctor specifying the list of drivers.
	 *
	 * @param   app		Application API.
	 * @param   list	List of <TT>ISQLDriver</TT> objects.
	 *
	 * @throws  IllegalArgumentException
	 *			thrown if a <TT>null</TT> <TT>DriversList</TT> passed.
	 */
	public DeleteDriverAction(IApplication app, DriversList list)
	{
		super(app);
		if (list == null)
		{
			throw new IllegalArgumentException("Null DriversList passed");
		}
		_drivers = list;
	}

	/**
	 * Perform this action. Use the <TT>DeleteDriverCommand</TT>.
	 *
	 * @param   evt	 The current event.
	 */
	public void actionPerformed(ActionEvent evt)
	{
		IApplication app = getApplication();
		DriversToolWindow tw = app.getMainFrame().getDriversToolWindow();
		tw.moveToFront();
		try
		{
			tw.setSelected(true);
		}
		catch (PropertyVetoException ignore)
		{
		}
		ISQLDriver driver = _drivers.getSelectedDriver();
		if (driver != null)
		{
			new DeleteDriverCommand(app, getParentFrame(evt), driver).execute();
		}
	}
}
