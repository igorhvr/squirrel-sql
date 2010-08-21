/*
 * Copyright (C) 2009 Rob Manning
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
package net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table;


import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.AbstractBaseDataSetTabTest;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.fw.sql.dbobj.BestRowIdentifier;

import org.junit.Before;

public class RowIDTabTest extends AbstractBaseDataSetTabTest
{

	@Before
	public void setUp() throws Exception
	{
		super.setUp();
		classUnderTest = new RowIDTab();
		super.clazz = RowIDTab.class;		
		
		expect(mockSQLMetaData.getBestRowIdentifier((ITableInfo) isNull())).andStubReturn(new BestRowIdentifier[] {});
	}

}
