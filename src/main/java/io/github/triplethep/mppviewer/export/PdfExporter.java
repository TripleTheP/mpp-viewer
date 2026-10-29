package io.github.triplethep.mppviewer.export;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2DFontTextDrawer;
import io.github.triplethep.mppviewer.core.ColumnDef;
import io.github.triplethep.mppviewer.print.PrintLayout;
import io.github.triplethep.mppviewer.print.PrintSettings;
import io.github.triplethep.mppviewer.render.CanvasPainter;
import io.github.triplethep.mppviewer.render.Style;
import io.github.triplethep.mppviewer.render.ViewSnapshot;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

/**
 * PDF-Export als Vektorgrafik – entweder Seite für Seite wie im Druck oder als ein einziges großes Blatt.
 */
public final class PdfExporter
{
   /** Größte Seitenkante, die Adobe Reader zuverlässig darstellt (200 Zoll). */
   private static final double MAX_PAGE = 14_400;

   private PdfExporter()
   {
   }

   /**
    * Exportiert genau die Seiten der Seitenansicht.
    */
   public static void exportPages(PrintLayout layout, String title, File file) throws IOException
   {
      PrintSettings s = layout.getSettings();
      float pw = (float) s.pageWidth();
      float ph = (float) s.pageHeight();
      try (PDDocument doc = new PDDocument())
      {
         info(doc, title);
         try (PdfBoxGraphics2DFontTextDrawer fonts = PdfFonts.createDrawer())
         {
            for (int i = 0; i < layout.getNumberOfPages(); i++)
            {
               PDPage page = new PDPage(new PDRectangle(pw, ph));
               doc.addPage(page);
               PdfBoxGraphics2D g = new PdfBoxGraphics2D(doc, pw, ph);
               g.setFontTextDrawer(fonts);
               Graphics2D gg = (Graphics2D) g.create();
               gg.translate(s.margin, s.margin);
               layout.paintPage(gg, i);
               gg.dispose();
               g.dispose();
               PDFormXObject form = g.getXFormObject();
               try (PDPageContentStream cs = new PDPageContentStream(doc, page))
               {
                  cs.drawForm(form);
               }
            }
         }
         doc.save(file);
      }
   }

   /**
    * Ganze Ansicht auf einer einzigen Seite (praktisch zum Weitergeben am Bildschirm oder für den Plotter).
    */
   public static void exportSinglePage(ViewSnapshot view, List<ColumnDef> columns, String title, File file) throws IOException
   {
      CanvasPainter canvas = new CanvasPainter(view, columns, ExportTimeline.forView(view));
      double margin = 24;
      double titleBand = 30;
      double cw = canvas.getWidth();
      double chH = Style.HEADER_HEIGHT + canvas.getRowCount() * (double) Style.ROW_HEIGHT;
      double scale = Math.min(1.0, Math.min((MAX_PAGE - 2 * margin) / cw, (MAX_PAGE - 2 * margin - titleBand) / chH));
      float pw = (float) (cw * scale + 2 * margin);
      float ph = (float) (chH * scale + 2 * margin + titleBand);
      try (PDDocument doc = new PDDocument())
      {
         info(doc, title);
         try (PdfBoxGraphics2DFontTextDrawer fonts = PdfFonts.createDrawer())
         {
            PDPage page = new PDPage(new PDRectangle(pw, ph));
            doc.addPage(page);
            PdfBoxGraphics2D g = new PdfBoxGraphics2D(doc, pw, ph);
            g.setFontTextDrawer(fonts);
            Graphics2D gg = (Graphics2D) g.create();
            gg.translate(margin, margin);
            Font font = Style.TABLE_FONT_BOLD.deriveFont(14f);
            gg.setFont(font);
            FontMetrics fm = gg.getFontMetrics();
            gg.setColor(Style.TEXT);
            gg.drawString(title, 0f, (float) fm.getAscent());
            gg.translate(0, titleBand);
            gg.scale(scale, scale);
            canvas.paint(gg, 0, cw, 0, canvas.getRowCount() - 1);
            gg.dispose();
            g.dispose();
            try (PDPageContentStream cs = new PDPageContentStream(doc, page))
            {
               cs.drawForm(g.getXFormObject());
            }
         }
         doc.save(file);
      }
   }

   private static void info(PDDocument doc, String title)
   {
      PDDocumentInformation info = doc.getDocumentInformation();
      info.setTitle(title);
      info.setCreator("MPP-Viewer");
      info.setCreationDate(Calendar.getInstance());
   }
}
