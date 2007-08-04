package adhoc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;

import net.sourceforge.squirrel_sql.fw.sql.SQLUtilities;

public class InformixTimeTypeTest {

    String jdbcUrl = "jdbc:informix-sqli:192.168.1.100:9088/dbcopydest:INFORMIXSERVER=sockets_srvr";
    
    String user = "informix";
    
    String pass = "password";
    
    Connection con = null;
    
    String dropSQL = 
        "drop table time_table ";
    
    String createTableSQL = 
        "CREATE TABLE time_table ( time_column datetime hour to second) ";

    String insertSQL = 
        "insert into time_table (time_column) " +
        "values (?) ";    
    
    String selectSQL = 
        "select time_column from time_table ";
    
    public InformixTimeTypeTest() throws Exception {
        init();
    }
    
    public void init() throws Exception {
        Class.forName("com.informix.jdbc.IfxDriver");
        con = DriverManager.getConnection(jdbcUrl,user, pass);
    }
    

    /**
     * This fails with a Connection failure - java.io.EOFException
     * @throws SQLException
     */
    public void doTest() throws SQLException {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(dropSQL);
        } catch (SQLException e) { 
            e.printStackTrace(); 
        } finally {
            SQLUtilities.closeStatement(stmt);
        }
        try {
            stmt = con.createStatement();
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            e.printStackTrace(); 
        } finally {
            SQLUtilities.closeStatement(stmt);
        }
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(insertSQL);
            Time time = new Time(System.currentTimeMillis());
            pstmt.setTime(1, time);
            pstmt.executeUpdate();
    
            rs = stmt.executeQuery(selectSQL);
            if (rs.next()) {
                java.sql.Timestamp ts = rs.getTimestamp(1);
                System.out.println("ts="+ts);
            }
        }  catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtilities.closeResultSet(rs);
            SQLUtilities.closeStatement(pstmt);
        }
    }

    
    public void shutdown() throws SQLException {
        con.close();
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        InformixTimeTypeTest test = new InformixTimeTypeTest();
        test.doTest();
        test.shutdown();
    }

}
