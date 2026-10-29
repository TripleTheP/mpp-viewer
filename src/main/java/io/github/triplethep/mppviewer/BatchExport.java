package io.github.triplethep.mppviewer;

import io.github.triplethep.mppviewer.core.ColumnDef;
import io.github.triplethep.mppviewer.core.ProjectDocument;
import io.github.triplethep.mppviewer.export.PdfExporter;
import io.github.triplethep.mppviewer.export.PngExporter;
import io.github.triplethep.mppviewer.export.TableExport;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.print.PrintLayout;
import io.github.triplethep.mppviewer.print.PrintSettings;
import io.github.triplethep.mppviewer.render.ViewKind;
import io.github.triplethep.mppviewer.render.ViewSnapshot;
import io.github.triplethep.mppviewer.ui.view.ProjectViewModel;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Export ohne Fenster, z. B. für Skripte oder geplante Aufgaben:
 * <pre>MPP-Viewer.exe --export Projekt.mpp Projekt.pdf</pre>
 * Das Format ergibt sich aus der Endung der Zieldatei (.pdf, .xlsx, .csv, .png).
 */
final class BatchExport
{
   private BatchExport()
   {
   }

   /**
    * @return Exit-Code (0 = Erfolg)
    */
   static int run(String input, String output)
   {
      try
      {
         ProjectDocument doc = ProjectDocument.load(new File(input));
         ProjectViewModel model = new ProjectViewModel(doc);
         ViewKind kind = model.getKind().isResourceView() ? ViewKind.GANTT : model.getKind();
         model.setKind(kind);
         List<ColumnDef> columns = model.getTaskColumns();
         int inPane = columnsInPane(columns, model.getDividerLocation());
         ViewSnapshot snap = new ViewSnapshot(doc, kind, model.getOutline(), columns, inPane, model.getOptions().copy(), model.getTimeline(), model.getFormats());
         File out = new File(output);
         String name = out.getName().toLowerCase(Locale.ROOT);
         if (name.endsWith(".pdf"))
         {
            PdfExporter.exportPages(new PrintLayout(snap, PrintSettings.defaults(I18n.language())), doc.getTitle(), out);
         }
         else if (name.endsWith(".xlsx"))
         {
            TableExport.exportXlsx(snap, columns, model.getResourceColumns(), out);
         }
         else if (name.endsWith(".csv"))
         {
            TableExport.exportCsv(snap, columns, out);
         }
         else if (name.endsWith(".png"))
         {
            PngExporter.export(snap, columns.subList(0, inPane), 2.0, out);
         }
         else
         {
            System.err.println("Unbekanntes Zielformat: " + output + " (erlaubt: .pdf, .xlsx, .csv, .png)");
            return 2;
         }
         System.out.println(out.getAbsolutePath());
         return 0;
      }
      catch (Exception ex)
      {
         System.err.println("Export fehlgeschlagen: " + ex);
         return 1;
      }
   }

   private static int columnsInPane(List<ColumnDef> columns, int divider)
   {
      if (divider <= 0)
      {
         return Math.min(columns.size(), 7);
      }
      int x = 0;
      int count = 0;
      for (ColumnDef c : columns)
      {
         x += c.getWidth();
         if (x <= divider + 4)
         {
            count++;
         }
      }
      return Math.max(1, count);
   }
}
