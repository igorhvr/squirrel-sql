package net.sourceforge.squirrel_sql.plugins.editextras.codereformat;
/*
 * Copyright (C) 2003 Gerd Wagner
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
public class StateOfPosition
{
	boolean isTopLevel;

	int commentIndex = -1;
	int literalSepCount = 0;
	int braketDepth = 0;

	public Object clone()
	{
		StateOfPosition ret = new StateOfPosition();
		ret.commentIndex = commentIndex;
		ret.literalSepCount = commentIndex;
		ret.braketDepth = braketDepth;
		ret.isTopLevel = isTopLevel;

		return ret;
	}
}
