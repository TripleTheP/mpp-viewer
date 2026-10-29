package io.github.triplethep.mppviewer.render;

import io.github.triplethep.mppviewer.core.Formats;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;

/**
 * Zeichnet die zweistufige Zeitskala über dem Gantt-Diagramm.
 */
public final class TimescalePainter
{
   private TimescalePainter()
   {
   }

   /**
    * @param x0 linker Rand des sichtbaren Bereichs (Zeitleisten-Koordinaten)
    * @param x1 rechter Rand
    */
   public static void paint(Graphics2D g, Timeline timeline, Formats fmt, double x0, double x1)
   {
      ZoomLevel level = timeline.getLevel();
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(Style.TIMESCALE_BG);
      g.fill(new Rectangle2D.Double(x0, 0, x1 - x0, Style.HEADER_HEIGHT));

      paintTier(g, timeline, fmt, level.top(), 0, x0, x1, false);
      paintTier(g, timeline, fmt, level.bottom(), Style.TIER_HEIGHT, x0, x1, true);

      g.setColor(Style.TIMESCALE_LINE);
      g.draw(new Line2D.Double(x0, Style.TIER_HEIGHT - 0.5, x1, Style.TIER_HEIGHT - 0.5));
      g.setColor(Style.HEADER_BORDER);
      g.draw(new Line2D.Double(x0, Style.HEADER_HEIGHT - 0.5, x1, Style.HEADER_HEIGHT - 0.5));
   }

   private static void paintTier(Graphics2D g, Timeline timeline, Formats fmt, ZoomLevel.Tier tier, int y, double x0, double x1, boolean centered)
   {
      g.setFont(Style.TIMESCALE_FONT);
      FontMetrics fm = g.getFontMetrics();
      TimeUnitScale unit = tier.unit();
      LocalDateTime t = unit.floor(timeline.time(x0), timeline.getWeekStart());
      int guard = 0;
      while (guard++ < 100_000)
      {
         LocalDateTime next = unit.next(t);
         double xs = timeline.x(t);
         double xe = timeline.x(next);
         if (xs > x1)
         {
            break;
         }
         if (xe >= x0)
         {
            // Trennlinie am Beginn jeder Einheit
            g.setColor(Style.TIMESCALE_LINE);
            g.draw(new Line2D.Double(xs - 0.5, y, xs - 0.5, y + Style.TIER_HEIGHT));

            String label = pickLabel(tier, t, fmt, fm, xe - xs - 6);
            if (label != null)
            {
               g.setColor(Style.HEADER_TEXT);
               int textWidth = fm.stringWidth(label);
               double tx = centered ? xs + (xe - xs - textWidth) / 2.0 : xs + 4;
               float ty = y + (Style.TIER_HEIGHT + fm.getAscent() - fm.getDescent()) / 2.0f;
               Shape oldClip = g.getClip();
               g.clip(new Rectangle2D.Double(xs, y, xe - xs, Style.TIER_HEIGHT));
               g.drawString(label, (float) tx, ty);
               g.setClip(oldClip);
            }
         }
         t = next;
      }
   }

   private static String pickLabel(ZoomLevel.Tier tier, LocalDateTime t, Formats fmt, FontMetrics fm, double available)
   {
      for (ZoomLevel.Label label : tier.labels())
      {
         String text = label.format(t, fmt);
         if (fm.stringWidth(text) <= available)
         {
            return text;
         }
      }
      return null;
   }
}
