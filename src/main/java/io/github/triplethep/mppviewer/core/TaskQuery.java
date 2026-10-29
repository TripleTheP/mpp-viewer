package io.github.triplethep.mppviewer.core;

import org.mpxj.Duration;
import org.mpxj.FieldType;
import org.mpxj.Filter;
import org.mpxj.Task;

import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Sortierung und Filter einer Vorgangsansicht: gespeicherte Sortierung und Filter aus der Datei sowie
 * AutoFilter-Auswahl und Sortierung des Benutzers (wie die Pfeile in den Spaltenköpfen von Project).
 */
public final class TaskQuery
{
   public enum SortMode
   {
      /** nach Nr. */
      ID,
      /** wie beim Speichern in Project */
      SAVED,
      /** nach einer Spalte */
      COLUMN
   }

   private final ProjectDocument doc;
   private final Map<Integer, Integer> savedPositions = new HashMap<>();
   private SortMode sortMode;
   private FieldType sortField;
   private boolean ascending = true;
   private boolean fileFiltersActive;
   private final Map<FieldType, Set<String>> columnFilters = new LinkedHashMap<>();
   private final Collator collator;

   public TaskQuery(ProjectDocument doc, Locale locale)
   {
      this.doc = doc;
      List<Integer> saved = doc.getSavedTaskOrder();
      for (int i = 0; i < saved.size(); i++)
      {
         savedPositions.put(saved.get(i), Integer.valueOf(i));
      }
      this.sortMode = saved.isEmpty() ? SortMode.ID : SortMode.SAVED;
      this.fileFiltersActive = !doc.getSavedFilters().isEmpty();
      this.collator = Collator.getInstance(locale);
      this.collator.setStrength(Collator.SECONDARY);
   }

   public boolean hasSavedOrder()
   {
      return !savedPositions.isEmpty();
   }

   public boolean hasFileFilters()
   {
      return !doc.getSavedFilters().isEmpty();
   }

   public SortMode getSortMode()
   {
      return sortMode;
   }

   public FieldType getSortField()
   {
      return sortMode == SortMode.COLUMN ? sortField : null;
   }

   public boolean isAscending()
   {
      return ascending;
   }

   public void sortById()
   {
      sortMode = SortMode.ID;
   }

   public void sortSaved()
   {
      if (hasSavedOrder())
      {
         sortMode = SortMode.SAVED;
      }
   }

   public void sortBy(FieldType field, boolean asc)
   {
      sortMode = SortMode.COLUMN;
      sortField = field;
      ascending = asc;
   }

   public boolean isFileFiltersActive()
   {
      return fileFiltersActive;
   }

   public void setFileFiltersActive(boolean active)
   {
      this.fileFiltersActive = active && hasFileFilters();
   }

   /**
    * AutoFilter-Auswahl einer Spalte: nur Vorgänge mit diesen Zelltexten anzeigen; {@code null} hebt den Filter auf.
    */
   public void setColumnFilter(FieldType field, Set<String> allowed)
   {
      if (allowed == null)
      {
         columnFilters.remove(field);
      }
      else
      {
         columnFilters.put(field, allowed);
      }
   }

   public Set<String> getColumnFilter(FieldType field)
   {
      return columnFilters.get(field);
   }

   public boolean isColumnFiltered(FieldType field)
   {
      return columnFilters.containsKey(field);
   }

   public void clearFilters()
   {
      columnFilters.clear();
      fileFiltersActive = false;
   }

   public boolean isFiltered()
   {
      return fileFiltersActive || !columnFilters.isEmpty();
   }

   /**
    * Sortierung für {@link TaskOutline#setOrder}.
    */
   public Comparator<Task> comparator()
   {
      return switch (sortMode)
      {
         case ID -> null;
         case SAVED -> Comparator.comparingLong(this::savedKey);
         case COLUMN -> columnComparator(sortField, ascending);
      };
   }

   private long savedKey(Task task)
   {
      Integer pos = task.getUniqueID() == null ? null : savedPositions.get(task.getUniqueID());
      long id = task.getID() == null ? 0 : task.getID().longValue();
      // Nicht gespeicherte Zeilen (z. B. in zugeklappten Sammelvorgängen) behalten ihre Nr.-Reihenfolge dahinter
      return pos != null ? pos.longValue() : 1_000_000_000L + id;
   }

   private Comparator<Task> columnComparator(FieldType field, boolean asc)
   {
      Comparator<Task> byValue = (a, b) ->
      {
         Object va = value(a, field);
         Object vb = value(b, field);
         if (va == null && vb == null)
         {
            return 0;
         }
         if (va == null)
         {
            return 1;
         }
         if (vb == null)
         {
            return -1;
         }
         int c = compareValues(va, vb);
         return asc ? c : -c;
      };
      return byValue.thenComparing(t -> t.getID() == null ? Integer.valueOf(0) : t.getID());
   }

   private static Object value(Task task, FieldType field)
   {
      try
      {
         Object v = task.get(field);
         if (v instanceof String s && s.isBlank())
         {
            return null;
         }
         return v;
      }
      catch (RuntimeException ex)
      {
         return null;
      }
   }

   @SuppressWarnings(
   {
      "unchecked",
      "rawtypes"
   })
   private int compareValues(Object a, Object b)
   {
      if (a instanceof Number na && b instanceof Number nb)
      {
         return Double.compare(na.doubleValue(), nb.doubleValue());
      }
      if (a instanceof Duration da && b instanceof Duration db)
      {
         return Double.compare(minutes(da), minutes(db));
      }
      if (a instanceof String sa && b instanceof String sb)
      {
         return collator.compare(sa, sb);
      }
      if (a instanceof Comparable ca && a.getClass().isInstance(b))
      {
         return ca.compareTo(b);
      }
      return collator.compare(a.toString(), b.toString());
   }

   private static double minutes(Duration d)
   {
      double v = d.getDuration();
      return switch (d.getUnits())
      {
         case MINUTES, ELAPSED_MINUTES -> v;
         case HOURS, ELAPSED_HOURS -> v * 60;
         case DAYS -> v * 480;
         case ELAPSED_DAYS -> v * 1440;
         case WEEKS -> v * 2400;
         case ELAPSED_WEEKS -> v * 10080;
         case MONTHS -> v * 9600;
         case ELAPSED_MONTHS -> v * 43200;
         case YEARS -> v * 115200;
         case ELAPSED_YEARS -> v * 525600;
         default -> v;
      };
   }

   /**
    * Filter für {@link TaskOutline#setFilter}; {@code null}, wenn nichts gefiltert ist.
    *
    * @param text liefert den angezeigten Zelltext eines Vorgangs für ein Feld
    */
   public Predicate<Task> predicate(BiFunction<Task, FieldType, String> text)
   {
      if (!isFiltered())
      {
         return null;
      }
      List<Filter> fileFilters = fileFiltersActive ? doc.getSavedFilters() : List.of();
      Map<FieldType, Set<String>> columns = new LinkedHashMap<>(columnFilters);
      return task ->
      {
         if (task.getNull())
         {
            return columns.isEmpty() && fileFilters.isEmpty();
         }
         for (Filter f : fileFilters)
         {
            try
            {
               if (!f.evaluate(task, null))
               {
                  return false;
               }
            }
            catch (RuntimeException ex)
            {
               // Filter mit Eingabeaufforderung o. Ä. – nicht anwendbar, Vorgang zeigen
            }
         }
         for (Map.Entry<FieldType, Set<String>> e : columns.entrySet())
         {
            String value = text.apply(task, e.getKey());
            if (!e.getValue().contains(value == null ? "" : value))
            {
               return false;
            }
         }
         return true;
      };
   }
}
