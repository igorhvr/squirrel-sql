package net.sourceforge.squirrel_sql.fw.sql;
/*
 * Copyright (C) 2002-2004 Colin Bell
 * colbell@users.sourceforge.net
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
import net.sourceforge.squirrel_sql.fw.id.IHasIdentifier;
import net.sourceforge.squirrel_sql.fw.id.IIdentifier;
import net.sourceforge.squirrel_sql.fw.id.IntegerIdentifierFactory;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
/**
 *
 * Defines the different types of database objects.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class DatabaseObjectType implements IHasIdentifier
{
	/** Internationalized strings for this class. */
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(DatabaseObjectType.class);

	/** Factory to generate unique IDs for these objects. */
	private final static IntegerIdentifierFactory s_idFactory = new IntegerIdentifierFactory();

	/** Other - general purpose. */
	public final static DatabaseObjectType OTHER = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.other"));

	/** Catalog. */
	public final static DatabaseObjectType BEST_ROW_ID = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.bestRowID"));

	/** Catalog. */
	public final static DatabaseObjectType CATALOG = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.catalog"));

	/** Column. */
	public final static DatabaseObjectType COLUMN = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.column"));

	/** Database. */
	public final static DatabaseObjectType SESSION = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.database"));

	/** Standard datatype. */
	public final static DatabaseObjectType DATATYPE = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.datatype"));

	/** Foreign Key relationship. */
	public final static DatabaseObjectType FOREIGN_KEY = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.foreignkey"));

	/** Function. */
	public final static DatabaseObjectType FUNCTION = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.function"));

	/** Index. */
	public final static DatabaseObjectType INDEX = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.index"));

	/** Stored procedure. */
	public final static DatabaseObjectType PROCEDURE = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.storproc"));

	/** Schema. */
	public final static DatabaseObjectType SCHEMA = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.schema"));

	/**
	 * An object that generates uniques IDs for primary keys. E.G. an Oracle
	 * sequence.
	 */
	public final static DatabaseObjectType SEQUENCE = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.sequence"));

	/** TABLE. */
	public final static DatabaseObjectType TABLE = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.table"));

   public static final DatabaseObjectType VIEW = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.view"));;

	/** Trigger. */
	public final static DatabaseObjectType TRIGGER = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.catalog"));

	/** User defined type. */
	public final static DatabaseObjectType UDT = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.udt"));

	/** A database user. */
	public final static DatabaseObjectType USER = createNewDatabaseObjectType(s_stringMgr.getString("DatabaseObjectType.user"));

	/** Uniquely identifies this Object. */
	private final IIdentifier _id;

	/** Describes this object type. */
	private final String _name;

   /**
	 * Default ctor.
	 */
	private DatabaseObjectType(String name)
	{
		super();
		_id = s_idFactory.createIdentifier();
		_name = name != null ? name : _id.toString();
	}

	/**
	 * Return the object that uniquely identifies this object.
	 *
	 * @return	Unique ID.
	 */
	public IIdentifier getIdentifier()
	{
		return _id;
	}

	/**
	 * Retrieve the descriptive name of this object.
	 *
	 * @return	The descriptive name of this object.
	 */
	public String getName()
	{
		return _name;
	}

	public String toString()
	{
		return getName();
	}

	public static DatabaseObjectType createNewDatabaseObjectType(String name)
	{
		return new DatabaseObjectType(name);
	}
}
