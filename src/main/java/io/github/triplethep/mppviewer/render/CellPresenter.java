package io.github.triplethep.mppviewer.render;

import io.github.triplethep.mppviewer.core.CellStyles;
import io.github.triplethep.mppviewer.core.ColumnDef;
import io.github.triplethep.mppviewer.core.Formats;
import io.github.triplethep.mppviewer.core.TaskRow;
import io.github.triplethep.mppviewer.i18n.I18n;
import org.mpxj.ConstraintType;
import org.mpxj.DataType;
import org.mpxj.Duration;
import org.mpxj.FieldType;
import org.mpxj.ProjectCalendar;
import org.mpxj.Resource;
import org.mpxj.ResourceField;
import org.mpxj.ResourceType;
import org.mpxj.Task;
import org.mpxj.TaskField;
import org.mpxj.TaskMode;

import javax.swing.SwingConstants;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ermittelt Text und Darstellung jeder Zelle wie in Microsoft Project.
 */
public final class CellPresenter
{
   private final Formats fmt;
   private final ProjectCalendar defaultCalendar;
   private final CellStyles styles;

   public CellPresenter(Formats fmt)
   {
      this(fmt, null, null);
   }

   /**
    * @param defaultCalendar Standardkalender des Projekts (Basiskalender von Ressourcen ohne eigenen Kalender)
    * @param styles          Textarten und Zellformate aus der Datei, darf {@code null} sein
    */
   public CellPresenter(Formats fmt, ProjectCalendar defaultCalendar, CellStyles styles)
   {
      this.fmt = fmt;
      this.defaultCalendar = defaultCalendar;
      this.styles = styles;
   }

   public Formats formats()
   {
      return fmt;
   }

   // ------------------------------------------------------------------ Vorgänge

   public CellContent task(TaskRow row, ColumnDef col)
   {
      Task task = row.task();
      boolean summary = row.hasChildren() || row.isProjectSummary();
      boolean inactive = !task.getActive();
      boolean blank = task.getNull();

      switch (col.getKind())
      {
         case ID:
            return new CellContent(task.getID() == null ? "" : task.getID().toString(), SwingConstants.RIGHT, false, false, false, 0, CellContent.Outline.NONE, List.of(), true);
         case INDICATORS:
            return icons(blank ? List.of() : taskIndicators(task));
         case TASK_MODE:
            if (blank)
            {
               return icons(List.of());
            }
            return icons(List.of(task.getTaskMode() == TaskMode.MANUALLY_SCHEDULED ? Indicator.MODE_MANUAL : Indicator.MODE_AUTO));
         case NAME:
         {
            CellContent.Outline outline = row.hasChildren() ? (row.expanded() ? CellContent.Outline.EXPANDED : CellContent.Outline.COLLAPSED) : CellContent.Outline.NONE;
            String name = task.getName() == null ? "" : task.getName();
            return style(new CellContent(name, SwingConstants.LEFT, summary, inactive, inactive, Math.max(0, row.level() - 1), outline, List.of(), false), row, col);
         }
         default:
            break;
      }

      if (blank)
      {
         return CellContent.plain("", col.getAlign());
      }
      String text = taskText(task, col.getField());
      return style(new CellContent(text, col.getAlign(), summary, inactive, inactive, 0, CellContent.Outline.NONE, List.of(), false), row, col);
   }

   private CellContent style(CellContent content, TaskRow row, ColumnDef col)
   {
      if (styles == null)
      {
         return content;
      }
      return styles.apply(content, row.task(), col.getField(), row.hasChildren() || row.isProjectSummary(), row.isProjectSummary());
   }

   private static CellContent icons(List<Indicator> icons)
   {
      return new CellContent("", SwingConstants.CENTER, false, false, false, 0, CellContent.Outline.NONE, icons, false);
   }

   public List<Indicator> taskIndicators(Task task)
   {
      List<Indicator> list = new ArrayList<>();
      if (!task.getActive())
      {
         list.add(Indicator.INACTIVE);
      }
      Number pct = task.getPercentageComplete();
      if (pct != null && pct.doubleValue() >= 100.0 && !task.getSummary())
      {
         list.add(Indicator.DONE);
      }
      ConstraintType ct = task.getConstraintType();
      if (ct != null && ct != ConstraintType.AS_SOON_AS_POSSIBLE && ct != ConstraintType.AS_LATE_AS_POSSIBLE && !task.getSummary())
      {
         boolean hard = ct == ConstraintType.MUST_START_ON || ct == ConstraintType.MUST_FINISH_ON;
         list.add(hard ? Indicator.CONSTRAINT_HARD : Indicator.CONSTRAINT);
      }
      LocalDateTime deadline = task.getDeadline();
      if (deadline != null && task.getFinish() != null && task.getFinish().isAfter(deadline))
      {
         list.add(Indicator.DEADLINE_MISSED);
      }
      String notes = task.getNotes();
      if (notes != null && !notes.isBlank())
      {
         list.add(Indicator.NOTE);
      }
      String link = task.getHyperlink();
      if (link != null && !link.isBlank())
      {
         list.add(Indicator.HYPERLINK);
      }
      return list;
   }

   /**
    * Text eines beliebigen Vorgangsfelds.
    */
   public String taskText(Task task, FieldType field)
   {
      if (field instanceof TaskField tf)
      {
         switch (tf)
         {
            case DURATION:
            {
               String manual = manualText(task, TaskField.DURATION_TEXT);
               if (manual != null && task.getDuration() == null)
               {
                  return manual;
               }
               return fmt.duration(task.getDuration(), task.getEstimated());
            }
            case START:
            case FINISH:
            {
               Object v = task.get(tf);
               if (v == null && task.getTaskMode() == TaskMode.MANUALLY_SCHEDULED)
               {
                  String manual = manualText(task, tf == TaskField.START ? TaskField.START_TEXT : TaskField.FINISH_TEXT);
                  return manual == null ? "" : manual;
               }
               return v instanceof LocalDateTime d ? fmt.date(d) : fmt.na();
            }
            case PREDECESSORS:
               return fmt.predecessors(task);
            case SUCCESSORS:
               return fmt.successors(task);
            case RESOURCE_NAMES:
               return fmt.resourceNames(task);
            case NOTES:
               return firstLine(task.getNotes());
            case TASK_MODE:
               return fmt.taskMode(task.getTaskMode());
            case NAME:
               return task.getName() == null ? "" : task.getName();
            case INDICATORS:
               return indicatorText(taskIndicators(task));
            default:
               break;
         }
      }
      Object value;
      try
      {
         value = task.get(field);
      }
      catch (RuntimeException ex)
      {
         value = null;
      }
      return format(field, value);
   }

   /**
    * Wert für den Excel-Export: Datum und Zahlen typisiert, alles andere als Text.
    */
   public Object taskExportValue(TaskRow row, ColumnDef col)
   {
      Task task = row.task();
      if (task.getNull())
      {
         return col.getKind() == ColumnDef.Kind.ID ? task.getID() : null;
      }
      switch (col.getKind())
      {
         case ID:
            return task.getID();
         case NAME:
            return task.getName();
         case INDICATORS:
            return indicatorText(taskIndicators(task));
         case TASK_MODE:
            return fmt.taskMode(task.getTaskMode());
         default:
            break;
      }
      return exportValue(col.getField(), () -> task.get(col.getField()), taskText(task, col.getField()));
   }

   // ------------------------------------------------------------------ Ressourcen

   public CellContent resource(Resource resource, ColumnDef col)
   {
      switch (col.getKind())
      {
         case ID:
            return new CellContent(resource.getID() == null ? "" : resource.getID().toString(), SwingConstants.RIGHT, false, false, false, 0, CellContent.Outline.NONE, List.of(), true);
         case INDICATORS:
         {
            List<Indicator> list = new ArrayList<>();
            if (resource.getNotes() != null && !resource.getNotes().isBlank())
            {
               list.add(Indicator.NOTE);
            }
            return icons(list);
         }
         default:
            break;
      }
      if (resource.getNull())
      {
         return CellContent.plain("", col.getAlign());
      }
      boolean inactive = !resource.getActive();
      return new CellContent(resourceText(resource, col.getField()), col.getAlign(), false, inactive, inactive, 0, CellContent.Outline.NONE, List.of(), false);
   }

   public String resourceText(Resource resource, FieldType field)
   {
      if (field instanceof ResourceField rf)
      {
         switch (rf)
         {
            case NAME:
               return resource.getName() == null ? "" : resource.getName();
            case MAX_UNITS:
            {
               if (resource.getType() != null && resource.getType() != ResourceType.WORK)
               {
                  return "";
               }
               Number units = resource.getMaxUnits();
               return units == null ? "" : fmt.percent(units);
            }
            case STANDARD_RATE:
               if (resource.getType() == ResourceType.COST)
               {
                  return "";
               }
               if (resource.getType() == ResourceType.MATERIAL)
               {
                  return fmt.currency(resource.getStandardRate() == null ? 0 : resource.getStandardRate().getAmount());
               }
               return fmt.rate(resource.getStandardRate());
            case OVERTIME_RATE:
               if (resource.getType() != null && resource.getType() != ResourceType.WORK)
               {
                  return "";
               }
               return fmt.rate(resource.getOvertimeRate());
            case COST_PER_USE:
               if (resource.getType() == ResourceType.COST)
               {
                  return "";
               }
               return fmt.currency(resource.getCostPerUse() == null ? Integer.valueOf(0) : resource.getCostPerUse());
            case ACCRUE_AT:
               return fmt.accrue(resource.getAccrueAt());
            case TYPE:
               return fmt.resourceType(resource.getType());
            case BASE_CALENDAR:
            {
               if (resource.getType() != null && resource.getType() != ResourceType.WORK)
               {
                  return "";
               }
               ProjectCalendar cal = resource.getCalendar();
               if (cal != null && cal.getParent() != null)
               {
                  cal = cal.getParent();
               }
               if (cal == null)
               {
                  cal = defaultCalendar;
               }
               return cal == null || cal.getName() == null ? "" : cal.getName();
            }
            case NOTES:
               return firstLine(resource.getNotes());
            default:
               break;
         }
      }
      Object value;
      try
      {
         value = resource.get(field);
      }
      catch (RuntimeException ex)
      {
         value = null;
      }
      return format(field, value);
   }

   public Object resourceExportValue(Resource resource, ColumnDef col)
   {
      switch (col.getKind())
      {
         case ID:
            return resource.getID();
         case NAME:
            return resource.getName();
         case INDICATORS:
            return resource.getNotes() != null && !resource.getNotes().isBlank() ? I18n.t(Indicator.NOTE.key()) : "";
         default:
            break;
      }
      return exportValue(col.getField(), () -> resource.get(col.getField()), resourceText(resource, col.getField()));
   }

   // ------------------------------------------------------------------ Hilfsfunktionen

   private String format(FieldType field, Object value)
   {
      DataType type = field == null ? null : field.getDataType();
      if (value == null)
      {
         return type == DataType.DATE ? fmt.na() : "";
      }
      if (type != null)
      {
         switch (type)
         {
            case CURRENCY:
               if (value instanceof Number n)
               {
                  return fmt.currency(n);
               }
               break;
            case PERCENTAGE:
               if (value instanceof Number n)
               {
                  return fmt.percent(n);
               }
               break;
            case UNITS:
               if (value instanceof Number n)
               {
                  return fmt.percent(n);
               }
               break;
            case WORK:
               if (value instanceof Duration d)
               {
                  return fmt.work(d);
               }
               break;
            case NOTES:
               return firstLine(value.toString());
            default:
               break;
         }
      }
      return fmt.value(value);
   }

   private Object exportValue(FieldType field, java.util.function.Supplier<Object> raw, String text)
   {
      DataType type = field == null ? null : field.getDataType();
      Object value;
      try
      {
         value = raw.get();
      }
      catch (RuntimeException ex)
      {
         value = null;
      }
      if (value == null)
      {
         return text == null || text.isEmpty() ? null : text;
      }
      if (type == DataType.DATE && value instanceof LocalDateTime)
      {
         return value;
      }
      if (type == DataType.PERCENTAGE || type == DataType.UNITS)
      {
         if (value instanceof Number n)
         {
            return new ExportPercent(n.doubleValue() / 100.0);
         }
      }
      if (type == DataType.CURRENCY && value instanceof Number n)
      {
         return new ExportCurrency(n.doubleValue());
      }
      if ((type == DataType.NUMERIC || type == DataType.INTEGER || type == DataType.SHORT) && value instanceof Number)
      {
         return value;
      }
      return text;
   }

   /**
    * Freitext manuell geplanter Vorgänge (z. B. „TBD“ oder „nach Abstimmung“).
    */
   private static String manualText(Task task, TaskField field)
   {
      if (task.getTaskMode() != TaskMode.MANUALLY_SCHEDULED)
      {
         return null;
      }
      try
      {
         Object v = task.get(field);
         return v == null || v.toString().isBlank() ? null : v.toString();
      }
      catch (RuntimeException ex)
      {
         return null;
      }
   }

   public static String indicatorText(List<Indicator> icons)
   {
      StringBuilder sb = new StringBuilder();
      for (Indicator i : icons)
      {
         if (!sb.isEmpty())
         {
            sb.append(", ");
         }
         sb.append(I18n.t(i.key()));
      }
      return sb.toString();
   }

   private static String firstLine(String text)
   {
      if (text == null)
      {
         return "";
      }
      String t = text.strip();
      int nl = t.indexOf('\n');
      return nl < 0 ? t : t.substring(0, nl).strip() + " …";
   }

   /**
    * Prozentwert für den Excel-Export (0..1).
    */
   public record ExportPercent(double value)
   {
   }

   /**
    * Währungsbetrag für den Excel-Export.
    */
   public record ExportCurrency(double value)
   {
   }
}
