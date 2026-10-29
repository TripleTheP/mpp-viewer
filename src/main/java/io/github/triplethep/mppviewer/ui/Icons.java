package io.github.triplethep.mppviewer.ui;

import io.github.triplethep.mppviewer.render.Style;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Eigene Vektorsymbole für Menüband, Backstage und Statusleiste – gestochen scharf bei jeder Bildschirmskalierung.
 * Gezeichnet in einem 32×32-Raster.
 */
public final class Icons
{
   public enum Name
   {
      APP,
      OPEN,
      RECENT,
      PRINT,
      PDF,
      XLSX,
      PNG,
      CSV,
      GANTT,
      TRACKING,
      TASK_SHEET,
      RESOURCE_SHEET,
      ZOOM_IN,
      ZOOM_OUT,
      ZOOM_ENTIRE,
      SCROLL_TO_TASK,
      TASK_INFO,
      NOTES,
      PROJECT_INFO,
      FIND,
      OUTLINE,
      COPY,
      LANGUAGE,
      CLOSE,
      BACK,
      LOCK,
      TODAY,
      TIMESCALE,
      CRITICAL,
      SETTINGS,
      EXPORT,
      TABLE,
      EXPAND,
      COLLAPSE,
      PREV,
      NEXT,
      SORT,
      FILTER
   }

   private static final Color INK = new Color(0x444444);
   private static final Color PAPER = Color.WHITE;
   private static final Color PAPER_EDGE = new Color(0x8A8A8A);
   private static final Color BLUE = new Color(0x2F6DB5);
   private static final Color YELLOW = new Color(0xF2C14E);
   private static final Color YELLOW_DARK = new Color(0xC8932A);
   private static final Color RED = new Color(0xC43E1C);
   private static final Color GREEN = new Color(0x217346);

   private Icons()
   {
   }

   public static Icon get(Name name, int size)
   {
      return new VectorIcon(name, size, null);
   }

   /**
    * Einfarbige Variante (z. B. weiß auf grünem Grund in der Backstage-Navigation).
    */
   public static Icon mono(Name name, int size, Color color)
   {
      return new VectorIcon(name, size, color);
   }

   public static List<Image> appImages()
   {
      List<Image> images = new ArrayList<>();
      for (int s : new int[]
      {
         16,
         20,
         24,
         32,
         40,
         48,
         64,
         128,
         256
      })
      {
         images.add(appImage(s));
      }
      return images;
   }

   public static BufferedImage appImage(int size)
   {
      BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = img.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.scale(size / 32.0, size / 32.0);
      paint(g, Name.APP, null);
      g.dispose();
      return img;
   }

   private static final class VectorIcon implements Icon
   {
      private final Name name;
      private final int size;
      private final Color mono;

      VectorIcon(Name name, int size, Color mono)
      {
         this.name = name;
         this.size = size;
         this.mono = mono;
      }

      @Override
      public void paintIcon(Component c, Graphics g0, int x, int y)
      {
         Graphics2D g = (Graphics2D) g0.create();
         try
         {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.translate(x, y);
            g.scale(size / 32.0, size / 32.0);
            if (c != null && !c.isEnabled())
            {
               g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.35f));
            }
            paint(g, name, mono);
         }
         finally
         {
            g.dispose();
         }
      }

      @Override
      public int getIconWidth()
      {
         return size;
      }

      @Override
      public int getIconHeight()
      {
         return size;
      }
   }

   // ------------------------------------------------------------------ Zeichnungen

   private static Color c(Color color, Color mono)
   {
      return mono != null ? mono : color;
   }

   private static void stroke(Graphics2D g, float w)
   {
      g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
   }

   private static void paint(Graphics2D g, Name name, Color m)
   {
      switch (name)
      {
         case APP -> app(g);
         case OPEN -> open(g, m);
         case RECENT -> recent(g, m);
         case PRINT -> print(g, m);
         case PDF -> fileBadge(g, m, RED, "PDF");
         case XLSX -> xlsx(g, m);
         case PNG -> png(g, m);
         case CSV -> fileBadge(g, m, new Color(0x4F6B8A), "CSV");
         case GANTT -> gantt(g, m, false);
         case TRACKING -> gantt(g, m, true);
         case TASK_SHEET -> sheet(g, m);
         case RESOURCE_SHEET -> resources(g, m);
         case ZOOM_IN -> magnifier(g, m, 1);
         case ZOOM_OUT -> magnifier(g, m, -1);
         case FIND -> magnifier(g, m, 0);
         case ZOOM_ENTIRE -> zoomEntire(g, m);
         case SCROLL_TO_TASK -> scrollToTask(g, m);
         case TASK_INFO -> info(g, m);
         case NOTES -> notes(g, m);
         case PROJECT_INFO -> projectInfo(g, m);
         case OUTLINE -> outline(g, m);
         case COPY -> copy(g, m);
         case LANGUAGE -> globe(g, m);
         case CLOSE -> close(g, m);
         case BACK -> back(g, m);
         case LOCK -> lock(g, m);
         case TODAY -> today(g, m);
         case TIMESCALE -> timescale(g, m);
         case CRITICAL -> critical(g, m);
         case SETTINGS -> gear(g, m);
         case EXPORT -> export(g, m);
         case TABLE -> sheet(g, m);
         case EXPAND -> plusMinus(g, m, true);
         case COLLAPSE -> plusMinus(g, m, false);
         case PREV -> chevron(g, m, true);
         case NEXT -> chevron(g, m, false);
         case SORT -> sort(g, m);
         case FILTER -> funnel(g, m);
      }
   }

   private static void app(Graphics2D g)
   {
      g.setColor(Style.ACCENT);
      g.fill(new RoundRectangle2D.Double(1, 1, 30, 30, 7, 7));
      g.setColor(Color.WHITE);
      g.fill(new RoundRectangle2D.Double(6, 7, 12, 4.5, 1.5, 1.5));
      g.fill(new RoundRectangle2D.Double(11, 13.75, 14, 4.5, 1.5, 1.5));
      g.fill(new RoundRectangle2D.Double(16, 20.5, 10, 4.5, 1.5, 1.5));
      g.setColor(new Color(255, 255, 255, 170));
      stroke(g, 1.3f);
      g.draw(new Line2D.Double(18, 9.25, 21, 9.25));
      g.draw(new Line2D.Double(21, 9.25, 21, 13.2));
   }

   private static void page(Graphics2D g, Color m, double x, double y, double w, double h)
   {
      GeneralPath p = new GeneralPath();
      p.moveTo(x, y);
      p.lineTo(x + w - 6, y);
      p.lineTo(x + w, y + 6);
      p.lineTo(x + w, y + h);
      p.lineTo(x, y + h);
      p.closePath();
      g.setColor(m != null ? new Color(0, 0, 0, 0) : PAPER);
      g.fill(p);
      g.setColor(c(PAPER_EDGE, m));
      stroke(g, 1.4f);
      g.draw(p);
      GeneralPath fold = new GeneralPath();
      fold.moveTo(x + w - 6, y);
      fold.lineTo(x + w - 6, y + 6);
      fold.lineTo(x + w, y + 6);
      g.draw(fold);
   }

   private static void open(Graphics2D g, Color m)
   {
      GeneralPath back = new GeneralPath();
      back.moveTo(3, 7);
      back.lineTo(12, 7);
      back.lineTo(15, 10);
      back.lineTo(27, 10);
      back.lineTo(27, 25);
      back.lineTo(3, 25);
      back.closePath();
      g.setColor(c(YELLOW_DARK, m));
      g.fill(back);
      GeneralPath front = new GeneralPath();
      front.moveTo(6, 14);
      front.lineTo(30, 14);
      front.lineTo(26, 25);
      front.lineTo(2.5, 25);
      front.closePath();
      g.setColor(m != null ? m.darker() : YELLOW);
      g.fill(front);
   }

   private static void recent(Graphics2D g, Color m)
   {
      g.setColor(m != null ? new Color(0, 0, 0, 0) : PAPER);
      g.fill(new Ellipse2D.Double(4, 4, 24, 24));
      g.setColor(c(INK, m));
      stroke(g, 2f);
      g.draw(new Ellipse2D.Double(4, 4, 24, 24));
      g.draw(new Line2D.Double(16, 16, 16, 9));
      g.draw(new Line2D.Double(16, 16, 21, 19));
   }

   private static void print(Graphics2D g, Color m)
   {
      g.setColor(m != null ? new Color(0, 0, 0, 0) : PAPER);
      g.fill(new Rectangle2D.Double(9, 3, 14, 9));
      g.setColor(c(PAPER_EDGE, m));
      stroke(g, 1.4f);
      g.draw(new Rectangle2D.Double(9, 3, 14, 9));
      g.setColor(c(new Color(0x5B6573), m));
      g.fill(new RoundRectangle2D.Double(3, 11, 26, 12, 4, 4));
      g.setColor(m != null ? new Color(0, 0, 0, 0) : PAPER);
      g.fill(new Rectangle2D.Double(9, 19, 14, 10));
      g.setColor(c(PAPER_EDGE, m));
      g.draw(new Rectangle2D.Double(9, 19, 14, 10));
      g.draw(new Line2D.Double(12, 23, 20, 23));
      g.draw(new Line2D.Double(12, 26, 18, 26));
      g.setColor(m != null ? m : new Color(0x7FD37F));
      g.fill(new Ellipse2D.Double(24, 14, 2.5, 2.5));
   }

   private static void fileBadge(Graphics2D g, Color m, Color color, String text)
   {
      page(g, m, 6, 2, 21, 28);
      g.setColor(c(color, m));
      g.fill(new RoundRectangle2D.Double(2, 14, 24, 11, 3, 3));
      if (m == null)
      {
         g.setColor(Color.WHITE);
         g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 8));
         FontMetrics fm = g.getFontMetrics();
         g.drawString(text, (float) (14 - fm.stringWidth(text) / 2.0), 22.6f);
      }
   }

   private static void xlsx(Graphics2D g, Color m)
   {
      page(g, m, 6, 2, 21, 28);
      g.setColor(c(new Color(0xBFDCC9), m));
      stroke(g, 1.1f);
      for (int i = 0; i < 4; i++)
      {
         g.draw(new Line2D.Double(14, 9 + i * 5, 24, 9 + i * 5));
      }
      g.setColor(c(GREEN, m));
      g.fill(new RoundRectangle2D.Double(2, 9, 15, 15, 3, 3));
      g.setColor(m != null ? Style.ACCENT : Color.WHITE);
      stroke(g, 2.2f);
      g.draw(new Line2D.Double(6, 12.5, 13, 20.5));
      g.draw(new Line2D.Double(13, 12.5, 6, 20.5));
   }

   private static void png(Graphics2D g, Color m)
   {
      g.setColor(m != null ? new Color(0, 0, 0, 0) : new Color(0xDDEBF7));
      g.fill(new RoundRectangle2D.Double(3, 5, 26, 22, 3, 3));
      g.setColor(c(new Color(0x6F8BA8), m));
      stroke(g, 1.4f);
      g.draw(new RoundRectangle2D.Double(3, 5, 26, 22, 3, 3));
      g.setColor(c(YELLOW, m));
      g.fill(new Ellipse2D.Double(19, 9, 6, 6));
      GeneralPath mountains = new GeneralPath();
      mountains.moveTo(4, 26);
      mountains.lineTo(12, 15);
      mountains.lineTo(18, 22);
      mountains.lineTo(21, 19);
      mountains.lineTo(28, 26);
      mountains.closePath();
      g.setColor(c(new Color(0x4C8C4A), m));
      g.fill(mountains);
   }

   private static void gantt(Graphics2D g, Color m, boolean tracking)
   {
      g.setColor(c(new Color(0xB8B8B8), m));
      stroke(g, 1.2f);
      g.draw(new Line2D.Double(3, 4, 3, 28));
      g.draw(new Line2D.Double(3, 28, 29, 28));
      Color bar = tracking ? new Color(0xE06666) : BLUE;
      g.setColor(c(bar, m));
      g.fill(new Rectangle2D.Double(6, 5, 11, 5));
      g.setColor(c(BLUE, m));
      g.fill(new Rectangle2D.Double(12, 13, 12, 5));
      g.fill(new Rectangle2D.Double(19, 21, 9, 5));
      if (tracking)
      {
         g.setColor(c(new Color(0x9A9A9A), m));
         g.fill(new Rectangle2D.Double(6, 10.5, 11, 2));
         g.fill(new Rectangle2D.Double(10, 18.5, 12, 2));
         g.fill(new Rectangle2D.Double(17, 26, 9, 1.5));
      }
      else
      {
         g.setColor(c(INK, m));
         stroke(g, 1.2f);
         g.draw(new Line2D.Double(17, 7.5, 18, 7.5));
         g.draw(new Line2D.Double(18, 7.5, 18, 12.5));
         g.draw(new Line2D.Double(24, 15.5, 25, 15.5));
         g.draw(new Line2D.Double(25, 15.5, 25, 20.5));
      }
   }

   private static void sheet(Graphics2D g, Color m)
   {
      g.setColor(m != null ? new Color(0, 0, 0, 0) : PAPER);
      g.fill(new Rectangle2D.Double(3, 5, 26, 22));
      g.setColor(c(Style.ACCENT, m));
      g.fill(new Rectangle2D.Double(3, 5, 26, 5));
      g.setColor(c(PAPER_EDGE, m));
      stroke(g, 1.2f);
      g.draw(new Rectangle2D.Double(3, 5, 26, 22));
      for (int i = 1; i <= 3; i++)
      {
         g.draw(new Line2D.Double(3, 10 + i * 4.25, 29, 10 + i * 4.25));
      }
      g.draw(new Line2D.Double(10, 5, 10, 27));
      g.draw(new Line2D.Double(19, 5, 19, 27));
   }

   private static void resources(Graphics2D g, Color m)
   {
      g.setColor(c(new Color(0x7A8794), m));
      g.fill(new Ellipse2D.Double(17, 5, 8, 8));
      g.fill(new Arc2D.Double(13, 14, 16, 14, 0, 180, Arc2D.CHORD));
      g.setColor(c(BLUE, m));
      g.fill(new Ellipse2D.Double(6, 7, 9, 9));
      g.fill(new Arc2D.Double(2, 17, 17, 16, 0, 180, Arc2D.CHORD));
   }

   private static void magnifier(Graphics2D g, Color m, int sign)
   {
      g.setColor(m != null ? new Color(0, 0, 0, 0) : new Color(0xEAF2FB));
      g.fill(new Ellipse2D.Double(4, 4, 17, 17));
      g.setColor(c(INK, m));
      stroke(g, 2.4f);
      g.draw(new Ellipse2D.Double(4, 4, 17, 17));
      stroke(g, 4f);
      g.draw(new Line2D.Double(19, 19, 27, 27));
      stroke(g, 2.2f);
      if (sign != 0)
      {
         g.draw(new Line2D.Double(8.5, 12.5, 16.5, 12.5));
      }
      if (sign > 0)
      {
         g.draw(new Line2D.Double(12.5, 8.5, 12.5, 16.5));
      }
   }

   private static void zoomEntire(Graphics2D g, Color m)
   {
      g.setColor(c(BLUE, m));
      g.fill(new Rectangle2D.Double(9, 9, 7, 4));
      g.fill(new Rectangle2D.Double(14, 15, 9, 4));
      g.setColor(c(INK, m));
      stroke(g, 1.8f);
      Path2D l = new Path2D.Double();
      l.moveTo(6, 5);
      l.lineTo(3, 5);
      l.lineTo(3, 23);
      l.lineTo(6, 23);
      g.draw(l);
      Path2D r = new Path2D.Double();
      r.moveTo(26, 5);
      r.lineTo(29, 5);
      r.lineTo(29, 23);
      r.lineTo(26, 23);
      g.draw(r);
      g.draw(new Line2D.Double(6, 27.5, 26, 27.5));
      Path2D a1 = new Path2D.Double();
      a1.moveTo(9, 25);
      a1.lineTo(6, 27.5);
      a1.lineTo(9, 30);
      g.draw(a1);
      Path2D a2 = new Path2D.Double();
      a2.moveTo(23, 25);
      a2.lineTo(26, 27.5);
      a2.lineTo(23, 30);
      g.draw(a2);
   }

   private static void scrollToTask(Graphics2D g, Color m)
   {
      g.setColor(c(BLUE, m));
      g.fill(new Rectangle2D.Double(15, 12, 14, 7));
      g.setColor(c(INK, m));
      stroke(g, 2.2f);
      g.draw(new Line2D.Double(3, 15.5, 12, 15.5));
      Path2D a = new Path2D.Double();
      a.moveTo(8, 11);
      a.lineTo(12.5, 15.5);
      a.lineTo(8, 20);
      g.draw(a);
   }

   private static void info(Graphics2D g, Color m)
   {
      g.setColor(c(BLUE, m));
      g.fill(new Ellipse2D.Double(3, 3, 26, 26));
      g.setColor(m != null ? Style.ACCENT : Color.WHITE);
      g.fill(new Ellipse2D.Double(14, 7.5, 4, 4));
      g.fill(new RoundRectangle2D.Double(14, 13.5, 4, 11, 2, 2));
   }

   private static void notes(Graphics2D g, Color m)
   {
      GeneralPath p = new GeneralPath();
      p.moveTo(5, 4);
      p.lineTo(27, 4);
      p.lineTo(27, 21);
      p.lineTo(20, 28);
      p.lineTo(5, 28);
      p.closePath();
      g.setColor(c(new Color(0xFFF1A8), m));
      g.fill(p);
      g.setColor(c(YELLOW_DARK, m));
      stroke(g, 1.4f);
      g.draw(p);
      g.draw(new Line2D.Double(9, 10, 23, 10));
      g.draw(new Line2D.Double(9, 14.5, 23, 14.5));
      g.draw(new Line2D.Double(9, 19, 18, 19));
   }

   private static void projectInfo(Graphics2D g, Color m)
   {
      page(g, m, 4, 2, 20, 27);
      g.setColor(c(new Color(0xBBBBBB), m));
      stroke(g, 1.4f);
      g.draw(new Line2D.Double(8, 10, 18, 10));
      g.draw(new Line2D.Double(8, 14, 16, 14));
      g.setColor(c(BLUE, m));
      g.fill(new Ellipse2D.Double(15, 15, 15, 15));
      g.setColor(m != null ? Style.ACCENT : Color.WHITE);
      g.fill(new Ellipse2D.Double(21.3, 17.5, 2.4, 2.4));
      g.fill(new RoundRectangle2D.Double(21.3, 21, 2.4, 6.5, 1.2, 1.2));
   }

   private static void outline(Graphics2D g, Color m)
   {
      g.setColor(c(INK, m));
      g.fill(new Rectangle2D.Double(3, 4, 18, 4));
      g.setColor(c(new Color(0x8A8A8A), m));
      g.fill(new Rectangle2D.Double(9, 12, 18, 3.5));
      g.fill(new Rectangle2D.Double(9, 19, 20, 3.5));
      g.fill(new Rectangle2D.Double(9, 26, 14, 3.5));
      stroke(g, 1.2f);
      g.draw(new Line2D.Double(5, 8, 5, 27.75));
      g.draw(new Line2D.Double(5, 13.75, 8, 13.75));
      g.draw(new Line2D.Double(5, 20.75, 8, 20.75));
      g.draw(new Line2D.Double(5, 27.75, 8, 27.75));
   }

   private static void copy(Graphics2D g, Color m)
   {
      page(g, m, 4, 3, 16, 20);
      page(g, m, 12, 9, 16, 20);
   }

   private static void globe(Graphics2D g, Color m)
   {
      g.setColor(m != null ? new Color(0, 0, 0, 0) : new Color(0xDCEBF8));
      g.fill(new Ellipse2D.Double(4, 4, 24, 24));
      g.setColor(c(BLUE, m));
      stroke(g, 1.6f);
      g.draw(new Ellipse2D.Double(4, 4, 24, 24));
      g.draw(new Ellipse2D.Double(10, 4, 12, 24));
      g.draw(new Line2D.Double(4, 16, 28, 16));
      g.draw(new Line2D.Double(16, 4, 16, 28));
      g.draw(new Arc2D.Double(6, 2, 20, 10, 200, 140, Arc2D.OPEN));
      g.draw(new Arc2D.Double(6, 20, 20, 10, 20, 140, Arc2D.OPEN));
   }

   private static void close(Graphics2D g, Color m)
   {
      page(g, m, 6, 2, 20, 27);
      g.setColor(c(RED, m));
      stroke(g, 2.6f);
      g.draw(new Line2D.Double(11, 13, 21, 23));
      g.draw(new Line2D.Double(21, 13, 11, 23));
   }

   private static void back(Graphics2D g, Color m)
   {
      g.setColor(c(INK, m));
      stroke(g, 1.8f);
      g.draw(new Ellipse2D.Double(3, 3, 26, 26));
      stroke(g, 2.4f);
      g.draw(new Line2D.Double(9, 16, 23, 16));
      Path2D a = new Path2D.Double();
      a.moveTo(15, 10);
      a.lineTo(9, 16);
      a.lineTo(15, 22);
      g.draw(a);
   }

   private static void lock(Graphics2D g, Color m)
   {
      g.setColor(c(INK, m));
      stroke(g, 2.6f);
      g.draw(new Arc2D.Double(9, 4, 14, 16, 0, 180, Arc2D.OPEN));
      g.draw(new Line2D.Double(9, 12, 9, 15));
      g.draw(new Line2D.Double(23, 12, 23, 15));
      g.setColor(c(new Color(0xC8932A), m));
      g.fill(new RoundRectangle2D.Double(6, 14, 20, 15, 3, 3));
      g.setColor(m != null ? Style.ACCENT : Color.WHITE);
      g.fill(new Ellipse2D.Double(14, 18.5, 4, 4));
      g.fill(new Rectangle2D.Double(15.2, 21, 1.6, 4));
   }

   private static void today(Graphics2D g, Color m)
   {
      g.setColor(m != null ? new Color(0, 0, 0, 0) : PAPER);
      g.fill(new RoundRectangle2D.Double(3, 5, 26, 24, 3, 3));
      g.setColor(c(RED, m));
      g.fill(new Rectangle2D.Double(3, 5, 26, 6));
      g.setColor(c(PAPER_EDGE, m));
      stroke(g, 1.4f);
      g.draw(new RoundRectangle2D.Double(3, 5, 26, 24, 3, 3));
      g.draw(new Line2D.Double(9, 2.5, 9, 7));
      g.draw(new Line2D.Double(23, 2.5, 23, 7));
      g.setColor(c(Style.CURRENT_DATE, m));
      stroke(g, 2.4f);
      g.draw(new Line2D.Double(16, 13, 16, 27));
   }

   private static void timescale(Graphics2D g, Color m)
   {
      g.setColor(m != null ? new Color(0, 0, 0, 0) : new Color(0xF6E7B8));
      g.fill(new Rectangle2D.Double(2, 10, 28, 12));
      g.setColor(c(YELLOW_DARK, m));
      stroke(g, 1.4f);
      g.draw(new Rectangle2D.Double(2, 10, 28, 12));
      for (int i = 0; i <= 6; i++)
      {
         double x = 5 + i * 4;
         g.draw(new Line2D.Double(x, 10, x, i % 2 == 0 ? 17 : 14));
      }
   }

   private static void critical(Graphics2D g, Color m)
   {
      g.setColor(c(RED, m));
      g.fill(new Rectangle2D.Double(4, 6, 12, 6));
      g.fill(new Rectangle2D.Double(16, 20, 12, 6));
      stroke(g, 1.6f);
      g.draw(new Line2D.Double(16, 9, 20, 9));
      g.draw(new Line2D.Double(20, 9, 20, 18));
   }

   private static void gear(Graphics2D g, Color m)
   {
      g.setColor(c(new Color(0x5B6573), m));
      Path2D gear = new Path2D.Double();
      int teeth = 8;
      for (int i = 0; i < teeth * 2; i++)
      {
         double a = Math.PI * 2 * i / (teeth * 2);
         double r = (i % 2 == 0) ? 13 : 10;
         double x = 16 + r * Math.cos(a);
         double y = 16 + r * Math.sin(a);
         if (i == 0)
         {
            gear.moveTo(x, y);
         }
         else
         {
            gear.lineTo(x, y);
         }
      }
      gear.closePath();
      g.fill(gear);
      g.setColor(m != null ? Style.ACCENT : Color.WHITE);
      g.fill(new Ellipse2D.Double(11, 11, 10, 10));
   }

   private static void export(Graphics2D g, Color m)
   {
      page(g, m, 4, 2, 18, 26);
      g.setColor(c(GREEN, m));
      stroke(g, 2.6f);
      g.draw(new Line2D.Double(14, 18, 27, 18));
      Path2D a = new Path2D.Double();
      a.moveTo(22, 13);
      a.lineTo(27.5, 18);
      a.lineTo(22, 23);
      g.draw(a);
   }

   private static void plusMinus(Graphics2D g, Color m, boolean plus)
   {
      g.setColor(c(INK, m));
      stroke(g, 1.6f);
      g.draw(new Rectangle2D.Double(6, 6, 20, 20));
      stroke(g, 2.4f);
      g.draw(new Line2D.Double(10.5, 16, 21.5, 16));
      if (plus)
      {
         g.draw(new Line2D.Double(16, 10.5, 16, 21.5));
      }
   }

   private static void sort(Graphics2D g, Color m)
   {
      g.setColor(c(BLUE, m));
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
      g.drawString("A", 3f, 13f);
      g.drawString("Z", 3f, 28f);
      g.setColor(c(INK, m));
      stroke(g, 2.2f);
      g.draw(new Line2D.Double(22, 4, 22, 27));
      Path2D a = new Path2D.Double();
      a.moveTo(17, 22);
      a.lineTo(22, 28);
      a.lineTo(27, 22);
      g.draw(a);
   }

   private static void funnel(Graphics2D g, Color m)
   {
      Path2D f = new Path2D.Double();
      f.moveTo(3, 5);
      f.lineTo(29, 5);
      f.lineTo(19, 17);
      f.lineTo(19, 28);
      f.lineTo(13, 25);
      f.lineTo(13, 17);
      f.closePath();
      g.setColor(c(Style.ACCENT, m));
      g.fill(f);
   }

   private static void chevron(Graphics2D g, Color m, boolean left)
   {
      g.setColor(c(INK, m));
      stroke(g, 3f);
      Path2D p = new Path2D.Double();
      if (left)
      {
         p.moveTo(20, 7);
         p.lineTo(11, 16);
         p.lineTo(20, 25);
      }
      else
      {
         p.moveTo(12, 7);
         p.lineTo(21, 16);
         p.lineTo(12, 25);
      }
      g.draw(p);
   }
}
