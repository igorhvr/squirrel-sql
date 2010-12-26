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
package net.sourceforge.squirrel_sql.plugins.mysql;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import net.sourceforge.squirrel_sql.client.plugin.AbstractSessionPluginTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class MysqlPluginTest extends AbstractSessionPluginTest
{	
	
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		classUnderTest = new MysqlPlugin();
	}

	@After
	public void tearDown() throws Exception
	{
		classUnderTest = null;
	}

	@Test
	public void testIsPluginSessionMySQL5() throws Exception
	{
		testIsPluginSession(MYSQL_PRODUCT_NAME, MYSQL_5_PRODUCT_VERSION, true);
	}

	@Test
	public void testIsPluginSessionMySQL4() throws Exception
	{
		testIsPluginSession(MYSQL_PRODUCT_NAME, MYSQL_4_PRODUCT_VERSION, true);
	}

	@Test
	public void testIsPluginSessionPostgreSQL() throws Exception {
		testIsPluginSession(POSTGRESQL_PRODUCT_NAME, POSTGRESQL_8_2_PRODUCT_VERSION, false);		
	}
		
	// Helper methods
	
	private void testIsPluginSession(String productName, String productVersion, boolean isPluginSession)
		throws Exception
	{
		when(super.mockSQLDatabaseMetaData.getDatabaseProductName()).thenReturn(productName);
		when(super.mockSQLDatabaseMetaData.getDatabaseProductVersion()).thenReturn(productVersion);
		
		boolean result = ((MysqlPlugin)classUnderTest).isPluginSession(mockSession);

		assertEquals("isPluginSession() != expected value: ",
			isPluginSession, result);

	}

	@Override
	protected String getDatabaseProductName()
	{
		return "mysql";
	}

	@Override
	protected String getDatabaseProductVersion()
	{
		return "5";
	}
		

}
