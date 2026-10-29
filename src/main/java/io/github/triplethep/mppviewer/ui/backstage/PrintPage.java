package io.github.triplethep.mppviewer.ui.backstage;

import io.github.triplethep.mppviewer.export.PdfExporter;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.print.PrintLayout;
import io.github.triplethep.mppviewer.print.PrintSettings;
import io.github.triplethep.mppviewer.render.Style;
import io.github.triplethep.mppviewer.render.ViewSnapshot;
import io.github.triplethep.mppviewer.ui.FileDialogs;
import io.github.triplethep.mppviewer.ui.Icons;
import io.github.triplethep.mppviewer.ui.Tasks;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaSizeName;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.print.PrinterJob;
import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * „Datei → Drucken“: Einstellungen links, Seitenansicht rechts – wie in Office.
 */
final class PrintPage extends JPanel
{
   private static final long serialVersionUID = 1L;
   private static final int FIELD_WIDTH = 270;

   private final BackstageHost host;
   private final PrintSettings settings;
   private final ViewSnapshot snapshot;
   private final PreviewPanel preview = new PreviewPanel();
   private final JLabel summary = new JLabel();
   private final JComboBox<PrintService> printer = new JComboBox<>();
   private final JSpinner copies = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
   private final JSpinner percent;
   private final JSpinner fromDate;
   private final JSpinner toDate;
   private PrintLayout layout;
   private boolean building = true;

   PrintPage(BackstageHost host)
   {
      super(new BorderLayout());
      this.host = host;
      this.settings = host.printSettings();
      this.snapshot = host.snapshot();
      setBackground(Backstage.CONTENT_BG);

      JPanel left = new JPanel();
      left.setBackground(Backstage.CONTENT_BG);
      left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
      left.setBorder(BorderFactory.createEmptyBorder(22, 36, 20, 20));

      JLabel title = new JLabel(I18n.t("backstage.print"));
      title.setFont(Backstage.TITLE_FONT);
      title.setAlignmentX(Component.LEFT_ALIGNMENT);
      title.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
      left.add(title);

      // Drucken + Kopien
      JPanel printRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      printRow.setOpaque(false);
      printRow.setAlignmentX(Component.LEFT_ALIGNMENT);
      JButton printButton = new JButton(I18n.t("print.button"), Icons.get(Icons.Name.PRINT, 32));
      printButton.setVerticalTextPosition(SwingConstants.BOTTOM);
      printButton.setHorizontalTextPosition(SwingConstants.CENTER);
      printButton.setPreferredSize(new Dimension(92, 76));
      printButton.addActionListener(e -> print());
      printRow.add(printButton);
      printRow.add(Box.createHorizontalStrut(16));
      JLabel copiesLabel = new JLabel(I18n.t("print.copies"));
      printRow.add(copiesLabel);
      printRow.add(Box.createHorizontalStrut(6));
      copies.setPreferredSize(new Dimension(64, 28));
      copiesLabel.setLabelFor(copies);
      printRow.add(copies);
      printRow.setMaximumSize(new Dimension(FIELD_WIDTH + 40, 80));
      left.add(printRow);

      left.add(Backstage.section(I18n.t("print.printer")));
      PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
      PrintService def = PrintServiceLookup.lookupDefaultPrintService();
      for (PrintService s : services)
      {
         printer.addItem(s);
      }
      if (def != null)
      {
         printer.setSelectedItem(def);
      }
      printer.setRenderer(new DefaultListCellRenderer()
      {
         private static final long serialVersionUID = 1L;

         @Override
         public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
         {
            Object text = value instanceof PrintService ps ? ps.getName() : I18n.t("print.noPrinter");
            return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
         }
      });
      if (services.length == 0)
      {
         printButton.setEnabled(false);
         printButton.setToolTipText(I18n.t("print.noPrinter"));
      }
      left.add(field(printer));

      left.add(Backstage.section(I18n.t("print.settings")));

      // Zeitraum
      JComboBox<String> range = new JComboBox<>(new String[]
      {
         I18n.t("print.range.whole"),
         I18n.t("print.range.dates")
      });
      range.setSelectedIndex(settings.range == PrintSettings.Range.DATES ? 1 : 0);
      left.add(field(range));
      LocalDate start = snapshot.doc().getStart().toLocalDate();
      LocalDate finish = snapshot.doc().getFinish().toLocalDate();
      fromDate = dateSpinner(settings.from != null ? settings.from : start);
      toDate = dateSpinner(settings.to != null ? settings.to : finish);
      JPanel dates = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
      dates.setOpaque(false);
      dates.add(new JLabel(I18n.t("print.range.from") + " "));
      dates.add(fromDate);
      dates.add(new JLabel("  " + I18n.t("print.range.to") + " "));
      dates.add(toDate);
      dates.setAlignmentX(Component.LEFT_ALIGNMENT);
      dates.setMaximumSize(new Dimension(FIELD_WIDTH + 60, 34));
      left.add(dates);
      Runnable rangeEnabled = () ->
      {
         boolean d = range.getSelectedIndex() == 1;
         fromDate.setEnabled(d);
         toDate.setEnabled(d);
      };
      rangeEnabled.run();
      range.addActionListener(e ->
      {
         settings.range = range.getSelectedIndex() == 1 ? PrintSettings.Range.DATES : PrintSettings.Range.WHOLE_PROJECT;
         rangeEnabled.run();
         rebuild();
      });
      fromDate.addChangeListener(e -> rebuild());
      toDate.addChangeListener(e -> rebuild());

      // Papier
      JComboBox<PrintSettings.Paper> paper = new JComboBox<>(PrintSettings.Paper.values());
      paper.setSelectedItem(settings.paper);
      paper.setRenderer(new DefaultListCellRenderer()
      {
         private static final long serialVersionUID = 1L;

         @Override
         public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
         {
            Object text = value instanceof PrintSettings.Paper p ? p.label() : value;
            return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
         }
      });
      paper.addActionListener(e ->
      {
         settings.paper = (PrintSettings.Paper) paper.getSelectedItem();
         rebuild();
      });
      left.add(field(paper));

      JComboBox<String> orientation = new JComboBox<>(new String[]
      {
         I18n.t("print.landscape"),
         I18n.t("print.portrait")
      });
      orientation.setSelectedIndex(settings.landscape ? 0 : 1);
      orientation.addActionListener(e ->
      {
         settings.landscape = orientation.getSelectedIndex() == 0;
         rebuild();
      });
      left.add(field(orientation));

      // Skalierung
      JComboBox<String> scaling = new JComboBox<>(new String[]
      {
         I18n.t("print.fit1"),
         I18n.t("print.fit2"),
         I18n.t("print.fit3"),
         I18n.t("print.fitOne"),
         I18n.t("print.custom")
      });
      scaling.setSelectedIndex(switch (settings.scaling)
      {
         case FIT_PAGES_WIDE -> Math.max(0, Math.min(2, settings.pagesWide - 1));
         case FIT_ONE_PAGE -> 3;
         case PERCENT -> 4;
      });
      percent = new JSpinner(new SpinnerNumberModel(settings.percent, 10, 400, 5));
      JPanel percentRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
      percentRow.setOpaque(false);
      percentRow.add(new JLabel(I18n.t("print.percent") + " "));
      percent.setPreferredSize(new Dimension(70, 26));
      percentRow.add(percent);
      percentRow.add(new JLabel(" %"));
      percentRow.setAlignmentX(Component.LEFT_ALIGNMENT);
      percentRow.setMaximumSize(new Dimension(FIELD_WIDTH, 32));
      percent.setEnabled(settings.scaling == PrintSettings.Scaling.PERCENT);
      scaling.addActionListener(e ->
      {
         int i = scaling.getSelectedIndex();
         if (i <= 2)
         {
            settings.scaling = PrintSettings.Scaling.FIT_PAGES_WIDE;
            settings.pagesWide = i + 1;
         }
         else if (i == 3)
         {
            settings.scaling = PrintSettings.Scaling.FIT_ONE_PAGE;
         }
         else
         {
            settings.scaling = PrintSettings.Scaling.PERCENT;
         }
         percent.setEnabled(settings.scaling == PrintSettings.Scaling.PERCENT);
         rebuild();
      });
      percent.addChangeListener(e ->
      {
         settings.percent = ((Number) percent.getValue()).intValue();
         rebuild();
      });
      left.add(field(scaling));
      left.add(percentRow);

      // Zeitskala
      JComboBox<String> timescale = new JComboBox<>();
      for (PrintSettings.Timescale t : PrintSettings.Timescale.values())
      {
         timescale.addItem(I18n.t("print.timescale." + t.name()));
      }
      timescale.setSelectedIndex(settings.timescale.ordinal());
      timescale.setToolTipText(I18n.t("print.timescale.tooltip"));
      timescale.addActionListener(e ->
      {
         settings.timescale = PrintSettings.Timescale.values()[timescale.getSelectedIndex()];
         rebuild();
      });
      timescale.setEnabled(snapshot.kind().hasChart());
      left.add(field(timescale));

      // Spalten
      JComboBox<String> columns = new JComboBox<>(new String[]
      {
         I18n.t("print.columns.pane"),
         I18n.t("print.columns.all")
      });
      columns.setSelectedIndex(settings.allColumns ? 1 : 0);
      columns.addActionListener(e ->
      {
         settings.allColumns = columns.getSelectedIndex() == 1;
         rebuild();
      });
      columns.setEnabled(snapshot.kind().hasChart());
      left.add(field(columns));

      JComboBox<String> order = new JComboBox<>(new String[]
      {
         I18n.t("print.order.down"),
         I18n.t("print.order.over")
      });
      order.setSelectedIndex(settings.order == PrintSettings.Order.DOWN_THEN_OVER ? 0 : 1);
      order.addActionListener(e ->
      {
         settings.order = order.getSelectedIndex() == 0 ? PrintSettings.Order.DOWN_THEN_OVER : PrintSettings.Order.OVER_THEN_DOWN;
         rebuild();
      });
      left.add(field(order));

      left.add(check(I18n.t("print.legend"), settings.legend, v -> settings.legend = v));
      left.add(check(I18n.t("print.title"), settings.title, v -> settings.title = v));
      left.add(check(I18n.t("print.pageNumbers"), settings.pageNumbers, v -> settings.pageNumbers = v));

      left.add(Box.createVerticalStrut(12));
      JButton pdf = new JButton(I18n.t("print.savePdf"), Icons.get(Icons.Name.PDF, 20));
      pdf.setAlignmentX(Component.LEFT_ALIGNMENT);
      pdf.addActionListener(e -> savePdf());
      left.add(pdf);
      left.add(Box.createVerticalStrut(12));
      summary.setForeground(Style.TEXT_MUTED);
      summary.setAlignmentX(Component.LEFT_ALIGNMENT);
      left.add(summary);
      left.add(Box.createVerticalGlue());

      JScrollPane leftScroll = new JScrollPane(left, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      leftScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0xE1E1E1)));
      leftScroll.getVerticalScrollBar().setUnitIncrement(16);
      leftScroll.setPreferredSize(new Dimension(FIELD_WIDTH + 80, 100));
      add(leftScroll, BorderLayout.WEST);
      add(preview, BorderLayout.CENTER);

      building = false;
      rebuild();
   }

   private static JComponent field(JComponent c)
   {
      c.setAlignmentX(Component.LEFT_ALIGNMENT);
      c.setMaximumSize(new Dimension(FIELD_WIDTH, 30));
      c.setPreferredSize(new Dimension(FIELD_WIDTH, 28));
      JPanel wrap = new JPanel(new BorderLayout());
      wrap.setOpaque(false);
      wrap.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
      wrap.add(c, BorderLayout.CENTER);
      wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
      wrap.setMaximumSize(new Dimension(FIELD_WIDTH, 36));
      return wrap;
   }

   private JCheckBox check(String text, boolean value, java.util.function.Consumer<Boolean> setter)
   {
      JCheckBox box = new JCheckBox(text, value);
      box.setOpaque(false);
      box.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.addActionListener(e ->
      {
         setter.accept(Boolean.valueOf(box.isSelected()));
         rebuild();
      });
      return box;
   }

   private static JSpinner dateSpinner(LocalDate value)
   {
      Date date = Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
      JSpinner s = new JSpinner(new SpinnerDateModel(date, null, null, java.util.Calendar.DAY_OF_MONTH));
      String pattern = I18n.language() == io.github.triplethep.mppviewer.i18n.Language.DE ? "dd.MM.yyyy" : "MM/dd/yyyy";
      s.setEditor(new JSpinner.DateEditor(s, pattern));
      s.setPreferredSize(new Dimension(108, 26));
      return s;
   }

   private static LocalDate toLocalDate(JSpinner s)
   {
      Date d = (Date) s.getValue();
      return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
   }

   private void rebuild()
   {
      if (building)
      {
         return;
      }
      settings.from = toLocalDate(fromDate);
      settings.to = toLocalDate(toDate);
      layout = new PrintLayout(snapshot, settings);
      preview.showLayout(layout);
      summary.setText(I18n.t("print.summary", layout.getNumberOfPages(), layout.getPagesWide(), layout.getPagesTall(), Math.round(layout.getScale() * 100)));
   }

   private void print()
   {
      PrintService service = (PrintService) printer.getSelectedItem();
      if (service == null || layout == null)
      {
         return;
      }
      PrintLayout job = layout;
      int count = ((Number) copies.getValue()).intValue();
      String title = snapshot.doc().getTitle();
      Tasks.run(host.component(), I18n.t("print.printing"), () ->
      {
         PrinterJob pj = PrinterJob.getPrinterJob();
         pj.setPrintService(service);
         pj.setJobName(title);
         pj.setPageable(job);
         PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
         attrs.add(new Copies(count));
         attrs.add(new JobName(title, null));
         MediaSizeName media = switch (job.getSettings().paper)
         {
            case A4 -> MediaSizeName.ISO_A4;
            case A3 -> MediaSizeName.ISO_A3;
            case A5 -> MediaSizeName.ISO_A5;
            case LETTER -> MediaSizeName.NA_LETTER;
            case LEGAL -> MediaSizeName.NA_LEGAL;
            case TABLOID -> MediaSizeName.TABLOID;
         };
         if (service.isAttributeValueSupported(media, null, null))
         {
            attrs.add(media);
         }
         pj.print(attrs);
         return null;
      }, result -> host.closeBackstage());
   }

   private void savePdf()
   {
      if (layout == null)
      {
         return;
      }
      File file = FileDialogs.chooseSaveFile(host.component(), host.settings(), host.document(), "pdf", I18n.t("export.filter.pdf"));
      if (file == null)
      {
         return;
      }
      PrintLayout job = layout;
      String title = snapshot.doc().getTitle();
      Tasks.run(host.component(), I18n.t("export.running"), () ->
      {
         PdfExporter.exportPages(job, title, file);
         return file;
      }, f -> FileDialogs.offerOpen(host.component(), f));
   }
}
