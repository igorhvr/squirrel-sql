/*
 * Copyright (C) 2008 Rob Manning
 * manningr@users.sourceforge.net
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
package net.sourceforge.squirrel_sql.client.update.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;


/**
 * Model for the UpdateSummaryTable.  This enforces some restrictions about what must be updated.
 *
 */
public class UpdateSummaryTableModel extends AbstractTableModel
{

      private static final long serialVersionUID = 1L;

      private List<ArtifactStatus> _artifacts = new ArrayList<ArtifactStatus>();

      private UpdateSummaryTable _table = null;
      
      /** Internationalized strings for this class. */
      private static final StringManager s_stringMgr = 
      	StringManagerFactory.getStringManager(UpdateSummaryTableModel.class);   
      
      private interface i18n {
         // i18n[UpdateSummaryTable.yes=yes]
         String YES_VAL = s_stringMgr.getString("UpdateSummaryTable.yes");

         // i18n[UpdateSummaryTable.no=no]
         String NO_VAL = s_stringMgr.getString("UpdateSummaryTable.no");
      }
      
      private final static Class<?>[] s_dataTypes = 
         new Class[] { 
            String.class, // ArtifactName
            String.class, // Type
            String.class, // Installed?
            UpdateSummaryTableActionItem.class, // Install/Update/Remove
      };
      
      private final String[] s_hdgs = new String[] {
         s_stringMgr.getString("UpdateSummaryTable.artifactNameLabel"),
         s_stringMgr.getString("UpdateSummaryTable.typeLabel"),
         s_stringMgr.getString("UpdateSummaryTable.installedLabel"),
         s_stringMgr.getString("UpdateSummaryTable.actionLabel"), };
      
      
      private final int[] s_columnWidths = new int[] { 150, 100, 100, 50 };      
            
      UpdateSummaryTableModel(List<ArtifactStatus> artifacts) {
         _artifacts = artifacts;
      }

      public void setTable(UpdateSummaryTable table) {
      	_table = table;
      }
      
      public Object getValueAt(int row, int col) {
         final ArtifactStatus as = _artifacts.get(row);
         switch (col) {
         case 0:
            return as.getName();
         case 1:
            return as.getType();
         case 2:
            return as.isInstalled() ? i18n.YES_VAL : i18n.NO_VAL;
         case 3:
         	if (as.isCoreArtifact()) {
         		return ArtifactAction.INSTALL;
         	}
            return as.getArtifactAction();
         default:
            throw new IndexOutOfBoundsException("" + col);
         }
      }

      public int getRowCount() {
         return _artifacts.size();
      }

      public int getColumnCount() {
         return s_hdgs.length;
      }

      public String getColumnName(int col) {
         return s_hdgs[col];
      }

      public Class<?> getColumnClass(int col) {
         return s_dataTypes[col];
      }

      public boolean isCellEditable(int row, int col) {
      	return col == 3;      	
      }

      public void setValueAt(Object value, int row, int col) {
         final ArtifactStatus as = _artifacts.get(row);
         ArtifactAction action = 
            ArtifactAction.valueOf(value.toString()); 
         as.setArtifactAction(action);
      }

		/**
		 * @return the s_hdgs
		 */
		public String[] getColumnHeaderNames()
		{
			return s_hdgs;
		}

		/**
		 * @return the s_columnWidths
		 */
		public int[] getColumnWidths()
		{
			return s_columnWidths;
		}
   
	
}
