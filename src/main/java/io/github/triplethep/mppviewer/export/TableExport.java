package io.github.triplethep.mppviewer.export;

import io.github.triplethep.mppviewer.core.ColumnDef;
import io.github.triplethep.mppviewer.core.Formats;
import io.github.triplethep.mppviewer.core.TaskOutline;
import io.github.triplethep.mppviewer.core.TaskRow;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.i18n.Language;
import io.github.triplethep.mppviewer.render.CellPresenter;
import io.github.triplethep.mppviewer.render.ViewSnapshot;
import org.mpxj.ProjectProperties;
import org.mpxj.Resource;
import org.mpxj.Task;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Export der Tabellen als Excel-Arbeitsmappe oder CSV-Datei. Exportiert werden alle Vorgänge, die der aktuelle
 * Filter zeigt (auch zugeklappte), in der angezeigten Sortierung und mit den Spalten der aktuellen Tabelle.
 */
public final class TableExport
{
   private TableExport()
   {
   }

   // ------------------------------------------------------------------ Excel

   public static void exportXlsx(ViewSnapshot view, List<ColumnDef> taskColumns, List<ColumnDef> resourceColumns, File file) throws IOException
   {
      Formats fmt = view.formats();
      boolean de = fmt.getLanguage() == Language.DE;
      String currencySymbol = view.doc().getCurrencySymbol().replace("\"", "");
      String currencyFormat = de ? "#,##0.00 \"" + currencySymbol + "\"" : "\"" + currencySymbol + "\"#,##0.00";
      XlsxWriter xlsx = new XlsxWriter(de ? "dd.mm.yy" : "m/d/yy", currencyFormat);
      xlsx.setTitle(view.doc().getTitle());
      String author = view.doc().getProperties().getAuthor();
      xlsx.setAuthor(author);

      if (view.kind().isResourceView())
      {
         resourceSheet(xlsx, view, resourceColumns);
         taskSheet(xlsx, view, taskColumns);
      }
      else
      {
         taskSheet(xlsx, view, taskColumns);
         resourceSheet(xlsx, view, resourceColumns);
      }
      projectSheet(xlsx, view);

      try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file.toPath())))
      {
         xlsx.write(out);
      }
   }

   private static void taskSheet(XlsxWriter xlsx, ViewSnapshot view, List<ColumnDef> columns)
   {
      XlsxWriter.Sheet sheet = xlsx.addSheet(I18n.t("export.sheet.tasks"));
      CellPresenter presenter = view.presenter();
      header(sheet, columns);
      for (TaskRow row : allTaskRows(view))
      {
         XlsxWriter.Row r = sheet.addRow().outlineLevel(Math.max(0, row.level() - 1));
         boolean bold = row.hasChildren() || row.isProjectSummary();
         for (ColumnDef col : columns)
         {
            if (col.getKind() == ColumnDef.Kind.NAME)
            {
               String name = row.task().getName() == null ? "" : row.task().getName();
               r.addIndented(name, bold, Math.max(0, row.level() - 1));
               continue;
            }
            Object value = presenter.taskExportValue(row, col);
            addValue(r, value, bold, col);
         }
      }
      sheet.freezeHeader();
      sheet.autoFilter(columns.size());
   }

   private static void resourceSheet(XlsxWriter xlsx, ViewSnapshot view, List<ColumnDef> columns)
   {
      XlsxWriter.Sheet sheet = xlsx.addSheet(I18n.t("export.sheet.resources"));
      CellPresenter presenter = view.presenter();
      header(sheet, columns);
      for (Resource res : view.resources())
      {
         XlsxWriter.Row r = sheet.addRow();
         for (ColumnDef col : columns)
         {
            addValue(r, presenter.resourceExportValue(res, col), false, col);
         }
      }
      sheet.freezeHeader();
      sheet.autoFilter(columns.size());
   }

   private static void projectSheet(XlsxWriter xlsx, ViewSnapshot view)
   {
      XlsxWriter.Sheet sheet = xlsx.addSheet(I18n.t("export.sheet.project"));
      sheet.setColumnWidth(0, 28);
      sheet.setColumnWidth(1, 50);
      ProjectProperties p = view.doc().getProperties();
      Formats fmt = view.formats();
      row(sheet, I18n.t("info.title"), view.doc().getTitle());
      row(sheet, I18n.t("info.file"), view.doc().getFile() == null ? "" : view.doc().getFile().getAbsolutePath());
      row(sheet, I18n.t("info.start"), view.doc().getStart());
      row(sheet, I18n.t("info.finish"), view.doc().getFinish());
      row(sheet, I18n.t("info.statusDate"), p.getStatusDate());
      row(sheet, I18n.t("info.author"), p.getAuthor());
      row(sheet, I18n.t("info.manager"), p.getManager());
      row(sheet, I18n.t("info.company"), p.getCompany());
      row(sheet, I18n.t("info.lastSaved"), p.getLastSaved());
      row(sheet, I18n.t("info.taskCount"), Integer.valueOf(view.doc().getTasks().size()));
      row(sheet, I18n.t("info.resourceCount"), Integer.valueOf(view.doc().getResources().size()));
      row(sheet, I18n.t("info.exported"), fmt.dateTime(LocalDateTime.now()));
   }

   private static void row(XlsxWriter.Sheet sheet, String label, Object value)
   {
      XlsxWriter.Row r = sheet.addRow();
      r.add(label, XlsxWriter.Style.LABEL);
      if (value instanceof LocalDateTime)
      {
         r.add(value, XlsxWriter.Style.DATE);
      }
      else if (value instanceof Number)
      {
         r.add(value, XlsxWriter.Style.NUMBER);
      }
      else
      {
         r.add(value == null ? "" : value.toString(), XlsxWriter.Style.TEXT);
      }
   }

   private static void header(XlsxWriter.Sheet sheet, List<ColumnDef> columns)
   {
      XlsxWriter.Row h = sheet.addRow();
      for (int i = 0; i < columns.size(); i++)
      {
         ColumnDef col = columns.get(i);
         h.add(title(col), XlsxWriter.Style.HEADER);
         double chars = Math.max(6, col.getWidth() / 7.0);
         if (col.getKind() == ColumnDef.Kind.INDICATORS)
         {
            chars = 14;
         }
         else if (col.getKind() == ColumnDef.Kind.TASK_MODE)
         {
            chars = 16;
         }
         else if (col.getKind() == ColumnDef.Kind.NAME)
         {
            chars = Math.max(chars, 40);
         }
         sheet.setColumnWidth(i, chars);
      }
   }

   private static String title(ColumnDef col)
   {
      return switch (col.getKind())
      {
         case ID -> I18n.t("column.id");
         case INDICATORS -> I18n.t("column.indicators");
         default -> col.getTitle();
      };
   }

   private static void addValue(XlsxWriter.Row r, Object value, boolean bold, ColumnDef col)
   {
      if (value == null)
      {
         r.add(null, XlsxWriter.Style.TEXT, bold);
      }
      else if (value instanceof LocalDateTime)
      {
         r.add(value, XlsxWriter.Style.DATE, bold);
      }
      else if (value instanceof CellPresenter.ExportPercent p)
      {
         r.add(new XlsxWriter.Percent(p.value()), XlsxWriter.Style.PERCENT, bold);
      }
      else if (value instanceof CellPresenter.ExportCurrency c)
      {
         r.add(new XlsxWriter.Money(c.value()), XlsxWriter.Style.CURRENCY, bold);
      }
      else if (value instanceof Number)
      {
         r.add(value, XlsxWriter.Style.NUMBER, bold);
      }
      else
      {
         r.add(value.toString(), XlsxWriter.Style.TEXT, bold);
      }
   }

   /**
    * Alle Vorgänge, unabhängig vom Auf-/Zuklappzustand der Ansicht.
    */
   static List<TaskRow> allTaskRows(ViewSnapshot view)
   {
      if (view.outline() != null)
      {
         // Gleiche Sortierung und gleicher Filter wie am Bildschirm, aber alle Teilvorgänge eingeblendet
         return view.outline().expandedCopy().getRows();
      }
      TaskOutline all = new TaskOutline(view.doc(), false);
      all.showLevel(Integer.MAX_VALUE);
      return all.getRows();
   }

   // ------------------------------------------------------------------ CSV

   /**
    * CSV mit Semikolon (deutsch) bzw. Komma (englisch), UTF-8 mit BOM – öffnet sich in Excel direkt korrekt.
    */
   public static void exportCsv(ViewSnapshot view, List<ColumnDef> columns, File file) throws IOException
   {
      Formats fmt = view.formats();
      char sep = fmt.getLanguage() == Language.DE ? ';' : ',';
      CellPresenter presenter = view.presenter();
      try (Writer w = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8)))
      {
         w.write('﻿');
         List<String> header = new ArrayList<>();
         for (ColumnDef col : columns)
         {
            header.add(title(col));
         }
         line(w, header, sep);
         if (view.kind().isResourceView())
         {
            for (Resource res : view.resources())
            {
               List<String> values = new ArrayList<>();
               for (ColumnDef col : columns)
               {
                  values.add(csvText(col, presenter.resource(res, col).text(), col.getKind() == ColumnDef.Kind.INDICATORS ? String.valueOf(presenter.resourceExportValue(res, col)) : null));
               }
               line(w, values, sep);
            }
         }
         else
         {
            for (TaskRow row : allTaskRows(view))
            {
               List<String> values = new ArrayList<>();
               Task task = row.task();
               for (ColumnDef col : columns)
               {
                  String text;
                  switch (col.getKind())
                  {
                     case INDICATORS -> text = task.getNull() ? "" : CellPresenter.indicatorText(presenter.taskIndicators(task));
                     case TASK_MODE -> text = task.getNull() ? "" : fmt.taskMode(task.getTaskMode());
                     case NAME -> text = "  ".repeat(Math.max(0, row.level() - 1)) + (task.getName() == null ? "" : task.getName());
                     default -> text = presenter.task(row, col).text();
                  }
                  values.add(text);
               }
               line(w, values, sep);
            }
         }
      }
   }

   private static String csvText(ColumnDef col, String text, String iconText)
   {
      if (col.getKind() == ColumnDef.Kind.INDICATORS)
      {
         return iconText == null || "null".equals(iconText) ? "" : iconText;
      }
      return text == null ? "" : text;
   }

   private static void line(Writer w, List<String> values, char sep) throws IOException
   {
      for (int i = 0; i < values.size(); i++)
      {
         if (i > 0)
         {
            w.write(sep);
         }
         w.write(quote(values.get(i), sep));
      }
      w.write("\r\n");
   }

   private static String quote(String value, char sep)
   {
      String v = value == null ? "" : value;
      if (v.indexOf(sep) >= 0 || v.indexOf('"') >= 0 || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0 || v.startsWith(" "))
      {
         return '"' + v.replace("\"", "\"\"") + '"';
      }
      return v;
   }
}
