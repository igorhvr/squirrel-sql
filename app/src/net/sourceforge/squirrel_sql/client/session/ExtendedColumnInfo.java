/*
 * Copyright (C) 2004 Gerd Wagner
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

package net.sourceforge.squirrel_sql.client.session;

public class ExtendedColumnInfo
{
   private String _columnName;
   private String _columnType;
   private int _columnSize;
   private int _decimalDigits;
   private boolean _nullable;

   public ExtendedColumnInfo(String columnName, String columnType, int columnSize, int decimalDigits, boolean nullable)
   {
      _columnName = columnName;
      _columnType = columnType;
      _columnSize = columnSize;
      _decimalDigits = decimalDigits;
      _nullable = nullable;
   }

   public String getColumnName()
   {
      return _columnName;
   }

   public String getColumnType()
   {
      return _columnType;
   }

   public int getColumnSize()
   {
      return _columnSize;
   }

   public int getDecimalDigits()
   {
      return _decimalDigits;
   }

   public boolean isNullable()
   {
      return _nullable;
   }

}
