package net.sourceforge.squirrel_sql.fw.dialects;
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
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase {
	public static Test suite() {
		TestSuite suite = new TestSuite("SQL dialect tests");
        suite.addTestSuite(AxionDialectTest.class);
        suite.addTestSuite(DaffodilDialectTest.class);
        suite.addTestSuite(DB2DialectTest.class);
        suite.addTestSuite(DerbyDialectTest.class);
        suite.addTestSuite(FirebirdDialectTest.class);
        suite.addTestSuite(FrontBaseDialectTest.class);
        suite.addTestSuite(H2DialectTest.class);
        suite.addTestSuite(HADBDialectTest.class);
        suite.addTestSuite(HSQLDialectTest.class);
        suite.addTestSuite(InformixDialectTest.class);
        suite.addTestSuite(IngresDialectTest.class);
        suite.addTestSuite(InterbaseDialectTest.class);
        suite.addTestSuite(MAXDBDialectTest.class);
        suite.addTestSuite(McKoiDialectTest.class);
        suite.addTestSuite(MySQLDialectTest.class);
        suite.addTestSuite(Oracle9iDialectTest.class);
        suite.addTestSuite(PointbaseDialectTest.class);
        suite.addTestSuite(PostgreSQLDialectTest.class);
        suite.addTestSuite(SQLServerDialectTest.class);
        suite.addTestSuite(SybaseDialectTest.class);
        suite.addTestSuite(TimesTenDialectTest.class);
		return suite;
	}
}
