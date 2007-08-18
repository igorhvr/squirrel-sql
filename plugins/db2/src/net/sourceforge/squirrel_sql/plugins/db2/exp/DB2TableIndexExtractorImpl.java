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
package net.sourceforge.squirrel_sql.plugins.db2.exp;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.expanders.ITableIndexExtractor;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

/**
 * Provides the query and parameter binding behavior for DB2's index catalog.
 *  
 * @author manningr
 */
public class DB2TableIndexExtractorImpl implements ITableIndexExtractor {

    /** Logger for this class */
    private final static ILogger s_log = 
        LoggerController.createLogger(DB2TableIndexExtractorImpl.class);
                
    /** The query that finds the indexes for a given table */
    private static final String query = 
        "select INDNAME from SYSCAT.INDEXES " +
        "where TABSCHEMA = ? " +
        "and TABNAME = ? ";
    
    /** The query that finds the indexes for a given table on OS/400 */
    private static final String OS_400_SQL = 
        "select " +
        "index_name, " +
        "from qsys2.sysindexes " +
        "where table_schema = ? " +
        "and table_name = ? ";        
    
    /** boolean to indicate whether or not this session is OS/400 */
    private boolean isOS400 = false;        
    
    /**
     * Ctor.
     * 
     * @param isOS400 whether or not the session is OS/400
     */
    public DB2TableIndexExtractorImpl(boolean isOS400) {
        this.isOS400 = isOS400;
    }
    
    /**
     * @see net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.expanders.ITableIndexExtractor#bindParamters(java.sql.PreparedStatement, net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo)
     */
    public void bindParamters(PreparedStatement pstmt, IDatabaseObjectInfo dbo)
        throws SQLException 
    {
        if (s_log.isDebugEnabled()) {
            s_log.debug("Binding schema name "+dbo.getSchemaName()+
                        " as first bind value");
            s_log.debug("Binding table name "+dbo.getSimpleName()+
                        " as second bind value");            
        }                        
        pstmt.setString(1, dbo.getSchemaName());
        pstmt.setString(2, dbo.getSimpleName());
    }

    /**
     * @see net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.expanders.ITableIndexExtractor#getTableIndexQuery()
     */
    public String getTableIndexQuery() {
        if (isOS400) {
            return OS_400_SQL;
        }
        return query;
    }

}
