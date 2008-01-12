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

import static net.sourceforge.squirrel_sql.fw.dialects.DialectUtils.CYCLE_CLAUSE;
import static net.sourceforge.squirrel_sql.fw.dialects.DialectUtils.NO_CYCLE_CLAUSE;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.ISQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.fw.sql.JDBCTypeMapper;
import net.sourceforge.squirrel_sql.fw.sql.TableColumnInfo;

/**
 * An extension to the standard Hibernate DB2 dialect
 */
public class DB2Dialect extends org.hibernate.dialect.DB2Dialect implements HibernateDialect
{

	public DB2Dialect()
	{
		super();
		registerColumnType(Types.BIGINT, "bigint");
		registerColumnType(Types.BINARY, 254, "char($l) for bit data");
		registerColumnType(Types.BINARY, "blob");
		registerColumnType(Types.BIT, "smallint");
		// DB2 spec says max=2147483647, but the driver throws an exception
		registerColumnType(Types.BLOB, 1073741823, "blob($l)");
		registerColumnType(Types.BLOB, "blob(1073741823)");
		registerColumnType(Types.BOOLEAN, "smallint");
		registerColumnType(Types.CHAR, 254, "char($l)");
		registerColumnType(Types.CHAR, 4000, "varchar($l)");
		registerColumnType(Types.CHAR, 32700, "long varchar");
		registerColumnType(Types.CHAR, 1073741823, "clob($l)");
		registerColumnType(Types.CHAR, "clob(1073741823)");
		// DB2 spec says max=2147483647, but the driver throws an exception
		registerColumnType(Types.CLOB, 1073741823, "clob($l)");
		registerColumnType(Types.CLOB, "clob(1073741823)");
		registerColumnType(Types.DATE, "date");
		registerColumnType(Types.DECIMAL, "decimal($p,$s)");
		registerColumnType(Types.DOUBLE, "float($p)");
		registerColumnType(Types.FLOAT, "float($p)");
		registerColumnType(Types.INTEGER, "int");
		registerColumnType(Types.LONGVARBINARY, 32700, "long varchar for bit data");
		registerColumnType(Types.LONGVARBINARY, 1073741823, "blob($l)");
		registerColumnType(Types.LONGVARBINARY, "blob(1073741823)");
		registerColumnType(Types.LONGVARCHAR, 32700, "long varchar");
		// DB2 spec says max=2147483647, but the driver throws an exception
		registerColumnType(Types.LONGVARCHAR, 1073741823, "clob($l)");
		registerColumnType(Types.LONGVARCHAR, "clob(1073741823)");
		registerColumnType(Types.NUMERIC, "bigint");
		registerColumnType(Types.REAL, "real");
		registerColumnType(Types.SMALLINT, "smallint");
		registerColumnType(Types.TIME, "time");
		registerColumnType(Types.TIMESTAMP, "timestamp");
		registerColumnType(Types.TINYINT, "smallint");
		registerColumnType(Types.VARBINARY, 254, "varchar($l) for bit data");
		registerColumnType(Types.VARBINARY, "blob");
		// The driver throws an exception for varchar with length > 3924
		registerColumnType(Types.VARCHAR, 3924, "varchar($l)");
		registerColumnType(Types.VARCHAR, 32700, "long varchar");
		// DB2 spec says max=2147483647, but the driver throws an exception
		registerColumnType(Types.VARCHAR, 1073741823, "clob($l)");
		registerColumnType(Types.VARCHAR, "clob(1073741823)");

	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#canPasteTo(net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo)
	 */
	public boolean canPasteTo(IDatabaseObjectInfo info)
	{
		boolean result = true;
		DatabaseObjectType type = info.getDatabaseObjectType();
		if (type.getName().equalsIgnoreCase("database"))
		{
			result = false;
		}
		return result;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#supportsSchemasInTableDefinition()
	 */
	public boolean supportsSchemasInTableDefinition()
	{
		return true;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getLengthFunction(int)
	 */
	public String getLengthFunction(int dataType)
	{
		return "length";
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getMaxFunction()
	 */
	public String getMaxFunction()
	{
		return "max";
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getMaxPrecision(int)
	 */
	public int getMaxPrecision(int dataType)
	{
		if (dataType == Types.DOUBLE || dataType == Types.FLOAT)
		{
			return 53;
		} else
		{
			return 31;
		}
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getMaxScale(int)
	 */
	public int getMaxScale(int dataType)
	{
		if (dataType == Types.DOUBLE || dataType == Types.FLOAT)
		{
			// double and float have no scale - that is DECIMAL_DIGITS is null.
			// Assume that is because it's variable - "floating" point.
			return 0;
		} else
		{
			return getMaxPrecision(dataType);
		}
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getPrecisionDigits(int, int)
	 */
	public int getPrecisionDigits(int columnSize, int dataType)
	{
		return columnSize;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getColumnLength(int, int)
	 */
	public int getColumnLength(int columnSize, int dataType)
	{
		return columnSize;
	}

	/**
	 * The string which identifies this dialect in the dialect chooser.
	 * 
	 * @return a descriptive name that tells the user what database this dialect is design to work with.
	 */
	public String getDisplayName()
	{
		return "DB2";
	}

	/**
	 * Returns boolean value indicating whether or not this dialect supports the specified database
	 * product/version.
	 * 
	 * @param databaseProductName
	 *           the name of the database as reported by DatabaseMetaData.getDatabaseProductName()
	 * @param databaseProductVersion
	 *           the version of the database as reported by DatabaseMetaData.getDatabaseProductVersion()
	 * @return true if this dialect can be used for the specified product name and version; false otherwise.
	 */
	public boolean supportsProduct(String databaseProductName, String databaseProductVersion)
	{
		if (databaseProductName == null)
		{
			return false;
		}
		if (databaseProductName.trim().startsWith("DB2"))
		{
			// We don't yet have the need to discriminate by version.
			return true;
		}
		return false;
	}

	/**
	 * Returns the SQL statement to use to add a column to the specified table using the information about the
	 * new column specified by info.
	 * 
	 * @param info
	 *           information about the new column such as type, name, etc.
	 * @return
	 * @throws UnsupportedOperationException
	 *            if the database doesn't support adding columns after a table has already been created.
	 */
	public String[] getAddColumnSQL(TableColumnInfo info, DatabaseObjectQualifier qualifier,
		SqlGenerationPreferences prefs) throws UnsupportedOperationException
	{
		final String qualifedTableName =
			DialectUtils.shapeQualifiableIdentifier(info.getTableName(), qualifier, prefs, this);
		final String shapedColumnName = DialectUtils.shapeIdentifier(info.getColumnName(), prefs, this);

		ArrayList<String> result = new ArrayList<String>();

		StringBuffer addColumn = new StringBuffer();
		addColumn.append("ALTER TABLE ");
		addColumn.append(qualifedTableName);
		addColumn.append(" ADD ");
		addColumn.append(shapedColumnName);
		addColumn.append(" ");
		addColumn.append(getTypeName(info.getDataType(),
			info.getColumnSize(),
			info.getColumnSize(),
			info.getDecimalDigits()));
		if (info.getDefaultValue() != null)
		{
			addColumn.append(" WITH DEFAULT ");
			if (JDBCTypeMapper.isNumberType(info.getDataType()))
			{
				addColumn.append(info.getDefaultValue());
			} else
			{
				addColumn.append("'");
				addColumn.append(info.getDefaultValue());
				addColumn.append("'");
			}
		}
		result.add(addColumn.toString());

		if (info.isNullable() == "NO")
		{
			// ALTER TABLE <TABLENAME> ADD CONSTRAINT NULL_FIELD CHECK (<FIELD> IS NOT
			// NULL)
			StringBuffer notnull = new StringBuffer();
			notnull.append("ALTER TABLE ");
			notnull.append(qualifedTableName);
			notnull.append(" ADD CONSTRAINT ");
			// TODO: should the constraint name simply be the column name or something more like a constraint
			// name?
			notnull.append(shapedColumnName);
			notnull.append(" CHECK (");
			notnull.append(shapedColumnName);
			notnull.append(" IS NOT NULL)");
			result.add(notnull.toString());
		}

		if (info.getRemarks() != null && !"".equals(info.getRemarks()))
		{
			result.add(getColumnCommentAlterSQL(info));
		}

		return result.toArray(new String[result.size()]);

	}

	/**
	 * Returns the SQL statement to use to add a comment to the specified column of the specified table.
	 * 
	 * @param tableName
	 *           the name of the table to create the SQL for.
	 * @param columnName
	 *           the name of the column to create the SQL for.
	 * @param comment
	 *           the comment to add.
	 * @return
	 * @throws UnsupportedOperationException
	 *            if the database doesn't support annotating columns with a comment.
	 */
	public String getColumnCommentAlterSQL(String tableName, String columnName, String comment)
		throws UnsupportedOperationException
	{
		return DialectUtils.getColumnCommentAlterSQL(tableName, columnName, comment);
	}

	/**
	 * Returns a boolean value indicating whether or not this database dialect supports dropping columns from
	 * tables.
	 * 
	 * @return true if the database supports dropping columns; false otherwise.
	 */
	public boolean supportsDropColumn()
	{
		return false;
	}

	/**
	 * Returns the SQL that forms the command to drop the specified colum in the specified table.
	 * 
	 * @param tableName
	 *           the name of the table that has the column
	 * @param columnName
	 *           the name of the column to drop.
	 * @return
	 * @throws UnsupportedOperationException
	 *            if the database doesn't support dropping columns.
	 */
	public String getColumnDropSQL(String tableName, String columnName)
	{
		int featureId = DialectUtils.COLUMN_DROP_TYPE;
		String msg = DialectUtils.getUnsupportedMessage(this, featureId);
		throw new UnsupportedOperationException(msg);
	}

	/**
	 * Returns the SQL that forms the command to drop the specified table. If cascade contraints is supported
	 * by the dialect and cascadeConstraints is true, then a drop statement with cascade constraints clause
	 * will be formed.
	 * 
	 * @param iTableInfo
	 *           the table to drop
	 * @param cascadeConstraints
	 *           whether or not to drop any FKs that may reference the specified table.
	 * @return the drop SQL command.
	 */
	public List<String> getTableDropSQL(ITableInfo iTableInfo, boolean cascadeConstraints,
		boolean isMaterializedView)
	{
		return DialectUtils.getTableDropSQL(iTableInfo,
			false,
			cascadeConstraints,
			false,
			DialectUtils.CASCADE_CLAUSE,
			false);
	}

	/**
	 * Returns the SQL that forms the command to add a primary key to the specified table composed of the given
	 * column names. ALTER TABLE table_name ADD CONSTRAINT contraint_name PRIMARY KEY (column_name)
	 * 
	 * @param pkName
	 *           the name of the constraint
	 * @param columnNames
	 *           the columns that form the key
	 * @return
	 */
	public String[] getAddPrimaryKeySQL(String pkName, TableColumnInfo[] columns, ITableInfo ti)
	{
		return new String[] { DialectUtils.getAddPrimaryKeySQL(ti, pkName, columns, false) };
	}

	/**
	 * Returns a boolean value indicating whether or not this dialect supports adding comments to columns.
	 * 
	 * @return true if column comments are supported; false otherwise.
	 */
	public boolean supportsColumnComment()
	{
		return true;
	}

	/**
	 * Returns the SQL statement to use to add a comment to the specified column of the specified table.
	 * 
	 * @param info
	 *           information about the column such as type, name, etc.
	 * @return
	 * @throws UnsupportedOperationException
	 *            if the database doesn't support annotating columns with a comment.
	 */
	public String getColumnCommentAlterSQL(TableColumnInfo info) throws UnsupportedOperationException
	{
		return DialectUtils.getColumnCommentAlterSQL(info);
	}

	/**
	 * Returns a boolean value indicating whether or not this database dialect supports changing a column from
	 * null to not-null and vice versa.
	 * 
	 * @return true if the database supports dropping columns; false otherwise.
	 */
	public boolean supportsAlterColumnNull()
	{
		return true;
	}

	/**
	 * Update: DB2 version 9.5 appears to support altering column nullability just fine via:
	 * 
	 * ALTER TABLE table_name ALTER COLUMN column_name SET NOT NULL
	 * 
	 * So, I'll use that 
	 * 
	 * Returns the SQL used to alter the specified column to not allow null values This appears to work: ALTER
	 * TABLE table_name ADD CONSTRAINT constraint_name CHECK (column_name IS NOT NULL) However, the jdbc driver
	 * still reports the column as nullable - which means I can't reliably display the correct value for this
	 * attribute in the UI. I tried this alternate syntax and it fails with an exception: ALTER TABLE
	 * table_name ALTER COLUMN column_name SET NOT NULL Error: com.ibm.db2.jcc.b.SqlException: DB2 SQL error:
	 * SQLCODE: -104, SQLSTATE: 42601, SQLERRMC: NOT;ER COLUMN mychar SET;DEFAULT, SQL State: 42601, Error
	 * Code: -104 I don't see how I can practically support changing column nullability in DB2.
	 * 
	 * @param info
	 *           the column to modify
	 * @return the SQL to execute
	 */
	public String getColumnNullableAlterSQL(TableColumnInfo info, DatabaseObjectQualifier qualifier,
		SqlGenerationPreferences prefs)
	{
		boolean nullable = info.isNullable().equalsIgnoreCase("yes");
		return getColumnNullableAlterSQL(info, nullable, qualifier, prefs);
	}

	/**
	 * Returns an SQL statement that alters the specified column nullability.
	 * 
	 * @param info
	 *           the column to modify
	 * @param nullable
	 *           whether or not the column should allow nulls after being altered
	 * @param qualifier
	 *           qualifier of the table
	 * @param prefs
	 *           preferences for generated sql scripts
	 * @return
	 */
	private String getColumnNullableAlterSQL(TableColumnInfo info, boolean nullable,
		DatabaseObjectQualifier qualifier, SqlGenerationPreferences prefs)
	{
		StringBuilder result = new StringBuilder();
		result.append("ALTER TABLE ");
		result.append(DialectUtils.shapeQualifiableIdentifier(info.getTableName(), qualifier, prefs, this));
		result.append(" ");
		result.append(DialectUtils.ALTER_COLUMN_CLAUSE);
		result.append(" ");
		result.append(DialectUtils.shapeIdentifier(info.getColumnName(), prefs, this));
		result.append(" SET ");
		if (nullable)
		{
			result.append("NULL");
		} else
		{
			result.append("NOT NULL");
		}
		return result.toString();		
	}
	
	/**
	 * Returns a boolean value indicating whether or not this database dialect supports renaming columns.
	 * 
	 * @return true if the database supports changing the name of columns; false otherwise.
	 */
	public boolean supportsRenameColumn()
	{
		return false;
	}

	/**
	 * Returns the SQL that is used to change the column name.
	 * 
	 * @param from
	 *           the TableColumnInfo as it is
	 * @param to
	 *           the TableColumnInfo as it wants to be
	 * @return the SQL to make the change
	 */
	public String getColumnNameAlterSQL(TableColumnInfo from, TableColumnInfo to)
	{
		int featureId = DialectUtils.COLUMN_NAME_ALTER_TYPE;
		String msg = DialectUtils.getUnsupportedMessage(this, featureId);
		throw new UnsupportedOperationException(msg);
	}

	/**
	 * Returns a boolean value indicating whether or not this dialect supports modifying a columns type.
	 * 
	 * @return true if supported; false otherwise
	 */
	public boolean supportsAlterColumnType()
	{
		return true;
	}

	/**
	 * Returns the SQL that is used to change the column type. ALTER TABLE table_name ALTER COLUMN column_name
	 * SET DATA TYPE data_type
	 * 
	 * @param from
	 *           the TableColumnInfo as it is
	 * @param to
	 *           the TableColumnInfo as it wants to be
	 * @return the SQL to make the change
	 * @throw UnsupportedOperationException if the database doesn't support modifying column types.
	 */
	public List<String> getColumnTypeAlterSQL(TableColumnInfo from, TableColumnInfo to,
		DatabaseObjectQualifier qualifier, SqlGenerationPreferences prefs) throws UnsupportedOperationException
	{
		ArrayList<String> result = new ArrayList<String>();
		StringBuffer tmp = new StringBuffer();
		tmp.append("ALTER TABLE ");
		tmp.append(DialectUtils.shapeQualifiableIdentifier(from.getTableName(), qualifier, prefs, this));
		tmp.append(" ALTER COLUMN ");
		tmp.append(DialectUtils.shapeIdentifier(from.getColumnName(), prefs, this));
		tmp.append(" SET DATA TYPE ");
		tmp.append(DialectUtils.getTypeName(to, this));
		result.add(tmp.toString());
		return result;
	}

	/**
	 * Returns a boolean value indicating whether or not this database dialect supports changing a column's
	 * default value.
	 * 
	 * @return true if the database supports modifying column defaults; false otherwise
	 */
	public boolean supportsAlterColumnDefault()
	{
		return true;
	}

	/**
	 * Returns the SQL command to change the specified column's default value ALTER TABLE EMPLOYEE ALTER COLUMN
	 * WORKDEPTSET SET DEFAULT '123'
	 * 
	 * @param info
	 *           the column to modify and it's default value.
	 * @return SQL to make the change
	 */
	public String getColumnDefaultAlterSQL(TableColumnInfo info)
	{
		String alterClause = DialectUtils.ALTER_COLUMN_CLAUSE;
		String defaultClause = DialectUtils.SET_DEFAULT_CLAUSE;
		return DialectUtils.getColumnDefaultAlterSQL(this, info, alterClause, false, defaultClause);
	}

	/**
	 * Returns the SQL command to drop the specified table's primary key.
	 * 
	 * @param pkName
	 *           the name of the primary key that should be dropped
	 * @param tableName
	 *           the name of the table whose primary key should be dropped
	 * @return
	 */
	public String getDropPrimaryKeySQL(String pkName, String tableName)
	{
		return DialectUtils.getDropPrimaryKeySQL(pkName, tableName, false, false);
	}

	/**
	 * Returns the SQL command to drop the specified table's foreign key constraint.
	 * 
	 * @param fkName
	 *           the name of the foreign key that should be dropped
	 * @param tableName
	 *           the name of the table whose foreign key should be dropped
	 * @return
	 */
	public String getDropForeignKeySQL(String fkName, String tableName)
	{
		return DialectUtils.getDropForeignKeySQL(fkName, tableName);
	}

	/**
	 * Returns the SQL command to create the specified table.
	 * 
	 * @param tables
	 *           the tables to get create statements for
	 * @param md
	 *           the metadata from the ISession
	 * @param prefs
	 *           preferences about how the resultant SQL commands should be formed.
	 * @param isJdbcOdbc
	 *           whether or not the connection is via JDBC-ODBC bridge.
	 * @return the SQL that is used to create the specified table
	 */
	public List<String> getCreateTableSQL(List<ITableInfo> tables, ISQLDatabaseMetaData md,
		CreateScriptPreferences prefs, boolean isJdbcOdbc) throws SQLException
	{
		return DialectUtils.getCreateTableSQL(tables, md, this, prefs, isJdbcOdbc);
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getDialectType()
	 */
	public DialectType getDialectType()
	{
		return DialectType.DB2;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getAccessMethodsTypes()
	 */
	public String[] getAccessMethodsTypes()
	{
		return new String[] { };
	}

	public String[] getAddAutoIncrementSQL(TableColumnInfo column, SqlGenerationPreferences prefs)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String[] getAddForeignKeyConstraintSQL(String localTableName, String refTableName,
		String constraintName, Boolean deferrable, Boolean initiallyDeferred, Boolean matchFull,
		boolean autoFKIndex, String fkIndexName, Collection<String[]> localRefColumns, String onUpdateAction,
		String onDeleteAction, DatabaseObjectQualifier qualifier, SqlGenerationPreferences prefs)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getAddUniqueConstraintSQL(java.lang.String, java.lang.String, TableColumnInfo[], net.sourceforge.squirrel_sql.fw.dialects.DatabaseObjectQualifier, net.sourceforge.squirrel_sql.fw.dialects.SqlGenerationPreferences)
	 */
	public String[] getAddUniqueConstraintSQL(String tableName, String constraintName, TableColumnInfo[] columns,
		DatabaseObjectQualifier qualifier, SqlGenerationPreferences prefs)
	{
		ArrayList<String> result = new ArrayList<String>();
		
		ArrayList<String> columnNotNullAlters = new ArrayList<String>();
		// DB2 requires that columns be not-null before applying a unique constraint
		for (TableColumnInfo column : columns) {
			if (column.isNullable().equalsIgnoreCase("YES")) {
				columnNotNullAlters.add(getColumnNullableAlterSQL(column, false, qualifier, prefs));
			}
		}
		if (columnNotNullAlters.size() > 0) {
			//set INTEGRITY FOR <tablename> IMMEDIATE CHECKED
			StringBuilder integritySql = new StringBuilder();
			integritySql.append("SET INTEGRITY FOR ");
			integritySql.append(DialectUtils.shapeQualifiableIdentifier(columns[0].getTableName(), qualifier, prefs, this));
			integritySql.append(" OFF");
			result.add(integritySql.toString());
			
			result.addAll(columnNotNullAlters);
			
			//set INTEGRITY FOR <tablename> IMMEDIATE CHECKED
			integritySql = new StringBuilder();
			integritySql.append("SET INTEGRITY FOR ");
			integritySql.append(DialectUtils.shapeQualifiableIdentifier(columns[0].getTableName(), qualifier, prefs, this));
			integritySql.append(" IMMEDIATE CHECKED");
			result.add(integritySql.toString());
		}
		
		result.add(
			DialectUtils.getAddUniqueConstraintSQL(tableName,
			constraintName,
			columns,
			qualifier,
			prefs,
			this));
		
		return result.toArray(new String[result.size()]);
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getAlterSequenceSQL(java.lang.String,
	 *      java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean,
	 *      net.sourceforge.squirrel_sql.fw.dialects.DatabaseObjectQualifier,
	 *      net.sourceforge.squirrel_sql.fw.dialects.SqlGenerationPreferences)
	 */
	public String[] getAlterSequenceSQL(String sequenceName, String increment, String minimum, String maximum,
		String restart, String cache, boolean cycle, DatabaseObjectQualifier qualifier,
		SqlGenerationPreferences prefs)
	{
		String cycleClause = NO_CYCLE_CLAUSE;
		if (cycle == true)
		{
			cycleClause = CYCLE_CLAUSE;
		}
		return new String[] {

		DialectUtils.getAlterSequenceSQL(sequenceName,
			increment,
			minimum,
			maximum,
			restart,
			cache,
			cycleClause,
			qualifier,
			prefs,
			this) };
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getCreateIndexSQL(java.lang.String,
	 *      java.lang.String, java.lang.String, java.lang.String[], boolean, java.lang.String,
	 *      java.lang.String, net.sourceforge.squirrel_sql.fw.dialects.DatabaseObjectQualifier,
	 *      net.sourceforge.squirrel_sql.fw.dialects.SqlGenerationPreferences)
	 */
	public String getCreateIndexSQL(String indexName, String tableName, String accessMethod, String[] columns,
		boolean unique, String tablespace, String constraints, DatabaseObjectQualifier qualifier,
		SqlGenerationPreferences prefs)
	{
		StringBuilder result = new StringBuilder();
		result.append("CREATE ");

		if (unique)
		{
			result.append("UNIQUE ");
		}
		result.append(" INDEX ");
		result.append(DialectUtils.shapeQualifiableIdentifier(indexName, qualifier, prefs, this));
		result.append(" ON ");
		result.append(DialectUtils.shapeQualifiableIdentifier(tableName, qualifier, prefs, this));
		result.append("(");
		for (String column : columns)
		{
			result.append(column);
			result.append(",");
		}
		result.setLength(result.length() - 1);
		result.append(")");
		return result.toString();

	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getCreateSequenceSQL(java.lang.String,
	 *      java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean,
	 *      net.sourceforge.squirrel_sql.fw.dialects.DatabaseObjectQualifier,
	 *      net.sourceforge.squirrel_sql.fw.dialects.SqlGenerationPreferences)
	 */
	public String getCreateSequenceSQL(String sequenceName, String increment, String minimum, String maximum,
		String start, String cache, boolean cycle, DatabaseObjectQualifier qualifier,
		SqlGenerationPreferences prefs)
	{
		return DialectUtils.getCreateSequenceSQL(sequenceName,
			increment,
			minimum,
			maximum,
			start,
			cache,
			null,
			qualifier,
			prefs,
			this);
	}

	public String getCreateTableSQL(String tableName, List<TableColumnInfo> columns,
		List<TableColumnInfo> primaryKeys, SqlGenerationPreferences prefs, DatabaseObjectQualifier qualifier)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getCreateViewSQL(java.lang.String,
	 *      java.lang.String, java.lang.String,
	 *      net.sourceforge.squirrel_sql.fw.dialects.DatabaseObjectQualifier,
	 *      net.sourceforge.squirrel_sql.fw.dialects.SqlGenerationPreferences)
	 */
	public String getCreateViewSQL(String viewName, String definition, String checkOption,
		DatabaseObjectQualifier qualifier, SqlGenerationPreferences prefs)
	{
		return DialectUtils.getCreateViewSQL(viewName, definition, checkOption, qualifier, prefs, this);
	}

	public String getDropConstraintSQL(String tableName, String constraintName,
		DatabaseObjectQualifier qualifier, SqlGenerationPreferences prefs)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getDropIndexSQL(java.lang.String,
	 *      boolean, net.sourceforge.squirrel_sql.fw.dialects.DatabaseObjectQualifier,
	 *      net.sourceforge.squirrel_sql.fw.dialects.SqlGenerationPreferences)
	 */
	public String getDropIndexSQL(String indexName, boolean cascade, DatabaseObjectQualifier qualifier,
		SqlGenerationPreferences prefs)
	{
		Boolean cascadeNotSupported = null;
		return DialectUtils.getDropIndexSQL(indexName, cascadeNotSupported, qualifier, prefs, this);
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getDropSequenceSQL(java.lang.String,
	 *      boolean, net.sourceforge.squirrel_sql.fw.dialects.DatabaseObjectQualifier,
	 *      net.sourceforge.squirrel_sql.fw.dialects.SqlGenerationPreferences)
	 */
	public String getDropSequenceSQL(String sequenceName, boolean cascade, DatabaseObjectQualifier qualifier,
		SqlGenerationPreferences prefs)
	{
		return DialectUtils.getDropSequenceSQL(sequenceName, false, qualifier, prefs, this);
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getDropViewSQL(java.lang.String, boolean,
	 *      net.sourceforge.squirrel_sql.fw.dialects.DatabaseObjectQualifier,
	 *      net.sourceforge.squirrel_sql.fw.dialects.SqlGenerationPreferences)
	 */
	public String getDropViewSQL(String viewName, boolean cascade, DatabaseObjectQualifier qualifier,
		SqlGenerationPreferences prefs)
	{
		Boolean cascadeNotSupported = null;

		return DialectUtils.getDropViewSQL(viewName, cascadeNotSupported, qualifier, prefs, this);
	}

	public String getInsertIntoSQL(String tableName, List<String> columns, String valuesPart,
		DatabaseObjectQualifier qualifier, SqlGenerationPreferences prefs)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getRenameTableSQL(java.lang.String,
	 *      java.lang.String, net.sourceforge.squirrel_sql.fw.dialects.DatabaseObjectQualifier,
	 *      net.sourceforge.squirrel_sql.fw.dialects.SqlGenerationPreferences)
	 */
	public String getRenameTableSQL(String oldTableName, String newTableName,
		DatabaseObjectQualifier qualifier, SqlGenerationPreferences prefs)
	{
		// RENAME TABLE <tablename> TO <newtablename>;
		StringBuilder sql = new StringBuilder();

		sql.append("RENAME TABLE ");
		sql.append(DialectUtils.shapeQualifiableIdentifier(oldTableName, qualifier, prefs, this));
		sql.append(" ");
		sql.append(" TO ");
		sql.append(DialectUtils.shapeIdentifier(newTableName, prefs, this));

		return sql.toString();
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getRenameViewSQL(java.lang.String,
	 *      java.lang.String, net.sourceforge.squirrel_sql.fw.dialects.DatabaseObjectQualifier,
	 *      net.sourceforge.squirrel_sql.fw.dialects.SqlGenerationPreferences)
	 */
	public String getRenameViewSQL(String oldViewName, String newViewName, DatabaseObjectQualifier qualifier,
		SqlGenerationPreferences prefs)
	{
		throw new UnsupportedOperationException("DB2 doesn't support renaming views");
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#getSequenceInformationSQL(java.lang.String,
	 *      net.sourceforge.squirrel_sql.fw.dialects.DatabaseObjectQualifier,
	 *      net.sourceforge.squirrel_sql.fw.dialects.SqlGenerationPreferences)
	 */
	public String getSequenceInformationSQL(String sequenceName, DatabaseObjectQualifier qualifier,
		SqlGenerationPreferences prefs)
	{
		// SELECT
		// SEQSCHEMA,SEQNAME,DEFINER,DEFINERTYPE,OWNER,OWNERTYPE,SEQID,SEQTYPE,INCREMENT,START,MAXVALUE,MINVALUE,
		// NEXTCACHEFIRSTVALUE,CYCLE,CACHE,ORDER,DATATYPEID,SOURCETYPEID,CREATE_TIME,ALTER_TIME,PRECISION,ORIGIN,REMARKS
		// FROM SYSCAT.SEQUENCES
		// WHERE SEQNAME = ?
		// and SEQSCHEMA = <schema>

		StringBuilder result = new StringBuilder();
		result.append("SELECT NEXTCACHEFIRSTVALUE, MAXVALUE, MINVALUE, CACHE, INCREMENT, CYCLE ");
		result.append("FROM SYSCAT.SEQUENCES ");
		result.append("WHERE ");
		if (qualifier.getSchema() != null)
		{
			result.append("SEQSCHEMA = upper(" + qualifier.getSchema() + ") AND ");
		}
		// TODO: figure out why bind variables aren't working
		result.append("SEQNAME = '");
		result.append(sequenceName);
		result.append("'");
		return result.toString();
	}

	public String getUpdateSQL(String tableName, String[] setColumns, String[] setValues, String[] fromTables,
		String[] whereColumns, String[] whereValues, DatabaseObjectQualifier qualifier,
		SqlGenerationPreferences prefs)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#supportsAccessMethods()
	 */
	public boolean supportsAccessMethods()
	{
		return false;
	}

	public boolean supportsAddForeignKeyConstraint()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#supportsAddUniqueConstraint()
	 */
	public boolean supportsAddUniqueConstraint()
	{
		return true;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#supportsAlterSequence()
	 */
	public boolean supportsAlterSequence()
	{
		return true;
	}

	public boolean supportsAutoIncrement()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public boolean supportsCheckOptionsForViews()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#supportsCreateIndex()
	 */
	public boolean supportsCreateIndex()
	{
		return true;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#supportsCreateSequence()
	 */
	public boolean supportsCreateSequence()
	{
		return true;
	}

	public boolean supportsCreateTable()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#supportsCreateView()
	 */
	public boolean supportsCreateView()
	{
		return true;
	}

	public boolean supportsDropConstraint()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#supportsDropIndex()
	 */
	public boolean supportsDropIndex()
	{
		return true;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#supportsDropSequence()
	 */
	public boolean supportsDropSequence()
	{
		return true;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#supportsDropView()
	 */
	public boolean supportsDropView()
	{
		return true;
	}

	public boolean supportsEmptyTables()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public boolean supportsIndexes()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public boolean supportsInsertInto()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public boolean supportsMultipleRowInserts()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#supportsRenameTable()
	 */
	public boolean supportsRenameTable()
	{
		return true;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#supportsRenameView()
	 */
	public boolean supportsRenameView()
	{
		// The following doesn't appear to work on DB2 (V9.5 LUW):
		// RENAME TABLE <viewname> to <newviewname>;
		// I get: The name used for this operation is not a table. SQL Code: -156, SQL State: 42809
		return false;
	}

	public boolean supportsSequence()
	{
		return true;
	}

	public boolean supportsSequenceInformation()
	{
		return true;
	}

	public boolean supportsTablespace()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public boolean supportsUpdate()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see net.sourceforge.squirrel_sql.fw.dialects.HibernateDialect#supportsAddColumn()
	 */
	public boolean supportsAddColumn()
	{
		return true;
	}

}
