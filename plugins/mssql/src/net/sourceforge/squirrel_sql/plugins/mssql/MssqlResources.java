package net.sourceforge.squirrel_sql.plugins.mssql;

/*
 * Copyright (C) 2004 Ryan Walberg <generalpf@yahoo.com>
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

import net.sourceforge.squirrel_sql.client.plugin.IPlugin;
import net.sourceforge.squirrel_sql.client.plugin.PluginResources;

final class MssqlResources extends PluginResources {
	interface IMenuResourceKeys {
		String SHOW_STATISTICS = "show_statistics";
        String INDEXDEFRAG = "indexdefrag";
		String MSSQL = "mssql";
	}

	MssqlResources(String rsrcBundleBaseName, IPlugin plugin) {
		super(rsrcBundleBaseName, plugin);
	}
}
