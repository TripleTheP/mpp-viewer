package io.github.triplethep.mppviewer.ui.view;

import io.github.triplethep.mppviewer.core.ColumnDef;
import io.github.triplethep.mppviewer.core.TaskRow;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.render.CellContent;
import io.github.triplethep.mppviewer.render.CellPainter;
import io.github.triplethep.mppviewer.render.Indicator;
import io.github.triplethep.mppviewer.render.Style;
import io.github.triplethep.mppviewer.render.Timeline;
import io.github.triplethep.mppviewer.render.ViewKind;
import io.github.triplethep.mppviewer.render.ZoomLevel;
import org.mpxj.Task;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Vorgangsansichten: Gantt-Diagramm, Überwachungs-Gantt und Vorgangstabelle. Links die Tabelle, rechts das
 * Diagramm mit gemeinsamem senkrechten Bildlauf – wie in Microsoft Project.
 */
public final class TaskView extends JPanel
{
   private static final long serialVersionUID = 1L;

   private final ProjectViewModel model;
   private final SheetTable table;
   private final ChartPanel chart;
   private final TimescaleHeader header;
   private final JScrollPane tableScroll;
   private final JScrollPane chartScroll;
   private final JSplitPane split;
   private final BoundedRangeModel ownVerticalModel = new DefaultBoundedRangeModel();
   private Consumer<Task> onOpenTask = t -> {
   };
   private Runnable onZoomChanged = () -> {
   };
   private Runnable onQueryChanged = () -> {
   };
   private boolean chartVisible = true;

   public TaskView(ProjectViewModel model)
   {
      super(new BorderLayout());
      this.model = model;
      setBackground(Color.WHITE);

      table = new SheetTable(model.getTaskColumns(), () -> model.getOutline().getRows().size(), (row, col) -> cell(row.intValue(), col))
      {
         private static final long serialVersionUID = 1L;

         @Override
         protected String tooltipForIcons(int row, CellContent c)
         {
            return iconTooltip(row, c);
         }
      };
      chart = new ChartPanel(model, table.getSelectionModel());
      header = new TimescaleHeader(model);

      tableScroll = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
      tableScroll.setBorder(BorderFactory.createEmptyBorder());
      tableScroll.getViewport().setBackground(Color.WHITE);
      tableScroll.setWheelScrollingEnabled(false);
      tableScroll.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, corner());

      chartScroll = new JScrollPane(chart, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
      chartScroll.setBorder(BorderFactory.createEmptyBorder());
      chartScroll.setColumnHeaderView(header);
      chartScroll.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, corner());
      chartScroll.getViewport().setBackground(Color.WHITE);
      chartScroll.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
      chartScroll.setWheelScrollingEnabled(false);

      // Gemeinsamer senkrechter Bildlauf
      tableScroll.getVerticalScrollBar().setModel(chartScroll.getVerticalScrollBar().getModel());

      split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, chartScroll);
      split.setBorder(BorderFactory.createEmptyBorder());
      split.setContinuousLayout(true);
      split.setDividerSize(6);
      split.setResizeWeight(0);
      add(split, BorderLayout.CENTER);

      installMouse();
      installKeys();
      installAutoFilter();
      table.getSelectionModel().addListSelectionListener(e -> chart.repaint());
      split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, e -> model.setDividerLocation(split.getDividerLocation()));
   }

   private static JComponent corner()
   {
      JPanel p = new JPanel();
      p.setBackground(Style.HEADER_BG);
      return p;
   }

   // ------------------------------------------------------------------ Aufbau

   /**
    * Muss nach dem Einfügen ins Fenster aufgerufen werden: setzt Teiler und Bildlaufposition.
    */
   public void initialLayout()
   {
      int divider = model.getDividerLocation();
      if (divider <= 0)
      {
         divider = defaultDivider();
      }
      split.setDividerLocation(Math.min(divider, Math.max(200, getWidth() - 200)));
      SwingUtilities.invokeLater(this::scrollToProjectStart);
   }

   private int defaultDivider()
   {
      // Bis einschließlich „Ende“ bzw. die ersten sieben Spalten
      int w = 0;
      int count = 0;
      for (ColumnDef c : model.getTaskColumns())
      {
         w += c.getWidth();
         count++;
         if (c.getField() == org.mpxj.TaskField.FINISH || count >= 7)
         {
            break;
         }
      }
      return w + 2;
   }

   public void setKind(ViewKind kind)
   {
      model.setKind(kind);
      boolean showChart = kind.hasChart();
      if (showChart != chartVisible)
      {
         chartVisible = showChart;
         removeAll();
         if (showChart)
         {
            tableScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            tableScroll.getVerticalScrollBar().setModel(chartScroll.getVerticalScrollBar().getModel());
            tableScroll.setWheelScrollingEnabled(false);
            split.setLeftComponent(tableScroll);
            split.setDividerLocation(model.getDividerLocation() > 0 ? model.getDividerLocation() : defaultDivider());
            add(split, BorderLayout.CENTER);
         }
         else
         {
            int divider = split.getDividerLocation();
            model.setDividerLocation(divider);
            tableScroll.getVerticalScrollBar().setModel(ownVerticalModel);
            tableScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            tableScroll.setWheelScrollingEnabled(true);
            split.remove(tableScroll);
            add(tableScroll, BorderLayout.CENTER);
         }
         revalidate();
      }
      chart.repaint();
   }

   public void refresh()
   {
      table.refresh();
      chart.revalidate();
      chart.repaint();
      header.revalidate();
      header.repaint();
   }

   public void setColumns(List<ColumnDef> columns)
   {
      model.setTaskColumns(columns);
      table.setColumnDefs(columns);
   }

   public List<ColumnDef> currentColumns()
   {
      return table.currentColumnDefs();
   }

   /**
    * Anzahl der Spalten, die links vom Teiler vollständig sichtbar sind.
    */
   public int columnsInPane()
   {
      if (!chartVisible)
      {
         return table.getColumnCount();
      }
      int visible = tableScroll.getViewport().getExtentSize().width;
      int offset = tableScroll.getViewport().getViewPosition().x;
      int x = 0;
      int count = 0;
      for (ColumnDef c : table.currentColumnDefs())
      {
         x += c.getWidth();
         if (x - offset <= visible + 4)
         {
            count++;
         }
      }
      return Math.max(1, count);
   }

   private CellContent cell(int row, ColumnDef col)
   {
      List<TaskRow> rows = model.getOutline().getRows();
      if (row < 0 || row >= rows.size())
      {
         return CellContent.plain("", col.getAlign());
      }
      return model.getPresenter().task(rows.get(row), col);
   }

   private String iconTooltip(int row, CellContent c)
   {
      List<TaskRow> rows = model.getOutline().getRows();
      if (row < 0 || row >= rows.size())
      {
         return null;
      }
      Task task = rows.get(row).task();
      StringBuilder sb = new StringBuilder("<html>");
      for (Indicator i : c.icons())
      {
         switch (i)
         {
            case NOTE -> sb.append("<b>").append(I18n.t("indicator.NOTE")).append(":</b> ").append(htmlNotes(task.getNotes())).append("<br>");
            case CONSTRAINT, CONSTRAINT_HARD -> sb.append(I18n.t("tooltip.constraint", model.getFormats().constraint(task.getConstraintType()), model.getFormats().date(task.getConstraintDate()))).append("<br>");
            case DONE -> sb.append(I18n.t("tooltip.done", model.getFormats().date(task.getActualFinish() != null ? task.getActualFinish() : task.getFinish()))).append("<br>");
            case DEADLINE_MISSED -> sb.append(I18n.t("tooltip.deadline", model.getFormats().date(task.getDeadline()))).append("<br>");
            case HYPERLINK -> sb.append(I18n.t("indicator.HYPERLINK")).append(": ").append(escape(task.getHyperlink())).append("<br>");
            case MODE_AUTO -> sb.append(I18n.t("tooltip.modeAuto")).append("<br>");
            case MODE_MANUAL -> sb.append(I18n.t("tooltip.modeManual")).append("<br>");
            default -> sb.append(I18n.t(i.key())).append("<br>");
         }
      }
      return sb.append("</html>").toString();
   }

   private static String htmlNotes(String notes)
   {
      String text = notes == null ? "" : notes.strip();
      if (text.length() > 600)
      {
         text = text.substring(0, 600) + " …";
      }
      return escape(text).replace("\n", "<br>");
   }

   private static String escape(String s)
   {
      return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
   }

   // ------------------------------------------------------------------ Maus und Tastatur

   private void installMouse()
   {
      table.addMouseListener(new MouseAdapter()
      {
         @Override
         public void mousePressed(MouseEvent e)
         {
            int row = table.rowAtPoint(e.getPoint());
            int col = table.columnAtPoint(e.getPoint());
            if (row < 0 || col < 0 || !SwingUtilities.isLeftMouseButton(e))
            {
               return;
            }
            CellContent c = table.contentAt(row, col);
            Rectangle r = table.getCellRect(row, col, false);
            if (CellPainter.hitsTriangle(c, e.getX() - r.x))
            {
               toggle(row);
               e.consume();
            }
         }

         @Override
         public void mouseClicked(MouseEvent e)
         {
            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
            {
               int row = table.rowAtPoint(e.getPoint());
               openRow(row);
            }
         }
      });

      chart.addMouseListener(new MouseAdapter()
      {
         @Override
         public void mousePressed(MouseEvent e)
         {
            int row = chart.rowAt(e.getY());
            if (row < 0)
            {
               return;
            }
            if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0)
            {
               table.addRowSelectionInterval(row, row);
            }
            else if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0 && table.getSelectedRow() >= 0)
            {
               table.setRowSelectionInterval(table.getSelectionModel().getAnchorSelectionIndex(), row);
            }
            else
            {
               table.setRowSelectionInterval(row, row);
            }
            table.requestFocusInWindow();
            if (e.getClickCount() == 2)
            {
               openRow(row);
            }
         }
      });

      MouseAdapter wheel = new MouseAdapter()
      {
         @Override
         public void mouseWheelMoved(MouseWheelEvent e)
         {
            handleWheel(e);
         }
      };
      tableScroll.addMouseWheelListener(new MouseAdapter()
      {
         @Override
         public void mouseWheelMoved(MouseWheelEvent e)
         {
            if (chartVisible)
            {
               handleWheel(e);
            }
         }
      });
      chartScroll.addMouseWheelListener(wheel);
   }

   private void handleWheel(MouseWheelEvent e)
   {
      if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0)
      {
         if (e.getWheelRotation() < 0)
         {
            zoomIn();
         }
         else if (e.getWheelRotation() > 0)
         {
            zoomOut();
         }
         return;
      }
      boolean horizontal = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
      JScrollBar bar = horizontal ? chartScroll.getHorizontalScrollBar() : chartScroll.getVerticalScrollBar();
      int amount = horizontal ? (int) Math.max(30, model.getTimeline().getPxPerDay() * 2) : Style.ROW_HEIGHT * 3;
      double rotation = e.getPreciseWheelRotation();
      bar.setValue(bar.getValue() + (int) Math.round(rotation * amount));
   }

   private void installKeys()
   {
      bind(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "collapse", () -> setSelectedExpanded(false));
      bind(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "collapse2", () -> setSelectedExpanded(false));
      bind(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "expand", () -> setSelectedExpanded(true));
      bind(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "expand2", () -> setSelectedExpanded(true));
      bind(KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "scrollToTask", this::scrollToSelectedTask);
      bind(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "copy", this::copySelection);
      bind(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open", () -> openRow(table.getSelectedRow()));
      bind(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK), "collapse3", () -> setSelectedExpanded(false));
      bind(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK), "expand3", () -> setSelectedExpanded(true));
   }

   private void bind(KeyStroke ks, String name, Runnable action)
   {
      table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, name);
      table.getInputMap(JComponent.WHEN_FOCUSED).put(ks, name);
      table.getActionMap().put(name, new javax.swing.AbstractAction()
      {
         private static final long serialVersionUID = 1L;

         @Override
         public void actionPerformed(ActionEvent e)
         {
            action.run();
         }
      });
   }

   private void openRow(int row)
   {
      List<TaskRow> rows = model.getOutline().getRows();
      if (row >= 0 && row < rows.size())
      {
         onOpenTask.accept(rows.get(row).task());
      }
   }

   private void toggle(int row)
   {
      Task task = model.getOutline().getRows().get(row).task();
      model.getOutline().toggle(task);
      afterOutlineChange(task);
   }

   private void setSelectedExpanded(boolean expanded)
   {
      List<Task> selected = getSelectedTasks();
      for (Task t : selected)
      {
         model.getOutline().setExpanded(t, expanded);
      }
      afterOutlineChange(selected.isEmpty() ? null : selected.get(0));
   }

   /**
    * Nach dem Auf-/Zuklappen Tabelle und Diagramm aktualisieren und die Auswahl erhalten.
    */
   public void afterOutlineChange(Task keepSelected)
   {
      refresh();
      if (keepSelected != null)
      {
         selectTask(keepSelected, false);
      }
   }

   public void setOnOpenTask(Consumer<Task> handler)
   {
      this.onOpenTask = handler;
   }

   public void setOnQueryChanged(Runnable handler)
   {
      this.onQueryChanged = handler;
   }

   private void installAutoFilter()
   {
      table.setAutoFilterSupport(new SheetTable.AutoFilterSupport()
      {
         @Override
         public boolean isEnabled()
         {
            return model.isAutoFilter();
         }

         @Override
         public boolean isFiltered(ColumnDef column)
         {
            return model.getQuery().isColumnFiltered(column.getField());
         }

         @Override
         public int sortDirection(ColumnDef column)
         {
            org.mpxj.FieldType f = model.getQuery().getSortField();
            if (f == null || f != column.getField())
            {
               return 0;
            }
            return model.getQuery().isAscending() ? 1 : -1;
         }

         @Override
         public void open(ColumnDef column, java.awt.Component header, Rectangle cell)
         {
            List<String> texts = new ArrayList<>();
            for (Task t : model.getDoc().getTasks())
            {
               if (!t.getNull())
               {
                  texts.add(model.getPresenter().taskText(t, column.getField()));
               }
            }
            List<String> values = AutoFilterPopup.distinct(texts, I18n.locale());
            AutoFilterPopup.show(header, cell, column, values, model.getQuery().getColumnFilter(column.getField()), new AutoFilterPopup.Actions()
            {
               @Override
               public void sort(ColumnDef c, boolean ascending)
               {
                  model.getQuery().sortBy(c.getField(), ascending);
                  queryChanged();
               }

               @Override
               public void filter(ColumnDef c, java.util.Set<String> allowed)
               {
                  model.getQuery().setColumnFilter(c.getField(), allowed);
                  queryChanged();
               }
            });
         }
      });
   }

   /**
    * Nach Änderung von Sortierung oder Filter neu aufbauen und die Auswahl erhalten.
    */
   public void queryChanged()
   {
      Task selected = getSelectedTask();
      model.applyQuery();
      afterOutlineChange(selected);
      table.getTableHeader().repaint();
      onQueryChanged.run();
   }

   public void setOnZoomChanged(Runnable handler)
   {
      this.onZoomChanged = handler;
   }

   // ------------------------------------------------------------------ Auswahl

   public List<Task> getSelectedTasks()
   {
      List<Task> result = new ArrayList<>();
      List<TaskRow> rows = model.getOutline().getRows();
      for (int i : table.getSelectedRows())
      {
         if (i >= 0 && i < rows.size())
         {
            result.add(rows.get(i).task());
         }
      }
      return result;
   }

   public Task getSelectedTask()
   {
      List<Task> list = getSelectedTasks();
      return list.isEmpty() ? null : list.get(0);
   }

   public void selectTask(Task task, boolean scrollChart)
   {
      int row = model.getOutline().indexOf(task);
      if (row < 0)
      {
         return;
      }
      table.setRowSelectionInterval(row, row);
      table.scrollRectToVisible(table.getCellRect(row, 0, true));
      if (scrollChart)
      {
         scrollToTask(task);
      }
   }

   public SheetTable getTable()
   {
      return table;
   }

   public void focusTable()
   {
      table.requestFocusInWindow();
   }

   /**
    * Kopiert die markierten Zeilen als Tabulator-getrennten Text (lässt sich in Excel einfügen).
    */
   public void copySelection()
   {
      int[] selected = table.getSelectedRows();
      if (selected.length == 0)
      {
         return;
      }
      List<ColumnDef> cols = table.currentColumnDefs();
      StringBuilder sb = new StringBuilder();
      List<TaskRow> rows = model.getOutline().getRows();
      for (int i = 0; i < cols.size(); i++)
      {
         if (i > 0)
         {
            sb.append('\t');
         }
         sb.append(cols.get(i).getKind() == ColumnDef.Kind.INDICATORS ? I18n.t("column.indicators") : cols.get(i).getTitle());
      }
      sb.append('\n');
      for (int r : selected)
      {
         TaskRow row = rows.get(r);
         for (int i = 0; i < cols.size(); i++)
         {
            if (i > 0)
            {
               sb.append('\t');
            }
            ColumnDef col = cols.get(i);
            String text = switch (col.getKind())
            {
               case INDICATORS -> row.task().getNull() ? "" : io.github.triplethep.mppviewer.render.CellPresenter.indicatorText(model.getPresenter().taskIndicators(row.task()));
               case TASK_MODE -> row.task().getNull() ? "" : model.getFormats().taskMode(row.task().getTaskMode());
               default -> model.getPresenter().task(row, col).text();
            };
            sb.append(text.replace('\t', ' ').replace('\n', ' '));
         }
         sb.append('\n');
      }
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
   }

   // ------------------------------------------------------------------ Zoom und Bildlauf

   public double getPxPerDay()
   {
      return model.getTimeline().getPxPerDay();
   }

   public void zoomIn()
   {
      setPxPerDay(ZoomLevel.nearest(getPxPerDay()).zoomIn().pxPerDay());
   }

   public void zoomOut()
   {
      setPxPerDay(ZoomLevel.nearest(getPxPerDay()).zoomOut().pxPerDay());
   }

   /**
    * Zoom ändern und dabei das Datum in der Bildmitte festhalten.
    */
   public void setPxPerDay(double ppd)
   {
      ppd = Math.max(ZoomLevel.values()[0].pxPerDay() * 0.5, Math.min(ZoomLevel.values()[ZoomLevel.values().length - 1].pxPerDay(), ppd));
      JViewport vp = chartScroll.getViewport();
      Rectangle view = vp.getViewRect();
      LocalDateTime center = model.getTimeline().time(view.x + view.width / 2.0);
      model.setPxPerDay(ppd);
      chart.revalidate();
      header.revalidate();
      chartScroll.validate();
      int x = (int) Math.round(model.getTimeline().x(center) - view.width / 2.0);
      scrollChartTo(x);
      chart.repaint();
      header.repaint();
      onZoomChanged.run();
   }

   /**
    * „Gesamtes Projekt“: Zoom so wählen, dass das ganze Projekt in den Diagrammbereich passt.
    */
   public void zoomEntireProject()
   {
      zoomRange(model.getDoc().getStart(), model.getDoc().getFinish());
   }

   /**
    * „Ausgewählte Vorgänge“: auf die markierten Vorgänge zoomen.
    */
   public void zoomSelectedTasks()
   {
      LocalDateTime min = null;
      LocalDateTime max = null;
      for (Task t : getSelectedTasks())
      {
         if (t.getStart() != null && (min == null || t.getStart().isBefore(min)))
         {
            min = t.getStart();
         }
         if (t.getFinish() != null && (max == null || t.getFinish().isAfter(max)))
         {
            max = t.getFinish();
         }
      }
      if (min != null && max != null)
      {
         zoomRange(min, max);
      }
   }

   private void zoomRange(LocalDateTime from, LocalDateTime to)
   {
      int width = Math.max(200, chartScroll.getViewport().getExtentSize().width - 40);
      double days = Math.max(1, java.time.Duration.between(from.toLocalDate().atStartOfDay(), to.toLocalDate().plusDays(1).atStartOfDay()).toMinutes() / 1440.0);
      double ppd = width / (days + 1);
      ppd = Math.max(ZoomLevel.values()[0].pxPerDay() * 0.5, Math.min(ZoomLevel.values()[ZoomLevel.values().length - 1].pxPerDay(), ppd));
      model.setPxPerDay(ppd);
      chart.revalidate();
      header.revalidate();
      chartScroll.validate();
      scrollChartTo((int) Math.round(model.getTimeline().x(from.toLocalDate().atStartOfDay()) - 20));
      chart.repaint();
      header.repaint();
      onZoomChanged.run();
   }

   public void scrollToProjectStart()
   {
      Timeline tl = model.getTimeline();
      scrollChartTo((int) Math.round(tl.x(model.getDoc().getStart().toLocalDate().atStartOfDay()) - Math.max(10, tl.getPxPerDay())));
   }

   public void scrollToToday()
   {
      Timeline tl = model.getTimeline();
      int width = chartScroll.getViewport().getExtentSize().width;
      scrollChartTo((int) Math.round(tl.x(LocalDateTime.now()) - width / 3.0));
   }

   /**
    * „Bildlauf zu Vorgang“ (Strg+Umschalt+F5 in Project).
    */
   public void scrollToSelectedTask()
   {
      Task task = getSelectedTask();
      if (task != null)
      {
         scrollToTask(task);
      }
   }

   private void scrollToTask(Task task)
   {
      double x = chart.painter().startXOf(task);
      if (x >= 0)
      {
         scrollChartTo((int) Math.round(x - Math.max(20, model.getTimeline().getPxPerDay())));
      }
   }

   private void scrollChartTo(int x)
   {
      JViewport vp = chartScroll.getViewport();
      int max = Math.max(0, chart.getPreferredSize().width - vp.getExtentSize().width);
      Point p = vp.getViewPosition();
      vp.setViewPosition(new Point(Math.max(0, Math.min(max, x)), p.y));
   }
}
