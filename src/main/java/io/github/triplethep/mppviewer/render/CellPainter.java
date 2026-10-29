package io.github.triplethep.mppviewer.render;

import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Zeichnet Tabellenzellen und Spaltenköpfe. Wird vom Bildschirm-Renderer und vom Druck gemeinsam genutzt, damit
 * beides gleich aussieht.
 */
public final class CellPainter
{
   private static final int PAD = 4;
   private static final int TRIANGLE_SLOT = 12;

   private CellPainter()
   {
   }

   public static void paintCell(Graphics2D g, double x, double y, double w, double h, CellContent c, boolean selected, boolean gridRight, boolean gridBottom)
   {
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      Color bg;
      if (c.header())
      {
         bg = selected ? Style.ID_SELECTED : Style.ID_BG;
      }
      else
      {
         bg = selected ? Style.ROW_SELECTED : Color.WHITE;
      }
      CellContent.Decoration deco = c.style() == null ? CellContent.Decoration.NONE : c.style();
      if (deco.background() != null && !c.header())
      {
         // Hintergrundfarbe aus Project; markierte Zeilen leicht abgedunkelt wie in Project
         bg = selected ? deco.background().darker() : deco.background();
      }
      g.setColor(bg);
      g.fill(new Rectangle2D.Double(x, y, w, h));

      Shape oldClip = g.getClip();
      g.clip(new Rectangle2D.Double(x, y, w - 1, h));

      double cx = x + PAD;
      if (!c.icons().isEmpty())
      {
         double size = Math.min(14, h - 4);
         double total = c.icons().size() * (size + 2) - 2;
         double ix = c.align() == SwingConstants.CENTER ? x + (w - total) / 2.0 : cx;
         ix = Math.max(x + 2, ix);
         for (Indicator icon : c.icons())
         {
            icon.paint(g, ix, y + (h - size) / 2.0, size);
            ix += size + 2;
         }
      }

      if (c.indent() > 0 || c.outline() != CellContent.Outline.NONE)
      {
         cx += c.indent() * Style.INDENT;
      }
      if (c.outline() != CellContent.Outline.NONE)
      {
         paintTriangle(g, cx, y, h, c.outline() == CellContent.Outline.EXPANDED);
         cx += TRIANGLE_SLOT;
      }
      else if (c.indent() > 0)
      {
         // Teilvorgänge ohne eigene Teilvorgänge stehen bündig unter dem Text ihres Sammelvorgangs
         cx += TRIANGLE_SLOT;
      }

      String text = c.text();
      if (text != null && !text.isEmpty())
      {
         Font font = c.bold() ? Style.TABLE_FONT_BOLD : Style.TABLE_FONT;
         if (deco.italic())
         {
            font = font.deriveFont(font.getStyle() | Font.ITALIC);
         }
         g.setFont(font);
         FontMetrics fm = g.getFontMetrics();
         double available = x + w - PAD - cx;
         String shown = ellipsize(text, fm, available);
         int tw = fm.stringWidth(shown);
         double tx = switch (c.align())
         {
            case SwingConstants.RIGHT -> x + w - PAD - 1 - tw;
            case SwingConstants.CENTER -> x + (w - tw) / 2.0;
            default -> cx;
         };
         float ty = (float) (y + (h + fm.getAscent() - fm.getDescent()) / 2.0);
         Color fg = deco.foreground() != null ? deco.foreground() : Style.TEXT;
         g.setColor(c.muted() ? Style.TEXT_MUTED : fg);
         g.drawString(shown, (float) tx, ty);
         if (deco.underline())
         {
            double uy = ty + Math.max(1, fm.getDescent() / 2.0);
            g.draw(new Line2D.Double(tx, uy, tx + tw, uy));
         }
         if (c.strike())
         {
            double sy = ty - fm.getAscent() / 3.0;
            g.draw(new Line2D.Double(tx, sy, tx + tw, sy));
         }
      }

      g.setClip(oldClip);

      g.setColor(c.header() ? Style.HEADER_BORDER : Style.GRID);
      if (gridRight)
      {
         g.draw(new Line2D.Double(x + w - 0.5, y, x + w - 0.5, y + h));
      }
      if (gridBottom)
      {
         g.draw(new Line2D.Double(x, y + h - 0.5, x + w, y + h - 0.5));
      }
   }

   /**
    * Breite, die der Name-Text inklusive Einzug benötigt (für automatische Spaltenbreiten).
    */
   public static int indentWidth(CellContent c)
   {
      int w = PAD + c.indent() * Style.INDENT;
      if (c.outline() != CellContent.Outline.NONE || c.indent() > 0)
      {
         w += TRIANGLE_SLOT;
      }
      return w;
   }

   /**
    * Liefert true, wenn der Klick bei {@code clickX} (relativ zur Zelle) das Gliederungsdreieck trifft.
    */
   public static boolean hitsTriangle(CellContent c, double clickX)
   {
      if (c.outline() == CellContent.Outline.NONE)
      {
         return false;
      }
      double start = PAD + c.indent() * Style.INDENT - 2;
      return clickX >= start && clickX <= start + TRIANGLE_SLOT + 2;
   }

   private static void paintTriangle(Graphics2D g, double x, double y, double h, boolean expanded)
   {
      double cy = y + h / 2.0;
      Path2D p = new Path2D.Double();
      if (expanded)
      {
         // ◢ wie in Project (zeigt nach unten rechts)
         p.moveTo(x + 7.5, cy - 3.5);
         p.lineTo(x + 7.5, cy + 3.5);
         p.lineTo(x + 0.5, cy + 3.5);
         p.closePath();
         g.setColor(Style.OUTLINE_TRIANGLE);
         g.fill(p);
      }
      else
      {
         p.moveTo(x + 2, cy - 4.5);
         p.lineTo(x + 6.5, cy);
         p.lineTo(x + 2, cy + 4.5);
         p.closePath();
         g.setColor(Style.OUTLINE_TRIANGLE);
         g.draw(p);
      }
   }

   /**
    * Spaltenkopf: grau, zentriert, bei Bedarf zweizeilig.
    */
   public static void paintHeader(Graphics2D g, double x, double y, double w, double h, String title, Indicator icon)
   {
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(Style.HEADER_BG);
      g.fill(new Rectangle2D.Double(x, y, w, h));
      Shape oldClip = g.getClip();
      g.clip(new Rectangle2D.Double(x, y, w - 1, h));
      if (icon != null)
      {
         double size = 14;
         icon.paint(g, x + (w - size) / 2.0, y + (h - size) / 2.0, size);
      }
      else if (title != null && !title.isEmpty())
      {
         g.setFont(Style.HEADER_FONT);
         FontMetrics fm = g.getFontMetrics();
         g.setColor(Style.HEADER_TEXT);
         double avail = w - 4;
         String[] lines = wrap(title, fm, avail);
         double lineH = fm.getHeight();
         double total = lines.length * lineH;
         double ty = y + (h - total) / 2.0 + fm.getAscent();
         for (String line : lines)
         {
            String shown = ellipsize(line, fm, avail);
            double tx = x + (w - fm.stringWidth(shown)) / 2.0;
            g.drawString(shown, (float) tx, (float) ty);
            ty += lineH;
         }
      }
      g.setClip(oldClip);
      g.setColor(Style.HEADER_BORDER);
      g.draw(new Line2D.Double(x + w - 0.5, y, x + w - 0.5, y + h));
      g.draw(new Line2D.Double(x, y + h - 0.5, x + w, y + h - 0.5));
   }

   private static final char SOFT_HYPHEN = '\u00AD';

   private static String[] wrap(String text, FontMetrics fm, double width)
   {
      String plain = text.replace(String.valueOf(SOFT_HYPHEN), "");
      if (fm.stringWidth(plain) <= width)
      {
         return new String[]
         {
            plain
         };
      }
      // Weiches Trennzeichen (z. B. „Vorgangs\u00ADmodus“) erlaubt die Trennung langer Wörter wie in Project
      int soft = text.indexOf(SOFT_HYPHEN);
      if (soft > 0 && !text.substring(0, soft).contains(" "))
      {
         return new String[]
         {
            text.substring(0, soft) + "-",
            text.substring(soft + 1).replace(String.valueOf(SOFT_HYPHEN), "")
         };
      }
      text = plain;
      if (!text.contains(" "))
      {
         return new String[]
         {
            text
         };
      }
      int best = -1;
      for (int i = 0; i < text.length(); i++)
      {
         if (text.charAt(i) == ' ' && fm.stringWidth(text.substring(0, i)) <= width)
         {
            best = i;
         }
      }
      if (best <= 0)
      {
         best = text.indexOf(' ');
      }
      return new String[]
      {
         text.substring(0, best),
         text.substring(best + 1)
      };
   }

   public static String ellipsize(String text, FontMetrics fm, double width)
   {
      if (fm.stringWidth(text) <= width)
      {
         return text;
      }
      String dots = "…";
      int end = text.length();
      while (end > 0 && fm.stringWidth(text.substring(0, end) + dots) > width)
      {
         end--;
      }
      // Project zeigt bei zu schmalen Zahlen-/Datumsspalten „#####“ – Text wird abgeschnitten
      return end <= 0 ? "" : text.substring(0, end) + dots;
   }
}
