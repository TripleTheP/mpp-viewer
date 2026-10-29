package io.github.triplethep.mppviewer.render;

import io.github.triplethep.mppviewer.i18n.I18n;
import org.mpxj.Task;
import org.mpxj.TaskMode;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Legende der Balkenarten für die Fußzeile beim Drucken – wie in Project.
 */
public final class LegendPainter
{
   public enum Item
   {
      TASK,
      PROGRESS,
      CRITICAL,
      CRITICAL_PROGRESS,
      MANUAL,
      MILESTONE,
      SUMMARY,
      PROJECT_SUMMARY,
      BASELINE,
      DEADLINE
   }

   private LegendPainter()
   {
   }

   public static List<Item> itemsFor(ViewSnapshot view)
   {
      List<Item> items = new ArrayList<>();
      GanttOptions o = view.options();
      boolean manual = false;
      boolean deadline = false;
      boolean progress = false;
      boolean milestone = false;
      boolean summary = false;
      for (Task t : view.doc().getTasks())
      {
         manual |= t.getTaskMode() == TaskMode.MANUALLY_SCHEDULED && !t.getSummary();
         deadline |= t.getDeadline() != null;
         progress |= t.getPercentageComplete() != null && t.getPercentageComplete().doubleValue() > 0;
         milestone |= t.getMilestone();
         summary |= t.getSummary();
      }
      if (o.showCritical())
      {
         items.add(Item.CRITICAL);
         if (progress)
         {
            items.add(Item.CRITICAL_PROGRESS);
         }
      }
      items.add(Item.TASK);
      if (progress)
      {
         items.add(Item.PROGRESS);
      }
      if (manual)
      {
         items.add(Item.MANUAL);
      }
      if (o.showBaseline())
      {
         items.add(Item.BASELINE);
      }
      if (milestone)
      {
         items.add(Item.MILESTONE);
      }
      if (summary)
      {
         items.add(Item.SUMMARY);
      }
      if (view.outline() != null && view.outline().isShowProjectSummary())
      {
         items.add(Item.PROJECT_SUMMARY);
      }
      if (deadline)
      {
         items.add(Item.DEADLINE);
      }
      return items;
   }

   /**
    * Zeichnet die Legende in das Rechteck (x, y, w, h).
    */
   public static void paint(Graphics2D g, double x, double y, double w, double h, List<Item> items, boolean tracking)
   {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      Font font = Style.BAR_FONT.deriveFont(8.5f);
      g.setFont(font);
      FontMetrics fm = g.getFontMetrics();
      double sampleW = 26;
      double itemW = 0;
      for (Item item : items)
      {
         itemW = Math.max(itemW, sampleW + 6 + fm.stringWidth(label(item)) + 12);
      }
      int rows = Math.max(1, (int) Math.floor(h / 13));
      int cols = Math.max(1, (int) Math.floor(w / Math.max(1, itemW)));
      int needed = (int) Math.ceil(items.size() / (double) rows);
      cols = Math.min(cols, Math.max(1, needed));
      int perCol = (int) Math.ceil(items.size() / (double) cols);
      for (int i = 0; i < items.size(); i++)
      {
         int c = i / perCol;
         int r = i % perCol;
         double ix = x + c * itemW;
         double iy = y + r * 13;
         if (ix + sampleW > x + w)
         {
            break;
         }
         sample(g, items.get(i), ix, iy + 1.5, sampleW, 10, tracking);
         g.setColor(Style.TEXT);
         g.drawString(label(items.get(i)), (float) (ix + sampleW + 6), (float) (iy + 1.5 + (10 + fm.getAscent() - fm.getDescent()) / 2.0));
      }
   }

   public static String label(Item item)
   {
      return I18n.t("legend." + item.name());
   }

   private static void sample(Graphics2D g, Item item, double x, double y, double w, double h, boolean tracking)
   {
      switch (item)
      {
         case TASK -> bar(g, x, y + 1, w, h - 2, Style.TASK_FILL, Style.TASK_BORDER);
         case CRITICAL -> bar(g, x, y + 1, w, h - 2, Style.CRITICAL_FILL, Style.CRITICAL_BORDER);
         case MANUAL -> bar(g, x, y + 1, w, h - 2, Style.MANUAL_FILL, Style.MANUAL_BORDER);
         case PROGRESS, CRITICAL_PROGRESS ->
         {
            Color c = item == Item.PROGRESS ? Style.TASK_PROGRESS : Style.CRITICAL_PROGRESS;
            g.setColor(c);
            if (tracking)
            {
               g.fill(new Rectangle2D.Double(x, y + 1, w, h - 2));
            }
            else
            {
               g.fill(new Rectangle2D.Double(x, y + h * 0.35, w, h * 0.3));
            }
         }
         case BASELINE ->
         {
            g.setColor(Style.BASELINE);
            g.fill(new Rectangle2D.Double(x, y + h / 2 - 2, w, 4));
         }
         case MILESTONE ->
         {
            double cx = x + w / 2;
            double cy = y + h / 2;
            Path2D d = new Path2D.Double();
            d.moveTo(cx, cy - 5);
            d.lineTo(cx + 5, cy);
            d.lineTo(cx, cy + 5);
            d.lineTo(cx - 5, cy);
            d.closePath();
            g.setColor(Style.MILESTONE);
            g.fill(d);
         }
         case SUMMARY, PROJECT_SUMMARY ->
         {
            GeneralPath p = new GeneralPath();
            double top = y + 1;
            double bottom = y + 5;
            p.moveTo(x, top);
            p.lineTo(x + w, top);
            p.lineTo(x + w, bottom + 4);
            p.lineTo(x + w - 4, bottom);
            p.lineTo(x + 4, bottom);
            p.lineTo(x, bottom + 4);
            p.closePath();
            g.setColor(item == Item.SUMMARY ? Style.SUMMARY : Style.PROJECT_SUMMARY);
            g.fill(p);
         }
         case DEADLINE ->
         {
            double cx = x + w / 2;
            Path2D a = new Path2D.Double();
            a.moveTo(cx - 1.5, y);
            a.lineTo(cx + 1.5, y);
            a.lineTo(cx + 1.5, y + 5);
            a.lineTo(cx + 4.5, y + 5);
            a.lineTo(cx, y + 10);
            a.lineTo(cx - 4.5, y + 5);
            a.lineTo(cx - 1.5, y + 5);
            a.closePath();
            g.setColor(Style.DEADLINE);
            g.fill(a);
         }
         default -> throw new IllegalArgumentException("Unbekanntes Legendenelement: " + item);
      }
   }

   private static void bar(Graphics2D g, double x, double y, double w, double h, Color fill, Color border)
   {
      g.setColor(fill);
      g.fill(new Rectangle2D.Double(x, y, w, h));
      g.setColor(border);
      g.draw(new Rectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1));
   }
}
