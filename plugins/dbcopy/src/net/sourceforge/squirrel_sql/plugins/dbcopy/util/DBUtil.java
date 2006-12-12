/*
 * Copyright (C) 2005 Rob Manning
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

package net.sourceforge.squirrel_sql.plugins.dbcopy.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import net.sourceforge.squirrel_sql.client.db.dialects.DialectFactory;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect;
import net.sourceforge.squirrel_sql.fw.dialects.UserCancelledOperationException;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.fw.sql.JDBCTypeMapper;
import net.sourceforge.squirrel_sql.fw.sql.SQLConnection;
import net.sourceforge.squirrel_sql.fw.sql.SQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.sql.TableColumnInfo;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.plugins.dbcopy.ColTypeMapper;
import net.sourceforge.squirrel_sql.plugins.dbcopy.I18NBaseObject;
import net.sourceforge.squirrel_sql.plugins.dbcopy.SessionInfoProvider;
import net.sourceforge.squirrel_sql.plugins.dbcopy.prefs.DBCopyPreferenceBean;
import net.sourceforge.squirrel_sql.plugins.dbcopy.prefs.PreferencesManager;
import net.sourceforge.squirrel_sql.plugins.dbcopy.sqlscript.IndexColInfo;
import net.sourceforge.squirrel_sql.plugins.dbcopy.sqlscript.IndexInfo;

import org.hibernate.MappingException;


/**
 * A utility class for interacting with the database.
 */
public class DBUtil extends I18NBaseObject {

    /** Logger for this class. */
    private final static ILogger log = 
        LoggerController.createLogger(DBUtil.class);    
    
    /** Plugin settings. The configuration panel uses this */
    private static DBCopyPreferenceBean _prefs = 
        PreferencesManager.getPreferences();
    
    /** Internationalized strings for this class */
    private static final StringManager s_stringMgr =
        StringManagerFactory.getStringManager(DBUtil.class);    
    
    
    /**
     * Returns a string that looks like:
     * 
     * (PK_COL1, PK_COL2, PK_COL3, ...)
     *  
     * or null if there is no primary key for the specified table.
     * 
     * @param sourceConn
     * @param ti
     * @return
     * @throws SQLException
     */
    public static String getPKColumnString(SQLConnection sourceConn,
                                           ITableInfo ti) 
        throws SQLException 
    {
        List pkColumns = getPKColumnList(sourceConn, ti);
        if (pkColumns == null || pkColumns.size() == 0) {
            return null;
        }
        StringBuffer sb = new StringBuffer("(");
        Iterator i = pkColumns.iterator();
        while (i.hasNext()) {
            String columnName = (String)i.next();
            sb.append(columnName);
            if (i.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
    
    /**
     * Returns a list of primary keys or null if there are no primary keys for
     * the specified table.
     * 
     * @param sourceConn
     * @param ti
     * @return
     * @throws SQLException
     */
    private static List getPKColumnList(SQLConnection sourceConn,
                                        ITableInfo ti) 
        throws SQLException 
    {
        ArrayList pkColumns = new ArrayList();
        DatabaseMetaData md = sourceConn.getConnection().getMetaData();
        ResultSet rs = null;
        if (md.supportsCatalogsInTableDefinitions()) {
            rs = md.getPrimaryKeys(ti.getCatalogName(), null, ti.getSimpleName());
        } else if (md.supportsSchemasInTableDefinitions()) {
            rs = md.getPrimaryKeys(null, ti.getSchemaName(), ti.getSimpleName());
        } else {
            rs = md.getPrimaryKeys(null, null, ti.getSimpleName());
        }
        while (rs.next()) {
            String keyColumn = rs.getString(4);
            if (keyColumn != null) {
                pkColumns.add(keyColumn);
            }
        }
        if (pkColumns.size() == 0) {
            return null;
        }        
        return pkColumns;
    }
        
    
    /**
     * Returns a List of SQL statements that add foreign key(s) to the table 
     * described in the specified ITableInfo.
     * 
     * @param sourceConn
     * @param ti
     * @return Set a set of SQL statements that can be used to create foreign
     *             key constraints.
     * @throws SQLException
     */
    public static Set getForeignKeySQL(SessionInfoProvider prov,  
                                        ITableInfo ti,
                                        ArrayList selectedTableInfos) 
        throws SQLException , UserCancelledOperationException
    {
        SQLConnection sourceConn = prov.getCopySourceSession().getSQLConnection();
        DatabaseMetaData md = sourceConn.getConnection().getMetaData();
        ResultSet rs = null;
        if (md.supportsCatalogsInTableDefinitions()) {
            rs = md.getImportedKeys(ti.getCatalogName(), null, ti.getSimpleName());
        } else if (md.supportsSchemasInTableDefinitions()) {
            rs = md.getImportedKeys(null, ti.getSchemaName(), ti.getSimpleName());
        } else {
            rs = md.getImportedKeys(null, null, ti.getSimpleName());
        }
        
        HashSet result = new HashSet();
        while (rs.next()) {
            StringBuffer sb = new StringBuffer();
            String pkTableName = rs.getString(3);
            String pkTableCol = rs.getString(4);
            String fkTableName = rs.getString(7);
            String fkTableCol = rs.getString(8);
            String fkName = rs.getString(12);
            //alter table ti.getSimpleName() 
            //add foreign key (fkTableCol) 
            //references pkTableName(pkTableCol);
            if (!containsTable(selectedTableInfos, pkTableName)) {
                // TODO: Maybe someday we could inform the user that the imported
                // key can't be created because the list of tables they've 
                // selected, doesn't include the table that this foreign key
                // depends upon.  For now, just log a warning and skip it.
                if (log.isDebugEnabled()) {
                    //i18n[DBUtil.error.missingtable=getForeignKeySQL: table 
                    //'{0}' has a column '{1}' that references table '{2}' 
                    //column '{3}'. However, that table is not being copied. 
                    //Skipping this foreign key.]
                    String msg = 
                        s_stringMgr.getString("DBUtil.error.missingtable",
                                              new String[] { fkTableName,
                                                             fkTableCol,
                                                             pkTableName,
                                                             pkTableCol });
                                           
                    log.debug(msg);
                }                    
                continue;
            }
            sb.append("ALTER TABLE ");
            ISession destSession = prov.getCopyDestSession();
            String destSchema = prov.getDestSelectedDatabaseObject().getSimpleName();
            String destCatalog = prov.getDestSelectedDatabaseObject().getCatalogName();
            String fkTable = getQualifiedObjectName(destSession,  
                                                    destCatalog, 
                                                    destSchema, 
                                                    ti.getSimpleName(), 
                                                    DialectFactory.DEST_TYPE);
            String pkTable = getQualifiedObjectName(destSession, 
                                                    destCatalog, 
                                                    destSchema, 
                                                    pkTableName, 
                                                    DialectFactory.DEST_TYPE);  
            sb.append(fkTable);
            sb.append(" ADD FOREIGN KEY (");
            sb.append(fkTableCol);
            sb.append(") REFERENCES ");
            sb.append(pkTable);
            sb.append("(");
            sb.append(pkTableCol);
            sb.append(")");
            result.add(sb.toString());
        }
        return result;
    }
    
    private static boolean containsTable(ArrayList tableInfos, String table) {
        boolean result = false;
        Iterator i = tableInfos.iterator();
        while (i.hasNext()) {
            ITableInfo ti = (ITableInfo)i.next();
            if (table.equalsIgnoreCase(ti.getSimpleName())) {
                result = true;
                break;
            }
        }
        return result;
    }
    
    /**
     * Executes the given SQL using the specified SQLConnection.
     * 
     * @param con the SQLConnection to execute the update on.
     * @param SQL the statement to execute.
     * @return either the row count for INSERT, UPDATE  or DELETE statements, 
     *         or 0 for SQL statements that return nothing
     * @throws SQLException if a database access error occurs or the given SQL 
     *                       statement produces a ResultSet object
     */
    public static int executeUpdate(SQLConnection con, 
                                    String SQL,
                                    boolean writeSQL) throws SQLException {
        Statement stmt = null;
        int result = 0;
        try {
            stmt = con.createStatement();
            if (writeSQL) {
                ScriptWriter.write(SQL);
            }
            if (log.isDebugEnabled()) {
                // i18n[DBUtil.info.executeupdate=executeupdate: Running SQL:\n '{0}']
                String msg = 
                    s_stringMgr.getString("DBUtil.info.executeupdate", SQL);
                log.debug(msg);
            }
            result = stmt.executeUpdate(SQL);
        } finally {
            closeStatement(stmt);
        }
        return result;
    }

    /**
     * Executes the specified sql statement on the specified connection and 
     * returns the ResultSet.
     * 
     * @param con
     * @param sql
     * @param mysqlBigResultFix if true, provides a work-around which is useful
     *        in the case that the connection is to a MySQL database.  If the 
     *        number of rows is large this will prevent the driver from reading
     *        them all into client memory.  MySQL's normal practice is to do 
     *        such a thing for performance reasons.
     * @return
     * @throws Exception
     */
    public static ResultSet executeQuery(ISession session, 
                                         String sql) 
        throws SQLException 
    {
    	SQLConnection sqlcon = session.getSQLConnection(); 
        if (sqlcon == null || sql == null) {
            return null;
        }
        Statement stmt = null;
        ResultSet rs = null;

        Connection con = sqlcon.getConnection();
        try {
            if (DialectFactory.isMySQLSession(session)) {
                stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                                           ResultSet.CONCUR_READ_ONLY);
            
                stmt.setFetchSize(Integer.MIN_VALUE);
            } else if (DialectFactory.isTimesTen(session)) {
            	stmt = con.createStatement();
            	int fetchSize = _prefs.getSelectFetchSize();
            	// TimesTen allows a maximum fetch size of 128. 
            	if (fetchSize > 128) {
            		log.info(
            			"executeQuery: TimesTen allows a maximum fetch size of " +
            			"128.  Altering preferred fetch size from "+fetchSize+
            			" to 128.");
            		fetchSize = 128;
            	}
            	stmt.setFetchSize(fetchSize);
            } else { 
                stmt = con.createStatement();
                stmt.setFetchSize(_prefs.getSelectFetchSize());
            }
        } catch(SQLException e) {
            // Only close the statement if SQLException - otherwise it has to 
            // remain open until the ResultSet is read through by the caller.
            if (stmt != null) { 
                try {stmt.close();} catch (SQLException ex) { /* Do Nothing */}
            }
            throw e;
        }
        if (log.isDebugEnabled()) {
            //i18n[DBUtil.info.executequery=executeQuery: Running SQL:\n '{0}']
            String msg = 
                s_stringMgr.getString("DBUtil.info.executequery", sql);
            log.debug(msg);
        }
        try {
            rs = stmt.executeQuery(sql);
        } catch (SQLException e) {
            // Only close the statement if SQLException - otherwise it has to 
            // remain open until the ResultSet is read through by the caller.
            if (stmt != null) { 
                try {stmt.close();} catch (SQLException ex) { /* Do Nothing */}
            }
            throw e;            
        }
 
        return rs;
    }    
    
    /**
     * Closes the specified ResultSet.
     * 
     * @param rs the ResultSet to close.
     */
    public static void closeResultSet(ResultSet rs) {
        if (rs == null) {
            return;
        }
        try {
            Statement stmt = rs.getStatement();
            closeStatement(stmt);
        } catch (Exception e) { /* Do Nothing */ }
    }

    
    /**
     * Closes the specified Statement.
     * 
     * @param stmt the Statement to close.
     */
    public static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                ResultSet rs = stmt.getResultSet();
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {}
            try { stmt.close(); } catch (SQLException e) {}
        }
    }
    
    /**
     * Returns a count of the records in the specified table.
     * 
     * @param con the SQLConnection to use to execute the count query.
     * @param tableName the name of the table.  This name should already be 
     *                  qualified by the schema.
     * 
     * @return -1 if the table does not exist, otherwise the record count is
     *         returned.
     */    
    private static int getTableCount(ISession session, String tableName) {
        int result = -1;
        ResultSet rs = null;
        try {
            String sql = "select count(*) from "+tableName;            
            rs = executeQuery(session, sql);
            if (rs.next()) {
                result = rs.getInt(1);
            }
        } catch (Exception e) {
            /* Do Nothing - this can happen when the table doesn't exist */
        } finally {
            closeResultSet(rs);
        }
        return result;        
    }
    
    /**
     * Returns a count of the records in the specified table.
     * 
     * @param con the SQLConnection to use to execute the count query.
     * @param tableName the name of the table
     * 
     * @return -1 if the table does not exist, otherwise the record count is
     *         returned.
     */
    public static int getTableCount(ISession session, 
                                    String catalog,
                                    String schema, 
                                    String tableName,
                                    int sessionType) 
        throws UserCancelledOperationException
    {
        String table = getQualifiedObjectName(session, 
                                              catalog, 
                                              schema,
                                              tableName, 
                                              sessionType);
        return getTableCount(session, table);
    }
    
    public static ITableInfo getTableInfo(ISession session,
                                          String schema,
                                          String tableName) 
        throws SQLException, MappingException, UserCancelledOperationException 
    {
        SQLConnection con = session.getSQLConnection();
        // Currently, as of milestone 3, Axion doesn't support "schemas" like 
        // other databases.  So, set the schema to emtpy string if we detect 
        // an Axion session.
        if (con.getSQLMetaData().getDriverName().toLowerCase().startsWith("axion")) {
            schema = "";
        }
        String catalog = null;
        // MySQL uses catalogs and not schemas
        if (DialectFactory.isMySQLSession(session)) {
            catalog = schema;
            schema = null;
        }
        // trim the table name in case of HADB
        tableName = tableName.trim();
        ITableInfo[] tis = Compat.getTables(session, catalog, schema, tableName);
        if (tis == null || tis.length == 0) {
            if (Character.isUpperCase(tableName.charAt(0))) {
                tableName = tableName.toLowerCase();
            } else {
                tableName = tableName.toUpperCase();
            }
            tis = Compat.getTables(session, null, schema, tableName);
            if (tis.length == 0) {
                if (Character.isUpperCase(tableName.charAt(0))) {
                    tableName = tableName.toLowerCase();
                } else {
                    tableName = tableName.toUpperCase();
                }
                tis = Compat.getTables(session, null, schema, tableName);
            }
        }
        if (tis.length == 0) {
            //i18n[DBUtil.error.tablenotfound=Couldn't locate table '{0}' in 
            //schema '(1)']
            String msg = 
                s_stringMgr.getString("DBUtil.error.tablenotfound",
                                      new String[] { tableName,
                                                     schema });
            throw new MappingException(msg);
        }
        if (tis.length > 1) {
        	if (log.isDebugEnabled()) {
        		log.debug(
        			"DBUtil.getTableInfo: found "+tis.length+" that matched "+
        			"catalog="+catalog+" schema="+schema+" tableName="+
        			tableName);
        	}
        }
        return tis[0];
    }
    
    /**
     * Decides whether or not the specified column types 
     * (java.sql.Type constants) use the same java type to read from the source
     * database as the one used to write to the destination database.  For 
     * example, Types.DECIMAL and Types.NUMERIC both use BigDecimal java type
     * to store the value in between reading and writing it.  Therefore, even
     * though these types are not equal, they are equivalent. This method has
     * not yet been fully implemented with equivalences from the bindVariable 
     * method.
     *  
     * @param sourceType the column type as identified by the source database
     *                   jdbc driver.
     * @param destType the column type as identified by the destination database
     *                 jdbc driver.
     * @return true if equivalent, false if not.
     */
    public static boolean typesAreEquivalent(int sourceType, int destType) {
        boolean result = false;
        if (sourceType == destType) {
            result = true;
        }
        if (sourceType == Types.DECIMAL && destType == Types.NUMERIC) {
            result = true;
        }
        if (sourceType == Types.NUMERIC && destType == Types.DECIMAL) {
            result = true;
        }
        if (sourceType == Types.BOOLEAN && destType == Types.BIT) {
            result = true;
        }
        if (sourceType == Types.BIT && destType == Types.BOOLEAN) {
            result = true;
        }
        return result;
    }
    
    /**
     * Check to see if the last column retrieved at the specified index was null.
     * If so, bind the specified PreparedStatement column at the specified index
     * to null and return true. 
     *  
     * @param rs the ResultSet that was used to read the last row.
     * @param ps the PreparedStatement that will be used to insetrt a row into 
     *           the destination database.
     * @param index the column in the row that was last read, whose value we mean
     *              to inspect.
     * @param type the type of the column.
     * @return true if last column was null; false otherwise.
     * @throws SQLException
     */
    private static boolean handleNull(ResultSet rs, 
                                      PreparedStatement ps, 
                                      int index, 
                                      int type)
        throws SQLException 
    {
        boolean result = false;
        if (rs.wasNull()) {
            ps.setNull(index, type);
            result = true;
        }
        return result;
    }
    
    /**
     * Takes the specified colInfo, gets the data type to see if it is 
     * 1111(OTHER).  If so then get the type name and try to match a jdbc type
     * with the same name to get it's type code.
     * 
     * @param colInfo
     * @return
     * @throws MappingException
     */
    public static int replaceOtherDataType(TableColumnInfo colInfo) 
    	throws MappingException 
    {
    	int colJdbcType = colInfo.getDataType();
        if (colJdbcType == java.sql.Types.OTHER) {
            String typeName = colInfo.getTypeName().toUpperCase();
            int parenIndex = typeName.indexOf("(");
            if (parenIndex != -1) {
                typeName = typeName.substring(0,parenIndex);
            }
            colJdbcType = JDBCTypeMapper.getJdbcType(typeName);
            if (colJdbcType == Types.NULL) {
                throw new MappingException(
                        "Encoutered jdbc type OTHER (1111) and couldn't map "+
                        "the database-specific type name ("+typeName+
                        ") to a jdbc type");
            }
        }
        return colJdbcType;
    }
    
    /**
     * Reads the value from the specified ResultSet at column index index, and 
     * based on the type, calls the appropriate setXXX method on ps with the 
     * value obtained.
     *  
     * @param ps
     * @param sourceColType 
     * @param destColType
     * @param index
     * @param rs
     * @return a string representation of the value that was bound.
     * @throws SQLException
     */
    public static String bindVariable(PreparedStatement ps, 
                                      int sourceColType,
                                      int destColType,
                                      int index, 
                                      ResultSet rs) throws SQLException {
        String result = "null";
        switch (sourceColType) {
            case Types.ARRAY:
                Array arrayVal = rs.getArray(index);
                result = getValue(arrayVal);
                ps.setArray(index, arrayVal);
                break;
            case Types.BIGINT:
                long bigintVal = rs.getLong(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = Long.toString(bigintVal);
                    ps.setLong(index, bigintVal);                    
                }
                break;
            case Types.BINARY:
                result = bindBlobVar(ps, index, rs, destColType);
                break;
            case Types.BIT:
                // JDBC spec says that BIT refers to a boolean column - i.e. a
                // single binary digit with value either "0" or "1". Also 
                // the same spec encourages use of getBoolean/setBoolean.
                // However, the SQL-92 standard clearly states that the BIT type
                // is a bit string with length >= 0.  So for SQL-92 compliant
                // databases (like PostgreSQL) the JDBC spec's support for BIT
                // is at best broken and unusable. Still, we do what the JDBC 
                // spec suggests as that is all that we can do.
                
                // TODO: just noticed that MySQL 5.0 supports a multi-bit BIT
                // column by using the getObject/setObject methods with a byte[].
                // So it would be valuable at some point to make this code a bit
                // more dbms-specific
                boolean bitValue = rs.getBoolean(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = Boolean.toString(bitValue);
                    ps.setBoolean(index, bitValue);
                }
                break;
            case Types.BLOB:
                result = bindBlobVar(ps, index, rs, destColType);
                break;
            case Types.BOOLEAN:
                boolean booleanValue = rs.getBoolean(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = Boolean.toString(booleanValue);
                    // HACK: some dbs (like Frontbase) don't support boolean
                    // types.  I've tried tinyint, bit and boolean as the column
                    // type, and setBoolean fails for all three.  It's a mystery
                    // at this point what column the getBoolean/setBoolean methods
                    // actually work on iin FrontBase.
                    switch (destColType) {
                        case Types.TINYINT:
                        case Types.SMALLINT:
                        case Types.BIGINT:
                        case Types.INTEGER:
                            ps.setInt(index, booleanValue? 1 : 0 );
                            break;
                        case Types.FLOAT:
                            ps.setFloat(index, booleanValue? 1 : 0 );
                            break;
                        case Types.DOUBLE:
                            ps.setDouble(index, booleanValue? 1 : 0 );
                            break;
                        case Types.VARCHAR:
                        case Types.CHAR:
                            ps.setString(index, booleanValue? "1" : "0" );
                            break;
                        default:
                            ps.setBoolean(index, booleanValue);
                            break;
                    }
                }
                break;
            case Types.CHAR:
                String charValue = rs.getString(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = charValue;
                    ps.setString(index, charValue);
                }
                break;
            case Types.CLOB:
                bindClobVar(ps, index, rs, destColType);
                break;
            case Types.DATALINK:
                // TODO: is this right???
                Object datalinkValue = rs.getObject(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = getValue(datalinkValue);
                    ps.setObject(index, datalinkValue);
                }
                break;
            case Types.DATE:
                Date dateValue = rs.getDate(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    // TODO: use the destination database type to derive a 
                    //       format that is acceptable.
                    result = getValue(dateValue);
                    ps.setDate(index, dateValue);
                }
                break;
            case Types.DECIMAL:
                BigDecimal decimalValue = rs.getBigDecimal(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = getValue(decimalValue);
                    ps.setBigDecimal(index, decimalValue);
                }
                break;
            case Types.DISTINCT:
                // TODO: is this right???
                Object distinctValue = rs.getObject(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = getValue(distinctValue);
                    ps.setObject(index, distinctValue);
                }
                break;
            case Types.DOUBLE:
                double doubleValue = rs.getDouble(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = Double.toString(doubleValue);
                    ps.setDouble(index, doubleValue);
                }
                break;
            case Types.FLOAT:
                // SQL FLOAT requires support for 15 digits of mantissa.
                double floatValue = rs.getDouble(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = Double.toString(floatValue);
                    ps.setDouble(index, floatValue);
                }
                break;
            case Types.INTEGER:
                int integerValue = rs.getInt(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = Integer.toString(integerValue);
                    ps.setInt(index, integerValue);
                }
                break;
            case Types.JAVA_OBJECT:
                Object objectValue = rs.getObject(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = getValue(objectValue);
                    ps.setObject(index, objectValue);
                }
                break;
            case Types.LONGVARBINARY:
                result = bindBlobVar(ps, index, rs, destColType);
                break;
            case Types.LONGVARCHAR:
                String longvarcharValue = rs.getString(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = longvarcharValue;
                    ps.setString(index, longvarcharValue);
                }
                break;
            case Types.NULL:
                // TODO: is this right??? 
                ps.setNull(index, Types.NULL);
                break;
            case Types.NUMERIC:
                BigDecimal numericValue = rs.getBigDecimal(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = getValue(numericValue);
                    ps.setBigDecimal(index, numericValue);
                }
                break;
            case Types.OTHER:
                // TODO: figure out a more reliable way to handle OTHER type 
                // which indicates a database-specific type.
                String testValue = rs.getString(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    try {
                        Double.parseDouble(testValue);
                        double numberValue = rs.getDouble(index);
                        ps.setDouble(index, numberValue);                    
                    } catch (SQLException e) {
                        byte[] otherValue = rs.getBytes(index);
                        result = getValue(otherValue);
                        ps.setBytes(index, otherValue);    
                    }
                }
                break;
            case Types.REAL:
                float realValue = rs.getFloat(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = Float.toString(realValue);
                    ps.setFloat(index, realValue);
                }
                break;
            case Types.REF:
                Ref refValue = rs.getRef(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = getValue(refValue);
                    ps.setRef(index, refValue);
                }
                break;
            case Types.SMALLINT:
                short smallintValue = rs.getShort(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = Short.toString(smallintValue);
                    ps.setShort(index, smallintValue);
                }
                break;
            case Types.STRUCT:
                Object structValue = rs.getObject(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = getValue(structValue);
                    ps.setObject(index, structValue);
                }
                break;
            case Types.TIME:
                Time timeValue = rs.getTime(index);
                // TODO: use the destination database type to derive a format 
                // that is acceptable.
                if (!handleNull(rs, ps, index, destColType)) {
                    result = getValue(timeValue);
                    ps.setTime(index, timeValue);
                }
                break;
            case Types.TIMESTAMP:
                Timestamp timestampValue = rs.getTimestamp(index);
                // TODO: use the destination database type to derive a format 
                // that is acceptable.
                if (!handleNull(rs, ps, index, destColType)) {
                    result = getValue(timestampValue);
                    ps.setTimestamp(index, timestampValue);
                }
                break;
            case Types.TINYINT:
                byte tinyintValue = rs.getByte(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = Byte.toString(tinyintValue);
                    ps.setByte(index, tinyintValue);
                }
                break;
            case Types.VARBINARY:
                result = bindBlobVar(ps, index, rs, destColType);
                break;
            case Types.VARCHAR:
                String varcharValue = rs.getString(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = varcharValue;
                    ps.setString(index, varcharValue);
                }
                break;
            default:
                //i18n[DBUtil.error.unknowntype=Unknown Java SQL column type: '{0}']
                String msg =
                    s_stringMgr.getString("DBUtil.error.unknowntype",
                                          new Integer(sourceColType));
                log.error(msg);
                // We still have to bind a value, or else the PS will throw
                // an exception.
                String value = rs.getString(index);
                if (!handleNull(rs, ps, index, destColType)) {
                    result = value;
                    ps.setString(index, value);
                }
                break;
        }
        return result;
    }

    private static String bindClobVar(PreparedStatement ps, 
                                      int index, 
                                      ResultSet rs,
                                      int type) throws SQLException 
    {
        String result = "null";
        if (_prefs.isUseFileCaching()) {
            try {            
                bindClobVarInFile(ps, index, rs, type);
            } catch (Exception e) {
                //i18n[DBUtil.error.bindclobfailure=bindBlobVar: failed to 
                //bind blob using filesystem - attempting to bind blob using 
                //memory]
                String msg = s_stringMgr.getString("DBUtil.error.bindclobfailure");
                log.error(msg, e);
                // if we failed to bind the blob in a file, try memory.
                result = bindClobVarInMemory(ps, index, rs, type);
            } 
        } else {
            result = bindClobVarInMemory(ps, index, rs, type);
        }
        return result;
    }
    
    private static String bindBlobVar(PreparedStatement ps, 
                                      int index, 
                                      ResultSet rs,
                                      int type) throws SQLException {
        String result = "null";
        if (_prefs.isUseFileCaching()) {
            try {            
                bindBlobVarInFile(ps, index, rs, type);
            } catch (Exception e) {
                //i18n[DBUtil.error.bindblobfailure=bindBlobVar: failed to 
                //bind blob using filesystem - attempting to bind blob using 
                //memory]
                String msg = 
                    s_stringMgr.getString("DBUtil.error.bindblobfailure");
                log.error(msg, e);
                // if we failed to bind the blob in a file, try memory.
                result = bindBlobVarInMemory(ps, index, rs, type);
            } 
        } else {
            result = bindBlobVarInMemory(ps, index, rs, type);
        }
        return result;
    }
    
    private static String bindClobVarInMemory(PreparedStatement ps, 
                                              int index, 
                                              ResultSet rs,
                                              int type) throws SQLException 
    {
        String clobValue = rs.getString(index);
        if (rs.wasNull()) {
            ps.setNull(index, type);
            return "null";
        }
        String result = getValue(clobValue);
        if (log.isDebugEnabled() && clobValue != null) {
            // i18n[DBUtil.info.bindclobmem=bindClobVarInMemory: binding '{0}' bytes]
            String msg = s_stringMgr.getString("DBUtil.info.bindclobmem",
                                               new Integer(clobValue.length()));
            log.debug(msg);
        }
        ps.setString(index, clobValue);
        return result;
    }

    
    private static String bindBlobVarInMemory(PreparedStatement ps, 
                                              int index, 
                                              ResultSet rs,
                                              int type) throws SQLException {
        byte[] blobValue = rs.getBytes(index);
        if (rs.wasNull()) {
            ps.setNull(index, type);
            return "null";
        }
        String result = getValue(blobValue);
        if (log.isDebugEnabled() && blobValue != null) {
            //i18n[DBUtil.info.bindblobmem=bindBlobVarInMemory: binding '{0}' bytes]
            String msg = 
                s_stringMgr.getString("DBUtil.info.bindblobmem",
                                      new Integer(blobValue.length));
            log.debug(msg);
        }
        ps.setBytes(index, blobValue);
        return result;
    }
    
    private static void bindClobVarInFile(PreparedStatement ps, 
                                          int index, 
                                          ResultSet rs,
                                          int type)
    throws IOException, SQLException 
    {
        // get ascii stream from rs
        InputStream is = rs.getAsciiStream(index);
        if (rs.wasNull()) {
            ps.setNull(index, type);
            return;
        }
        
        // Open file output stream
        long millis = System.currentTimeMillis();
        File f = File.createTempFile("clob", ""+millis);
        f.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(f);
        if (log.isDebugEnabled()) {
            //i18n[DBUtil.info.bindclobfile=bindClobVarInFile: Opening temp file '{0}']
            String msg = s_stringMgr.getString("DBUtil.info.bindclobfile",
                                               f.getAbsolutePath());
            log.debug(msg);
        }
        
        // read rs input stream write to file output stream
        byte[] buf = new byte[_prefs.getFileCacheBufferSize()];
        int length = 0;
        int total = 0;
        while ((length = is.read(buf)) >= 0) {
            if (log.isDebugEnabled()) {
                //i18n[DBUtil.info.bindcloblength=bindClobVarInFile: writing '{0}' bytes.]
                String msg =
                    s_stringMgr.getString("DBUtil.info.bindcloblength",
                                          new Integer(length));
                log.debug(msg);
            }
            fos.write(buf, 0, length);
            total += length;
        }
        fos.close();
        
        // set the ps to read from the file we just created.
        FileInputStream fis = new FileInputStream(f);
        BufferedInputStream bis = new BufferedInputStream(fis);
        ps.setAsciiStream(index, bis, total);
    }
    
    private static void bindBlobVarInFile(PreparedStatement ps, 
                                          int index, 
                                          ResultSet rs,
                                          int type) 
        throws IOException, SQLException 
    {
        // get binary stream from rs
        InputStream is = rs.getBinaryStream(index);
        if (rs.wasNull()) {
            ps.setNull(index, type);
            return;
        }
        // Open file output stream
        long millis = System.currentTimeMillis();
        File f = File.createTempFile("blob", ""+millis);
        f.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(f);
        if (log.isDebugEnabled()) {
            //i18n[DBUtil.info.bindblobfile=bindBlobVarInFile: Opening temp file '{0}']
            String msg = s_stringMgr.getString("DBUtil.info.bindblobfile",
                                               f.getAbsolutePath());
            log.debug(msg);
        }
        
        
        // read rs input stream write to file output stream
        byte[] buf = new byte[_prefs.getFileCacheBufferSize()];
        int length = 0;
        int total = 0;
        while ((length = is.read(buf)) >= 0) {
            if (log.isDebugEnabled()) {
                //i18n[DBUtil.info.bindbloblength=bindBlobVarInFile: writing '{0}' bytes.]
                String msg =
                    s_stringMgr.getString("DBUtil.info.bindbloblength",
                                          new Integer(length));
                log.debug(msg);
            }
            fos.write(buf, 0, length);
            total += length;
        }
        fos.close();
        
        // set the ps to read from the file we just created.
        FileInputStream fis = new FileInputStream(f);
        BufferedInputStream bis = new BufferedInputStream(fis);
        ps.setBinaryStream(index, bis, total);
    }
    
    /**
     * Returns the string representation of the specified object, or "null" if
     * the specified object is null.
     * 
     * @param o
     * @return
     */
    private static String getValue(Object o) {
        if (o != null) {
            return o.toString();
        }
        return "null";
    }
    
    /**
     * 
     * @param con
     * @param synonym
     * @param columnName
     * @return
     * @throws SQLException
     */
    public static int getColumnType(SQLConnection con, 
                                    ITableInfo ti, 
                                    String columnName) throws SQLException { 
        int result = -1;
        if (ti != null) {
            TableColumnInfo[] tciArr = con.getSQLMetaData().getColumnInfo(ti);
            for (int i=0; i < tciArr.length; i++) {
                if (tciArr[i].getColumnName().equalsIgnoreCase(columnName)) {
                    result = tciArr[i].getDataType();
                    break;
                }
            }
        }
        return result;
    }
        
    public static int[] getColumnTypes(SQLConnection con, 
                                       ITableInfo ti, 
                                       String[] colNames) 
        throws SQLException 
    {
        TableColumnInfo[] tciArr = con.getSQLMetaData().getColumnInfo(ti);
        int[] result = new int[tciArr.length];
        for (int i=0; i < tciArr.length; i++) {
            boolean found = false;
            for (int j=0; j < colNames.length && !found; j++) {
                String columnName = colNames[j];
                if (tciArr[i].getColumnName().equalsIgnoreCase(columnName)) {
                    result[i] = tciArr[i].getDataType();
                    found = true;
                }
            }
        }
        return result;
    }
    
    public static boolean tableHasPrimaryKey(SQLConnection con,
                                             ITableInfo ti) 
        throws SQLException
    {
        boolean result = false;
        ResultSet rs = null;
        try {
            DatabaseMetaData md = con.getConnection().getMetaData();
            String cat = ti.getCatalogName();
            String schema = ti.getSchemaName();
            String tableName = ti.getSimpleName();
            rs = md.getPrimaryKeys(cat, schema, tableName);
            if (rs.next()) {
                result = true;
            }
        } finally { 
            closeResultSet(rs);
        }
        return result;
    }

    /**
     * Check the specified session to determine if the specified data is a 
     * keyword.
     * 
     * @param session
     * @param data
     * @return
     */
    public static boolean isKeyword(ISession session, String data) {
        return Compat.isKeyword(session, data);
    }
    
    /**
     * Deletes existing data from the destination connection specified in the 
     * specified table. This will use preferences to determine if truncate
     * command is preferred.  If truncate is preferred and fails, then delete
     * will be attempted.
     * 
     * @param con
     * @param tablename
     * @throws SQLException
     */
    public static void deleteDataInExistingTable(ISession session,
                                                 String catalogName,
                                                 String schemaName, 
                                                 String tableName) 
        throws SQLException, UserCancelledOperationException
    {
        SQLConnection con = session.getSQLConnection();
        boolean useTrunc = PreferencesManager.getPreferences().isUseTruncate();
        String fullTableName = 
            getQualifiedObjectName(session, 
                                   catalogName, 
                                   schemaName, 
                                   tableName, 
                                   DialectFactory.DEST_TYPE);
        String truncSQL = "TRUNCATE TABLE "+fullTableName;
        String deleteSQL = "DELETE FROM "+fullTableName;
        try {
            if (useTrunc) {
                DBUtil.executeUpdate(con, truncSQL, true);
            } else {
                DBUtil.executeUpdate(con, deleteSQL, true);
            }
        } catch (SQLException e) {
            // If truncate was attempted and not supported, then try delete.  
            // If on the other hand delete was attempted, just throw the 
            // SQLException that resulted from the delete.
            if (useTrunc) {
                DBUtil.executeUpdate(con, deleteSQL, true);
            } else {
                throw e;
            }
        }
    }    
    
    /**
     * This will take into account any special needs that the destination 
     * session has with regard to user preferences, and throw a MappingException
     * if any user preference isn't valid for the specified destination session.
     * 
     * @param destSession
     */
    public static void sanityCheckPreferences(ISession destSession) 
        throws MappingException
    {
       
        if (DialectFactory.isFirebirdSession(destSession)) {
            if (!PreferencesManager.getPreferences().isCommitAfterTableDefs()) {
                // TODO: maybe instead of throwing an exception, we could ask 
                // the user if they would like us to adjust their preference for
                // them.
                
                //i18n[DBUtil.error.firebirdcommit=Firebird requires commit 
                //table create before inserting records. Please adjust your 
                //preferences.]
                String msg = 
                    s_stringMgr.getString("DBUtil.error.firebirdcommit");
                throw new MappingException(msg);
            }
        }
    }
    
    public static String getCreateTableSql(SessionInfoProvider prov, 
                                           ITableInfo ti) 
        throws SQLException, MappingException, UserCancelledOperationException
    {

        ISession sourceSession = prov.getCopySourceSession();
        String sourceSchema = 
            prov.getSourceSelectedDatabaseObjects()[0].getSchemaName();
        String sourceCatalog = 
            prov.getSourceSelectedDatabaseObjects()[0].getCatalogName();
        String sourceTableName = getQualifiedObjectName(sourceSession, 
                                                        sourceCatalog, 
                                                        sourceSchema,
                                                        ti.getSimpleName(), 
                                                        DialectFactory.SOURCE_TYPE);
        ISession destSession = prov.getCopyDestSession();
        String destSchema = prov.getDestSelectedDatabaseObject().getSimpleName();
        String destCatalog = prov.getDestSelectedDatabaseObject().getCatalogName();
        String destinationTableName = getQualifiedObjectName(destSession, 
                                                             destCatalog, 
                                                             destSchema,
                                                             ti.getSimpleName(), 
                                                             DialectFactory.DEST_TYPE); 
        StringBuffer result = new StringBuffer("CREATE TABLE ");
        result.append(destinationTableName);
        result.append(" ( ");
        result.append("\n");
        TableColumnInfo colInfo = null;
        try {
            SQLConnection sourceCon = prov.getCopySourceSession().getSQLConnection();
            TableColumnInfo[] colInfoArr = sourceCon.getSQLMetaData().getColumnInfo(ti);
            if (colInfoArr.length == 0) {
                //i18n[DBUtil.error.nocolumns=Table '{0}' in schema '{1}' has 
                //no columns to copy]
                String msg = 
                    s_stringMgr.getString("DBUtil.error.nocolumns",
                                          new String[] { ti.getSimpleName(),
                                                         ti.getSchemaName() });
                throw new MappingException(msg); 
            }
            for (int i = 0; i < colInfoArr.length; i++) {
                colInfo = colInfoArr[i];
                result.append("\t");
                String columnSql =
                    DBUtil.getColumnSql(prov, colInfo, 
                                        sourceTableName, destinationTableName);
                result.append(columnSql);
                if (i < colInfoArr.length-1) {
                    result.append(",\n");
                }
            }
            
            // If the user wants the primary key copied and the source session
            // isn't Axion (Axion throws SQLException for getPrimaryKeys())
            
            // TODO: Perhaps we can tell the user when they click "Copy Table"
            // if the source session is Axion and they want primary keys that 
            // it's not possible.
            if (_prefs.isCopyPrimaryKeys()
            		&& !DialectFactory.isAxionSession(sourceSession)) {
                String pkString = DBUtil.getPKColumnString(sourceCon, ti);
                if (pkString != null) {
                    result.append(",\n\tPRIMARY KEY ");
                    result.append(pkString);
                }
            }
            result.append(")");
        } catch (MappingException e) {
            if (colInfo != null) {
                //i18n[DBUtil.error.maptype=Couldn't map type for table='{0}' 
                // column='{1}']
                String msg = 
                    s_stringMgr.getString("DBUtil.error.maptype",
                                          new String[] { destinationTableName,
                                                         colInfo.getColumnName()});
                log.error(msg, e);
            }
            throw e;
        }
        
        return result.toString();
    }
    
    /**
     * 
     * @param con
     * @param ti
     * @return
     * @throws SQLException
     */
    public static String getColumnList(TableColumnInfo[] colInfoArr) 
        throws SQLException 
    {
        StringBuffer result = new StringBuffer();
        
        for (int i = 0; i < colInfoArr.length; i++) {
            TableColumnInfo colInfo = colInfoArr[i];
            String columnName = colInfo.getColumnName();
            result.append(columnName);
            if (i < colInfoArr.length-1) {
                result.append(", ");
            }
        }       
        return result.toString();
    }    
    
    /**
     * Uses the column type mapper to get the column type and appends that to the
     * name with an optional not null modifier.
     * 
     * @param colInfo
     * @return
     * @throws UserCancelledOperationException
     * @throws MappingException
     */
    public static String getColumnSql(SessionInfoProvider prov, 
                                      TableColumnInfo colInfo, 
                                      String sourceTableName,
                                      String destTableName) 
        throws UserCancelledOperationException, MappingException 
    {
        String columnName = colInfo.getColumnName();
        if (_prefs.isCheckKeywords()) {
            checkKeyword(prov.getCopyDestSession(), destTableName, columnName);
        }
        StringBuffer result = new StringBuffer(columnName);
        boolean notNullable = colInfo.isNullable().equalsIgnoreCase("NO");
        String typeName = ColTypeMapper.mapColType(prov.getCopySourceSession(), 
                                                   prov.getCopyDestSession(), 
                                                   colInfo,
                                                   sourceTableName,
                                                   destTableName);
        result.append(" ");
        result.append(typeName);
        if (notNullable) {
            result.append(" NOT NULL");
        } else {
            ISession destSession = prov.getCopyDestSession();
            HibernateDialect d = 
                DialectFactory.getDialect(destSession, DialectFactory.DEST_TYPE);
            String nullString = d.getNullColumnString().toUpperCase();
            result.append(nullString);
        }
        return result.toString();
    }
    
    /**
     * Checks the specified column is not a keyword in the specified session.
     * 
     * @param session the session whose keywords to check against
     * @param table the name of the table to use in the error message
     * @param column the name of the column to check
     * 
     * @throws MappingException if the specified column is a keyword in the 
     *                          specified session
     */
    public static void checkKeyword(ISession session, String table, String column) 
        throws MappingException 
    {
        if (isKeyword(session, column)) {
            String message = getMessage("DBUtil.mappingErrorKeyword",
                                        new String[] { table, column });
            throw new MappingException(message);
        }                   
    }    
        
    /**
     * 
     * @param sourceConn
     * @param ti
     * @param column
     * @return
     * @throws SQLException
     */
    public static String getColumnName(SQLConnection sourceConn, 
                                       ITableInfo ti, 
                                       int column) 
    throws SQLException 
    {
        TableColumnInfo[] infoArr = sourceConn.getSQLMetaData().getColumnInfo(ti);
        TableColumnInfo colInfo = infoArr[column];
        return colInfo.getColumnName();
    }
        
    /**
     * 
     * @param sourceConn
     * @param ti
     * @return
     * @throws SQLException
     */
    public static String[] getColumnNames(SQLConnection sourceConn, 
                                          ITableInfo ti) 
        throws SQLException 
    {
        TableColumnInfo[] infoArr = sourceConn.getSQLMetaData().getColumnInfo(ti);
        String[] result = new String[infoArr.length];
        for (int i = 0; i < result.length; i++) {
            TableColumnInfo colInfo = infoArr[i];
            result[i] = colInfo.getColumnName();
        }
        return result;
    }
    
    /**
     * 
     * @param columnList
     * @param ti
     * @return
     * @throws SQLException
     */
    public static String getSelectQuery(SessionInfoProvider prov, 
                                        String columnList, 
                                        ITableInfo ti) 
        throws SQLException, UserCancelledOperationException 
    {
        StringBuffer result = new StringBuffer("select ");
        result.append(columnList);
        result.append(" from ");
        ISession sourceSession = prov.getCopySourceSession();
        
        //String sourceSchema = null;
        // MySQL uses catalogs instead of schemas
        /*
        if (DialectFactory.isMySQLSession(sourceSession)) {
            if (log.isDebugEnabled()) {
                String catalog = 
                    prov.getSourceSelectedDatabaseObjects()[0].getCatalogName();
                String schema =
                    prov.getSourceSelectedDatabaseObjects()[0].getSchemaName();
                log.debug("Detected MySQL, using catalog ("+catalog+") " +
                          "instead of schema ("+schema+")");
            }
            sourceSchema = 
                prov.getSourceSelectedDatabaseObjects()[0].getCatalogName();
        } else {
            sourceSchema = 
                prov.getSourceSelectedDatabaseObjects()[0].getSchemaName();
        }
        */
        String tableName = getQualifiedObjectName(sourceSession, 
                                                  ti.getCatalogName(), 
                                                  ti.getSchemaName(), 
                                                  ti.getSimpleName(), 
                                                  DialectFactory.SOURCE_TYPE);
        result.append(tableName);
        return result.toString();
    }
    
    
    /**
     * 
     * @param sourceConn
     * @param columnList
     * @param ti
     * @return
     * @throws SQLException
     */
    public static String getInsertSQL(SessionInfoProvider prov, 
                                      String columnList, 
                                      ITableInfo ti,
                                      int columnCount) 
        throws SQLException, UserCancelledOperationException
    {
        StringBuffer result = new StringBuffer();
        result.append("insert into ");
        String destSchema = prov.getDestSelectedDatabaseObject().getSimpleName();
        String destCatalog = prov.getDestSelectedDatabaseObject().getCatalogName();
        ISession destSession = prov.getCopyDestSession();
        result.append(getQualifiedObjectName(destSession, 
                                             destCatalog, 
                                             destSchema,
                                             ti.getSimpleName(), 
                                             DialectFactory.DEST_TYPE));
        result.append(" ( ");
        result.append(columnList);
        result.append(" ) values ( ");
        result.append(getQuestionMarks(columnCount));
        result.append(" )");
        return result.toString();
    }
    
    /**
     * Returns a boolean value indicating whether or not the specified 
     * TableColumnInfo represents a database column that holds binary type 
     * data.
     * 
     * @param columnInfo the TableColumnInfo to examine
     * @return true if binary; false otherwise.
     */
    public static boolean isBinaryType(TableColumnInfo columnInfo) {
        boolean result = false;
        int type = columnInfo.getDataType();
        if (type == Types.BINARY
                || type == Types.BLOB
                || type == Types.LONGVARBINARY
                || type == Types.VARBINARY)
        {
            result = true;
        }
        return result;
    }  
    
    /**
     * Decide whether or not the session specified needs fully qualified table
     * names (schema.table).  In most databases this is optional (Oracle).  
     * In others it is required (Progress).  In still others it must not occur.
     * (Axion, Hypersonic)
     * 
     * @param session
     * @param catalogName
     * @param schemaName
     * @param objectName
     * @return
     * @throws UserCancelledOperationException
     */
    public static String getQualifiedObjectName(ISession session,
                                                String catalogName,
                                                String schemaName,
                                                String objectName, 
                                                int sessionType) 
        throws UserCancelledOperationException
    {
        String catalog = fixCase(session, catalogName);
        String schema = fixCase(session, schemaName);
        String object = fixCase(session, objectName);
        

        if ((catalog == null || catalog.equals("")) && 
                (schema == null || schema.equals(""))) {
            return object;
        }
        StringBuffer result = new StringBuffer();
        if (catalog != null && !catalog.equals("")) {
            result.append(catalog);
            result.append(getCatSep(session));
        }
        if (schema != null && !schema.equals("")) {
            result.append(schema);
            result.append(".");
        }
        result.append(object);
        return result.toString();
    }    
    
    public static String getCatSep(ISession session) {
        String catsep = ".";
        try {
            SQLDatabaseMetaData md = session.getSQLConnection().getSQLMetaData();
            catsep = md.getCatalogSeparator();
        } catch (SQLException e) {
            log.error("getCatSep: Unexpected Exception - "+e.getMessage(), e);
        }
        return catsep;
    }
    
    public static String fixCase(ISession session, String identifier)  
    {
        if (identifier == null || identifier.equals("")) {
            return identifier;
        }
        SQLDatabaseMetaData md = session.getSQLConnection().getSQLMetaData();
        try {
            if (md.storesUpperCaseIdentifiers()) {
                return identifier.toUpperCase();
            } else {
                return identifier.toLowerCase();
            }
        } catch (SQLException e) {
            if (log.isDebugEnabled()) {
                log.debug("fixCase: unexpected exception: "+e.getMessage());
            }
            return identifier;
        }
    }    
        
    /**
     * Generates a string of question marks which are used for creating 
     * PreparedStatements.  The question marks are delimited by commas.
     * 
     * @param count the number of question marks (representing PS bind variables).
     * @return
     */
    private static String getQuestionMarks(int count) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < count; i++) {
            result.append("?");
            if (i < count-1) {
                result.append(", ");
            }
        }
        return result.toString();
    }    
    
    /**
     * 
     * @param sourceConn
     * @param ti
     * @return
     * @throws SQLException
     */
    public static int getColumnCount(SQLConnection sourceConn, ITableInfo ti) 
        throws SQLException 
    {
        return sourceConn.getSQLMetaData().getColumnInfo(ti).length;
    }
    
    /**
     * 
     * @param con
     * @param ti
     * @param column
     * @return
     * @throws SQLException
     */
    public static int getColumnType(SQLConnection con, ITableInfo ti, int column) 
        throws SQLException 
    {
        TableColumnInfo[] infoArr = con.getSQLMetaData().getColumnInfo(ti);
        TableColumnInfo colInfo = infoArr[column];
        return colInfo.getDataType();
    }
    
    public static int[] getColumnTypes(SQLConnection con, ITableInfo ti) 
        throws SQLException 
    {
        TableColumnInfo[] infoArr = con.getSQLMetaData().getColumnInfo(ti);
        int[] result = new int[infoArr.length];
        for (int i = 0; i < result.length; i++) {
            TableColumnInfo colInfo = infoArr[i];
            result[i] = colInfo.getDataType();
        }
        return result;
    }
    
    /** 
     * Shamelessly copied this from the SQL Scripts plugin 
     * by Johan Compagner and Gerd Wagner
     */
    public static Collection getCreateIndicesSQL(SessionInfoProvider prov, 
                                                 ITableInfo ti) 
        throws SQLException, UserCancelledOperationException 
    {
      
        ISession sourceSession = prov.getCopySourceSession();
        ISession destSession = prov.getCopyDestSession();
        SQLConnection con = sourceSession.getSQLConnection();
        String destSchema = prov.getDestSelectedDatabaseObject().getSimpleName();
        String destCatalog = prov.getDestSelectedDatabaseObject().getCatalogName();                

        ArrayList result = new ArrayList();
        Vector pkCols = new Vector();
        
        DatabaseMetaData metaData = con.getConnection().getMetaData();
        ResultSet primaryKeys = null;
        if (_prefs.isCopyPrimaryKeys()) {
            try {
                primaryKeys = metaData.getPrimaryKeys(ti.getCatalogName(), 
                                                      ti.getSchemaName(), 
                                                      ti.getSimpleName());
                while(primaryKeys.next()) {
                    IndexColInfo indexColInfo = 
                        new IndexColInfo(primaryKeys.getString("COLUMN_NAME"));
                    pkCols.add(indexColInfo);
                }
    
            } catch (SQLException e) {
                
            } finally {
                if (primaryKeys != null) try { primaryKeys.close(); } catch (SQLException e) {}
            }
            Collections.sort(pkCols, IndexColInfo.NAME_COMPARATOR);
        }
        
        Hashtable buf = new Hashtable();
        
        ResultSet indexInfo = metaData.getIndexInfo(ti.getCatalogName(), 
                                                    ti.getSchemaName(), 
                                                    ti.getSimpleName(), 
                                                    false, false);
        
        boolean unique = false;
        while(indexInfo.next()) {
            String ixName = indexInfo.getString("INDEX_NAME");
            if(null == ixName) {
                continue;
            }
            unique = !indexInfo.getBoolean("NON_UNIQUE");
            IndexInfo ixi = (IndexInfo) buf.get(ixName);
            if(null == ixi) {
                Vector ixCols = new Vector();
                String table = indexInfo.getString("TABLE_NAME");
                ixCols.add(new IndexColInfo(indexInfo.getString("COLUMN_NAME"), 
                                            indexInfo.getInt("ORDINAL_POSITION")));
                buf.put(ixName, new IndexInfo(table, ixName, ixCols));
            } else {
                ixi.cols.add(new IndexColInfo(indexInfo.getString("COLUMN_NAME"), 
                                              indexInfo.getInt("ORDINAL_POSITION")));
            }
        }
        indexInfo.close();
        IndexInfo[] ixs = (IndexInfo[]) buf.values().toArray(new IndexInfo[buf.size()]);
        HashMap indexMap = new HashMap();
        for (int i = 0; i < ixs.length; i++) {
            Collections.sort(ixs[i].cols, IndexColInfo.NAME_COMPARATOR);
            
            if(pkCols.equals(ixs[i].cols)) {
                // Serveral DBs automatically create an index for primary key fields
                // and return this index in getIndexInfo(). We remove this index from the script
                // because it would break the script with an index already exists error.
                continue;
            }
            
            Collections.sort(ixs[i].cols, IndexColInfo.ORDINAL_POSITION_COMPARATOR);
            
            StringBuffer sbToAppend = new StringBuffer();
            sbToAppend.append("CREATE");
            sbToAppend.append(unique ? " UNIQUE ": " ");
            sbToAppend.append("INDEX ");
            sbToAppend.append(ixs[i].ixName);
            sbToAppend.append(" ON ");
            String table = getQualifiedObjectName(destSession, 
                                                  destCatalog, 
                                                  destSchema, 
                                                  ixs[i].table, 
                                                  DialectFactory.DEST_TYPE);
            sbToAppend.append(table);
            StringBuffer indexMapKey = new StringBuffer(ixs[i].table);
            StringBuffer columnBuffer = new StringBuffer();
            if(ixs[i].cols.size() == 1) {
                columnBuffer.append("(").append(ixs[i].cols.get(0));
                
                for (int j = 1; j < ixs[i].cols.size(); j++) {
                    columnBuffer.append(",").append(ixs[i].cols.get(j));
                }
            } else {
                columnBuffer.append("\n(\n");
                for (int j = 0; j < ixs[i].cols.size(); j++) {
                    if(j < ixs[i].cols.size() -1) {
                        columnBuffer.append("  " + ixs[i].cols.get(j) + ",\n");
                    } else {
                        columnBuffer.append("  " + ixs[i].cols.get(j) + "\n");
                    }
                }
            }
            columnBuffer.append(")");
            indexMapKey.append("|");
            indexMapKey.append(columnBuffer.toString());
            sbToAppend.append(columnBuffer.toString());
            if (_prefs.isPruneDuplicateIndexDefs()
                    && indexMap.containsKey(indexMapKey.toString())) 
            {
                if (log.isDebugEnabled()) {
                    //i18n[DBUtil.info.prunedupidxs=getCreateIndicesSQL: 
                    // pruning duplicate index named '{0}' on '{1}']
                    String msg =
                        s_stringMgr.getString("DBUtil.info.prunedupidxs",
                                              new String[] { ixs[i].ixName,
                                                      indexMapKey.toString() });
                    log.debug(msg);
                }
            } else {
                indexMap.put(indexMapKey.toString(),null);
                result.add(sbToAppend.toString());
            }
        }
      return result;
    }
        
    public static void validateColumnNames(ITableInfo ti, 
                                           SessionInfoProvider prov) 
        throws MappingException, UserCancelledOperationException
    {
        if (prov == null) {
            return;
        }
        ISession sourceSession = prov.getCopySourceSession();
        ISession destSession = prov.getCopyDestSession();
        if (sourceSession == null || destSession == null) {
            return;
        }
        SQLConnection sourceCon = sourceSession.getSQLConnection();
        SQLConnection con = destSession.getSQLConnection();        
        TableColumnInfo[] colInfoArr = null;
        try {
            colInfoArr = sourceCon.getSQLMetaData().getColumnInfo(ti);
        } catch (SQLException e) {
            // ignore any SQLExceptions.  This would only if we could not get
            // column info from the SQL database meta data.
            return;
        }
        for (int colIdx = 0; colIdx < colInfoArr.length; colIdx++) {
            TableColumnInfo colInfo = colInfoArr[colIdx];
            IDatabaseObjectInfo selectedDestObj = 
                prov.getDestSelectedDatabaseObject();
            String schema = selectedDestObj.getSimpleName();
            String catalog = selectedDestObj.getCatalogName(); 
            String tableName = getQualifiedObjectName(destSession, 
                                                      catalog, 
                                                      schema,
                                                      "dbcopytest", 
                                                      DialectFactory.DEST_TYPE); 
            
            StringBuffer sql = 
                new StringBuffer("CREATE TABLE ");
            sql.append(tableName);
            sql.append(" ( ");
            sql.append(colInfo.getColumnName());
            sql.append(" CHAR(10) )");
            boolean cascade = DialectFactory.isFrontBaseSession(destSession);
            try {
                dropTable(tableName, 
                          schema, 
                          catalog, 
                          destSession, 
                          cascade, DialectFactory.DEST_TYPE);
                DBUtil.executeUpdate(con, sql.toString(), false);
            } catch (SQLException e) {
                String message = getMessage("DBUtil.mappingErrorKeyword",
                                            new String[] { ti.getSimpleName(), 
                                                           colInfo.getColumnName() });
                log.error(message, e);
                throw new MappingException(message);
            } finally {
                dropTable(tableName, 
                          schema, 
                          catalog, 
                          destSession, 
                          cascade, DialectFactory.DEST_TYPE);
            }
            
        }        
    }
    
    public static boolean dropTable(String tableName,
                                    String schemaName,
                                    String catalogName,
                                    ISession session,
                                    boolean cascade, int sessionType) 
        throws UserCancelledOperationException
    {
        boolean result = false;
        SQLConnection con = session.getSQLConnection();
        String table = getQualifiedObjectName(session, 
                                             null, 
                                             schemaName,
                                             tableName, sessionType);
        String dropsql = "DROP TABLE "+table;
        if (cascade) {
            dropsql += " CASCADE";
        }
        try {
            DBUtil.executeUpdate(con, dropsql, false);
            result = true;
        } catch (SQLException e) {
            /* Do nothing */
        }
        return result;
    }
    
    public static boolean sameDatabaseType(ISession session1, 
                                           ISession session2) 
    {
        boolean result = false;
        String driver1ClassName = session1.getDriver().getDriverClassName();
        String driver2ClassName = session2.getDriver().getDriverClassName();
        if (driver1ClassName.equals(driver2ClassName)) {
            result = true; 
        }
        return result;
    }

    /**
     * Gets the SQL statement which can be used to select the maximum length
     * of the current data found in tableName within the specified column.
     * 
     * @param sourceSession
     * @param colInfo
     * @param tableName
     * @return
     */
    public static String getMaxColumnLengthSQL(ISession sourceSession, 
                                               TableColumnInfo colInfo,
                                               String tableName)
        throws UserCancelledOperationException
    {
        StringBuffer result = new StringBuffer();
        HibernateDialect dialect = 
            DialectFactory.getDialect(sourceSession, DialectFactory.DEST_TYPE);
        String lengthFunction = dialect.getLengthFunction(colInfo.getDataType());
        if (lengthFunction == null) {
            log.error("Length function is null for dialect="+
                      dialect.getClass().getName()+". Using 'length'");
            lengthFunction = "length";
        }
        String maxFunction = dialect.getMaxFunction();
        if (maxFunction == null) {
            log.error("Max function is null for dialect="+
                      dialect.getClass().getName()+". Using 'max'");
            maxFunction = "max";
        }
        result.append("select ");
        result.append(maxFunction);
        result.append("(");
        result.append(lengthFunction);
        result.append("(");
        result.append(colInfo.getColumnName());
        result.append(")) from ");
        String table = getQualifiedObjectName(sourceSession, 
                                              colInfo.getCatalogName(), 
                                              colInfo.getSchemaName(),
                                              tableName, 
                                              DialectFactory.SOURCE_TYPE); 
        result.append(table);
        return result.toString();
    }
}
