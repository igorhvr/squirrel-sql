package net.sourceforge.squirrel_sql.plugins.codecompletion.completionfunctions;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.ExtendedColumnInfo;

import java.util.Hashtable;
import java.util.Vector;


public class LeftJoin extends AbstractJoin
{

   public LeftJoin(ISession session)
   {
      super(session);
   }

   public String getCompareString()
   {
      return "#l";
   }

   public String getCompletionString()
   {
      return "#l,<table1>,<table2>,...<tableN>,";
   }

   public String toString()
   {
      return getCompletionString() + " left join";
   }

   protected String getJoinClause(String fkName, String table1, String table2, Hashtable colBuffersByFkName)
   {
      return "LEFT JOIN ";
   }
}
