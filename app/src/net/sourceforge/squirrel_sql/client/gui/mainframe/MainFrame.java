package net.sourceforge.squirrel_sql.client.gui.mainframe;
/*
 * Copyright (C) 2001-2004 Colin Bell
 * colbell@users.sourceforge.net
 *
 * Modifications Copyright (C) 2003-2004 Jason Height
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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultDesktopManager;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import net.sourceforge.squirrel_sql.fw.gui.CascadeInternalFramePositioner;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.gui.IInternalFramePositioner;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.Version;
import net.sourceforge.squirrel_sql.client.gui.ScrollableDesktopPane;
import net.sourceforge.squirrel_sql.client.gui.session.SessionInternalFrame;
import net.sourceforge.squirrel_sql.client.preferences.SquirrelPreferences;
import net.sourceforge.squirrel_sql.client.resources.SquirrelResources;
import net.sourceforge.squirrel_sql.client.session.MessagePanel;

public class MainFrame extends JFrame //BaseMDIParentFrame
{
	public interface IMenuIDs extends MainFrameMenuBar.IMenuIDs
	{
		// Empty body.
	}

	/** Logger for this class. */
	private final ILogger s_log = LoggerController.createLogger(MainFrame.class);

	/** Application API. */
	private final IApplication _app;

//	private AliasesListInternalFrame _aliasesListWindow;
//	private DriversListInternalFrame _driversListWindow;

	/** Toolbar at top of window. */
	private MainFrameToolBar _toolBar;


	/** Status bar at bottom of window. */
	private MainFrameStatusBar _statusBar;

	/** Message panel at bottom of window. */
	// JASON: Should be part of status bar?
	private MessagePanel _msgPnl;

	/** If <TT>true</TT> then status bar is visible. */
	private boolean _statusBarVisible = false;

	private JDesktopPane _desktop;

	private final IInternalFramePositioner _internalFramePositioner = new CascadeInternalFramePositioner();
//	private Map _children = new HashMap();

   /**
	 * Ctor.
	 *
	 * @param	app	 Application API.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if <TT>null</TT> <TT>IApplication</TT>
	 *			passed.
	 */
	public MainFrame(IApplication app)
	{
		super(Version.getVersion());
		if (app == null)
		{
			throw new IllegalArgumentException("Null IApplication passed");
		}
		_app = app;
		_desktop = new ScrollableDesktopPane();
		createUserInterface();
		preferencesHaveChanged(null); // Initial load of prefs.
		_app.getSquirrelPreferences().addPropertyChangeListener(new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				synchronized (MainFrame.this)
				{
					preferencesHaveChanged(evt);
				}
			}
		});

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				ScrollableDesktopPane comp = (ScrollableDesktopPane)getDesktopPane();
				comp.setPreferredSize(comp.getRequiredSize());
				comp.revalidate();
			}
		});
	}

	public void dispose()
	{
		if (_app.shutdown())
		{
			closeAllToolWindows();
			super.dispose();
			System.exit(0);
		}
	}

	// TODO: Why?
	public void pack()
	{
		// Don't call super. Packing this frame causes problems.
	}

	public IApplication getApplication()
	{
		return _app;
	}

	public JDesktopPane getDesktopPane()
	{
		return _desktop;
	}

	/**
	 * Add the passed internal frame to this MDI frame.
	 * Calls <TT>addInternalFrame(child, createMenuItem, null)</TT>.
	 *
	 * @param	child			The internal frame to be added.
	 * @param	createMenuItem	If <TT>true</TT> add an item to the MDI
	 * 							Window menu to select the passed internal frame.
	 *
	 * @throws	IllegalArgumentException if null <TT>JInternalFrame</TT> passed.
	 */
	public void addInternalFrame(JInternalFrame child, boolean createMenuItem)
	{
		addInternalFrame(child, createMenuItem, null);
	}

	// JASON: Move code to Windowmanager
	public void addInternalFrame(JInternalFrame child, boolean addToWindowMenu,
									Action action)
	{
      if (!GUIUtils.isToolWindow(child))
      {
         Dimension cs = getDesktopPane().getSize();
         // Cast to int required as Dimension::setSize(double,double)
         // doesn't appear to do anything in JDK1.2.2.
         cs.setSize((int) (cs.width * 0.8d), (int) (cs.height * 0.8d));
         child.setSize(cs);
      }


      if (child == null)
      {
         throw new IllegalArgumentException("Null JInternalFrame added");
      }

      _desktop.add(child);
      if (!GUIUtils.isToolWindow(child))
      {
         positionNewInternalFrame(child);
      }
//		JInternalFrame[] frames = GUIUtils.getOpenNonToolWindows(getDesktopPane().getAllFrames());
//		_app.getActionCollection().internalFrameOpenedOrClosed(frames.length);

		// Size non-tool child window.
		if (!GUIUtils.isToolWindow(child))
		{
			if (child.isMaximizable() &&
					_app.getSquirrelPreferences().getMaximizeSessionSheetOnOpen())
			{
				try
				{
					child.setMaximum(true);
				}
				catch (PropertyVetoException ex)
				{
					s_log.error("Unable to maximize window", ex);
				}
			}
		}
	}

	/**
	 * Return the Drivers tool window.
	 */
//	public DriversListInternalFrame getDriversToolWindow()
//	{
//		return _driversToolWindow;
//	}
//
//	/**
//	 * Return the Aliases tool window.
//	 */
//	public AliasesListInternalFrame getAliasesToolWindow()
//	{
//		return _aliasesToolWindow;
//	}
//
//	WindowState getAliasesWindowState()
//	{
//		return new WindowState(_aliasesToolWindow);
//	}
//
//	WindowState getDriversWindowState()
//	{
//		return new WindowState(_driversToolWindow);
//	}

	public JMenu getSessionMenu()
	{
		return ((MainFrameMenuBar) getJMenuBar()).getSessionMenu();
	}

	public void addToMenu(int menuId, JMenu menu)
	{
		if (menu == null)
		{
			throw new IllegalArgumentException("Null JMenu passed");
		}
		((MainFrameMenuBar)getJMenuBar()).addToMenu(menuId, menu);
	}

	public void addToMenu(int menuId, Action action)
	{
		if (action == null)
		{
			throw new IllegalArgumentException("Null BaseAction passed");
		}
		((MainFrameMenuBar)getJMenuBar()).addToMenu(menuId, action);
	}

	/**
	 * Add component to the status bar.
	 *
	 * @param	comp	Component to add.
	 *
	 * @throws	IllegalArgumentException
	 * 			Thrown if <TT>null</TT> <TT>JComponent</TT> passed.
	 */
	public void addToStatusBar(JComponent comp)
	{
		if (comp == null)
		{
			throw new IllegalArgumentException("JComponent == null");
		}
		_statusBar.addJComponent(comp);
	}

	/**
	 * Remove component to the main frames status bar.
	 *
	 * @param	comp	Component to remove.
	 */
	public void removeFromStatusBar(JComponent comp)
	{
		if (comp == null)
		{
			throw new IllegalArgumentException("JComponent == null");
		}
		_statusBar.remove(comp);
	}

   public MessagePanel getMessagePanel()
	{
		return _msgPnl;
	}

	private void preferencesHaveChanged(PropertyChangeEvent evt)
	{
		String propName = evt != null ? evt.getPropertyName() : null;

		final SquirrelPreferences prefs = _app.getSquirrelPreferences();

		if (propName == null
			|| propName.equals(
				SquirrelPreferences.IPropertyNames.SHOW_CONTENTS_WHEN_DRAGGING))
		{
			if (prefs.getShowContentsWhenDragging())
			{
				getDesktopPane().putClientProperty("JDesktopPane.dragMode", null);
			}
			else
			{
				getDesktopPane().putClientProperty("JDesktopPane.dragMode", "outline");
			}
		}

		if (propName == null
			|| propName.equals(SquirrelPreferences.IPropertyNames.SHOW_MAIN_STATUS_BAR))
		{
			final boolean show = prefs.getShowMainStatusBar();
			if (!show && _statusBarVisible)
			{
				getContentPane().remove(_statusBar);
				_statusBarVisible = false;
			}
			else if (show && !_statusBarVisible)
			{
				getContentPane().add(_statusBar, BorderLayout.SOUTH);
				_statusBarVisible = true;
			}
		}
		if (propName == null
			|| propName.equals(SquirrelPreferences.IPropertyNames.SHOW_MAIN_TOOL_BAR))
		{
			final boolean show = prefs.getShowMainToolBar();
			if (!show && _toolBar != null)
			{
				getContentPane().remove(_toolBar);
				_toolBar = null;
			}
			else if (show && _toolBar == null)
			{
				_toolBar = new MainFrameToolBar(_app);
				getContentPane().add(_toolBar, BorderLayout.NORTH);
			}
		}

	}

	private void closeAllToolWindows()
	{
		JInternalFrame[] frames =
			GUIUtils.getOpenToolWindows(getDesktopPane().getAllFrames());
		for (int i = 0; i < frames.length; ++i)
		{
			frames[i].dispose();
		}
	}

	private void createUserInterface()
	{
		setVisible(false);
		setDefaultCloseOperation(MainFrame.DO_NOTHING_ON_CLOSE);

		final SquirrelResources rsrc = _app.getResources();

		getDesktopPane().setDesktopManager(new MyDesktopManager());

		final Container content = getContentPane();

//		_aliasesToolWindow = new AliasesListInternalFrame(_app);
//		_driversToolWindow = new DriversListInternalFrame(_app);

//		preLoadActions();
		content.setLayout(new BorderLayout());
		final JScrollPane sp = new JScrollPane(getDesktopPane());
		sp.setBorder(BorderFactory.createEmptyBorder());

		_msgPnl = new MessagePanel();
		_msgPnl.setEditable(false);

		JSplitPane splitPn = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPn.add(sp);
		splitPn.add(new JScrollPane(_msgPnl));
		splitPn.setResizeWeight(0.9);

		content.add(splitPn, BorderLayout.CENTER);

		_statusBar = new MainFrameStatusBar();
		final Font fn = _app.getFontInfoStore().getStatusBarFontInfo().createFont();
		_statusBar.setFont(fn);

		setJMenuBar(new MainFrameMenuBar(_app, getDesktopPane(), _app.getActionCollection()));

		setupFromPreferences();

		final ImageIcon icon = rsrc.getIcon(SquirrelResources.IImageNames.APPLICATION_ICON);
		if (icon != null)
		{
			setIconImage(icon.getImage());
		}
		else
		{
			s_log.error("Missing icon for mainframe");
		}

		// On Win 2000 & XP mnemonics are normally hidden. To make them
		// visible you press the alt key. Under the Windows L&F pressing
		// alt may not work. This code is a workaround. See bug report
		// 4736093 for more information.
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ALT, Event.ALT_MASK, false),
				"repaint");

		validate();

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt)
			{
				dispose();
			}
		});
	}

	// JASON: Move functionality to WindowManager
//	private void preLoadActions()
//	{
//		final ActionCollection actions = _app.getActionCollection();
//		if (actions == null)
//		{
//			throw new IllegalStateException("ActionCollection hasn't been created.");
//		}
//
//		final WindowManager mgr = _app.getWindowManager();
//		if (mgr == null)
//		{
//			throw new IllegalStateException("WindowManager hasn't been created.");
//		}
//
//		actions.add(new ViewAliasesAction(_app, mgr.getAliasesListInternalFrame()));
//		actions.add(new ViewDriversAction(_app, mgr.getDriversListInternalFrame()));
//	}

	private void setupFromPreferences()
	{
		final SquirrelPreferences prefs = _app.getSquirrelPreferences();
		MainFrameWindowState ws = prefs.getMainFrameWindowState();

		// Position window to where it was when last closed. If this is not
		// on the screen, move it back on to the screen.
		setBounds(ws.getBounds().createRectangle());
		if (!GUIUtils.isWithinParent(this))
		{
			setLocation(new Point(10, 10));
		}

//		final WindowManager mgr = _app.getWindowManager();

		// JASON: Move functionality to WindowManager
//		DriversListInternalFrame dlif = mgr.getDriversListInternalFrame();
//		addInternalFrame(dlif, false, null);
//		WindowState toolWs = ws.getDriversWindowState();
//		dlif.setBounds(toolWs.getBounds().createRectangle());
//		dlif.setVisible(toolWs.isVisible());
//		try
//		{
//			dlif.setSelected(true);
//		}
//		catch (PropertyVetoException ex)
//		{
//			s_log.error("Error selecting window", ex);
//		}

		// JASON: Move functionality to WindowManager
//		AliasesListInternalFrame alif = mgr.getAliasesListInternalFrame();
//		addInternalFrame(alif, false, null);
//		toolWs = ws.getAliasesWindowState();
//		alif.setBounds(toolWs.getBounds().createRectangle());
//		if (toolWs.isVisible())
//		{
//			alif.setVisible(true);
//			try
//			{
//				alif.setSelected(true);
//			}
//			catch (PropertyVetoException ex)
//			{
//				s_log.error("Error selecting window", ex);
//			}
//		}
//		else
//		{
//			alif.setVisible(false);
//		}

//		prefs.setMainFrameWindowState(new MainFrameWindowState(_app.getWindowManager()));
	}

   private void positionNewInternalFrame(JInternalFrame child)
	{
		_internalFramePositioner.positionInternalFrame(child);
	}

	public JMenu getWindowsMenu()
	{
		return ((MainFrameMenuBar)getJMenuBar()).getWindowsMenu();
	}

	private class MyDesktopManager extends DefaultDesktopManager
	{
		public void activateFrame(JInternalFrame f)
		{
			super.activateFrame(f);

			_app.getActionCollection().internalFrameActivated(f);
			if (f instanceof SessionInternalFrame)
			{
				((SessionInternalFrame)f).getSessionPanel().updateState();
			}
		}
		public void deactivateFrame(JInternalFrame f)
		{
			super.deactivateFrame(f);

			_app.getActionCollection().internalFrameDeactivated(f);
			if (f instanceof SessionInternalFrame)
			{
				((SessionInternalFrame)f).getSessionPanel().updateState();
			}
		}
	}
}
