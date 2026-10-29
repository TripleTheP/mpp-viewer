package io.github.triplethep.mppviewer.core;

import org.mpxj.MPXJException;
import org.mpxj.ProjectCalendar;
import org.mpxj.ProjectFile;
import org.mpxj.ProjectProperties;
import org.mpxj.Resource;
import org.mpxj.Table;
import org.mpxj.Task;
import org.mpxj.View;
import org.mpxj.ViewState;
import org.mpxj.ViewType;
import org.mpxj.mpp.GanttChartView;
import org.mpxj.reader.UniversalProjectReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Eine geladene Projektdatei samt vorbereiteter Daten für die Anzeige. Wird nie verändert.
 */
public final class ProjectDocument
{
   private final File file;
   private final ProjectFile project;
   private final List<Task> tasks;
   private final Task projectSummaryTask;
   private final List<Resource> resources;
   private final LocalDateTime start;
   private final LocalDateTime finish;
   private final ProjectCalendar calendar;
   private final Map<LocalDate, Boolean> workingDates = new ConcurrentHashMap<>();
   private final GanttChartView ganttView;
   private final String savedViewName;
   private final List<Integer> savedTaskOrder;
   private final List<org.mpxj.Filter> savedFilters;

   private ProjectDocument(File file, ProjectFile project)
   {
      this.file = file;
      this.project = project;

      List<Task> all = new ArrayList<>(project.getTasks());
      all.sort(Comparator.comparing(Task::getID, Comparator.nullsLast(Comparator.naturalOrder())));
      Task summary = null;
      List<Task> list = new ArrayList<>();
      for (Task task : all)
      {
         Integer id = task.getID();
         Integer level = task.getOutlineLevel();
         if ((id != null && id.intValue() == 0) || (level != null && level.intValue() == 0))
         {
            if (summary == null)
            {
               summary = task;
            }
            continue;
         }
         list.add(task);
      }
      this.tasks = Collections.unmodifiableList(list);
      this.projectSummaryTask = summary;

      List<Resource> res = new ArrayList<>();
      for (Resource r : project.getResources())
      {
         Integer id = r.getID();
         if (id != null && id.intValue() == 0)
         {
            continue; // interner "Nicht zugeordnet"-Datensatz
         }
         res.add(r);
      }
      res.sort(Comparator.comparing(Resource::getID, Comparator.nullsLast(Comparator.naturalOrder())));
      this.resources = Collections.unmodifiableList(res);

      LocalDateTime[] range = computeRange(project, list, summary);
      this.start = range[0];
      this.finish = range[1];
      this.calendar = project.getDefaultCalendar();

      ViewState state = null;
      try
      {
         state = project.getViews().getViewState();
      }
      catch (RuntimeException ex)
      {
         // nicht alle Formate kennen einen Ansichtsstatus
      }
      this.savedViewName = state == null ? null : state.getViewName();
      this.ganttView = findGanttView(project, savedViewName);
      this.savedTaskOrder = savedOrder(project, state, list);
      this.savedFilters = savedFilters(state, ganttView);
   }

   /**
    * Zeilenreihenfolge der beim Speichern aktiven Vorgangsansicht, falls sie von der Nr.-Reihenfolge abweicht
    * (die Ansicht war sortiert). Sonst leer.
    */
   private static List<Integer> savedOrder(ProjectFile project, ViewState state, List<Task> tasks)
   {
      if (state == null || state.getUniqueIdList() == null || state.getUniqueIdList().size() < 2)
      {
         return List.of();
      }
      View view = null;
      for (View v : project.getViews())
      {
         if (v.getName() != null && v.getName().equalsIgnoreCase(state.getViewName()))
         {
            view = v;
            break;
         }
      }
      if (view != null && view.getType() != ViewType.GANTT_CHART && view.getType() != ViewType.TASK_SHEET)
      {
         return List.of();
      }
      java.util.Map<Integer, Task> byUid = new java.util.HashMap<>();
      for (Task t : tasks)
      {
         if (t.getUniqueID() != null)
         {
            byUid.put(t.getUniqueID(), t);
         }
      }
      List<Integer> unique = new ArrayList<>();
      java.util.Set<Integer> seen = new java.util.HashSet<>();
      boolean sorted = false;
      int lastId = Integer.MIN_VALUE;
      for (Integer uid : state.getUniqueIdList())
      {
         Task t = byUid.get(uid);
         if (t == null || !seen.add(uid))
         {
            continue;
         }
         unique.add(uid);
         int id = t.getID() == null ? 0 : t.getID().intValue();
         if (id < lastId)
         {
            sorted = true;
         }
         lastId = id;
      }
      return sorted && unique.size() >= 2 ? Collections.unmodifiableList(unique) : List.of();
   }

   private static List<org.mpxj.Filter> savedFilters(ViewState state, GanttChartView view)
   {
      List<org.mpxj.Filter> result = new ArrayList<>();
      try
      {
         org.mpxj.Filter filter = state == null ? null : state.getFilter();
         if (filter != null && filter.isTaskFilter() && filter.getCriteria() != null)
         {
            result.add(filter);
         }
      }
      catch (RuntimeException ex)
      {
         // Filter nicht lesbar
      }
      try
      {
         if (view != null && view.getAutoFilters() != null)
         {
            for (org.mpxj.Filter f : view.getAutoFilters())
            {
               if (f != null && f.getCriteria() != null)
               {
                  result.add(f);
               }
            }
         }
      }
      catch (RuntimeException ex)
      {
         // AutoFilter nicht lesbar
      }
      return Collections.unmodifiableList(result);
   }

   /**
    * Gespeicherte Zeilenreihenfolge (eindeutige Nr.), leer wenn unsortiert.
    */
   public List<Integer> getSavedTaskOrder()
   {
      return savedTaskOrder;
   }

   /**
    * Beim Speichern aktive Filter und AutoFilter der Vorgangsansicht.
    */
   public List<org.mpxj.Filter> getSavedFilters()
   {
      return savedFilters;
   }

   public boolean isAutoFilterEnabled()
   {
      try
      {
         return getProperties().getAutoFilter() || (ganttView != null && ganttView.getAutoFilters() != null && !ganttView.getAutoFilters().isEmpty());
      }
      catch (RuntimeException ex)
      {
         return false;
      }
   }

   /**
    * Liest eine Projektdatei. Unterstützt .mpp (Project 98 bis heute), .mpt, .mpx, Project-XML und alle weiteren
    * Formate, die MPXJ erkennt.
    */
   public static ProjectDocument load(File file) throws ProjectLoadException
   {
      if (!file.isFile())
      {
         throw new ProjectLoadException(ProjectLoadException.Reason.NOT_FOUND, null);
      }
      ProjectFile project;
      // Den Stream selbst öffnen und schließen: read(File) lässt die Datei offen, wenn MPXJ schon beim Erkennen des
      // Formats scheitert – unter Windows bliebe sie dann bis zum Programmende gesperrt.
      try (InputStream in = Files.newInputStream(file.toPath()))
      {
         project = new UniversalProjectReader().read(in);
      }
      catch (IOException ex)
      {
         throw new ProjectLoadException(ProjectLoadException.Reason.READ_ERROR, ex);
      }
      catch (MPXJException ex)
      {
         String msg = String.valueOf(ex.getMessage()).toLowerCase(Locale.ROOT);
         if (msg.contains("password"))
         {
            throw new ProjectLoadException(ProjectLoadException.Reason.PASSWORD_PROTECTED, ex);
         }
         if (ex.getCause() instanceof IOException)
         {
            throw new ProjectLoadException(ProjectLoadException.Reason.READ_ERROR, ex);
         }
         throw new ProjectLoadException(ProjectLoadException.Reason.INVALID_FILE, ex);
      }
      catch (RuntimeException ex)
      {
         throw new ProjectLoadException(ProjectLoadException.Reason.INVALID_FILE, ex);
      }
      if (project == null)
      {
         throw new ProjectLoadException(ProjectLoadException.Reason.UNSUPPORTED, null);
      }
      return new ProjectDocument(file, project);
   }

   /**
    * Für Tests und Beispiele: ein im Speicher erzeugtes Projekt anzeigen.
    */
   public static ProjectDocument of(File file, ProjectFile project)
   {
      return new ProjectDocument(file, project);
   }

   public File getFile()
   {
      return file;
   }

   public ProjectFile getProject()
   {
      return project;
   }

   public ProjectProperties getProperties()
   {
      return project.getProjectProperties();
   }

   /**
    * Alle Vorgänge in Anzeigereihenfolge, ohne den Projektsammelvorgang (Nr. 0).
    */
   public List<Task> getTasks()
   {
      return tasks;
   }

   public Task getProjectSummaryTask()
   {
      return projectSummaryTask;
   }

   public List<Resource> getResources()
   {
      return resources;
   }

   public LocalDateTime getStart()
   {
      return start;
   }

   public LocalDateTime getFinish()
   {
      return finish;
   }

   public GanttChartView getGanttView()
   {
      return ganttView;
   }

   public String getSavedViewName()
   {
      return savedViewName;
   }

   public String getTitle()
   {
      ProjectProperties props = getProperties();
      String title = props.getProjectTitle();
      if (title == null || title.isBlank())
      {
         title = props.getName();
      }
      if ((title == null || title.isBlank()) && projectSummaryTask != null)
      {
         title = projectSummaryTask.getName();
      }
      if (title == null || title.isBlank())
      {
         title = file == null ? "" : stripExtension(file.getName());
      }
      return title;
   }

   public String getFileName()
   {
      return file == null ? getTitle() : file.getName();
   }

   public boolean isShowProjectSummaryTaskDefault()
   {
      try
      {
         return getProperties().getShowProjectSummaryTask() && projectSummaryTask != null;
      }
      catch (RuntimeException ex)
      {
         return false;
      }
   }

   public DayOfWeek getWeekStartDay()
   {
      DayOfWeek day = getProperties().getWeekStartDay();
      return day == null ? DayOfWeek.MONDAY : day;
   }

   public String getCurrencySymbol()
   {
      String symbol = getProperties().getCurrencySymbol();
      return symbol == null ? "€" : symbol;
   }

   /**
    * Arbeitstag laut Standardkalender des Projekts (zwischengespeichert, da die Kalenderberechnung teuer ist).
    */
   public boolean isWorkingDate(LocalDate date)
   {
      return workingDates.computeIfAbsent(date, d ->
      {
         if (calendar != null)
         {
            try
            {
               return Boolean.valueOf(calendar.isWorkingDate(d));
            }
            catch (RuntimeException ex)
            {
               // Fallback unten
            }
         }
         DayOfWeek dow = d.getDayOfWeek();
         return Boolean.valueOf(dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY);
      }).booleanValue();
   }

   /**
    * Tabellen für Vorgangsansichten, wie sie in der Datei gespeichert sind.
    */
   public List<Table> getTaskTables()
   {
      List<Table> result = new ArrayList<>();
      for (Table table : project.getTables())
      {
         if (!table.getResourceFlag() && !table.getColumns().isEmpty())
         {
            result.add(table);
         }
      }
      return result;
   }

   public List<Table> getResourceTables()
   {
      List<Table> result = new ArrayList<>();
      for (Table table : project.getTables())
      {
         if (table.getResourceFlag() && !table.getColumns().isEmpty())
         {
            result.add(table);
         }
      }
      return result;
   }

   /**
    * Tabelle der Ansicht, die beim Speichern aktiv war, sonst die der Gantt-Ansicht.
    */
   public Table getDefaultTaskTable()
   {
      Table table = null;
      if (ganttView != null)
      {
         table = ganttView.getTable();
      }
      if (table == null)
      {
         for (Table t : getTaskTables())
         {
            String name = normalize(t.getName());
            if (name.equals("entry") || name.equals("eingabe"))
            {
               return t;
            }
         }
      }
      return table != null && !table.getColumns().isEmpty() ? table : null;
   }

   public Table getDefaultResourceTable()
   {
      for (View view : project.getViews())
      {
         if (view.getType() == ViewType.RESOURCE_SHEET)
         {
            Table table = safeTable(view);
            if (table != null && table.getResourceFlag() && !table.getColumns().isEmpty())
            {
               return table;
            }
         }
      }
      for (Table t : getResourceTables())
      {
         String name = normalize(t.getName());
         if (name.equals("entry") || name.equals("eingabe"))
         {
            return t;
         }
      }
      return null;
   }

   public static String displayName(String name)
   {
      return name == null ? "" : name.replace("&", "").trim();
   }

   private static String normalize(String name)
   {
      return displayName(name).toLowerCase(Locale.ROOT);
   }

   private static Table safeTable(View view)
   {
      try
      {
         return view.getTable();
      }
      catch (RuntimeException ex)
      {
         return null;
      }
   }

   private static GanttChartView findGanttView(ProjectFile project, String savedViewName)
   {
      GanttChartView firstGantt = null;
      GanttChartView standard = null;
      try
      {
         for (View view : project.getViews())
         {
            if (!(view instanceof GanttChartView))
            {
               continue;
            }
            GanttChartView gantt = (GanttChartView) view;
            String name = normalize(view.getName());
            if (savedViewName != null && name.equals(normalize(savedViewName)))
            {
               return gantt;
            }
            if (firstGantt == null)
            {
               firstGantt = gantt;
            }
            if (standard == null && (name.equals("gantt chart") || name.equals("gantt-diagramm")))
            {
               standard = gantt;
            }
         }
      }
      catch (RuntimeException ex)
      {
         return null;
      }
      return standard != null ? standard : firstGantt;
   }

   private static LocalDateTime[] computeRange(ProjectFile project, List<Task> tasks, Task summary)
   {
      LocalDateTime min = null;
      LocalDateTime max = null;
      for (Task task : tasks)
      {
         min = earliest(min, task.getStart());
         min = earliest(min, task.getBaselineStart());
         max = latest(max, task.getFinish());
         max = latest(max, task.getBaselineFinish());
         max = latest(max, task.getDeadline());
      }
      if (min == null && summary != null)
      {
         min = summary.getStart();
         max = summary.getFinish();
      }
      ProjectProperties props = project.getProjectProperties();
      if (min == null)
      {
         min = props.getStartDate();
      }
      if (max == null)
      {
         max = props.getFinishDate();
      }
      if (min == null)
      {
         min = LocalDate.now().atStartOfDay();
      }
      if (max == null || max.isBefore(min))
      {
         max = min.plusDays(14);
      }
      return new LocalDateTime[]
      {
         min,
         max
      };
   }

   private static LocalDateTime earliest(LocalDateTime a, LocalDateTime b)
   {
      if (b == null)
      {
         return a;
      }
      return a == null || b.isBefore(a) ? b : a;
   }

   private static LocalDateTime latest(LocalDateTime a, LocalDateTime b)
   {
      if (b == null)
      {
         return a;
      }
      return a == null || b.isAfter(a) ? b : a;
   }

   private static String stripExtension(String name)
   {
      int dot = name.lastIndexOf('.');
      return dot > 0 ? name.substring(0, dot) : name;
   }
}
