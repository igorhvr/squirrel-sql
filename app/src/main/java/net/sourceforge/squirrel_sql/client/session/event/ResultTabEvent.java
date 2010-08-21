package net.sourceforge.squirrel_sql.client.session.event;
/*
 * Copyright (C) 2001 Colin Bell
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
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.mainpanel.IResultTab;

public class ResultTabEvent
{
	private ISession _session;
	private IResultTab _tab;

	public ResultTabEvent(ISession session, IResultTab tab)
		throws IllegalArgumentException
	{
		super();
		if (session == null)
		{
			throw new IllegalArgumentException("Null ISession passed");
		}
		if (tab == null)
		{
			throw new IllegalArgumentException("Null ResultTab passed");
		}
		_session = session;
		_tab = tab;
	}

	public ISession getSession()
	{
		return _session;
	}

	public IResultTab getResultTab()
	{
		return _tab;
	}
}
