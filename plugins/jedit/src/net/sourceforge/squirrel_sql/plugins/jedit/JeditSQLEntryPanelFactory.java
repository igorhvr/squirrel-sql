package net.sourceforge.squirrel_sql.plugins.jedit;
/*
 * Copyright (C) 2001-2003 Colin Bell
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
import net.sourceforge.squirrel_sql.client.session.ISQLEntryPanel;
import net.sourceforge.squirrel_sql.client.session.ISQLEntryPanelFactory;
import net.sourceforge.squirrel_sql.client.session.ISession;

public class JeditSQLEntryPanelFactory implements ISQLEntryPanelFactory
{
	private JeditPlugin _plugin;

	/** The original Squirrel SQL CLient factory for creating SQL entry panels. */
	private ISQLEntryPanelFactory _originalFactory;

	JeditSQLEntryPanelFactory(JeditPlugin plugin,
		ISQLEntryPanelFactory originalFactory)
	{
		if (plugin == null)
		{
			throw new IllegalArgumentException("Null JeditPlugin passed");
		}

		if (originalFactory == null)
		{
			throw new IllegalArgumentException("Null originalFactory passed");
		}

		_plugin = plugin;
		_originalFactory = originalFactory;
	}

	/**
	 * @see ISQLEntryPanelFactory#createSQLEntryPanel()
	 */
	public ISQLEntryPanel createSQLEntryPanel(ISession session)
		throws IllegalArgumentException
	{
		if (session == null)
		{
			throw new IllegalArgumentException("Null ISession passed");
		}

		final JeditPreferences prefs = getPreferences(session);

		if (prefs.getUseJeditTextControl())
		{
			JeditSQLEntryPanel pnl = getPanel(session);

			if (pnl == null)
			{
				pnl = new JeditSQLEntryPanel(session, _plugin, prefs);
				savePanel(session, pnl);
			}

			return pnl;
		}

		removePanel(session);

		return _originalFactory.createSQLEntryPanel(session);
	}

	private JeditPreferences getPreferences(ISession session)
	{
		return (JeditPreferences)session.getPluginObject(_plugin,
			JeditConstants.ISessionKeys.PREFS);
	}

	private JeditSQLEntryPanel getPanel(ISession session)
	{
		return (JeditSQLEntryPanel)session.getPluginObject(_plugin,
			JeditConstants.ISessionKeys.JEDIT_SQL_ENTRY_CONTROL);
	}

	private void savePanel(ISession session, JeditSQLEntryPanel pnl)
	{
		session.putPluginObject(_plugin,
			JeditConstants.ISessionKeys.JEDIT_SQL_ENTRY_CONTROL, pnl);
	}

	private void removePanel(ISession session)
	{
		session.removePluginObject(_plugin,
			JeditConstants.ISessionKeys.JEDIT_SQL_ENTRY_CONTROL);
	}
}
