/*
 * KeywordMap.java - Fast keyword->id map
 * Copyright (C) 1998, 1999 Slava Pestov
 * Copyright (C) 1999 Mike Dillon
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA.
 */
package org.gjt.sp.jedit.syntax;

import javax.swing.text.Segment;


/**
 * A <code>KeywordMap</code> is similar to a hashtable in that it maps keys
 * to values. However, the `keys' are Swing segments. This allows lookups of
 * text substrings without the overhead of creating a new string object.
 * <p>
 * This class is used by <code>CTokenMarker</code> to map keywords to ids.
 *
 * @author Slava Pestov, Mike Dillon
 * @version $Id: KeywordMap.java,v 1.2 2002-12-21 00:34:18 colbell Exp $
 */
public class KeywordMap
{
	// protected members
	protected int mapLength;
	private Keyword[] map;
	private boolean ignoreCase;

	/**
 * Creates a new <code>KeywordMap</code>.
 * @param ignoreCase True if keys are case insensitive
 */
	public KeywordMap(boolean ignoreCase)
	{
		this(ignoreCase, 52);
		this.ignoreCase = ignoreCase;
	}

	/**
 * Creates a new <code>KeywordMap</code>.
 * @param ignoreCase True if the keys are case insensitive
 * @param mapLength The number of `buckets' to create.
 * A value of 52 will give good performance for most maps.
 */
	public KeywordMap(boolean ignoreCase, int mapLength)
	{
		this.mapLength = mapLength;
		this.ignoreCase = ignoreCase;
		map = new Keyword[mapLength];
	}

	/**
 * Looks up a key.
 * @param text The text segment
 * @param offset The offset of the substring within the text segment
 * @param length The length of the substring
 */
	public Keyword lookup(Segment text, int offset, int length)
	{
		if (length == 0)
		{
			return null;
		}

		Keyword k = map[getSegmentMapKey(text, offset, length)];

		while (k != null)
		{
			if (length != k.keyword.length)
			{
				k = k.next;

				continue;
			}

			if (SyntaxUtilities.regionMatches(ignoreCase, text, offset,
						k.keyword))
			{
				return k;
			}

			k = k.next;
		}

		return null;
	}

	/**
 * Adds a key-value mapping.
 * @param keyword The key
 * @Param id The value
 */
	public void add(String keyword, byte id)
	{
		int key = getStringMapKey(keyword);
		map[key] = new Keyword(keyword.toCharArray(), id, map[key]);
	}

	/**
 * Returns true if the keyword map is set to be case insensitive,
 * false otherwise.
 */
	public boolean getIgnoreCase()
	{
		return ignoreCase;
	}

	/**
 * Sets if the keyword map should be case insensitive.
 * @param ignoreCase True if the keyword map should be case
 * insensitive, false otherwise
 */
	public void setIgnoreCase(boolean ignoreCase)
	{
		this.ignoreCase = ignoreCase;
	}

	protected int getStringMapKey(String s)
	{
		return (Character.toUpperCase(s.charAt(0)) +
		Character.toUpperCase(s.charAt(s.length() - 1))) % mapLength;
	}

	protected int getSegmentMapKey(Segment s, int off, int len)
	{
		return (Character.toUpperCase(s.array[off]) +
		Character.toUpperCase(s.array[(off + len) - 1])) % mapLength;
	}

	// private members
	class Keyword
	{
		public char[] keyword;
		public byte id;
		public Keyword next;

		public Keyword(char[] keyword, byte id, Keyword next)
		{
			this.keyword = keyword;
			this.id = id;
			this.next = next;
		}
	}
}


/*
 * ChangeLog:
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2000/01/12 03:17:59  bruce
 *
 * Addition of Syntax Colour Highlighting Package to CVS tree.  This is LGPL code used in the Moe Editor to provide syntax highlighting.
 *
 * Revision 1.15  1999/06/05 02:13:22  sp
 * LGPL'd remaining syntax files
 *
 * Revision 1.14  1999/05/01 00:55:11  sp
 * Option pane updates (new, easier API), syntax colorizing updates
 *
 * Revision 1.13  1999/04/19 05:38:20  sp
 * Syntax API changes
 *
 * Revision 1.12  1999/04/07 05:22:46  sp
 * Buffer options bug fix, keyword map API change (get/setIgnoreCase() methods)
 *
 * Revision 1.11  1999/03/17 05:32:52  sp
 * Event system bug fix, history text field updates (but it still doesn't work), code cleanups, lots of banging head against wall
 *
 * Revision 1.10  1999/03/13 08:50:39  sp
 * Syntax colorizing updates and cleanups, general code reorganizations
 *
 * Revision 1.9  1999/03/13 00:09:07  sp
 * Console updates, uncomment removed cos it's too buggy, cvs log tags added
 *
 */
