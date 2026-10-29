package io.github.triplethep.mppviewer.ui.view;

import io.github.triplethep.mppviewer.core.CellStyles;
import io.github.triplethep.mppviewer.core.ColumnDef;
import io.github.triplethep.mppviewer.core.ColumnSets;
import io.github.triplethep.mppviewer.core.Formats;
import io.github.triplethep.mppviewer.core.ProjectDocument;
import io.github.triplethep.mppviewer.core.TaskOutline;
import io.github.triplethep.mppviewer.core.TaskQuery;
import io.github.triplethep.mppviewer.render.CellPresenter;
import io.github.triplethep.mppviewer.render.GanttOptions;
import io.github.triplethep.mppviewer.render.Timeline;
import io.github.triplethep.mppviewer.render.ViewKind;
import io.github.triplethep.mppviewer.render.ZoomLevel;
import org.mpxj.Table;
import org.mpxj.mpp.GanttChartView;
import org.mpxj.mpp.TimescaleTier;
import org.mpxj.TimescaleUnits;

import java.util.List;

/**
 * Zustand der Anzeige eines geöffneten Projekts: Gliederung, Spalten, Zoom und Optionen.
 */
public final class ProjectViewModel
{
   private final ProjectDocument doc;
   private final TaskOutline outline;
   private final GanttOptions options = new GanttOptions();
   private Formats formats;
   private CellPresenter presenter;
   private Table taskTable;
   private List<ColumnDef> taskColumns;
   private List<ColumnDef> resourceColumns;
   private Timeline timeline;
   private ViewKind kind = ViewKind.GANTT;
   private int dividerLocation;
   private final TaskQuery query;
   private boolean autoFilter;

   public ProjectViewModel(ProjectDocument doc)
   {
      this.doc = doc;
      this.outline = new TaskOutline(doc, doc.isShowProjectSummaryTaskDefault());
      this.formats = Formats.current(doc.getProperties());
      this.presenter = new CellPresenter(formats, doc.getProject().getDefaultCalendar(), CellStyles.of(doc));
      this.taskTable = doc.getDefaultTaskTable();
      this.taskColumns = ColumnSets.fromTable(taskTable, false);
      this.resourceColumns = ColumnSets.fromTable(doc.getDefaultResourceTable(), true);

      GanttChartView view = doc.getGanttView();
      double ppd = ZoomLevel.DEFAULT.pxPerDay();
      if (view != null)
      {
         try
         {
            options.roundBarsToWholeDays = view.getRoundBarsToWholeDays();
            ppd = initialZoom(view);
            int tableWidth = view.getTableWidth();
            if (tableWidth > 60)
            {
               dividerLocation = tableWidth;
            }
         }
         catch (RuntimeException ex)
         {
            // Ansichtsdaten unvollständig – Standardwerte verwenden
         }
      }
      else
      {
         options.roundBarsToWholeDays = true;
      }
      this.timeline = Timeline.forProject(doc, ppd);
      this.query = new TaskQuery(doc, io.github.triplethep.mppviewer.i18n.I18n.locale());
      this.autoFilter = doc.isAutoFilterEnabled();
      applyQuery();
   }

   /**
    * Sortierung und Filter auf die Gliederung anwenden.
    */
   public void applyQuery()
   {
      outline.setOrder(query.comparator());
      CellPresenter p = presenter;
      outline.setFilter(query.predicate(p::taskText), true);
   }

   public TaskQuery getQuery()
   {
      return query;
   }

   public boolean isAutoFilter()
   {
      return autoFilter;
   }

   public void setAutoFilter(boolean autoFilter)
   {
      this.autoFilter = autoFilter;
   }

   /**
    * Zoomstufe aus der in der Datei gespeicherten Zeitskala ableiten.
    */
   private static double initialZoom(GanttChartView view)
   {
      TimescaleTier bottom = view.getTimescaleBottomTier();
      if (view.getTimescaleShowTiers() == 1 && view.getTimescaleMiddleTier() != null)
      {
         bottom = view.getTimescaleMiddleTier();
      }
      if (bottom == null || bottom.getUnits() == null)
      {
         return ZoomLevel.DEFAULT.pxPerDay();
      }
      TimescaleUnits units = bottom.getUnits();
      int count = Math.max(1, bottom.getCount());
      double size = view.getTimescaleSize() > 0 ? view.getTimescaleSize() / 100.0 : 1.0;
      ZoomLevel level = switch (units)
      {
         case MINUTES, HOURS -> ZoomLevel.DAYS_HOURS;
         case DAYS -> count >= 2 ? ZoomLevel.WEEKS_DAYS_M : ZoomLevel.WEEKS_DAYS;
         case WEEKS, THIRDS_OF_MONTHS -> ZoomLevel.MONTHS_WEEKS;
         case MONTHS -> ZoomLevel.QUARTERS_MONTHS;
         case QUARTERS, HALF_YEARS -> ZoomLevel.YEARS_QUARTERS;
         case YEARS -> ZoomLevel.YEARS_QUARTERS_S;
         default -> ZoomLevel.DEFAULT;
      };
      return ZoomLevel.nearest(level.pxPerDay() * Math.max(0.5, Math.min(2.0, size))).pxPerDay();
   }

   /**
    * Nach einem Sprachwechsel: Formate und Spaltentitel neu aufbauen, Breiten behalten.
    */
   public void languageChanged(List<ColumnDef> currentTaskColumns, List<ColumnDef> currentResourceColumns)
   {
      this.formats = Formats.current(doc.getProperties());
      this.presenter = new CellPresenter(formats, doc.getProject().getDefaultCalendar(), CellStyles.of(doc));
      this.taskColumns = retitle(ColumnSets.fromTable(taskTable, false), currentTaskColumns);
      this.resourceColumns = retitle(ColumnSets.fromTable(doc.getDefaultResourceTable(), true), currentResourceColumns);
      // Spaltenfilter beruhen auf angezeigten Texten, die sich mit der Sprache ändern
      for (ColumnDef c : taskColumns)
      {
         query.setColumnFilter(c.getField(), null);
      }
      applyQuery();
   }

   private static List<ColumnDef> retitle(List<ColumnDef> fresh, List<ColumnDef> current)
   {
      if (current == null || current.size() != fresh.size())
      {
         return fresh;
      }
      List<ColumnDef> result = new java.util.ArrayList<>();
      for (int i = 0; i < fresh.size(); i++)
      {
         result.add(fresh.get(i).withWidth(current.get(i).getWidth()));
      }
      return result;
   }

   public ProjectDocument getDoc()
   {
      return doc;
   }

   public TaskOutline getOutline()
   {
      return outline;
   }

   public GanttOptions getOptions()
   {
      return options;
   }

   public Formats getFormats()
   {
      return formats;
   }

   public CellPresenter getPresenter()
   {
      return presenter;
   }

   public Table getTaskTable()
   {
      return taskTable;
   }

   public void setTaskTable(Table table)
   {
      this.taskTable = table;
      this.taskColumns = ColumnSets.fromTable(table, false);
   }

   public List<ColumnDef> getTaskColumns()
   {
      return taskColumns;
   }

   public void setTaskColumns(List<ColumnDef> columns)
   {
      this.taskColumns = columns;
   }

   public List<ColumnDef> getResourceColumns()
   {
      return resourceColumns;
   }

   public void setResourceColumns(List<ColumnDef> columns)
   {
      this.resourceColumns = columns;
   }

   public Timeline getTimeline()
   {
      return timeline;
   }

   public void setPxPerDay(double ppd)
   {
      this.timeline = Timeline.forProject(doc, ppd);
   }

   public ViewKind getKind()
   {
      return kind;
   }

   public void setKind(ViewKind kind)
   {
      this.kind = kind;
      options.tracking = kind == ViewKind.TRACKING_GANTT;
   }

   public int getDividerLocation()
   {
      return dividerLocation;
   }

   public void setDividerLocation(int dividerLocation)
   {
      this.dividerLocation = dividerLocation;
   }
}
