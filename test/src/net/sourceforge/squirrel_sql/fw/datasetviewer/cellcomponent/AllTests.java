package net.sourceforge.squirrel_sql.fw.datasetviewer.cellcomponent;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase {
	public static Test suite() {
		TestSuite suite = new TestSuite("cellcomponent tests");
		suite.addTestSuite(DataTypeDoubleTest.class);
		return suite;
	}
}
