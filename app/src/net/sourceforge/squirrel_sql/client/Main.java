package net.sourceforge.squirrel_sql.client;

import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import javax.swing.*;

/*
 * Copyright (C) 2001-2003 Colin Bell
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
/**
 * Application entry point.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class Main
{
	/**
	 * Default ctor. private as class should never be instantiated.
	 */
	private Main()
	{
		super();
	}

	/**
	 * Application entry point.
	 *
	 * @param	args	Arguments passed on command line.
	 */
	public static void main(String[] args)
	{
		if (ApplicationArguments.initialize(args))
		{

         if(false == Version.supportsUsedJDK())
         {
            JOptionPane.showMessageDialog(null, Version.getUnsupportedJDKMessage());
            System.exit(-1);
         }

			final ApplicationArguments appArgs = ApplicationArguments.getInstance();
			if (appArgs.getShowHelp())
			{
				appArgs.printHelp();
			}
			else
			{
				new Application().startup();
			}
		}
	}
}
