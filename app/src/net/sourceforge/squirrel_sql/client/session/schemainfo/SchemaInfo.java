package net.sourceforge.squirrel_sql.client.session.schemainfo;
/*
 * Copyright (C) 2001-2003 Colin Bell
 * colbell@users.sourceforge.net
 *
 * Copyright (C) 2001 Johan Compagner
 * jcompagner@j-com.nl
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
import java.sql.DatabaseMetaData;
import java.util.*;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.gui.db.SchemaLoadInfo;
import net.sourceforge.squirrel_sql.client.gui.db.SchemaNameLoadInfo;
import net.sourceforge.squirrel_sql.client.gui.db.SQLAliasSchemaProperties;
import net.sourceforge.squirrel_sql.client.session.event.SessionAdapter;
import net.sourceforge.squirrel_sql.client.session.event.SessionEvent;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.ExtendedColumnInfo;
import net.sourceforge.squirrel_sql.fw.sql.*;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import javax.swing.*;

public class SchemaInfo
{
   public static final int TABLE_EXT_NOT_A_TABLE = 0;
   public static final int TABLE_EXT_COLS_LOADED_IN_THIS_CALL = 1;
   public static final int TABLE_EXT_COLS_LOADED_BEFORE = 2;



   private static final StringManager s_stringMgr =
      StringManagerFactory.getStringManager(SchemaInfo.class);

   private boolean _loading = false;
   private boolean _loaded = false;

   private SQLDatabaseMetaData _dmd;
   ISession _session = null;


   private static final ILogger s_log = LoggerController.createLogger(SchemaInfo.class);
   private SessionAdapter _sessionListener;

   /**
    * The number of load methods
    */
   private static final int LOAD_METHODS_COUNT = 7;

   private static final int MAX_PROGRESS = 100;

   final HashMap _tablesLoadingColsInBackground = new HashMap();


   private SchemaInfoCache _schemaInfoCache;


   private Vector _listeners = new Vector();
   private boolean _inInitialLoad;
   private long _initalLoadBeginTime;
   private boolean _sessionStartupTimeHintShown;

   private boolean _schemasAndCatalogsLoaded;
   private boolean _tablesLoaded;
   private boolean _storedProceduresLoaded;

   public SchemaInfo(IApplication app)
   {
      _sessionListener = new SessionAdapter()
      {
         public void connectionClosedForReconnect(SessionEvent evt)
         {
            if (null != _session && _session.getIdentifier().equals(evt.getSession().getIdentifier()))
            {
               _dmd = null;
            }
         }

         public void reconnected(SessionEvent evt)
         {
            if (null != _session && _session.getIdentifier().equals(evt.getSession().getIdentifier()))
            {
               _dmd = _session.getSQLConnection().getSQLMetaData();
               if (null != _dmd)
               {
                  s_log.info(s_stringMgr.getString("SchemaInfo.SuccessfullyRestoredDatabaseMetaData"));
               }
            }
         }

         public void sessionClosing(SessionEvent evt)
         {
            SchemaInfoCacheSerializer.store(_session, _schemaInfoCache);
         }
      };

      if (app != null)
      {
         app.getSessionManager().addSessionListener(_sessionListener);
      }
   }


   public void initialLoad(ISession session)
   {
      _session = session;

      breathing();
      _schemaInfoCache = SchemaInfoCacheSerializer.load(_session);

      try
      {
         _inInitialLoad = true;
         _initalLoadBeginTime = System.currentTimeMillis();
         privateLoadAll();
      }
      finally
      {
         _inInitialLoad = false;
         _schemaInfoCache.initialLoadDone();
      }
   }


   public void reloadAll()
   {
      _schemaInfoCache.clearAll();
      privateLoadAll();
      fireSchemaInfoUpdate();
   }


   private void privateLoadAll()
   {
      synchronized (this)
      {
         if(_loading)
         {
            return;
         }

         _loading = true;

         _schemasAndCatalogsLoaded = false;
         _tablesLoaded = false;
         _storedProceduresLoaded = false;
      }

      breathing();



      long mstart = System.currentTimeMillis();
      String msg;

      try
      {
         SQLConnection conn = _session.getSQLConnection();
         _dmd = conn.getSQLMetaData();

         _dmd.clearCache();


         int progress = 0;


         progress = loadCatalogs(progress);

         progress = loadSchemas(progress);

         notifySchemasAndCatalogsLoad();


         try
         {
            // i18n[SchemaInfo.loadingKeywords=Loading keywords]
            msg = s_stringMgr.getString("SchemaInfo.loadingKeywords");
            s_log.debug(msg);
            long start = System.currentTimeMillis();

            int beginProgress = getLoadMethodProgress(progress++);
            int endProgress = getLoadMethodProgress(progress);
            setProgress(msg, beginProgress);
            loadKeywords(msg, beginProgress);

            long finish = System.currentTimeMillis();
            s_log.debug("Keywords loaded in " + (finish - start) + " ms");
         }
         catch (Exception ex)
         {
            s_log.error("Error loading keywords", ex);
         }

         try
         {
            // i18n[SchemaInfo.loadingDataTypes=Loading data types]
            msg = s_stringMgr.getString("SchemaInfo.loadingDataTypes");
            s_log.debug(msg);
            long start = System.currentTimeMillis();

            int beginProgress = getLoadMethodProgress(progress++);
            int endProgress = getLoadMethodProgress(progress);
            setProgress(msg, beginProgress);
            loadDataTypes(msg, beginProgress);

            long finish = System.currentTimeMillis();
            s_log.debug("Data types loaded in " + (finish - start) + " ms");
         }
         catch (Exception ex)
         {
            s_log.error("Error loading data types", ex);
         }

         try
         {
            // i18n[SchemaInfo.loadingFunctions=Loading functions]
            msg = s_stringMgr.getString("SchemaInfo.loadingFunctions");
            s_log.debug(msg);

            long start = System.currentTimeMillis();
            int beginProgress = getLoadMethodProgress(progress++);
            int endProgress = getLoadMethodProgress(progress);
            setProgress(msg, beginProgress);
            loadGlobalFunctions(msg, beginProgress);

            long finish = System.currentTimeMillis();
            s_log.debug("Functions loaded in " + (finish - start) + " ms");
         }
         catch (Exception ex)
         {
            s_log.error("Error loading functions", ex);
         }

         progress = loadTables(null, null, null, null, progress);
         notifyTablesLoaded();


         progress = loadStoredProcedures(null, null, null, progress);
         notifyStoredProceduresLoaded();

      }
      finally
      {
         if (_session != null && _session.getSessionSheet() != null)
         {
            _session.getSessionSheet().setStatusBarProgressFinished();
         }

         _loading = false;
         _loaded = true;
      }
      long mfinish = System.currentTimeMillis();
      s_log.debug("SchemaInfo.load took " + (mfinish - mstart) + " ms");
   }

   private void notifyStoredProceduresLoaded()
   {
      synchronized(this)
      {
         _storedProceduresLoaded = true;
         this.notifyAll();
      }
   }

   private void notifyTablesLoaded()
   {
      synchronized(this)
      {
         _tablesLoaded = true;
         this.notifyAll();
      }
   }

   private void notifySchemasAndCatalogsLoad()
   {
      synchronized(this)
      {
         _schemasAndCatalogsLoaded = true;
         this.notifyAll();
      }
   }


   private int loadStoredProcedures(String catalog, String schema, String procNamePattern, int progress)
   {
      String msg;
      try
      {
         // i18n[SchemaInfo.loadingStoredProcedures=Loading stored procedures]
         msg = s_stringMgr.getString("SchemaInfo.loadingStoredProcedures");
         s_log.debug(msg);
         long start = System.currentTimeMillis();

         int beginProgress = getLoadMethodProgress(progress++);
         int endProgress = getLoadMethodProgress(progress);
         setProgress(msg, beginProgress);
         privateLoadStoredProcedures(catalog, schema, procNamePattern, msg, beginProgress);

         long finish = System.currentTimeMillis();
         s_log.debug("stored procedures loaded in " + (finish - start) + " ms");
      }
      catch (Exception ex)
      {
         s_log.error("Error loading stored procedures", ex);
      }
      return progress;
   }

   private int loadTables(String catalog, String schema, String tableNamePattern, String[] types, int progress)
   {
      String msg;
      try
      {
         // i18n[SchemaInfo.loadingTables=Loading tables]
         msg = s_stringMgr.getString("SchemaInfo.loadingTables");
         s_log.debug(msg);
         long start = System.currentTimeMillis();

         int beginProgress = getLoadMethodProgress(progress++);
         setProgress(msg, beginProgress);
         privateLoadTables(catalog, schema, tableNamePattern, types, msg, beginProgress);

         long finish = System.currentTimeMillis();
         s_log.debug("Tables loaded in " + (finish - start) + " ms");
      }
      catch (Exception ex)
      {
         s_log.error("Error loading tables", ex);
      }
      return progress;
   }

   private int loadSchemas(int progress)
   {
      try
      {
         String msg;
         // i18n[SchemaInfo.loadingSchemas=Loading schemas]
         msg = s_stringMgr.getString("SchemaInfo.loadingSchemas");
         s_log.debug(msg);
         long start = System.currentTimeMillis();

         int beginProgress = getLoadMethodProgress(progress++);
         int endProgress = getLoadMethodProgress(progress);
         setProgress(msg, beginProgress);
         privateLoadSchemas(msg, beginProgress, endProgress);

         long finish = System.currentTimeMillis();
         s_log.debug("Schemas loaded in " + (finish - start) + " ms");
      }
      catch (Exception ex)
      {
         s_log.error("Error loading schemas", ex);
      }
      return progress;
   }

   private int loadCatalogs(int progress)
   {
      try
      {
         String msg;
         // i18n[SchemaInfo.loadingCatalogs=Loading catalogs]
         msg = s_stringMgr.getString("SchemaInfo.loadingCatalogs");
         s_log.debug(msg);
         long start = System.currentTimeMillis();

         int beginProgress = getLoadMethodProgress(progress++);
         int endProgress = getLoadMethodProgress(progress);
         setProgress(msg, beginProgress);
         privateLoadCatalogs(msg, beginProgress, endProgress);

         long finish = System.currentTimeMillis();
         s_log.debug("Catalogs loaded in " + (finish - start) + " ms");
      }
      catch (Exception ex)
      {
         s_log.error("Error loading catalogs", ex);
      }
      return progress;
   }

   private int getLoadMethodProgress(int progress)
   {
      return (int)(((double)progress) / ((double)LOAD_METHODS_COUNT) * ((double)MAX_PROGRESS));
   }

   private void setProgress(final String note, final int value)
   {
      breathing();

      if (_session == null || _session.getSessionSheet() == null)
      {
         return;
      }

     _session.getSessionSheet().setStatusBarProgress(note, 0, MAX_PROGRESS, value);

      if(_inInitialLoad
         && false == _sessionStartupTimeHintShown
         && false == _session.getAlias().getSchemaProperties().isCacheSchemaIndependentMetaData()
         && SQLAliasSchemaProperties.GLOBAL_STATE_LOAD_ALL_CACHE_NONE == _session.getAlias().getSchemaProperties().getGlobalState()
         && _session.getApplication().getSquirrelPreferences().getShowSessionStartupTimeHint()
         && System.currentTimeMillis() - _initalLoadBeginTime > 3000)
      {
         _sessionStartupTimeHintShown = true;
         SwingUtilities.invokeLater(new Runnable()
         {
            public void run()
            {
               new SessionStartupTimeHintController(_session);
            }
         });
      }
   }

   /**
    * We found that the UI behaves much nicer at startup if
    * loading schema info interupted for little moments.
    */
   private void breathing()
   {
      synchronized(this)
      {
         try
         {
            wait(50);
         }
         catch (InterruptedException e)
         {
            s_log.info("Interrupted", e);
         }
      }
   }

   private void privateLoadStoredProcedures(String catalog, String schema, String procNamePattern, final String msg, final int beginProgress)
   {
      try
      {

         ProgressCallBack pcb = new ProgressCallBack()
         {
            public void currentlyLoading(String simpleName)
            {
               setProgress(msg + " (" + simpleName + ")", beginProgress);
            }
         };


         SchemaLoadInfo[] schemaLoadInfos = _schemaInfoCache.getMatchingSchemaLoadInfos(schema);

         for (int i = 0; i < schemaLoadInfos.length; i++)
         {
            if(schemaLoadInfos[i].loadProcedures)
            {
               IProcedureInfo[] procedures = _dmd.getProcedures(catalog, schemaLoadInfos[i].schemaName, procNamePattern, pcb);

               for (int j = 0; j < procedures.length; j++)
               {
                  _schemaInfoCache.writeToProcedureCache(procedures[j]);
               }
            }
         }
      }
      catch (Throwable th)
      {
         s_log.error("Failed to load stored procedures", th);
      }

   }

   private void privateLoadCatalogs(String msg, int beginProgress, int endProgress)
   {
      try
      {
         if(false == _schemaInfoCache.loadSchemaIndependentMetaData())
         {
            return;
         }

         _schemaInfoCache.writeCatalogs(_dmd.getCatalogs());
      }
      catch (Throwable th)
      {
         s_log.error("failed to load catalog names", th);
      }
   }

   private void privateLoadSchemas(String msg, int beginProgress, int endProgress)
   {
      try
      {

         _session.getApplication().getSessionManager().clearAllowedSchemaCache(_session);

         SchemaNameLoadInfo schemaNameLoadInfo = _schemaInfoCache.getSchemaNameLoadInfo();

         if(SchemaNameLoadInfo.STATE_DONT_REFERESH_SCHEMA_NAMES == schemaNameLoadInfo.state)
         {
            return;
         }

         String[] schemasToWrite;
         if(SchemaNameLoadInfo.STATE_REFERESH_SCHEMA_NAMES_FROM_DB == schemaNameLoadInfo.state)
         {
            schemasToWrite = _session.getApplication().getSessionManager().getAllowedSchemas(_session);
         }
         else if(SchemaNameLoadInfo.STATE_USES_PROVIDED_SCHEMA_NAMES == schemaNameLoadInfo.state)
         {
            schemasToWrite = schemaNameLoadInfo.schemaNames;
         }
         else
         {
            throw new IllegalArgumentException("Unknown SchemaNameLoadInfo.state = " + schemaNameLoadInfo.state);
         }

         _schemaInfoCache.writeSchemas(schemasToWrite);
      }
      catch (Throwable th)
      {
         s_log.error("failed to load schema names", th);
      }
   }


   public boolean isKeyword(String data)
   {
      return isKeyword(new CaseInsensitiveString(data));
   }

   /**
    * Retrieve whether the passed string is a keyword.
    *
    * @param	keyword		String to check.
    *
    * @return	<TT>true</TT> if a keyword.
    */
   public boolean isKeyword(CaseInsensitiveString data)
   {
      if (!_loading && data != null)
      {
         return _schemaInfoCache.getKeywordsForReadOnly().containsKey(data);
      }
      return false;
   }


   public boolean isDataType(String data)
   {
      return isDataType(new CaseInsensitiveString(data));
   }


   /**
    * Retrieve whether the passed string is a data type.
    *
    * @param	keyword		String to check.
    *
    * @return	<TT>true</TT> if a data type.
    */
   public boolean isDataType(CaseInsensitiveString data)
   {
      if (!_loading && data != null)
      {
         return _schemaInfoCache.getDataTypesForReadOnly().containsKey(data);
      }
      return false;
   }


   public boolean isFunction(String data)
   {
      return isFunction(new CaseInsensitiveString(data));
   }

   /**
    * Retrieve whether the passed string is a function.
    *
    * @param	keyword		String to check.
    *
    * @return	<TT>true</TT> if a function.
    */
   public boolean isFunction(CaseInsensitiveString data)
   {
      if (!_loading && data != null)
      {
         return _schemaInfoCache.getFunctionsForReadOnly().containsKey(data);
      }
      return false;
   }

   public boolean isTable(String data)
   {
      return isTable(new CaseInsensitiveString(data));
   }


   public boolean isTable(CaseInsensitiveString data)
   {
      int tableExtRes = isTableExt(data);
      return TABLE_EXT_COLS_LOADED_IN_THIS_CALL == tableExtRes || TABLE_EXT_COLS_LOADED_BEFORE == tableExtRes;

   }

   /**
    * Retrieve whether the passed string is a table and wether this table's colums where loaded before this call.
    *
    * @param	keyword		String to check.
    *
    * @return	<TT>true</TT> if a table.
    */
   public int isTableExt(CaseInsensitiveString data)
   {
      if (!_loading && data != null)
      {
         if(_schemaInfoCache.getTableNamesForReadOnly().containsKey(data))
         {
            if (loadColumns(data))
            {
               return TABLE_EXT_COLS_LOADED_IN_THIS_CALL;
            }
            else
            {
               return TABLE_EXT_COLS_LOADED_BEFORE;
            }
         }
      }
      return TABLE_EXT_NOT_A_TABLE;
   }


   public boolean isColumn(String data)
   {
      return isColumn(new CaseInsensitiveString(data));
   }


   /**
    * Retrieve whether the passed string is a column.
    *
    * @param	keyword		String to check.
    *
    * @return	<TT>true</TT> if a column.
    */
   public boolean isColumn(CaseInsensitiveString data)
   {
      if (!_loading && data != null)
      {
         return _schemaInfoCache.getExtColumnInfosByColumnNameForReadOnly().containsKey(data);
      }
      return false;
   }

   /**
    * This method returns the case sensitive name of a table as it is stored
    * in the database.
    * The case sensitive name is needed for example if you want to retrieve
    * a table's meta data. Quote from the API doc of DataBaseMetaData.getTables():
    * Parameters:
    * ...
    * tableNamePattern - a table name pattern; must match the table name as it is stored in the database
    *
    *
    * @param data The tables name in arbitrary case.
    * @return the table name as it is stored in the database
    */
   public String getCaseSensitiveTableName(String data)
   {
      if (!_loading && data != null)
      {
         return (String) _schemaInfoCache.getTableNamesForReadOnly().get(new CaseInsensitiveString(data));
      }
      return null;
   }

   public String getCaseSensitiveProcedureName(String data)
   {
      if (!_loading && data != null)
      {
         return (String) _schemaInfoCache.getProcedureNamesForReadOnly().get(new CaseInsensitiveString(data));
      }
      return null;
   }




   private void loadKeywords(String msg, int beginProgress)
   {
      try
      {
         if(false == _schemaInfoCache.loadSchemaIndependentMetaData())
         {
            return;
         }

         setProgress(msg + " (default keywords)", beginProgress);

         Hashtable keywordsBuf = new Hashtable();

         for (int i = 0; i < DefaultKeywords.KEY_WORDS.length; i++)
         {
            String kw = DefaultKeywords.KEY_WORDS[i];
            keywordsBuf.put(new CaseInsensitiveString(kw), kw);
         }


         // Extra keywords that this DBMS supports.
         if (_dmd != null)
         {

            setProgress(msg + " (DB specific keywords)", beginProgress);

            String[] sqlKeywords = _dmd.getSQLKeywords();

            for (int i = 0; i < sqlKeywords.length; i++)
            {
               _schemaInfoCache.getKeywordsForReadOnly().put(new CaseInsensitiveString(sqlKeywords[i]), sqlKeywords[i]);
            }

            String catalogTerm = _dmd.getCatalogTerm();
            if (catalogTerm != null) {
            	keywordsBuf.put(new CaseInsensitiveString(catalogTerm), catalogTerm);
            }

            String schemaTerm = _dmd.getSchemaTerm();
            if (schemaTerm != null) {
            	keywordsBuf.put(new CaseInsensitiveString(schemaTerm), schemaTerm);
            }

            String procedureTerm = _dmd.getProcedureTerm();
            if (procedureTerm != null) {
            	keywordsBuf.put(new CaseInsensitiveString(procedureTerm), procedureTerm);
            }
         }

         _schemaInfoCache.writeKeywords(keywordsBuf);
      }
      catch (Throwable ex)
      {
         s_log.error("Error occured creating keyword collection", ex);
      }
   }

   private void loadDataTypes(String msg, int beginProgress)
   {
      try
      {
         if(false == _schemaInfoCache.loadSchemaIndependentMetaData())
         {
            return;
         }

         Hashtable dataTypesBuf = new Hashtable();

         DataTypeInfo[] infos = _dmd.getDataTypes();
         for (int i = 0; i < infos.length; i++)
         {
            String typeName = infos[i].getSimpleName();
            dataTypesBuf.put(new CaseInsensitiveString(typeName), typeName);

            if(0 == i % 100 )
            {
               setProgress(msg + " (" + typeName + ")", beginProgress);
            }

         }

         _schemaInfoCache.writeDataTypes(dataTypesBuf);
      }
      catch (Throwable ex)
      {
         s_log.error("Error occured creating data types collection", ex);
      }
   }

   private void loadGlobalFunctions(String msg, int beginProgress)
   {
      if(false == _schemaInfoCache.loadSchemaIndependentMetaData())
      {
         return;
      }

      ArrayList buf = new ArrayList();

      try
      {
         setProgress(msg + " (numeric functions)", beginProgress);
         buf.addAll(Arrays.asList(_dmd.getNumericFunctions()));
      }
      catch (Throwable ex)
      {
         s_log.error("Error", ex);
      }

      try
      {
         setProgress(msg + " (string functions)", beginProgress);
         buf.addAll(Arrays.asList((_dmd.getStringFunctions())));
      }
      catch (Throwable ex)
      {
         s_log.error("Error", ex);
      }

      try
      {
         setProgress(msg + " (time/date functions)", beginProgress);
         buf.addAll(Arrays.asList(_dmd.getTimeDateFunctions()));
      }
      catch (Throwable ex)
      {
         s_log.error("Error", ex);
      }

      Hashtable functionsBuf = new Hashtable();
      for (int i = 0; i < buf.size(); i++)
      {
         String func = (String) buf.get(i);
         if (func.length() > 0)
         {
            functionsBuf.put(new CaseInsensitiveString(func) ,func);
         }

      }

      _schemaInfoCache.writeFunctions(functionsBuf);
   }



   public String[] getKeywords()
   {
      return (String[]) _schemaInfoCache.getKeywordsForReadOnly().values().toArray(new String[_schemaInfoCache.getKeywordsForReadOnly().size()]);
   }

   public String[] getDataTypes()
   {
      return (String[]) _schemaInfoCache.getDataTypesForReadOnly().values().toArray(new String[_schemaInfoCache.getDataTypesForReadOnly().size()]);
   }

   public String[] getFunctions()
   {
      return (String[]) _schemaInfoCache.getFunctionsForReadOnly().values().toArray(new String[_schemaInfoCache.getFunctionsForReadOnly().size()]);
   }

   public String[] getTables()
   {
      return (String[]) _schemaInfoCache.getTableNamesForReadOnly().values().toArray(new String[_schemaInfoCache.getTableNamesForReadOnly().size()]);
   }

   public String[] getCatalogs()
   {
      return (String[]) _schemaInfoCache.getCatalogsForReadOnly().toArray(new String[_schemaInfoCache.getCatalogsForReadOnly().size()]);
   }

   public String[] getSchemas()
   {
      return (String[]) _schemaInfoCache.getSchemasForReadOnly().toArray(new String[_schemaInfoCache.getSchemasForReadOnly().size()]);
   }

   public ITableInfo[] getITableInfos()
   {
      return getITableInfos(null, null);
   }

   public ITableInfo[] getITableInfos(String catalog, String schema)
   {
      return getITableInfos(catalog, schema, null);
   }


   public ITableInfo[] getITableInfos(String catalog, String schema, String simpleName)
   {
      return getITableInfos(catalog, schema, simpleName, null);
   }

   public ITableInfo[] getITableInfos(String catalog, String schema, String tableNamePattern, String[] types)
   {
      ArrayList ret = new ArrayList();
      if (null != types)
      {
         // By default null == types we return only cached types
         ITableInfo[] tableInfosForUncachedTypes = getTableInfosForUncachedTypes(catalog, schema, tableNamePattern, types);
         ret.addAll(Arrays.asList(tableInfosForUncachedTypes));
      }

      for(Iterator i=_schemaInfoCache.getITableInfosForReadOnly().keySet().iterator(); i.hasNext();)
      {
         ITableInfo iTableInfo = (ITableInfo) i.next();

         if(null != catalog && false == catalog.equalsIgnoreCase(iTableInfo.getCatalogName()) )
         {
            continue;
         }

         if(null != schema && false == schema.equalsIgnoreCase(iTableInfo.getSchemaName()) )
         {
            continue;
         }

         if(false == SchemaInfoCache.containsType(types, iTableInfo.getType()))
         {
            continue;
         }

         if(null != tableNamePattern && false == tableNamePattern.endsWith("%") && false == iTableInfo.getSimpleName().equals(tableNamePattern))
         {
            continue;
         }

         if(null != tableNamePattern && tableNamePattern.endsWith("%"))
         {
            String tableNameBegin = tableNamePattern.substring(0, tableNamePattern.length() - 1);
            if(false == iTableInfo.getSimpleName().startsWith(tableNameBegin))
            {
               continue;
            }
         }

         ret.add(iTableInfo);
      }

      return (ITableInfo[]) ret.toArray(new ITableInfo[ret.size()]);
   }

   private ITableInfo[] getTableInfosForUncachedTypes(String catalog, String schema, String tableNamePattern, String[] types)
   {
      try
      {
         ArrayList missingTypes = new ArrayList();
         for (int i = 0; i < types.length; i++)
         {
            if(false == _schemaInfoCache.isCachedTableType(types[i]))
            {
               missingTypes.add(types[i]);
            }
         }

         if(0 < missingTypes.size())
         {
            try
            {
               String[] buf = (String[]) missingTypes.toArray(new String[missingTypes.size()]);
               ProgressCallBack pcb = new ProgressCallBack()
               {
                  public void currentlyLoading(String simpleName)
                  {
                     String msg = s_stringMgr.getString("SchemaInfo.loadingTables");
                     setProgress(msg + " (" + simpleName + ")", 1);
                  }
               };
               return _dmd.getTables(catalog, schema, tableNamePattern, buf, pcb);
            }
            finally
            {
               _session.getSessionSheet().setStatusBarProgressFinished();
            }
         }

      }
      catch (SQLException e)
      {
         s_log.error("Error loading uncached tables", e);
      }

      return new ITableInfo[0];
   }



   public IProcedureInfo[] getStoredProceduresInfos(String catalog, String schema)
   {
      return getStoredProceduresInfos(catalog, schema, null);
   }


   public IProcedureInfo[] getStoredProceduresInfos(String catalog, String schema, String procNamePattern)
   {
      ArrayList ret = new ArrayList();

      for (Iterator i = _schemaInfoCache.getIProcedureInfosForReadOnly().keySet().iterator(); i.hasNext();)
      {

         IProcedureInfo iProcInfo = (IProcedureInfo) i.next();
         boolean toAdd = true;
         if (null != catalog && false == catalog.equalsIgnoreCase(iProcInfo.getCatalogName()))
         {
            toAdd = false;
         }

         if (null != schema && false == schema.equalsIgnoreCase(iProcInfo.getSchemaName()))
         {
            toAdd = false;
         }

         if (null != procNamePattern && false == procNamePattern.endsWith("%") && false == iProcInfo.getSimpleName().equals(procNamePattern))
         {
            toAdd = false;
         }

         if (null != procNamePattern && procNamePattern.endsWith("%"))
         {
            String tableNameBegin = procNamePattern.substring(0, procNamePattern.length() - 1);
            if (false == iProcInfo.getSimpleName().startsWith(tableNameBegin))
            {
               toAdd = false;
            }
         }


         if (toAdd)
         {
            ret.add(iProcInfo);
         }
      }

      return (IProcedureInfo[]) ret.toArray(new IProcedureInfo[ret.size()]);
   }


   public boolean isLoaded()
   {
      return _loaded;
   }

   private void privateLoadTables(String catalog,
                                  String schema,
                                  String tableNamePattern,
                                  String[] types,
                                  final String msg,
                                  final int beginProgress)
   {
      try
      {
         ProgressCallBack pcb = new ProgressCallBack()
         {
            public void currentlyLoading(String simpleName)
            {
               setProgress(msg + " (" + simpleName + ")", beginProgress);
            }
         };

         SchemaLoadInfo[] schemaLoadInfos = _schemaInfoCache.getMatchingSchemaLoadInfos(schema, types);

         for (int i = 0; i < schemaLoadInfos.length; i++)
         {
            ITableInfo[] infos = _dmd.getTables(catalog, schemaLoadInfos[i].schemaName, tableNamePattern, schemaLoadInfos[i].tableTypes, pcb);
            for (int j = 0; j < infos.length; j++)
            {
               _schemaInfoCache.writeToTableCache(infos[j]);
            }
         }
      }
      catch (Throwable th)
      {
         s_log.error("failed to load table names", th);
      }
   }

   /**
    *
    * @return true only when the table's columns are loaded within this call.
    */
   private boolean loadColumns(final CaseInsensitiveString tableName)
   {
      try
      {
         if(_schemaInfoCache.getExtendedColumnInfosByTableNameForReadOnly().containsKey(tableName))
         {
            return false;
         }


         if (_session.getProperties().getLoadColumnsInBackground())
         {
            if(_tablesLoadingColsInBackground.containsKey(tableName))
            {
               return false;
            }

            // Note: A CaseInsensitiveString can be a mutable string.
            // In fact it is a mutable string here because this is usually called from
            // within Syntax coloring which uses a mutable string.
            final CaseInsensitiveString imutableString = new CaseInsensitiveString(tableName.toString());
            _tablesLoadingColsInBackground.put(imutableString, imutableString);
            _session.getApplication().getThreadPool().addTask(new Runnable()
            {
               public void run()
               {
                  try
                  {
                     accessDbToLoadColumns(imutableString);
                     _tablesLoadingColsInBackground.remove(imutableString);
                  }
                  catch (SQLException e)
                  {
                     throw new RuntimeException(e);
                  }
               }
            });
         }
         else
         {
            accessDbToLoadColumns(tableName);
         }
      }
      catch (Throwable th)
      {
         s_log.error("failed to load table names", th);
      }

      return true;
   }

   private void accessDbToLoadColumns(CaseInsensitiveString tableName)
      throws SQLException
   {
      if (null == _dmd)
      {
         s_log.warn(s_stringMgr.getString("SchemaInfo.UnableToLoadColumns", tableName));
         return;
      }
      String name = getCaseSensitiveTableName(tableName.toString());
      TableInfo ti =
         new TableInfo(null, null, name, "TABLE", null, _dmd);
      TableColumnInfo[] infos = _dmd.getColumnInfo(ti);
      _schemaInfoCache.writeColumsToCache(infos, tableName);
   }


   public ExtendedColumnInfo[] getExtendedColumnInfos(String tableName)
   {
      return getExtendedColumnInfos(null, null, tableName);
   }

   public ExtendedColumnInfo[] getExtendedColumnInfos(String catalog, String schema, String tableName)
   {
      CaseInsensitiveString cissTableName = new CaseInsensitiveString(tableName);
      loadColumns(cissTableName);
      ArrayList extColInfo = (ArrayList) _schemaInfoCache.getExtendedColumnInfosByTableNameForReadOnly().get(cissTableName);

      if (null == extColInfo)
      {
         return new ExtendedColumnInfo[0];
      }

      if (null == catalog && null == schema)
      {
         return (ExtendedColumnInfo[]) extColInfo.toArray(new ExtendedColumnInfo[extColInfo.size()]);
      }
      else
      {
         ArrayList ret = new ArrayList();

         for (int i = 0; i < extColInfo.size(); i++)
         {
            ExtendedColumnInfo extendedColumnInfo = (ExtendedColumnInfo) extColInfo.get(i);
            boolean toAdd = true;
            if (null != catalog && false == catalog.equalsIgnoreCase(extendedColumnInfo.getCatalog()))
            {
               toAdd = false;
            }

            if (null != schema && false == schema.equalsIgnoreCase(extendedColumnInfo.getSchema()))
            {
               toAdd = false;
            }

            if (toAdd)
            {
               ret.add(extendedColumnInfo);
            }
         }

         return (ExtendedColumnInfo[]) ret.toArray(new ExtendedColumnInfo[ret.size()]);
      }
   }

   public void dispose()
   {
      // The SessionManager is global to SQuirreL.
      // If we don't remove the listeners the
      // Session won't get Garbeage Collected.
      _session.getApplication().getSessionManager().removeSessionListener(_sessionListener);
   }

   public boolean isProcedure(CaseInsensitiveString data)
   {
      return _schemaInfoCache.getProcedureNamesForReadOnly().containsKey(data);
   }

   public void reload(IDatabaseObjectInfo doi)
   {
      boolean doReloadAll = false;

      try
      {
         synchronized (this)
         {
            if(_loading)
            {
               return;
            }
            _loading = true;
            _schemasAndCatalogsLoaded = false;
            _tablesLoaded = false;
            _storedProceduresLoaded = false;

         }


         if (doi instanceof ITableInfo)
         {
            ITableInfo ti = (ITableInfo) doi;
            DatabaseObjectType dot = ti.getDatabaseObjectType();

            String[] types = null;
            if (DatabaseObjectType.TABLE == dot)
            {
               types = new String[]{"TABLE"};
            }
            else if (DatabaseObjectType.VIEW == dot)
            {
               types = new String[]{"VIEW"};
            }

            _schemaInfoCache.clearTables(ti.getCatalogName(), ti.getSchemaName(), ti.getSimpleName(), types);
            loadTables(ti.getCatalogName(), ti.getSchemaName(), ti.getSimpleName(), types, 1);
         }
         else if(doi instanceof IProcedureInfo)
         {
            IProcedureInfo pi = (IProcedureInfo) doi;
            _schemaInfoCache.clearStoredProcedures(pi.getCatalogName(), pi.getSchemaName(), pi.getSimpleName());
            loadStoredProcedures(pi.getCatalogName(), pi.getSchemaName(), pi.getSimpleName(), 1);
         }
         else if(DatabaseObjectType.TABLE_TYPE_DBO == doi.getDatabaseObjectType())
         {
            // load all table types with catalog = doi.getCatalog() and schema = doi.getSchema()
            _schemaInfoCache.clearTables(doi.getCatalogName(), doi.getSchemaName(), null, null);
            loadTables(doi.getCatalogName(), doi.getSchemaName(), null, null, 0);
         }
         else if(DatabaseObjectType.TABLE == doi.getDatabaseObjectType())
         {
            // load tables with catalog = doi.getCatalog() and schema = doi.getSchema()
            _schemaInfoCache.clearTables(doi.getCatalogName(), doi.getSchemaName(), null, new String[]{"TABLE"});
            loadTables(doi.getCatalogName(), doi.getSchemaName(), null, new String[]{"TABLE"}, 1);
         }
         else if(DatabaseObjectType.VIEW == doi.getDatabaseObjectType())
         {
            // load views with catalog = doi.getCatalog() and schema = doi.getSchema()
            _schemaInfoCache.clearTables(doi.getCatalogName(), doi.getSchemaName(), null, new String[]{"VIEW"});
            loadTables(doi.getCatalogName(), doi.getSchemaName(), null, new String[]{"VIEW"}, 1);
         }
         else if(DatabaseObjectType.PROCEDURE == doi.getDatabaseObjectType() || DatabaseObjectType.PROC_TYPE_DBO == doi.getDatabaseObjectType())
         {
            _schemaInfoCache.clearStoredProcedures(doi.getCatalogName(), doi.getSchemaName(), null);
            loadStoredProcedures(doi.getCatalogName(), doi.getSchemaName(), null, 1);
         }
         else if(DatabaseObjectType.SCHEMA == doi.getDatabaseObjectType())
         {
            //int progress = loadSchemas(1);
            // load tables with catalog = null
            _schemaInfoCache.clearTables(null, doi.getSchemaName(), null, null);
            int progress = loadTables(null, doi.getSchemaName(), null, null, 1);

            // load procedures with catalog = null
            _schemaInfoCache.clearStoredProcedures(null, doi.getSchemaName(), null);
            loadStoredProcedures(null, doi.getSchemaName(), null, progress);
         }
         else if(DatabaseObjectType.CATALOG == doi.getDatabaseObjectType())
         {
            //int progress = loadCatalogs(1);
            // load tables with schema = null
            _schemaInfoCache.clearTables(doi.getCatalogName(), null, null, null);
            int progress = loadTables(doi.getCatalogName(), null, null, null, 1);

            // load procedures with schema = null
            _schemaInfoCache.clearStoredProcedures(doi.getCatalogName(), null, null);
            loadStoredProcedures(doi.getCatalogName(), null, null, progress);
         }
         else if(DatabaseObjectType.SESSION == doi.getDatabaseObjectType())
         {
            doReloadAll = true;
         }

         // If called here it is called far to often and restoring selection in the
         // Object tree doesn't work.
         //fireSchemaInfoUpdate();
      }
      finally
      {
         _session.getSessionSheet().setStatusBarProgressFinished();
         _loading = false;
         _schemasAndCatalogsLoaded = true;
         _tablesLoaded = true;
         _storedProceduresLoaded = true;
         notifySchemasAndCatalogsLoad();
         notifyTablesLoaded();
         notifyStoredProceduresLoaded();

         fireSchemaInfoUpdate();


         if(doReloadAll)
         {
            reloadAll();
         }
      }
   }

   public void fireSchemaInfoUpdate()
   {
      SwingUtilities.invokeLater(new Runnable()
      {
         public void run()
         {
            SchemaInfoUpdateListener[] listeners = (SchemaInfoUpdateListener[]) _listeners.toArray(new SchemaInfoUpdateListener[0]);

            for (int i = 0; i < listeners.length; i++)
            {
               listeners[i].schemaInfoUpdated();
            }
         }
      });


   }

   public void addSchemaInfoUpdateListener(SchemaInfoUpdateListener l)
   {
      _listeners.add(l);
   }

   public void removeSchemaInfoUpdateListener(SchemaInfoUpdateListener l)
   {
      _listeners.remove(l);
   }

   public void refershCacheForSimpleTableName(String simpleTableName)
   {
      HashMap caseSensitiveTableNames = new HashMap();

      CaseInsensitiveString caseInsensitiveTableName = new CaseInsensitiveString(simpleTableName);
      String caseSensitiveTableName = (String) _schemaInfoCache.getTableNamesForReadOnly().get(caseInsensitiveTableName);

      caseSensitiveTableNames.put(caseSensitiveTableName, caseSensitiveTableName);

      ////////////////////////////////////////////////////////////////////////
      // Reload  all matching table types
      for(Iterator i=caseSensitiveTableNames.keySet().iterator(); i.hasNext();)
      {
         String buf = (String) i.next();
         TableInfo ti = new TableInfo(null, null, buf, null, null, _dmd);
         reload(ti);
      }
      //
      ////////////////////////////////////////////////////////////////////////
      fireSchemaInfoUpdate();
   }

   public void refreshCacheForSimpleProcedureName(String simpleProcName)
   {
      HashMap caseSensitiveProcNames = new HashMap();

      CaseInsensitiveString caseInsensitiveProcName = new CaseInsensitiveString(simpleProcName);
      String caseSensitiveProcName = (String) _schemaInfoCache.getProcedureNamesForReadOnly().remove(caseInsensitiveProcName);

      caseSensitiveProcNames.put(caseSensitiveProcName, caseSensitiveProcName);

      ////////////////////////////////////////////////////////////////////////
      // Reload  all matching procedure types
      for(Iterator i=caseSensitiveProcNames.keySet().iterator(); i.hasNext();)
      {
         String buf = (String) i.next();
         ProcedureInfo pi = new ProcedureInfo(null, null, buf, null, DatabaseMetaData.procedureResultUnknown, _dmd);
         reload(pi);
      }
      //
      ////////////////////////////////////////////////////////////////////////
      fireSchemaInfoUpdate();
   }

   public void waitTillSchemasAndCatalogsLoaded()
   {
      try
      {
         synchronized (this)
         {
            while (false == _schemasAndCatalogsLoaded)
            {
               this.wait();
            }
         }
      }
      catch (InterruptedException e)
      {
         throw new RuntimeException(e);
      }
   }

   public void waitTillTablesLoaded()
   {
      try
      {
         synchronized (this)
         {
            while (false == _tablesLoaded)
            {
               this.wait();
            }
         }
      }
      catch (InterruptedException e)
      {
         throw new RuntimeException(e);
      }
   }

   public void waitTillStoredProceduresLoaded()
   {
      try
      {
         synchronized (this)
         {
            while (false == _storedProceduresLoaded)
            {
               this.wait();
            }
         }
      }
      catch (InterruptedException e)
      {
         throw new RuntimeException(e);
      }
   }


}
