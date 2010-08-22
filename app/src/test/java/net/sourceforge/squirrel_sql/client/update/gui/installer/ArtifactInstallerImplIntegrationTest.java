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


import net.sourceforge.squirrel_sql.client.ApplicationArguments;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({})
@ContextConfiguration (locations={
	"/net/sourceforge/squirrel_sql/client/update/gui/installer/net.sourceforge.squirrel_sql.client.update.gui.installer.applicationContext.xml",
	"/net/sourceforge/squirrel_sql/client/update/gui/installer/event/net.sourceforge.squirrel_sql.client.update.gui.installer.event.applicationContext.xml",
	"/net/sourceforge/squirrel_sql/client/update/gui/installer/util/net.sourceforge.squirrel_sql.client.update.gui.installer.util.applicationContext.xml",
	"/net/sourceforge/squirrel_sql/client/update/util/net.sourceforge.squirrel_sql.client.update.util.applicationContext.xml"	
})
public class ArtifactInstallerImplIntegrationTest extends AbstractJUnit4SpringContextTests
{	

	static {
		ApplicationArguments.initialize(new String[] { "-home", "./target" });
	}
	
	public static final String beanIdToTest = 
		"net.sourceforge.squirrel_sql.client.update.gui.installer.ArtifactInstaller";
	
	@Test
	public void testLoadBean() throws Exception {
		super.applicationContext.getBean(beanIdToTest);
	}
	
}
