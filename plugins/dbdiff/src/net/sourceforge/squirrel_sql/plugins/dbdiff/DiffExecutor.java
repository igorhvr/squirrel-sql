package net.sourceforge.squirrel_sql.plugins.dbdiff;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.squirrel_sql.client.gui.mainframe.MainFrame;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.fw.dialects.DialectFactory;
import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.ISQLConnection;
import net.sourceforge.squirrel_sql.fw.sql.ISQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.plugins.dbdiff.gui.ColumnDiffDialog;
import net.sourceforge.squirrel_sql.plugins.dbdiff.prefs.DBDiffPreferenceBean;
import net.sourceforge.squirrel_sql.plugins.dbdiff.prefs.PreferencesManager;
import net.sourceforge.squirrel_sql.plugins.dbdiff.util.DBUtil;

/**
 * This is the class that performs the table copy using database connections 
 * to two different database schemas.  
 */
public class DiffExecutor extends I18NBaseObject {

    /** the class that provides out session information */
    SessionInfoProvider prov = null;
    
    /** the source session.  This comes from prov */
    ISession sourceSession = null;
    
    /** the destination session.  This comes from prov */
    ISession destSession = null;
    
    /** the thread we do the work in */
    private Thread execThread = null;
    
    /** the user's preferences */
    private static DBDiffPreferenceBean prefs = 
                                            PreferencesManager.getPreferences();    
    
    /** Logger for this class. */
    private final static ILogger log = 
                         LoggerController.createLogger(DiffExecutor.class);
    
    /** Internationalized strings for this class. */
    private static final StringManager s_stringMgr =
        StringManagerFactory.getStringManager(DiffExecutor.class);
    
    /** the CopyTableListeners that have registered with this class */
    private ArrayList<DiffListener> listeners = new ArrayList<DiffListener>();
    
    /** whether or not the user cancelled the copy operation */
    private volatile boolean cancelled = false;    
    
    /** impl that gives us feedback from the user */
    //private UICallbacks pref = null;
    
    /** the start time in millis that the copy operation began */
    private long start = 0;
    
    /** the finish time in millis that the copy operation began */
    private long end = 0;
    
    private List<ColumnDifference> colDifferences = 
        new ArrayList<ColumnDifference>();
    
    /**
     * Constructor.
     * 
     * @param p the provider of information regarding what to copy where.
     */
    public DiffExecutor(SessionInfoProvider p) {
        prov = p;
        sourceSession = prov.getDiffSourceSession();
        destSession = prov.getDiffDestSession();
    }
    
    /**
     * Starts the thread that executes the copy operation.
     */
    public void execute() {
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    _execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        execThread = new Thread(runnable);
        execThread.setName("DBDiff Executor Thread");
        execThread.start();
    }

    /** 
     * Cancels the copy operation.
     */
    public void cancel() {
        cancelled = true;
        execThread.interrupt();        
    }
    
    /**
     * Performs the table copy operation. 
     */
    private void _execute() throws SQLException {
        start = System.currentTimeMillis();
        boolean encounteredException = false;
        ISQLConnection destConn = destSession.getSQLConnection();
        IDatabaseObjectInfo[] sourceObjs = 
            prov.getSourceSelectedDatabaseObjects();
        IDatabaseObjectInfo[] destObjs = 
            prov.getDestSelectedDatabaseObjects();

        if (!sanityCheck(sourceObjs, destObjs)) {
            return;
        }
        /*
        String sourceSchema = sourceObjs[0].getSchemaName();
        String sourceCatalog = sourceObjs[0].getCatalogName();
        String destSchema = destObjs[0].getSimpleName();
        String destCatalog = destObjs[0].getCatalogName();
        */
        
        
        ISQLDatabaseMetaData sourceMetaData = 
            prov.getDiffSourceSession().getMetaData();
        ISQLDatabaseMetaData destMetaData = 
            prov.getDiffDestSession().getMetaData();

        
        
        Map<String, ITableInfo> tableMap1 = getTableMap(sourceMetaData, sourceObjs);
        Map<String, ITableInfo> tableMap2 = getTableMap(destMetaData, destObjs);
         
        Set<String> tableNames = getAllTableNames(tableMap1);
        tableNames.addAll(getAllTableNames(tableMap2));
        
        try {
            TableDiffExecutor diff = new TableDiffExecutor(sourceMetaData,
                                                           destMetaData);
            for (String table : tableNames) {
                if (tableMap1.containsKey(table)) {
                    if (tableMap2.containsKey(table)) {
                        ITableInfo t1 = tableMap1.get(table);
                        ITableInfo t2 = tableMap2.get(table);
                        diff.setTableInfos(t1, t2);
                        diff.execute();
                        List<ColumnDifference> columnDiffs = 
                            diff.getColumnDifferences();
                        if (columnDiffs != null && columnDiffs.size() > 0) {
                            colDifferences.addAll(columnDiffs);
                            for (ColumnDifference colDiff : columnDiffs) {
                                System.out.println(colDiff.toString());
                            }
                        }
                    } else { 
                        // table exists in source db but not dest
                    }
                } else {
                    // table doesn't exist in source db
                }
                    
            }
            MainFrame frame = sourceSession.getApplication().getMainFrame();
            ColumnDiffDialog dialog = new ColumnDiffDialog(frame, false); 
            if (colDifferences != null && colDifferences.size() > 0) {
                dialog.setColumnDifferences(colDifferences);
                dialog.setSession1Label(sourceSession.getAlias().getName());
                dialog.setSession2Label(destSession.getAlias().getName());
                dialog.setVisible(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        if (encounteredException) {
            return;
        }         
        end = System.currentTimeMillis();
    }
    
    private Set<String> getAllTableNames(Map<String, ITableInfo> tables) {
        HashSet<String> result = new HashSet<String>();
        result.addAll(tables.keySet());
        return result;
    }
    
    private Map<String, ITableInfo> getTableMap(ISQLDatabaseMetaData md, 
                                                IDatabaseObjectInfo[] objs) 
        throws SQLException 
    {
        HashMap<String, ITableInfo> result = new HashMap<String, ITableInfo>();
        if (objs[0].getDatabaseObjectType() == DatabaseObjectType.TABLE) {
            for (int i = 0; i < objs.length; i++) {
                IDatabaseObjectInfo info = objs[i];
                result.put(info.getSimpleName(), (ITableInfo)info);
            }
        } else {
            // Assume objs[0] is a schema/catalog
            String catalog = objs[0].getCatalogName();
            String schema = objs[0].getSchemaName();
            md.getTables(catalog, schema, null, new String[] { "TABLE" }, null);
        }
        return result;
    }
    
    /**
     * Returns a list of column differences.
     * 
     * @return Returns null if no diffs exist.
     */
    public List<ColumnDifference> getColumnDifferences() {
        return colDifferences;
    }
    
    /**
     * Registers the specified listener to receive copy events from this class.
     * 
     * @param listener
     */
    public void addListener(DiffListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        listeners.add(listener);
    }
        
    /**
     * Must have the same number of objects to compare in each set, and they 
     * must be the same type of Objects (Schemas or Tables)
     * 
     * @param sourceObjs
     * @param destObjs
     * @return
     */
    private boolean sanityCheck(IDatabaseObjectInfo[] sourceObjs, 
                                IDatabaseObjectInfo[] destObjs) 
    {
        boolean result = true;
        if (sourceObjs.length != destObjs.length) {
            result = false;
        }
        if (sourceObjs[0].getDatabaseObjectType() 
                != destObjs[0].getDatabaseObjectType()) 
        {
            result = false;
        }
        return result;
    }
    
    private int[] getTableCounts() {
        int[] result = null;
        
        ISession sourceSession = prov.getDiffSourceSession();
        IDatabaseObjectInfo[] dbObjs = prov.getSourceSelectedDatabaseObjects();
        if (dbObjs != null) {
            result = new int[dbObjs.length];
            for (int i = 0; i < dbObjs.length; i++) {
                if (false == dbObjs[i] instanceof ITableInfo) {
                    continue;
                }          
                try {
                    ITableInfo ti = (ITableInfo) dbObjs[i];
                    result[i] = 
                        DBUtil.getTableCount(sourceSession,
                                             ti.getCatalogName(),
                                             ti.getSchemaName(),
                                             ti.getSimpleName(),
                                             DialectFactory.SOURCE_TYPE);
                } catch (Exception e) {
                    log.error("",e);
                    result[i] = 0;
                }
            }           
        }
        return result;
    }
    
    /**
     * 
     * @return
     */
    private long getElapsedSeconds() {
        long result = 1;
        double elapsed = end - start;
        if (elapsed > 1000) {
            result = Math.round(elapsed / 1000);
        }
        return result;
    }
        
        
}
