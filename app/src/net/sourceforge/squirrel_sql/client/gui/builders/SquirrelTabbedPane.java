package net.sourceforge.squirrel_sql.client.gui.builders;
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

import javax.swing.*;

import net.sourceforge.squirrel_sql.client.preferences.SquirrelPreferences;
import net.sourceforge.squirrel_sql.client.IApplication;

class SquirrelTabbedPane extends JTabbedPane
{
	private SquirrelPreferences _prefs;

	private PropsListener _prefsListener;
   private IApplication _app;
   private static boolean _jdk14SrollWarningWasIssued = false;

   /** Convenient way to refer to Application Preferences property names. */
	private interface IAppPrefPropertynames
							extends SquirrelPreferences.IPropertyNames
	{
		// Empty block.
	}

	SquirrelTabbedPane(SquirrelPreferences prefs, IApplication app)
	{
		super();

		if (prefs == null)
		{
			throw new IllegalArgumentException("SquirrelPreferences == null");
		}
		_prefs = prefs;
      _app = app;

      int tabLayoutPolicy = _prefs.getUseScrollableTabbedPanes() ? JTabbedPane.SCROLL_TAB_LAYOUT : JTabbedPane.WRAP_TAB_LAYOUT;
      setTabLayoutPolicy(tabLayoutPolicy);
	}

	/**
	 * Component is being added to its parent so add a property change
	 * listener to application perferences.
	 */
	public void addNotify()
	{
		super.addNotify();
		_prefsListener = new PropsListener();
		_prefs.addPropertyChangeListener(_prefsListener);
		propertiesHaveChanged(null);
	}

	/**
	 * Component is being removed from its parent so remove the property change
	 * listener from the application perferences.
	 */
	public void removeNotify()
	{
		super.removeNotify();
		if (_prefsListener != null)
		{
			_prefs.removePropertyChangeListener(_prefsListener);
			_prefsListener = null;
		}
	}

	private void propertiesHaveChanged(String propName)
	{
		if (propName == null || propName.equals(IAppPrefPropertynames.SCROLLABLE_TABBED_PANES))
		{
         int tabLayoutPolicy = _prefs.getUseScrollableTabbedPanes() ? JTabbedPane.SCROLL_TAB_LAYOUT : JTabbedPane.WRAP_TAB_LAYOUT;
         setTabLayoutPolicy(tabLayoutPolicy);
		}
	}

	private final class PropsListener implements PropertyChangeListener
	{
		public void propertyChange(PropertyChangeEvent evt)
		{
			propertiesHaveChanged(evt.getPropertyName());
		}
	}
}
