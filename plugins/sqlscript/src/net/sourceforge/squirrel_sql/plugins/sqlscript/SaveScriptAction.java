package net.sourceforge.squirrel_sql.plugins.sqlscript;

/*
 * Copyright (C) 2001 Johan Compagner
 * jcompagner@j-com.nl
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

import net.sourceforge.squirrel_sql.fw.util.Resources;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.SquirrelAction;
import net.sourceforge.squirrel_sql.client.plugin.IPlugin;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.action.ISessionAction;

public class SaveScriptAction extends SquirrelAction implements ISessionAction
{
	private SQLScriptPlugin _plugin;
	private SaveAndLoadScriptActionDelegate _delegate;

	public SaveScriptAction(IApplication app, Resources rsrc, SQLScriptPlugin plugin)
			throws IllegalArgumentException
	{
		super(app, rsrc);
		_plugin = plugin;
		setEnabled(false);
	}

	public void actionPerformed(ActionEvent evt)
	{
		_delegate.actionPerformed(getParentFrame(evt), evt, false);
	}

	public void setSession(ISession session)
	{
		if(null != session)
		{
			_delegate = _plugin.getLoadAndSaveDelegate(session);
			setEnabled(true);
		}
		else
		{
			setEnabled(false);
		}
	}
}
