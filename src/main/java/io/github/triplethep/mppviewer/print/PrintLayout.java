package io.github.triplethep.mppviewer.print;

import io.github.triplethep.mppviewer.core.ColumnDef;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.render.CanvasPainter;
import io.github.triplethep.mppviewer.render.LegendPainter;
import io.github.triplethep.mppviewer.render.Style;
import io.github.triplethep.mppviewer.render.Timeline;
import io.github.triplethep.mppviewer.render.ViewSnapshot;
import io.github.triplethep.mppviewer.render.ZoomLevel;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Teilt eine Ansicht auf Druckseiten auf. Dient gleichzeitig als {@link Pageable} für den Drucker und als Quelle
 * für Seitenansicht und PDF-Export – alle drei sehen dadurch identisch aus.
 */
public final class PrintLayout implements Pageable, Printable
{
   private static final double TITLE_BAND = 24;
   private static final double LEGEND_BAND = 58;
   private static final double PAGE_NUMBER_BAND = 16;

   private final ViewSnapshot view;
   private final PrintSettings settings;
   private final CanvasPainter canvas;
   private final PageFormat pageFormat;
   private final double scale;
   private final List<double[]> slices = new ArrayList<>();
   private final int rowsPerPage;
   private final int verticalPages;
   private final List<LegendPainter.Item> legendItems;
   private final String printedOn;

   public PrintLayout(ViewSnapshot view, PrintSettings settings)
   {
      this.view = view;
      this.settings = settings.copy();
      this.legendItems = view.kind().hasChart() && settings.legend ? LegendPainter.itemsFor(view) : List.of();
      this.printedOn = view.formats().date(LocalDate.now());

      List<ColumnDef> cols = settings.allColumns || !view.kind().hasChart() ? view.columns() : view.columns().subList(0, Math.max(1, Math.min(view.columns().size(), view.columnsInPane())));
      double tableWidth = 0;
      for (ColumnDef c : cols)
      {
         tableWidth += c.getWidth();
      }
      Timeline timeline = null;
      if (view.kind().hasChart())
      {
         int pages = settings.scaling == PrintSettings.Scaling.FIT_PAGES_WIDE ? Math.max(1, settings.pagesWide) : 1;
         double available = (settings.pageWidth() - 2 * settings.margin) * pages;
         timeline = printTimeline(view, settings, tableWidth, available);
      }
      this.canvas = new CanvasPainter(view, cols, timeline);

      // Seitenformat mit Rändern
      Paper paper = new Paper();
      double pw = settings.paper.width();
      double ph = settings.paper.height();
      paper.setSize(pw, ph);
      double m = settings.margin;
      paper.setImageableArea(m, m, pw - 2 * m, ph - 2 * m);
      PageFormat pf = new PageFormat();
      pf.setPaper(paper);
      pf.setOrientation(settings.landscape ? PageFormat.LANDSCAPE : PageFormat.PORTRAIT);
      this.pageFormat = pf;

      double contentW = contentWidth();
      double contentH = contentHeight();
      double canvasW = canvas.getWidth();
      int rows = canvas.getRowCount();
      double canvasH = Style.HEADER_HEIGHT + rows * (double) Style.ROW_HEIGHT;

      double s = switch (settings.scaling)
      {
         case PERCENT -> Math.max(10, Math.min(400, settings.percent)) / 100.0;
         case FIT_PAGES_WIDE -> Math.min(1.0, Math.max(1, settings.pagesWide) * contentW / Math.max(1, canvasW));
         case FIT_ONE_PAGE -> Math.min(contentW / Math.max(1, canvasW), contentH / Math.max(1, canvasH));
      };
      this.scale = Math.max(0.02, s);

      // Waagerechte Aufteilung, möglichst an Spaltengrenzen der Tabelle
      double sliceW = contentW / scale;
      double x = 0;
      int guard = 0;
      while (x < canvasW - 0.5 && guard++ < 10_000)
      {
         double end = Math.min(canvasW, x + sliceW);
         if (end < canvas.getTableWidth() && end < canvasW)
         {
            double snapped = lastColumnBoundary(cols, x, end);
            if (snapped > x + 1)
            {
               end = snapped;
            }
         }
         slices.add(new double[]
         {
            x,
            end
         });
         x = end;
      }
      if (slices.isEmpty())
      {
         slices.add(new double[]
         {
            0,
            Math.max(1, canvasW)
         });
      }

      double rowsSpace = contentH / scale - Style.HEADER_HEIGHT;
      this.rowsPerPage = Math.max(1, (int) Math.floor(rowsSpace / Style.ROW_HEIGHT));
      this.verticalPages = Math.max(1, (int) Math.ceil(rows / (double) rowsPerPage));
   }

   private static final double TEXT_SPACE = 140;

   private static Timeline printTimeline(ViewSnapshot view, PrintSettings settings, double tableWidth, double availableWidth)
   {
      Timeline screen = view.timeline();
      LocalDateTime from;
      LocalDateTime to;
      if (settings.range == PrintSettings.Range.DATES && settings.from != null && settings.to != null && !settings.to.isBefore(settings.from))
      {
         from = settings.from.atStartOfDay();
         to = settings.to.plusDays(1).atStartOfDay();
      }
      else
      {
         from = view.doc().getStart().toLocalDate().minusDays(2).atStartOfDay();
         to = view.doc().getFinish().toLocalDate().plusDays(4).atStartOfDay();
      }
      double ppd = switch (settings.timescale)
      {
         case VIEW -> screen.getPxPerDay();
         case AUTO ->
         {
            if (settings.scaling == PrintSettings.Scaling.PERCENT)
            {
               yield screen.getPxPerDay();
            }
            // Zeitskala so wählen, dass die Seite gut gefüllt und die Schrift lesbar bleibt:
            // bevorzugt ca. 90 % Zoom; reicht der Platz dafür nicht, mindestens Wochenauflösung bei ≥ 60 % Zoom
            double days = Math.max(1, java.time.Duration.between(from, to).toMinutes() / 1440.0);
            double fixed = tableWidth + 3 + TEXT_SPACE;
            double fill = (availableWidth / 0.9 - fixed) / days;
            double readable = Math.min(ZoomLevel.MONTHS_WEEKS_M.pxPerDay(), (availableWidth / 0.6 - fixed) / days);
            yield Math.max(0.35, Math.min(48, Math.max(fill, readable)));
         }
         default -> settings.timescale.pxPerDay();
      };
      // Platz für Balkentexte rechts vom letzten Balken
      double extraDays = TEXT_SPACE / ppd;
      to = to.plusMinutes((long) (extraDays * 1440));
      return Timeline.forRange(from, to, ppd, screen.getWeekStart());
   }

   private static double lastColumnBoundary(List<ColumnDef> cols, double start, double end)
   {
      double x = 0;
      double best = -1;
      for (ColumnDef c : cols)
      {
         x += c.getWidth();
         if (x > start + 1 && x <= end)
         {
            best = x;
         }
      }
      return best;
   }

   private double contentWidth()
   {
      return settings.pageWidth() - 2 * settings.margin;
   }

   private double headerBand()
   {
      return settings.title ? TITLE_BAND : 0;
   }

   private double footerBand()
   {
      if (!legendItems.isEmpty())
      {
         return LEGEND_BAND;
      }
      return settings.pageNumbers ? PAGE_NUMBER_BAND : 0;
   }

   private double contentHeight()
   {
      return settings.pageHeight() - 2 * settings.margin - headerBand() - footerBand();
   }

   // ------------------------------------------------------------------ Pageable

   @Override
   public int getNumberOfPages()
   {
      return slices.size() * verticalPages;
   }

   @Override
   public PageFormat getPageFormat(int pageIndex)
   {
      return pageFormat;
   }

   @Override
   public Printable getPrintable(int pageIndex)
   {
      return this;
   }

   public PageFormat getPageFormat()
   {
      return pageFormat;
   }

   public PrintSettings getSettings()
   {
      return settings;
   }

   public double getScale()
   {
      return scale;
   }

   public int getPagesWide()
   {
      return slices.size();
   }

   public int getPagesTall()
   {
      return verticalPages;
   }

   // ------------------------------------------------------------------ Printable

   @Override
   public int print(Graphics graphics, PageFormat pf, int pageIndex)
   {
      if (pageIndex < 0 || pageIndex >= getNumberOfPages())
      {
         return NO_SUCH_PAGE;
      }
      Graphics2D g = (Graphics2D) graphics.create();
      try
      {
         g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
         g.translate(pf.getImageableX(), pf.getImageableY());
         paintPage(g, pageIndex);
      }
      finally
      {
         g.dispose();
      }
      return PAGE_EXISTS;
   }

   /**
    * Zeichnet Seite {@code pageIndex}; der Ursprung liegt in der linken oberen Ecke des bedruckbaren Bereichs.
    */
   public void paintPage(Graphics2D g, int pageIndex)
   {
      int sliceIndex;
      int blockIndex;
      if (settings.order == PrintSettings.Order.DOWN_THEN_OVER)
      {
         sliceIndex = pageIndex / verticalPages;
         blockIndex = pageIndex % verticalPages;
      }
      else
      {
         sliceIndex = pageIndex % slices.size();
         blockIndex = pageIndex / slices.size();
      }
      double contentW = contentWidth();
      double contentH = contentHeight();
      double headerBand = headerBand();

      if (settings.title)
      {
         Font font = Style.TABLE_FONT_BOLD.deriveFont(12f);
         g.setFont(font);
         FontMetrics fm = g.getFontMetrics();
         String title = view.doc().getTitle();
         g.setColor(Style.TEXT);
         g.drawString(title, (float) ((contentW - fm.stringWidth(title)) / 2.0), (float) (fm.getAscent() + 2));
      }

      // Inhalt
      double[] slice = slices.get(sliceIndex);
      int rowFrom = blockIndex * rowsPerPage;
      int rowTo = Math.min(canvas.getRowCount(), rowFrom + rowsPerPage) - 1;
      Graphics2D c = (Graphics2D) g.create();
      c.translate(0, headerBand);
      c.clip(new Rectangle2D.Double(0, 0, contentW, contentH));
      c.scale(scale, scale);
      c.translate(-slice[0], 0);
      c.clip(new Rectangle2D.Double(slice[0], 0, slice[1] - slice[0], Style.HEADER_HEIGHT + Math.max(0, rowTo - rowFrom + 1) * (double) Style.ROW_HEIGHT));
      if (rowTo >= rowFrom)
      {
         canvas.paint(c, slice[0], slice[1], rowFrom, rowTo);
      }
      else
      {
         canvas.paint(c, slice[0], slice[1], 0, -1);
      }
      c.dispose();

      // Fußzeile
      double footerTop = headerBand + contentH;
      if (!legendItems.isEmpty())
      {
         paintLegendFooter(g, footerTop, contentW, pageIndex);
      }
      else if (settings.pageNumbers)
      {
         paintPageNumber(g, footerTop + PAGE_NUMBER_BAND - 4, contentW, pageIndex);
      }
   }

   private void paintLegendFooter(Graphics2D g, double top, double width, int pageIndex)
   {
      double y = top + 6;
      double h = LEGEND_BAND - 8;
      g.setColor(Style.HEADER_BORDER);
      g.draw(new Rectangle2D.Double(0.5, y + 0.5, width - 1, h - 1));

      // Projekt und Datum links
      double infoW = Math.min(200, width * 0.3);
      Font font = Style.BAR_FONT.deriveFont(8.5f);
      g.setFont(font);
      FontMetrics fm = g.getFontMetrics();
      g.setColor(Style.TEXT);
      g.drawString(I18n.t("print.footer.project", view.doc().getTitle()), 6f, (float) (y + 6 + fm.getAscent()));
      g.drawString(I18n.t("print.footer.date", printedOn), 6f, (float) (y + 6 + fm.getAscent() + fm.getHeight() + 1));
      g.setColor(Style.HEADER_BORDER);
      g.draw(new Line2D.Double(infoW, y, infoW, y + h));

      double pageNoW = settings.pageNumbers ? 90 : 0;
      LegendPainter.paint(g, infoW + 10, y + 5, width - infoW - 20 - pageNoW, h - 8, legendItems, view.options().tracking);

      if (settings.pageNumbers)
      {
         g.setColor(Style.HEADER_BORDER);
         g.draw(new Line2D.Double(width - pageNoW, y, width - pageNoW, y + h));
         paintPageNumber(g, y + h / 2 + 4, width, pageIndex);
      }
   }

   private void paintPageNumber(Graphics2D g, double baseline, double width, int pageIndex)
   {
      Font font = Style.BAR_FONT.deriveFont(8.5f);
      g.setFont(font);
      FontMetrics fm = g.getFontMetrics();
      String text = I18n.t("print.page", pageIndex + 1, getNumberOfPages());
      g.setColor(new Color(0x404040));
      g.drawString(text, (float) (width - 8 - fm.stringWidth(text)), (float) baseline);
   }
}
