package net.sourceforge.squirrel_sql.client.mainframe.action;
/*
 * Copyright (C) 2001-2004 Colin Bell
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

import net.sourceforge.squirrel_sql.fw.sql.ISQLAlias;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.SquirrelAction;
import net.sourceforge.squirrel_sql.client.gui.db.AliasesListInternalFrame;
import net.sourceforge.squirrel_sql.client.gui.db.IAliasesList;
/**
 * This <CODE>Action</CODE> allows the user to copy an <TT>ISQLAlias</TT>
 * and maintain the newly copied one.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class CopyAliasAction extends SquirrelAction
{
	/** Logger for this class. */
	private static ILogger s_log =
		LoggerController.createLogger(ConnectToAliasAction.class);

	/**
	 * List of all the users aliases.
	 */
	private final IAliasesList _aliases;

	/**
	 * Ctor specifying the list of aliases.
	 *
	 * @param	app		Application API.
	 * @param	list	List of <TT>ISQLAlias</TT> objects.
	 *
	 * @throws	IllegalArgumentException
	 *			thrown if a <TT>null</TT> <TT>AliasesList</TT> passed.
	 */
	public CopyAliasAction(IApplication app, IAliasesList list)
	{
		super(app);
		if (list == null)
		{
			throw new IllegalArgumentException("Null AliasesList passed");
		}
		_aliases = list;
	}

	/**
	 * Perform this action. Use the <TT>CopyAliasCommand</TT>.
	 *
	 * @param	evt	The current event.
	 */
	public void actionPerformed(ActionEvent evt)
	{
		IApplication app = getApplication();
		AliasesListInternalFrame tw = app.getWindowManager().getAliasesListInternalFrame();
		tw.moveToFront();
		try
		{
			tw.setSelected(true);
		}
		catch (PropertyVetoException ex)
		{
			s_log.error("Error selecting window", ex);
		}
		ISQLAlias alias = _aliases.getSelectedAlias();
		if (alias != null)
		{
			new CopyAliasCommand(getApplication(), alias).execute();
		}
	}
}
