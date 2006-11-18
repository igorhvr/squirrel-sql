package net.sourceforge.squirrel_sql.fw.dialects;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import net.sourceforge.squirrel_sql.client.ApplicationArguments;
import net.sourceforge.squirrel_sql.client.db.dialects.DialectFactory;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.MockSession;
import net.sourceforge.squirrel_sql.fw.sql.TableColumnInfo;

/**
 * The purpose of this class is to hookup to the database(s) specified in 
 * dialectLiveTest.properties and test SQL generation parts of the dialect 
 * syntatically using the database' native parser.  This is not a JUnit test, 
 * as it requires a running database to complete.
 * 
 * @author manningr
 */
public class DialectLiveTestRunner {

    ArrayList sessions = new ArrayList();
    ResourceBundle bundle = null;
    
    TableColumnInfo firstCol = null;
    TableColumnInfo secondCol = null;
    TableColumnInfo thirdCol = null;
    TableColumnInfo fourthCol = null;
    TableColumnInfo dropCol = null;
    TableColumnInfo noDefaultValueVarcharCol = null;
    TableColumnInfo noDefaultValueIntegerCol = null;
    TableColumnInfo renameCol = null;
    TableColumnInfo pkCol = null;
    
    public DialectLiveTestRunner() throws Exception {
        ApplicationArguments.initialize(new String[] {});
        bundle = ResourceBundle.getBundle("net.sourceforge.squirrel_sql.fw.dialects.dialectLiveTest");
        initSessions();
    }
    
    private void initSessions() throws Exception {
        String dbsToTest = bundle.getString("dbsToTest");
        StringTokenizer st = new StringTokenizer(dbsToTest, ",");
        ArrayList dbs = new ArrayList();
        while (st.hasMoreTokens()) {
            String db = st.nextToken().trim();
            dbs.add(db);
        }
        for (Iterator iter = dbs.iterator(); iter.hasNext();) {
            String db = (String) iter.next();
            String url = bundle.getString(db+"_jdbcUrl");
            String user = bundle.getString(db+"_jdbcUser");
            String pass = bundle.getString(db+"_jdbcPass");
            String driver = bundle.getString(db+"_jdbcDriver");
            sessions.add(new MockSession(driver, url, user, pass));            
        }
    }
    
    private void init(ISession session) throws Exception {
        createTestTable(session);
        firstCol = getIntegerColumn("nullint", true, "0", "An int comment");
        secondCol = getIntegerColumn("notnullint", false, "0", "An int comment");
        thirdCol = getVarcharColumn("nullvc", true, "defVal", "A varchar comment");
        fourthCol = getVarcharColumn("notnullvc", false, "defVal", "A varchar comment");
        noDefaultValueVarcharCol = 
            getVarcharColumn("noDefaultVarcharCol", true, null, "A varchar column with no default value"); 
        dropCol = getVarcharColumn("dropCol", true, null, "A varchar comment");        
        noDefaultValueIntegerCol = 
            getIntegerColumn("noDefaultIntgerCol", true, null, "An integer column with no default value");
        renameCol = getVarcharColumn("renameCol", true, null, "A column to be renamed");
        pkCol = getIntegerColumn("pkCol", false, "0", "primary key column");
    }
    
    private void runTests() throws Exception {

        for (Iterator iter = sessions.iterator(); iter.hasNext();) {
            ISession session = (ISession) iter.next();
            init(session);
            testAddColumn(session);
            testDropColumn(session);
            testAlterDefaultValue(session);
            testColumnComment(session);
            testAlterNull(session);
            testAlterName(session);
            testAlterColumnlength(session);
            testAddPrimaryKey(session, new TableColumnInfo[] {pkCol});
        }
    }
    
    private void testAlterName(ISession session) throws Exception {
        HibernateDialect dialect = 
            DialectFactory.getDialect(session, DialectFactory.DEST_TYPE);  

        TableColumnInfo newNameCol = 
            getVarcharColumn("newNameCol", true, null, "A column to be renamed");
        if (dialect.supportsRenameColumn()) {
            String sql = dialect.getColumnNameAlterSQL(renameCol, newNameCol);
            runSQL(session, sql);
        } else {
            try {
                dialect.getColumnNameAlterSQL(renameCol, newNameCol);
            } catch (UnsupportedOperationException e) {
                // this is expected
                System.err.println(e.getMessage());
            }
        }
    }
    
    private void testDropColumn(ISession session) throws Exception {
        HibernateDialect dialect = 
            DialectFactory.getDialect(session, DialectFactory.DEST_TYPE);  
        
        if (dialect.supportsDropColumn()) {
            dropColumn(session, dropCol);
        } else {
            try {
                dropColumn(session, dropCol);
                throw new IllegalStateException(
                        "Expected dialect to fail to provide SQL for dropping a column");
            } catch (UnsupportedOperationException e) {
                // This is what we expect
                System.err.println(e.getMessage());
            }
        }        
    }
    
    private void testAlterColumnlength(ISession session) throws Exception {
        HibernateDialect dialect = 
            DialectFactory.getDialect(session, DialectFactory.DEST_TYPE);  
        
        
        //convert nullint into a varchar(100)
        /*
         * This won't work on Derby where non-varchar columns cannot be 
         * altered among other restrictions.
         * 
        TableColumnInfo nullintVC = 
            getVarcharColumn("nullint", true, "defVal", "A varchar comment");
        String alterColTypeSQL = dialect.getColumnTypeAlterSQL(firstCol, nullintVC);
        runSQL(session, alterColTypeSQL);
        */
        
        TableColumnInfo thirdColLonger = 
            getVarcharColumn("nullvc", true, "defVal", "A varchar comment", 1000);
        String alterColLengthSQL = 
            dialect.getColumnTypeAlterSQL(thirdCol, thirdColLonger);
        runSQL(session, alterColLengthSQL);        
    }
    
    private void testAlterDefaultValue(ISession session) throws Exception {
        HibernateDialect dialect = 
            DialectFactory.getDialect(session, DialectFactory.DEST_TYPE);  
        
        TableColumnInfo varcharColWithDefaultValue = 
            getVarcharColumn("noDefaultVarcharCol", 
                             true, 
                             "Default Value", 
                             "A column with a default value");
        
        TableColumnInfo integerColWithDefaultVal = 
            getIntegerColumn("noDefaultIntgerCol", 
                             true, 
                             "0", 
                             "An integer column with a default value");
        
        if (dialect.supportsAlterColumnDefault()) {
            String defaultValSQL = 
                dialect.getColumnDefaultAlterSQL(varcharColWithDefaultValue);
            runSQL(session, defaultValSQL);
            
            defaultValSQL = 
                dialect.getColumnDefaultAlterSQL(integerColWithDefaultVal);
            runSQL(session, defaultValSQL);
        } else {
            try {
                dialect.getColumnDefaultAlterSQL(noDefaultValueVarcharCol);
                throw new IllegalStateException(
                        "Expected dialect to fail to provide SQL for column default alter");
            } catch (UnsupportedOperationException e) {
                // This is what we expect.
                System.err.println(e.getMessage());
            }
        }        
    }
    
    private void testAlterNull(ISession session) throws Exception {
        HibernateDialect dialect = 
            DialectFactory.getDialect(session, DialectFactory.DEST_TYPE);  
        TableColumnInfo notNullThirdCol = 
            getVarcharColumn("nullvc", false, "defVal", "A varchar comment");        
        if (dialect.supportsAlterColumnNull()) {
            String notNullSQL = 
                dialect.getColumnNullableAlterSQL(notNullThirdCol);
            runSQL(session, notNullSQL);
        } else {
            try {
                dialect.getColumnNullableAlterSQL(notNullThirdCol);     
                throw new IllegalStateException(
                        "Expected dialect to fail to provide SQL for column nullable alter");
            } catch (UnsupportedOperationException e) {
                // this is expected
                System.err.println(e.getMessage());
            }
        }
    }
    
    private void createTestTable(ISession session) throws Exception {
        HibernateDialect dialect = 
            DialectFactory.getDialect(session, DialectFactory.DEST_TYPE);
        try {
            runSQL(session, dialect.getTableDropSQL("test", true));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (DialectFactory.isIngresSession(session)) {
            // alterations fail for some reason unless you do this...
            runSQL(session, "create table test ( mychar char(10)) with page_size=4096");
        } else {
            runSQL(session, "create table test ( mychar char(10))");
        }
    }
    
    private void testAddColumn(ISession session) 
        throws Exception 
    {
        addColumn(session, firstCol);
        addColumn(session, secondCol);
        addColumn(session, thirdCol);
        addColumn(session, fourthCol);
        addColumn(session, dropCol);      
        addColumn(session, noDefaultValueVarcharCol);
        addColumn(session, noDefaultValueIntegerCol);
        addColumn(session, renameCol);
        addColumn(session, pkCol);
    }
    
    private void addColumn(ISession session,    
                           TableColumnInfo info) 
        throws Exception 
    {
        HibernateDialect dialect = 
            DialectFactory.getDialect(session, DialectFactory.DEST_TYPE);
       
        String[] sqls = dialect.getColumnAddSQL(info);
        for (int i = 0; i < sqls.length; i++) {
            String sql = sqls[i];
            runSQL(session, sql);
        }
        
    }

    private void testColumnComment(ISession session) throws Exception {
        HibernateDialect dialect = getDialect(session);
        if (dialect.supportsColumnComment()) {
            alterColumnComment(session, firstCol);
            alterColumnComment(session, secondCol);
            alterColumnComment(session, thirdCol);
            alterColumnComment(session, fourthCol);
        } else {
            try {
                alterColumnComment(session, firstCol);    
            } catch (UnsupportedOperationException e) {
                // This is expected
                System.err.println(e.getMessage());
            }
        }
    }
    
    private void alterColumnComment(ISession session,    
                                    TableColumnInfo info) 
        throws Exception    
    {
        HibernateDialect dialect = 
            DialectFactory.getDialect(session, DialectFactory.DEST_TYPE);
        String commentSQL = dialect.getColumnCommentAlterSQL(info);
        if (commentSQL != null && !commentSQL.equals("")) {
            runSQL(session, commentSQL);
        }
    }
        
    private void testAddPrimaryKey(ISession session,
                               TableColumnInfo[] colInfos) 
        throws Exception 
    {
        HibernateDialect dialect = 
            DialectFactory.getDialect(session, DialectFactory.DEST_TYPE);

        String tableName = colInfos[0].getTableName();
        
        String[] pkSQLs = 
            dialect.getAddPrimaryKeySQL(tableName.toUpperCase()+"_PK", colInfos);
        
        for (int i = 0; i < pkSQLs.length; i++) {
            String pkSQL = pkSQLs[i];
            runSQL(session, pkSQL);
        }
    }
    
    private void dropColumn(ISession session,    
                            TableColumnInfo info) 
    throws Exception 
    {
        HibernateDialect dialect = 
            DialectFactory.getDialect(session, DialectFactory.DEST_TYPE);

        String sql = dialect.getColumnDropSQL("test", info.getColumnName());
        runSQL(session, sql);
    }
    
    private HibernateDialect getDialect(ISession session) throws Exception  {
        return DialectFactory.getDialect(session, DialectFactory.DEST_TYPE);
    }
    
    private void runSQL(ISession session, String sql) throws Exception {
        HibernateDialect dialect = getDialect(session);        
        Connection con = session.getSQLConnection().getConnection();
        Statement stmt = con.createStatement();
        System.out.println("Running SQL ("+dialect.getDisplayName()+"): "+sql);
        stmt.execute(sql);
    }
    
    private TableColumnInfo getIntegerColumn(String name,
                                             boolean nullable, 
                                             String defaultVal,
                                             String comment) 
    {
        return getColumn(java.sql.Types.INTEGER, 
                         "INTEGER", 
                         name, 
                         nullable, 
                         defaultVal, 
                         comment, 
                         10, 
                         0);
    }

    private TableColumnInfo getVarcharColumn(String name,
                                             boolean nullable, 
                                             String defaultVal,
                                             String comment,
                                             int size)
    {
        return getColumn(java.sql.Types.VARCHAR,
                "VARCHAR",
                name,
                nullable, 
                defaultVal, 
                comment, 
                size, 
                0);
    }
    
    
    private TableColumnInfo getVarcharColumn(String name,
                                             boolean nullable, 
                                             String defaultVal,
                                             String comment)
    {
        return getColumn(java.sql.Types.VARCHAR,
                         "VARCHAR",
                         name,
                         nullable, 
                         defaultVal, 
                         comment, 
                         100, 
                         0);
    }
    
    private TableColumnInfo getColumn(int dataType,
                                      String dataTypeName,
                                      String name,
                                      boolean nullable, 
                                      String defaultVal,
                                      String comment,
                                      int columnSize,
                                      int scale) 
    {
        String isNullable = "YES";
        int isNullAllowed = DatabaseMetaData.columnNullable;
        if (!nullable) {
            isNullable = "NO";
            isNullAllowed = DatabaseMetaData.columnNoNulls;
        }        
        TableColumnInfo result = 
            new TableColumnInfo("testCatalog",          // catalog 
                                "testSchema",           // schema
                                "test",                 // tableName
                                name,                   // columnName
                                dataType,               // dataType
                                dataTypeName,           // typeName 
                                columnSize,             // columnSize
                                scale,                  // decimalDigits
                                10,                     // radix
                                isNullAllowed,          // isNullAllowed
                                comment,                // remarks
                                defaultVal,             // defaultValue
                                0,                      // octet length
                                0,                      // ordinal position
                                isNullable);            // isNullable 
        return result;
    }
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        DialectLiveTestRunner runner = new DialectLiveTestRunner();
        runner.runTests();
    }
}
