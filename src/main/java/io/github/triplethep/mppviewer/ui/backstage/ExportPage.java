package io.github.triplethep.mppviewer.ui.backstage;

import io.github.triplethep.mppviewer.core.ColumnDef;
import io.github.triplethep.mppviewer.export.PdfExporter;
import io.github.triplethep.mppviewer.export.PngExporter;
import io.github.triplethep.mppviewer.export.TableExport;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.print.PrintLayout;
import io.github.triplethep.mppviewer.render.Style;
import io.github.triplethep.mppviewer.render.ViewSnapshot;
import io.github.triplethep.mppviewer.ui.FileDialogs;
import io.github.triplethep.mppviewer.ui.Icons;
import io.github.triplethep.mppviewer.ui.Tasks;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.util.List;

/**
 * „Datei → Exportieren“: PDF, Excel, PNG und CSV.
 */
final class ExportPage extends JPanel
{
   private static final long serialVersionUID = 1L;

   private final BackstageHost host;
   private final JRadioButton pdfPages = new JRadioButton();
   private final JComboBox<String> pngScale = new JComboBox<>();
   private final JCheckBox allColumns = new JCheckBox();

   ExportPage(BackstageHost host)
   {
      super(new BorderLayout());
      this.host = host;
      setBackground(Backstage.CONTENT_BG);
      JPanel page = Backstage.pagePanel(I18n.t("backstage.export"));
      add(page, BorderLayout.CENTER);

      JPanel body = new JPanel();
      body.setBackground(Backstage.CONTENT_BG);
      body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

      ViewSnapshot snap = host.snapshot();
      JLabel scope = new JLabel(I18n.t("export.scope", snap.viewTitle()));
      scope.setForeground(Style.TEXT_MUTED);
      scope.setAlignmentX(Component.LEFT_ALIGNMENT);
      scope.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
      body.add(scope);

      // PDF
      JButton pdf = Backstage.tileButton(Icons.Name.PDF, I18n.t("export.pdf"), I18n.t("export.pdf.hint"));
      pdf.addActionListener(e -> exportPdf());
      body.add(pdf);
      JPanel pdfOptions = options();
      ButtonGroup g = new ButtonGroup();
      pdfPages.setText(I18n.t("export.pdf.pages"));
      pdfPages.setSelected(true);
      JRadioButton pdfPoster = new JRadioButton(I18n.t("export.pdf.single"));
      for (JRadioButton rb : new JRadioButton[]
      {
         pdfPages,
         pdfPoster
      })
      {
         rb.setOpaque(false);
         g.add(rb);
         pdfOptions.add(rb);
      }
      body.add(pdfOptions);
      body.add(Box.createVerticalStrut(8));

      // Excel
      JButton xlsx = Backstage.tileButton(Icons.Name.XLSX, I18n.t("export.xlsx"), I18n.t("export.xlsx.hint"));
      xlsx.addActionListener(e -> exportXlsx());
      body.add(xlsx);
      body.add(Box.createVerticalStrut(8));

      // PNG
      JButton png = Backstage.tileButton(Icons.Name.PNG, I18n.t("export.png"), I18n.t("export.png.hint"));
      png.addActionListener(e -> exportPng());
      png.setEnabled(true);
      body.add(png);
      JPanel pngOptions = options();
      pngOptions.add(new JLabel(I18n.t("export.png.resolution") + " "));
      pngScale.addItem(I18n.t("export.png.normal"));
      pngScale.addItem(I18n.t("export.png.high"));
      pngScale.setSelectedIndex(1);
      pngOptions.add(pngScale);
      body.add(pngOptions);
      body.add(Box.createVerticalStrut(8));

      // CSV
      JButton csv = Backstage.tileButton(Icons.Name.CSV, I18n.t("export.csv"), I18n.t("export.csv.hint"));
      csv.addActionListener(e -> exportCsv());
      body.add(csv);
      body.add(Box.createVerticalStrut(14));

      allColumns.setText(I18n.t("export.allColumns"));
      allColumns.setSelected(true);
      allColumns.setOpaque(false);
      allColumns.setAlignmentX(Component.LEFT_ALIGNMENT);
      allColumns.setToolTipText(I18n.t("export.allColumns.hint"));
      if (snap.kind().hasChart())
      {
         body.add(allColumns);
      }

      JScrollPane scroll = new JScrollPane(body);
      scroll.setBorder(BorderFactory.createEmptyBorder());
      scroll.getViewport().setBackground(Backstage.CONTENT_BG);
      scroll.getVerticalScrollBar().setUnitIncrement(16);
      page.add(scroll, BorderLayout.CENTER);
   }

   private static JPanel options()
   {
      JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
      p.setOpaque(false);
      p.setAlignmentX(Component.LEFT_ALIGNMENT);
      p.setBorder(BorderFactory.createEmptyBorder(0, 58, 0, 0));
      p.setMaximumSize(new Dimension(700, 32));
      return p;
   }

   /**
    * Spalten für Bild- und PDF-Export (Diagrammansichten: wahlweise nur bis zum Teiler).
    */
   private List<ColumnDef> chartColumns(ViewSnapshot snap)
   {
      if (!snap.kind().hasChart() || allColumns.isSelected())
      {
         return snap.columns();
      }
      return snap.columns().subList(0, Math.max(1, Math.min(snap.columns().size(), snap.columnsInPane())));
   }

   private void exportPdf()
   {
      ViewSnapshot snap = host.snapshot();
      File file = FileDialogs.chooseSaveFile(host.component(), host.settings(), host.document(), "pdf", I18n.t("export.filter.pdf"));
      if (file == null)
      {
         return;
      }
      boolean pages = pdfPages.isSelected();
      List<ColumnDef> cols = chartColumns(snap);
      io.github.triplethep.mppviewer.print.PrintSettings ps = host.printSettings().copy();
      String title = snap.doc().getTitle();
      Tasks.run(host.component(), I18n.t("export.running"), () ->
      {
         if (pages)
         {
            PdfExporter.exportPages(new PrintLayout(snap, ps), title, file);
         }
         else
         {
            PdfExporter.exportSinglePage(snap, cols, title, file);
         }
         return file;
      }, f -> FileDialogs.offerOpen(host.component(), f));
   }

   private void exportPng()
   {
      ViewSnapshot snap = host.snapshot();
      File file = FileDialogs.chooseSaveFile(host.component(), host.settings(), host.document(), "png", I18n.t("export.filter.png"));
      if (file == null)
      {
         return;
      }
      double scale = pngScale.getSelectedIndex() == 1 ? 2.0 : 1.0;
      List<ColumnDef> cols = chartColumns(snap);
      Tasks.run(host.component(), I18n.t("export.running"), () ->
      {
         PngExporter.export(snap, cols, scale, file);
         return file;
      }, f -> FileDialogs.offerOpen(host.component(), f));
   }

   private void exportXlsx()
   {
      ViewSnapshot snap = host.snapshot();
      File file = FileDialogs.chooseSaveFile(host.component(), host.settings(), host.document(), "xlsx", I18n.t("export.filter.xlsx"));
      if (file == null)
      {
         return;
      }
      List<ColumnDef> tasks = host.taskColumns();
      List<ColumnDef> resources = host.resourceColumns();
      Tasks.run(host.component(), I18n.t("export.running"), () ->
      {
         TableExport.exportXlsx(snap, tasks, resources, file);
         return file;
      }, f -> FileDialogs.offerOpen(host.component(), f));
   }

   private void exportCsv()
   {
      ViewSnapshot snap = host.snapshot();
      File file = FileDialogs.chooseSaveFile(host.component(), host.settings(), host.document(), "csv", I18n.t("export.filter.csv"));
      if (file == null)
      {
         return;
      }
      List<ColumnDef> cols = snap.kind().isResourceView() ? host.resourceColumns() : host.taskColumns();
      Tasks.run(host.component(), I18n.t("export.running"), () ->
      {
         TableExport.exportCsv(snap, cols, file);
         return file;
      }, f -> FileDialogs.offerOpen(host.component(), f));
   }
}
