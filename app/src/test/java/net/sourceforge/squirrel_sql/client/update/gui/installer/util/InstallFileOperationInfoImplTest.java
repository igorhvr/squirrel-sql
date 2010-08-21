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
package net.sourceforge.squirrel_sql.client.update.gui.installer.util;

import static org.junit.Assert.*;

import net.sourceforge.squirrel_sql.BaseSQuirreLJUnit4TestCase;
import net.sourceforge.squirrel_sql.fw.util.FileWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.EasyMockHelper;

public class InstallFileOperationInfoImplTest extends BaseSQuirreLJUnit4TestCase 
{

	private InstallFileOperationInfoImpl classUnderTest = null;
	
	EasyMockHelper mockHelper = new EasyMockHelper();
	
	FileWrapper mockFileToInstall = mockHelper.createMock("mockFileToInstall", FileWrapper.class);
	FileWrapper mockInstallDir = mockHelper.createMock("mockInstallDir", FileWrapper.class);
	
	@Before
	public void setUp() throws Exception
	{
		classUnderTest = new InstallFileOperationInfoImpl(mockFileToInstall, mockInstallDir);
	}

	@After
	public void tearDown() throws Exception
	{
		classUnderTest = null;
	}

	@Test
	public void testGetSetFileToInstall()
	{
		FileWrapper mockNewFileToInstall = mockHelper.createMock(FileWrapper.class);
		
		mockHelper.replayAll();
		assertEquals(mockFileToInstall, classUnderTest.getFileToInstall());
		classUnderTest.setFileToInstall(mockNewFileToInstall);
		assertEquals(mockNewFileToInstall, classUnderTest.getFileToInstall());
		mockHelper.verifyAll();
		
	}

	@Test
	public void testGetSetInstallDir()
	{
		FileWrapper mockNewInstallDir = mockHelper.createMock(FileWrapper.class);
		
		mockHelper.replayAll();
		assertEquals(mockInstallDir, classUnderTest.getInstallDir());
		classUnderTest.setInstallDir(mockNewInstallDir);
		assertEquals(mockNewInstallDir, classUnderTest.getInstallDir());
		mockHelper.verifyAll();
	}

	@Test
	public void testGetSetPlugin()
	{
		mockHelper.replayAll();
		assertFalse(classUnderTest.isPlugin());
		classUnderTest.setPlugin(true);
		assertTrue(classUnderTest.isPlugin());
		mockHelper.verifyAll();
	}

}
