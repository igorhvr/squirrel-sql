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
package net.sourceforge.squirrel_sql.client.update.gui.installer;

import java.io.File;
import java.io.IOException;

import net.sourceforge.squirrel_sql.client.ApplicationArguments;
import net.sourceforge.squirrel_sql.client.SquirrelLoggerFactory;
import net.sourceforge.squirrel_sql.client.util.ApplicationFiles;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * This is a small application that will be launched each time SQuirreL is started to quickly check to see if
 * updates are available to be applied. Since this application uses Spring, it sets up the Spring context.
 */
public class PreLaunchUpdateApplication
{

	private static PreLaunchHelper helper = null;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException
	{
		ApplicationArguments.initialize(args);
		initializeLogger();
		boolean prompt = getPromptMode();
		setupSpringContext();
		helper.installUpdates(prompt);
	}
	
	// Helper methods
	
	private static void setupSpringContext()
	{
		String[] appCtx = new String[] {
			"classpath:net/sourceforge/squirrel_sql/**/*applicationContext.xml"
		};
		ApplicationContext ctx = new ClassPathXmlApplicationContext(appCtx);
		helper = (PreLaunchHelper)ctx.getBean(PreLaunchHelper.class.getName());
	}
	
	private static boolean getPromptMode()
	{
		boolean prompt = false;
		if (Boolean.getBoolean("prompt")) {
			prompt = true;
		}
		return prompt;
	}
	
	private static void initializeLogger() throws IOException
	{
		String logConfigFileName = ApplicationArguments.getInstance().getLoggingConfigFileName();
		if (logConfigFileName != null) {
			PropertyConfigurator.configure(logConfigFileName);
		} else {
			ApplicationFiles appFiles = new ApplicationFiles();
			
			String logMessagePattern = "%-4r [%t] %-5p %c %x - %m%n";
			Layout layout = new PatternLayout(logMessagePattern);
			
			File logsDir = new File(appFiles.getUserSettingsDirectory(), "logs");
			File updateLogFile = new File(logsDir, "updater.log");
			
			FileAppender appender = new FileAppender(layout, updateLogFile.getAbsolutePath());
			
			LoggerController.registerLoggerFactory(new SquirrelLoggerFactory(appender, false));
		}
	}	
	

}
