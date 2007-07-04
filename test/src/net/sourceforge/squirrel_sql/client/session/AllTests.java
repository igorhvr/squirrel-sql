package net.sourceforge.squirrel_sql.client.session;

/*
 * Copyright (C) 2007 Rob Manning
 * manningr@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terdims of the GNU Lesser General Public
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
import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses( { MessagePanelTest.class,
                 SQLExecuterTaskTest.class })
public class AllTests {
	public static Test suite() {
		TestSuite suite = new TestSuite("Client Session tests");
        suite.addTest(new JUnit4TestAdapter(MessagePanelTest.class));
        suite.addTest(new JUnit4TestAdapter(SessionTest.class));
        suite.addTestSuite(SQLExecuterTaskTest.class);
		return suite;
	}
}
