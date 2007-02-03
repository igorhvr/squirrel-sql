package net.sourceforge.squirrel_sql.fw.sql;


/**
 * This should be implemented to provide script tokenizing behavior that is 
 * specific to a database.
 * 
 * @author rmmannin
 */
public interface IQueryTokenizer {
    
    /**
     * Returns a boolean value indicating whether or not there are more 
     * statements to be sent to the server.
     * 
     * @return true if nextQuery can be called to get the next statement; 
     *         false otherwise.
     */
    boolean hasQuery();
    
    /**
     * Returns the next statement, or null if there are no more statements to 
     * get.
     * 
     * @return the next statement or null if there is no next statement.
     */
    String nextQuery();
    
    /**
     * Sets the script to be tokenized into one or more queries that should be 
     * sent to the database.  Query here means statement, so this includes more
     * than just select statements.
     * 
     * @param script a string representing one or more SQL statements.
     */
    void setScriptToTokenize(String script);
    
}
