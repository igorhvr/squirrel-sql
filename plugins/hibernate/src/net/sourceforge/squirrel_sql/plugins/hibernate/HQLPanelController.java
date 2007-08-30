package net.sourceforge.squirrel_sql.plugins.hibernate;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.fw.util.ExceptionFormatter;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.Utilities;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class HQLPanelController
{
   private static final StringManager s_stringMgr =
      StringManagerFactory.getStringManager(HQLPanelController.class);

   private static ILogger s_log = LoggerController.createLogger(HQLPanelController.class);

   private IHibernateTabController _hibernateTabController;
   private ISession _sess;
   private HibernateConnection _con;
   private AbstractAction _convertToSQL;
   private HQLEntryPanelManager _hqlEntryPanelManager;

   public HQLPanelController(IHibernateTabController hibernateTabController, ISession sess, HibernatePluginResources resource)
   {
      _hibernateTabController = hibernateTabController;
      _sess = sess;

      _hqlEntryPanelManager = new HQLEntryPanelManager(_sess, resource, hibernateTabController.getHibernateConnectionProvider());

   }

   void initActions()
   {
      _convertToSQL = new AbstractAction()
      {
         public void actionPerformed(ActionEvent e)
         {
            onConvertToSQL();
         }
      };

      // i18n[hibernate.hqlToSqlLong=HQL to SQL]
      _convertToSQL.putValue(AbstractAction.NAME,  s_stringMgr.getString("hibernate.hqlToSqlLong"));

      // i18n[hibernate.hqlToSqlShort=Convert HQL to SQL (ctrl + enter)]
      _convertToSQL.putValue(AbstractAction.SHORT_DESCRIPTION,  s_stringMgr.getString("hibernate.hqlToSqlShort"));

      _convertToSQL.setEnabled(false);

      _hibernateTabController.addToToolbar(_convertToSQL);

      KeyStroke ctrlEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Event.CTRL_MASK);
      _hqlEntryPanelManager.registerKeyboardAction(_convertToSQL, ctrlEnter);
   }


   private void onConvertToSQL()
   {
      try
      {
         if(false == _convertToSQL.isEnabled())
         {
            return;
         }

         String hql = _hqlEntryPanelManager.getEntryPanel().getSQLToBeExecuted();

         if(null != hql && 0 != hql.trim().length())
         {


            ArrayList<String> list = null;

            long begin = System.currentTimeMillis();
            long duration = 0;
            try
            {
               list = _con.generateSQL(hql);
               duration = System.currentTimeMillis() - begin;
            }
            catch (Exception e)
            {
               Throwable t = Utilities.getDeepestThrowable(e);
               ExceptionFormatter formatter = _sess.getExceptionFormatter();
               String message = formatter.format(t);
               _sess.showErrorMessage(message);

               if(_sess.getProperties().getWriteSQLErrorsToLog() ||
                  (-1 == t.getClass().getName().toLowerCase().indexOf("hibernate") && -1 == t.getClass().getName().toLowerCase().indexOf("antlr")))
               {
                  // If this is not a hibernate error we write a log entry
                  s_log.error(t);
               }

               return;
            }

            _hibernateTabController.displaySqls(list);


            // i18n[HQLPanelController.hqlToSqlSuccess=Generated {0} SQL(s) in {1} milliseconds.]
            _sess.getApplication().getMessageHandler().showMessage(s_stringMgr.getString("SQLPanelController.hqlToSqlSuccess",list.size(), duration));

         }
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }


   public void setConnection(HibernateConnection con)
   {
      _con = con;

      if(null == _con)
      {
         _convertToSQL.setEnabled(false);
      }
      else
      {
         _convertToSQL.setEnabled(true);
      }
   }

   public JComponent getComponent()
   {
      return _hqlEntryPanelManager.getComponent();
   }

   public void requestFocus()
   {
      _hqlEntryPanelManager.requestFocus();
   }
}
