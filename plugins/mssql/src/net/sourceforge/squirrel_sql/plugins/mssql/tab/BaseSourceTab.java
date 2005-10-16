package net.sourceforge.squirrel_sql.plugins.mssql.tab;
/*
 * Copyright (C) 2002 Colin Bell
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
import java.awt.BorderLayout;
import java.awt.Component;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.BaseObjectTab;

abstract class BaseSourceTab extends BaseObjectTab
{
	/**
	 * This interface defines locale specific strings. This should be
	 * replaced with a property file.
	 */
	private interface i18n
	{
		String TITLE = "Source";
	}

	/** Hint to display for tab. */
	private final String _hint;

        private String _title;

	/** Component to display in tab. */
	private BaseSourcePanel _comp;

	/** Scrolling pane for <TT>_comp. */
	private JScrollPane _scroller;

	/** Logger for this class. */
	private final static ILogger s_log =
		LoggerController.createLogger(BaseSourceTab.class);

	public BaseSourceTab(String hint)
	{
		super();
		_hint = hint != null ? hint : i18n.TITLE;
                _title = i18n.TITLE;

	}

        public BaseSourceTab(String title, String hint) {
          this(hint);
          _title = title != null ? title : i18n.TITLE;
        }

	/**
	 * Return the title for the tab.
	 *
	 * @return	The title for the tab.
	 */
	public String getTitle()
	{
		return _title;
	}

	/**
	 * Return the hint for the tab.
	 *
	 * @return	The hint for the tab.
	 */
	public String getHint()
	{
		return _hint;
	}

	public void clear()
	{
	}

	public Component getComponent()
	{
		if (_comp == null)
		{
			_comp = new BaseSourcePanel();
			_scroller = new JScrollPane(_comp);

                        LineNumber lineNumber = new LineNumber( _comp );
                        _scroller.setRowHeaderView( lineNumber );                        
		}
		return _scroller;
	}

	protected void refreshComponent()
	{
		ISession session = getSession();
		if (session == null)
		{
			throw new IllegalStateException("Null ISession");
		}
		try
		{
			PreparedStatement pstmt = createStatement();
			try
			{
				_comp.load(getSession(), pstmt);
			}
			finally
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException ex)
				{
					s_log.error(ex);
				}
			}
		}
		catch (SQLException ex)
		{
			s_log.error(ex);
			session.getMessageHandler().showErrorMessage(ex);
		}
	}

	protected abstract PreparedStatement createStatement() throws SQLException;

	private final class BaseSourcePanel extends JPanel
	{
		private JTextArea _ta;

		BaseSourcePanel()
		{
			super(new BorderLayout());
			createUserInterface();
		}

		void load(ISession session, PreparedStatement stmt)
		{
			_ta.setText("");
			try
			{
				ResultSet rs = stmt.executeQuery();
				StringBuffer buf = new StringBuffer(4096);
				while (rs.next())
				{
					buf.append(rs.getString(1));
				}
				_ta.setText(buf.toString());
				_ta.setCaretPosition(0);
			}
			catch (SQLException ex)
			{
				session.getMessageHandler().showErrorMessage(ex);
			}

		}

		private void createUserInterface()
		{
			_ta = new JTextArea();
			_ta.setEditable(false);
			add(_ta, BorderLayout.CENTER);
		}
	}
}
