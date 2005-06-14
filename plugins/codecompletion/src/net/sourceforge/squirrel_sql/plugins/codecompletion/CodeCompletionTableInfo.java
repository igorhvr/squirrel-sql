/*
 * Copyright (C) 2003 Gerd Wagner
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
package net.sourceforge.squirrel_sql.plugins.codecompletion;

import net.sourceforge.squirrel_sql.client.session.ExtendedColumnInfo;
import net.sourceforge.squirrel_sql.client.session.SchemaInfo;

import java.sql.SQLException;
import java.util.Vector;

public class CodeCompletionTableInfo extends CodeCompletionInfo
{
   private String _tableName;
   private String _tableType;
   private CodeCompletionColumnInfo[] _colInfos;
   private String _toString;
   private String _catalog;
   private String _schema;


   public CodeCompletionTableInfo(String tableName, String tableType, String catalog, String schema)
   {
      _tableName = tableName;
      _tableType = tableType;
      _catalog = catalog;
      _schema = schema;


      if(null != _tableType && !"TABLE".equals(_tableType))
      {
         _toString = _tableName  + " (" + _tableType + ")";
      }
      else
      {
         _toString = _tableName;
      }
   }

   void setHasDuplicateNameInDfifferentSchemas()
   {

      String tabNameWithSchemaHint = _tableName + (null == _catalog ? "": " catalog=" + _catalog) + (null == _schema ? "":" schema=" + _schema);

      if(null != _tableType && !"TABLE".equals(_tableType))
      {
         _toString = tabNameWithSchemaHint  + " (" + _tableType + ")";
      }
      else
      {
         _toString = tabNameWithSchemaHint;
      }

   }


   public String getCompareString()
   {
      return _tableName;
   }

   public CodeCompletionInfo[] getColumns(SchemaInfo schemaInfo, String colNamePattern)
      throws SQLException
   {
      if(null == _colInfos)
      {
         ExtendedColumnInfo[] schemColInfos = schemaInfo.getExtendedColumnInfos(_catalog, _schema, _tableName);


         Vector colInfosBuf = new Vector();
         for (int i = 0; i < schemColInfos.length; i++)
         {
            if(   (null == _catalog || ("" + _catalog).equals("" + schemColInfos[i].getCatalog()))
               && (null == _schema || ("" + _schema).equals("" + schemColInfos[i].getSchema()))   )
            {
               String columnName = schemColInfos[i].getColumnName();
               String columnType = schemColInfos[i].getColumnType();
               int columnSize = schemColInfos[i].getColumnSize();
               int decimalDigits = schemColInfos[i].getDecimalDigits();
               boolean nullable = schemColInfos[i].isNullable();
               CodeCompletionColumnInfo buf = new CodeCompletionColumnInfo(columnName, columnType, columnSize, decimalDigits, nullable);
               colInfosBuf.add(buf);
            }
         }

         _colInfos = (CodeCompletionColumnInfo[]) colInfosBuf.toArray(new CodeCompletionColumnInfo[colInfosBuf.size()]);
      }

      String upperCaseColNamePattern = colNamePattern.toUpperCase().trim();

      if("".equals(upperCaseColNamePattern))
      {
         return _colInfos;
      }

      Vector ret = new Vector();
      for(int i=0; i < _colInfos.length; ++i)
      {
         if(_colInfos[i].upperCaseCompletionStringStartsWith(upperCaseColNamePattern))
         {
            ret.add(_colInfos[i]);
         }
      }

      return (CodeCompletionInfo[])ret.toArray(new CodeCompletionInfo[0]);
   }

   public boolean hasColumns()
   {
      return true;
   }


   public String toString()
   {
      return _toString;
   }
}
