/*
 * Copyright (C) 2007 Rob Manning
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
package net.sourceforge.squirrel_sql.client.update;

import static net.sourceforge.squirrel_sql.client.update.UpdateUtil.RELEASE_XML_FILENAME;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.gui.mainframe.MainFrame;
import net.sourceforge.squirrel_sql.client.plugin.IPluginManager;
import net.sourceforge.squirrel_sql.client.plugin.PluginInfo;
import net.sourceforge.squirrel_sql.client.preferences.GlobalPreferencesActionListener;
import net.sourceforge.squirrel_sql.client.preferences.GlobalPreferencesSheet;
import net.sourceforge.squirrel_sql.client.preferences.UpdatePreferencesPanel;
import net.sourceforge.squirrel_sql.client.update.downloader.ArtifactDownloader;
import net.sourceforge.squirrel_sql.client.update.downloader.event.DownloadEventType;
import net.sourceforge.squirrel_sql.client.update.downloader.event.DownloadStatusEvent;
import net.sourceforge.squirrel_sql.client.update.downloader.event.DownloadStatusListener;
import net.sourceforge.squirrel_sql.client.update.gui.ArtifactAction;
import net.sourceforge.squirrel_sql.client.update.gui.ArtifactStatus;
import net.sourceforge.squirrel_sql.client.update.gui.CheckUpdateListener;
import net.sourceforge.squirrel_sql.client.update.gui.UpdateManagerDialog;
import net.sourceforge.squirrel_sql.client.update.gui.UpdateSummaryDialog;
import net.sourceforge.squirrel_sql.client.update.xmlbeans.ChannelXmlBean;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.util.IUpdateSettings;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

/**
 * This class implements the business logic needed by the view
 * (UpdateManagerDialog), to let the user install new or updated software
 * (the model)
 * 
 * @author manningr
 */
public class UpdateControllerImpl implements UpdateController,
      CheckUpdateListener {

	/** Logger for this class. */
   private static final ILogger s_log =
      LoggerController.createLogger(UpdateControllerImpl.class);

   /** I18n strings for this class */
   private static final StringManager s_stringMgr = 
   	StringManagerFactory.getStringManager(UpdateControllerImpl.class);
   
   /** the application and services it provides */
   private IApplication _app = null;

   /** utility class for low-level update routines */
   private UpdateUtil _util = null;

   /** the release that we downloaded when we last checked */
   private ChannelXmlBean _currentChannelBean = null;

   /** the release we had installed the last time we checked / updated */
   private ChannelXmlBean _installedChannelBean = null;
   
   /** the time that we last checked the server to see if we were uptodate */
   private long _timeOfLastCheck = -1;

   /** Used to be able to bring the update dialog back up after re-config */
   private static GlobalPrefsListener listener = null;
   
   /** The class that we use which is responsible for downloading artifacts */ 
   ArtifactDownloader _downloader = null;
   
   static interface i18n {
   	
   	// i18n[UpdateControllerImpl.downloadingUpdatesMsg=Downloading Files]
   	String DOWNLOADING_UPDATES_MSG = s_stringMgr.getString("UpdateControllerImpl.downloadingUpdatesMsg");
   	
   	// i18n[UpdateControllerImpl.exceptionMsg=Exception was: ]
      String EXCEPTION_MSG = s_stringMgr.getString("UpdateControllerImpl.exceptionMsg");

      // i18n[UpdateControllerImpl.updateCheckFailedTitle=Update Check Failed]
   	String UPDATE_CHECK_FAILED_TITLE = s_stringMgr.getString("UpdateControllerImpl.updateCheckFailedTitle");

   	//i18n[UpdateControllerImpl.softwareVersionCurrentMsg=This software's version is the most recent]
   	String SOFTWARE_VERSION_CURRENT_MSG = 
   		s_stringMgr.getString("UpdateControllerImpl.softwareVersionCurrentMsg");

   	//i18n[UpdateControllerImpl.updateCheckTitle=Update Check]   	
   	String UPDATE_CHECK_TITLE = s_stringMgr.getString("UpdateControllerImpl.updateCheckTitle");
   	
   	//i18n[UpdateControllerImpl.updateDownloadCompleteTitle=Update Download Complete]
   	String UPDATE_DOWNLOAD_COMPLETE_TITLE = 
   		s_stringMgr.getString("UpdateControllerImpl.updateDownloadCompleteTitle");

   	//i18n[UpdateControllerImpl.updateDownloadCompleteMsg=Requested updates will be installed when 
   	//SQuirreL is restarted]
   	String UPDATE_DOWNLOAD_COMPLETE_MSG = 
   		s_stringMgr.getString("UpdateControllerImpl.updateDownloadCompleteMsg");
   	
   	//i18n[UpdateControllerImpl.updateDownloadFailed=Update Download Failed]
   	String UPDATE_DOWNLOAD_FAILED_TITLE = 
   		s_stringMgr.getString("UpdateControllerImpl.updateDownloadFailed");
   	
   	//i18n[UpdateControllerImpl.updateDownloadFailedMsg=Please consult the log for details]
      String UPDATE_DOWNLOAD_FAILED_MSG = 
      	s_stringMgr.getString("UpdateControllerImpl.updateDownloadFailedMsg");

      //i18n[UpdateControllerImpl.releaseFileDownloadFailedMsg=Release file couldn't be downloaded.  Please 
      //check your settings.]
		String RELEASE_FILE_DOWNLOAD_FAILED_MSG = 
			s_stringMgr.getString("UpdateControllerImpl.releaseFileDownloadFailedMsg");
   	
   }
   
   /**
    * Constructor
    * 
    * @param app
    *           the application and services it provides
    */
   public UpdateControllerImpl(IApplication app) {
      _app = app;
      if (listener == null) {
         listener = new GlobalPrefsListener();
         GlobalPreferencesSheet.addGlobalPreferencesActionListener(listener);
      }
   }

   /**
    * Sets the utility class for low-level update routines
    * @param util the Update utility class to use.
    */
   public void setUpdateUtil(UpdateUtil util) {
      this._util = util;
      _util.setPluginManager(_app.getPluginManager());
   }
   
   /**
    * @see net.sourceforge.squirrel_sql.client.update.UpdateController#showUpdateDialog()
    */
   public void showUpdateDialog() {
      JFrame parent = _app.getMainFrame();
      IUpdateSettings settings = getUpdateSettings();
      boolean isRemoteUpdateSite = settings.isRemoteUpdateSite();
      UpdateManagerDialog dialog = 
         new UpdateManagerDialog(parent, isRemoteUpdateSite);
      if (isRemoteUpdateSite) {
         dialog.setUpdateServerName(settings.getUpdateServer());
         dialog.setUpdateServerPort(settings.getUpdateServerPort());
         dialog.setUpdateServerPath(settings.getUpdateServerPath());
         dialog.setUpdateServerChannel(settings.getUpdateServerChannel());
      } else {
         dialog.setLocalUpdatePath(settings.getFileSystemUpdatePath());
      }
      dialog.addCheckUpdateListener(this);
      dialog.setVisible(true);
   }

   /**
    * @see net.sourceforge.squirrel_sql.client.update.UpdateController#isUpToDate()
    */
   public boolean isUpToDate() throws Exception {

      IUpdateSettings settings = getUpdateSettings();
      
      // 1. Find the local release.xml file
      String releaseFilename = _util.getLocalReleaseFile().getAbsolutePath();

      // 2. Load the local release.xml file as a ChannelXmlBean.
      _installedChannelBean = _util.getLocalReleaseInfo(releaseFilename);

      // 3. Determine the channel that the user has (stable or snapshot)
      String channelName = _installedChannelBean.getName();

      StringBuilder releasePath = new StringBuilder("/");
      releasePath.append(getUpdateServerPath());
      releasePath.append("/");
      releasePath.append(channelName);
      releasePath.append("/");

      // 4. Get the release.xml file as a ChannelXmlBean from the server or 
      //    filesystem.
      if (settings.isRemoteUpdateSite()) {

         _currentChannelBean = _util.downloadCurrentRelease(getUpdateServerName(),
                                                            getUpdateServerPortAsInt(),
                                                            releasePath.toString(),
                                                            RELEASE_XML_FILENAME);
      } else {
         _currentChannelBean = 
            _util.loadUpdateFromFileSystem(settings.getFileSystemUpdatePath());
      }

      _timeOfLastCheck = System.currentTimeMillis();

      // 5. Is it the same as the local copy, which was placed either by the
      // installer or the last update?
      return _currentChannelBean.equals(_installedChannelBean);
   }

   /**
    * Returns a set of plugins (internal names) of plugins that are currently
    * installed (regardless of whether or not they are enabled).
    * 
    * @return a set of plugin internal names
    */
   public Set<String> getInstalledPlugins() {
      Set<String> result = new HashSet<String>();
      IPluginManager pmgr = _app.getPluginManager();
      PluginInfo[] infos = pmgr.getPluginInformation();
      for (PluginInfo info : infos) {
         result.add(info.getInternalName());
      }
      return result;
   }

   /**
    * Go get the files that need to be updated.  The specified list could have
    * new files to get (INSTALL), existing files to remove (REMOVE).  This 
    * method's only concern is with fetching the new artifacts to be installed.
    * 
    */
   public void pullDownUpdateFiles(List<ArtifactStatus> artifactStatusList,
         DownloadStatusListener listener) {

      List<ArtifactStatus> newartifactsList = 
         new ArrayList<ArtifactStatus>();
      
      for (ArtifactStatus status : artifactStatusList) {
         if (status.getArtifactAction() == ArtifactAction.INSTALL) {
         	newartifactsList.add(status);
         }
      }
      
      _downloader = new ArtifactDownloader(newartifactsList);
      _downloader.setUtil(_util);
      _downloader.setIsRemoteUpdateSite(isRemoteUpdateSite());
      _downloader.setHost(getUpdateServerName());
      _downloader.setPort(Integer.parseInt(getUpdateServerPort()));
      _downloader.setPath(getUpdateServerPath());
      _downloader.setFileSystemUpdatePath(getUpdateSettings().getFileSystemUpdatePath());
      _downloader.addDownloadStatusListener(listener);
      _downloader.setChannelName(_installedChannelBean.getName());
      _downloader.start();
   }

   /**
    * @see net.sourceforge.squirrel_sql.client.update.UpdateController#getUpdateServerChannel()
    */
   public String getUpdateServerChannel() {
      return getUpdateSettings().getUpdateServerChannel();
   }

   /**
    * @see net.sourceforge.squirrel_sql.client.update.UpdateController#getUpdateServerName()
    */
   public String getUpdateServerName() {
      return getUpdateSettings().getUpdateServer();
   }

   /**
    * @see net.sourceforge.squirrel_sql.client.update.UpdateController#isRemoteUpdateSite()
    */
   public boolean isRemoteUpdateSite() {
      return getUpdateSettings().isRemoteUpdateSite();
   }
   
   /**
    * @see net.sourceforge.squirrel_sql.client.update.UpdateController#getUpdateServerPath()
    */
   public String getUpdateServerPath() {
      return getUpdateSettings().getUpdateServerPath();
   }
   
   /**
    * @see net.sourceforge.squirrel_sql.client.update.UpdateController#getUpdateServerPort()
    */
   public String getUpdateServerPort() {
      return getUpdateSettings().getUpdateServerPort();
   }

   /**
    * @see net.sourceforge.squirrel_sql.client.update.UpdateController#getUpdateServerPortAsInt()
    */
   public int getUpdateServerPortAsInt() {
      return Integer.parseInt(getUpdateServerPort());
   }

   /**
    * @see net.sourceforge.squirrel_sql.client.update.UpdateController#showMessage(java.lang.String,
    *      java.lang.String)
    */
   public void showMessage(String title, String msg) {
      JOptionPane.showMessageDialog(_app.getMainFrame(),
                                    msg,
                                    title,
                                    JOptionPane.INFORMATION_MESSAGE);

   }

   /**
    * @see net.sourceforge.squirrel_sql.client.update.UpdateController#showErrorMessage(java.lang.String, java.lang.String)
    */
   public void showErrorMessage(String title, String msg, Exception e) {
      s_log.error(msg, e);
      JOptionPane.showMessageDialog(_app.getMainFrame(),
                                    msg,
                                    title,
                                    JOptionPane.ERROR_MESSAGE);
      
   }

   /**
    * @see net.sourceforge.squirrel_sql.client.update.UpdateController#showErrorMessage(java.lang.String, java.lang.String)
    */
   public void showErrorMessage(String title, String msg) {
      showErrorMessage(title, msg, null);
   }
   
   /**
    * @see net.sourceforge.squirrel_sql.client.update.UpdateController#checkUpToDate()
    */
   public void checkUpToDate() {
      try {
         if (isUpToDate()) {
            showMessage(i18n.UPDATE_CHECK_TITLE, i18n.SOFTWARE_VERSION_CURRENT_MSG);
         } 
         List<ArtifactStatus> artifactStatusItems = 
            this._util.getArtifactStatus(_currentChannelBean);
         UpdateSummaryDialog dialog = new UpdateSummaryDialog(_app.getMainFrame(),
                                                              artifactStatusItems,
                                                              this);
         String installedVersion = 
            _installedChannelBean.getCurrentRelease().getVersion();
         dialog.setInstalledVersion(installedVersion);
         
         String currentVersion =
            _currentChannelBean.getCurrentRelease().getVersion();
         dialog.setAvailableVersion(currentVersion);
         
         GUIUtils.centerWithinParent(_app.getMainFrame());
         dialog.setVisible(true);
      } catch (FileNotFoundException e) {
      	showErrorMessage(i18n.UPDATE_CHECK_FAILED_TITLE, i18n.RELEASE_FILE_DOWNLOAD_FAILED_MSG);
      } catch (Exception e) {
         showErrorMessage(i18n.UPDATE_CHECK_FAILED_TITLE, i18n.EXCEPTION_MSG
               + e.getClass().getName() + ":" + e.getMessage(), e);
      }
   }

   /**
    * @see net.sourceforge.squirrel_sql.client.update.UpdateController#applyChanges(java.util.List, boolean)
    */
   public void applyChanges(List<ArtifactStatus> artifactStatusList, boolean releaseVersionWillChange) {
      try {
         // Persists the change list to the update directory.
         _util.saveChangeList(artifactStatusList);
               
         // Kick off a thread to go and fetch the files one-by-one and register 
         // callback class - DownloadStatusEventHandler
         pullDownUpdateFiles(artifactStatusList,
                             new DownloadStatusEventHandler());
      
         // if the release version doesn't change, we won't be pulling down core artifacts.  So, we just 
         // need to make sure that all core files have been copied from their installed locations into the 
         // corresponding directory in download, which is in the CLASSPATH of the updater.  This covers the
         // case where the update is being run for the first time after install, and no new version is 
         // available, but the user wants to install/remove plugins and/or translations.
         // TODO: do we need to show the user a progress dialog here ?
         if (!releaseVersionWillChange) {
         	_util.copyDir(_util.getSquirrelLibraryDir(), _util.getCoreDownloadsDir());
         	_util.copyFile(_util.getInstalledSquirrelMainJarLocation(), _util.getCoreDownloadsDir());
         }
      } catch (Exception e) {
         showErrorMessage(i18n.UPDATE_CHECK_FAILED_TITLE, i18n.EXCEPTION_MSG
                          + e.getClass().getName() + ":" + e.getMessage(), e);         
      }
      
   }

   /**
    * @see net.sourceforge.squirrel_sql.client.update.gui.CheckUpdateListener#showPreferences()
    */
   public void showPreferences() {
      // 1. Wait for user to click ok/close
      listener.setWaitingForOk(true);
      
      // 2. Display global preferences
      GlobalPreferencesSheet.showSheet(_app, UpdatePreferencesPanel.class);
   
   }
   
   /* Helper methods */
      
   /**
	 * Returns the UpdateSettings from preferences.
	 * 
	 * @return
	 */
   private IUpdateSettings getUpdateSettings() {
      return _app.getSquirrelPreferences().getUpdateSettings();      
   }
   
   private class GlobalPrefsListener implements GlobalPreferencesActionListener {
      
      private boolean waitingForOk = false;
      
      public void onDisplayGlobalPreferences() {}
      public void onPerformClose() {
         showDialog();        
      }
      public void onPerformOk() {
         showDialog();
      }
      
      /**
       * Re-show the dialog if we were waiting for Ok/Close.
       */
      private void showDialog() {
         // 2. When the user clicks ok, then display update dialog again.
         if (waitingForOk) {
            waitingForOk = false;
            showUpdateDialog();
         }         
      }
      
      /**
       * @param waitingForOk the waitingForOk to set
       */
      public void setWaitingForOk(boolean waitingForOk) {
         this.waitingForOk = waitingForOk;
      }      
   }
   
   /**
    * Listener for download events and handle them appropriately.
    * 
    * @author manningr
    */
   private class DownloadStatusEventHandler implements DownloadStatusListener {

		ProgressMonitor progressMonitor = null;
      int currentFile = 0;
      int totalFiles = 0;
      /**
       * @see net.sourceforge.squirrel_sql.client.update.downloader.event.DownloadStatusListener#
       * 	handleDownloadStatusEvent(net.sourceforge.squirrel_sql.client.update.downloader.event.DownloadStatusEvent)
       */
      public void handleDownloadStatusEvent(DownloadStatusEvent evt) {
         
      	if (progressMonitor != null && progressMonitor.isCanceled()) {
      		_downloader.stopDownload();
      		return;
      	}
      	
         if (evt.getType() == DownloadEventType.DOWNLOAD_STARTED) {
            totalFiles = evt.getFileCountTotal();
            handleDownloadStarted();
         }
         if (evt.getType() == DownloadEventType.DOWNLOAD_FILE_STARTED) {
            setNote("File: "+evt.getFilename());
         }
         
         if (evt.getType() == DownloadEventType.DOWNLOAD_FILE_COMPLETED) {
            setProgress(++currentFile);
         }
         
         if (evt.getType() == DownloadEventType.DOWNLOAD_STOPPED) {
            setProgress(totalFiles);
         }
         
         // When all updates are retrieved, consult the user to see if they want to install now or upon the 
         // next startup.
         if (evt.getType() == DownloadEventType.DOWNLOAD_COMPLETED) {
            showMessage(i18n.UPDATE_DOWNLOAD_COMPLETE_TITLE,
                        i18n.UPDATE_DOWNLOAD_COMPLETE_MSG);
            setProgress(totalFiles);
         }
         if (evt.getType() == DownloadEventType.DOWNLOAD_FAILED) {
            showErrorMessage(i18n.UPDATE_DOWNLOAD_FAILED_TITLE,
                             i18n.UPDATE_DOWNLOAD_FAILED_MSG);
            
            setProgress(totalFiles);
         }
      }
      
      private void setProgress(final int value) {
         GUIUtils.processOnSwingEventThread(new Runnable() {
            public void run() {
               progressMonitor.setProgress(value);     
            }
         });
      }
      
      private void setNote(final String note) {
         GUIUtils.processOnSwingEventThread(new Runnable() {
            public void run() {
               progressMonitor.setNote(note);     
            }
         });
      }
      
      private void handleDownloadStarted() {
         GUIUtils.processOnSwingEventThread(new Runnable() {
            public void run() {
               final MainFrame frame = 
                  UpdateControllerImpl.this._app.getMainFrame();
               progressMonitor = 
                  new ProgressMonitor(frame,
                  						  i18n.DOWNLOADING_UPDATES_MSG,
                  						  i18n.DOWNLOADING_UPDATES_MSG, 
                                      0, 
                                      totalFiles);
               setProgress(0);
            }
         });
      }
      
      
   }
}
