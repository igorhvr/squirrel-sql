package net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.tabs.table;
/*
 * Copyright (C) 2001-2004 Colin Bell
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.sourceforge.squirrel_sql.client.session.DataSetUpdateableTableModelImpl;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.properties.SessionProperties;
import net.sourceforge.squirrel_sql.client.session.sqlfilter.OrderByClausePanel;
import net.sourceforge.squirrel_sql.client.session.sqlfilter.SQLFilterClauses;
import net.sourceforge.squirrel_sql.client.session.sqlfilter.WhereClausePanel;
import net.sourceforge.squirrel_sql.fw.datasetviewer.ColumnDisplayDefinition;
import net.sourceforge.squirrel_sql.fw.datasetviewer.DataSetException;
import net.sourceforge.squirrel_sql.fw.datasetviewer.DataSetScrollingPanel;
import net.sourceforge.squirrel_sql.fw.datasetviewer.DataSetUpdateableTableModelListener;
import net.sourceforge.squirrel_sql.fw.datasetviewer.DataSetViewerTablePanel;
import net.sourceforge.squirrel_sql.fw.datasetviewer.IDataSet;
import net.sourceforge.squirrel_sql.fw.datasetviewer.IDataSetUpdateableTableModel;
import net.sourceforge.squirrel_sql.fw.datasetviewer.ResultSetDataSet;
import net.sourceforge.squirrel_sql.fw.gui.TablePopupMenu;
import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.fw.sql.SQLConnection;
import net.sourceforge.squirrel_sql.fw.sql.SQLDatabaseMetaData;
import net.sourceforge.squirrel_sql.fw.sql.TableColumnInfo;
import net.sourceforge.squirrel_sql.fw.sql.dbobj.BestRowIdentifier;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

/**
 * This is the tab showing the contents (data) of the table.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class ContentsTab extends BaseTableTab
	implements IDataSetUpdateableTableModel
{
   private DataSetUpdateableTableModelImpl _dataSetUpdateableTableModel = new DataSetUpdateableTableModelImpl();

	public static String TITLE = i18n.TITLE;

	/**
	 * Name of the table that this tab displayed last time it was loaded.
	 * This is needed to prevent an on-demand edit operation from turning
	 * all data into editable tables.
	 * The initial value of "" allows us to dispense with a check for null
	 * on the first pass.
	 */
	String previousTableName = "";

   private SQLFilterClauses _sqlFilterClauses = new SQLFilterClauses();


   /**
	 * This interface defines locale specific strings. This should be
	 * replaced with a property file.
	 */
	private interface i18n
	{
		String TITLE = "Content";
		String HINT = "Sample Contents";
	}

	/** Logger for this class. */
	private static final ILogger s_log =
		LoggerController.createLogger(ContentsTab.class);


   public ContentsTab()
   {
      System.out.println("");
   }

	/**
	 * Return the title for the tab.
	 *
	 * @return	The title for the tab.
	 */
	public String getTitle()
	{
		return i18n.TITLE;
	}

	/**
	 * Return the hint for the tab.
	 *
	 * @return	The hint for the tab.
	 */
	public String getHint()
	{
		return i18n.HINT;
	}
	
	/**
	 * Override the parent's getComponent method so that we can
	 * attach a menu to the ContentsTab pane that allows the user
	 * to insert a new row when the table is empty.
	 */
	public Component getComponent()
	{
		final Component c = super.getComponent();
		
		if (c != null)
		{
			// remove any previously set listeners
			MouseListener[] oldListeners = c.getMouseListeners();
			for (int i=0; i< oldListeners.length; i++)
			{
				c.removeMouseListener(oldListeners[i]);
			}

			// If the viewer is a table AND table iseditable, add a listener using the current viewer
			if (((DataSetScrollingPanel)c).getViewer() instanceof DataSetViewerTablePanel)
			{
				final DataSetViewerTablePanel viewer = (DataSetViewerTablePanel)((DataSetScrollingPanel)c).getViewer();
				if (viewer.isTableEditable())
				{
					c.addMouseListener(new MouseAdapter()
					{
						public void mousePressed(MouseEvent evt)
						{
							// in theory, we should only need to check the PopupTrigger, but
							// at least one user had a problem where they clicked on the right
							// mouse button and the event was not classified as the PopupTrigger.
							// It is unknown why that occured, but we needed to add the second
							// test (button3&&clickcount=1) to resolve that issue.
// TODO: Put back in when JDK1.4 is minimum supported version.
//							if (evt.isPopupTrigger() || (evt.getButton()==3 && evt.getClickCount() == 1))
							if (evt.isPopupTrigger() || (((evt.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) && evt.getClickCount() == 1))
							{
								new ContentsTabPopupMenu(viewer).show(evt);
							}
						}
					});
				}
			}
		}

		return c;
	}

   public SQLFilterClauses getSQLFilterClauses()
   {
      return _sqlFilterClauses;
   }


   /**
    * Create the <TT>IDataSet</TT> to be displayed in this tab.
    */
   protected IDataSet createDataSet() throws DataSetException
   {
      final ISession session = getSession();
      final SQLConnection conn = session.getSQLConnection();
      SQLDatabaseMetaData md = conn.getSQLMetaData();
      //ISQLDatabaseMetaData md = conn.getSQLMetaData();
      //md = (ISQLDatabaseMetaData)Spin.off(md);
//		final SQLFilterClauses sqlFilterClauses = session.getSQLFilterClauses();

      try
      {
         final Statement stmt = conn.createStatement();
         try
         {
            final SessionProperties props = session.getProperties();
            if (props.getContentsLimitRows())
            {
               try
               {
                  stmt.setMaxRows(props.getContentsNbrRowsToShow());
               }
               catch (Exception ex)
               {
                  s_log.error("Error on Statement.setMaxRows()", ex);
               }
            }
            final ITableInfo ti = getTableInfo();

            /**
             * When the SessionProperties are set to read-only (either table or text)
             * but the user has selected "Make Editable" on the Popup menu, we want
             * to limit the edit capability to only that table, and only for as long
             * as the user is looking at that one table.  When the user switches away
             * to another table, that new table should not be editable.
             */
            final String currentTableName = ti.getQualifiedName();
            if (!currentTableName.equals(previousTableName))
            {
               previousTableName = currentTableName;	// needed to prevent an infinite loop
               _dataSetUpdateableTableModel.setEditModeForced(false);

               /**
                * Tell the GUI to rebuild itself.
                * Unfortunately, this has the side effect of calling this same function
                * another time.  The second call does not seem to be a problem,
                * but we need to have reset the previousTableName before makeing
                * this call or we will be in an infinite loop.
                */
               //props.forceTableContentsOutputClassNameChange();
            }

            /**
             * If the table has a pseudo-column that is the best unique
             * identifier for the rows (like Oracle's rowid), then we
             * want to include that field in the query so that it will
             * be available if the user wants to edit the data later.
             */
            String pseudoColumn = "";

            try
            {
               BestRowIdentifier[] rowIDs = md.getBestRowIdentifier(ti);
               for (int i = 0; i < rowIDs.length; ++i)
               {
                  short pseudo = rowIDs[i].getPseudoColumn();
                  if (pseudo == DatabaseMetaData.bestRowPseudo)
                  {
                     pseudoColumn = " ," + rowIDs[i].getColumnName();
                     break;
                  }
               }
            }

            // Some DBMS's (EG Think SQL) throw an exception on a call to
            // getBestRowIdentifier.
            catch (Throwable th)
            {
               s_log.debug("getBestRowIdentifier not supported for table "+
                           currentTableName, th);
            }

            // TODO: - Col - Add method to Databasemetadata that returns array
            // of objects for getBestRowIdentifier. For PostgreSQL put this kludge in
            // the new function. THis way all the kludges are kept in one place.
            //
            // KLUDGE!!!!!!
            //
            // For some DBs (e.g. PostgreSQL) there is actually a pseudo-column
            // providing the rowId, but the getBestRowIdentifier function is not
            // implemented.  This kludge hardcodes the knowledge that specific
            // DBs use a specific pseudo-column.
            //
            if (pseudoColumn.length() == 0)
            {
               String dbName = md.getDatabaseProductName().toUpperCase();
               if (dbName.equals("POSTGRESQL"))
               {
                  pseudoColumn = ", oid";
               }
            }

            ResultSet rs = null;
            try
            {
               // Note. Some DBMSs such as Oracle do not allow:
               // "select *, rowid from table"
               // You cannot have any column name in the columns clause
               // if you have * in there. Aliasing the table name seems to
               // be the best way to get around the problem.
               final StringBuffer buf = new StringBuffer();
               buf.append("select tbl.*")
                  .append(pseudoColumn)
                  .append(" from ")
                  .append(ti.getQualifiedName())
                  .append(" tbl");

               String clause = _sqlFilterClauses.get(WhereClausePanel.getClauseIdentifier(), ti.getQualifiedName());
               if ((clause != null) && (clause.length() > 0))
               {
                 buf.append(" where ").append(clause);
               }
               clause = _sqlFilterClauses.get(OrderByClausePanel.getClauseIdentifier(), ti.getQualifiedName());
               if ((clause != null) && (clause.length() > 0))
               {
                 buf.append(" order by ").append(clause);
               }

               rs = stmt.executeQuery(buf.toString());
            }
            catch (SQLException ex)
            {
               if (pseudoColumn.length() == 0)
               {
                  throw ex;
               }

               // Some tables have pseudo column primary keys and others
               // do not.  JDBC on some DBMSs does not handle pseudo
               // columns 'correctly'.  Also, getTables returns 'views' as
               // well as tables, so the thing we are looking at might not
               // be a table. (JDBC does not give a simple way to
               // determine what we are looking at since the type of
               // object is described in a DBMS-specific encoding.)  For
               // these reasons, rather than testing for all these
               // conditions, we just try using the pseudo column info to
               // get the table data, and if that fails, we try to get the
               // table data without using the pseudo column.
               // TODO: Should we change the mode from editable to
               // non-editable?
               s_log.debug("Error querying using pseudo column", ex);
               final StringBuffer buf = new StringBuffer();
               buf.append("select *")
                  .append(" from ")
                  .append(ti.getQualifiedName())
                  .append(" tbl");

               String clause = _sqlFilterClauses.get(WhereClausePanel.getClauseIdentifier(), ti.getQualifiedName());
               if ((clause != null) && (clause.length() > 0))
               {
                 buf.append(" where ").append(clause);
               }
               clause = _sqlFilterClauses.get(OrderByClausePanel.getClauseIdentifier(), ti.getQualifiedName());
               if ((clause != null) && (clause.length() > 0))
               {
                 buf.append(" order by ").append(clause);
               }

               rs = stmt.executeQuery(buf.toString());
            }

            final ResultSetDataSet rsds = new ResultSetDataSet();

            // to allow the fw to save and reload user options related to
            // specific columns, we construct a unique name for the table
            // so the column can be associcated with only that table.
            // Some drivers do not provide the catalog or schema info, so
            // those parts of the name will end up as null.  That's ok since
            // this string is never viewed by the user and is just used to
            // distinguish this table from other tables in the DB.
            // We also include the URL used to connect to the DB so that
            // the same table/DB on different machines is treated differently.
            rsds.setContentsTabResultSet(rs, _dataSetUpdateableTableModel.getFullTableName());
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) {}
            }
            // KLUDGE:
            // We want some info about the columns to be available for validating the
            // user input during cell editing operations.  Ideally we would get that
            // info inside the ResultSetDataSet class during the creation of the
            // columnDefinition objects by using various functions in ResultSetMetaData
            // such as isNullable(idx).  Unfortunately, in at least some DBMSs (e.g.
            // Postgres, HSDB) the results of those calls are not the same (and are less accurate
            // than) the SQLMetaData.getColumns() call used in ColumnsTab to get the column info.
            // Even more unfortunate is the fact that the set of attributes reported on by the two
            // calls is not the same, with the ResultSetMetadata listing things not provided by
            // getColumns.  Most of the data provided by the ResultSetMetaData calls is correct.
            // However, the nullable/not-nullable property is not set correctly in at least two
            // DBMSs, while it is correct for those DBMSs in the getColumns() info.  Therefore,
            // we collect the collumn nullability information from getColumns() and pass that
            // info to the ResultSet to override what it got from the ResultSetMetaData.
            TableColumnInfo[] columnInfos = md.getColumnInfo(getTableInfo());
            final ColumnDisplayDefinition[] colDefs = 
                rsds.getDataSetDefinition().getColumnDefinitions();

            // get the nullability information and pass it into the ResultSet
            // Unfortunately, not all DBMSs provide the column number in object 17 as stated in the
            // SQL documentation, so we have to guess that the result set is in column order
            for (int i = 0; i < columnInfos.length; i++) {
                boolean isNullable = true;
                TableColumnInfo info = columnInfos[i];
                if (info.isNullAllowed() == DatabaseMetaData.columnNoNulls) {
                    isNullable = false;
                }
                if (i < colDefs.length) {
                    colDefs[i].setIsNullable(isNullable);
                }
            }

            //?? remember which column is the rowID (if any) so we can
            //?? prevent editing on it
            if (pseudoColumn.length() > 0)
            {
               _dataSetUpdateableTableModel.setRowIDCol(rsds.getColumnCount() - 1);
            }

            return rsds;
         }
         finally
         {
            stmt.close();
         }

      }
      catch (SQLException ex)
      {
         throw new DataSetException(ex);
      }
   }

   public void setDatabaseObjectInfo(IDatabaseObjectInfo value)
   {
      super.setDatabaseObjectInfo(value);
      _dataSetUpdateableTableModel.setTableInfo(getTableInfo());
   }

   public void setSession(ISession session) throws IllegalArgumentException
   {
      super.setSession(session);
      _dataSetUpdateableTableModel.setSession(session);
   }


	/**
	 * return the name of the table that is unambiguous across DB accesses,
	 * including the same DB on different machines.
	 * This function is static because it is used elsewhere to generate the same
	 * name as is used within instances of this class.
	 * 
	 * @return the name of the table that is unique for this DB access
	 */
	public static String getUnambiguousTableName(ISession session, String name)
   {
		return DataSetUpdateableTableModelImpl.getUnambiguousTableName(session, name);
	}

   ////////////////////////////////////////////////////////
   // Implementataion of IDataSetUpdateableTableModel:
   // Delegation to _dataSetUpdateableTableModel
   public String getWarningOnCurrentData(Object[] values, ColumnDisplayDefinition[] colDefs, int col, Object oldValue)
   {
      return _dataSetUpdateableTableModel.getWarningOnCurrentData(values, colDefs, col, oldValue);
   }

   public String getWarningOnProjectedUpdate(Object[] values, ColumnDisplayDefinition[] colDefs, int col, Object newValue)
   {
      return _dataSetUpdateableTableModel.getWarningOnProjectedUpdate(values, colDefs, col, newValue);
   }

   public Object reReadDatum(Object[] values, ColumnDisplayDefinition[] colDefs, int col, StringBuffer message)
   {
      return _dataSetUpdateableTableModel.reReadDatum(values, colDefs, col, message);
   }

   public String updateTableComponent(Object[] values, ColumnDisplayDefinition[] colDefs, int col, Object oldValue, Object newValue)
   {
      return _dataSetUpdateableTableModel.updateTableComponent(values, colDefs, col, oldValue, newValue);
   }

   public int getRowidCol()
   {
      return _dataSetUpdateableTableModel.getRowidCol();
   }

   public String deleteRows(Object[][] rowData, ColumnDisplayDefinition[] colDefs)
   {
      return _dataSetUpdateableTableModel.deleteRows(rowData, colDefs);
   }

   public String[] getDefaultValues(ColumnDisplayDefinition[] colDefs)
   {
      return _dataSetUpdateableTableModel.getDefaultValues(colDefs);
   }

   public String insertRow(Object[] values, ColumnDisplayDefinition[] colDefs)
   {
      return _dataSetUpdateableTableModel.insertRow(values, colDefs);
   }

   public void addListener(DataSetUpdateableTableModelListener l)
   {
      _dataSetUpdateableTableModel.addListener(l);
   }

   public void removeListener(DataSetUpdateableTableModelListener l)
   {
      _dataSetUpdateableTableModel.removeListener(l);
   }

   public void forceEditMode(boolean mode)
   {
      _dataSetUpdateableTableModel.forceEditMode(mode);
   }

   public boolean editModeIsForced()
   {
      return _dataSetUpdateableTableModel.editModeIsForced();
   }

   protected String getDestinationClassName()
   {
      return _dataSetUpdateableTableModel.getDestinationClassName();
   }
   //
   //////////////////////////////////////////////////////////////////////////////////



   /**
	 * This inner class defines a pop-up menu with only one item, "insert", which
	 * allows the user to add a new row to an empty table.
	 */
	class ContentsTabPopupMenu extends TablePopupMenu
	{
		public ContentsTabPopupMenu(DataSetViewerTablePanel viewer) {
			super(viewer.isTableEditable(), ContentsTab.this, viewer);
			removeAll();	// the normal constructor creates a bunch of entries we do not want
			add(_insertRow);
		}
	}

}
