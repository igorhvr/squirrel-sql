package net.sourceforge.squirrel_sql.client.mainframe.action;
/*
 * Copyright (C) 2007 Rob Manning
 * manningr@users.sourceforge.net
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
import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.update.UpdateControllerImpl;
import net.sourceforge.squirrel_sql.client.update.UpdateUtilImpl;
import net.sourceforge.squirrel_sql.client.update.downloader.ArtifactDownloaderFactory;
import net.sourceforge.squirrel_sql.client.update.downloader.ArtifactDownloaderFactoryImpl;
import net.sourceforge.squirrel_sql.fw.util.ICommand;
/**
 * This <CODE>ICommand</CODE> allows the user to check for updates and apply changes to the currently 
 * installed software.
 */
public class UpdateCommand implements ICommand
{
	/** Application API. */
	private IApplication _app;

	/**
	 * Ctor.
	 *
	 * @param	app	 Application API.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a <TT>null</TT> <TT>IApplication</TT> passed.
	 */
	public UpdateCommand(IApplication app)
	{
		super();
		if (app == null)
		{
			throw new IllegalArgumentException("Null IApplication passed");
		}
		_app = app;
	}

	/**
	 * Display the software update dialog
	 */
	public void execute()
	{
	   UpdateControllerImpl updateController = new UpdateControllerImpl(_app);
	   ArtifactDownloaderFactory downloaderFactory = new ArtifactDownloaderFactoryImpl();
	   updateController.setArtifactDownloaderFactory(downloaderFactory);
	   updateController.setUpdateUtil(new UpdateUtilImpl());
	   updateController.showUpdateDialog();
	}
}
