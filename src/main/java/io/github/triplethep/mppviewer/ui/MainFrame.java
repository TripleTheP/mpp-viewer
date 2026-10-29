package io.github.triplethep.mppviewer.ui;

import io.github.triplethep.mppviewer.core.ColumnDef;
import io.github.triplethep.mppviewer.core.ProjectDocument;
import io.github.triplethep.mppviewer.core.ProjectLoadException;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.i18n.Language;
import io.github.triplethep.mppviewer.print.PrintSettings;
import io.github.triplethep.mppviewer.render.GanttOptions;
import io.github.triplethep.mppviewer.render.Style;
import io.github.triplethep.mppviewer.render.ViewKind;
import io.github.triplethep.mppviewer.render.ViewSnapshot;
import io.github.triplethep.mppviewer.render.ZoomLevel;
import io.github.triplethep.mppviewer.settings.Settings;
import io.github.triplethep.mppviewer.ui.backstage.Backstage;
import io.github.triplethep.mppviewer.ui.backstage.BackstageHost;
import io.github.triplethep.mppviewer.ui.dialogs.FindDialog;
import io.github.triplethep.mppviewer.ui.dialogs.ProjectInfoDialog;
import io.github.triplethep.mppviewer.ui.dialogs.ResourceInfoDialog;
import io.github.triplethep.mppviewer.ui.dialogs.TaskInfoDialog;
import io.github.triplethep.mppviewer.ui.ribbon.Ribbon;
import io.github.triplethep.mppviewer.ui.ribbon.RibbonButtons;
import io.github.triplethep.mppviewer.ui.ribbon.RibbonGroup;
import io.github.triplethep.mppviewer.ui.view.ProjectViewModel;
import io.github.triplethep.mppviewer.ui.view.ResourceView;
import io.github.triplethep.mppviewer.ui.view.TaskView;
import org.mpxj.Resource;
import org.mpxj.Table;
import org.mpxj.Task;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Hauptfenster im Stil von Microsoft Project: Menüband, Ansicht, Statusleiste und Backstage („Datei“).
 */
public final class MainFrame extends JFrame implements BackstageHost
{
   private static final long serialVersionUID = 1L;
   private static final String CARD_MAIN = "main";
   private static final String CARD_BACKSTAGE = "backstage";
   private static final String VIEW_WELCOME = "welcome";
   private static final String VIEW_TASKS = "tasks";
   private static final String VIEW_RESOURCES = "resources";

   private final Settings settings;
   private final JPanel root = new JPanel(new CardLayout());
   private final List<Component> needsDocument = new ArrayList<>();
   private final List<Runnable> syncers = new ArrayList<>();
   private PrintSettings printSettings;
   private ProjectViewModel model;
   private TaskView taskView;
   private ResourceView resourceView;
   private Ribbon ribbon;
   private StatusBar statusBar;
   private ViewStrip viewStrip;
   private JPanel viewHost;
   private Backstage backstage;
   private FindDialog findDialog;

   public MainFrame(Settings settings)
   {
      this.settings = settings;
      this.printSettings = PrintSettings.defaults(I18n.language());
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      setIconImages(Icons.appImages());
      setMinimumSize(new Dimension(820, 520));
      setContentPane(root);
      restoreBounds();
      buildUi();
      installDragAndDrop();
      addWindowListener(new WindowAdapter()
      {
         @Override
         public void windowClosing(WindowEvent e)
         {
            saveWindowState();
         }

         @Override
         public void windowClosed(WindowEvent e)
         {
            System.exit(0);
         }
      });
      updateTitle();
   }

   // ------------------------------------------------------------------ Aufbau

   private void buildUi()
   {
      needsDocument.clear();
      syncers.clear();
      root.removeAll();
      Tasks.setStatusSink(s ->
      {
         if (statusBar != null)
         {
            statusBar.setStatus(s);
         }
      });

      JPanel main = new JPanel(new BorderLayout());
      ribbon = buildRibbon();
      main.add(ribbon, BorderLayout.NORTH);

      viewHost = new JPanel(new CardLayout());
      viewHost.add(new WelcomePanel(settings.getRecentFiles(), this::openFileDialog, this::openFile), VIEW_WELCOME);
      viewStrip = new ViewStrip();
      JPanel center = new JPanel(new BorderLayout());
      center.add(viewStrip, BorderLayout.WEST);
      center.add(viewHost, BorderLayout.CENTER);
      main.add(center, BorderLayout.CENTER);

      statusBar = new StatusBar(this::showView, ppd ->
      {
         if (taskView != null)
         {
            taskView.setPxPerDay(ppd);
         }
      });
      main.add(statusBar, BorderLayout.SOUTH);

      root.add(main, CARD_MAIN);
      setJMenuBar(buildQuickAccess());
      installKeys(main);

      if (model != null)
      {
         attachViews();
      }
      else
      {
         ((CardLayout) viewHost.getLayout()).show(viewHost, VIEW_WELCOME);
         viewStrip.setVisible(false);
      }
      updateEnabled();
      root.revalidate();
      root.repaint();
   }

   private JMenuBar buildQuickAccess()
   {
      JMenuBar bar = new JMenuBar();
      bar.setBorder(BorderFactory.createEmptyBorder());
      bar.setOpaque(false);
      JButton open = quickButton(Icons.Name.OPEN, I18n.t("qat.open"), e -> openFileDialog());
      JButton print = quickButton(Icons.Name.PRINT, I18n.t("qat.print"), e -> showBackstage(Backstage.Page.PRINT));
      needsDocument.add(print);
      bar.add(open);
      bar.add(print);
      return bar;
   }

   private static JButton quickButton(Icons.Name icon, String tooltip, java.awt.event.ActionListener action)
   {
      JButton b = new JButton(Icons.mono(icon, 16, Color.WHITE));
      b.putClientProperty("JButton.buttonType", "toolBarButton");
      b.setFocusable(false);
      b.setToolTipText(tooltip);
      b.getAccessibleContext().setAccessibleName(tooltip);
      b.setBackground(Style.ACCENT);
      b.addActionListener(action);
      return b;
   }

   private Ribbon buildRibbon()
   {
      Ribbon r = new Ribbon();
      r.addFileTab(I18n.t("ribbon.file"), () -> showBackstage(model == null ? Backstage.Page.OPEN : Backstage.Page.INFO));

      // ---- Vorgang
      JPanel task = r.addTab(I18n.t("ribbon.task"));
      task.add(new RibbonGroup(I18n.t("ribbon.group.view")).large(doc(RibbonButtons.largeMenu(Icons.Name.GANTT, I18n.t("ribbon.ganttChart"), I18n.t("ribbon.view.tooltip"), this::fillViewMenu))));
      task.add(Ribbon.separator());
      task.add(new RibbonGroup(I18n.t("ribbon.group.clipboard")).large(doc(RibbonButtons.large(Icons.Name.COPY, I18n.t("ribbon.copy"), I18n.t("ribbon.copy.tooltip"), e -> copy()))));
      task.add(Ribbon.separator());
      task.add(new RibbonGroup(I18n.t("ribbon.group.properties"))
         .large(doc(RibbonButtons.large(Icons.Name.TASK_INFO, I18n.t("ribbon.information"), I18n.t("ribbon.information.tooltip"), e -> showSelectedInfo(0))))
         .stack(doc(RibbonButtons.small(Icons.Name.NOTES, I18n.t("ribbon.notes"), I18n.t("ribbon.notes.tooltip"), e -> showSelectedInfo(4)))));
      task.add(Ribbon.separator());
      task.add(new RibbonGroup(I18n.t("ribbon.group.editing"))
         .large(doc(RibbonButtons.large(Icons.Name.SCROLL_TO_TASK, I18n.t("ribbon.scrollToTask"), I18n.t("ribbon.scrollToTask.tooltip"), e -> withTaskView(TaskView::scrollToSelectedTask))))
         .stack(doc(RibbonButtons.small(Icons.Name.FIND, I18n.t("ribbon.find"), I18n.t("ribbon.find.tooltip"), e -> showFind())),
            doc(RibbonButtons.small(Icons.Name.EXPAND, I18n.t("ribbon.expand"), I18n.t("ribbon.expand.tooltip"), e -> outlineLevel(Integer.MAX_VALUE))),
            doc(RibbonButtons.small(Icons.Name.COLLAPSE, I18n.t("ribbon.collapse"), I18n.t("ribbon.collapse.tooltip"), e -> outlineLevel(1)))));

      // ---- Ressource
      JPanel resource = r.addTab(I18n.t("ribbon.resource"));
      resource.add(new RibbonGroup(I18n.t("ribbon.group.view")).large(doc(RibbonButtons.large(Icons.Name.RESOURCE_SHEET, I18n.t("view.RESOURCE_SHEET.short"), I18n.t("view.RESOURCE_SHEET"), e -> showView(ViewKind.RESOURCE_SHEET)))));
      resource.add(Ribbon.separator());
      resource.add(new RibbonGroup(I18n.t("ribbon.group.properties")).large(doc(RibbonButtons.large(Icons.Name.TASK_INFO, I18n.t("ribbon.information"), I18n.t("ribbon.resourceInfo.tooltip"), e -> showResourceInfo()))));

      // ---- Projekt
      JPanel project = r.addTab(I18n.t("ribbon.project"));
      project.add(new RibbonGroup(I18n.t("ribbon.group.properties")).large(doc(RibbonButtons.large(Icons.Name.PROJECT_INFO, I18n.t("ribbon.projectInfo"), I18n.t("ribbon.projectInfo.tooltip"), e -> showProjectInfo()))));

      // ---- Ansicht
      JPanel view = r.addTab(I18n.t("ribbon.view"));
      view.add(new RibbonGroup(I18n.t("ribbon.group.taskViews"))
         .large(doc(RibbonButtons.large(Icons.Name.GANTT, I18n.t("ribbon.ganttChart"), I18n.t("view.GANTT"), e -> showView(ViewKind.GANTT))))
         .large(doc(RibbonButtons.large(Icons.Name.TRACKING, I18n.t("view.TRACKING_GANTT.short"), I18n.t("view.TRACKING_GANTT"), e -> showView(ViewKind.TRACKING_GANTT))))
         .large(doc(RibbonButtons.large(Icons.Name.TASK_SHEET, I18n.t("view.TASK_SHEET.short"), I18n.t("view.TASK_SHEET"), e -> showView(ViewKind.TASK_SHEET)))));
      view.add(Ribbon.separator());
      view.add(new RibbonGroup(I18n.t("ribbon.group.resourceViews")).large(doc(RibbonButtons.large(Icons.Name.RESOURCE_SHEET, I18n.t("view.RESOURCE_SHEET.short"), I18n.t("view.RESOURCE_SHEET"), e -> showView(ViewKind.RESOURCE_SHEET)))));
      view.add(Ribbon.separator());
      JCheckBox autoFilter = RibbonButtons.check(I18n.t("ribbon.autofilter"), I18n.t("ribbon.autofilter.tooltip"), false, v ->
      {
         if (model != null)
         {
            model.setAutoFilter(v.booleanValue());
            taskView.getTable().getTableHeader().repaint();
         }
      });
      syncers.add(() -> autoFilter.setSelected(model != null && model.isAutoFilter()));
      doc(autoFilter);
      view.add(new RibbonGroup(I18n.t("ribbon.group.data")).stack(
         doc(RibbonButtons.smallMenu(Icons.Name.SORT, I18n.t("ribbon.sort"), I18n.t("ribbon.sort.tooltip"), this::fillSortMenu)),
         doc(RibbonButtons.smallMenu(Icons.Name.OUTLINE, I18n.t("ribbon.outline"), I18n.t("ribbon.outline.tooltip"), this::fillOutlineMenu)),
         doc(RibbonButtons.smallMenu(Icons.Name.TABLE, I18n.t("ribbon.tables"), I18n.t("ribbon.tables.tooltip"), this::fillTablesMenu)))
         .stack(doc(RibbonButtons.smallMenu(Icons.Name.FILTER, I18n.t("ribbon.filter"), I18n.t("ribbon.filter.tooltip"), this::fillFilterMenu)),
            autoFilter));
      view.add(Ribbon.separator());
      view.add(new RibbonGroup(I18n.t("ribbon.group.zoom"))
         .stack(doc(RibbonButtons.smallMenu(Icons.Name.TIMESCALE, I18n.t("ribbon.timescale"), I18n.t("ribbon.timescale.tooltip"), this::fillTimescaleMenu)),
            doc(RibbonButtons.small(Icons.Name.ZOOM_IN, I18n.t("ribbon.zoomIn"), null, e -> withTaskView(TaskView::zoomIn))),
            doc(RibbonButtons.small(Icons.Name.ZOOM_OUT, I18n.t("ribbon.zoomOut"), null, e -> withTaskView(TaskView::zoomOut))))
         .large(doc(RibbonButtons.large(Icons.Name.ZOOM_ENTIRE, I18n.t("ribbon.entireProject"), I18n.t("ribbon.entireProject.tooltip"), e -> withTaskView(TaskView::zoomEntireProject))))
         .large(doc(RibbonButtons.large(Icons.Name.SCROLL_TO_TASK, I18n.t("ribbon.selectedTasks"), I18n.t("ribbon.selectedTasks.tooltip"), e -> withTaskView(TaskView::zoomSelectedTasks))))
         .large(doc(RibbonButtons.large(Icons.Name.TODAY, I18n.t("ribbon.today"), I18n.t("ribbon.today.tooltip"), e -> withTaskView(TaskView::scrollToToday)))));

      // ---- Format
      JPanel format = r.addTab(I18n.t("ribbon.format"));
      format.add(new RibbonGroup(I18n.t("ribbon.group.barStyles")).stack(
         option(I18n.t("format.critical"), I18n.t("format.critical.tooltip"), o -> o.criticalTasks, (o, v) -> o.criticalTasks = v),
         option(I18n.t("format.baseline"), I18n.t("format.baseline.tooltip"), o -> o.baseline, (o, v) -> o.baseline = v),
         option(I18n.t("format.links"), I18n.t("format.links.tooltip"), o -> o.links, (o, v) -> o.links = v)));
      format.add(Ribbon.separator());
      JCheckBox summary = RibbonButtons.check(I18n.t("format.projectSummary"), I18n.t("format.projectSummary.tooltip"), false, v ->
      {
         if (model != null)
         {
            model.getOutline().setShowProjectSummary(v.booleanValue());
            taskView.afterOutlineChange(taskView.getSelectedTask());
         }
      });
      syncers.add(() -> summary.setSelected(model != null && model.getOutline().isShowProjectSummary()));
      doc(summary);
      format.add(new RibbonGroup(I18n.t("ribbon.group.showHide")).stack(
         summary,
         option(I18n.t("format.barText"), I18n.t("format.barText.tooltip"), o -> o.barText, (o, v) -> o.barText = v),
         option(I18n.t("format.currentDate"), I18n.t("format.currentDate.tooltip"), o -> o.currentDate, (o, v) -> o.currentDate = v)));
      format.add(Ribbon.separator());
      format.add(new RibbonGroup(I18n.t("ribbon.group.layout")).stack(
         option(I18n.t("format.nonWorking"), I18n.t("format.nonWorking.tooltip"), o -> o.nonWorking, (o, v) -> o.nonWorking = v),
         option(I18n.t("format.roundBars"), I18n.t("format.roundBars.tooltip"), o -> o.roundBarsToWholeDays, (o, v) -> o.roundBarsToWholeDays = v)));

      r.select(0);
      return r;
   }

   private JCheckBox option(String text, String tooltip, java.util.function.Function<GanttOptions, Boolean> getter, java.util.function.BiConsumer<GanttOptions, Boolean> setter)
   {
      JCheckBox box = RibbonButtons.check(text, tooltip, false, v ->
      {
         if (model != null)
         {
            setter.accept(model.getOptions(), v);
            taskView.refresh();
         }
      });
      syncers.add(() -> box.setSelected(model != null && getter.apply(model.getOptions()).booleanValue()));
      doc(box);
      return box;
   }

   private <T extends JComponent> T doc(T c)
   {
      needsDocument.add(c);
      return c;
   }

   private void fillViewMenu(JPopupMenu menu)
   {
      for (ViewKind kind : ViewKind.values())
      {
         JRadioButtonMenuItem item = new JRadioButtonMenuItem(I18n.t(kind.key()), model != null && model.getKind() == kind);
         item.addActionListener(e -> showView(kind));
         menu.add(item);
      }
   }

   private void fillSortMenu(JPopupMenu menu)
   {
      if (model == null)
      {
         return;
      }
      io.github.triplethep.mppviewer.core.TaskQuery q = model.getQuery();
      JRadioButtonMenuItem byId = new JRadioButtonMenuItem(I18n.t("sort.id"), q.getSortMode() == io.github.triplethep.mppviewer.core.TaskQuery.SortMode.ID);
      byId.addActionListener(e -> changeQuery(() -> q.sortById()));
      menu.add(byId);
      if (q.hasSavedOrder())
      {
         JRadioButtonMenuItem saved = new JRadioButtonMenuItem(I18n.t("sort.saved"), q.getSortMode() == io.github.triplethep.mppviewer.core.TaskQuery.SortMode.SAVED);
         saved.addActionListener(e -> changeQuery(() -> q.sortSaved()));
         menu.add(saved);
      }
      menu.addSeparator();
      for (org.mpxj.TaskField f : new org.mpxj.TaskField[]
      {
         org.mpxj.TaskField.START,
         org.mpxj.TaskField.FINISH,
         org.mpxj.TaskField.NAME,
         org.mpxj.TaskField.DURATION,
         org.mpxj.TaskField.PRIORITY
      })
      {
         JRadioButtonMenuItem item = new JRadioButtonMenuItem(I18n.t("sort.by", io.github.triplethep.mppviewer.core.ColumnSets.fieldTitle(f).replace("\u00AD", "")), q.getSortField() == f);
         item.addActionListener(e -> changeQuery(() -> q.sortBy(f, true)));
         menu.add(item);
      }
   }

   private void fillFilterMenu(JPopupMenu menu)
   {
      if (model == null)
      {
         return;
      }
      io.github.triplethep.mppviewer.core.TaskQuery q = model.getQuery();
      JRadioButtonMenuItem all = new JRadioButtonMenuItem(I18n.t("filter.all"), !q.isFiltered());
      all.addActionListener(e -> changeQuery(q::clearFilters));
      menu.add(all);
      if (q.hasFileFilters())
      {
         StringBuilder names = new StringBuilder();
         for (org.mpxj.Filter f : model.getDoc().getSavedFilters())
         {
            if (!names.isEmpty())
            {
               names.append(", ");
            }
            names.append(f.getName() == null ? "AutoFilter" : ProjectDocument.displayName(f.getName()));
         }
         JCheckBoxMenuItem file = new JCheckBoxMenuItem(I18n.t("filter.file", names.toString()), q.isFileFiltersActive());
         file.addActionListener(e -> changeQuery(() -> q.setFileFiltersActive(file.isSelected())));
         menu.add(file);
      }
   }

   private void changeQuery(Runnable change)
   {
      if (model == null)
      {
         return;
      }
      change.run();
      if (model.getKind().isResourceView())
      {
         showView(ViewKind.GANTT);
      }
      taskView.queryChanged();
   }

   private void updateFilterStatus()
   {
      if (model == null)
      {
         statusBar.setFilterInfo(null, null);
         return;
      }
      io.github.triplethep.mppviewer.core.TaskQuery q = model.getQuery();
      if (q.isFiltered())
      {
         int shown = 0;
         for (io.github.triplethep.mppviewer.core.TaskRow r : model.getOutline().expandedCopy().getRows())
         {
            if (!r.isProjectSummary())
            {
               shown++;
            }
         }
         statusBar.setFilterInfo(I18n.t("status.filtered", shown, model.getDoc().getTasks().size()), () -> changeQuery(q::clearFilters));
      }
      else
      {
         statusBar.setFilterInfo(null, null);
      }
   }

   private void fillOutlineMenu(JPopupMenu menu)
   {
      if (model == null)
      {
         return;
      }
      JMenuItem all = new JMenuItem(I18n.t("outline.all"));
      all.addActionListener(e -> outlineLevel(Integer.MAX_VALUE));
      menu.add(all);
      menu.addSeparator();
      int deepest = Math.min(9, model.getOutline().getDeepestLevel());
      for (int i = 1; i <= deepest; i++)
      {
         int level = i;
         JMenuItem item = new JMenuItem(I18n.t("outline.level", level));
         item.addActionListener(e -> outlineLevel(level));
         menu.add(item);
      }
   }

   private void fillTablesMenu(JPopupMenu menu)
   {
      if (model == null)
      {
         return;
      }
      List<Table> tables = model.getDoc().getTaskTables();
      if (tables.isEmpty())
      {
         JMenuItem none = new JMenuItem(I18n.t("tables.none"));
         none.setEnabled(false);
         menu.add(none);
         return;
      }
      for (Table t : tables)
      {
         JCheckBoxMenuItem item = new JCheckBoxMenuItem(localizedTableName(t.getName()), t == model.getTaskTable());
         item.addActionListener(e ->
         {
            model.setTaskTable(t);
            taskView.setColumns(model.getTaskColumns());
            if (model.getKind().isResourceView())
            {
               showView(ViewKind.GANTT);
            }
         });
         menu.add(item);
      }
   }

   /**
    * Englische Standardtabellennamen aus Project in die Oberflächensprache übersetzen.
    */
   private static String localizedTableName(String name)
   {
      String clean = ProjectDocument.displayName(name);
      String key = "table." + clean.toLowerCase(Locale.ROOT).replace(' ', '_').replace(":", "");
      String text = I18n.opt(key);
      return text != null ? text : clean;
   }

   private void fillTimescaleMenu(JPopupMenu menu)
   {
      if (model == null)
      {
         return;
      }
      ZoomLevel current = ZoomLevel.nearest(model.getTimeline().getPxPerDay());
      ZoomLevel[] presets =
      {
         ZoomLevel.DAYS_HOURS,
         ZoomLevel.WEEKS_DAYS_XL,
         ZoomLevel.WEEKS_DAYS,
         ZoomLevel.MONTHS_WEEKS,
         ZoomLevel.QUARTERS_MONTHS,
         ZoomLevel.YEARS_QUARTERS
      };
      for (ZoomLevel level : presets)
      {
         JRadioButtonMenuItem item = new JRadioButtonMenuItem(I18n.t("zoom." + level.name()), level == current);
         item.addActionListener(e -> withTaskView(v -> v.setPxPerDay(level.pxPerDay())));
         menu.add(item);
      }
   }

   private void installKeys(JComponent c)
   {
      int menu = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
      key(c, KeyStroke.getKeyStroke(KeyEvent.VK_O, menu), "open", this::openFileDialog);
      key(c, KeyStroke.getKeyStroke(KeyEvent.VK_P, menu), "print", () ->
      {
         if (model != null)
         {
            showBackstage(Backstage.Page.PRINT);
         }
      });
      key(c, KeyStroke.getKeyStroke(KeyEvent.VK_F, menu), "find", this::showFind);
      key(c, KeyStroke.getKeyStroke(KeyEvent.VK_W, menu), "close", this::closeDocument);
      key(c, KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "export", () ->
      {
         if (model != null)
         {
            showBackstage(Backstage.Page.EXPORT);
         }
      });
      key(c, KeyStroke.getKeyStroke(KeyEvent.VK_ADD, menu), "zoomIn", () -> withTaskView(TaskView::zoomIn));
      key(c, KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, menu), "zoomOut", () -> withTaskView(TaskView::zoomOut));
      key(c, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK), "fileTab", () -> showBackstage(model == null ? Backstage.Page.OPEN : Backstage.Page.INFO));
   }

   private static void key(JComponent c, KeyStroke ks, String name, Runnable action)
   {
      c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, name);
      c.getActionMap().put(name, new AbstractAction()
      {
         private static final long serialVersionUID = 1L;

         @Override
         public void actionPerformed(ActionEvent e)
         {
            action.run();
         }
      });
   }

   private void installDragAndDrop()
   {
      TransferHandler handler = new TransferHandler()
      {
         private static final long serialVersionUID = 1L;

         @Override
         public boolean canImport(TransferSupport support)
         {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
         }

         @Override
         public boolean importData(TransferSupport support)
         {
            try
            {
               @SuppressWarnings("unchecked")
               List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
               if (!files.isEmpty())
               {
                  SwingUtilities.invokeLater(() -> openFile(files.get(0)));
                  return true;
               }
            }
            catch (Exception ex)
            {
               // ungültiger Inhalt
            }
            return false;
         }
      };
      getRootPane().setTransferHandler(handler);
      root.setTransferHandler(handler);
   }

   // ------------------------------------------------------------------ Dokument

   @Override
   public void openFileDialog()
   {
      File f = FileDialogs.chooseProjectFile(this, settings);
      if (f != null)
      {
         openFile(f);
      }
   }

   @Override
   public void openFile(File file)
   {
      closeBackstage();
      Tasks.run(this, I18n.t("status.loading", file.getName()), () ->
      {
         try
         {
            return ProjectDocument.load(file);
         }
         catch (ProjectLoadException ex)
         {
            return ex;
         }
      }, result ->
      {
         if (result instanceof ProjectLoadException ex)
         {
            if (ex.getReason() == ProjectLoadException.Reason.NOT_FOUND)
            {
               settings.removeRecentFile(file);
               settings.save();
            }
            String detail = ex.getCause() == null ? "" : String.valueOf(ex.getCause().getMessage());
            JOptionPane.showMessageDialog(this, I18n.t("error.load." + ex.getReason().name(), file.getName(), detail), AppInfo.NAME, JOptionPane.ERROR_MESSAGE);
            return;
         }
         setDocument((ProjectDocument) result);
      });
   }

   void setDocument(ProjectDocument doc)
   {
      if (findDialog != null)
      {
         findDialog.dispose();
         findDialog = null;
      }
      model = new ProjectViewModel(doc);
      model.setKind(initialKind(doc.getSavedViewName()));
      if (doc.getFile() != null)
      {
         settings.addRecentFile(doc.getFile());
         settings.setLastDirectory(doc.getFile().getParentFile());
         settings.save();
      }
      attachViews();
      updateEnabled();
      updateTitle();
   }

   /**
    * Beim Öffnen die Ansicht zeigen, die beim Speichern in Project aktiv war.
    */
   private static ViewKind initialKind(String savedView)
   {
      if (savedView == null)
      {
         return ViewKind.GANTT;
      }
      String v = savedView.replace("&", "").toLowerCase(Locale.ROOT);
      if (v.contains("tracking") || v.contains("überwachung"))
      {
         return ViewKind.TRACKING_GANTT;
      }
      if (v.contains("resource sheet") || v.contains("ressource: tabelle"))
      {
         return ViewKind.RESOURCE_SHEET;
      }
      if (v.contains("task sheet") || v.contains("vorgang: tabelle"))
      {
         return ViewKind.TASK_SHEET;
      }
      return ViewKind.GANTT;
   }

   private void attachViews()
   {
      if (taskView != null)
      {
         viewHost.remove(taskView);
      }
      if (resourceView != null)
      {
         viewHost.remove(resourceView);
      }
      taskView = new TaskView(model);
      taskView.setOnOpenTask(this::showTaskInfo);
      taskView.setOnZoomChanged(() -> statusBar.setZoom(model.getTimeline().getPxPerDay()));
      taskView.setOnQueryChanged(this::updateFilterStatus);
      resourceView = new ResourceView(model);
      resourceView.setOnOpenResource(this::showResourceInfo);
      viewHost.add(taskView, VIEW_TASKS);
      viewHost.add(resourceView, VIEW_RESOURCES);
      viewStrip.setVisible(true);
      statusBar.setDocumentLoaded(true);
      statusBar.setCount(I18n.t("status.count", model.getDoc().getTasks().size(), model.getDoc().getResources().size()));
      statusBar.setZoom(model.getTimeline().getPxPerDay());
      updateFilterStatus();
      ViewKind kind = model.getKind();
      showView(kind);
      viewHost.validate();
      SwingUtilities.invokeLater(() ->
      {
         taskView.initialLayout();
         if (!kind.isResourceView())
         {
            taskView.focusTable();
         }
      });
   }

   @Override
   public void closeDocument()
   {
      closeBackstage();
      if (model == null)
      {
         return;
      }
      model = null;
      if (findDialog != null)
      {
         findDialog.dispose();
         findDialog = null;
      }
      viewHost.remove(taskView);
      viewHost.remove(resourceView);
      taskView = null;
      resourceView = null;
      buildUi();
      statusBar.setDocumentLoaded(false);
      updateTitle();
   }

   public void showView(ViewKind kind)
   {
      if (model == null)
      {
         return;
      }
      CardLayout cl = (CardLayout) viewHost.getLayout();
      if (kind.isResourceView())
      {
         model.setKind(kind);
         cl.show(viewHost, VIEW_RESOURCES);
         resourceView.getTable().requestFocusInWindow();
      }
      else
      {
         taskView.setKind(kind);
         cl.show(viewHost, VIEW_TASKS);
         taskView.focusTable();
      }
      viewStrip.setViewName(I18n.t(kind.key()));
      statusBar.setActiveView(kind);
      for (Runnable r : syncers)
      {
         r.run();
      }
   }

   private void updateEnabled()
   {
      boolean loaded = model != null;
      for (Component c : needsDocument)
      {
         c.setEnabled(loaded);
      }
      for (Runnable r : syncers)
      {
         r.run();
      }
      statusBar.setDocumentLoaded(loaded);
      if (loaded)
      {
         statusBar.setActiveView(model.getKind());
      }
   }

   private void updateTitle()
   {
      if (model == null)
      {
         setTitle(AppInfo.NAME);
      }
      else
      {
         setTitle(model.getDoc().getFileName() + " [" + I18n.t("title.readOnly") + "] - " + AppInfo.NAME);
      }
   }

   private void withTaskView(java.util.function.Consumer<TaskView> action)
   {
      if (model == null || taskView == null)
      {
         return;
      }
      if (!model.getKind().hasChart())
      {
         showView(ViewKind.GANTT);
      }
      action.accept(taskView);
   }

   private void outlineLevel(int level)
   {
      if (model == null)
      {
         return;
      }
      Task selected = taskView.getSelectedTask();
      model.getOutline().showLevel(level);
      taskView.afterOutlineChange(selected);
   }

   private void copy()
   {
      if (model == null)
      {
         return;
      }
      if (model.getKind().isResourceView())
      {
         resourceView.copySelection();
      }
      else
      {
         taskView.copySelection();
      }
      statusBar.setStatus(I18n.t("status.copied"));
   }

   // ------------------------------------------------------------------ Dialoge

   private void showSelectedInfo(int tab)
   {
      if (model == null)
      {
         return;
      }
      if (model.getKind().isResourceView())
      {
         showResourceInfo();
         return;
      }
      Task task = taskView.getSelectedTask();
      if (task == null)
      {
         JOptionPane.showMessageDialog(this, I18n.t("info.selectTask"), AppInfo.NAME, JOptionPane.INFORMATION_MESSAGE);
         return;
      }
      TaskInfoDialog dialog = new TaskInfoDialog(this, task, model.getFormats());
      dialog.selectTab(tab);
      dialog.setVisible(true);
   }

   private void showTaskInfo(Task task)
   {
      if (task != null && !task.getNull())
      {
         new TaskInfoDialog(this, task, model.getFormats()).setVisible(true);
      }
   }

   private void showResourceInfo()
   {
      if (model == null)
      {
         return;
      }
      if (!model.getKind().isResourceView())
      {
         showView(ViewKind.RESOURCE_SHEET);
      }
      Resource r = resourceView.getSelectedResource();
      if (r == null)
      {
         JOptionPane.showMessageDialog(this, I18n.t("info.selectResource"), AppInfo.NAME, JOptionPane.INFORMATION_MESSAGE);
         return;
      }
      showResourceInfo(r);
   }

   private void showResourceInfo(Resource r)
   {
      new ResourceInfoDialog(this, r, model.getFormats()).setVisible(true);
   }

   @Override
   public void showProjectInfo()
   {
      if (model != null)
      {
         new ProjectInfoDialog(this, model.getDoc()).setVisible(true);
      }
   }

   private void showFind()
   {
      if (model == null)
      {
         return;
      }
      if (model.getKind().isResourceView())
      {
         showView(ViewKind.GANTT);
      }
      if (findDialog == null)
      {
         findDialog = new FindDialog(this, model.getDoc(), () -> taskView.getSelectedTask(), this::reveal);
      }
      findDialog.setVisible(true);
      findDialog.focusText();
   }

   /**
    * Vorgang sichtbar machen: übergeordnete Sammelvorgänge aufklappen, markieren und hinscrollen.
    */
   private void reveal(Task task)
   {
      if (model.getKind().isResourceView())
      {
         showView(ViewKind.GANTT);
      }
      Task parent = task.getParentTask();
      while (parent != null)
      {
         model.getOutline().setExpanded(parent, true);
         parent = parent.getParentTask();
      }
      taskView.afterOutlineChange(null);
      taskView.selectTask(task, model.getKind().hasChart());
   }

   // ------------------------------------------------------------------ Backstage

   public void showBackstage(Backstage.Page page)
   {
      backstage = new Backstage(this);
      root.add(backstage, CARD_BACKSTAGE);
      ((CardLayout) root.getLayout()).show(root, CARD_BACKSTAGE);
      backstage.show(page);
      getJMenuBar().setVisible(false);
   }

   @Override
   public void closeBackstage()
   {
      if (backstage != null)
      {
         root.remove(backstage);
         backstage = null;
      }
      ((CardLayout) root.getLayout()).show(root, CARD_MAIN);
      if (getJMenuBar() != null)
      {
         getJMenuBar().setVisible(true);
      }
      if (taskView != null && model != null && !model.getKind().isResourceView())
      {
         taskView.focusTable();
      }
   }

   @Override
   public Settings settings()
   {
      return settings;
   }

   @Override
   public ProjectDocument document()
   {
      return model == null ? null : model.getDoc();
   }

   @Override
   public ViewSnapshot snapshot()
   {
      if (model == null)
      {
         return null;
      }
      ViewKind kind = model.getKind();
      if (kind.isResourceView())
      {
         List<ColumnDef> cols = resourceView.currentColumns();
         return new ViewSnapshot(model.getDoc(), kind, model.getOutline(), cols, cols.size(), model.getOptions().copy(), model.getTimeline(), model.getFormats());
      }
      List<ColumnDef> cols = taskView.currentColumns();
      int inPane = kind.hasChart() ? taskView.columnsInPane() : cols.size();
      return new ViewSnapshot(model.getDoc(), kind, model.getOutline(), cols, inPane, model.getOptions().copy(), model.getTimeline(), model.getFormats());
   }

   @Override
   public List<ColumnDef> taskColumns()
   {
      return taskView == null ? List.of() : taskView.currentColumns();
   }

   @Override
   public List<ColumnDef> resourceColumns()
   {
      return resourceView == null ? List.of() : resourceView.currentColumns();
   }

   @Override
   public PrintSettings printSettings()
   {
      return printSettings;
   }

   @Override
   public void setLanguage(Language language)
   {
      settings.setLanguage(language);
      settings.save();
      List<ColumnDef> taskCols = taskView == null ? null : taskView.currentColumns();
      List<ColumnDef> resCols = resourceView == null ? null : resourceView.currentColumns();
      I18n.setLanguage(language);
      PrintSettings fresh = PrintSettings.defaults(language);
      fresh.paper = printSettings.paper;
      fresh.landscape = printSettings.landscape;
      printSettings = fresh;
      if (model != null)
      {
         model.languageChanged(taskCols, resCols);
      }
      closeBackstage();
      buildUi();
      updateTitle();
      showBackstage(Backstage.Page.OPTIONS);
   }

   @Override
   public Component component()
   {
      return this;
   }

   // ------------------------------------------------------------------ Fensterzustand

   private void restoreBounds()
   {
      Rectangle bounds = settings.getWindowBounds();
      Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
      if (bounds != null && screen.intersects(bounds) && bounds.width >= 600 && bounds.height >= 400)
      {
         setBounds(bounds);
      }
      else
      {
         setSize(Math.min(1400, screen.width - 40), Math.min(860, screen.height - 40));
         setLocationRelativeTo(null);
      }
      if (settings.isWindowMaximized())
      {
         setExtendedState(getExtendedState() | Frame.MAXIMIZED_BOTH);
      }
   }

   private void saveWindowState()
   {
      boolean maximized = (getExtendedState() & Frame.MAXIMIZED_BOTH) != 0;
      settings.setWindowMaximized(maximized);
      if (!maximized)
      {
         settings.setWindowBounds(getBounds());
      }
      settings.save();
   }

   /**
    * Für Tests: Knopf im Menüband anhand seines Tooltips finden.
    */
   AbstractButton findButton(String tooltip)
   {
      return findButton(ribbon, tooltip);
   }

   private static AbstractButton findButton(Component c, String tooltip)
   {
      if (c instanceof AbstractButton b && tooltip.equals(b.getToolTipText()))
      {
         return b;
      }
      if (c instanceof java.awt.Container container)
      {
         for (Component child : container.getComponents())
         {
            AbstractButton hit = findButton(child, tooltip);
            if (hit != null)
            {
               return hit;
            }
         }
      }
      return null;
   }
}
