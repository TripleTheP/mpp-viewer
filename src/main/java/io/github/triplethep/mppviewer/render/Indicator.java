package io.github.triplethep.mppviewer.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Kleine Symbole der Indikatoren- und Vorgangsmodus-Spalte, als Vektorgrafik gezeichnet (scharf auf jedem
 * Bildschirm und im PDF).
 */
public enum Indicator
{
   NOTE,
   DONE,
   CONSTRAINT,
   CONSTRAINT_HARD,
   HYPERLINK,
   DEADLINE_MISSED,
   INACTIVE,
   MODE_AUTO,
   MODE_MANUAL,
   /** Kopf der Indikatorenspalte */
   INFO;

   /**
    * Zeichnet das Symbol in ein Quadrat der Kantenlänge {@code size} mit linker oberer Ecke (x, y).
    */
   public void paint(Graphics2D g0, double x, double y, double size)
   {
      Graphics2D g = (Graphics2D) g0.create();
      try
      {
         g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g.translate(x, y);
         g.scale(size / 16.0, size / 16.0);
         g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
         switch (this)
         {
            case NOTE -> note(g);
            case DONE -> done(g);
            case CONSTRAINT -> calendar(g, Style.ICON_BLUE);
            case CONSTRAINT_HARD -> calendar(g, Style.ICON_RED);
            case HYPERLINK -> link(g);
            case DEADLINE_MISSED -> deadline(g);
            case INACTIVE -> inactive(g);
            case MODE_AUTO -> modeAuto(g);
            case MODE_MANUAL -> modeManual(g);
            case INFO -> info(g);
         }
      }
      finally
      {
         g.dispose();
      }
   }

   /**
    * Text für Export und Tooltips.
    */
   public String key()
   {
      return "indicator." + name();
   }

   private static void note(Graphics2D g)
   {
      Path2D p = new Path2D.Double();
      p.moveTo(3, 2);
      p.lineTo(10, 2);
      p.lineTo(13, 5);
      p.lineTo(13, 14);
      p.lineTo(3, 14);
      p.closePath();
      g.setColor(new Color(0xFFF4C2));
      g.fill(p);
      g.setColor(new Color(0x9A7B14));
      g.draw(p);
      g.draw(new Line2D.Double(5, 7, 11, 7));
      g.draw(new Line2D.Double(5, 9.5, 11, 9.5));
      g.draw(new Line2D.Double(5, 12, 9, 12));
   }

   private static void done(Graphics2D g)
   {
      g.setColor(Style.ICON_GREEN);
      g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      Path2D p = new Path2D.Double();
      p.moveTo(3, 8.5);
      p.lineTo(6.5, 12);
      p.lineTo(13, 4);
      g.draw(p);
   }

   private static void calendar(Graphics2D g, Color accent)
   {
      g.setColor(Color.WHITE);
      g.fill(new RoundRectangle2D.Double(2, 3, 12, 11, 2, 2));
      g.setColor(accent);
      g.fill(new Rectangle2D.Double(2, 3, 12, 3.5));
      g.setColor(Style.ICON_GREY);
      g.draw(new RoundRectangle2D.Double(2, 3, 12, 11, 2, 2));
      g.fill(new Rectangle2D.Double(5, 9, 2, 2));
      g.fill(new Rectangle2D.Double(9, 9, 2, 2));
      g.draw(new Line2D.Double(5, 1.5, 5, 4));
      g.draw(new Line2D.Double(11, 1.5, 11, 4));
   }

   private static void link(Graphics2D g)
   {
      g.setColor(Style.ICON_BLUE);
      g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.draw(new RoundRectangle2D.Double(1.5, 6, 7.5, 4.5, 4.5, 4.5));
      g.draw(new RoundRectangle2D.Double(7, 6, 7.5, 4.5, 4.5, 4.5));
   }

   private static void deadline(Graphics2D g)
   {
      Path2D d = new Path2D.Double();
      d.moveTo(8, 1.5);
      d.lineTo(14.5, 8);
      d.lineTo(8, 14.5);
      d.lineTo(1.5, 8);
      d.closePath();
      g.setColor(Style.ICON_RED);
      g.fill(d);
      g.setColor(Color.WHITE);
      g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.draw(new Line2D.Double(8, 4.8, 8, 8.8));
      g.fill(new Ellipse2D.Double(7, 10.2, 2, 2));
   }

   private static void inactive(Graphics2D g)
   {
      g.setColor(Style.ICON_GREY);
      g.draw(new Ellipse2D.Double(2.5, 2.5, 11, 11));
      g.draw(new Line2D.Double(4.5, 11.5, 11.5, 4.5));
   }

   private static void modeAuto(Graphics2D g)
   {
      g.setColor(Style.TASK_FILL);
      g.fill(new Rectangle2D.Double(1, 4.5, 10, 7));
      g.setColor(Style.TASK_BORDER);
      g.draw(new Rectangle2D.Double(1, 4.5, 10, 7));
      g.setColor(Style.ICON_GREY);
      Path2D a = new Path2D.Double();
      a.moveTo(11.5, 4.5);
      a.lineTo(15.5, 8);
      a.lineTo(11.5, 11.5);
      a.closePath();
      g.fill(a);
   }

   private static void info(Graphics2D g)
   {
      g.setColor(Style.ICON_GREY);
      g.setStroke(new BasicStroke(1.3f));
      g.draw(new Ellipse2D.Double(2, 2, 12, 12));
      g.fill(new Ellipse2D.Double(7.1, 4.2, 1.8, 1.8));
      g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.draw(new Line2D.Double(8, 7.6, 8, 11.6));
   }

   private static void modeManual(Graphics2D g)
   {
      // Pinnwandnadel wie in Project
      g.setColor(Style.MANUAL_BORDER);
      Path2D head = new Path2D.Double();
      head.moveTo(6, 1.5);
      head.lineTo(12, 1.5);
      head.lineTo(11, 3);
      head.lineTo(11, 7);
      head.lineTo(13, 9);
      head.lineTo(5, 9);
      head.lineTo(7, 7);
      head.lineTo(7, 3);
      head.closePath();
      g.fill(head);
      g.setColor(Style.ICON_GREY);
      g.setStroke(new BasicStroke(1.3f));
      g.draw(new Line2D.Double(9, 9, 9, 15));
   }
}
