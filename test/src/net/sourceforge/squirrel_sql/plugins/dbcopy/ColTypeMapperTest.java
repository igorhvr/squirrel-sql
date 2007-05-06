package net.sourceforge.squirrel_sql.plugins.dbcopy;
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
import static java.lang.System.out;

import java.sql.ResultSet;

import net.sourceforge.squirrel_sql.BaseSQuirreLTestCase;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.fw.sql.ISQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.sql.JDBCTypeMapper;
import net.sourceforge.squirrel_sql.fw.sql.TableColumnInfo;
import net.sourceforge.squirrel_sql.test.TestUtil;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class ColTypeMapperTest extends BaseSQuirreLTestCase {

    static String[] dbNames = {
        "Axion",
        "Daffodil",
        "DB2",
        "Apache Derby",
        "Firebird",
        "FrontBase",
        "H2",
        "sun java system high availability",
        "HSQL",
        "informix",
        "ingres",
        "interbase",
        "maxdb",
        "mckoi",
        "mysql",
        "oracle",
        "pointbase",
        "postgresql",
        "progress",
        "microsoft",
        "sybase",
        "timesten",
    };
    
    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }
    
    public void  testMapColType() 
    {
        for (String sourceName : dbNames) {
            for (String destName : dbNames) {
                try {
                    out.println("processing source = "+sourceName+" dest = "+
                                destName);
                    testBlobColType(sourceName, destName);
                    testClobColType(sourceName, destName);
                    testDateColType(sourceName, destName);
                    testIntegerColType(sourceName, destName);
                    testLongVarcharColType(sourceName, destName);
                    testVarcharColType(sourceName, destName);
                } catch (Exception e) {
                    out.println("Unexpected exception: "+e.getMessage());
                }
            }
        }
    }

    private void testBlobColType(String fromDb, String toDb) throws Exception 
    {
        ISQLDatabaseMetaData md = TestUtil.getEasyMockSQLMetaData(fromDb);
        TableColumnInfo column = TestUtil.getBlobColumnInfo(md, true);
        // This is for brute force detection of BLOB/CLOB lengths if necessary
        ResultSet rs = createNiceMock(ResultSet.class);
        expect(rs.next()).andReturn(true).once();
        expect(rs.getInt(1)).andReturn(5000).once();
        testColType(md, toDb, column, rs);
    }    

    private void testClobColType(String fromDb, String toDb) throws Exception 
    {
        ISQLDatabaseMetaData md = TestUtil.getEasyMockSQLMetaData(fromDb);
        TableColumnInfo column = TestUtil.getClobColumnInfo(md, true);
        // This is for brute force detection of BLOB/CLOB lengths if necessary
        ResultSet rs = createNiceMock(ResultSet.class);
        expect(rs.next()).andReturn(true).once();
        expect(rs.getInt(1)).andReturn(5000).once();
        testColType(md, toDb, column, rs);        
        testColType(md, toDb, column);
    }    

    
    private void testDateColType(String fromDb, String toDb) throws Exception 
    {
        ISQLDatabaseMetaData md = TestUtil.getEasyMockSQLMetaData(fromDb);
        TableColumnInfo column = TestUtil.getDateColumnInfo(md, true);
        testColType(md, toDb, column);
    }    

    private void testLongVarcharColType(String fromDb, String toDb) 
        throws Exception 
    {
        ISQLDatabaseMetaData md = TestUtil.getEasyMockSQLMetaData(fromDb);
        TableColumnInfo column = 
            TestUtil.getLongVarcharColumnInfo(md, true, Integer.MAX_VALUE);
        testColType(md, toDb, column);
    }        
    
    private void testVarcharColType(String fromDb, String toDb) throws Exception 
    {
        ISQLDatabaseMetaData md = TestUtil.getEasyMockSQLMetaData(fromDb);
        TableColumnInfo column = TestUtil.getVarcharColumnInfo(md, true, 2000);
        testColType(md, toDb, column);
    }    
    
    private void testIntegerColType(String fromDb, String toDb) throws Exception 
    {
        ISQLDatabaseMetaData md = TestUtil.getEasyMockSQLMetaData(fromDb);
        TableColumnInfo column = TestUtil.getIntegerColumnInfo(md, true);
        testColType(md, toDb, column);
    }
    
    private void testColType(ISQLDatabaseMetaData md, 
                             String toDb, 
                             TableColumnInfo column) 
        throws Exception 
    {
        ISession sourceSession = TestUtil.getEasyMockSession(md);
        ISession destSession = TestUtil.getEasyMockSession(toDb);
        
        
        String type = ColTypeMapper.mapColType(sourceSession, 
                                               destSession, 
                                               column, 
                                               "TestTable", 
                                               "TestTable");
        if (JDBCTypeMapper.isNumberType(column.getDataType())) {
            checkType(type, "-1");  
        }
    }

    private void testColType(ISQLDatabaseMetaData md, 
                             String toDb, 
                             TableColumnInfo column,
                             ResultSet rs) 
        throws Exception 
    {
        ISession sourceSession = TestUtil.getEasyMockSession(md, rs);
        ISession destSession = TestUtil.getEasyMockSession(toDb);


        String type = ColTypeMapper.mapColType(sourceSession, 
                                               destSession, 
                                               column, 
                                               "TestTable", 
                                               "TestTable");
        if (JDBCTypeMapper.isNumberType(column.getDataType())) {
            checkType(type, "-1");  
        }
    }
    
    private void checkType(String type, String pattern) {
        if (type.indexOf(pattern) != -1) {
            fail("Found -1 in type: "+type);
        } else {
            out.println(type);
        }
    }
        
    
    
}
