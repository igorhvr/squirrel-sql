/*
* DefaultSyntaxDocument.java - Simple implementation of SyntaxDocument
* Copyright (C) 1999 Slava Pestov
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

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;


/**
* A simple implementation of <code>SyntaxDocument</code>. It takes
* care of inserting and deleting lines from the token marker's state.
*
* @author Slava Pestov
* @version $Id: DefaultSyntaxDocument.java,v 1.4 2003-03-05 10:27:54 colbell Exp $
*
* @see org.gjt.sp.jedit.syntax.SyntaxDocument
*/
public class DefaultSyntaxDocument extends PlainDocument
	implements SyntaxDocument
{
	// protected members
	protected TokenMarker tokenMarker;
	protected SyntaxStyle[] styles;
	protected TextAreaDefaults textAreaDefaults;

	/**
 * Creates a new <code>DefaultSyntaxDocument</code> instance.
 */
	public DefaultSyntaxDocument()
	{
		styles = SyntaxUtilities.getDefaultSyntaxStyles();
		addDocumentListener(new DocumentHandler());
	}

	/**
 * Returns the token marker that is to be used to split lines
 * of this document up into tokens. May return null if this
 * document is not to be colorized.
 */
	public TokenMarker getTokenMarker()
	{
		return tokenMarker;
	}

	public void setTextAreaDefaults(TextAreaDefaults dfts)
	{
		textAreaDefaults = dfts;
	}

	public TextAreaDefaults getTextAreaDefaults()
	{
		return textAreaDefaults;
	}

	/**
 * Sets the token marker that is to be used to split lines of
 * this document up into tokens. May throw an exception if
 * this is not supported for this type of document.
 * @param tm The new token marker
 */
	public void setTokenMarker(TokenMarker tm)
	{
		tokenMarker = tm;

		if (tm == null)
		{
			return;
		}

		tokenMarker.insertLines(0, getDefaultRootElement().getElementCount());
		tokenizeLines();
	}

	/**
 * Returns the syntax styles used to paint colorized text. Entry <i>n</i>
 * will be used to paint tokens with id = <i>n</i>.
 * @see org.gjt.sp.jedit.syntax.Token
 */
	public final SyntaxStyle[] getStyles()
	{
		return styles;
	}

	/**
 * Sets the syntax styles used to paint colorized text. Entry <i>n</i>
 * will be used to paint tokens with id = <i>n</i>.
 * @param styles The syntax styles
 * @see org.gjt.sp.jedit.syntax.Token
 */
	public final void setStyles(SyntaxStyle[] styles)
	{
		this.styles = styles;
	}

	/**
 * Reparses the document, by passing all lines to the token
 * marker. This should be called after the document is first
 * loaded.
 */
	public void tokenizeLines()
	{
		tokenizeLines(0, getDefaultRootElement().getElementCount());
	}

	/**
 * Reparses the document, by passing the specified lines to the
 * token marker. This should be called after a large quantity of
 * text is first inserted.
 * @param start The first line to parse
 * @param len The number of lines, after the first one to parse
 */
	public void tokenizeLines(int start, int len)
	{
		if (tokenMarker == null)
		{
			return;
		}

		Segment lineSegment = new Segment();
		Element map = getDefaultRootElement();

		len += start;

		try
		{
			for (int i = start; i < len; i++)
			{
				Element lineElement = map.getElement(i);
				int lineStart = lineElement.getStartOffset();
				getText(lineStart, lineElement.getEndOffset() - lineStart - 1,
					lineSegment);
				tokenMarker.markTokens(lineSegment, i);
			}
		}
		catch (BadLocationException bl) {}
	}

	/**
 * An implementation of <code>DocumentListener</code> that
 * inserts and deletes lines from the token marker's state.
 */
	public class DocumentHandler implements DocumentListener
	{
		public void insertUpdate(DocumentEvent evt)
		{
			if (tokenMarker == null)
			{
				return;
			}

			DocumentEvent.ElementChange ch = evt.getChange(getDefaultRootElement());

			if (ch == null)
			{
				return;
			}

			tokenMarker.insertLines(ch.getIndex() + 1,
				ch.getChildrenAdded().length - ch.getChildrenRemoved().length);
		}

		public void removeUpdate(DocumentEvent evt)
		{
			if (tokenMarker == null)
			{
				return;
			}

			DocumentEvent.ElementChange ch = evt.getChange(getDefaultRootElement());

			if (ch == null)
			{
				return;
			}

			Element[] children = ch.getChildrenRemoved();

			if (children == null)
			{
				return;
			}

			tokenMarker.deleteLines(ch.getIndex() + 1,
				ch.getChildrenRemoved().length - ch.getChildrenAdded().length);
		}

		public void changedUpdate(DocumentEvent evt) {}
	}
}


/*
* ChangeLog:
* $Log: not supported by cvs2svn $
* Revision 1.2  2002/12/21 00:34:18  colbell
* Add syntax styles
*
* Revision 1.2  2000/01/14 03:33:04  mik
* changed syntax colours
*
* Revision 1.1  2000/01/12 03:17:58  bruce
*
* Addition of Syntax Colour Highlighting Package to CVS tree.  This is LGPL code used in the Moe Editor to provide syntax highlighting.
*
* Revision 1.5  1999/06/05 00:22:58  sp
* LGPL'd syntax package
*
* Revision 1.4  1999/05/02 00:07:21  sp
* Syntax system tweaks, console bugfix for Swing 1.1.1
*
* Revision 1.3  1999/04/19 05:38:20  sp
* Syntax API changes
*
* Revision 1.2  1999/03/29 06:30:25  sp
* Documentation updates, fixed bug in DefaultSyntaxDocument, fixed bug in
* goto-line
*
* Revision 1.1  1999/03/22 04:35:48  sp
* Syntax colorizing updates
*
* Revision 1.1  1999/03/13 09:11:46  sp
* Syntax code updates, code cleanups
*
*/
