package net.sourceforge.squirrel_sql.client.session.action;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.action.SquirrelAction;
import net.sourceforge.squirrel_sql.client.session.ISQLPanelAPI;

import java.awt.event.ActionEvent;


public class CloseAllSQLResultTabsButCurrentAction extends SquirrelAction
											implements ISQLPanelAction
{

	/** Current panel. */
	private ISQLPanelAPI _panel;


	/**
	 * Ctor specifying Application API.
	 *
	 * @param	app	Application API.
	 */
	public CloseAllSQLResultTabsButCurrentAction(IApplication app)
	{
		super(app);
	}

	public void setSQLPanel(ISQLPanelAPI panel)
	{
		_panel = panel;
	}

	/**
	 * Display the previous results tab.
	 *
	 * @param	evt		Event being executed.
	 */
	public synchronized void actionPerformed(ActionEvent evt)
	{
		if (_panel != null)
		{
         _panel.closeAllButCurrentResultTabs();
		}
	}
}
