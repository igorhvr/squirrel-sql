package net.sourceforge.squirrel_sql.fw.sql;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import junit.framework.TestCase;
import net.sourceforge.squirrel_sql.client.ApplicationManager;
import net.sourceforge.squirrel_sql.plugins.oracle.gui.DummyPlugin;
import net.sourceforge.squirrel_sql.plugins.oracle.prefs.OraclePreferenceBean;
import net.sourceforge.squirrel_sql.plugins.oracle.prefs.PreferencesManager;
import net.sourceforge.squirrel_sql.plugins.oracle.tokenizer.OracleQueryTokenizer;

public class OracleQueryTokenizerTest extends TestCase
                                      implements OracleSQL {

    static String nullSQL = null;       
    static String tmpFilename = null;
    static boolean removeMultilineComment = true;
    static {
        ApplicationManager.initApplication();        
    }
    
    QueryTokenizer qt = null;
    static int sqlFileStmtCount = 0;
    
    static OraclePreferenceBean _prefs;
    
    public void setUp() throws Exception {
        createSQLFile();
        DummyPlugin plugin = new DummyPlugin();
        PreferencesManager.initialize(plugin);
        _prefs = PreferencesManager.getPreferences();         
    }
    
    public void tearDown() {
        
    }
    
    public void testHasQuery() {
        qt = new OracleQueryTokenizer(_prefs);
        qt.setScriptToTokenize(SELECT_DUAL);
        SQLUtil.checkQueryTokenizer(qt, 1);
        
        qt = new OracleQueryTokenizer(_prefs);
        qt.setScriptToTokenize(SELECT_DUAL_2);
        SQLUtil.checkQueryTokenizer(qt, 1);        
    }

    public void testGenericSQL() {
        String script = SQLUtil.getGenericSQLScript();
        qt = new OracleQueryTokenizer(_prefs);
        qt.setScriptToTokenize(script);
        SQLUtil.checkQueryTokenizer(qt, SQLUtil.getGenericSQLCount());
    }
    
    public void testCreateStoredProcedure() {
        qt = new OracleQueryTokenizer(_prefs);
        qt.setScriptToTokenize(CREATE_STORED_PROC);
        SQLUtil.checkQueryTokenizer(qt, 1);
    }

    public void testCreateOrReplaceStoredProcedure() {
        qt = new OracleQueryTokenizer(_prefs);
        qt.setScriptToTokenize(CREATE_OR_REPLACE_STORED_PROC);
        SQLUtil.checkQueryTokenizer(qt, 1);
    }
    
    public void testHasQueryFromFile() {
        String fileSQL = "@" + tmpFilename + ";\n";
        qt = new OracleQueryTokenizer(_prefs);
        qt.setScriptToTokenize(fileSQL);
        SQLUtil.checkQueryTokenizer(qt, 6);
    }

    public void testExecAnonProcedure() {
        qt = new OracleQueryTokenizer(_prefs);
        qt.setScriptToTokenize(ANON_PROC_EXEC);
        SQLUtil.checkQueryTokenizer(qt, 1);
    }    
    
    private static void createSQLFile() throws IOException {
        if (tmpFilename != null) {
            return;
        }
        File f = File.createTempFile("test", ".sql");
        //f.deleteOnExit();
        PrintWriter out = new PrintWriter(new FileWriter(f));
        out.println(SELECT_DUAL);
        out.println();
        out.print(UPDATE_TEST);
        out.println();
        out.println(CREATE_STORED_PROC);
        out.println();
        out.println(CREATE_OR_REPLACE_STORED_PROC);
        out.println();
        out.println(ANON_PROC_EXEC);
        out.println();
        out.println(SELECT_DUAL);
        out.println();
        out.println(STUDENTS_NOT_TAKING_CS112);
        out.println();
        out.close();
        tmpFilename = f.getAbsolutePath();
        System.out.println("tmpFilename="+tmpFilename);
        
        // important to set this to the number of statements in the file 
        // above.
        sqlFileStmtCount = 7;
    }
    
}
