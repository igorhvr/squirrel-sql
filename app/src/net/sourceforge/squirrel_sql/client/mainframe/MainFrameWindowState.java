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
import java.awt.Rectangle;

import net.sourceforge.squirrel_sql.fw.gui.WindowState;
import net.sourceforge.squirrel_sql.fw.util.beanwrapper.RectangleWrapper;
/**
 * This bean describes the state of the main window.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class MainFrameWindowState extends WindowState
{
	public interface IPropertyNames
	{
		String ALIASES_WINDOW_STATE = "aliasesWindowState";
		String DRIVERS_WINDOW_STATE = "driversWindowState";
	}

	private WindowState _driversWindowState = new WindowState();
	private WindowState _aliasesWindowState = new WindowState();

	private MainFrame _frame;

	public MainFrameWindowState()
	{
		super();
		_driversWindowState.setBounds(new RectangleWrapper(new Rectangle(5, 5, 250, 250)));
		_aliasesWindowState.setBounds(new RectangleWrapper(new Rectangle(400, 5, 250, 250)));
	}

	public MainFrameWindowState(MainFrame frame)
	{
		super(frame);
		_frame = frame;
	}

	/**
	 * This bean is about to be written out to XML so load its values from its
	 * window.
	 */
	public void aboutToBeWritten()
	{
		super.aboutToBeWritten();
		refresh();
	}

	public WindowState getAliasesWindowState()
	{
		refresh();
		return _aliasesWindowState;
	}

	public WindowState getDriversWindowState()
	{
		refresh();
		return _driversWindowState;
	}

	public void setAliasesWindowState(WindowState value)
	{
		_aliasesWindowState = value;
	}

	public void setDriversWindowState(WindowState value)
	{
		_driversWindowState = value;
	}

	private void refresh()
	{
		if (_frame != null)
		{
			if (_aliasesWindowState == null)
			{
				_aliasesWindowState = new WindowState();
			}
			if (_driversWindowState == null)
			{
				_driversWindowState = new WindowState();
			}
			_aliasesWindowState.setBounds(_frame.getAliasesWindowState().getBounds());
			_driversWindowState.setBounds(_frame.getDriversWindowState().getBounds());
		}
	}
}