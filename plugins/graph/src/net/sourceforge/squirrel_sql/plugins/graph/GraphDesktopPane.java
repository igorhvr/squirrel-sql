package net.sourceforge.squirrel_sql.plugins.graph;

import net.sourceforge.squirrel_sql.client.gui.ScrollableDesktopPane;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.util.Vector;
import java.util.Arrays;


public class GraphDesktopPane extends ScrollableDesktopPane implements GraphPrintable
{
   private Vector _graphComponents = new Vector();
   private ConstraintViewListener _constraintViewListener;

   /////////////////////////////////////////////////////////
   // Printing
   private double _formatWidth;
   private double _formatHeight;
   private double _formatScale;
   private boolean _isPrinting;
   //
   /////////////////////////////////////////////////////////

   public GraphDesktopPane()
   {
      _constraintViewListener = new ConstraintViewListener()
      {
         public void foldingPointMoved(ConstraintView source)
         {
            revalidate();
         }
      };
   }


   public void paint(Graphics g)
   {
      super.paintComponent(g);
      super.paintBorder(g);

      paintGraphComponents(g);

      super.paintChildren(g);
   }

   private void paintGraphComponents(Graphics g)
   {
      for (int i = 0; i < _graphComponents.size(); i++)
      {
         GraphComponent comp = (GraphComponent)_graphComponents.elementAt(i);
         if(comp instanceof EdgesGraphComponent)
         {
            ((EdgesGraphComponent)comp).setBounds(getWidth(), getHeight());
         }

         if(_isPrinting && comp instanceof EdgesGraphComponent)
         {
            // When printing edges are not painted
         }
         else
         {
            comp.paint(g);
         }
      }
   }

   public void putGraphComponents(GraphComponent[] graphComponents)
   {
      for (int i = 0; i < graphComponents.length; i++)
      {
         if(false == _graphComponents.contains(graphComponents[i]))
         {
            if(graphComponents[i] instanceof ConstraintView)
            {
               ((ConstraintView)graphComponents[i]).addConstraintViewListener(_constraintViewListener);
            }

            _graphComponents.add(graphComponents[i]);
         }
      }
   }

   public void removeGraphComponents(GraphComponent[] graphComponents)
   {
      _graphComponents.removeAll(Arrays.asList(graphComponents));
   }

   public Vector getGraphComponents()
   {
      return _graphComponents;
   }


   public Dimension getRequiredSize()
   {
      Dimension reqSize = super.getRequiredSize();
      for (int i = 0; i < _graphComponents.size(); i++)
      {
         GraphComponent graphComponent = (GraphComponent) _graphComponents.elementAt(i);
         Dimension buf = graphComponent.getRequiredSize();

         if(buf.width > reqSize.width)
         {
            reqSize.width = buf.width;
         }

         if(buf.height > reqSize.height)
         {
            reqSize.height = buf.height;
         }
      }

      return reqSize;

   }

   ////////////////////////////////////////////////////////////////////////////////////////
   // Printing
   public void initPrint(double formatWidth, double formatHeight, double formatScale)
   {
      _formatWidth = formatWidth;
      _formatHeight = formatHeight;
      _formatScale = formatScale;
   }


   public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException
   {
      int pixelByCm = (int) (Toolkit.getDefaultToolkit().getScreenResolution() * EdgesGraphComponent.CM_BY_INCH + 0.5);
      double edgesWitdthInPixel = _formatWidth * pixelByCm * _formatScale;
      double edgesHeightInPixel = _formatHeight * pixelByCm * _formatScale;


      int pageCountHorizontal = getPageCountHorizontal(edgesWitdthInPixel);
      int pageCountVertical = getPageCountVertical(edgesHeightInPixel);


      if(pageIndex >= pageCountHorizontal * pageCountVertical)
      {
         return Printable.NO_SUCH_PAGE;
      }

      Graphics2D g2d = (Graphics2D) graphics;

      AffineTransform oldTransform = g2d.getTransform();

      boolean origDoubleBufferingEnabled = RepaintManager.currentManager(this).isDoubleBufferingEnabled();

      try
      {
         _isPrinting = true;
         RepaintManager.currentManager(this).setDoubleBufferingEnabled(false);

         double tx = -getPageWidthInPixel(pageFormat) * (pageIndex % pageCountHorizontal) + pageFormat.getImageableX();
         double ty = -getPageHeightInPixel(pageFormat) * (pageIndex / pageCountHorizontal) + pageFormat.getImageableY();

         g2d.translate(tx, ty);

         double sx = getPageWidthInPixel(pageFormat) / edgesWitdthInPixel;
         double sy = getPageHeightInPixel(pageFormat) / edgesHeightInPixel;

         g2d.scale(sx, sy);

         paintGraphComponents(g2d);
         super.paintChildren(g2d);

      }
      finally
      {
         g2d.setTransform(oldTransform);
         RepaintManager.currentManager(this).setDoubleBufferingEnabled(origDoubleBufferingEnabled);
         _isPrinting = false;
      }

      return Printable.PAGE_EXISTS;
   }

   private double getPageHeightInPixel(PageFormat pageFormat)
   {
      return pageFormat.getImageableHeight();
   }

   private double getPageWidthInPixel(PageFormat pageFormat)
   {
      return pageFormat.getImageableWidth();
   }



   public int getPageCountHorizontal(double pageWidthInPixel)
   {
      return roundPageCount(getRequiredSize().width / pageWidthInPixel);
   }

   public int getPageCountVertical(double pageHeightInPixel)
   {
      return roundPageCount(getRequiredSize().height / pageHeightInPixel);
   }

   private int roundPageCount(double d)
   {
      return 0 < d - (int)d ? (int)(d+1) : (int)d;
   }
   //
   ///////////////////////////////////////////////////////////////////////////////////////


}
