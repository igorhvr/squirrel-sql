package net.sourceforge.squirrel_sql.fw.gui;
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
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
/**
 * This is a statusbar component with a text control for messages.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class StatusBar extends JPanel
{
	/**
	 * Message to display if there is no msg to display. Defaults to a
	 * blank string.
	 */
	private String _msgWhenEmpty = " ";

	/** Label showing the message in the statusbar. */
	private final JLabel _textLbl = new JLabel();

	/** Constraints used to add new controls to this statusbar. */
	private final GridBagConstraints _gbc = new GridBagConstraints();

	private Font _font;

	/**
	 * Default ctor.
	 */
	public StatusBar()
	{
		super(new GridBagLayout());
		createGUI();
	}

	/**
	 * Set the font for controls in this statusbar.
	 *
	 * @param	font	The font to use.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if <TT>null</TT> <TT>Font</TT> passed.
	 */
	public synchronized void setFont(Font font)
	{
		if (font == null)
		{
			throw new IllegalArgumentException("Font == null");
		}
		super.setFont(font);
		_font = font;
		updateSubcomponentsFont(this);
	}

	/**
	 * Set the text to display in the message label.
	 *
	 * @param	text	Text to display in the message label.
	 */
	public synchronized void setText(String text)
	{
		String myText = null;
		if (text != null)
		{
			myText = text.trim();
		}
		if (myText != null && myText.length() > 0)
		{
			_textLbl.setText(myText);
		}
		else
		{
			clearText();
		}
	}

	public synchronized void clearText()
	{
		_textLbl.setText(_msgWhenEmpty);
	}

	public synchronized void setTextWhenEmpty(String value)
	{
		final boolean wasEmpty = _textLbl.getText().equals(_msgWhenEmpty);
		if (value != null && value.length() > 0)
		{
			_msgWhenEmpty = value;
		}
		else
		{
			_msgWhenEmpty = " ";
		}
		if (wasEmpty)
		{
			clearText();
		}
	}

	public synchronized void addJComponent(JComponent comp)
	{
		if (comp == null)
		{
			throw new IllegalArgumentException("JComponent == null");
		}
		comp.setBorder(createComponentBorder());
		if (_font != null)
		{
			comp.setFont(_font);
			updateSubcomponentsFont(comp);
		}
		super.add(comp, _gbc);
	}

	public static Border createComponentBorder()
	{
		return BorderFactory.createCompoundBorder(
			BorderFactory.createBevelBorder(BevelBorder.LOWERED),
			BorderFactory.createEmptyBorder(0, 4, 0, 4));
	}

	private void createGUI()
	{
		clearText();

		// The message area is on the right of the statusbar and takes
		// up all available space.
		_gbc.anchor = GridBagConstraints.WEST;
		_gbc.weightx = 1.0;
		_gbc.fill = GridBagConstraints.HORIZONTAL;
		_gbc.gridy = 0;
		_gbc.gridx = 0;
		addJComponent(_textLbl);

		// Any other components are on the right.
		_gbc.weightx = 0.0;
		_gbc.anchor = GridBagConstraints.CENTER;
		_gbc.gridx = GridBagConstraints.RELATIVE;
	}

	private void updateSubcomponentsFont(Container cont)
	{
		Component[] comps = cont.getComponents();
		for (int i = 0; i < comps.length; ++i)
		{
			comps[i].setFont(_font);
			if (comps[i] instanceof Container)
			{
				updateSubcomponentsFont((Container)comps[i]);
			}
		}
	}

	public static class StatusBarLabel extends JLabel
	{
		public StatusBarLabel()
		{
			super();
		}
	}
}
