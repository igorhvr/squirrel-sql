package net.sourceforge.squirrel_sql.client.session;
/*
 * Copyright (C) 2003 Gerd Wagner
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
public class ExtendedTableInfo
{
	private String _tableName;
	private String _tableType;
   private String _catalog;
   private String _schema;

   ExtendedTableInfo(String tableName, String tableType, String catalog, String schema)
	{
		_tableName = tableName;
		_tableType = tableType;
      _catalog = catalog;
      _schema = schema;
   }

	public String getTableName()
	{
		return _tableName;
	}

	public String getTableType()
	{
		return _tableType;
	}

   public String getCatalog()
   {
      return _catalog;
   }

   public String getSchema()
   {
      return _schema;
   }

}