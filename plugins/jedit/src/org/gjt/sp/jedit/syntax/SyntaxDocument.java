/*
 * SyntaxDocument.java - Interface all colorized documents must implement
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

import java.awt.Color;

import javax.swing.text.Document;


/**
 * The interface a document must implement to be colorizable by the
 * <code>SyntaxEditorKit</code>. It defines two methods, one that returns
 * the <code>TokenMarker</code> that will split a line into a list of
 * tokens, and a method that returns a color array that maps identification
 * tags returned by the token marker into <code>Color</code> objects. The
 * possible token identifiers are defined as static fields in the
 * <code>Token</code> class.<p>
 *
 * @author Slava Pestov
 * @version $Id: SyntaxDocument.java,v 1.2 2002-12-21 00:34:18 colbell Exp $
 *
 * @see org.gjt.sp.jedit.syntax.DefaultSyntaxDocument
 * @see org.gjt.sp.jedit.syntax.SyntaxEditorKit
 * @see org.gjt.sp.jedit.syntax.TokenMarker
 * @see org.gjt.sp.jedit.syntax.Token
 */
public interface SyntaxDocument extends Document
{
	/**
 * Returns the token marker that is to be used to split lines
 * of this document up into tokens. May return null if this
 * document is not to be colorized.
 */
	public TokenMarker getTokenMarker();

	/**
 * Sets the token marker that is to be used to split lines of
 * this document up into tokens. May throw an exception if
 * this is not supported for this type of document.
 * @param tm The new token marker
 */
	public void setTokenMarker(TokenMarker tm);

	/**
 * Returns the syntax styles used to paint colorized text. Entry <i>n</i>
 * will be used to paint tokens with id = <i>n</i>.
 * @see org.gjt.sp.jedit.syntax.Token
 */
	public SyntaxStyle[] getStyles();

	/**
 * Sets the syntax styles used to paint colorized text. Entry <i>n</i>
 * will be used to paint tokens with id = <i>n</i>.
 * @param styles The syntax styles
 * @see org.gjt.sp.jedit.syntax.Token
 */
	public void setStyles(SyntaxStyle[] styles);

	public void setTextAreaDefaults(TextAreaDefaults dfts);

	public TextAreaDefaults getTextAreaDefaults();

	/**
 * Reparses the document, by passing all lines to the token
 * marker. This should be called after the document is first
 * loaded.
 */
	public void tokenizeLines();

	/**
 * Reparses the document, by passing the specified lines to the
 * token marker. This should be called after a large quantity of
 * text is first inserted.
 * @param start The first line to parse
 * @param len The number of lines, after the first one to parse
 */
	public void tokenizeLines(int start, int len);
}


/*
 * ChangeLog:
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2000/01/12 03:17:59  bruce
 *
 * Addition of Syntax Colour Highlighting Package to CVS tree.  This is LGPL code used in the Moe Editor to provide syntax highlighting.
 *
 * Revision 1.6  1999/06/05 00:22:58  sp
 * LGPL'd syntax package
 *
 * Revision 1.5  1999/05/02 00:07:21  sp
 * Syntax system tweaks, console bugfix for Swing 1.1.1
 *
 * Revision 1.4  1999/04/19 05:38:20  sp
 * Syntax API changes
 *
 * Revision 1.3  1999/04/02 00:39:19  sp
 * Fixed console bug, syntax API changes, minor jEdit.java API change
 *
 * Revision 1.2  1999/03/22 04:20:01  sp
 * Syntax colorizing updates
 *
 * Revision 1.1  1999/03/13 09:11:46  sp
 * Syntax code updates, code cleanups
 *
 */
