package net.sourceforge.squirrel_sql.plugins.graph;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.plugins.graph.xmlbeans.ColumnInfoXmlBean;
import net.sourceforge.squirrel_sql.plugins.graph.xmlbeans.ConstraintViewXmlBean;
import net.sourceforge.squirrel_sql.plugins.graph.xmlbeans.TableFrameControllerXmlBean;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.*;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public class TableFrameController
{
   ////////////////////////////////////////
   // Serialized attributes
   private String _schema;
   private String _catalog;
   private String _tableName;
   private TableFrame _frame;

   private ColumnInfo[] _colInfos;
   private ConstraintView[] _constraintViews;
   private String[] _tablesExportedTo;
   //
   ///////////////////////////////////////////


   private ISession _session;
   private Rectangle _startSize;
   private GraphDesktopController _desktopController;
   private Vector _listeners = new Vector();
   private Vector _openFramesConnectedToMe = new Vector();
   private Hashtable _compListenersToOtherFramesByFrameCtrlr = new Hashtable();
   private Hashtable _scrollListenersToOtherFramesByFrameCtrlr = new Hashtable();
   private Hashtable _columnSortListenersToOtherFramesByFrameCtrlr = new Hashtable();

   private Vector _mySortListeners = new Vector();

   private JPopupMenu _popUp;
   private JMenuItem _mnuAddTableForForeignKey;
   private JMenuItem _mnuAddChildTables;
   private JMenuItem _mnuAddParentTables;
   private JMenuItem _mnuAddAllRelatedTables;
   private JCheckBoxMenuItem _mnuOrderByName;
   private JCheckBoxMenuItem _mnuPksAndConstraintsOnTop;
   private JCheckBoxMenuItem _mnuDbOrder;
   private AddTableListener _addTablelListener;
   private ConstraintViewListener _constraintViewListener;

   private int _columnOrder = ORDER_DB;
   private static final int ORDER_DB = 0;
   private static final int ORDER_NAME = 1;
   private static final int ORDER_PK_CONSTRAINT = 2;
   private ColumnInfo[] _orderedColumnInfos;
   private static final String MNU_PROP_COLUMN_INFO = "MNU_PROP_COLUMN_INFO";


   public TableFrameController(ISession session, GraphDesktopController paintManager, AddTableListener listener, String tableName, TableFrameControllerXmlBean xmlBean)
   {
      try
      {
         _session = session;
         _desktopController = paintManager;
         _addTablelListener = listener;

         TableToolTipProvider toolTipProvider = new TableToolTipProvider()
         {
            public String getToolTipText(MouseEvent event)
            {
               return onGetToolTipText(event);
            }
         };

         Hashtable constaintInfosByConstraintName = new Hashtable();
         if(null == xmlBean)
         {
            _tableName = tableName;
            _frame = new TableFrame(_tableName, null, toolTipProvider);

            DatabaseMetaData metaData = _session.getSQLConnection().getConnection().getMetaData();
            _catalog = _session.getSQLConnection().getCatalog();

            ResultSet res;

            _schema = null;
            Vector colInfosBuf = new Vector();
            res = metaData.getColumns(_catalog, null, _tableName, null);
            while(res.next())
            {
               _schema = res.getString("TABLE_SCHEM");
               String columnName = res.getString("COLUMN_NAME");
               String columnType = res.getString("TYPE_NAME");
               int columnSize = res.getInt("COLUMN_SIZE");
               boolean nullable = "YES".equals(res.getString("IS_NULLABLE"));

               ColumnInfo colInfo = new ColumnInfo(columnName, columnType, columnSize, nullable);
               colInfosBuf.add(colInfo);
            }
            res.close();
            _colInfos = (ColumnInfo[]) colInfosBuf.toArray(new ColumnInfo[colInfosBuf.size()]);

            res = metaData.getPrimaryKeys(_catalog, _schema, _tableName);
            while(res.next())
            {
               for (int i = 0; i < _colInfos.length; i++)
               {
                  if(_colInfos[i].getName().equals(res.getString("COLUMN_NAME")))
                  {
                     _colInfos[i].markPrimaryKey();
                  }

               }
            }
            res.close();

            res = metaData.getImportedKeys(_catalog, _schema, _tableName);
            while(res.next())
            {
               ColumnInfo colInfo = findColumnInfo(res.getString("FKCOLUMN_NAME"));
               colInfo.setImportData(res.getString("PKTABLE_NAME"), res.getString("PKCOLUMN_NAME"), res.getString("FK_NAME"));

               ConstraintData  constraintData = (ConstraintData) constaintInfosByConstraintName.get(res.getString("FK_NAME"));

               if(null == constraintData)
               {
                  constraintData = new ConstraintData(res.getString("PKTABLE_NAME"), _tableName, res.getString("FK_NAME"));
                  constaintInfosByConstraintName.put(res.getString("FK_NAME"), constraintData);
               }
               constraintData.addColumnInfo(colInfo);
            }
            res.close();

            ConstraintData[] buf = (ConstraintData[]) constaintInfosByConstraintName.values().toArray(new ConstraintData[0]);

            _constraintViews = new ConstraintView[buf.length];
            for (int i = 0; i < buf.length; i++)
            {
               _constraintViews[i] = new ConstraintView(buf[i], _desktopController, _session);
            }

         }
         else
         {
            _tableName = xmlBean.getTablename();
            _frame = new TableFrame(_tableName, xmlBean.getTableFrameXmlBean(), toolTipProvider);
            _catalog = xmlBean.getCatalog();
            _schema = xmlBean.getSchema();
            _columnOrder = xmlBean.getColumOrder();
            _colInfos = new ColumnInfo[xmlBean.getColumnIfoXmlBeans().length];
            for (int i = 0; i < _colInfos.length; i++)
            {
               _colInfos[i] = new ColumnInfo(xmlBean.getColumnIfoXmlBeans()[i]);
            }

            _constraintViews = new ConstraintView[xmlBean.getConstraintViewXmlBeans().length];
            for (int i = 0; i < _constraintViews.length; i++)
            {
               _constraintViews[i] = new ConstraintView(xmlBean.getConstraintViewXmlBeans()[i], _desktopController, _session);
               _constraintViews[i].replaceCopiedColsByReferences(_colInfos);
            }
         }


         _constraintViewListener = new ConstraintViewListener()
         {
            public void foldingPointMoved(ConstraintView source)
            {
               onFoldingPointMoved(source);
            }
         };



         _frame.addInternalFrameListener(new InternalFrameAdapter()
         {
            public void internalFrameClosing(InternalFrameEvent e)
            {
               onClose();
            }
         });

         _frame.addComponentListener(new ComponentAdapter()
         {
            public void componentMoved(ComponentEvent e)
            {
               recalculateAllConnections();
            }

            public void componentResized(ComponentEvent e)
            {
               recalculateAllConnections();
            }

            public void componentShown(ComponentEvent e)
            {
               recalculateAllConnections();
            }
         });

         _frame.scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener()
         {
            public void adjustmentValueChanged(AdjustmentEvent e)
            {
               recalculateAllConnections();
            }
         });

         createPopUp();

         orderColumns();


      }
      catch (SQLException e)
      {
         throw new RuntimeException(e);
      }
   }

   private String onGetToolTipText(MouseEvent event)
   {
      ColumnInfo ci = getColumnInfoForPoint(event.getPoint());

      if(null == ci)
      {
         return null;
      }

      return ci.getConstraintToolTipText();

   }

   private ColumnInfo getColumnInfoForPoint(Point point)
   {
      FontMetrics fm = _frame.txtColums.getGraphics().getFontMetrics(_frame.txtColums.getFont());

      for (int i = 0; i < _colInfos.length; i++)
      {
         int unscrolledHeight = _colInfos[i].getIndex() * fm.getHeight();
         if(unscrolledHeight <= point.y &&  point.y  <= unscrolledHeight +  fm.getHeight())
         {
            return _colInfos[i];
         }
      }

      return null;
   }

   public TableFrameControllerXmlBean getXmlBean()
   {
      TableFrameControllerXmlBean ret = new TableFrameControllerXmlBean();
      ret.setSchema(_schema);
      ret.setCatalog(_catalog);
      ret.setTablename(_tableName);
      ret.setTableFrameXmlBean(_frame.getXmlBean());
      ret.setColumOrder(_columnOrder);

      ColumnInfoXmlBean[] colXmlBeans = new ColumnInfoXmlBean[_colInfos.length];
      for (int i = 0; i < _colInfos.length; i++)
      {
         colXmlBeans[i] = _colInfos[i].getXmlBean();
      }
      ret.setColumnIfoXmlBeans(colXmlBeans);

      ConstraintViewXmlBean[] constViewXmlBeans = new ConstraintViewXmlBean[_constraintViews.length];
      for (int i = 0; i < _constraintViews.length; i++)
      {
         constViewXmlBeans[i] =_constraintViews[i].getXmlBean();
      }
      ret.setConstraintViewXmlBeans(constViewXmlBeans);

      ret.setTablesExportedTo(_tablesExportedTo);


      return ret;
   }


   private void createPopUp()
   {
      _popUp = new JPopupMenu();

      _mnuAddTableForForeignKey = new JMenuItem();
      _mnuAddTableForForeignKey.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            onAddTableForForeignKey((ColumnInfo)_mnuAddTableForForeignKey.getClientProperty(MNU_PROP_COLUMN_INFO));
         }
      });

      _mnuAddChildTables = new JMenuItem("Add child tables");
      _mnuAddChildTables.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            onAddChildTables();
         }
      });

      _mnuAddParentTables = new JMenuItem("Add parent tables");
      _mnuAddParentTables.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            onAddParentTables();
         }
      });

      _mnuAddAllRelatedTables = new JMenuItem("Add all related tables");
      _mnuAddAllRelatedTables.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            onAddAllRelatedTables();
         }
      });


      _mnuDbOrder = new JCheckBoxMenuItem("db order");
      _mnuDbOrder.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            onDBOrder();
         }
      });

      _mnuOrderByName = new JCheckBoxMenuItem("order by name");
      _mnuOrderByName.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            onNameOrder();
         }
      });

      _mnuPksAndConstraintsOnTop = new JCheckBoxMenuItem("order PKs/constraints on top");
      _mnuPksAndConstraintsOnTop.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            onPkConstraintOrder();
         }
      });


      _popUp.add(_mnuAddTableForForeignKey);
      _popUp.add(_mnuAddChildTables);
      _popUp.add(_mnuAddParentTables);
      _popUp.add(_mnuAddAllRelatedTables);
      _popUp.add(new JSeparator());
      _popUp.add(_mnuDbOrder);
      _popUp.add(_mnuOrderByName);
      _popUp.add(_mnuPksAndConstraintsOnTop);
      _frame.txtColums.addMouseListener(new MouseAdapter()
      {
         public void mousePressed(MouseEvent e)
         {
            maybeShowPopup(e);
         }

         public void mouseReleased(MouseEvent e)
         {
            maybeShowPopup(e);
         }
      });


   }

   private void onAddTableForForeignKey(ColumnInfo columnInfo)
   {
      _addTablelListener.addTablesRequest(new String[]{columnInfo.getImportedTableName()});
   }

   private void onPkConstraintOrder()
   {
      _columnOrder = ORDER_PK_CONSTRAINT;
      orderColumns();
      SwingUtilities.invokeLater(new Runnable()
      {
         public void run()
         {
            recalculateAllConnections();
            fireSortListeners();
         }
      });
   }

   private void onNameOrder()
   {
      _columnOrder = ORDER_NAME;
      orderColumns();
      SwingUtilities.invokeLater(new Runnable()
      {
         public void run()
         {
            recalculateAllConnections();
            fireSortListeners();
         }
      });
   }

   private void onDBOrder()
   {
      _columnOrder = ORDER_DB;
      orderColumns();
      SwingUtilities.invokeLater(new Runnable()
      {
         public void run()
         {
            recalculateAllConnections();
            fireSortListeners();
         }
      });
   }

   private void fireSortListeners()
   {
      ColumnSortListener[] listeners = (ColumnSortListener[]) _mySortListeners.toArray(new ColumnSortListener[_mySortListeners.size()]);

      for (int i = 0; i < listeners.length; i++)
      {
         listeners[i].columnOrderChanged();
      }
   }

   private void orderColumns()
   {
      Comparator comp;

      switch(_columnOrder)
      {
         case ORDER_DB:
            _orderedColumnInfos = _colInfos;
            _mnuDbOrder.setSelected(true);
            _mnuOrderByName.setSelected(false);
            _mnuPksAndConstraintsOnTop.setSelected(false);
            break;
         case ORDER_NAME:
            _orderedColumnInfos = new ColumnInfo[_colInfos.length];
            System.arraycopy(_colInfos, 0, _orderedColumnInfos, 0, _colInfos.length);

            comp = new Comparator()
            {
               public int compare(Object o1, Object o2)
               {
                  ColumnInfo c1 = (ColumnInfo) o1;
                  ColumnInfo c2 = (ColumnInfo) o2;
                  return c1.getName().compareTo(c2.getName());
               }
            };
            Arrays.sort(_orderedColumnInfos, comp);

            _mnuDbOrder.setSelected(false);
            _mnuOrderByName.setSelected(true);
            _mnuPksAndConstraintsOnTop.setSelected(false);
            break;
         case ORDER_PK_CONSTRAINT:
            _orderedColumnInfos = new ColumnInfo[_colInfos.length];
            System.arraycopy(_colInfos, 0, _orderedColumnInfos, 0, _colInfos.length);

            comp = new Comparator()
            {
               public int compare(Object o1, Object o2)
               {
                  ColumnInfo c1 = (ColumnInfo) o1;
                  ColumnInfo c2 = (ColumnInfo) o2;
                  if(c1.isPrimaryKey() && false == c2.isPrimaryKey())
                  {
                     return -1;
                  }
                  else if(false == c1.isPrimaryKey() && c2.isPrimaryKey())
                  {
                     return 1;
                  }
                  else
                  {
                     if(null != c1.getConstraintName() && null == c2.getConstraintName())
                     {
                        return -1;
                     }
                     else if(null == c1.getConstraintName() && null != c2.getConstraintName())
                     {
                        return 1;
                     }
                     else
                     {
                        if(null != c1.getConstraintName() && null != c2.getConstraintName())
                        {
                           String s1 = c1.getConstraintName() + "_" + c1.getName();
                           String s2 = c2.getConstraintName() + "_" + c2.getName();
                           return s1.compareTo(s2);
                        }
                        else
                        {
                           return c1.getName().compareTo(c2.getName());
                        }
                     }
                  }
               }
            };
            Arrays.sort(_orderedColumnInfos, comp);

            _mnuDbOrder.setSelected(false);
            _mnuOrderByName.setSelected(false);
            _mnuPksAndConstraintsOnTop.setSelected(true);
            break;
         default:
            throw new IllegalStateException("Unknown order " + _columnOrder);
      }

      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < _orderedColumnInfos.length; i++)
      {
         _orderedColumnInfos[i].setIndex(i);
         sb.append(_orderedColumnInfos[i]).append('\n');
      }
      _frame.txtColums.setText(sb.toString());

      SwingUtilities.invokeLater(new Runnable()
      {
         public void run()
         {
            _frame.txtColums.scrollRectToVisible(new Rectangle(0,0,1,1));
         }
      });
   }



   private void onAddAllRelatedTables()
   {
      onAddChildTables();
      onAddParentTables();
   }


   private void onAddChildTables()
   {
      try
      {
         DatabaseMetaData metaData = _session.getSQLConnection().getConnection().getMetaData();


         if(null == _tablesExportedTo)
         {
            Hashtable exportBuf = new Hashtable();
            ResultSet res = metaData.getExportedKeys(_catalog, _schema, _tableName);
            while(res.next())
            {
               String tableName = res.getString("FKTABLE_NAME");
               exportBuf.put(tableName, tableName);
            }
            _tablesExportedTo = (String[]) exportBuf.keySet().toArray(new String[0]);
         }
         _addTablelListener.addTablesRequest(_tablesExportedTo);

      }
      catch (SQLException e)
      {
         throw new RuntimeException(e);
      }
   }



   private void onAddParentTables()
   {
      Vector tablesToAdd = new Vector();

      for (int i = 0; i < _constraintViews.length; i++)
      {
         tablesToAdd.add(_constraintViews[i].getData().getPkTableName());
      }

      _addTablelListener.addTablesRequest((String[]) tablesToAdd.toArray(new String[tablesToAdd.size()]));
   }



   private void maybeShowPopup(MouseEvent e)
   {
      if (e.isPopupTrigger())
      {

         ColumnInfo ci = getColumnInfoForPoint(e.getPoint());
         if(null == ci || null == ci.getImportedTableName())
         {
            _mnuAddTableForForeignKey.setEnabled(false);
            _mnuAddTableForForeignKey.setText("add table referenced by (no hit on FK)");
         }
         else
         {
            _mnuAddTableForForeignKey.setEnabled(true);
            _mnuAddTableForForeignKey.setText("add table referenced by " + ci.getName());
            _mnuAddTableForForeignKey.putClientProperty(MNU_PROP_COLUMN_INFO, ci);
         }

         _popUp.show(e.getComponent(), e.getX(), e.getY());
      }
      else if(2 == e.getClickCount())
      {
         ColumnInfo ci = getColumnInfoForPoint(e.getPoint());
         if(null != ci && null != ci.getImportedTableName())
         {
            _addTablelListener.addTablesRequest(new String[]{ci.getImportedTableName()});
         }
      }


   }


   void initAfterAddedToDesktop(TableFrameController[] openFrames, boolean resetBounds)
   {
      calculateStartSize();
      for (int i = 0; i < openFrames.length; i++)
      {
         tableFrameOpen(openFrames[i]);
      }

      if(resetBounds)
      {
         _frame.setBounds(_startSize);
      }
      _frame.setVisible(true);
      _frame.txtColums.scrollRectToVisible(new Rectangle(0,0,1,1));

   }

   private void onClose()
   {
      _desktopController.removeConstraintViews(_constraintViews);

      for (int i = 0; i < _listeners.size(); i++)
      {
         TableFrameControllerListener tableFrameControllerListener = (TableFrameControllerListener) _listeners.elementAt(i);
         tableFrameControllerListener.closed(this);
      }

      for(Enumeration e=_compListenersToOtherFramesByFrameCtrlr.keys(); e.hasMoreElements();)
      {
         TableFrameController tfc = (TableFrameController) e.nextElement();
         ComponentAdapter listenerToRemove = (ComponentAdapter) _compListenersToOtherFramesByFrameCtrlr.get(tfc);
         tfc._frame.removeComponentListener(listenerToRemove);
      }

      for(Enumeration e=_scrollListenersToOtherFramesByFrameCtrlr.keys(); e.hasMoreElements();)
      {
         TableFrameController tfc = (TableFrameController) e.nextElement();
         AdjustmentListener listenerToRemove = (AdjustmentListener) _scrollListenersToOtherFramesByFrameCtrlr.get(tfc);
         tfc._frame.scrollPane.getVerticalScrollBar().removeAdjustmentListener(listenerToRemove);
      }


   }

   void tableFrameOpen(final TableFrameController tfc)
   {
      if(_openFramesConnectedToMe.contains(tfc))
      {
         return;
      }


      if(false == recalculateConnectionsTo(tfc))
      {
         return;
      }

      _openFramesConnectedToMe.add(tfc);

      ComponentAdapter compListener =
         new ComponentAdapter()
         {
            public void componentMoved(ComponentEvent e)
            {
               recalculateConnectionsTo(tfc);
            }
            public void componentResized(ComponentEvent e)
            {
               recalculateConnectionsTo(tfc);
            }
            public void componentShown(ComponentEvent e)
            {
               recalculateConnectionsTo(tfc);
            }
         };

      _compListenersToOtherFramesByFrameCtrlr.put(tfc, compListener);
      tfc._frame.addComponentListener(compListener);

      AdjustmentListener adjListener = new AdjustmentListener()
      {
         public void adjustmentValueChanged(AdjustmentEvent e)
         {
            recalculateConnectionsTo(tfc);
         }
      };
      tfc._frame.scrollPane.getVerticalScrollBar().addAdjustmentListener(adjListener);
      _scrollListenersToOtherFramesByFrameCtrlr.put(tfc, adjListener);

      ColumnSortListener sortListener = new ColumnSortListener()
      {
         public void columnOrderChanged()
         {
            recalculateConnectionsTo(tfc);
         }
      };

      tfc.addSortListener(sortListener);
      _columnSortListenersToOtherFramesByFrameCtrlr.put(tfc, sortListener);

   }

   private void addSortListener(ColumnSortListener sortListener)
   {
      _mySortListeners.add(sortListener);
   }

   private void removeSortListener(ColumnSortListener sortListener)
   {
      _mySortListeners.remove(sortListener);
   }


   private void recalculateAllConnections()
   {
      for (int i = 0; i < _openFramesConnectedToMe.size(); i++)
      {
         TableFrameController tableFrameController = (TableFrameController) _openFramesConnectedToMe.elementAt(i);
         recalculateConnectionsTo(tableFrameController);
      }
   }


   public void tableFrameRemoved(TableFrameController tfc)
   {
      _openFramesConnectedToMe.remove(tfc);

      Vector constraintDataToRemove = new Vector();
      Vector newConstraintData = new Vector();

      for (int i = 0; i < _constraintViews.length; i++)
      {
         if(_constraintViews[i].getData().getPkTableName().equals(tfc._tableName))
         {
            constraintDataToRemove.add(_constraintViews[i]);
         }
         else
         {
            newConstraintData.add(_constraintViews[i]);
         }
      }

      ComponentAdapter compListenerToRemove = (ComponentAdapter) _compListenersToOtherFramesByFrameCtrlr.remove(tfc);
      if(null != compListenerToRemove)
      {
         tfc._frame.removeComponentListener(compListenerToRemove);
      }


      AdjustmentListener adjListenerToRemove = (AdjustmentListener) _scrollListenersToOtherFramesByFrameCtrlr.remove(tfc);
      if(null != adjListenerToRemove)
      {
         tfc._frame.scrollPane.getVerticalScrollBar().removeAdjustmentListener(adjListenerToRemove);
      }

      ColumnSortListener columnSortListener = (ColumnSortListener) _columnSortListenersToOtherFramesByFrameCtrlr.get(tfc);
      if(null != columnSortListener)
      {
         tfc.removeSortListener(columnSortListener);
      }

      ConstraintView[] buf = (ConstraintView[]) constraintDataToRemove.toArray(new ConstraintView[constraintDataToRemove.size()]);
      _desktopController.removeConstraintViews(buf);
   }

   private boolean recalculateConnectionsTo(TableFrameController other)
   {
      ConstraintView[] constraintView = findConstraintViews(other._tableName);

      if(0 == constraintView.length)
      {
         return false;
      }

      for (int i = 0; i < constraintView.length; i++)
      {
         ColumnInfo[] colInfos = constraintView[i].getData().getColumnInfos();

         Point firstFoldingPoint = constraintView[i].getFirstFoldingPoint();
         Point lastFoldingPoint = constraintView[i].getLastFoldingPoint();

         ConnectionPoints fkPoints = getConnectionPoints(colInfos, this, other, firstFoldingPoint);

         ColumnInfo[] othersColInfos = new ColumnInfo[colInfos.length];
         for (int j = 0; j < othersColInfos.length; j++)
         {
            othersColInfos[j] = other.findColumnInfo(colInfos[j].getImportedColumnName());
         }

         ConnectionPoints pkPoints = getConnectionPoints(othersColInfos, other, this, lastFoldingPoint);

         constraintView[i].setConnectionPoints(fkPoints, pkPoints, other, _constraintViewListener);
      }

      _desktopController.putConstraintViews(constraintView);
      _desktopController.repaint();

      return true;

   }

   private void onFoldingPointMoved(ConstraintView source)
   {
      recalculateConnectionsTo(source.getPkFramePointingTo());
   }



   private static ConnectionPoints getConnectionPoints(ColumnInfo[] colInfos, TableFrameController me, TableFrameController other, Point myNextFoldingPoint)
   {
      int[] relPointHeights = me.calculateRelativeConnectionPointHeights(colInfos);

      Rectangle myBounds = me._frame.getBounds();
      Rectangle othersBounds = other._frame.getBounds();

      ConnectionPoints ret = new ConnectionPoints();
      ret.points = new Point[relPointHeights.length];

      for (int i = 0; i < ret.points.length; i++)
      {

         if(null == myNextFoldingPoint)
         {
            if(myBounds.x + myBounds.width *3/4 < othersBounds.x)
            {
               ret.points[i] = new Point(myBounds.x + myBounds.width, myBounds.y + relPointHeights[i]);
               ret.pointsAreLeftOfWindow = false;
            }
            else
            {
               ret.points[i] = new Point(myBounds.x , myBounds.y + relPointHeights[i]);
               ret.pointsAreLeftOfWindow = true;
            }
         }
         else
         {
            if(myBounds.x + myBounds.width / 2 < myNextFoldingPoint.x)
            {
               ret.points[i] = new Point(myBounds.x + myBounds.width, myBounds.y + relPointHeights[i]);
               ret.pointsAreLeftOfWindow = false;
            }
            else
            {
               ret.points[i] = new Point(myBounds.x , myBounds.y + relPointHeights[i]);
               ret.pointsAreLeftOfWindow = true;
            }
         }
      }
      return ret;
   }



   private ConstraintView[] findConstraintViews(String tableName)
   {
      Vector ret = new Vector();
      for (int i = 0; i < _constraintViews.length; i++)
      {
         if(_constraintViews[i].getData().getPkTableName().equals(tableName))
         {
            ret.add(_constraintViews[i]);
         }
      }
      return (ConstraintView[]) ret.toArray(new ConstraintView[ret.size()]);
   }


   private int[] calculateRelativeConnectionPointHeights(ColumnInfo[] colInfos)
   {
      Hashtable buf = new Hashtable();
      FontMetrics fm = _frame.txtColums.getGraphics().getFontMetrics(_frame.txtColums.getFont());

      for (int i = 0; i < colInfos.length; i++)
      {
         int unscrolledHeight = colInfos[i].getIndex() * fm.getHeight() + fm.getHeight() / 2;
         int scrolledHeight;
         Rectangle viewRect = _frame.scrollPane.getViewport().getViewRect();

         scrolledHeight = unscrolledHeight - viewRect.y;
         if(scrolledHeight < 0)
         {
            scrolledHeight = 0;
         }
         if(scrolledHeight > viewRect.height)
         {
            scrolledHeight = viewRect.height;
         }

         scrolledHeight += + _frame.getTitlePane().getSize().height + 6;

         buf.put(new Integer(scrolledHeight), new Integer(scrolledHeight));
      }

      int[] ret = new int[buf.size()];

      int i=0;
      for(Enumeration e=buf.keys(); e.hasMoreElements(); )
      {
         ret[i++] = ((Integer)e.nextElement()).intValue();
      }
      return ret;
   }

   private ColumnInfo findColumnInfo(String colName)
   {
      for (int i = 0; i < _colInfos.length; i++)
      {
         if(_colInfos[i].getName().equals(colName))
         {
            return _colInfos[i];
         }
      }

      throw new IllegalArgumentException("Column " + colName + " not found");
   }

   private void calculateStartSize()
   {
      int maxViewingCols = 15;

      FontMetrics fm = _frame.txtColums.getGraphics().getFontMetrics(_frame.txtColums.getFont());
      int width = getMaxSize(_colInfos, fm) + 30;
      int height = (int)(Math.min(_colInfos.length, maxViewingCols) * (fm.getHeight()) + 47);
      _startSize = new Rectangle(width, height);
   }

   private int getMaxSize(ColumnInfo[] infos, FontMetrics fontMetrics)
   {
      int maxSize = 0;
      for (int i = 0; i < infos.length; i++)
      {
         int buf = fontMetrics.stringWidth(infos[i].toString());
         if(maxSize < buf)
         {
            maxSize = buf;
         }
      }
      return maxSize;

   }


   TableFrame getFrame()
   {
      return _frame;
   }

   public void addTableFrameControllerListener(TableFrameControllerListener l)
   {
      _listeners.add(l);
   }

   public boolean equals(Object obj)
   {
      if(obj instanceof TableFrameController)
      {
         TableFrameController other = (TableFrameController) obj;

         return other._tableName.equals(_tableName);
      }
      else
      {
         return false;
      }

   }

   public int hashCode()
   {
      return _tableName.hashCode();
   }

}
