package net.sourceforge.squirrel_sql.plugins.hibernate;

import net.sourceforge.squirrel_sql.client.session.ISQLEntryPanel;

public interface EditExtrasExternalService
{
   void quoteSQL(ISQLEntryPanel entryPanel);

   void quoteSQLSb(ISQLEntryPanel entryPanel);

   void unquoteSQL(ISQLEntryPanel entryPanel);
}
