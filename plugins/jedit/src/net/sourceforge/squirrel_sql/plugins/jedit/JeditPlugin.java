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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.fw.xml.XMLBeanReader;
import net.sourceforge.squirrel_sql.fw.xml.XMLBeanWriter;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.plugin.DefaultSessionPlugin;
import net.sourceforge.squirrel_sql.client.plugin.PluginException;
import net.sourceforge.squirrel_sql.client.plugin.PluginResources;
import net.sourceforge.squirrel_sql.client.preferences.INewSessionPropertiesPanel;
import net.sourceforge.squirrel_sql.client.session.ISQLEntryPanel;
import net.sourceforge.squirrel_sql.client.session.ISQLEntryPanelFactory;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.properties.ISessionPropertiesPanel;
/**
 * The jEdit plugin class. This plugin replaces the standard SQL entry text area
 * with the jEdit edit area.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class JeditPlugin extends DefaultSessionPlugin
{
	/** Logger for this class. */
	private static final ILogger s_log = LoggerController.createLogger(JeditPlugin.class);

	/** Preferences for new sessions. */
	private JeditPreferences _newSessionPrefs;

	/** Folder to store user settings in. */
	private File _userSettingsFolder;

	/** Factory that creates jEdit text controls. */
	private ISQLEntryPanelFactory _jeditFactory;

	/**
	 * Listeners to the jEdit preferences object in each open
	 * session.
	 */
	private Map _prefListeners = new HashMap();

	/** Resources for this plugin. */
	private JeditPluginResources _resources;

	/**
	 * Return the internal name of this plugin.
	 *
	 * @return  the internal name of this plugin.
	 */
	public String getInternalName()
	{
		return "jedit";
	}

	/**
	 * Return the descriptive name of this plugin.
	 *
	 * @return  the descriptive name of this plugin.
	 */
	public String getDescriptiveName()
	{
		return "jEdit Text Area Plugin";
	}

	/**
	 * Returns the current version of this plugin.
	 *
	 * @return  the current version of this plugin.
	 */
	public String getVersion()
	{
		return "0.30";
	}

	/**
	 * Returns the authors name.
	 *
	 * @return  the authors name.
	 */
	public String getAuthor()
	{
		return "Colin Bell, Johan Compagner";
	}

	/**
	 * Returns a comma separated list of other contributors.
	 *
	 * @return	Contributors names.
	 */
	public String getContributors()
	{
		return "Greg Mackness, Gerd Wagner";
	}

	/**
	 * Returns the name of the change log for the plugin. This should
	 * be a text or HTML file residing in the <TT>getPluginAppSettingsFolder</TT>
	 * directory.
	 *
	 * @return    the changelog file name or <TT>null</TT> if plugin doesn't have
	 *             a change log.
	 */
	public String getChangeLogFileName()
	{
		return "changes.txt";
	}

	/**
	 * Returns the name of the Help file for the plugin. This should
	 * be a text or HTML file residing in the <TT>getPluginAppSettingsFolder</TT>
	 * directory.
	 *
	 * @return    the Help file name or <TT>null</TT> if plugin doesn't have
	 *             a help file.
	 */
	public String getHelpFileName()
	{
		return "readme.html";
	}

	/**
	 * Returns the name of the Licence file for the plugin. This should
	 * be a text or HTML file residing in the <TT>getPluginAppSettingsFolder</TT>
	 * directory.
	 *
	 * @return    the Licence file name or <TT>null</TT> if plugin doesn't have
	 *             a licence file.
	 */
	public String getLicenceFileName()
	{
		return "licence.txt";
	}

	/**
	 * Initialize this plugin.
	 */
	public synchronized void initialize() throws PluginException
	{
		super.initialize();

		_resources = new JeditPluginResources(this);

		// Folder to store user settings.
		try
		{
			_userSettingsFolder = getPluginUserSettingsFolder();
		}
		catch (IOException ex)
		{
			throw new PluginException(ex);
		}

		// Load plugin preferences.
		loadPrefs();

		// Install the jEdit factory for creating SQL entry text controls.
		final IApplication app = getApplication();
		final ISQLEntryPanelFactory originalFactory = app.getSQLEntryPanelFactory();
		_jeditFactory = new JeditSQLEntryPanelFactory(this, originalFactory);
		app.setSQLEntryPanelFactory(_jeditFactory);
	}

	/**
	 * Application is shutting down so save preferences.
	 */
	public void unload()
	{
		savePrefs();
		super.unload();
	}

	/**
	 * Called when a session created but the UI hasn't been built for the
	 * session.
	 *
	 * @param    session    The session that is starting.
	 */
	public void sessionCreated(ISession session)
	{
		super.sessionCreated(session);

		JeditPreferences prefs = null;

		try
		{
			prefs = (JeditPreferences)_newSessionPrefs.clone();
		}
		catch (CloneNotSupportedException ex)
		{
			throw new InternalError("CloneNotSupportedException for JeditPreferences");
		}

		session.putPluginObject(this, JeditConstants.ISessionKeys.PREFS, prefs);

		SessionPreferencesListener lis = new SessionPreferencesListener(this,
												session, prefs);
		prefs.addPropertyChangeListener(lis);
		_prefListeners.put(session.getIdentifier(), lis);
	}

	/**
	 * Called when a session shutdown.
	 *
	 * @param    session    The session that is ending.
	 */
	public void sessionEnding(ISession session)
	{
		session.removePluginObject(this, JeditConstants.ISessionKeys.PREFS);
		_prefListeners.remove(session.getIdentifier());
	}

	/**
	 * Create preferences panel for the New Session Properties dialog.
	 *
	 * @return  preferences panel.
	 */
	public INewSessionPropertiesPanel[] getNewSessionPropertiesPanels()
	{
		return new INewSessionPropertiesPanel[]
		{
			new JeditPreferencesPanel(_newSessionPrefs, _resources)
		};
	}

	/**
	 * Create panels for the Session Properties dialog.
	 *
	 * @return		Array of panels for the properties dialog.
	 */
	public ISessionPropertiesPanel[] getSessionPropertiesPanels(ISession session)
	{
		JeditPreferences sessionPrefs = (JeditPreferences)session.getPluginObject(this,
											JeditConstants.ISessionKeys.PREFS);

		return new ISessionPropertiesPanel[]
		{
			new JeditPreferencesPanel(sessionPrefs, _resources)
		};
	}

	PluginResources getResources()
	{
		return _resources;
	}

	ISQLEntryPanelFactory getJeditFactory()
	{
		return _jeditFactory;
	}

	/**
	 * Load from preferences file.
	 */
	private void loadPrefs()
	{
		try
		{
			final XMLBeanReader doc = new XMLBeanReader();
			final File file = new File(_userSettingsFolder,
					JeditConstants.USER_PREFS_FILE_NAME);
			doc.load(file, getClass().getClassLoader());

			Iterator it = doc.iterator();

			if (it.hasNext())
			{
				_newSessionPrefs = (JeditPreferences)it.next();
			}
		}
		catch (FileNotFoundException ignore)
		{
			// property file not found for user - first time user ran pgm.
		}
		catch (Exception ex)
		{
			final String msg = "Error occured reading from preferences file: " +
				JeditConstants.USER_PREFS_FILE_NAME;
			s_log.error(msg, ex);
		}

		if (_newSessionPrefs == null)
		{
			_newSessionPrefs = new JeditPreferences();
		}
	}

	/**
	 * Save preferences to disk.
	 */
	private void savePrefs()
	{
		try
		{
			final XMLBeanWriter wtr = new XMLBeanWriter(_newSessionPrefs);
			wtr.save(new File(_userSettingsFolder, JeditConstants.USER_PREFS_FILE_NAME));
		}
		catch (Exception ex)
		{
			final String msg = "Error occured writing to preferences file: " +
								JeditConstants.USER_PREFS_FILE_NAME;
			s_log.error(msg, ex);
		}
	}

	private static final class SessionPreferencesListener
		implements PropertyChangeListener
	{
		private JeditPlugin _plugin;
		private ISession _session;
		private JeditPreferences _prefs;
		private boolean _usingJeditControl;

		SessionPreferencesListener(JeditPlugin plugin, ISession session,
			JeditPreferences prefs)
		{
			super();
			_plugin = plugin;
			_session = session;
			_prefs = prefs;
		}

		public void propertyChange(PropertyChangeEvent evt)
		{
			final String propName = evt.getPropertyName();

			if ((propName == null) ||
					propName.equals(JeditPreferences.IPropertyNames.USE_JEDIT_CONTROL))
			{
				synchronized (_session)
				{
					ISQLEntryPanelFactory factory = _plugin.getJeditFactory();
					ISQLEntryPanel pnl = factory.createSQLEntryPanel(_session);
					_session.getSQLPanelAPI(_plugin).installSQLEntryPanel(pnl);
				}
			}

			if ((propName == null) ||
					!propName.equals(
						JeditPreferences.IPropertyNames.USE_JEDIT_CONTROL))
			{
				if (_prefs.getUseJeditTextControl())
				{
					JeditSQLEntryPanel pnl = (JeditSQLEntryPanel)_session.getPluginObject(_plugin,
							JeditConstants.ISessionKeys.JEDIT_SQL_ENTRY_CONTROL);

					if (pnl != null)
					{
						pnl.updateFromPreferences();
					}
				}
			}
		}
	}
}
