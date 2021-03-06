import java.sql.Driver;
import java.sql.DriverManager;

/*
 * Copyright (C) 2010 Rob Manning
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

public class DriverInspector
{
	public static void main (String [] args) throws Exception {
		
		if (args.length != 1) {
			System.err.println("usage: java -cp DriverInspector.jar:vendordriver.jar <driver classname>");
			System.exit(1);
		}
		
		Class<?> driverClass = Class.forName(args[0]);
		Driver d = (Driver)driverClass.newInstance();
		
		System.out.println("Driver class: "+d.getClass().getName());
		System.out.println("Driver Major Version: "+d.getMajorVersion());
		System.out.println("Driver Minor Version: "+d.getMinorVersion());		
	}
}
