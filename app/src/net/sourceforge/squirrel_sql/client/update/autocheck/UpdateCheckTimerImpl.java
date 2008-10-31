/*
 * Copyright (C) 2008 Rob Manning
 * manningr@users.sourceforge.net
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
package net.sourceforge.squirrel_sql.client.update.autocheck;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.preferences.IUpdateSettings;
import net.sourceforge.squirrel_sql.client.update.UpdateCheckFrequency;
import net.sourceforge.squirrel_sql.client.update.UpdateController;
import net.sourceforge.squirrel_sql.client.update.UpdateControllerFactory;
import net.sourceforge.squirrel_sql.client.update.UpdateControllerFactoryImpl;
import net.sourceforge.squirrel_sql.client.update.UpdateUtilImpl;
import net.sourceforge.squirrel_sql.client.update.downloader.ArtifactDownloaderFactoryImpl;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

/**
 * This class maintains it's own Thread so that it can periodically check for updates, and notify the user
 * when updates are detected.
 */
public class UpdateCheckTimerImpl implements UpdateCheckTimer
{

	private UpdateCheckRunnable runnable = new UpdateCheckRunnable();

	private UpdateControllerFactory updateControllerFactory = new UpdateControllerFactoryImpl();

	private UpdateController updateController = null;

	private IApplication _app = null;

	private IUpdateSettings _updateSettings = null;

	/** Logger for this class. */
	private static final ILogger s_log = LoggerController.createLogger(UpdateCheckTimerImpl.class);
	
	public UpdateCheckTimerImpl(IApplication app)
	{
		this._app = app;
		_updateSettings = _app.getSquirrelPreferences().getUpdateSettings();
	}

	public void start()
	{
		if (!_updateSettings.isEnableAutomaticUpdates()) {
			return;
		}
		updateController =
			updateControllerFactory.createUpdateController(_app, new ArtifactDownloaderFactoryImpl(),
				new UpdateUtilImpl());
		Thread t = new Thread(runnable);
		t.setName("UpdateCheckTimerImpl Thread");
		t.start();
	}

	public void stop()
	{
		if (!_updateSettings.isEnableAutomaticUpdates()) {
			return;
		}		
		runnable.stop();
	}

	/**
	 * Class that implements the timer functionality.
	 */
	private class UpdateCheckRunnable implements Runnable
	{

		private boolean stopped = false;

		private boolean firstCheck = true;

		/**
		 * After making an initial check This loops indefinitely 
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			// Since this timer must be started when SQuirreL is launched, wait a couple of minutes for
			// SQuirreL to get done initializing the UI.
			sleep(120 * 1000L);

			while (!stopped)
			{
				if (firstCheck)
				{
					firstCheck = false;
					if (isUpdateCheckFrequencyAtStartup() && !isUpToDate())
					{
						logDebug("run: update check configured for startup and software is not up-to-date");
						updateController.promptUserToDownloadAvailableUpdates();
						
						// Since the user only wants to be notified of updates at startup, return here, thereby 
						// freeing up the thread.
						return;
					}
				}
				else
				{
					logDebug("run: not the first check; sleeping for an hour.");
					sleepForAnHour();
				}

				if (!isUpdateCheckFrequencyAtStartup() && updateController.isTimeToCheckForUpdates())
				{
					logDebug("run: not configured to check at startup and it's now time to check again.");
					if (!isUpToDate())
					{
						logDebug("run: software is not up-to-date, so prompting user to download updates.");
						updateController.promptUserToDownloadAvailableUpdates();
					}
				}
			}

		}
		
		private void logDebug(String msg) {
			if (s_log.isDebugEnabled()) {
				s_log.debug(msg);
			}
		}

		private boolean isUpToDate()
		{
			boolean result = true;
			try
			{
				logDebug("isUpToDate: checking to see if software is up-to-date; currentTimeMillis = "+
					System.currentTimeMillis());
				result = updateController.isUpToDate();
			}
			catch (Exception e)
			{
				s_log.error("isUpToDate: Unable to determine up-to-date status: "+e.getMessage(), e);
			}
			return result;
		}

		private boolean isUpdateCheckFrequencyAtStartup()
		{
			String freqStr = _updateSettings.getUpdateCheckFrequency();
			UpdateCheckFrequency updateCheckFrequency = UpdateCheckFrequency.getEnumForString(freqStr);

			return updateCheckFrequency == UpdateCheckFrequency.STARTUP;
		}

		public void stop()
		{
			stopped = true;
		}

		private void sleep(long millis)
		{
			try
			{
				Thread.sleep(millis);
			}
			catch (Exception e)
			{
				/* Do Nothing */
			}
		}
		
		private void sleepForAnHour()
		{
			sleep(1000 * 60 * 60);
		}
		
	}

}
