package net.sourceforge.squirrel_sql.client.mainframe;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;

import net.sourceforge.squirrel_sql.fw.gui.BasePopupMenu;
import net.sourceforge.squirrel_sql.fw.gui.ToolBar;
import net.sourceforge.squirrel_sql.fw.sql.ISQLAlias;
import net.sourceforge.squirrel_sql.fw.util.ICommand;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.ActionCollection;
import net.sourceforge.squirrel_sql.client.mainframe.action.ConnectToAliasAction;
import net.sourceforge.squirrel_sql.client.mainframe.action.ConnectToAliasCommand;
import net.sourceforge.squirrel_sql.client.mainframe.action.CopyAliasAction;
import net.sourceforge.squirrel_sql.client.mainframe.action.CreateAliasAction;
import net.sourceforge.squirrel_sql.client.mainframe.action.DeleteAliasAction;
import net.sourceforge.squirrel_sql.client.mainframe.action.ModifyAliasAction;
import net.sourceforge.squirrel_sql.client.preferences.SquirrelPreferences;
/**
 * JASON: Rename to AliasesToolInternalFrame
 * This window shows all the database aliases defined in the system.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class AliasesToolWindow extends BaseToolWindow
{
	/** Internationalized strings for this class. */
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(AliasesToolWindow.class);

	/** Application API. */
	private IApplication _app;

	/** User Interface facory. */
	private UserInterfaceFactory _uiFactory;

	/**
	 * Default ctor.
	 */
	public AliasesToolWindow(IApplication app)
	{
		super(new UserInterfaceFactory(app));
		_app = app;
		_uiFactory = (UserInterfaceFactory) getUserInterfaceFactory();
//		_uiFactory.setAliasesToolWindow(this);

		// Enable/disable actions depending on whether an item is selected in
		// the list.
		_uiFactory.enableDisableActions();

		_app.getSquirrelPreferences().addPropertyChangeListener(new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				final String propName = evt != null ? evt.getPropertyName() : null;
				if (propName == null
					|| propName.equals(SquirrelPreferences.IPropertyNames.SHOW_ALIASES_TOOL_BAR))
				{
					boolean show = _app.getSquirrelPreferences().getShowAliasesToolBar();
					if (show)
					{
						_uiFactory.createToolBar();
					}
					else
					{
						_uiFactory._tb = null;
					}
					setToolBar(_uiFactory.getToolBar());
				}
			}
		});
	}

	/**
	 * Retrieve the index of the currently selected alias.
	 *
	 * @return	index of currently selected alias.
	 */
	public int getSelectedIndex()
	{
		return _uiFactory._aliasesList.getSelectedIndex();
	}

	private static final class UserInterfaceFactory
		implements BaseToolWindow.IUserInterfaceFactory
	{
		private IApplication _app;
		private AliasesList _aliasesList;
		private ToolBar _tb;
		private BasePopupMenu _pm = new BasePopupMenu();
//		private AliasesToolWindow _tw;
		private ConnectToAliasAction _connectToAliasAction;
		private CopyAliasAction _copyAliasAction;
		private CreateAliasAction _createAliasAction;
		private DeleteAliasAction _deleteAliasAction;
		private ModifyAliasAction _modifyAliasAction;

		UserInterfaceFactory(IApplication app) throws IllegalArgumentException
		{
			super();
			if (app == null)
			{
				throw new IllegalArgumentException("Null IApplication passed");
			}
			_app = app;
			_aliasesList = new AliasesList(app);

			preloadActions();

			if (_app.getSquirrelPreferences().getShowAliasesToolBar())
			{
				createToolBar();
			}

			_pm.add(_connectToAliasAction);
			_pm.addSeparator();
			_pm.add(_createAliasAction);
			_pm.addSeparator();
			_pm.add(_modifyAliasAction);
			_pm.add(_copyAliasAction);
			_pm.addSeparator();
			_pm.add(_deleteAliasAction);
			_pm.addSeparator();
		}

		public ToolBar getToolBar()
		{
			return _tb;
		}

		public BasePopupMenu getPopupMenu()
		{
			return _pm;
		}

		public JList getList()
		{
			return _aliasesList;
		}

		public String getWindowTitle()
		{
			return s_stringMgr.getString("AliasesToolWindow.windowtitle");
		}

		public ICommand getDoubleClickCommand()
		{
			ICommand cmd = null;
			ISQLAlias alias = _aliasesList.getSelectedAlias();
			if (alias != null)
			{
				cmd = new ConnectToAliasCommand(_app, alias);
			}
			return cmd;
		}

		/**
		 * Enable/disable actions depending on whether an item is selected in list.
		 */
		public void enableDisableActions()
		{
			boolean enable = false;
			try
			{
				enable = _aliasesList.getSelectedAlias() != null;
			}
			catch (Exception ignore)
			{
				// Getting an error in the JDK.
				// Exception occurred during event dispatching:
				// java.lang.ArrayIndexOutOfBoundsException: 0 >= 0
				// at java.util.Vector.elementAt(Vector.java:417)
				// at javax.swing.DefaultListModel.getElementAt(DefaultListModel.java:70)
				// at javax.swing.JList.getSelectedValue(JList.java:1397)
				// at net.sourceforge.squirrel_sql.mainframe.AliasesList.getSelectedAlias(AliasesList.java:77)
			}
			_connectToAliasAction.setEnabled(enable);
			_copyAliasAction.setEnabled(enable);
			_deleteAliasAction.setEnabled(enable);
			_modifyAliasAction.setEnabled(enable);
		}

//		void setAliasesToolWindow(AliasesToolWindow tw)
//		{
//			_tw = tw;
//		}

		private void preloadActions()
		{
			ActionCollection actions = _app.getActionCollection();
			actions.add(_modifyAliasAction = new ModifyAliasAction(_app, _aliasesList));
			actions.add(_deleteAliasAction = new DeleteAliasAction(_app, _aliasesList));
			actions.add(_copyAliasAction = new CopyAliasAction(_app, _aliasesList));
			actions.add(_connectToAliasAction = new ConnectToAliasAction(_app, _aliasesList));
			actions.add(_createAliasAction = new CreateAliasAction(_app));
		}

		private void createToolBar()
		{
			_tb = new ToolBar();
			_tb.setUseRolloverButtons(true);
			_tb.setFloatable(false);

			final JLabel lbl = new JLabel(getWindowTitle(), SwingConstants.CENTER);
			lbl.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
			_tb.add(lbl, 0);

			_tb.add(_connectToAliasAction);
			_tb.addSeparator();
			_tb.add(_createAliasAction);
			_tb.add(_modifyAliasAction);
			_tb.add(_copyAliasAction);
			_tb.add(_deleteAliasAction);
		}
	}
}
