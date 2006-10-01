/*
 * Copyright (C) CollabraSpace Inc. All rights reserved.
 */
package adhoc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLDecimalTest {

    String jdbcUrl = "jdbc:hsqldb:file:/tools/hsqldb-1_8_0_2/data/dbcopydest";
    
    String user = "sa";
    
    String pass = "";
    
    Connection con = null;
    
    String testSQL = 
        "select nr from test";

    
    public MySQLDecimalTest() throws Exception {
        init();
    }
    
    public void init() throws Exception {
        Class.forName("org.hsqldb.jdbcDriver");
        con = DriverManager.getConnection(jdbcUrl,user, pass);
    }
    

    /**
     * This fails with a Connection failure - java.io.EOFException
     * @throws SQLException
     */
    public void doTest() throws SQLException {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(testSQL);
        if (rs.next()) {
            BigDecimal d = rs.getBigDecimal(1);
            System.out.println("d="+d);
        }
        stmt.close();
    }

    
    public void shutdown() throws SQLException {
        con.close();
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        // TODO Auto-generated method stub
        PointbaseBLOBTest test = new PointbaseBLOBTest();
        
        test.doTest();
        
        test.shutdown();
    }

}
