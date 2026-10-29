package io.github.triplethep.mppviewer.render;

import io.github.triplethep.mppviewer.core.ColumnDef;
import org.mpxj.Resource;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Zeichnet eine komplette Ansicht – Tabelle links, Gantt-Diagramm rechts – auf eine „Leinwand“. Druck, PDF und
 * PNG schneiden daraus ihre Seiten bzw. ihr Bild.
 * <p>
 * Koordinaten: x = 0 ist der linke Tabellenrand, das Diagramm beginnt bei {@link #getTableWidth()}. Die Kopfzeile
 * ist {@link Style#HEADER_HEIGHT} hoch, darunter folgen die Zeilen.
 */
public final class CanvasPainter
{
   private static final double DIVIDER = 3;

   private final ViewSnapshot view;
   private final List<ColumnDef> columns;
   private final Timeline timeline;
   private final GanttPainter gantt;
   private final CellPresenter presenter;
   private final double tableWidth;

   /**
    * @param columns  zu zeichnende Spalten (leer = nur Diagramm)
    * @param timeline Zeitleiste für das Diagramm, {@code null} für reine Tabellenansichten
    */
   public CanvasPainter(ViewSnapshot view, List<ColumnDef> columns, Timeline timeline)
   {
      this.view = view;
      this.columns = columns;
      this.timeline = view.kind().hasChart() ? timeline : null;
      this.presenter = view.presenter();
      this.gantt = this.timeline == null ? null : new GanttPainter(view.doc(), view.outline(), this.timeline, view.options(), view.formats());
      double w = 0;
      for (ColumnDef c : columns)
      {
         w += c.getWidth();
      }
      this.tableWidth = w;
   }

   public double getTableWidth()
   {
      return tableWidth;
   }

   public double getChartWidth()
   {
      return timeline == null ? 0 : timeline.getWidth();
   }

   public double getWidth()
   {
      return tableWidth + (timeline == null ? 0 : DIVIDER + getChartWidth());
   }

   public double getChartOffset()
   {
      return tableWidth + DIVIDER;
   }

   public int getRowCount()
   {
      return view.rowCount();
   }

   public double getBodyHeight(int rows)
   {
      return rows * (double) Style.ROW_HEIGHT;
   }

   /**
    * Zeichnet den Ausschnitt x0..x1 mit Kopfzeile und den Zeilen rowFrom..rowTo (inklusive). Die Kopfzeile beginnt
    * bei y = 0, Zeile rowFrom bei y = {@link Style#HEADER_HEIGHT}.
    */
   public void paint(Graphics2D g, double x0, double x1, int rowFrom, int rowTo)
   {
      Shape clip = g.getClip();
      double headerH = Style.HEADER_HEIGHT;
      double bodyH = (rowTo - rowFrom + 1) * (double) Style.ROW_HEIGHT;

      // ---- Tabelle
      if (tableWidth > 0 && x0 < tableWidth)
      {
         double cx = 0;
         for (int ci = 0; ci < columns.size(); ci++)
         {
            ColumnDef col = columns.get(ci);
            double w = col.getWidth();
            if (cx + w >= x0 && cx <= x1)
            {
               Indicator headerIcon = col.getKind() == ColumnDef.Kind.INDICATORS ? Indicator.INFO : null;
               String title = col.getKind() == ColumnDef.Kind.ID ? "" : col.getHeaderTitle();
               if (col.getKind() == ColumnDef.Kind.INDICATORS)
               {
                  title = null;
               }
               paintIndicatorHeaderOrTitle(g, cx, w, title, headerIcon);
               for (int r = rowFrom; r <= rowTo; r++)
               {
                  double y = headerH + (r - rowFrom) * (double) Style.ROW_HEIGHT;
                  CellContent content = cell(r, col);
                  CellPainter.paintCell(g, cx, y, w, Style.ROW_HEIGHT, content, false, true, true);
               }
            }
            cx += w;
         }
      }

      // ---- Diagramm
      if (timeline != null && x1 > tableWidth)
      {
         double offset = getChartOffset();
         // Teiler zwischen Tabelle und Diagramm
         g.setColor(Style.HEADER_BORDER);
         g.fill(new Rectangle2D.Double(tableWidth, 0, DIVIDER, headerH + bodyH));

         double cx0 = Math.max(0, x0 - offset);
         double cx1 = Math.min(getChartWidth(), x1 - offset);
         if (cx1 > cx0)
         {
            Graphics2D hg = (Graphics2D) g.create();
            hg.translate(offset, 0);
            hg.clip(new Rectangle2D.Double(cx0, 0, cx1 - cx0, headerH));
            TimescalePainter.paint(hg, timeline, view.formats(), cx0, cx1);
            hg.dispose();

            Graphics2D bg = (Graphics2D) g.create();
            bg.translate(offset, headerH - rowFrom * (double) Style.ROW_HEIGHT);
            bg.clip(new Rectangle2D.Double(cx0, rowFrom * (double) Style.ROW_HEIGHT, cx1 - cx0, bodyH));
            gantt.paint(bg, cx0, cx1, rowFrom, rowTo, null);
            bg.dispose();
         }
      }

      // Rahmen unten
      g.setColor(Style.HEADER_BORDER);
      g.draw(new Line2D.Double(x0, headerH + bodyH - 0.5, Math.min(x1, getWidth()), headerH + bodyH - 0.5));
      g.setClip(clip);
   }

   private void paintIndicatorHeaderOrTitle(Graphics2D g, double x, double w, String title, Indicator icon)
   {
      CellPainter.paintHeader(g, x, 0, w, Style.HEADER_HEIGHT, title, title == null ? icon : null);
   }

   private CellContent cell(int row, ColumnDef col)
   {
      if (view.kind().isResourceView())
      {
         List<Resource> resources = view.resources();
         return row < resources.size() ? presenter.resource(resources.get(row), col) : CellContent.plain("", col.getAlign());
      }
      return row < view.taskRows().size() ? presenter.task(view.taskRows().get(row), col) : CellContent.plain("", col.getAlign());
   }
}
