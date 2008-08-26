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
package net.sourceforge.squirrel_sql.client.update.gui.installer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.squirrel_sql.client.update.UpdateUtil;
import net.sourceforge.squirrel_sql.client.update.gui.ArtifactAction;
import net.sourceforge.squirrel_sql.client.update.gui.ArtifactStatus;
import net.sourceforge.squirrel_sql.client.update.gui.installer.event.InstallEventType;
import net.sourceforge.squirrel_sql.client.update.gui.installer.event.InstallStatusEvent;
import net.sourceforge.squirrel_sql.client.update.gui.installer.event.InstallStatusEventFactory;
import net.sourceforge.squirrel_sql.client.update.gui.installer.event.InstallStatusListener;
import net.sourceforge.squirrel_sql.client.update.gui.installer.util.InstallFileOperationInfo;
import net.sourceforge.squirrel_sql.client.update.gui.installer.util.InstallFileOperationInfoFactory;
import net.sourceforge.squirrel_sql.client.update.xmlbeans.ChangeListXmlBean;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

/**
 * This class is used by the PreLaunchUpdateApplication to install artifact files that belong to a particular
 * change list.
 * 
 * @author manningr
 */
public class ArtifactInstallerImpl implements ArtifactInstaller
{

	/** Logger for this class. */
	private static ILogger s_log = LoggerController.createLogger(ArtifactInstallerImpl.class);

	/** 
	 * this list is derived from the changelist xmlbean.  It will only contain the artifacts that need to 
	 * be change (installed, removed).
	 */
	private List<ArtifactStatus> _changeList = null;
	
	/** listeners to notify of important install events */
	private List<InstallStatusListener> _listeners = new ArrayList<InstallStatusListener>();

	/**
	 * the top-level directory beneath which reside all files needed for updating the application (e.g.
	 * /opt/squirrel/update)
	 */
	private File updateDir = null;

	// Download directories
	
	/** the downlaods root directory (e.g. /opt/squirrel/update/downloads) */
	private File downloadsRootDir = null;
	
	/** the core sub-directory of the backup directory (e.g. /opt/squirrel/update/downloads/core) */
	private File coreDownloadsDir = null;

	/** the plugin sub-directory of the backup directory (e.g. /opt/squirrel/update/downloads/plugin) */
	private File pluginDownloadsDir = null;
	
	/** the i18n sub-directory of the backup directory (e.g. /opt/squirrel/update/downloads/i18n) */
	private File i18nDownloadsDir = null;	
	
	// Backup directories

	/** the backup directory (e.g. /opt/squirrel/update/backup) */
	private File backupRootDir = null;

	/** the core sub-directory of the backup directory (e.g. /opt/squirrel/update/backup/core) */
	private File coreBackupDir = null;

	/** the plugin sub-directory of the backup directory (e.g. /opt/squirrel/update/backup/plugin) */
	private File pluginBackupDir = null;

	/** the i18n sub-directory of the backup directory (e.g. /opt/squirrel/update/backup/i18n) */
	private File translationBackupDir = null;

	// Install directories

	/** the top-level SQuirreL installation direction where launch scripts are (e.g. /opt/squirrel) */
	private File installRootDir = null;

	/** the lib directory where most core jars are (e.g. /opt/squirrel/lib) */
	private File coreInstallDir = null;

	/** the plugins directory where all of the plugin files are (e.g. /opt/squirrel/plugins) */
	private File pluginInstallDir = null;

	/** the lib directory where translation jars are (e.g. /opt/squirrel/lib) */
	private File i18nInstallDir = null;
	
	/** 
	 * the file that was used to build a ChangeListXmlBean that we are using to determine which files need to
	 * be installed/removed
	 */
	private File changeListFile = null;
		
	/* Spring-injected dependencies */

	/** Spring-injected factory for creating install events */
	private InstallStatusEventFactory installStatusEventFactory = null;
	public void setInstallStatusEventFactory(InstallStatusEventFactory installStatusEventFactory)
	{
		this.installStatusEventFactory = installStatusEventFactory;
	}

	/** Spring-injected factory for creating file operation infos */
	private InstallFileOperationInfoFactory installFileOperationInfoFactory = null;
	public void setInstallFileOperationInfoFactory(
		InstallFileOperationInfoFactory installFileOperationInfoFactory)
	{
		this.installFileOperationInfoFactory = installFileOperationInfoFactory;
	}

	/** Utility which provides path information and abstraction to file operations */
	private UpdateUtil _util = null;
	public void setUpdateUtil(UpdateUtil util)
	{
		this._util = util;
		updateDir = _util.getSquirrelUpdateDir();
		backupRootDir = _util.checkDir(updateDir, UpdateUtil.BACKUP_ROOT_DIR_NAME);
		downloadsRootDir = _util.checkDir(updateDir, UpdateUtil.DOWNLOADS_DIR_NAME);
		
		coreBackupDir = _util.checkDir(backupRootDir, UpdateUtil.CORE_ARTIFACT_ID);
		pluginBackupDir = _util.checkDir(backupRootDir, UpdateUtil.PLUGIN_ARTIFACT_ID);
		translationBackupDir = _util.checkDir(backupRootDir, UpdateUtil.TRANSLATION_ARTIFACT_ID);

		installRootDir = _util.getSquirrelHomeDir();

		coreInstallDir = _util.getSquirrelLibraryDir();
		pluginInstallDir = _util.getSquirrelPluginsDir();
		i18nInstallDir = _util.getSquirrelLibraryDir();
		
		coreDownloadsDir = _util.getCoreDownloadsDir();
		pluginDownloadsDir = _util.getPluginDownloadsDir();
		i18nDownloadsDir = _util.getI18nDownloadsDir();
	}	
	
	
	/**
	 * @see net.sourceforge.squirrel_sql.client.update.gui.installer.ArtifactInstaller#setChangeList(
	 * net.sourceforge.squirrel_sql.client.update.xmlbeans.ChangeListXmlBean)
	 */
	public void setChangeList(ChangeListXmlBean changeList) throws FileNotFoundException
	{
		_changeList = initializeChangeList(changeList);
	}

	/**
	 * @see net.sourceforge.squirrel_sql.client.update.gui.installer.ArtifactInstaller#getChangeListFile()
	 */
	public File getChangeListFile()
	{
		return changeListFile;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.client.update.gui.installer.ArtifactInstaller#
	 * setChangeListFile(java.io.File)
	 */
	public void setChangeListFile(File changeListFile)
	{
		this.changeListFile = changeListFile;
	}
	
	/**
	 * @see net.sourceforge.squirrel_sql.client.update.gui.installer.ArtifactInstaller#
	 * addListener(net.sourceforge.squirrel_sql.client.update.gui.installer.event.InstallStatusListener)
	 */
	public void addListener(InstallStatusListener listener)
	{
		_listeners.add(listener);
	}

	/**
	 * @see net.sourceforge.squirrel_sql.client.update.gui.installer.ArtifactInstaller#backupFiles()
	 */
	public boolean backupFiles() throws FileNotFoundException, IOException
	{
		boolean result = true;
		sendBackupStarted();

		File localReleaseFile = _util.getLocalReleaseFile();
		_util.copyFile(localReleaseFile, _util.getBackupDir());
		
		for (ArtifactStatus status : _changeList)
		{
			String artifactName = status.getName();
			// Skip files that are not installed - new files
			if (!status.isInstalled())
			{
				if (s_log.isInfoEnabled())
				{
					s_log.info("Skipping backup of artifact (" + status + ") which isn't installed.");
				}
				continue;
			}
			if (status.isCoreArtifact())
			{
				File installDir = getCoreArtifactLocation(artifactName, installRootDir, coreInstallDir);
				File coreFile = _util.getFile(installDir, artifactName);
				File backupFile = _util.getFile(coreBackupDir, artifactName);
				_util.copyFile(coreFile, backupFile);
			}
			if (status.isPluginArtifact())
			{
				// artifact name for plugins is <plugin internal name>.zip
				File pluginBackupFile = _util.getFile(pluginBackupDir, artifactName);
				String pluginDirectory = artifactName.replace(".zip", "");
				String pluginJarFilename = artifactName.replace(".zip", ".jar");

				ArrayList<File> filesToZip = new ArrayList<File>();
				File pluginDirectoryFile = _util.getFile(pluginInstallDir, pluginDirectory);
				if (pluginDirectoryFile.exists()) {
					filesToZip.add(pluginDirectoryFile);
				}
				File pluginJarFile = _util.getFile(pluginInstallDir, pluginJarFilename);
				if (pluginJarFile.exists()) {
					filesToZip.add(pluginJarFile);
				}
				if (filesToZip.size() > 0) {
					_util.createZipFile(pluginBackupFile, filesToZip.toArray(new File[filesToZip.size()]));
				} else {
					s_log.error("Plugin ("+status.getName()+") was listed as already installed, but it's " +
							"files didn't exist and couldn't be backed up: pluginDirectoryFile="+
							pluginDirectoryFile.getAbsolutePath()+" pluginJarFile="+
							pluginJarFile.getAbsolutePath());
				}
			}
			if (status.isTranslationArtifact())
			{
				File translationFile = _util.getFile(i18nInstallDir, artifactName);
				File backupFile = _util.getFile(translationBackupDir, artifactName);
				if (translationFile.exists())
				{
					_util.copyFile(translationFile, backupFile);
				}
			}
		}

		sendBackupComplete();
		return result;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.client.update.gui.installer.ArtifactInstaller#installFiles()
	 */
	public void installFiles() throws IOException
	{
		sendInstallStarted();

		List<File> filesToRemove = new ArrayList<File>();
		List<InstallFileOperationInfo> filesToInstall = new ArrayList<InstallFileOperationInfo>();

		for (ArtifactStatus status : _changeList)
		{
			ArtifactAction action = status.getArtifactAction();
			File installDir = null;
			File fileToCopy = null;
			File fileToRemove = null;
			String artifactName = status.getName();
			boolean isPlugin = false;

			if (status.isCoreArtifact())
			{
				if (action == ArtifactAction.REMOVE)
				{
					s_log.error("Skipping core artifact (" + status.getName() + ") that was marked for removal");
					continue;
				}
				installDir = getCoreArtifactLocation(status.getName(), installRootDir, coreInstallDir);
				fileToCopy = _util.getFile(coreDownloadsDir, artifactName);
				fileToRemove = _util.getFile(installDir, artifactName);
				filesToRemove.add(fileToRemove);
			}
			if (status.isPluginArtifact())
			{
				isPlugin = true;
				installDir = pluginInstallDir;
				if (action != ArtifactAction.REMOVE) {
					fileToCopy = _util.getFile(pluginDownloadsDir, artifactName);
				}
				
				// Need to remove the existing jar in the plugins directory and all of the files beneath the
				// plugin-named directory.
				String jarFileToRemove = artifactName.replace(".zip", ".jar");
				String pluginDirectoryToRemove = artifactName.replace(".zip", "");

				filesToRemove.add(_util.getFile(installDir, jarFileToRemove));
				filesToRemove.add(_util.getFile(installDir, pluginDirectoryToRemove));
			}
			if (status.isTranslationArtifact())
			{
				installDir = i18nInstallDir;
				if (action != ArtifactAction.REMOVE) {
					fileToCopy = _util.getFile(i18nDownloadsDir, artifactName);
				}
				fileToRemove = _util.getFile(installDir, artifactName);
				filesToRemove.add(fileToRemove);
			}
			if (fileToCopy != null) {
				InstallFileOperationInfo info = installFileOperationInfoFactory.create(fileToCopy, installDir);
				info.setPlugin(isPlugin);
				filesToInstall.add(info);
			}
		}
		boolean success = removeOldFiles(filesToRemove);
		success = success && installFiles(filesToInstall);
		success = success && backupAndDeleteChangeListFile();
		success = success && installNewReleaseXmlFile();
		
		if (!success) {
			restoreFilesFromBackup(filesToInstall);
		} 
		
		sendInstallComplete();
	}

	/**
	 * @see net.sourceforge.squirrel_sql.client.update.gui.installer.ArtifactInstaller#restoreBackupFiles(
	 * net.sourceforge.squirrel_sql.client.update.xmlbeans.ChangeListXmlBean)
	 */
	public boolean restoreBackupFiles() throws FileNotFoundException, IOException
	{
		for (ArtifactStatus status : _changeList) {
			String name = status.getName();
			File backupDir = null;
			File installDir = null;
			
			if (status.isCoreArtifact()) {
				backupDir = coreBackupDir;
				installDir = getCoreArtifactLocation(name, installRootDir, coreInstallDir);
			}
			if (status.isPluginArtifact()) {
				backupDir = pluginBackupDir;
				installDir = pluginInstallDir;
			}
			if (status.isTranslationArtifact()) {
				backupDir = translationBackupDir;
				installDir = coreInstallDir; // translations are most likely to be found in core lib dir.
			}
			File backupJarPath = _util.getFile(backupDir, name);
			File installJarPath = _util.getFile(installDir, name);
			
			if (!_util.deleteFile(installJarPath)) { 
				return false;
			} else {
				_util.copyFile(backupJarPath, installJarPath);
			}			
		}
		if (!_util.deleteFile(_util.getLocalReleaseFile())) {
			return false;
		} else {
			File backupReleaseFile = _util.getFile(_util.getBackupDir(), UpdateUtil.RELEASE_XML_FILENAME);
			_util.copyFile(backupReleaseFile, updateDir);
		}
		
		return true;
	}
	
	// Helper methods	
	
	/**
	 * Since it possible that some artifacts haven't changed between releases, and it is time-consuming to read
	 * the contents of the installed file to compute it's checksum, we do that here just once and boil the 
	 * change list down to just those files that have physically changed, by comparing byte-size and checksum.
	 */
	private List<ArtifactStatus> initializeChangeList(ChangeListXmlBean changeListBean) {
		ArrayList<ArtifactStatus> result = new ArrayList<ArtifactStatus>();
		for (ArtifactStatus status : changeListBean.getChanges()) {
			// Always add plugins - there is not a good way to compare plugin zips and their extracted contents
			// at the moment.
			// TODO: Determine the best way to derive the filesize and checksum of the plugin zip that was last 
			// extracted.  Should we keep it around? How about using the current release.xml file ? Come to 
			// think of it, perhaps we shouldn't be computing the checksum of *any* existing files, why don't 
			// we just get it from the current release.xml file?
			if (status.isPluginArtifact()) {
				result.add(status);
				continue;
			}
			
			if (status.getArtifactAction() == ArtifactAction.INSTALL) {
				File installedFileLocation = null;
				// Skip the artifact if it is identical to the one that is already installed
				if (status.isCoreArtifact()) {
					installedFileLocation = 
						_util.getFile(getCoreArtifactLocation(status.getName(), installRootDir, coreInstallDir),
										  status.getName());
				}
				if (status.isTranslationArtifact()) {
					installedFileLocation = _util.getFile(coreInstallDir, status.getName());
				}
				
				long installedSize = installedFileLocation.length();
				if (installedSize == status.getSize()) {
					long installedCheckSum = _util.getCheckSum(installedFileLocation);
					if (installedCheckSum == status.getChecksum()) {
						if (s_log.isDebugEnabled()) {
							s_log.debug("initializeChangeList: found a core/translation artifact that is not " +
									"installed: installedSize= "+installedSize+"installedCheckSum="+
									installedCheckSum+" statusSize="+status.getSize()+" statusChecksum="+
									status.getChecksum());
						}
						continue;
					}
				}
			}
			
			// We have a core or translation file that is not already installed - add it 
			result.add(status);
		}
		
		return result;
	}
	
	
	/* Handle squirrel-sql.jar specially - it lives at the top */
	private File getCoreArtifactLocation(String artifactName, File rootDir, File coreDir) {
		if (UpdateUtil.SQUIRREL_SQL_JAR_FILENAME.equals(artifactName)) {
			return rootDir;
		} else {
			return coreDir;
		}
	}
	
	private void restoreFilesFromBackup(List<InstallFileOperationInfo> filesToInstall)
	{
		// TODO Auto-generated method stub
	}

	private boolean backupAndDeleteChangeListFile()
	{
		boolean result = true;
		if (changeListFile != null) {
			try {
				_util.copyFile(changeListFile, backupRootDir);
				result = _util.deleteFile(changeListFile);
			} catch (IOException e) {
				result = false;
				s_log.error("Unexpected exception: "+e.getMessage(), e);
			}
		} else {
			if (s_log.isInfoEnabled()) {
				s_log.info("moveChangeListFile: Changelist file was null.  Skipping move");
			}
		}
		return result;
	}

	/**
	 * Install the downloaded release.xml file into the updates root directory so that the update knows the 
	 * current release has changed.
	 * 
	 * @return true if install was successful; false otherwise.
	 */
	private boolean installNewReleaseXmlFile() {
		boolean result = true;
				
		try
		{
			_util.deleteFile(_util.getLocalReleaseFile());
		}
		catch (FileNotFoundException e)
		{
			// strange that release xml file wasn't found; but not a problem at this point - just log it.
			if (s_log.isInfoEnabled())
			{
				s_log.info("installNewReleaseXmlFile: release file to be replaced was missing.");
			}
		}
		File downloadReleaseFile = _util.getFile(downloadsRootDir, UpdateUtil.RELEASE_XML_FILENAME);
		try
		{
			_util.copyFile(downloadReleaseFile, updateDir);
		}
		catch (FileNotFoundException e)
		{
			result = false;
			s_log.error("installNewReleaseXmlFile: unexpected exception - "+e.getMessage(), e);
		}
		catch (IOException e)
		{
			result = false;
			s_log.error("installNewReleaseXmlFile: unexpected exception - "+e.getMessage(), e);
		}
		return result;
	}
	
	/**
	 * Removes the specified list of File objects (can represent either a file or directory)
	 * 
	 * @param filesToRemove the files to be removed.
	 * @return true if the remove operation was successful and false otherwise.
	 */
	private boolean removeOldFiles(List<File> filesToRemove)
	{
		boolean result = true;
		for (File fileToRemove : filesToRemove) {
			result = removeOldFile(fileToRemove);
			if (!result) {
				break;
			}
		}
		return result;
	}

	/**
	 * Removes the old file that will be replaced by the new.
	 * 
	 * @param fileToRemove
	 *           the File that represents the file to be removed
	 * @return true if the remove operation was successful and false otherwise.
	 */
	private boolean removeOldFile(File fileToRemove)
	{
		boolean result = true;
		String absolutePath = fileToRemove.getAbsolutePath();
		if (s_log.isDebugEnabled()) {
			s_log.debug("Examining file to remove: "+absolutePath);
		}
		if (fileToRemove.exists()) {		
			try {
				if (s_log.isDebugEnabled()) {
					s_log.debug("Attempting to delete file: "+absolutePath);
				}			
				result = _util.deleteFile(fileToRemove);
				if (!result) {
					s_log.error("Delete operation failed for file/directory: "+absolutePath);
				}
			} catch (SecurityException e) {
				result = false;
				s_log.error("Unexpected security exception: "+e.getMessage());
			}
		} else {
			if (s_log.isInfoEnabled()) {
				s_log.info("Skipping delete of file doesn't appear to exist: "+absolutePath);
			}
		}
		return result;
	}

	private boolean installFiles(List<InstallFileOperationInfo> filesToInstall) throws IOException
	{
		boolean result = true;
		for (InstallFileOperationInfo info : filesToInstall) {
			File installDir = info.getInstallDir();
			File fileToCopy = info.getFileToInstall();
			try {
				installFile(installDir, fileToCopy);
			} catch (Exception e) {
				s_log.error("installFiles: unexpected exception: "+e.getMessage(), e);
				result = false;
				break;
			}
		}
		return result;
	}

	private void installFile(File installDir, File fileToCopy) throws IOException
	{
		if (fileToCopy.getAbsolutePath().endsWith(".zip")) {
			// This file is a zip; it needs to be extracted into the plugins directory. All zips are packaged
			// in such a way that the extraction beneath plugins directory is all that is required.
			_util.extractZipFile(fileToCopy, installDir);
		} else {			
			_util.copyFile(fileToCopy, installDir);
		}
				
	}

	private void sendBackupStarted()
	{
		if (s_log.isInfoEnabled())
		{
			s_log.info("Backup started");
		}

		InstallStatusEvent evt = installStatusEventFactory.create(InstallEventType.BACKUP_STARTED);
		sendEvent(evt);
	}

	private void sendBackupComplete()
	{
		if (s_log.isInfoEnabled())
		{
			s_log.info("Backup complete");
		}
		InstallStatusEvent evt = installStatusEventFactory.create(InstallEventType.BACKUP_COMPLETE);
		sendEvent(evt);
	}

	private void sendInstallStarted()
	{
		if (s_log.isInfoEnabled())
		{
			s_log.info("Install started");
		}
		InstallStatusEvent evt = installStatusEventFactory.create(InstallEventType.INSTALL_STARTED);
		sendEvent(evt);
	}

	private void sendInstallComplete()
	{
		if (s_log.isInfoEnabled())
		{
			s_log.info("Install completed");
		}
		InstallStatusEvent evt = installStatusEventFactory.create(InstallEventType.INSTALL_COMPLETE);
		sendEvent(evt);
	}

	private void sendEvent(InstallStatusEvent evt)
	{
		for (InstallStatusListener listener : _listeners)
		{
			listener.handleInstallStatusEvent(evt);
		}
	}

}
