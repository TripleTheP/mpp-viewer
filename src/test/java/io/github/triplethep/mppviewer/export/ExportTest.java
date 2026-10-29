package io.github.triplethep.mppviewer.export;

import io.github.triplethep.mppviewer.SampleProject;
import io.github.triplethep.mppviewer.core.ColumnSets;
import io.github.triplethep.mppviewer.core.Formats;
import io.github.triplethep.mppviewer.core.ProjectDocument;
import io.github.triplethep.mppviewer.core.TaskOutline;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.i18n.Language;
import io.github.triplethep.mppviewer.print.PrintLayout;
import io.github.triplethep.mppviewer.print.PrintSettings;
import io.github.triplethep.mppviewer.render.GanttOptions;
import io.github.triplethep.mppviewer.render.Timeline;
import io.github.triplethep.mppviewer.render.ViewKind;
import io.github.triplethep.mppviewer.render.ViewSnapshot;
import io.github.triplethep.mppviewer.render.ZoomLevel;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportTest
{
   private static ProjectDocument doc;

   @TempDir
   Path tmp;

   @BeforeAll
   static void load() throws Exception
   {
      I18n.setLanguage(Language.DE);
      doc = ProjectDocument.of(new File("Beispielprojekt.xml"), SampleProject.create());
   }

   private static ViewSnapshot snapshot(ViewKind kind)
   {
      TaskOutline outline = new TaskOutline(doc, false);
      GanttOptions options = new GanttOptions();
      options.tracking = kind == ViewKind.TRACKING_GANTT;
      var columns = kind.isResourceView() ? ColumnSets.defaultResourceColumns() : ColumnSets.defaultTaskColumns();
      return new ViewSnapshot(doc, kind, outline, columns, 7, options, Timeline.forProject(doc, ZoomLevel.DEFAULT.pxPerDay()), Formats.current(doc.getProperties()));
   }

   @Test
   void pdfPagesContainTitleAndTasks() throws Exception
   {
      PrintLayout layout = new PrintLayout(snapshot(ViewKind.GANTT), PrintSettings.defaults(Language.DE));
      assertTrue(layout.getNumberOfPages() >= 1);
      File pdf = tmp.resolve("plan.pdf").toFile();
      PdfExporter.exportPages(layout, doc.getTitle(), pdf);
      try (PDDocument d = Loader.loadPDF(pdf))
      {
         assertEquals(layout.getNumberOfPages(), d.getNumberOfPages());
         assertEquals(doc.getTitle(), d.getDocumentInformation().getTitle());
      }
   }

   @Test
   void pdfSinglePageTextIsSelectableWhenFontIsAvailable() throws Exception
   {
      File pdf = tmp.resolve("poster.pdf").toFile();
      PdfExporter.exportSinglePage(snapshot(ViewKind.TRACKING_GANTT), ColumnSets.defaultTaskColumns(), doc.getTitle(), pdf);
      try (PDDocument d = Loader.loadPDF(pdf))
      {
         assertEquals(1, d.getNumberOfPages());
         String text = new PDFTextStripper().getText(d);
         // Ohne eingebettete Schrift wird der Text als Pfad gezeichnet – dann gibt es nichts zu prüfen
         if (!text.isBlank())
         {
            assertTrue(text.contains("Anforderungskatalog"), text);
         }
      }
   }

   @Test
   void pngHasExpectedSize() throws Exception
   {
      File png = tmp.resolve("plan.png").toFile();
      double scale = PngExporter.export(snapshot(ViewKind.GANTT), ColumnSets.defaultTaskColumns(), 1.0, png);
      BufferedImage img = ImageIO.read(png);
      assertNotNull(img);
      assertEquals(1.0, scale, 1e-9);
      assertEquals(40 + doc.getTasks().size() * 20, img.getHeight());
   }

   @Test
   void xlsxContainsAllSheets() throws Exception
   {
      File xlsx = tmp.resolve("plan.xlsx").toFile();
      TableExport.exportXlsx(snapshot(ViewKind.GANTT), ColumnSets.defaultTaskColumns(), ColumnSets.defaultResourceColumns(), xlsx);
      try (ZipFile zip = new ZipFile(xlsx))
      {
         assertNotNull(zip.getEntry("xl/workbook.xml"));
         assertNotNull(zip.getEntry("xl/worksheets/sheet3.xml"));
         String sheet1 = new String(zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).readAllBytes(), StandardCharsets.UTF_8);
         assertTrue(sheet1.contains("Schnittstelle zum ERP-System"));
         assertTrue(sheet1.contains("outlineLevel=\"1\""));
         String workbook = new String(zip.getInputStream(zip.getEntry("xl/workbook.xml")).readAllBytes(), StandardCharsets.UTF_8);
         assertTrue(workbook.contains("Vorgänge"));
      }
   }

   @Test
   void csvUsesSemicolonAndBom() throws Exception
   {
      File csv = tmp.resolve("plan.csv").toFile();
      TableExport.exportCsv(snapshot(ViewKind.GANTT), ColumnSets.defaultTaskColumns(), csv);
      List<String> lines = Files.readAllLines(csv.toPath(), StandardCharsets.UTF_8);
      assertTrue(lines.get(0).startsWith("﻿Nr.;Indikatoren;Vorgangsmodus;Vorgangsname;Dauer;Anfang;Ende"), lines.get(0));
      assertEquals(doc.getTasks().size() + 1, lines.size());
      assertTrue(lines.stream().anyMatch(l -> l.contains("Fachkonzept\";9 Tage")));
   }

   @Test
   void resourceViewExports() throws Exception
   {
      File csv = tmp.resolve("res.csv").toFile();
      TableExport.exportCsv(snapshot(ViewKind.RESOURCE_SHEET), ColumnSets.defaultResourceColumns(), csv);
      String content = Files.readString(csv.toPath(), StandardCharsets.UTF_8);
      assertTrue(content.contains("Projektleitung"));
      assertTrue(content.contains("95,00 €/Std."));
      PrintLayout layout = new PrintLayout(snapshot(ViewKind.RESOURCE_SHEET), PrintSettings.defaults(Language.DE));
      assertEquals(1, layout.getNumberOfPages());
   }

   @Test
   void xlsxSerialDates()
   {
      assertEquals(46321.0, XlsxWriter.serial(java.time.LocalDateTime.of(2026, 10, 26, 0, 0)), 1e-9);
      assertEquals("A", XlsxWriter.columnName(0));
      assertEquals("Z", XlsxWriter.columnName(25));
      assertEquals("AA", XlsxWriter.columnName(26));
   }
}
