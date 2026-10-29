package io.github.triplethep.mppviewer.core;

import io.github.triplethep.mppviewer.i18n.I18n;
import org.mpxj.Column;
import org.mpxj.DataType;
import org.mpxj.FieldType;
import org.mpxj.ResourceField;
import org.mpxj.Table;
import org.mpxj.TaskField;

import javax.swing.SwingConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Baut die Spalten einer Ansicht – bevorzugt aus der in der .mpp-Datei gespeicherten Tabelle, damit der Viewer
 * dieselben Spalten in derselben Reihenfolge und Breite zeigt wie Microsoft Project beim Kollegen.
 */
public final class ColumnSets
{
   /** Pixel pro Zeichen der in Project gespeicherten Spaltenbreite. */
   private static final double PX_PER_CHAR = 7.0;

   private ColumnSets()
   {
   }

   /**
    * Spalten der Standardtabelle „Eingabe“ in Project 2013 und neuer.
    */
   public static List<ColumnDef> defaultTaskColumns()
   {
      List<ColumnDef> list = new ArrayList<>();
      list.add(taskColumn(TaskField.ID, null, 0, -1));
      list.add(taskColumn(TaskField.INDICATORS, null, 0, -1));
      list.add(taskColumn(TaskField.TASK_MODE, null, 0, -1));
      list.add(taskColumn(TaskField.NAME, null, 0, -1));
      list.add(taskColumn(TaskField.DURATION, null, 0, -1));
      list.add(taskColumn(TaskField.START, null, 0, -1));
      list.add(taskColumn(TaskField.FINISH, null, 0, -1));
      list.add(taskColumn(TaskField.PREDECESSORS, null, 0, -1));
      list.add(taskColumn(TaskField.RESOURCE_NAMES, null, 0, -1));
      return list;
   }

   /**
    * Spalten der Standardtabelle „Ressource: Tabelle“.
    */
   public static List<ColumnDef> defaultResourceColumns()
   {
      List<ColumnDef> list = new ArrayList<>();
      list.add(resourceColumn(ResourceField.ID, null, 0, -1));
      list.add(resourceColumn(ResourceField.INDICATORS, null, 0, -1));
      list.add(resourceColumn(ResourceField.NAME, null, 0, -1));
      list.add(resourceColumn(ResourceField.TYPE, null, 0, -1));
      list.add(resourceColumn(ResourceField.MATERIAL_LABEL, null, 0, -1));
      list.add(resourceColumn(ResourceField.INITIALS, null, 0, -1));
      list.add(resourceColumn(ResourceField.GROUP, null, 0, -1));
      list.add(resourceColumn(ResourceField.MAX_UNITS, null, 0, -1));
      list.add(resourceColumn(ResourceField.STANDARD_RATE, null, 0, -1));
      list.add(resourceColumn(ResourceField.OVERTIME_RATE, null, 0, -1));
      list.add(resourceColumn(ResourceField.COST_PER_USE, null, 0, -1));
      list.add(resourceColumn(ResourceField.ACCRUE_AT, null, 0, -1));
      list.add(resourceColumn(ResourceField.BASE_CALENDAR, null, 0, -1));
      list.add(resourceColumn(ResourceField.CODE, null, 0, -1));
      return list;
   }

   /**
    * Spalten aus einer Tabelle der Projektdatei; {@code null} oder leere Tabelle ergibt die Standardspalten.
    */
   public static List<ColumnDef> fromTable(Table table, boolean resource)
   {
      if (table == null || table.getColumns().isEmpty())
      {
         return resource ? defaultResourceColumns() : defaultTaskColumns();
      }
      List<ColumnDef> list = new ArrayList<>();
      for (Column column : table.getColumns())
      {
         FieldType field = column.getFieldType();
         if (field == null)
         {
            continue;
         }
         String customTitle = customTitle(column, field);
         int width = column.getWidth() > 0 ? (int) Math.round(column.getWidth() * PX_PER_CHAR) : 0;
         int align = align(column.getAlignData());
         if (resource && field instanceof ResourceField rf)
         {
            list.add(resourceColumn(rf, customTitle, width, align));
         }
         else if (!resource && field instanceof TaskField tf)
         {
            list.add(taskColumn(tf, customTitle, width, align));
         }
      }
      if (list.isEmpty())
      {
         return resource ? defaultResourceColumns() : defaultTaskColumns();
      }
      // Project zeigt die Nr.-Spalte immer ganz links, auch wenn sie nicht in der Tabelle steht
      if (list.get(0).getKind() != ColumnDef.Kind.ID)
      {
         list.removeIf(c -> c.getKind() == ColumnDef.Kind.ID);
         list.add(0, resource ? resourceColumn(ResourceField.ID, null, 0, -1) : taskColumn(TaskField.ID, null, 0, -1));
      }
      return list;
   }

   private static String customTitle(Column column, FieldType field)
   {
      try
      {
         String title = column.getTitle(Locale.ENGLISH);
         String standard = field.getName(Locale.ENGLISH);
         if (title != null && !title.isBlank() && !title.equals(standard))
         {
            return title;
         }
      }
      catch (RuntimeException ex)
      {
         // Titel nicht ermittelbar: Standardtitel verwenden
      }
      return null;
   }

   private static int align(int mpxjAlign)
   {
      return switch (mpxjAlign)
      {
         case Column.ALIGN_LEFT -> SwingConstants.LEFT;
         case Column.ALIGN_CENTER -> SwingConstants.CENTER;
         case Column.ALIGN_RIGHT -> SwingConstants.RIGHT;
         default -> -1;
      };
   }

   public static ColumnDef taskColumn(TaskField field, String customTitle, int width, int align)
   {
      // Project speichert Dauer/Anfang/Ende seit 2010 als Textvarianten (für manuell geplante Vorgänge wie „TBD“)
      field = switch (field)
      {
         case DURATION_TEXT -> TaskField.DURATION;
         case START_TEXT -> TaskField.START;
         case FINISH_TEXT -> TaskField.FINISH;
         default -> field;
      };
      ColumnDef.Kind kind = switch (field)
      {
         case ID -> ColumnDef.Kind.ID;
         case INDICATORS -> ColumnDef.Kind.INDICATORS;
         case TASK_MODE -> ColumnDef.Kind.TASK_MODE;
         case NAME -> ColumnDef.Kind.NAME;
         default -> ColumnDef.Kind.FIELD;
      };
      String title = customTitle != null ? customTitle : fieldTitle(field);
      int w = width > 0 ? width : defaultWidth(kind, field);
      int a = align >= 0 ? align : defaultAlign(kind, field);
      if (kind == ColumnDef.Kind.ID || kind == ColumnDef.Kind.INDICATORS || kind == ColumnDef.Kind.TASK_MODE)
      {
         a = kind == ColumnDef.Kind.ID ? SwingConstants.RIGHT : SwingConstants.CENTER;
      }
      if (kind == ColumnDef.Kind.ID)
      {
         w = Math.max(w, 34);
      }
      return new ColumnDef(kind, field, title, w, a);
   }

   public static ColumnDef resourceColumn(ResourceField field, String customTitle, int width, int align)
   {
      ColumnDef.Kind kind = switch (field)
      {
         case ID -> ColumnDef.Kind.ID;
         case INDICATORS -> ColumnDef.Kind.INDICATORS;
         case NAME -> ColumnDef.Kind.NAME;
         default -> ColumnDef.Kind.FIELD;
      };
      String title = customTitle != null ? customTitle : fieldTitle(field);
      int w = width > 0 ? width : defaultWidth(kind, field);
      int a = align >= 0 ? align : defaultAlign(kind, field);
      if (kind == ColumnDef.Kind.ID)
      {
         a = SwingConstants.RIGHT;
         w = Math.max(w, 34);
      }
      return new ColumnDef(kind, field, title, w, a);
   }

   /**
    * Lokalisierter Spaltentitel; unbekannte Felder erhalten den englischen MPXJ-Namen.
    */
   public static String fieldTitle(FieldType field)
   {
      String prefix = field instanceof ResourceField ? "field.resource." : "field.task.";
      String text = I18n.opt(prefix + field.name());
      if (text != null)
      {
         return text;
      }
      String custom = customFieldTitle(field.name());
      if (custom != null)
      {
         return custom;
      }
      try
      {
         String name = field.getName(Locale.ENGLISH);
         return name == null ? field.name() : name;
      }
      catch (RuntimeException ex)
      {
         return field.name();
      }
   }

   /**
    * Benutzerdefinierte Felder wie TEXT1, NUMBER3, FLAG10 lokalisieren („Text1“, „Zahl3“, „Attribut10“).
    */
   private static String customFieldTitle(String name)
   {
      int i = name.length();
      while (i > 0 && Character.isDigit(name.charAt(i - 1)))
      {
         i--;
      }
      if (i == name.length() || i == 0)
      {
         return null;
      }
      String base = name.substring(0, i);
      String number = name.substring(i);
      String text = I18n.opt("field.custom." + base);
      return text == null ? null : text + number;
   }

   private static int defaultWidth(ColumnDef.Kind kind, FieldType field)
   {
      switch (kind)
      {
         case ID:
            return 38;
         case INDICATORS:
            return 42;
         case TASK_MODE:
            return 62;
         case NAME:
            return field instanceof ResourceField ? 160 : 260;
         default:
            break;
      }
      DataType type = field.getDataType();
      if (type == null)
      {
         return 100;
      }
      return switch (type)
      {
         case DATE -> 100;
         case DURATION, WORK -> 80;
         case CURRENCY, RATE -> 95;
         case PERCENTAGE, UNITS, NUMERIC, INTEGER, SHORT -> 70;
         case BOOLEAN -> 60;
         case RELATION_LIST -> 100;
         default -> 120;
      };
   }

   private static int defaultAlign(ColumnDef.Kind kind, FieldType field)
   {
      if (kind == ColumnDef.Kind.NAME)
      {
         return SwingConstants.LEFT;
      }
      DataType type = field.getDataType();
      if (type == null)
      {
         return SwingConstants.LEFT;
      }
      return switch (type)
      {
         case CURRENCY, RATE, PERCENTAGE, UNITS, NUMERIC, INTEGER, SHORT, WORK -> SwingConstants.RIGHT;
         default -> SwingConstants.LEFT;
      };
   }
}
