package io.github.triplethep.mppviewer.ui.view;

import io.github.triplethep.mppviewer.core.ColumnDef;
import io.github.triplethep.mppviewer.render.CellContent;
import io.github.triplethep.mppviewer.render.CellPainter;
import io.github.triplethep.mppviewer.render.Indicator;
import io.github.triplethep.mppviewer.render.Style;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Schreibgeschützte Tabelle im Project-Stil (graue Nr.-Spalte, gezeichnete Zellen, Gliederung). Wird für Vorgangs-
 * und Ressourcentabellen verwendet.
 */
public class SheetTable extends JTable
{
   private static final long serialVersionUID = 1L;

   private final Model model;

   /**
    * @param cells liefert den Zelleninhalt für (Zeile, Spalte)
    */
   public SheetTable(List<ColumnDef> columns, IntSupplierRows rows, BiFunction<Integer, ColumnDef, CellContent> cells)
   {
      this.model = new Model(columns, rows, cells);
      setModel(model);
      setAutoCreateColumnsFromModel(false);
      rebuildColumns();
      setRowHeight(Style.ROW_HEIGHT);
      setShowGrid(false);
      setIntercellSpacing(new Dimension(0, 0));
      setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      setRowSelectionAllowed(true);
      setColumnSelectionAllowed(false);
      setFillsViewportHeight(true);
      setBackground(java.awt.Color.WHITE);
      getTableHeader().setReorderingAllowed(false);
      getTableHeader().setDefaultRenderer(new HeaderRenderer());
      installHeaderClicks();
      getTableHeader().setPreferredSize(new Dimension(10, Style.HEADER_HEIGHT));
      setDefaultRenderer(Object.class, new Renderer());
      putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
      setToolTipText("");
   }

   /**
    * Liefert die aktuelle Zeilenanzahl.
    */
   @FunctionalInterface
   public interface IntSupplierRows
   {
      int count();
   }

   public List<ColumnDef> getColumnDefs()
   {
      return model.columns;
   }

   public void setColumnDefs(List<ColumnDef> columns)
   {
      model.columns = columns;
      model.fireTableStructureChanged();
      rebuildColumns();
   }

   private void rebuildColumns()
   {
      TableColumnModel cm = getColumnModel();
      while (cm.getColumnCount() > 0)
      {
         cm.removeColumn(cm.getColumn(0));
      }
      for (int i = 0; i < model.columns.size(); i++)
      {
         ColumnDef def = model.columns.get(i);
         TableColumn col = new TableColumn(i, def.getWidth());
         col.setHeaderValue(def);
         col.setMinWidth(12);
         col.setPreferredWidth(def.getWidth());
         col.setWidth(def.getWidth());
         cm.addColumn(col);
      }
   }

   /**
    * Spaltendefinitionen mit den aktuellen (ggf. vom Benutzer geänderten) Breiten.
    */
   public List<ColumnDef> currentColumnDefs()
   {
      List<ColumnDef> result = new java.util.ArrayList<>();
      TableColumnModel cm = getColumnModel();
      for (int i = 0; i < cm.getColumnCount(); i++)
      {
         TableColumn col = cm.getColumn(i);
         ColumnDef def = (ColumnDef) col.getHeaderValue();
         result.add(def.withWidth(col.getWidth()));
      }
      return result;
   }

   public void refresh()
   {
      model.fireTableDataChanged();
   }

   public CellContent contentAt(int row, int column)
   {
      ColumnDef def = (ColumnDef) getColumnModel().getColumn(column).getHeaderValue();
      return model.cells.apply(Integer.valueOf(row), def);
   }

   /**
    * Unter der letzten Zeile leere Rasterzeilen zeichnen, wie Project es tut.
    */
   @Override
   protected void paintComponent(Graphics g0)
   {
      super.paintComponent(g0);
      int rows = getRowCount();
      int top = rows * getRowHeight();
      Rectangle clip = g0.getClipBounds();
      int bottom = clip == null ? getHeight() : clip.y + clip.height;
      if (bottom <= top)
      {
         return;
      }
      Graphics2D g = (Graphics2D) g0.create();
      try
      {
         TableColumnModel cm = getColumnModel();
         int x = 0;
         for (int c = 0; c < cm.getColumnCount(); c++)
         {
            int w = cm.getColumn(c).getWidth();
            ColumnDef def = (ColumnDef) cm.getColumn(c).getHeaderValue();
            if (def.getKind() == ColumnDef.Kind.ID)
            {
               g.setColor(Style.ID_BG);
               g.fillRect(x, top, w, bottom - top);
            }
            g.setColor(def.getKind() == ColumnDef.Kind.ID ? Style.HEADER_BORDER : Style.GRID);
            g.drawLine(x + w - 1, top, x + w - 1, bottom);
            x += w;
         }
         g.setColor(Style.GRID);
         for (int y = top + getRowHeight() - 1; y < bottom; y += getRowHeight())
         {
            g.drawLine(0, y, x, y);
         }
      }
      finally
      {
         g.dispose();
      }
   }

   @Override
   public boolean isCellEditable(int row, int column)
   {
      return false;
   }

   @Override
   public String getToolTipText(MouseEvent e)
   {
      Point p = e.getPoint();
      int row = rowAtPoint(p);
      int col = columnAtPoint(p);
      if (row < 0 || col < 0)
      {
         return null;
      }
      CellContent c = contentAt(row, col);
      if (!c.icons().isEmpty())
      {
         return tooltipForIcons(row, c);
      }
      String text = c.text();
      if (text == null || text.isEmpty())
      {
         return null;
      }
      Rectangle r = getCellRect(row, col, false);
      FontMetrics fm = getFontMetrics(c.bold() ? Style.TABLE_FONT_BOLD : Style.TABLE_FONT);
      if (fm.stringWidth(text) + CellPainter.indentWidth(c) + 6 > r.width)
      {
         return text;
      }
      return null;
   }

   /**
    * Tooltip für Symbolspalten; Unterklassen ergänzen z. B. den Notiztext.
    */
   protected String tooltipForIcons(int row, CellContent c)
   {
      StringBuilder sb = new StringBuilder("<html>");
      for (Indicator i : c.icons())
      {
         sb.append(io.github.triplethep.mppviewer.i18n.I18n.t(i.key())).append("<br>");
      }
      return sb.append("</html>").toString();
   }

   @Override
   protected JTableHeader createDefaultTableHeader()
   {
      return new JTableHeader(columnModel)
      {
         private static final long serialVersionUID = 1L;

         @Override
         public String getToolTipText(MouseEvent e)
         {
            int col = columnAtPoint(e.getPoint());
            if (col < 0)
            {
               return null;
            }
            Object value = getColumnModel().getColumn(col).getHeaderValue();
            return value instanceof ColumnDef def ? def.getTitle() : null;
         }
      };
   }

   // ------------------------------------------------------------------ Modell und Renderer

   private static final class Model extends AbstractTableModel
   {
      private static final long serialVersionUID = 1L;
      private List<ColumnDef> columns;
      private final IntSupplierRows rows;
      private final BiFunction<Integer, ColumnDef, CellContent> cells;

      Model(List<ColumnDef> columns, IntSupplierRows rows, BiFunction<Integer, ColumnDef, CellContent> cells)
      {
         this.columns = columns;
         this.rows = rows;
         this.cells = cells;
      }

      @Override
      public int getRowCount()
      {
         return rows.count();
      }

      @Override
      public int getColumnCount()
      {
         return columns.size();
      }

      @Override
      public String getColumnName(int column)
      {
         return columns.get(column).getTitle();
      }

      @Override
      public Object getValueAt(int rowIndex, int columnIndex)
      {
         return Integer.valueOf(rowIndex);
      }
   }

   private final class Renderer extends JComponent implements TableCellRenderer
   {
      private static final long serialVersionUID = 1L;
      private CellContent content;
      private boolean selected;

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
      {
         ColumnDef def = (ColumnDef) table.getColumnModel().getColumn(column).getHeaderValue();
         this.content = model.cells.apply(Integer.valueOf(row), def);
         this.selected = isSelected;
         return this;
      }

      @Override
      protected void paintComponent(Graphics g)
      {
         if (content != null)
         {
            CellPainter.paintCell((Graphics2D) g, 0, 0, getWidth(), getHeight(), content, selected, true, true);
         }
      }

      // Leistungsoptimierung wie bei DefaultTableCellRenderer
      @Override
      public void invalidate()
      {
      }

      @Override
      public void validate()
      {
      }

      @Override
      public void revalidate()
      {
      }

      @Override
      public void repaint(long tm, int x, int y, int width, int height)
      {
      }

      @Override
      public void repaint(Rectangle r)
      {
      }

      @Override
      public void repaint()
      {
      }

      @Override
      protected void firePropertyChange(String propertyName, Object oldValue, Object newValue)
      {
      }
   }

   /**
    * AutoFilter-Pfeile in den Spaltenköpfen (wie in Project, wenn „AutoFilter anzeigen“ aktiv ist).
    */
   public interface AutoFilterSupport
   {
      boolean isEnabled();

      boolean isFiltered(ColumnDef column);

      /** 1 = aufsteigend, -1 = absteigend, 0 = nicht nach dieser Spalte sortiert */
      int sortDirection(ColumnDef column);

      void open(ColumnDef column, Component header, Rectangle cell);
   }

   private static final int ARROW_ZONE = 16;
   private AutoFilterSupport autoFilter;

   public void setAutoFilterSupport(AutoFilterSupport support)
   {
      this.autoFilter = support;
      getTableHeader().repaint();
   }

   private void installHeaderClicks()
   {
      getTableHeader().addMouseListener(new java.awt.event.MouseAdapter()
      {
         @Override
         public void mouseClicked(MouseEvent e)
         {
            if (autoFilter == null || !autoFilter.isEnabled() || !javax.swing.SwingUtilities.isLeftMouseButton(e))
            {
               return;
            }
            JTableHeader header = getTableHeader();
            if (header.getCursor().getType() == java.awt.Cursor.E_RESIZE_CURSOR)
            {
               return;
            }
            int col = header.columnAtPoint(e.getPoint());
            if (col < 0)
            {
               return;
            }
            Rectangle r = header.getHeaderRect(col);
            ColumnDef def = (ColumnDef) getColumnModel().getColumn(col).getHeaderValue();
            if (def.getKind() == ColumnDef.Kind.INDICATORS || def.getKind() == ColumnDef.Kind.ID)
            {
               return;
            }
            if (e.getX() >= r.x + r.width - ARROW_ZONE - 4)
            {
               autoFilter.open(def, header, r);
            }
         }
      });
   }

   private final class HeaderRenderer extends JComponent implements TableCellRenderer
   {
      private static final long serialVersionUID = 1L;
      private ColumnDef def;

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
      {
         this.def = value instanceof ColumnDef d ? d : null;
         return this;
      }

      @Override
      protected void paintComponent(Graphics g0)
      {
         if (def == null)
         {
            return;
         }
         Graphics2D g = (Graphics2D) g0;
         Indicator icon = def.getKind() == ColumnDef.Kind.INDICATORS ? Indicator.INFO : null;
         String title = def.getKind() == ColumnDef.Kind.ID || icon != null ? null : def.getHeaderTitle();
         boolean arrows = autoFilter != null && autoFilter.isEnabled() && icon == null && def.getKind() != ColumnDef.Kind.ID;
         double textWidth = arrows && def.getKind() != ColumnDef.Kind.ID ? getWidth() - ARROW_ZONE + 2 : getWidth();
         CellPainter.paintHeader(g, 0, 0, getWidth(), getHeight(), null, icon);
         if (title != null)
         {
            java.awt.Shape clip = g.getClip();
            g.clip(new java.awt.geom.Rectangle2D.Double(0, 0, textWidth, getHeight() - 1));
            CellPainter.paintHeader(g, 0, 0, textWidth, getHeight(), title, null);
            g.setClip(clip);
            g.setColor(Style.HEADER_BG);
            g.fill(new java.awt.geom.Rectangle2D.Double(textWidth - 1, 0, 1, getHeight() - 1));
         }
         if (arrows)
         {
            paintArrow(g, autoFilter.isFiltered(def), autoFilter.sortDirection(def));
         }
      }

      private void paintArrow(Graphics2D g, boolean filtered, int sort)
      {
         g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
         double x = getWidth() - ARROW_ZONE + 2;
         double y = getHeight() - 16;
         g.setColor(new java.awt.Color(0x606060));
         if (filtered)
         {
            // Trichter
            java.awt.geom.Path2D f = new java.awt.geom.Path2D.Double();
            f.moveTo(x + 1, y + 2);
            f.lineTo(x + 11, y + 2);
            f.lineTo(x + 7, y + 7);
            f.lineTo(x + 7, y + 12);
            f.lineTo(x + 5, y + 11);
            f.lineTo(x + 5, y + 7);
            f.closePath();
            g.setColor(Style.ACCENT);
            g.fill(f);
         }
         else
         {
            java.awt.geom.Path2D a = new java.awt.geom.Path2D.Double();
            a.moveTo(x + 2, y + 6);
            a.lineTo(x + 10, y + 6);
            a.lineTo(x + 6, y + 10);
            a.closePath();
            g.fill(a);
         }
         if (sort != 0)
         {
            double sx = x - 7;
            g.setColor(Style.ACCENT);
            g.setStroke(new java.awt.BasicStroke(1.4f));
            g.draw(new java.awt.geom.Line2D.Double(sx + 3, y + 2, sx + 3, y + 11));
            java.awt.geom.Path2D h = new java.awt.geom.Path2D.Double();
            if (sort > 0)
            {
               h.moveTo(sx, y + 5);
               h.lineTo(sx + 3, y + 1.5);
               h.lineTo(sx + 6, y + 5);
            }
            else
            {
               h.moveTo(sx, y + 8);
               h.lineTo(sx + 3, y + 11.5);
               h.lineTo(sx + 6, y + 8);
            }
            g.draw(h);
         }
      }

      @Override
      public Dimension getPreferredSize()
      {
         return new Dimension(def == null ? 50 : def.getWidth(), Style.HEADER_HEIGHT);
      }
   }
}
