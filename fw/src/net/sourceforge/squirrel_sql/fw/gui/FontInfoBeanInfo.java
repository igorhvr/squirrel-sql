package net.sourceforge.squirrel_sql.fw.gui;
/*
 * Copyright (C) 2001 Colin Bell
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
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * This is the <CODE>BeanInfo</CODE> class for <CODE>FontInfo</CODE>.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public final class FontInfoBeanInfo extends SimpleBeanInfo
{

	/**
	 * See http://tinyurl.com/63no6t for discussion of the proper thread-safe way to implement
	 * getPropertyDescriptors().
	 * 
	 * @see java.beans.SimpleBeanInfo#getPropertyDescriptors()
	 */
	@Override
	public PropertyDescriptor[] getPropertyDescriptors()
	{
		try
		{
			PropertyDescriptor[] result = new PropertyDescriptor[4];
			result[0] =
				new PropertyDescriptor(
					FontInfo.IPropertyNames.FAMILY,
					FontInfo.class,
					"getFamily",
					"setFamily");
			result[1] =
				new PropertyDescriptor(
					FontInfo.IPropertyNames.IS_BOLD,
					FontInfo.class,
					"isBold",
					"setIsBold");
			result[2] =
				new PropertyDescriptor(
					FontInfo.IPropertyNames.IS_ITALIC,
					FontInfo.class,
					"isItalic",
					"setIsItalic");
			result[3] =
				new PropertyDescriptor(
					FontInfo.IPropertyNames.SIZE,
					FontInfo.class,
					"getSize",
					"setSize");
			
			return result;
		}
		catch (IntrospectionException e)
		{
			throw new Error(e);
		}
	}
}
