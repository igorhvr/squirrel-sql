package net.sourceforge.squirrel_sql.plugins.sqlscript.table_script;

/*
 * Copyright (C) 2001 Johan Compagner
 * jcompagner@j-com.nl
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sourceforge.squirrel_sql.fw.sql.SQLConnection;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.fw.util.ICommand;
import net.sourceforge.squirrel_sql.plugins.sqlscript.SQLScriptPlugin;
import net.sourceforge.squirrel_sql.plugins.sqlscript.FrameWorkAcessor;

import net.sourceforge.squirrel_sql.client.session.ISession;

public class CreateDataScriptOfCurrentSQLCommand extends CreateDataScriptCommand
{
   /**
    * Current plugin.
    */
   private final SQLScriptPlugin _plugin;

   /**
    * Ctor specifying the current session.
    */
   public CreateDataScriptOfCurrentSQLCommand(ISession session, SQLScriptPlugin plugin)
   {
      super(session, plugin);
      _plugin = plugin;
   }

   /**
    * Execute this command.
    */
   public void execute()
   {
      _session.getApplication().getThreadPool().addTask(new Runnable()
      {
         public void run()
         {
            SQLConnection conn = _session.getSQLConnection();

            //String selectSQL = _session.getSQLPanelAPI(_plugin).getSQLScriptToBeExecuted();
            String selectSQL = FrameWorkAcessor.getSQLPanelAPI(_session, _plugin).getSQLScriptToBeExecuted();

            final StringBuffer sbRows = new StringBuffer(1000);
            try
            {
               final Statement stmt = conn.createStatement();
               try
               {
                  ResultSet srcResult = stmt.executeQuery(selectSQL);
                  ResultSetMetaData metaData = srcResult.getMetaData();
                  String sTable = metaData.getTableName(1);
                  if (sTable == null || sTable.equals(""))
                  {

                     int iFromIndex = getTokenBeginIndex(selectSQL, "from");
                     sTable = getNextToken(selectSQL, iFromIndex + "from".length());
                  }
                  genInserts(srcResult, sTable, sbRows);
               }
               finally
               {
                  try
                  {
                     stmt.close();
                  }
                  catch (Exception e)
                  {
                  }
               }
            }
            catch (Exception e)
            {
               _session.getMessageHandler().showErrorMessage(e);
            }
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  if (sbRows.length() > 0)
                  {
                     //_session.getSQLPanelAPI(_plugin).appendSQLScript(sbRows.toString(), true);
                     FrameWorkAcessor.getSQLPanelAPI(_session, _plugin).appendSQLScript(sbRows.toString(), true);

                     _session.selectMainTab(ISession.IMainPanelTabIndexes.SQL_TAB);
                  }
                  hideAbortFrame();
               }
            });
         }
      });
      showAbortFrame();
   }

   private String getNextToken(String selectSQL, int startPos)
   {
      int curPos = startPos;
      while(curPos < selectSQL.length() && true == Character.isWhitespace(selectSQL.charAt(curPos)))
      {
         // Move over leading whitespaces
         ++curPos;
      }

      int startPosTrimed = curPos;


      while(curPos < selectSQL.length() && false == Character.isWhitespace(selectSQL.charAt(curPos)))
      {
         ++curPos;
      }

      return selectSQL.substring(startPosTrimed, curPos);
   }

   private int getTokenBeginIndex(String selectSQL, String token)
   {
      String lowerSel = selectSQL.toLowerCase();
      String lowerToken = token.toLowerCase().trim();

      int curPos = 0;
      while(-1 != curPos)
      {
         curPos = lowerSel.indexOf(lowerToken);

         if(
                -1 < curPos
             && (0 == curPos || Character.isWhitespace(lowerSel.charAt(curPos-1)))
             && (lowerSel.length() == curPos + lowerToken.length() || Character.isWhitespace(lowerSel.charAt(curPos + lowerToken.length())))
           )
         {
            return curPos;
         }
      }

      return curPos;
   }
}