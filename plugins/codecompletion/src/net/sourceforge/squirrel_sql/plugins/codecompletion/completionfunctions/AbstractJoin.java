package net.sourceforge.squirrel_sql.plugins.codecompletion.completionfunctions;

import net.sourceforge.squirrel_sql.plugins.codecompletion.CodeCompletionInfo;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.ExtendedColumnInfo;

import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.DatabaseMetaData;


public abstract class AbstractJoin extends CodeCompletionFunction
{
   private ISession _session;

   public AbstractJoin(ISession session)
   {
      _session = session;
   }


   public CodeCompletionInfo[] getFunctionResults(String functionSting)
   {
      try
      {
         if(false == functionMatches(functionSting))
         {
            return null;
         }

         StringTokenizer st = new StringTokenizer(functionSting, ",");

         if(3 > st.countTokens())
         {
            return null;
         }

         if(false == functionSting.endsWith(","))
         {
            return null;
         }

         st.nextToken(); // remove the function name

         Vector tables = new Vector();
         while(st.hasMoreTokens())
         {
            String table = st.nextToken().trim();
            if(false == _session.getSchemaInfo().isTable(table))
            {
               return null;
            }
            tables.add(table);
         }

         DatabaseMetaData jdbcMetaData = _session.getSQLConnection().getSQLMetaData().getJDBCMetaData();

         String catalog = _session.getSQLConnection().getCatalog();
         ResultSet resSchema = jdbcMetaData.getTables(catalog, null, (String) tables.get(0), new String[]{"TABLE"});
         resSchema.next();
         String schema = resSchema.getString("TABLE_SCHEM");


         Vector completions = new Vector();
         completions.add("");

         for (int i = 1; i < tables.size(); i++)
         {
            Hashtable conditionByFkName = new Hashtable();
            Hashtable colBuffersByFkName = new Hashtable();
            ResultSet res;
            res = jdbcMetaData.getImportedKeys(catalog, schema, (String) tables.get(i-1));
            fillConditionByFkName(res, (String)tables.get(i-1), (String)tables.get(i), conditionByFkName, colBuffersByFkName);

            res = jdbcMetaData.getExportedKeys(catalog, schema, (String) tables.get(i-1));
            fillConditionByFkName(res, (String)tables.get(i-1), (String)tables.get(i), conditionByFkName, colBuffersByFkName);


            Vector twoTableCompletions = new Vector();
            for(Enumeration e=conditionByFkName.keys(); e.hasMoreElements();)
            {
               String fkName = (String) e.nextElement();

               String joinClause = getJoinClause(fkName, (String)tables.get(i-1), (String)tables.get(i), colBuffersByFkName);

               StringBuffer sb = new StringBuffer();
               sb.append(joinClause).append(tables.get(i)).append(" ON ");

               Vector conditions = (Vector) conditionByFkName.get(fkName);
               if(1 == conditions.size())
               {
                  sb.append(conditions.get(0));
               }
               else if(1 < conditions.size())
               {
                  sb.append("(");
                  sb.append(conditions.get(0));
                  for (int j = 1; j < conditions.size(); j++)
                  {
                     sb.append(" AND ").append(conditions.get(j));
                  }
                  sb.append(")");
               }
               sb.append("\n");

               twoTableCompletions.add(sb.toString());
            }

            if(0 == conditionByFkName.size())
            {
               twoTableCompletions.add("INNER JOIN " + tables.get(i) + " ON " + tables.get(i-1) + ". = " + tables.get(i) + ".\n");
            }


            Vector newCompletions = new Vector();

            for (int j = 0; j < completions.size(); j++)
            {
               String begin = (String) completions.get(j);
               for (int k = 0; k < twoTableCompletions.size(); k++)
               {
                  String end = (String) twoTableCompletions.get(k);
                  newCompletions.add(begin + end);
               }
            }
            completions = newCompletions;
         }

         GenericCodeCompletionInfo[] ret = new GenericCodeCompletionInfo[completions.size()];

         for (int i = 0; i < completions.size(); i++)
         {
            ret[i] = new GenericCodeCompletionInfo((String) completions.get(i));
         }

         return ret;

      }
      catch (SQLException e)
      {
         throw new RuntimeException(e);
      }
   }

   protected abstract String getJoinClause(String fkName, String table1, String table2, Hashtable colBuffersByFkName);

   private void fillConditionByFkName(ResultSet res, String table1, String table2, Hashtable conditionByFkName, Hashtable colBuffersByFkName)
      throws SQLException
   {
      while(res.next())
      {
         if
         (
              (res.getString("PKTABLE_NAME").equalsIgnoreCase((String) table2)  && res.getString("FKTABLE_NAME").equalsIgnoreCase((String) table1))
           || (res.getString("FKTABLE_NAME").equalsIgnoreCase((String) table2)  && res.getString("PKTABLE_NAME").equalsIgnoreCase((String) table1))
         )
         {
            String fkName = "" + res.getString("FK_NAME");

            Vector conditions = (Vector) conditionByFkName.get(fkName);
            if(null == conditions)
            {
               conditions = new Vector();
               conditionByFkName.put(fkName, conditions);
            }

            conditions.add(res.getString("PKTABLE_NAME") + "." + res.getString("PKCOLUMN_NAME") + " = " +
                           res.getString("FKTABLE_NAME") + "." + res.getString("FKCOLUMN_NAME"));


            Vector cols = (Vector) colBuffersByFkName.get(fkName);
            if(null == cols)
            {
               cols = new Vector();
               colBuffersByFkName.put(fkName, cols);
            }
            cols.add(new ColBuffer(res.getString("FKTABLE_NAME"), res.getString("FKCOLUMN_NAME")));
         }
      }
   }

   static class ColBuffer
   {
      String tableName;
      String colName;

      public ColBuffer(String tableName, String colName)
      {
         this.tableName = tableName;
         this.colName = colName;
      }
   }
}
