package net.sourceforge.squirrel_sql.client.mainframe.action;
/*
 * Copyright (C) 2002 Colin Bell
 * colbell@users.sourceforge.net
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
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import net.sourceforge.squirrel_sql.fw.datasetviewer.DataSetViewerTextFileDestination;
import net.sourceforge.squirrel_sql.fw.datasetviewer.IDataSetViewer;
import net.sourceforge.squirrel_sql.fw.util.ICommand;
import net.sourceforge.squirrel_sql.fw.util.IMessageHandler;
import net.sourceforge.squirrel_sql.fw.util.NullMessageHandler;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.fw.xml.XMLBeanWriter;
import net.sourceforge.squirrel_sql.fw.xml.XMLException;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.plugin.PluginInfo;
import net.sourceforge.squirrel_sql.client.plugin.PluginInfoArrayDataSet;
import net.sourceforge.squirrel_sql.client.plugin.PluginManager;
import net.sourceforge.squirrel_sql.client.preferences.SquirrelPreferences;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.action.DumpSessionCommand;

/**
 * This <CODE>ICommand</CODE> will dump the status of the application.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class DumpApplicationCommand implements ICommand
{
	/** Logger for this class. */
	private final ILogger s_log =
		LoggerController.createLogger(DumpApplicationCommand.class);

	/** Prefix for temp file names. */
	private static final String PREFIX = "dump";

	/** Suffix for temp file names. */
	private static final String SUFFIX = "tmp";

	/** Used to separate lines of data in the dump file. */
	private static String SEP = "===================================================";

	/** Application. */
	private IApplication _app;

	/** File to dump application to. */
	private File _outFile;

	/** Message handler to write status/error info to. */
	private IMessageHandler _msgHandler;

	/**
	 * Ctor.
	 *
	 * @param	app			Application
	 * @param	outFile		File to dump session to.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if a <TT>null</TT> <TT>IApplication</TT> or <TT>File</TT> passed.
	 */
	public DumpApplicationCommand(IApplication app, File outFile,
									IMessageHandler msgHandler)
	{
		super();
		if (app == null)
		{
			throw new IllegalArgumentException("Null IApplication passed");
		}
		if (outFile == null)
		{
			throw new IllegalArgumentException("Null File passed");
		}
		_app = app;
		_outFile = outFile;
		
		// TODO: app should have a msg handler that displays error dialog.
		_msgHandler = msgHandler != null ? msgHandler : NullMessageHandler.getInstance();
	}

	/**
	 * Dump the application.
	 */
	public void execute()
	{
		List files = new ArrayList();
		List titles = new ArrayList();
		synchronized(_app)
		{
			ApplicationStatusBean bean = new ApplicationStatusBean();
			bean.load(_app);
			try
			{
				files.add(createJavaBeanDumpFile(bean));
				titles.add("Application Status Bean");
			}
			catch (Throwable th)
			{
				final String msg = "Error dumping Application Status bean";
				_msgHandler.showMessage(msg);
				_msgHandler.showMessage(th.toString());
				s_log.error(msg, th);
			}
	
			// Dump ActionCollection
//			
//			// Dump drivers
//			try
//			{
//				File tempFile = File.createTempFile(PREFIX, SUFFIX);
//				_app.getDataCache().saveDrivers(tempFile);
//				files.add(tempFile);
//				titles.add("Drivers");
//			}
//			catch (Throwable th)
//			{
//				final String msg = "Error dumping drivers";
//				_msgHandler.showMessage(msg);
//				_msgHandler.showMessage(th.toString());
//				s_log.error(msg, th);
//			}
//			
//			// Dump aliases.
//			try
//			{
//				File tempFile = File.createTempFile(PREFIX, SUFFIX);
//				_app.getDataCache().saveAliases(tempFile);
//				files.add(tempFile);
//				titles.add("Aliases");
//			}
//			catch (Throwable th)
//			{
//				final String msg = "Error dumping drivers";
//				_msgHandler.showMessage(msg);
//				_msgHandler.showMessage(th.toString());
//				s_log.error(msg, th);
//			}
//			
//			// Dump sessions.
//			ISession[] sessions = _app.getActiveSessions();
//			DumpSessionCommand sessionCmd = new DumpSessionCommand();
//			for (int i = 0; i < sessions.length; ++i)
//			{
//				try
//				{
//					File tempFile = File.createTempFile(PREFIX, SUFFIX);
//					sessionCmd.setSession(sessions[i]);
//					sessionCmd.setDumpFile(tempFile);
//					sessionCmd.execute();
//					files.add(tempFile);
//					titles.add("Session Dump: " + sessions[i].getIdentifier());
//				}
//				catch (Throwable th)
//				{
//					final String msg = "Error dumping sessions";
//					_msgHandler.showMessage(msg);
//					_msgHandler.showMessage(th.toString());
//					s_log.error(msg, th);
//				}
//			}
			
			// Dump thread store.
		}			
			
		// Combine the multiple dump files into one file.
		try
		{
			PrintWriter wtr = new PrintWriter(new FileWriter(_outFile));
			try
			{
				wtr.println("SQuirreL SQL Client Application Dump " +
								Calendar.getInstance().getTime());
				for (int i = 0, limit = files.size(); i < limit; ++i)
				{
					wtr.println();
					wtr.println();
					wtr.println(SEP);
					wtr.println(titles.get(i));
					wtr.println(SEP);
					File file = (File)files.get(i);
					BufferedReader rdr = new BufferedReader(new FileReader(file));
					try
					{
						String line = null;
						while((line = rdr.readLine()) != null)
						{
							wtr.println(line);
						}
					}
					finally
					{
						rdr.close();
					}
				}
			}
			finally
			{
				wtr.close();
			}
		}
		catch (IOException ex)
		{
			final String msg = "Error combining temp files into dump file";
			_msgHandler.showMessage(msg);
			_msgHandler.showMessage(ex.toString());
			s_log.error(msg, ex);
		}
	}

	private File createJavaBeanDumpFile(Object obj)
		throws IOException, XMLException
	{
		File tempFile = File.createTempFile(PREFIX, SUFFIX);
		XMLBeanWriter wtr = new XMLBeanWriter(obj);
		wtr.save(tempFile);

		return tempFile;
	}

	public final static class ApplicationStatusBean
	{
		private SquirrelPreferences _prefs;
		private PluginInfo[] _plugins;

		public ApplicationStatusBean()
		{
			super();
		}

		void load(IApplication app)
		{
			_prefs = app.getSquirrelPreferences();
			_plugins = app.getPluginManager().getPluginInformation();
		}

		public SquirrelPreferences getPreferences()
		{
			return _prefs;
		}

		public PluginInfo[] getPluginInfo()
		{
			return _plugins;
		}

		public PluginInfo getPluginInfo(int idx)
			throws ArrayIndexOutOfBoundsException
		{
			return _plugins[idx];
		}
	}

//	public final static class ApplicationStatusBeanBeanInfo
//		extends SimpleBeanInfo
//	{
//		private PropertyDescriptor[] s_descriptors;
//		private final Class CLAZZ = ApplicationStatusBean.class;
//
//		public ApplicationStatusBeanBeanInfo() throws IntrospectionException
//		{
//			super();
//			if (s_descriptors == null)
//			{
//				s_descriptors = new PropertyDescriptor[2];
//				s_descriptors[0] = new PropertyDescriptor(
//						"prefs", CLAZZ,
//						"getPreferences", null);
//				s_descriptors[1] =
//					new IndexedPropertyDescriptor(
//						"plugins", CLAZZ,
//						"getPluginInfo", null,
//						"getPluginInfo", null);
//			}
//		}
//
//		public PropertyDescriptor[] getPropertyDescriptors()
//		{
//			return s_descriptors;
//		}
//	}
}
