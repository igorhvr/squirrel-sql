package net.sourceforge.squirrel_sql.client.session.properties;
/*
 * Copyright (C) 2001-2003 Colin Bell
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
/**
 * @author gwg
 * Object to save, manage and restore lists of specific columns
 * to use in the WHERE clause when doing editing of cells in tables as
 * in the ContentsTab.
 */

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

public class EditWhereCols {
	/**
	 * The singleton instance of this object
	 */
	private static EditWhereCols singleton = null;
	
	/**
	 * The version of the data suitible for loading/unloading from/to files.
	 * The internal representation is a HashMap containing HashMaps containing strings.
	 * The XMLReader/Writer Beans in fw.xml do not handle the general case of HashMaps,
	 * so rather than trying to handle that there, we just convert the data internally into a form
	 * that those classes can handle, i.e. an array of strings.
	 * Each string consists of the name of the table, a space, then a comma-separated list of
	 * all of the columns that SHOULD be used in the WHERE clause when doing editing operations.
	 */
	private String[] dataArray = new String[0];
	
	/**
	 * The mapping from table name to object (also a HashMap)
	 * containing the list of names of columns to use in WHERE clause.
	 */
	private HashMap tables = new HashMap();
	
	/**
	 * Singleton - do not allow external objects to create instances of this
	 */
	private EditWhereCols() {}
	
	/**
	 * let external objects get a copy of this singleton,
	 * creating the instance the first time it is accessed.
	 */
	static public EditWhereCols getInstance() {
		if (singleton == null)
			singleton = new EditWhereCols();
		return singleton;
	}
	
	/**
	 * get the data from the instance
	 */
	public HashMap getTables() {
		return tables;
	}
	
	/**
	 * get data in form that can be used to output to file
	 */
	public String[] getDataArray() {
		// first convert internal data into the string array
		dataArray = new String[tables.size()];
		Iterator keys = tables.keySet().iterator();
		int index = 0;
		
		// get each table's info
		while (keys.hasNext()) {
			String tableName = (String)keys.next();
			HashMap h = (HashMap)tables.get(tableName);
			Iterator columnNames = h.keySet().iterator();
			String outData = tableName + " ";
			while (columnNames.hasNext()) {
				String colName = (String)columnNames.next();
				outData += colName;
				if (columnNames.hasNext())
					outData +=  ",";
			}
			
			// put this into the data array
			dataArray[index++] = outData;
		}

		return dataArray;
	}
	
	/**
	 * Data in the external form (array of strings) is passed in and must be converted
	 * to the internal form.
	 * @param inData array of strings in form "tableName col,col,col..."
	 */
	public void setDataArray(String[] inData) {
		tables = new HashMap();	// make sure we are starting clean
		
		// convert each string into key+HashMap and fill it into the data
		for (int i=0; i< inData.length; i++) {
			int endIndex = inData[i].indexOf(" ");
			String tableName = inData[i].substring(0, endIndex);
			
			int startIndex;
			ArrayList colList = new ArrayList();
			while (true) {
				startIndex = endIndex+1;
				endIndex = inData[i].indexOf(',', startIndex);
				if (endIndex == -1) {
					// we are at the last one in the list
					colList.add(inData[i].substring(startIndex));
					break;
				}
				colList.add(inData[i].substring(startIndex, endIndex));
			}
		}
	}

	/**
	 * add or replace a table-name/hashmap-of-column-names mapping.
	 * If map is null, remove the entry from the tables.
	 */
	public void put(String tableName, HashMap colNames) {
		if (colNames == null)
			tables.remove(tableName);
		else 
			tables.put(tableName, colNames);
		return;
	}
	
	/**
	 * get the HashMap of column names for the given table name.
	 * it will be null if the table does not have any limitation on the columns to use.
	 */
	public HashMap get(String tableName) {
		return (HashMap)tables.get(tableName);
	}
}
