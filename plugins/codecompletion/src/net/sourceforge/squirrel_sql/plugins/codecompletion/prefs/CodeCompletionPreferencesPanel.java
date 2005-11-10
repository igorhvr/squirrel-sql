package net.sourceforge.squirrel_sql.plugins.codecompletion.prefs;

import net.sourceforge.squirrel_sql.client.plugin.PluginResources;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.gui.MultipleLineLabel;

import javax.swing.*;
import java.awt.*;

public class CodeCompletionPreferencesPanel extends JPanel
{
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(CodeCompletionPreferencesPanel.class);


	JRadioButton optSPWithParams;
	JRadioButton optSPWithoutParams;
	JRadioButton optUDFWithParams;
	JRadioButton optUDFWithoutParams;

	JTable tblPrefixes;

	JButton btnNewRow;
	JButton btnDeleteRows;



	public CodeCompletionPreferencesPanel()
	{
		setLayout(new GridBagLayout());

		GridBagConstraints gbc;

		gbc = new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5,5,5,5),0,0 );
		// i18n[codeCompletion.prefsExplain=When completing functions SQuirreL doesn't know
		// if a function is a stored procedure or a user defined function.
		// To make code completion of these two kinds of functions convenient SQuirreL offers to
		// configure which way completion should work.]
		add(new MultipleLineLabel(s_stringMgr.getString("codeCompletion.prefsExplain")), gbc);

		gbc = new GridBagConstraints(0,1,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5,5,5,5),0,0 );
		// i18n[codeCompletion.globalFunctCompltion=If there is no matching prefix configuration functions should complete like:]
		add(new JLabel(s_stringMgr.getString("codeCompletion.globalFunctCompltion")),gbc);

		ButtonGroup grp = new ButtonGroup();

		// i18n[codeCompletion.spWithParams=stored procedure with parameter info: {call mySP(<IN INTEGER tid>)}]
		optSPWithParams = new JRadioButton(s_stringMgr.getString("codeCompletion.spWithParams"));
		gbc = new GridBagConstraints(0,2,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0,5,5,5),0,0 );
		add(optSPWithParams,gbc);
		grp.add(optSPWithParams);

		// i18n[codeCompletion.spWithoutParams=stored procedure without parameter info: {call mySP()}]
		optSPWithoutParams = new JRadioButton(s_stringMgr.getString("codeCompletion.spWithoutParams"));
		gbc = new GridBagConstraints(0,3,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0,5,5,5),0,0 );
		add(optSPWithoutParams,gbc);
		grp.add(optSPWithoutParams);

		// i18n[codeCompletion.UDFWithParams=user defined function with parameter info: myFunct(<IN INTEGER tid>)]
		optUDFWithParams = new JRadioButton(s_stringMgr.getString("codeCompletion.UDFWithParams"));
		gbc = new GridBagConstraints(0,4,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0,5,5,5),0,0 );
		add(optUDFWithParams,gbc);
		grp.add(optUDFWithParams);

		// i18n[codeCompletion.UDFWithoutParams=user defined function without parameter info: myFunct()]
		optUDFWithoutParams = new JRadioButton(s_stringMgr.getString("codeCompletion.UDFWithoutParams"));
		gbc = new GridBagConstraints(0,5,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0,5,5,5),0,0 );
		add(optUDFWithoutParams,gbc);
		grp.add(optUDFWithoutParams);


		gbc = new GridBagConstraints(0,6,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10,5,5,5),0,0 );
		// i18n[codeCompletion.prefixConfig=Configure function completion for function name prefixes:]
		add(new JLabel(s_stringMgr.getString("codeCompletion.prefixConfig")), gbc);


		tblPrefixes = new JTable();
		gbc = new GridBagConstraints(0,7,1,1,1,1,GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0,5,5,5),0,0 );
		add(new JScrollPane(tblPrefixes), gbc);


		JPanel pnlButtons = new JPanel(new GridBagLayout());

		// i18n[codeCompletion.prefixConfig.newRow=Add new row]
		btnNewRow = new JButton(s_stringMgr.getString("codeCompletion.prefixConfig.newRow"));
		gbc = new GridBagConstraints(0,0,1,1,0,0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0,0,5,5),0,0);
		pnlButtons.add(btnNewRow, gbc);

		// i18n[codeCompletion.prefixConfig.deleteSelRows=Delete selected rows]
		btnDeleteRows = new JButton(s_stringMgr.getString("codeCompletion.prefixConfig.deleteSelRows"));
		gbc = new GridBagConstraints(1,0,1,1,0,0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0,5,5,5),0,0);
		pnlButtons.add(btnDeleteRows, gbc);

		gbc = new GridBagConstraints(0,8,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0,5,15,5),0,0 );
		add(pnlButtons,gbc);



	}
}
