package net.sourceforge.squirrel_sql.client.mainframe.action;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.gui.db.IToogleableAliasesList;

import java.awt.event.ActionEvent;

public class CollapseAllAliasFolderAction extends AliasAction
{
   private IToogleableAliasesList _aliasesList;

   public CollapseAllAliasFolderAction(IApplication app, IToogleableAliasesList al)
   {
      super(app);
      _aliasesList = al;
   }

   public void actionPerformed(ActionEvent e)
   {
      _aliasesList.getAliasTreeInterface().collapseAll();
   }
}