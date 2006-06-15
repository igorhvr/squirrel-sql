package net.sourceforge.squirrel_sql.client.gui.db;

import java.io.Serializable;

public class SQLAliasSchemaDetailProperties implements Comparable, Serializable
{
   private String _schemaName;
   private int _table ;
   private int _view;
   private int _function;

   public String getSchemaName()
   {
      return _schemaName;
   }

   public void setSchemaName(String schemaName)
   {
      _schemaName = schemaName;
   }


   public int getTable()
   {
      return _table;
   }

   public int getView()
   {
      return _view;
   }

   public int getFunction()
   {
      return _function;
   }

   public void setTable(int id)
   {
      _table = id;
   }

   public void setView(int id)
   {
      _view = id;
   }

   public void setFunction(int id)
   {
      _function = id;
   }

   public int compareTo(Object other)
   {
      SQLAliasSchemaDetailProperties buf = (SQLAliasSchemaDetailProperties) other;
      return _schemaName.compareTo(buf._schemaName);
   }
}
