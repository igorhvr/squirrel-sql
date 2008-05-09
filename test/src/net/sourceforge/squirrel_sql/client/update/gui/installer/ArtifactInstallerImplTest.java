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
package net.sourceforge.squirrel_sql.client.update.gui.installer;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.squirrel_sql.BaseSQuirreLJUnit4TestCase;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.EasyMockHelper;

public class ArtifactInstallerImplTest extends BaseSQuirreLJUnit4TestCase
{

	ArtifactInstallerImpl implUnderTest = null;

	EasyMockHelper helper = new EasyMockHelper();

	private ChangeListXmlBean mockChangeListBean = helper.createMock(ChangeListXmlBean.class);

	private UpdateUtil mockUpdateUtil = helper.createMock(UpdateUtil.class);

	private InstallStatusEventFactory mockInstallStatusEventFactory =
		helper.createMock(InstallStatusEventFactory.class);

	private InstallStatusEvent mockBackupStartedStatusEvent = helper.createMock(InstallStatusEvent.class);

	private InstallStatusEvent mockBackupCompletedStatusEvent = helper.createMock(InstallStatusEvent.class);

	private InstallStatusEvent mockInstallStartedStatusEvent = helper.createMock(InstallStatusEvent.class);

	private InstallStatusEvent mockInstallCompletedStatusEvent = helper.createMock(InstallStatusEvent.class);

	private InstallFileOperationInfoFactory mockInstallFileOperationInfoFactory =
		helper.createMock(InstallFileOperationInfoFactory.class);

	private InstallStatusListener mockInstallStatusListener = helper.createMock(InstallStatusListener.class);

	/* Test Constants */
	private static final String FW_JAR_FILENAME = "fw.jar";

	private static final String SQUIRREL_SQL_JAR_FILENAME = "squirrel-sql.jar";

	private static final String SPRING_JAR_FILENAME = "spring.jar";

	private static final String DBCOPY_ZIP_FILENAME = "dbcopy.zip";
	
	private static final String DBCOPY_JAR_FILENAME = "dbcopy.jar";
	
	private static final String DBCOPY_DIR_FILENAME = "dbcopy";

	private static final String SQUIRREL_SQL_ES_JAR_FILENAME = "squirrel-sql_es.jar";

	private File mockSquirreHomeDirFile = helper.createMock(File.class);

	private File mockSquirreLLibDirFile = helper.createMock(File.class);

	private File mockSquirrelPluginsDirFile = helper.createMock(File.class);

	private File mockUpdateRootDirFile = helper.createMock(File.class);

	private File mockBackupRootDirFile = helper.createMock(File.class);

	private File mockBackupCoreDirFile = helper.createMock(File.class);

	private File mockBackupPluginDirFile = helper.createMock(File.class);

	private File mockBackupTranslationDirFile = helper.createMock(File.class);

	private File mockInstalledFrameworkJarFile = helper.createMock(File.class);

	private File mockBackupFrameworkJarFile = helper.createMock(File.class);

	private File mockInstalledSquirrelSqlJarFile = helper.createMock(File.class);

	private File mockBackupSquirrelSqlJarFile = helper.createMock(File.class);

	private File mockBackupDbCopyZipFile = helper.createMock(File.class);

	private File mockInstalledDbCopyPluginDirFile = helper.createMock(File.class);

	//private File mockInstalledDbCopyPluginJarFile = helper.createMock(File.class);

	private File mockInstalledSquirrelSqlEsJarFile = helper.createMock(File.class);

	private File mockInstalledDbCopyZipFile = helper.createMock(File.class);
	
	private File mockBackupSquirrelSqlEsJarFile = helper.createMock(File.class);

	private File mockDownloadsCoreDirFile = helper.createMock(File.class);

	private File mockDownloadsPluginDirFile = helper.createMock(File.class);

	private File mockDownloadsFrameworkJarFile = helper.createMock(File.class);

	private File mockDownloadsSquirrelSqlJarFile = helper.createMock(File.class);

	private File mockDownloadsSpringJarFile = helper.createMock(File.class);

	private File mockDownloadsDbCopyPluginZipFile = helper.createMock(File.class);
	
	private File mockDownloadsSquirrelSqlEsJarFile = helper.createMock(File.class);
	
	private File mockPathToInstalledDBCopyJarFile = helper.createMock(File.class);
	
	private File mockPathToInstalledDBCopyDirectory = helper.createMock(File.class);
	
	private InstallFileOperationInfo mockInstallSquirrelSqlJarOperationInfo =
		helper.createMock(InstallFileOperationInfo.class);

	private InstallFileOperationInfo mockInstallFrameworkJarOperationInfo =
		helper.createMock(InstallFileOperationInfo.class);

	private InstallFileOperationInfo mockInstallSpringJarOperationInfo =
		helper.createMock(InstallFileOperationInfo.class);

	private InstallFileOperationInfo mockInstallDbCopyZipOperationInfo = 
		helper.createMock(InstallFileOperationInfo.class);

	private InstallFileOperationInfo mockInstallSquirrelSqlEsOperationInfo = 
		helper.createMock(InstallFileOperationInfo.class);
	
	private File mockDownloadsI18nDirFile = helper.createMock(File.class);

	private File mockInstalledSpringJarFile = helper.createMock(File.class);

	@Before
	public void setUp() throws Exception
	{
		helper.resetAll();
		setupUpdateUtil();
		setupFileAbsolutePathExpectations();
	}
	
	private void setupUpdateUtil()
	{
		expect(mockUpdateUtil.getSquirrelHomeDir()).andReturn(mockSquirreHomeDirFile).anyTimes();
		expect(mockUpdateUtil.getSquirrelUpdateDir()).andReturn(mockUpdateRootDirFile).anyTimes();
		expect(mockUpdateUtil.getSquirrelLibraryDir()).andReturn(mockSquirreLLibDirFile).anyTimes();
		expect(mockUpdateUtil.getSquirrelPluginsDir()).andReturn(mockSquirrelPluginsDirFile).anyTimes();
		
		implUnderTest = new ArtifactInstallerImpl();
		implUnderTest.setInstallStatusEventFactory(mockInstallStatusEventFactory);
		implUnderTest.setInstallFileOperationInfoFactory(mockInstallFileOperationInfoFactory);
		implUnderTest.addListener(mockInstallStatusListener);
	}

	private void setupFileAbsolutePathExpectations() {
		
		expect(mockSquirreHomeDirFile.getAbsolutePath()).andReturn("mockSquirreHomeDirFile").anyTimes();

		expect(mockSquirreLLibDirFile.getAbsolutePath()).andReturn("mockSquirreLLibDirFile").anyTimes();

		expect(mockSquirrelPluginsDirFile.getAbsolutePath()).andReturn("mockSquirrelPluginsDirFile").anyTimes();

		expect(mockUpdateRootDirFile.getAbsolutePath()).andReturn("mockUpdateRootDirFile").anyTimes();

		expect(mockBackupRootDirFile.getAbsolutePath()).andReturn("mockBackupRootDirFile").anyTimes();

		expect(mockBackupCoreDirFile.getAbsolutePath()).andReturn("mockBackupCoreDirFile").anyTimes();

		expect(mockBackupPluginDirFile.getAbsolutePath()).andReturn("mockBackupPluginDirFile").anyTimes();

		expect(mockBackupTranslationDirFile.getAbsolutePath()).andReturn("mockBackupTranslationDirFile")
			.anyTimes();

		expect(mockInstalledFrameworkJarFile.getAbsolutePath()).andReturn("mockInstalledFrameworkJarFile")
			.anyTimes();

		expect(mockBackupFrameworkJarFile.getAbsolutePath()).andReturn("mockBackupFrameworkJarFile").anyTimes();

		expect(mockInstalledSquirrelSqlJarFile.getAbsolutePath()).andReturn("mockInstalledSquirrelSqlJarFile")
			.anyTimes();

		expect(mockBackupSquirrelSqlJarFile.getAbsolutePath()).andReturn("mockBackupSquirrelSqlJarFile")
			.anyTimes();

		expect(mockBackupDbCopyZipFile.getAbsolutePath()).andReturn("mockBackupDbCopyZipFile").anyTimes();

		expect(mockInstalledDbCopyPluginDirFile.getAbsolutePath()).andReturn("mockInstalledDbCopyPluginDirFile")
			.anyTimes();

//		expect(mockInstalledDbCopyPluginJarFile.getAbsolutePath()).andReturn("mockInstalledDbCopyPluginJarFile")
//			.anyTimes();

		expect(mockInstalledSquirrelSqlEsJarFile.getAbsolutePath()).andReturn(
			"mockInstalledSquirrelSqlEsJarFile").anyTimes();

		expect(mockInstalledDbCopyZipFile.getAbsolutePath()).andReturn("mockInstalledDbCopyZipFile").anyTimes();

		expect(mockBackupSquirrelSqlEsJarFile.getAbsolutePath()).andReturn("mockBackupSquirrelSqlEsJarFile")
			.anyTimes();

		expect(mockDownloadsCoreDirFile.getAbsolutePath()).andReturn("mockDownloadsCoreDirFile").anyTimes();

		expect(mockDownloadsPluginDirFile.getAbsolutePath()).andReturn("mockDownloadsPluginDirFile").anyTimes();

		expect(mockDownloadsFrameworkJarFile.getAbsolutePath()).andReturn("mockDownloadsFrameworkJarFile")
			.anyTimes();

		expect(mockDownloadsSquirrelSqlJarFile.getAbsolutePath()).andReturn("mockDownloadsSquirrelSqlJarFile")
			.anyTimes();

		expect(mockDownloadsSpringJarFile.getAbsolutePath()).andReturn("mockDownloadsSpringJarFile").anyTimes();

		expect(mockDownloadsDbCopyPluginZipFile.getAbsolutePath()).andReturn("mockDownloadsDbCopyPluginZipFile")
			.anyTimes();

		expect(mockDownloadsSquirrelSqlEsJarFile.getAbsolutePath()).andReturn(
			"mockDownloadsSquirrelSqlEsJarFile").anyTimes();

		expect(mockPathToInstalledDBCopyJarFile.getAbsolutePath()).andReturn("mockPathToInstalledDBCopyJarFile")
			.anyTimes();

		expect(mockPathToInstalledDBCopyDirectory.getAbsolutePath()).andReturn(
			"mockPathToInstalledDBCopyDirectory").anyTimes();
		
		expect(mockInstalledSpringJarFile.getAbsolutePath()).andReturn("mockInstalledSpringJarFile").anyTimes();
	}
	
	@After
	public void tearDown() throws Exception
	{
		implUnderTest = null;
	}

	/**
	 * @param filename
	 *           The name of the file to copy such that when it is appended to the installedDir, it yields the
	 *           specified installedFile
	 * @param installedDir
	 *           the directory in which the file to be backed resides
	 * @param installedFile
	 *           the File that is returned from concatenating installedDir and filename
	 * @param backupDir
	 *           the directory into which the backup file will be copied.
	 * @param backupFile
	 *           the File that represents the file after it is copied into the backup directory
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void setupFileCopyExpectations(String filename, File installedDir, File installedFile,
		File backupDir, File backupFile) throws FileNotFoundException, IOException
	{
		expect(mockUpdateUtil.getFile(installedDir, filename)).andReturn(
			installedFile);
		
		expect(mockUpdateUtil.getFile(backupDir, filename)).andReturn(
			backupFile);
		//expect(backupFile.getAbsolutePath()).andReturn("mockBackupFrameworkJarFile");
		mockUpdateUtil.copyFile(installedFile, backupFile);
	}
	
	
	@Test
	public final void testBackupFiles() throws Exception
	{

		/* expectations that are specific to this test */
		makeCommonUpdateUtilAssertions();

		setupFileCopyExpectations(FW_JAR_FILENAME, mockSquirreLLibDirFile, mockInstalledFrameworkJarFile,
			mockBackupCoreDirFile, mockInstalledFrameworkJarFile);

		setupFileCopyExpectations(SQUIRREL_SQL_JAR_FILENAME, mockSquirreHomeDirFile, 
			mockInstalledSquirrelSqlJarFile, mockBackupCoreDirFile, mockBackupSquirrelSqlJarFile);
		
		expect(mockUpdateUtil.getFile(mockBackupPluginDirFile, "dbcopy.zip")).andReturn(mockBackupDbCopyZipFile);
		
		expect(mockUpdateUtil.getFile(mockSquirrelPluginsDirFile, "dbcopy")).andReturn(
			mockInstalledDbCopyPluginDirFile);
		expect(mockUpdateUtil.getFile(mockSquirrelPluginsDirFile, "dbcopy.jar")).andReturn(
			mockPathToInstalledDBCopyJarFile);
		
		mockUpdateUtil.createZipFile(isA(File.class), isA(File[].class));

		expect(mockUpdateUtil.getFile(mockSquirreLLibDirFile, SQUIRREL_SQL_ES_JAR_FILENAME)).andReturn(
			mockInstalledSquirrelSqlEsJarFile);
		expect(mockUpdateUtil.getFile(mockBackupTranslationDirFile, SQUIRREL_SQL_ES_JAR_FILENAME)).andReturn(
			mockBackupSquirrelSqlEsJarFile);
		expect(mockUpdateUtil.fileExists(mockInstalledSquirrelSqlEsJarFile)).andReturn(true);
		mockUpdateUtil.copyFile(mockInstalledSquirrelSqlEsJarFile, mockBackupSquirrelSqlEsJarFile);

		expect(mockInstallStatusEventFactory.create(InstallEventType.BACKUP_STARTED)).andReturn(
			mockBackupStartedStatusEvent);
		expect(mockInstallStatusEventFactory.create(InstallEventType.BACKUP_COMPLETE)).andReturn(
			mockBackupCompletedStatusEvent);

		expect(mockChangeListBean.getChanges()).andReturn(buildChangeList());

		mockInstallStatusListener.handleInstallStatusEvent(mockBackupStartedStatusEvent);
		mockInstallStatusListener.handleInstallStatusEvent(mockBackupCompletedStatusEvent);

		helper.replayAll();
		implUnderTest.setChangeList(mockChangeListBean);
		implUnderTest.setUpdateUtil(mockUpdateUtil);
		implUnderTest.backupFiles();
		helper.verifyAll();
	}
	
	@Test
	public final void testInstallFiles() throws IOException
	{
		/* expectations that are specific to this test */
		makeCommonUpdateUtilAssertions();

		expect(mockInstallStatusEventFactory.create(InstallEventType.INSTALL_STARTED)).andReturn(
			mockInstallStartedStatusEvent);
		expect(mockInstallStatusEventFactory.create(InstallEventType.INSTALL_COMPLETE)).andReturn(
			mockInstallCompletedStatusEvent);

		mockInstallStatusListener.handleInstallStatusEvent(mockInstallStartedStatusEvent);
		mockInstallStatusListener.handleInstallStatusEvent(mockInstallCompletedStatusEvent);

		expect(mockChangeListBean.getChanges()).andReturn(buildChangeList());
		
		/* expect getFile for updated files that will be removed */
		expect(mockUpdateUtil.getFile(mockSquirreHomeDirFile, SQUIRREL_SQL_JAR_FILENAME)).andReturn(
			mockInstalledSquirrelSqlJarFile);
		expect(mockInstalledSquirrelSqlJarFile.exists()).andReturn(true).anyTimes();
		expect(mockUpdateUtil.deleteFile(mockInstalledSquirrelSqlJarFile)).andReturn(true);
		
		expect(mockUpdateUtil.getFile(mockSquirreLLibDirFile, FW_JAR_FILENAME)).andReturn(
			mockInstalledFrameworkJarFile);
		expect(mockInstalledFrameworkJarFile.exists()).andReturn(true).anyTimes();
		expect(mockUpdateUtil.deleteFile(mockInstalledFrameworkJarFile)).andReturn(true);
		
		expect(mockUpdateUtil.getFile(mockSquirreLLibDirFile, SPRING_JAR_FILENAME)).andReturn(
			mockInstalledSpringJarFile);
		expect(mockInstalledSpringJarFile.exists()).andReturn(true).anyTimes();
		expect(mockUpdateUtil.deleteFile(mockInstalledSpringJarFile)).andReturn(true);
				
		expect(mockUpdateUtil.getFile(mockSquirreLLibDirFile, SQUIRREL_SQL_ES_JAR_FILENAME
			)).andReturn(
			mockInstalledSquirrelSqlEsJarFile);
		expect(mockInstalledSquirrelSqlEsJarFile.exists()).andReturn(false);
		
		/* expect getFile for updated files that were downloaded */
		expect(mockUpdateUtil.getFile(mockDownloadsCoreDirFile, SQUIRREL_SQL_JAR_FILENAME)).andReturn(
			mockDownloadsSquirrelSqlJarFile);
		
		expect(mockUpdateUtil.getFile(mockDownloadsCoreDirFile, FW_JAR_FILENAME)).andReturn(
			mockDownloadsFrameworkJarFile);
		
		expect(mockUpdateUtil.getFile(mockDownloadsCoreDirFile, SPRING_JAR_FILENAME)).andReturn(
			mockDownloadsSpringJarFile);
		
		expect(mockUpdateUtil.getFile(mockDownloadsPluginDirFile, DBCOPY_ZIP_FILENAME)).andReturn(
			mockDownloadsDbCopyPluginZipFile).anyTimes();
		
		expect(mockUpdateUtil.getFile(mockSquirrelPluginsDirFile, DBCOPY_JAR_FILENAME)).andReturn(
			mockPathToInstalledDBCopyJarFile);
		expect(mockPathToInstalledDBCopyJarFile.exists()).andReturn(true);
		expect(mockUpdateUtil.deleteFile(mockPathToInstalledDBCopyJarFile)).andReturn(true);
		
		expect(mockUpdateUtil.getFile(mockSquirrelPluginsDirFile, DBCOPY_DIR_FILENAME)).andReturn(
			mockPathToInstalledDBCopyDirectory);		
		expect(mockPathToInstalledDBCopyDirectory.exists()).andReturn(true);
		
		expect(mockUpdateUtil.deleteFile(mockPathToInstalledDBCopyDirectory)).andReturn(true);
		
		expect(mockUpdateUtil.getFile(mockDownloadsI18nDirFile, SQUIRREL_SQL_ES_JAR_FILENAME)).andReturn(
			mockDownloadsSquirrelSqlEsJarFile);

		boolean isPlugin = true;
		boolean isNotPlugin = false;
		
		/* expected fileOperationInfos for files that will be installed */
		setupFileCopyOperationInfo(mockDownloadsSquirrelSqlJarFile, mockSquirreHomeDirFile,
			mockInstallSquirrelSqlJarOperationInfo, isNotPlugin);
		setupFileCopyOperationInfo(mockDownloadsFrameworkJarFile, mockSquirreLLibDirFile,
			mockInstallFrameworkJarOperationInfo, isNotPlugin);
		setupFileCopyOperationInfo(mockDownloadsSpringJarFile, mockSquirreLLibDirFile,
			mockInstallSpringJarOperationInfo, isNotPlugin);
		setupFileCopyOperationInfo(mockDownloadsDbCopyPluginZipFile, mockSquirrelPluginsDirFile,
			mockInstallDbCopyZipOperationInfo, isPlugin);
		setupFileCopyOperationInfo(mockDownloadsSquirrelSqlEsJarFile, mockSquirreLLibDirFile,
			mockInstallSquirrelSqlEsOperationInfo, isNotPlugin);
									
		helper.replayAll();
		implUnderTest.setChangeList(mockChangeListBean);
		implUnderTest.setUpdateUtil(mockUpdateUtil);
		implUnderTest.installFiles();
		helper.verifyAll();
	}

	private void setupFileCopyOperationInfo(File downloadsFile, File installDir, InstallFileOperationInfo info, boolean isPlugin) throws IOException {
		expect(mockInstallFileOperationInfoFactory.create(downloadsFile, installDir)).andReturn(info);
		expect(info.getInstallDir()).andReturn(installDir);
		expect(info.getFileToInstall()).andReturn(downloadsFile);
		info.setPlugin(isPlugin);
		mockUpdateUtil.copyFile(downloadsFile, installDir);
	}
	
	@Test
	public void testDisallowCoreTypeFileRemoval() throws Exception {
		/* expectations that are specific to this test */
		makeCommonUpdateUtilAssertions();

		expect(mockInstallStatusEventFactory.create(InstallEventType.INSTALL_STARTED)).andReturn(
			mockInstallStartedStatusEvent);
		expect(mockInstallStatusEventFactory.create(InstallEventType.INSTALL_COMPLETE)).andReturn(
			mockInstallCompletedStatusEvent);		
		
		mockInstallStatusListener.handleInstallStatusEvent(mockInstallStartedStatusEvent);
		mockInstallStatusListener.handleInstallStatusEvent(mockInstallCompletedStatusEvent);
		
		
		expect(mockChangeListBean.getChanges()).andReturn(buildRemoveCoreFileChangeList());
		
		helper.replayAll();
		implUnderTest.setChangeList(mockChangeListBean);
		implUnderTest.setUpdateUtil(mockUpdateUtil);
		implUnderTest.installFiles();
		helper.verifyAll();		
	}
	
	private void makeCommonUpdateUtilAssertions()
	{
		expect(mockUpdateUtil.checkDir(mockUpdateRootDirFile, UpdateUtil.BACKUP_ROOT_DIR_NAME)).andReturn(
			mockBackupRootDirFile);
		expect(mockUpdateUtil.checkDir(mockBackupRootDirFile, UpdateUtil.CORE_ARTIFACT_ID)).andReturn(
			mockBackupCoreDirFile);
		expect(mockUpdateUtil.checkDir(mockBackupRootDirFile, UpdateUtil.PLUGIN_ARTIFACT_ID)).andReturn(
			mockBackupPluginDirFile);
		expect(mockUpdateUtil.checkDir(mockBackupRootDirFile, UpdateUtil.TRANSLATION_ARTIFACT_ID)).andReturn(
			mockBackupTranslationDirFile);

		expect(mockUpdateUtil.getCoreDownloadsDir()).andReturn(mockDownloadsCoreDirFile).atLeastOnce();
		expect(mockUpdateUtil.getPluginDownloadsDir()).andReturn(mockDownloadsPluginDirFile).atLeastOnce();
		expect(mockUpdateUtil.getI18nDownloadsDir()).andReturn(mockDownloadsI18nDirFile).atLeastOnce();
	}

	private List<ArtifactStatus> buildRemoveCoreFileChangeList() {
		ArrayList<ArtifactStatus> result = new ArrayList<ArtifactStatus>();
		final String coreType = UpdateUtil.CORE_ARTIFACT_ID;
		final boolean installed = true;
		ArtifactStatus squirrelSqlJarToRemove =
			getArtifactToRemove(SQUIRREL_SQL_JAR_FILENAME, installed, coreType);
		result.add(squirrelSqlJarToRemove);
		return result;
	}
	
	private List<ArtifactStatus> buildChangeList()
	{
		ArrayList<ArtifactStatus> result = new ArrayList<ArtifactStatus>();

		final boolean installed = true;
		final boolean notInstalled = false;
		final String coreType = UpdateUtil.CORE_ARTIFACT_ID;
		final String pluginType = UpdateUtil.PLUGIN_ARTIFACT_ID;
		final String i18nType = UpdateUtil.TRANSLATION_ARTIFACT_ID;

		ArtifactStatus newSquirrelSqlJar = getArtifactToInstall(SQUIRREL_SQL_JAR_FILENAME, true, coreType);
		ArtifactStatus newFrameworkJar = getArtifactToInstall(FW_JAR_FILENAME, installed, coreType);
		ArtifactStatus newSpringJar = getArtifactToInstall(SPRING_JAR_FILENAME, notInstalled, coreType);
		ArtifactStatus newDbcopyZip = getArtifactToInstall(DBCOPY_ZIP_FILENAME, installed, pluginType);
		ArtifactStatus newSquirrelSqlEsJar =
			getArtifactToInstall(SQUIRREL_SQL_ES_JAR_FILENAME, installed, i18nType);

		result.add(newSquirrelSqlJar);
		result.add(newFrameworkJar);
		result.add(newSpringJar);
		result.add(newDbcopyZip);
		result.add(newSquirrelSqlEsJar);
		return result;
	}

	private ArtifactStatus getArtifactToInstall(String name, boolean installed, String type)
	{
		ArtifactStatus result = new ArtifactStatus();
		result.setArtifactAction(ArtifactAction.INSTALL);
		result.setName(name);
		result.setInstalled(installed);
		result.setType(type);
		return result;
	}

	private ArtifactStatus getArtifactToRemove(String name, boolean installed, String type)
	{
		ArtifactStatus result = new ArtifactStatus();
		result.setArtifactAction(ArtifactAction.REMOVE);
		result.setName(name);
		result.setInstalled(installed);
		result.setType(type);
		return result;
	}
	
}
