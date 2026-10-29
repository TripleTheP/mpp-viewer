package io.github.triplethep.mppviewer.core;

import org.mpxj.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Gliederung der Vorgänge mit Auf-/Zuklappen, Sortierung und Filter. Liefert die Liste der aktuell sichtbaren
 * Zeilen. Sortiert wird wie in Project mit „Gliederungsstruktur beibehalten“: nur Geschwister untereinander.
 */
public final class TaskOutline
{
   private final ProjectDocument doc;
   private final Map<Integer, Boolean> expanded = new HashMap<>();
   private final List<Task> roots;
   private boolean showProjectSummary;
   private int maxLevel = Integer.MAX_VALUE;
   private Comparator<Task> order;
   private Predicate<Task> filter;
   private boolean showRelatedSummaries = true;
   private List<TaskRow> rows = List.of();
   private Map<Integer, Integer> rowByUid = Map.of();

   public TaskOutline(ProjectDocument doc, boolean showProjectSummary)
   {
      this.doc = doc;
      this.showProjectSummary = showProjectSummary;
      this.roots = findRoots(doc);
      for (Task task : doc.getTasks())
      {
         if (hasChildren(task) && task.getUniqueID() != null)
         {
            expanded.put(task.getUniqueID(), Boolean.valueOf(initiallyExpanded(task)));
         }
      }
      rebuild();
   }

   /**
    * Kopie mit gleicher Sortierung und gleichem Filter, aber allen Teilvorgängen eingeblendet (für Exporte).
    */
   public TaskOutline expandedCopy()
   {
      TaskOutline copy = new TaskOutline(doc, showProjectSummary);
      copy.order = order;
      copy.filter = filter;
      copy.showRelatedSummaries = showRelatedSummaries;
      copy.showLevel(Integer.MAX_VALUE);
      return copy;
   }

   private static List<Task> findRoots(ProjectDocument doc)
   {
      Task summary = doc.getProjectSummaryTask();
      List<Task> list = new ArrayList<>();
      Set<Task> all = new HashSet<>(doc.getTasks());
      for (Task task : doc.getTasks())
      {
         Task parent = task.getParentTask();
         if (parent == null || parent == summary || !all.contains(parent))
         {
            list.add(task);
         }
      }
      return list;
   }

   private static boolean initiallyExpanded(Task task)
   {
      try
      {
         return task.getExpanded();
      }
      catch (RuntimeException ex)
      {
         return true;
      }
   }

   private static boolean hasChildren(Task task)
   {
      List<Task> children = task.getChildTasks();
      return children != null && !children.isEmpty();
   }

   public List<TaskRow> getRows()
   {
      return rows;
   }

   /**
    * Zeilenindex eines Vorgangs (über die eindeutige Nr.) oder -1, wenn er gerade nicht sichtbar ist.
    */
   public int indexOf(Task task)
   {
      if (task == null || task.getUniqueID() == null)
      {
         return -1;
      }
      Integer row = rowByUid.get(task.getUniqueID());
      return row == null ? -1 : row.intValue();
   }

   public boolean isShowProjectSummary()
   {
      return showProjectSummary;
   }

   public void setShowProjectSummary(boolean show)
   {
      this.showProjectSummary = show;
      rebuild();
   }

   /**
    * Sortierung der Geschwister; {@code null} = Reihenfolge nach Nr.
    */
   public void setOrder(Comparator<Task> order)
   {
      this.order = order;
      rebuild();
   }

   public Comparator<Task> getOrder()
   {
      return order;
   }

   /**
    * Filter; {@code null} zeigt alle Vorgänge.
    *
    * @param showRelatedSummaries Sammelvorgänge passender Vorgänge mit anzeigen (Project-Standard bei AutoFilter)
    */
   public void setFilter(Predicate<Task> filter, boolean showRelatedSummaries)
   {
      this.filter = filter;
      this.showRelatedSummaries = showRelatedSummaries;
      rebuild();
   }

   public boolean isFiltered()
   {
      return filter != null;
   }

   public void toggle(Task task)
   {
      if (task == null || task.getUniqueID() == null)
      {
         return;
      }
      Integer uid = task.getUniqueID();
      if (expanded.containsKey(uid))
      {
         expanded.put(uid, Boolean.valueOf(!expanded.get(uid).booleanValue()));
         maxLevel = Integer.MAX_VALUE;
         rebuild();
      }
   }

   public void setExpanded(Task task, boolean value)
   {
      if (task != null && task.getUniqueID() != null && expanded.containsKey(task.getUniqueID()))
      {
         expanded.put(task.getUniqueID(), Boolean.valueOf(value));
         maxLevel = Integer.MAX_VALUE;
         rebuild();
      }
   }

   /**
    * Wie „Gliederung anzeigen → Ebene n“ in Project; {@code Integer.MAX_VALUE} zeigt alle Teilvorgänge.
    */
   public void showLevel(int level)
   {
      this.maxLevel = level;
      for (Map.Entry<Integer, Boolean> e : expanded.entrySet())
      {
         e.setValue(Boolean.TRUE);
      }
      if (level != Integer.MAX_VALUE)
      {
         for (Task task : doc.getTasks())
         {
            Integer uid = task.getUniqueID();
            if (uid != null && expanded.containsKey(uid))
            {
               expanded.put(uid, Boolean.valueOf(level(task) < level));
            }
         }
      }
      rebuild();
   }

   public int getMaxLevel()
   {
      return maxLevel;
   }

   /**
    * Höchste Gliederungsebene im Projekt.
    */
   public int getDeepestLevel()
   {
      int max = 1;
      for (Task task : doc.getTasks())
      {
         max = Math.max(max, level(task));
      }
      return max;
   }

   private static int level(Task task)
   {
      Integer level = task.getOutlineLevel();
      return level == null ? 1 : Math.max(1, level.intValue());
   }

   private void rebuild()
   {
      List<TaskRow> list = new ArrayList<>();
      Task summary = doc.getProjectSummaryTask();
      if (showProjectSummary && summary != null)
      {
         list.add(new TaskRow(summary, 0, true, true));
      }

      Set<Task> visible = filter == null ? null : visibleByFilter();
      addAll(list, roots, visible);

      Map<Integer, Integer> index = new HashMap<>();
      for (int i = 0; i < list.size(); i++)
      {
         Integer uid = list.get(i).task().getUniqueID();
         if (uid != null)
         {
            index.putIfAbsent(uid, Integer.valueOf(i));
         }
      }
      this.rows = Collections.unmodifiableList(list);
      this.rowByUid = index;
   }

   private void addAll(List<TaskRow> out, List<Task> siblings, Set<Task> visible)
   {
      List<Task> sorted = siblings;
      if (order != null)
      {
         sorted = new ArrayList<>(siblings);
         sorted.sort(order);
      }
      for (Task task : sorted)
      {
         boolean children = hasChildren(task);
         if (visible != null && !visible.contains(task))
         {
            // Sammelvorgang selbst ausgefiltert: passende Teilvorgänge trotzdem zeigen
            if (children)
            {
               addAll(out, task.getChildTasks(), visible);
            }
            continue;
         }
         boolean isExpanded = true;
         if (children)
         {
            Boolean state = task.getUniqueID() == null ? null : expanded.get(task.getUniqueID());
            isExpanded = state == null || state.booleanValue();
         }
         out.add(new TaskRow(task, level(task), children, isExpanded));
         if (children && isExpanded)
         {
            addAll(out, task.getChildTasks(), visible);
         }
      }
   }

   /**
    * Vorgänge, die der Filter zeigt – bei Bedarf samt ihren Sammelvorgängen.
    */
   private Set<Task> visibleByFilter()
   {
      Set<Task> result = new HashSet<>();
      for (Task task : doc.getTasks())
      {
         boolean pass;
         try
         {
            pass = filter.test(task);
         }
         catch (RuntimeException ex)
         {
            pass = true;
         }
         if (pass)
         {
            result.add(task);
            if (showRelatedSummaries)
            {
               Task parent = task.getParentTask();
               while (parent != null)
               {
                  result.add(parent);
                  parent = parent.getParentTask();
               }
            }
         }
      }
      return result;
   }
}
