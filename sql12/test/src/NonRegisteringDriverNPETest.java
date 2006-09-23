import java.sql.Driver;
import java.util.Properties;


public class NonRegisteringDriverNPETest {

    public static void main(String[] args) throws Exception {
        String className1 = "com.mysql.jdbc.Driver";
        String className2 = "org.gjt.mm.mysql.Driver";
        
        // Testing class com.mysql.jdbc.Driver
        System.out.println("Testing class "+className1);
        Driver driverInst = 
            (Driver) Class.forName(className1).newInstance();
        Properties info = new Properties();
        // This works
        System.out.println("Getting driver properties with correct URL");
        driverInst.getPropertyInfo("jdbc:mysql://localhost:3306/test", info);

        try {
            System.out.println("Getting driver properties with malformed URL");
            // This throws NPE
            driverInst.getPropertyInfo("jdbc:mysql://localhost:3306", info);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Testing clas org.gjt.mm.mysql.Driver
        System.out.println("Testing class "+className2);
        driverInst = (Driver) Class.forName(className2).newInstance();
        
        // This works
        System.out.println("Getting driver properties with correct URL");
        driverInst.getPropertyInfo("jdbc:mysql://localhost:3306/test", info);
        
        System.out.println("Getting driver properties with malformed URL");
        // This throws NPE
        try {
            driverInst.getPropertyInfo("jdbc:mysql://localhost:3306", info);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

}
