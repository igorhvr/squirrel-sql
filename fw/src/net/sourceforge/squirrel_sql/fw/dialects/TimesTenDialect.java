/*
 * Copyright (C) 2006 Rob Manning
 * manningr@users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sourceforge.squirrel_sql.fw.dialects;

import java.sql.Types;

import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.TableColumnInfo;

/**
 * An extension to the standard Hibernate Informix dialect
 * 
 */
public class TimesTenDialect extends org.hibernate.dialect.TimesTenDialect 
                             implements HibernateDialect {
    
    public TimesTenDialect() {
        super();
        registerColumnType(Types.BIGINT, "bigint");
        registerColumnType(Types.BINARY, 8300,"binary($l)");
        registerColumnType(Types.BINARY, 4194304,"varbinary($l)");
        registerColumnType(Types.BINARY, "varbinary(4194304)");
        registerColumnType(Types.BIT, "tinyint");
        registerColumnType(Types.BLOB, 4194304, "varbinary($l)");
        registerColumnType(Types.BLOB, "varbinary(4194304)");
        registerColumnType(Types.BOOLEAN, "tinyint");
        registerColumnType(Types.CHAR, 8300, "char($l)");
        registerColumnType(Types.CHAR, 4194304, "varchar($l)");
        registerColumnType(Types.CHAR, "varchar(4194304)");        
        registerColumnType(Types.CLOB, 4194304, "varchar($l)");
        registerColumnType(Types.CLOB, "varchar(4194304)");
        registerColumnType(Types.DATE, "date");
        registerColumnType(Types.DECIMAL, "decimal($p,$s)");
        registerColumnType(Types.DOUBLE, "double");
        registerColumnType(Types.FLOAT, "float");
        registerColumnType(Types.INTEGER, "integer");        
        registerColumnType(Types.LONGVARBINARY, 4194304,"varbinary($l)");
        registerColumnType(Types.LONGVARBINARY, "varbinary(4194304)");
        registerColumnType(Types.LONGVARCHAR, 4194304, "varchar($l)");
        registerColumnType(Types.LONGVARCHAR, "varchar(4194304)");
        registerColumnType(Types.NUMERIC, "numeric($p,$s)");
        registerColumnType(Types.REAL, "float");
        registerColumnType(Types.SMALLINT, "smallint");
        registerColumnType(Types.TIME, "time");
        registerColumnType(Types.TIMESTAMP, "timestamp");
        registerColumnType(Types.TINYINT, "tinyint");
        registerColumnType(Types.VARBINARY, 4194304, "varbinary($l)");
        registerColumnType(Types.VARBINARY, "varbinary(4194304)");
        registerColumnType(Types.VARCHAR, 4194304,"varchar($l)");
        registerColumnType(Types.VARCHAR, "varchar(4194304)");
      
    }    
    
    /* (non-Javadoc)
     * @see net.sourceforge.squirrel_sql.plugins.dbcopy.dialects.HibernateDialect#canPasteTo(net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType)
     */
    public boolean canPasteTo(IDatabaseObjectInfo info) {
        return true;
    }
    
    /* (non-Javadoc)
     * @see net.sourceforge.squirrel_sql.plugins.dbcopy.dialects.HibernateDialect#supportsSchemasInTableDefinition()
     */
    public boolean supportsSchemasInTableDefinition() {
        return true;
    }

    /* (non-Javadoc)
     * @see net.sourceforge.squirrel_sql.plugins.dbcopy.dialects.HibernateDialect#getLengthFunction()
     */
    public String getLengthFunction(int dataType) {
        return "length";
    }

    /* (non-Javadoc)
     * @see net.sourceforge.squirrel_sql.plugins.dbcopy.dialects.HibernateDialect#getMaxFunction()
     */
    public String getMaxFunction() {
        return "max";
    }

    /* (non-Javadoc)
     * @see net.sourceforge.squirrel_sql.plugins.dbcopy.dialects.HibernateDialect#getMaxPrecision(int)
     */
    public int getMaxPrecision(int dataType) {
    	int result = Integer.MAX_VALUE;
    	if (dataType == Types.DECIMAL || dataType == Types.NUMERIC) {
    		result = 40;
    	}
    	if (dataType == Types.FLOAT) {
    		result = 53;
    	}
    	return result;
    }

    /* (non-Javadoc)
     * @see net.sourceforge.squirrel_sql.plugins.dbcopy.dialects.HibernateDialect#getMaxScale(int)
     */
    public int getMaxScale(int dataType) {
        return getMaxPrecision(dataType);
    }
    
    /* (non-Javadoc)
     * @see net.sourceforge.squirrel_sql.plugins.dbcopy.dialects.HibernateDialect#getPrecisionDigits(int, int)
     */
    public int getPrecisionDigits(int columnSize, int dataType) {
        return columnSize;
    }

    /* (non-Javadoc)
     * @see net.sourceforge.squirrel_sql.plugins.dbcopy.dialects.HibernateDialect#getColumnLength(int, int)
     */
    public int getColumnLength(int columnSize, int dataType) {
        return columnSize;
    }

    /**
     * The string which identifies this dialect in the dialect chooser.
     * 
     * @return a descriptive name that tells the user what database this dialect
     *         is design to work with.
     */
    public String getDisplayName() {
        return "TimesTen";
    }    
    
    /**
     * Returns boolean value indicating whether or not this dialect supports the
     * specified database product/version.
     * 
     * @param databaseProductName the name of the database as reported by 
     * 							  DatabaseMetaData.getDatabaseProductName()
     * @param databaseProductVersion the version of the database as reported by
     *                              DatabaseMetaData.getDatabaseProductVersion()
     * @return true if this dialect can be used for the specified product name
     *              and version; false otherwise.
     */
    public boolean supportsProduct(String databaseProductName, 
								   String databaseProductVersion) 
	{
    	if (databaseProductName == null) {
    		return false;
    	}
    	if (databaseProductName.trim().toLowerCase().startsWith("timesten")) {
    		// We don't yet have the need to discriminate by version.
    		return true;
    	}
		return false;
	}
    
    /**
     * Returns the SQL statement to use to add a column to the specified table
     * using the information about the new column specified by info.
     * 
     * @param tableName the name of the table to create the SQL for.
     * @param info information about the new column such as type, name, etc.
     * @return
     * @throws UnsupportedOperationException if the database doesn't support 
     *         adding columns after a table has already been created.
     */
    public String[] getColumnAddSQL(String tableName, TableColumnInfo info) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("This database dialect doesn't support adding columns to tables");
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
     * Returns the SQL statement to use to add a comment to the specified 
     * column of the specified table.
     * 
     * @param tableName the name of the table to create the SQL for.
     * @param columnName the name of the column to create the SQL for.
     * @param comment the comment to add.
     * @return
     * @throws UnsupportedOperationException if the database doesn't support 
     *         annotating columns with a comment.
     */
    public String getColumnCommentAlterSQL(String tableName, String columnName, String comment) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("This database dialect doesn't support adding comments to columns");
    }
    
}
