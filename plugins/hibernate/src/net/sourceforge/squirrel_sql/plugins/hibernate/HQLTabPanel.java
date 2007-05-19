package net.sourceforge.squirrel_sql.plugins.hibernate;

import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.util.prefs.Preferences;

public class HQLTabPanel extends JPanel
{
   private static final StringManager s_stringMgr =
      StringManagerFactory.getStringManager(HQLTabPanel.class);

   private static final String PERF_KEY_HQL_TAB_DIVIDER_LOCATION = "Squirrel.hibernateplugin.hqlTabDivLoc";


   JTextArea txtHQL;
   JTextArea txtSQL;
   JSplitPane split;
   JComboBox cboConfigurations;
   JToggleButton btnConnected;
   private JPanel _toolbar;
   private int _curXOfToolbar;


   public HQLTabPanel()
   {
      setLayout(new GridBagLayout());

      GridBagConstraints gbc;

      gbc = new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0 );
      _toolbar = createToolbar();
      add(_toolbar, gbc);

      txtHQL = new JTextArea()
      {
         protected void paintComponent(Graphics g)
         {
            super.paintComponent(g);
            super.paintBorder(g);
            drawTextAreaName(g, "HQL", txtHQL);
            super.paintChildren(g);
         }
      };
      txtSQL = new JTextArea()
      {
         protected void paintComponent(Graphics g)
         {
            super.paintComponent(g);
            super.paintBorder(g);
            drawTextAreaName(g, "SQL", txtSQL);
            super.paintChildren(g);
         }
      };


      split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(txtHQL), new JScrollPane(txtSQL));
      gbc = new GridBagConstraints(0,1,1,1,1,1,GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0 );
      add(split, gbc);


      SwingUtilities.invokeLater(new Runnable()
      {
         public void run()
         {
            split.setDividerLocation(Preferences.userRoot().getDouble(PERF_KEY_HQL_TAB_DIVIDER_LOCATION, 0.5));
         }
      });

   }

   private void drawTextAreaName(Graphics g, String name, JTextArea txtArea)
   {
      Color oldCol = g.getColor();
      Rectangle2D bounds = g.getFontMetrics().getStringBounds(name, g);

      Dimension size = txtArea.getSize();

      g.setColor(Color.lightGray);

      int x = size.width - bounds.getBounds().width  - 10;
      int y = size.height - bounds.getBounds().height - 20;

      g.drawString(name, x, y);

      g.setColor(oldCol);
   }

   public void closing()
   {
      Preferences.userRoot().putDouble(PERF_KEY_HQL_TAB_DIVIDER_LOCATION, ((double)split.getDividerLocation())/ ((double)split.getHeight()) );
   }


   private JPanel createToolbar()
   {
      JPanel ret = new JPanel();

      ret.setLayout(new GridBagLayout());

      GridBagConstraints gbc;

      _curXOfToolbar = 0;

      // i18n[HQLTabPanel.configuration=Configuration]
      JLabel lblCfg = new JLabel(s_stringMgr.getString("HQLTabPanel.configuration"));
      gbc = new GridBagConstraints(_curXOfToolbar++,0,1,1, 0,0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5,5,5,5), 0,0);
      ret.add(lblCfg, gbc);

      cboConfigurations = new JComboBox();
      gbc = new GridBagConstraints(_curXOfToolbar++,0,1,1, 1,0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5,0,5,5), 0,0);
      ret.add(cboConfigurations, gbc);

      gbc = new GridBagConstraints(_curXOfToolbar++,0,1,1, 0,0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5,5,5,5), 0,0);
      
      btnConnected = new JToggleButton();
      ret.add(btnConnected, gbc);

      return ret;
   }


   public void addToToolbar(JComponent comp)
   {
      GridBagConstraints  gbc = new GridBagConstraints(_curXOfToolbar++,0,1,1, 0,0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5,5,5,5), 0,0);
      _toolbar.add(comp, gbc);

      _toolbar.validate();
   }
}
