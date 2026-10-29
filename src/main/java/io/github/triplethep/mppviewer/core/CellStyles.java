package io.github.triplethep.mppviewer.core;

import io.github.triplethep.mppviewer.render.CellContent;
import org.mpxj.FieldType;
import org.mpxj.Task;
import org.mpxj.TaskField;
import org.mpxj.mpp.BackgroundPattern;
import org.mpxj.mpp.FontStyle;
import org.mpxj.mpp.GanttChartView;
import org.mpxj.mpp.TableFontStyle;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Textformatierungen aus der Projektdatei: Textarten (z. B. für Sammelvorgänge oder Meilensteine) sowie einzeln
 * formatierte Spalten, Zeilen und Zellen inklusive Hintergrundfarbe.
 */
public final class CellStyles
{
   /** Formatierung mit „nicht gesetzt“ = {@code null}. */
   private record Style(Boolean bold, Boolean italic, Boolean underline, Boolean strike, Color color, Color background)
   {
      static final Style EMPTY = new Style(null, null, null, null, null, null);

      Style over(Style top)
      {
         if (top == null)
         {
            return this;
         }
         return new Style(pick(top.bold, bold), pick(top.italic, italic), pick(top.underline, underline), pick(top.strike, strike), top.color != null ? top.color : color, top.background != null ? top.background : background);
      }

      private static Boolean pick(Boolean a, Boolean b)
      {
         return a != null ? a : b;
      }
   }

   private final Style summary;
   private final Style milestone;
   private final Style projectSummary;
   private final Style marked;
   private final Style external;
   private final Map<String, Style> cells = new HashMap<>();
   private final boolean empty;

   private CellStyles(GanttChartView view)
   {
      if (view == null)
      {
         summary = new Style(Boolean.TRUE, null, null, null, null, null);
         milestone = null;
         projectSummary = summary;
         marked = null;
         external = null;
         empty = true;
         return;
      }
      summary = category(safe(view::getSummaryTasksFontStyle), new Style(Boolean.TRUE, null, null, null, null, null));
      milestone = category(safe(view::getMilestoneTasksFontStyle), null);
      projectSummary = category(safe(view::getProjectSummaryTasksFontStyle), summary);
      marked = category(safe(view::getMarkedTasksFontStyle), null);
      external = category(safe(view::getExternalTasksFontStyle), null);

      TableFontStyle[] table = null;
      try
      {
         table = view.getTableFontStyles();
      }
      catch (RuntimeException ex)
      {
         // keine Zellformate lesbar
      }
      if (table != null)
      {
         for (TableFontStyle t : table)
         {
            Style s = cell(t);
            if (s != null)
            {
               cells.merge(key(t.getRowUniqueID(), t.getFieldType() == null ? null : normalize(t.getFieldType())), s, Style::over);
            }
         }
      }
      empty = cells.isEmpty();
   }

   public static CellStyles of(ProjectDocument doc)
   {
      return new CellStyles(doc.getGanttView());
   }

   private interface Getter
   {
      FontStyle get();
   }

   private static FontStyle safe(Getter g)
   {
      try
      {
         return g.get();
      }
      catch (RuntimeException ex)
      {
         return null;
      }
   }

   /**
    * Textart einer Kategorie. Schwarz/Weiß ohne Hintergrund entsprechen der Standardanzeige und werden ignoriert,
    * damit die Project-typische Darstellung erhalten bleibt.
    */
   private static Style category(FontStyle fs, Style fallback)
   {
      if (fs == null)
      {
         return fallback;
      }
      Color color = fs.getColor();
      if (color != null && isBlack(color))
      {
         color = null;
      }
      Color background = fs.getBackgroundColor();
      if (background != null && (isWhite(background) || isBlack(background) || fs.getBackgroundPattern() == BackgroundPattern.TRANSPARENT))
      {
         background = null;
      }
      return new Style(Boolean.valueOf(fs.getBold()), Boolean.valueOf(fs.getItalic()), Boolean.valueOf(fs.getUnderline()), Boolean.valueOf(fs.getStrikethrough()), color, background);
   }

   private static Style cell(TableFontStyle t)
   {
      Boolean bold = t.getBoldChanged() ? Boolean.valueOf(t.getBold()) : null;
      Boolean italic = t.getItalicChanged() ? Boolean.valueOf(t.getItalic()) : null;
      Boolean underline = t.getUnderlineChanged() ? Boolean.valueOf(t.getUnderline()) : null;
      Boolean strike = t.getStrikethroughChanged() ? Boolean.valueOf(t.getStrikethrough()) : null;
      Color color = t.getColorChanged() ? t.getColor() : null;
      Color background = null;
      if (t.getBackgroundColorChanged() && t.getBackgroundColor() != null)
      {
         BackgroundPattern pattern = t.getBackgroundPatternChanged() ? t.getBackgroundPattern() : BackgroundPattern.SOLID;
         if (pattern != BackgroundPattern.TRANSPARENT)
         {
            background = t.getBackgroundColor();
         }
      }
      if (bold == null && italic == null && underline == null && strike == null && color == null && background == null)
      {
         return null;
      }
      return new Style(bold, italic, underline, strike, color, background);
   }

   private static String key(int rowUid, FieldType field)
   {
      return rowUid + ":" + (field == null ? "*" : field.name());
   }

   private static boolean isBlack(Color c)
   {
      return c.getRed() == 0 && c.getGreen() == 0 && c.getBlue() == 0;
   }

   private static boolean isWhite(Color c)
   {
      return c.getRed() == 255 && c.getGreen() == 255 && c.getBlue() == 255;
   }

   /**
    * Wendet Textart und Zellformat auf den Zellinhalt an.
    *
    * @param projectSummary Zeile ist der Projektsammelvorgang
    */
   public CellContent apply(CellContent content, Task task, FieldType field, boolean isSummaryRow, boolean projectSummaryRow)
   {
      Style s = Style.EMPTY;
      if (task.getMilestone() && milestone != null)
      {
         s = s.over(milestone);
      }
      if (isSummaryRow)
      {
         s = s.over(projectSummaryRow ? projectSummary : summary);
      }
      if (external != null && task.getExternalTask())
      {
         s = s.over(external);
      }
      if (marked != null && safeMarked(task))
      {
         s = s.over(marked);
      }
      if (!empty && task.getUniqueID() != null)
      {
         FieldType f = normalize(field);
         s = s.over(cells.get(key(-1, f)));
         int uid = task.getUniqueID().intValue();
         s = s.over(cells.get(key(uid, null)));
         s = s.over(cells.get(key(uid, f)));
      }
      boolean bold = s.bold != null ? s.bold.booleanValue() : content.bold();
      boolean strike = content.strike() || (s.strike != null && s.strike.booleanValue());
      CellContent.Decoration deco = new CellContent.Decoration(s.color, s.background, s.italic != null && s.italic.booleanValue(), s.underline != null && s.underline.booleanValue());
      return content.styled(bold, strike, deco);
   }

   private static boolean safeMarked(Task task)
   {
      try
      {
         return task.getMarked();
      }
      catch (RuntimeException ex)
      {
         return false;
      }
   }

   /**
    * Zellformate stehen in der Datei teils an den Textvarianten (Dauer-Text …), teils an den Feldern selbst.
    */
   private static FieldType normalize(FieldType f)
   {
      if (f == TaskField.DURATION_TEXT)
      {
         return TaskField.DURATION;
      }
      if (f == TaskField.START_TEXT)
      {
         return TaskField.START;
      }
      if (f == TaskField.FINISH_TEXT)
      {
         return TaskField.FINISH;
      }
      return f;
   }
}
