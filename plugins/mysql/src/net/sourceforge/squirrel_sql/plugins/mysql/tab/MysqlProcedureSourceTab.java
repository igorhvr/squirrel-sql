package net.sourceforge.squirrel_sql.plugins.mysql.tab;
/*
 * Copyright (C) 2007 Rob Manning
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
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.FormattedSourceTab;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
/**
 * This class provides the necessary information to the parent tab to display the source for an MySQL 
 * Stored Procedure.
 */
public class MysqlProcedureSourceTab extends FormattedSourceTab {
    
	/**
	 * Constructor
	 * 
	 * @param hint
	 *        what the user sees on mouse-over tool-tip
	 */
	public MysqlProcedureSourceTab(String hint)
	{
		super(hint);
        super.setCompressWhitespace(false);
	}

	/**
	 * @see net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table.PSFormattedSourceTab#getSqlStatement()
	 */
	@Override
   protected String getSqlStatement()
   {
	   return 
	   "select routine_definition " +
      "from information_schema.ROUTINES " +
      "where ROUTINE_SCHEMA = ? " +
      "and ROUTINE_NAME = ? ";	   
   }

	/**
    * @see net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table.PSFormattedSourceTab#getBindValues()
    */
   @Override
   protected String[] getBindValues()
   {
   	final IDatabaseObjectInfo doi = getDatabaseObjectInfo();
   	return new String[] { doi.getCatalogName(), doi.getSimpleName() };
   }
}
