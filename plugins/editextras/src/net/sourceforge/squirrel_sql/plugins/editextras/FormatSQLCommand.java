package net.sourceforge.squirrel_sql.plugins.editextras;
/*
 * Copyright (C) 2003 Gerd Wagner
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
import net.sourceforge.squirrel_sql.fw.util.BaseException;
import net.sourceforge.squirrel_sql.fw.util.ICommand;

import net.sourceforge.squirrel_sql.plugins.editextras.codereformat.CodeReformator;
import net.sourceforge.squirrel_sql.plugins.editextras.codereformat.CommentSpec;

import net.sourceforge.squirrel_sql.client.session.ISQLPanelAPI;
import net.sourceforge.squirrel_sql.client.session.ISession;
/**
 * This command will &quot;quote&quot; an SQL string.
 *
 * @author  Gerd Wagner
 */
class FormatSQLCommand implements ICommand
{
	private final ISession _session;
	private final EditExtrasPlugin _plugin;

	FormatSQLCommand(ISession session, EditExtrasPlugin plugin)
	{
		super();
		_session = session;
		_plugin = plugin;
	}

	public void execute() throws BaseException
	{
		ISQLPanelAPI api = _session.getSQLPanelAPI(_plugin);
		String textToReformat = api.getSelectedSQLScript();
		boolean isSelection = true;
		if (null == textToReformat)
		{
			textToReformat = api.getEntireSQLScript();
			isSelection = false;
		}
		if (null == textToReformat)
		{
			return;
		}

		CommentSpec[] commentSpecs =
		  new CommentSpec[]
		  {
			  new CommentSpec("/*", "*/"),
			  new CommentSpec("--", "\n")
		  };

		char ch =  _session.getProperties().getSQLStatementSeparatorChar();
		String statementSep = new String(new char[] {ch});
		
		CodeReformator cr = new CodeReformator(statementSep, commentSpecs);

		String reformatedText = cr.reformat(textToReformat);
		if (isSelection)
		{
			api.replaceSelectedSQLScript(reformatedText, true);
		}
		else
		{
			api.setEntireSQLScript(reformatedText);
		}
	}
}
