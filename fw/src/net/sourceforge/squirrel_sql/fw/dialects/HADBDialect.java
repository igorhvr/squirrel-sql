package net.sourceforge.squirrel_sql.fw.dialects;

import java.sql.Types;

import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.TableColumnInfo;

import org.hibernate.HibernateException;
import org.hibernate.dialect.GenericDialect;

public class HADBDialect extends GenericDialect implements HibernateDialect {

    public HADBDialect() {
        super();

        registerColumnType(Types.BIGINT, "double integer");
        registerColumnType(Types.BINARY, 8000, "binary($l)");
        registerColumnType(Types.BINARY, "binary(8000)");
        registerColumnType(Types.BIT, "smallint");
        registerColumnType(Types.BOOLEAN, "smallint");
        registerColumnType(Types.BLOB, "blob");
        registerColumnType(Types.CHAR, 8000, "char($l)");
        registerColumnType(Types.CHAR, "char(8000)");
        registerColumnType(Types.CLOB, "clob");
        registerColumnType(Types.DATE, "date");
        registerColumnType(Types.DECIMAL, "decimal($p,$s)");
        registerColumnType(Types.DOUBLE, "double precision");
        registerColumnType(Types.FLOAT, "float($p)");
        registerColumnType(Types.INTEGER, "integer");
        registerColumnType(Types.LONGVARBINARY, "blob");
        registerColumnType(Types.LONGVARCHAR, "clob");
        registerColumnType(Types.NUMERIC, "decimal($p,$s)");
        registerColumnType(Types.REAL, "real");
        registerColumnType(Types.SMALLINT, "smallint");
        registerColumnType(Types.TIME, "time");
        registerColumnType(Types.TIMESTAMP, "timestamp");
        registerColumnType(Types.TINYINT, "smallint");
        registerColumnType(Types.VARBINARY, 8000, "varbinary($l)");
        registerColumnType(Types.VARBINARY, "varbinary(8000)");
        registerColumnType(Types.VARCHAR, 8000, "varchar($l)");
        registerColumnType(Types.VARCHAR, "varchar(8000)");
        
    }
    
    /**
     * Returns a boolean indicating whether or not the specified database object
     * can be pasted into for this database dialect.  Some databases support the
     * notion of schemas where tables live, and in those cases pasting to a 
     * database object in the object tree is not really appropriate.  However, 
     * other databases don't support schemas (like Axion, Firebird)
     * 
     * @param info
     * @return
     */    
    public boolean canPasteTo(IDatabaseObjectInfo info) {
        return true;
    }

    public String[] getAddPrimaryKeySQL(String pkName,
            TableColumnInfo[] colInfos) {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getColumnAddSQL(TableColumnInfo info)
            throws HibernateException, UnsupportedOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getColumnCommentAlterSQL(TableColumnInfo info)
        throws UnsupportedOperationException 
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Returns the SQL that forms the command to drop the specified colum in the
     * specified table.
     * 
     * @param tableName the name of the table that has the column
     * @param columnName the name of the column to drop.
     * @return
     * @throw UnsupportedOperationException if the database doesn't support 
     *         dropping columns. 
     */    
    public String getColumnDropSQL(String tableName, String columnName)
        throws UnsupportedOperationException 
    {
        throw new UnsupportedOperationException("Not yet implemented");    
    }

    /**
     * Some jdbc drivers are hopelessly broken with regard to reporting the 
     * COLUMN_SIZE.  For example, MaxDB has a "long byte" data type which can
     * store up to 2G of data, yet the driver reports that the column size is 
     * "8" - real helpful.  So for drivers that have this problem, return the 
     * "proper" maximum column length for the specified dataType.  If the driver
     * doesn't have this problem, just return the columnSize.
     * 
     * @param columnSize the size of the column as reported by the jdbc driver
     * @param dataType the type of the column.
     * 
     * @return the specified columnSize if the jdbc driver isn't broken; 
     *         otherwise, the maximum column size for the specified dataType if
     *         the driver is broken. 
     */    
    public int getColumnLength(int columnSize, int dataType) {
        // HADB reports "10" for column size of BLOB/CLOB
        if (dataType == Types.CLOB || dataType == Types.BLOB) {
            return Integer.MAX_VALUE; // 2GB (2^32)
        }
        return columnSize;
    }

    /**
     * Returns a boolean value indicating whether or not this database dialect
     * supports renaming columns.
     * 
     * @return true if the database supports changing the name of columns;  
     *         false otherwise.
     */
    public boolean supportsRenameColumn() {
        // TODO: need to verify this
        return true;
    }    
    
    /**
     * Returns the SQL that is used to change the column name.
     * 
     * alter table test rename column mycol mycol2
     * 
     * @param from the TableColumnInfo as it is
     * @param to the TableColumnInfo as it wants to be
     * 
     * @return the SQL to make the change
     */    
    public String getColumnNameAlterSQL(TableColumnInfo from, TableColumnInfo to) {
        StringBuffer result = new StringBuffer();
        result.append("ALTER TABLE ");
        result.append(from.getTableName());
        result.append(" RENAME COLUMN ");
        result.append(from.getColumnName());
        result.append(" ");
        result.append(to.getColumnName());
        return result.toString();
    }

    /**
     * Returns the SQL used to alter the nullability of the specified column 
     * 
     * @param info the column to modify
     * @return the SQL to execute
     */
    public String getColumnNullableAlterSQL(TableColumnInfo info) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Returns the SQL that is used to change the column type.
     * 
     * @param from the TableColumnInfo as it is
     * @param to the TableColumnInfo as it wants to be
     * 
     * @return the SQL to make the change
     * @throw UnsupportedOperationException if the database doesn't support 
     *         modifying column types. 
     */    
    public String getColumnTypeAlterSQL(TableColumnInfo from, TableColumnInfo to)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * The string which identifies this dialect in the dialect chooser.
     * 
     * @return a descriptive name that tells the user what database this dialect
     *         is design to work with.
     */        
    public String getDisplayName() {
        return "Sun HADB";
    }

    /**
     * Returns the name of the function that measures the length of a character
     * string.
     * 
     * @param dataType the java.sql.Types data type.  Some databases have 
     *                 different length functions for different data types. 
     * 
     * @return the name of the function to be applied to a column to determine
     *         the length of that column in a particular record.
     */    
    public String getLengthFunction(int dataType) {
        return "char_length";
    }

    /**
     * Returns the name of the aggregate function that determines the max value
     * of an expression.
     *  
     * @return the name of the function to be applied to a set to determine the 
     *         element with the highest numeric value.
     */    
    public String getMaxFunction() {
        return "max";
    }

    /**
     * Returns the maximum precision allowed by the database for number type 
     * fields that specify the length of the number to the left of the decimal
     * point in digits.  If the HibernateDialect implementation doesn't ever
     * use $p in any call to registerColumnType(), then this maximum precsision
     * will not be used.
     * 
     * @param dataType the java.sql.Types data type.
     * 
     * @return the maximum number that can be used in a column declaration for 
     *         precision for the specified type.
     */    
    public int getMaxPrecision(int dataType) {
        if (dataType == Types.FLOAT) {
            return 52;
        } 
        if (dataType == Types.DECIMAL
                || dataType == Types.NUMERIC) {
            return 31;
        }
        return 0;
    }

    /**
     * Returns the maximum scale allowed by the database for number type 
     * fields that specify the length of the number to the right of the decimal
     * point in digits.  If the HibernateDialect implementation doesn't ever
     * use $s in any call to registerColumnType(), then this maximum scale
     * will not be used.
     * 
     * @param dataType the java.sql.Types data type.
     * 
     * @return the maximum number that can be used in a column declaration for 
     *         scale for the specified type.
     */    
    public int getMaxScale(int dataType) {
        return getMaxPrecision(dataType);
    }

    /**
     * Returns the number of digits of precision is represented by the specifed
     * columnSize for the specified dataType. Some DBs represent precision as 
     * the total number of digits on the right or left of the decimal.  That is
     * what we want.  Others (like PostgreSQL) give the number of bytes of 
     * storage a column can use - less than useful, since the SQL-92 says 
     * "number of digits" and this is what most other DBs use. 
     * 
     * @param columnSize the size of the column as reported by the driver. 
     * @param dataType the java.sql.Types data type.
     * 
     * @return a number indicating the total number of digits (includes both
     *         sides of the decimal point) the column can represent.
     */    
    public int getPrecisionDigits(int columnSize, int dataType) {
        return columnSize;
    }

    /**
     * Returns the SQL that forms the command to drop the specified table.  If
     * cascade contraints is supported by the dialect and cascadeConstraints is
     * true, then a drop statement with cascade constraints clause will be 
     * formed.
     * 
     * @param tableName the table to drop
     * @param cascadeConstraints whether or not to drop any FKs that may 
     * reference the specified table.
     * 
     * @return the drop SQL command.
     */    
    public String getTableDropSQL(String tableName, boolean cascadeConstraints) {
        return DialectUtils.getTableDropSQL(tableName, false, cascadeConstraints);
    }

    /**
     * Returns a boolean value indicating whether or not this dialect supports
     * adding comments to columns.
     * 
     * @return true if column comments are supported; false otherwise.
     */    
    public boolean supportsColumnComment() {
        return false;
    }

    /**
     * Returns a boolean value indicating whether or not this database dialect
     * supports dropping columns from tables.
     * 
     * @return true if the database supports dropping columns; false otherwise.
     */    
    public boolean supportsDropColumn() {
        return false;
    }

    /**
     * Returns boolean value indicating whether or not this dialect supports the
     * specified database product/version.
     * 
     * @param databaseProductName the name of the database as reported by 
     *                            DatabaseMetaData.getDatabaseProductName()
     * @param databaseProductVersion the version of the database as reported by
     *                              DatabaseMetaData.getDatabaseProductVersion()
     * @return true if this dialect can be used for the specified product name
     *              and version; false otherwise.
     */    
    public boolean supportsProduct(String databaseProductName,
                                   String databaseProductVersion) {
        if (databaseProductName == null) {
            return false;
        }
        String prodName = "sun java system high availability";
        if (databaseProductName.trim().toLowerCase().startsWith(prodName)) {
            // We don't yet have the need to discriminate by version.
            return true;
        }
        return false;
    }

    public boolean supportsSchemasInTableDefinition() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Returns a boolean value indicating whether or not this database dialect
     * supports changing a column from null to not-null and vice versa.
     * 
     * @return true if the database supports dropping columns; false otherwise.
     */    
    public boolean supportsAlterColumnNull() {
        // TODO Auto-generated method stub
        return false;
    }
    
    /**
     * Returns a boolean value indicating whether or not this database dialect
     * supports changing a column's default value.
     * 
     * @return true if the database supports modifying column defaults; false 
     *         otherwise
     */
    public boolean supportsAlterColumnDefault() {
        // TODO Need to verify this
        return true;
    }
    
    /**
     * Returns the SQL command to change the specified column's default value
     *   
     * @param info the column to modify and it's default value.
     * @return SQL to make the change
     */
    public String getColumnDefaultAlterSQL(TableColumnInfo info) {
        // TODO need to implement or change the message
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
}
