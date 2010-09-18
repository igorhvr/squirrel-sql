package net.sourceforge.squirrel_sql.plugins.laf;
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
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JComboBox;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
/**
 * This <TT>JComboBox</TT> will display all the Look and Feels
 * that have been registered with the <TT>UIManager</TT>.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class LookAndFeelComboBox extends JComboBox
{
    private static final long serialVersionUID = 1L;

    /**
	 * <TT>LookAndFeelInfo</TT> objects keyed by the
	 * Look and Feel name.
	 */
	private Map<String, LookAndFeelInfo> _lafsByName = 
        new TreeMap<String, LookAndFeelInfo>();

	/**
	 * <TT>LookAndFeelInfo</TT> objects keyed by the
	 * Class name of the Look and Feel.
	 */
	private Map<String, LookAndFeelInfo> _lafsByClassName = 
        new TreeMap<String, LookAndFeelInfo>();

	/**
	 * Default ctor. Select the currently active L & F after
	 * building the combo box.
	 */
	public LookAndFeelComboBox()
	{
		this(null);
	}

	/**
	 * Ctor specifying the L & F to select after building the combo box.
	 *
	 * @param	Name of the L & F to be selected.
	 */
	public LookAndFeelComboBox(String selectedLafName)
	{
		super();
		generateLookAndFeelInfo();
		if (selectedLafName == null)
		{
			selectedLafName = UIManager.getLookAndFeel().getName();
		}
		setSelectedLookAndFeelName(selectedLafName);
	}

	public LookAndFeelInfo getSelectedLookAndFeel()
	{
		return _lafsByName.get(getSelectedItem());
	}

	public void setSelectedLookAndFeelName(String selectedLafName)
	{
		if (selectedLafName != null)
		{
			getModel().setSelectedItem(selectedLafName);
		}
	}

	public void setSelectedLookAndFeelClassName(String selectedLafClassName)
	{
		if (selectedLafClassName != null)
		{
			LookAndFeelInfo info =_lafsByClassName.get(selectedLafClassName);
			if (info != null)
			{
				setSelectedLookAndFeelName(info.getName());
			}
		}
	}

	/**
	 * Fill combo with the names of all the Look and Feels in
	 * alpabetical sequence.
	 */
	private void generateLookAndFeelInfo()
	{
		// Put all available "Look and Feel" objects into collections
		// keyed by LAF name and by the class name.
		LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
		_lafsByName = new TreeMap<String, LookAndFeelInfo>();
		for (int i = 0; i < info.length; ++i)
		{
			_lafsByName.put(info[i].getName(), info[i]);
			_lafsByClassName.put(info[i].getClassName(), info[i]);
		}
		
		// Need to populate the list with the Substance placeholder, to allow the user to select substance, 
		// and then the skin, which is the actual look and feel.
		SubstanceLafPlaceholder substanceLaf = new SubstanceLafPlaceholder();
		LookAndFeelInfo substanceLafInfo = substanceLaf.getLookAndFeelInfo();
		_lafsByName.put(substanceLaf.getName(), substanceLafInfo);
		_lafsByClassName.put(SubstanceLafPlaceholder.class.getName(), substanceLafInfo);

		// Add the names of all LAF objects to control. By doing thru the Map
		// these will be sorted.
		for (Iterator<LookAndFeelInfo> it = _lafsByName.values().iterator(); it.hasNext();)
		{
			addItem(it.next().getName());
		}
	}
}
