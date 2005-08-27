package net.sourceforge.squirrel_sql.client.preferences;
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
import net.sourceforge.squirrel_sql.client.action.ActionKeys;
import net.sourceforge.squirrel_sql.client.gui.mainframe.MainFrameWindowState;
import net.sourceforge.squirrel_sql.client.plugin.PluginStatus;
import net.sourceforge.squirrel_sql.client.session.properties.SessionProperties;
import net.sourceforge.squirrel_sql.client.util.ApplicationFiles;
import net.sourceforge.squirrel_sql.fw.util.PropertyChangeReporter;
import net.sourceforge.squirrel_sql.fw.util.ProxySettings;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.fw.xml.XMLBeanReader;
import net.sourceforge.squirrel_sql.fw.xml.XMLBeanWriter;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
/**
 * This class represents the application preferences.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class SquirrelPreferences implements Serializable
{

   public interface IPropertyNames
	{
		String ACTION_KEYS = "actionKeys";
		String ALIASES_SELECTED_INDEX = "aliasesSelectdIndex";
		String CONFIRM_SESSION_CLOSE = "confirmSessionClose";
		String DRIVERS_SELECTED_INDEX = "driversSelectdIndex";
 		String FIRST_RUN = "firstRun";
		String JDBC_DEBUG_TYPE = "jdbcDebugtype";
		String LOGIN_TIMEOUT = "loginTimeout";
		String MAIN_FRAME_STATE = "mainFrameWindowState";
		String MAXIMIMIZE_SESSION_SHEET_ON_OPEN = "maximizeSessionSheetOnOpen";
		String NEW_SESSION_VIEW = "newSessionView";
		String PLUGIN_OBJECTS = "pluginObjects";
		String PLUGIN_STATUSES = "pluginStatuses";
		String PROXY = "proxyPerferences";
		String SCROLLABLE_TABBED_PANES = "useScrollableTabbedPanes";
		String SESSION_PROPERTIES = "sessionProperties";
      String SHOW_ALIASES_TOOL_BAR = "showAliasesToolBar";
		String SHOW_CONTENTS_WHEN_DRAGGING = "showContentsWhenDragging";
		String SHOW_DRIVERS_TOOL_BAR = "showDriversToolBar";
		String SHOW_LOADED_DRIVERS_ONLY = "showLoadedDriversOnly";
		String SHOW_MAIN_STATUS_BAR = "showMainStatusBar";
		String SHOW_MAIN_TOOL_BAR = "showMainToolBar";
		String SHOW_TOOLTIPS = "showToolTips";
		String SHOW_COLOR_ICONS_IN_TOOLBAR="showColorIconsInToolbars";
        String SHOW_PLUGIN_FILES_IN_SPLASH_SCREEN="showPluginFilesInSplashScreen";
      String FILE_OPEN_IN_PREVIOUS_DIR = "fileOpenInPreviousDir";
      String FILE_OPEN_IN_SPECIFIED_DIR = "fileOpenInSpecifiedDir";
      String FILE_SPECIFIED_DIR = "fileSpecifiedDir";
      String FILE_PREVIOUS_DIR = "filePreviousdDir";
   }

	public interface IJdbcDebugTypes
	{
		int NONE = 0;
		int TO_STREAM = 1;
		int TO_WRITER = 2;
	}

	/** Internationalized strings for this class. */
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(SquirrelPreferences.class);

	/** Logger for this class. */
	private final static ILogger s_log =
		LoggerController.createLogger(SquirrelPreferences.class);

	/** Bounds of the main frame. */
	private MainFrameWindowState _mainFrameState = new MainFrameWindowState();

	/** Properties for new sessions. */
	private SessionProperties _sessionProps = new SessionProperties();

	/**
	 * Show contents of internal frames when dragging. <CODE>false</CODE> makes
	 * dragging faster.
	 */
	private boolean _showContentsWhenDragging = false;


   private boolean _fileOpenInPreviousDir = true;

   private boolean _fileOpenInSpecifiedDir = false;

   private String _fileSpecifiedDir = "";

   private String _filePreviousDir = System.getProperty("user.home");

	/** JDBC Debug Type. */
	private int _jdbcDebugType = IJdbcDebugTypes.NONE;

	/** Login timeout (seconds). */
	private int _loginTimeout = 30;

	/** The View to start when a new session is created. */
	// JASON: What are its valid values?
	private String _newSessionView;

	/** Show tooltips for controls. */
	private boolean _showToolTips = true;

	/** Use scrollable tabbed panes. JDK 1.4 and above only. */
	private boolean _useScrollableTabbedPanes = false;

	/** Show main statusbar. */
	private boolean _showMainStatusBar = true;

	/** Show main toolbar. */
	private boolean _showMainToolBar = true;

	/** Show main action toolbar. */
	private boolean _showActionToolBar = true;

	/** Show toolbar in the drivers window. */
	private boolean _showDriversToolBar = true;

	/** Maximize session sheet on open. */
	private boolean _maxSessionSheetOnOpen = false;

	/** Show toolbar in the aliases window. */
	private boolean _showAliasesToolBar = true;

	/** Show color icons in toolbars. */
	private boolean _showColorIconsInToolbars = true;

    /** Show the name of each jar being loaded when loading plugins */
    private boolean _showPluginFilesInSplashScreen = false;
    
	/** Accelerators and mnemonics for actions. */
	private ActionKeys[] _actionsKeys = new ActionKeys[0];

	/** Proxy settings. */
	private ProxySettings _proxySettings = new ProxySettings();

	/** Index in drivers window to be initially selected. */
	private int _driversSelectedIndex = -1;

	/** Index in aliases window to be initially selected. */
	private int _aliasesSelectedIndex = -1;

	/** Show loaded drivers only in the Drivers window. */
	private boolean _showLoadedDriversOnly;

 	/** Is this the first time SQuirreL has been run? */
 	private boolean _firstRun = true;

	/** Confirm closing sessions */
 	private boolean _confirmSessionClose = true;

	/** Collection of <TT>PluginStatus</tt> objects. */
	private final ArrayList _pluginStatusInfoColl = new ArrayList();

	/**
	 * Objects stored by plugins. Each element of this collection is a <TT>Map</TT>
	 * keyed by the plugin's internal name and containing all objects for that
	 * plugin.
	 */
	//private Map _allPluginObjects = new HashMap();

	/** Object to handle property change events. */
	private transient PropertyChangeReporter _propChgReporter;

	/**
	 * Default ctor.
	 */
	public SquirrelPreferences()
	{
		super();
		loadDefaults();
	}

	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		getPropertyChangeReporter().addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
		getPropertyChangeReporter().removePropertyChangeListener(listener);
	}

	public String getNewSessionView()
	{
		return _newSessionView;
	}
	
	public synchronized void setNewSessionView(String data)
	{
		if (((data == null) && (_newSessionView != null)) || (data != null)
				&& !data.equals(_newSessionView))
		{
			final String oldValue = _newSessionView;
			_newSessionView = data;
			getPropertyChangeReporter().firePropertyChange(
					IPropertyNames.NEW_SESSION_VIEW, oldValue, _newSessionView);
		}
	}

   public SessionProperties getSessionProperties()
	{
		return _sessionProps;
	}

	public synchronized void setSessionProperties(SessionProperties data)
	{
		if (_sessionProps != data)
		{
			final SessionProperties oldValue = _sessionProps;
			_sessionProps = data;
			getPropertyChangeReporter().firePropertyChange(IPropertyNames.SESSION_PROPERTIES,
												oldValue, _sessionProps);
		}
	}

	public MainFrameWindowState getMainFrameWindowState()
	{
		return _mainFrameState;
	}

	// TODO: Only set if changed
	public synchronized void setMainFrameWindowState(MainFrameWindowState data)
	{
		final MainFrameWindowState oldValue = _mainFrameState;
		_mainFrameState = data;
		getPropertyChangeReporter().firePropertyChange(IPropertyNames.MAIN_FRAME_STATE,
											oldValue, _mainFrameState);
	}

	public boolean getShowContentsWhenDragging()
	{
		return _showContentsWhenDragging;
	}

	public synchronized void setShowContentsWhenDragging(boolean data)
	{
		if (data != _showContentsWhenDragging)
		{
			final boolean oldValue = _showContentsWhenDragging;
			_showContentsWhenDragging = data;
			getPropertyChangeReporter().firePropertyChange(IPropertyNames.SHOW_CONTENTS_WHEN_DRAGGING,
												oldValue, _showContentsWhenDragging);
		}
	}

	public boolean getShowMainStatusBar()
	{
		return _showMainStatusBar;
	}

	public synchronized void setShowMainStatusBar(boolean data)
	{
		if (data != _showMainStatusBar)
		{
			final boolean oldValue = _showMainStatusBar;
			_showMainStatusBar = data;
			getPropertyChangeReporter().firePropertyChange(IPropertyNames.SHOW_MAIN_STATUS_BAR,
											oldValue, _showMainStatusBar);
		}
	}

	public boolean getShowMainToolBar()
	{
		return _showMainToolBar;
	}

	public synchronized void setShowMainToolBar(boolean data)
	{
		if (data != _showMainToolBar)
		{
			final boolean oldValue = _showMainToolBar;
			_showMainToolBar = data;
			getPropertyChangeReporter().firePropertyChange(IPropertyNames.SHOW_MAIN_TOOL_BAR,
												oldValue, _showMainToolBar);
		}
	}

	public boolean getShowAliasesToolBar()
	{
		return _showAliasesToolBar;
	}

	public synchronized void setShowAliasesToolBar(boolean data)
	{
		if (data != _showAliasesToolBar)
		{
			final boolean oldValue = _showAliasesToolBar;
			_showAliasesToolBar = data;
			getPropertyChangeReporter().firePropertyChange(IPropertyNames.SHOW_ALIASES_TOOL_BAR,
												oldValue, _showAliasesToolBar);
		}
	}

	public boolean getShowDriversToolBar()
	{
		return _showDriversToolBar;
	}

	public synchronized void setShowDriversToolBar(boolean data)
	{
		if (data != _showDriversToolBar)
		{
			final boolean oldValue = _showDriversToolBar;
			_showDriversToolBar = data;
			getPropertyChangeReporter().firePropertyChange(IPropertyNames.SHOW_DRIVERS_TOOL_BAR,
												oldValue, _showDriversToolBar);
		}
	}

	public boolean getShowColoriconsInToolbar()
	{
		return _showColorIconsInToolbars;
	}

	public synchronized void setShowColoriconsInToolbar(boolean data)
	{
		if (data != _showColorIconsInToolbars)
		{
			final boolean oldValue = _showColorIconsInToolbars;
			_showColorIconsInToolbars = data;
			getPropertyChangeReporter().firePropertyChange(IPropertyNames.SHOW_COLOR_ICONS_IN_TOOLBAR,
												oldValue, _showColorIconsInToolbars);
		}
	}

    public boolean getShowPluginFilesInSplashScreen()
    {
        return _showPluginFilesInSplashScreen;
    }

    public synchronized void setShowPluginFilesInSplashScreen(boolean data)
    {
        if (data != _showPluginFilesInSplashScreen)
        {
            final boolean oldValue = _showPluginFilesInSplashScreen;
            _showPluginFilesInSplashScreen = data;
            getPropertyChangeReporter().firePropertyChange(
                            IPropertyNames.SHOW_PLUGIN_FILES_IN_SPLASH_SCREEN,
                            oldValue, 
                            _showPluginFilesInSplashScreen);
        }
    }    
    
	public int getLoginTimeout()
	{
		return _loginTimeout;
	}

	public synchronized void setLoginTimeout(int data)
	{
		if (data != _loginTimeout)
		{
			final int oldValue = _loginTimeout;
			_loginTimeout = data;
			getPropertyChangeReporter().firePropertyChange(IPropertyNames.LOGIN_TIMEOUT,
												oldValue, _loginTimeout);
		}
	}

	public int getJdbcDebugType()
	{
		return _jdbcDebugType;
	}

	public synchronized void setJdbcDebugType(int data)
	{
		if (data < IJdbcDebugTypes.NONE || data > IJdbcDebugTypes.TO_WRITER)
		{
			throw new IllegalArgumentException("Invalid setDebugJdbcToStream of :" + data);
		}

		if (data != _jdbcDebugType)
		{
			final int oldValue = _jdbcDebugType;
			_jdbcDebugType = data;
			getPropertyChangeReporter().firePropertyChange(
					IPropertyNames.JDBC_DEBUG_TYPE, oldValue, _jdbcDebugType);
		}
	}

	public boolean getShowToolTips()
	{
		return _showToolTips;
	}

	public synchronized void setShowToolTips(boolean data)
	{
		if (data != _showToolTips)
		{
			final boolean oldValue = _showToolTips;
			_showToolTips = data;
			getPropertyChangeReporter().firePropertyChange(
												IPropertyNames.SHOW_TOOLTIPS,
												oldValue, _showToolTips);
		}
	}

	public boolean useScrollableTabbedPanes()
	{
		return _useScrollableTabbedPanes;
	}

	public synchronized void setUseScrollableTabbedPanes(boolean data)
	{
		if (data != _useScrollableTabbedPanes)
		{
			final boolean oldValue = _useScrollableTabbedPanes;
			_useScrollableTabbedPanes = data;
			getPropertyChangeReporter().firePropertyChange(
										IPropertyNames.SCROLLABLE_TABBED_PANES,
										oldValue, _useScrollableTabbedPanes);
		}
	}

	public boolean getMaximizeSessionSheetOnOpen()
	{
		return _maxSessionSheetOnOpen;
	}

	public synchronized void setMaximizeSessionSheetOnOpen(boolean data)
	{
		if (data != _maxSessionSheetOnOpen)
		{
			final boolean oldValue = _maxSessionSheetOnOpen;
			_maxSessionSheetOnOpen= data;
			getPropertyChangeReporter().firePropertyChange(
							IPropertyNames.MAXIMIMIZE_SESSION_SHEET_ON_OPEN,
							oldValue, _maxSessionSheetOnOpen);
		}
	}

	public ActionKeys[] getActionKeys()
	{
		return _actionsKeys;
	}

	public ActionKeys getActionKeys(int idx)
	{
		return _actionsKeys[idx];
	}

	// TODO: Only set if changed? May not be practical.
	public synchronized void setActionKeys(ActionKeys[] data)
	{
		final ActionKeys[] oldValue = _actionsKeys;
		_actionsKeys = data != null ? data : new ActionKeys[0];
		getPropertyChangeReporter().firePropertyChange(IPropertyNames.ACTION_KEYS,
											oldValue, _actionsKeys);
	}

	// TODO: Only set if changed? May not be practical.
	public void setActionKeys(int idx, ActionKeys value)
	{
		final ActionKeys[] oldValue = _actionsKeys;
		_actionsKeys[idx] = value;
		getPropertyChangeReporter().firePropertyChange(IPropertyNames.ACTION_KEYS,
											oldValue, _actionsKeys);
	}

	public synchronized PluginStatus[] getPluginStatuses()
	{
		final PluginStatus[] ar = new PluginStatus[_pluginStatusInfoColl.size()];
		return (PluginStatus[])_pluginStatusInfoColl.toArray(ar);
	}

	public PluginStatus getPluginStatus(int idx)
	{
		return (PluginStatus)_pluginStatusInfoColl.get(idx);
	}

	// TODO: Only set if changed? May not be practical.
	public synchronized void setPluginStatuses(PluginStatus[] data)
	{
		if (data == null)
		{
			data = new PluginStatus[0];
		}

		PluginStatus[] oldValue = new PluginStatus[_pluginStatusInfoColl.size()];
		oldValue = (PluginStatus[])_pluginStatusInfoColl.toArray(oldValue);
		_pluginStatusInfoColl.clear();
		_pluginStatusInfoColl.addAll(Arrays.asList(data));
		getPropertyChangeReporter().firePropertyChange(IPropertyNames.PLUGIN_STATUSES,
											oldValue, data);
	}

	// TODO: Only set if changed? May not be practical.
	public synchronized void setPluginStatus(int idx, PluginStatus value)
	{
		_pluginStatusInfoColl.ensureCapacity(idx + 1);
		final PluginStatus oldValue = (PluginStatus)_pluginStatusInfoColl.get(idx);;
		_pluginStatusInfoColl.set(idx, value);
		getPropertyChangeReporter().firePropertyChange(IPropertyNames.PLUGIN_STATUSES,
											oldValue, value);
	}

	/**
	 * Retrieve the proxy settings. Note that this method returns a clone
	 * of the actual proxy settings used.
	 *
	 * @return	<TT>ProxySettings</TT> object.
	 */
	public ProxySettings getProxySettings()
	{
		return (ProxySettings)_proxySettings.clone();
	}

	// TODO: Only set if changed
	public synchronized void setProxySettings(ProxySettings data)
	{
		if (data == null)
		{
			data = new ProxySettings();
		}
		final ProxySettings oldValue = _proxySettings;
		_proxySettings= data;
		getPropertyChangeReporter().firePropertyChange(IPropertyNames.PROXY,
											oldValue, _proxySettings);
	}

	/**
	 * Retrieve the index of the selected alias in the aliases window.
	 *
	 * @return	the index of the selected alias in the aliases window
	 * 			or -1 if none selected.
	 */
	public int getAliasesSelectedIndex()
	{
		return _aliasesSelectedIndex;
	}

	/**
	 * Set the index of the selected alias.
	 *
	 * @param	idx	the index of the selected alias in the aliases window
	 * 				or -1 if none selected.
	 */
	public synchronized void setAliasesSelectedIndex(int idx)
	{
		if (idx != _aliasesSelectedIndex)
		{
			final int oldValue = _aliasesSelectedIndex;
			_aliasesSelectedIndex = idx;
			getPropertyChangeReporter().firePropertyChange(
										IPropertyNames.ALIASES_SELECTED_INDEX,
										oldValue, _aliasesSelectedIndex);
		}
	}

	/**
	 * Retrieve the index of the selected driver in the drivers window.
	 *
	 * @return	the index of the selected driver in the drivers window
	 * 			or -1 if none selected.
	 */
	public int getDriversSelectedIndex()
	{
		return _driversSelectedIndex;
	}

	/**
	 * Set the index of the selected driver.
	 *
	 * @param	idx	the index of the selected driver in the drivers window
	 * 				or -1 if none selected.
	 */
	public synchronized void setDriversSelectedIndex(int idx)
	{
		if (idx != _driversSelectedIndex)
		{
			final int oldValue = _driversSelectedIndex;
			_driversSelectedIndex = idx;
			getPropertyChangeReporter().firePropertyChange(
										IPropertyNames.DRIVERS_SELECTED_INDEX,
										oldValue, _driversSelectedIndex);
		}
	}

	/**
	 * @return	whether only the loaded JDBC drivers are displayed in the
	 *			Drivers window.
	 */
	public boolean getShowLoadedDriversOnly()
	{
		return _showLoadedDriversOnly;
	}

	/**
	 * Set whether only the loaded JDBC drivers are displayed in the
	 * Drivers window.
	 *
	 * @param	data	New value for this property.
	 */
	public synchronized void setShowLoadedDriversOnly(boolean data)
	{
		if (data != _showLoadedDriversOnly)
		{
			final boolean oldValue = _showLoadedDriversOnly;
			_showLoadedDriversOnly = data;
			getPropertyChangeReporter().firePropertyChange(
										IPropertyNames.SHOW_LOADED_DRIVERS_ONLY,
										oldValue, _showLoadedDriversOnly);
		}
	}

 	/**
 	 * Is this the first time SQuirreL has been run?
 	 *
 	 * @return	<tt>true</tt> if this is the first time SQuirreL has been run
 	 *			else <tt>false</tt>.
 	 */
 	public boolean isFirstRun()
 	{
 		return _firstRun;
 	}

 	public synchronized void setFirstRun(boolean data)
 	{
 		if (data != _firstRun)
 		{
 			final boolean oldValue = _firstRun;
 			_firstRun = data;
 			getPropertyChangeReporter().firePropertyChange(IPropertyNames.FIRST_RUN,
 											oldValue, _firstRun);
 		}
 	}
 	/**
 	 * Should user confirm whether sessions should be closed.
 	 *
 	 * @return	<tt>true</tt> if user should have to confirm session close
 	 *			else <tt>false</tt>.
 	 */
 	public boolean getConfirmSessionClose()
 	{
 		return _confirmSessionClose;
 	}

 	public synchronized void setConfirmSessionClose(boolean data)
 	{
 		if (data != _confirmSessionClose)
 		{
 			final boolean oldValue = _confirmSessionClose;
 			_confirmSessionClose = data;
 			getPropertyChangeReporter().firePropertyChange(
 										IPropertyNames.CONFIRM_SESSION_CLOSE,
 										oldValue, _confirmSessionClose);
 		}
 	}


   public boolean isFileOpenInPreviousDir()
   {
      return _fileOpenInPreviousDir;
   }

   public synchronized void setFileOpenInPreviousDir(boolean data)
   {
      if (data != _fileOpenInPreviousDir)
      {
         final boolean oldValue = _fileOpenInPreviousDir;
         _fileOpenInPreviousDir = data;
         getPropertyChangeReporter().firePropertyChange(
                              IPropertyNames.FILE_OPEN_IN_PREVIOUS_DIR,
                              oldValue, _fileOpenInPreviousDir);
      }
   }


   public boolean isFileOpenInSpecifiedDir()
   {
      return _fileOpenInSpecifiedDir;
   }

   public synchronized void setFileOpenInSpecifiedDir(boolean data)
   {
      if (data != _fileOpenInSpecifiedDir)
      {
         final boolean oldValue = _fileOpenInSpecifiedDir;
         _fileOpenInSpecifiedDir = data;
         getPropertyChangeReporter().firePropertyChange(
                              IPropertyNames.FILE_OPEN_IN_SPECIFIED_DIR,
                              oldValue, _fileOpenInSpecifiedDir);
      }
   }

   public String getFileSpecifiedDir()
   {
      return _fileSpecifiedDir;
   }

   public synchronized void setFileSpecifiedDir(String data)
   {
      if (false == ("" + data).equals(_fileSpecifiedDir))
      {
         final String oldValue = _fileSpecifiedDir;
         _fileSpecifiedDir = data;
         getPropertyChangeReporter().firePropertyChange(
                              IPropertyNames.FILE_SPECIFIED_DIR,
                              oldValue, _fileSpecifiedDir);
      }
   }

   public String getFilePreviousDir()
   {
      return _filePreviousDir;
   }

   public synchronized void setFilePreviousDir(String data)
   {
      if (false == ("" + data).equals(_filePreviousDir))
      {
         final String oldValue = _filePreviousDir;
         _filePreviousDir = data;
         getPropertyChangeReporter().firePropertyChange(
                              IPropertyNames.FILE_PREVIOUS_DIR,
                              oldValue, _filePreviousDir);
      }
   }



	/**
	 * Helper method.
	 */
	public boolean isJdbcDebugToStream()
	{
		return _jdbcDebugType == IJdbcDebugTypes.TO_STREAM;
	}

	/**
	 * Helper method.
	 */
	public boolean isJdbcDebugToWriter()
	{
		return _jdbcDebugType == IJdbcDebugTypes.TO_WRITER;
	}

	/**
	 * Helper method.
	 */
	public boolean isJdbcDebugDontDebug()
	{
		return !(isJdbcDebugToStream() || isJdbcDebugToWriter());
	}

	/**
	 * Helper method.
	 */
	public void doJdbcDebugToStream()
	{
		setJdbcDebugType(IJdbcDebugTypes.TO_STREAM);
	}

	/**
	 * Helper method.
	 */
	public void doJdbcDebugToWriter()
	{
		setJdbcDebugType(IJdbcDebugTypes.TO_WRITER);
	}

	/**
	 * Helper method.
	 */
	public void dontDoJdbcDebug()
	{
		setJdbcDebugType(IJdbcDebugTypes.NONE);
	}

	public static SquirrelPreferences load()
	{
		File prefsFile = new ApplicationFiles().getUserPreferencesFile();
		try
		{
			XMLBeanReader doc = new XMLBeanReader();
			doc.load(prefsFile);
			Iterator it = doc.iterator();
			if (it.hasNext())
			{
				return (SquirrelPreferences)it.next();

			}
		}
		catch (FileNotFoundException ignore)
		{
			// property file not found for user - first time user ran pgm.
		}
		catch (Exception ex)
		{
			s_log.error(s_stringMgr.getString("SquirrelPreferences.error.reading", prefsFile.getPath()), ex);
		}
		return new SquirrelPreferences();
	}

	/**
	 * Save preferences to disk.
	 */
	public synchronized void save()
	{
		File prefsFile = new ApplicationFiles().getUserPreferencesFile();
		try
		{
			XMLBeanWriter wtr = new XMLBeanWriter(this);
			wtr.save(prefsFile);
		}
		catch (Exception ex)
		{
			s_log.error(s_stringMgr.getString("SquirrelPreferences.error.writing",
												prefsFile.getPath()), ex);
		}
	}

	private void loadDefaults()
	{
		if (_loginTimeout == -1)
		{
			_loginTimeout = DriverManager.getLoginTimeout();
		}
	}

	private synchronized PropertyChangeReporter getPropertyChangeReporter()
	{
		if (_propChgReporter == null)
		{
			_propChgReporter = new PropertyChangeReporter(this);
		}
		return _propChgReporter;
	}

}
