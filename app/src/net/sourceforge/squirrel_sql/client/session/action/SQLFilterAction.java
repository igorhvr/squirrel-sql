/*
 * SQLFilterAction.java
 *
 * Created on March 22, 2003, 10:55 AM
 */

package net.sourceforge.squirrel_sql.client.session.action;

import java.awt.event.ActionEvent;

import net.sourceforge.squirrel_sql.fw.gui.CursorChanger;
import net.sourceforge.squirrel_sql.fw.sql.DatabaseObjectType;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.SquirrelAction;
import net.sourceforge.squirrel_sql.client.plugin.IPlugin;
import net.sourceforge.squirrel_sql.client.session.IClientSession;
/** Copyright (C) 2003 Maury Hammel
 * mjhammel@users.sourceforge.net
 *
 * Adapted from SessionPropertiesAction.java by Colin Bell.
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
 * @author Maury Hammel
 *
 */
public class SQLFilterAction extends SquirrelAction implements IClientSessionAction
{
  /** The SQuirreL session instance for which this SQL Filter Action applies. */  
	private IClientSession _session;
  /** The SQuirreL application instance. */  
  private IApplication _app;
	
	/** Creates a new instance of SQLFilterAction
   * @param app A reference to the SQuirreL application instance
   */
	public SQLFilterAction(IApplication app)
	{
		super(app);
    _app = app;
	}
	
  /** Sets the _session variable with a reference to the current SQuirrel session
   * instance.
   * @param session A reference to the current SQuirrel session instance.
   */  
	public void setClientSession(IClientSession session)
	{
		_session = session;
	}

	/** Invoked when an action occurs.
   * @param e The event that triggered this procedure.
   */
	public void actionPerformed(ActionEvent e)
	{
		if (_session != null)
		{
      // Ensure that the proper type of Object is selected in the Object Tree before allowing the SQL Filter to be activated.
      
      IDatabaseObjectInfo selectedObjects[] = _session.getObjectTreeAPI(_app.getDummyAppPlugin()).getSelectedDatabaseObjects();
      int objectTotal = selectedObjects.length;
      
      if ((objectTotal > 0) && (selectedObjects[0].getDatabaseObjectType() == DatabaseObjectType.TABLE))
      {
        CursorChanger cursorChg = new CursorChanger(getApplication().getMainFrame());
        cursorChg.show();
        try
        {
          new SQLFilterCommand(_session, selectedObjects[0]).execute();
        }
        finally
        {
          cursorChg.restore();
        }
      }
      else _session.getMessageHandler().showMessage("You must have a single table or view selected to activate the SQL Filter");
		}
	}
}
