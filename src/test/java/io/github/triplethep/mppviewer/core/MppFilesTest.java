package io.github.triplethep.mppviewer.core;

import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.i18n.Language;
import io.github.triplethep.mppviewer.print.PrintLayout;
import io.github.triplethep.mppviewer.print.PrintSettings;
import io.github.triplethep.mppviewer.render.CanvasPainter;
import io.github.triplethep.mppviewer.render.GanttOptions;
import io.github.triplethep.mppviewer.render.Style;
import io.github.triplethep.mppviewer.render.Timeline;
import io.github.triplethep.mppviewer.render.ViewKind;
import io.github.triplethep.mppviewer.render.ViewSnapshot;
import io.github.triplethep.mppviewer.render.ZoomLevel;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Öffnet und zeichnet echte .mpp-Dateien aus dem Ordner {@code testdata} (nicht im Repository, da die Beispieldateien
 * fremden Lizenzen unterliegen). Ohne den Ordner wird der Test übersprungen.
 */
class MppFilesTest
{
   @Test
   void openAndRenderAllTestFiles() throws Exception
   {
      String dir = System.getProperty("mppviewer.testdata", "testdata");
      File[] files = new File(dir).listFiles((d, n) -> n.toLowerCase().endsWith(".mpp"));
      Assumptions.assumeTrue(files != null && files.length > 0, "Keine .mpp-Testdateien vorhanden");
      I18n.setLanguage(Language.DE);
      List<String> failures = new ArrayList<>();
      for (File f : files)
      {
         try
         {
            ProjectDocument doc = ProjectDocument.load(f);
            for (ViewKind kind : ViewKind.values())
            {
               TaskOutline outline = new TaskOutline(doc, true);
               GanttOptions options = new GanttOptions();
               options.tracking = kind == ViewKind.TRACKING_GANTT;
               var cols = kind.isResourceView() ? ColumnSets.fromTable(doc.getDefaultResourceTable(), true) : ColumnSets.fromTable(doc.getDefaultTaskTable(), false);
               ViewSnapshot snap = new ViewSnapshot(doc, kind, outline, cols, cols.size(), options, Timeline.forProject(doc, ZoomLevel.DEFAULT.pxPerDay()), Formats.current(doc.getProperties()));
               CanvasPainter canvas = new CanvasPainter(snap, cols, snap.timeline());
               BufferedImage img = new BufferedImage((int) Math.min(4000, canvas.getWidth()), Style.HEADER_HEIGHT + Math.max(1, canvas.getRowCount()) * Style.ROW_HEIGHT, BufferedImage.TYPE_INT_RGB);
               Graphics2D g = img.createGraphics();
               canvas.paint(g, 0, img.getWidth(), 0, canvas.getRowCount() - 1);
               g.dispose();
               PrintLayout layout = new PrintLayout(snap, PrintSettings.defaults(Language.DE));
               BufferedImage page = new BufferedImage(842, 595, BufferedImage.TYPE_INT_RGB);
               Graphics2D pg = page.createGraphics();
               layout.paintPage(pg, 0);
               pg.dispose();
               assertTrue(layout.getNumberOfPages() > 0);
            }
         }
         catch (Exception | AssertionError ex)
         {
            failures.add(f.getName() + ": " + ex);
         }
      }
      assertTrue(failures.isEmpty(), String.join("\n", failures));
   }
}
