package io.github.triplethep.mppviewer.ui;

import io.github.triplethep.mppviewer.render.Style;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Locale;

/**
 * Senkrechter Ansichtsname am linken Rand („GANTT-DIAGRAMM“), wie in Project 2013 und neuer.
 */
public final class ViewStrip extends javax.swing.JPanel
{
   private static final long serialVersionUID = 1L;
   private static final Color BG = new Color(0xE6E6E6);
   private static final Color FG = new Color(0x5A5A5A);
   private String text = "";

   public ViewStrip()
   {
      setOpaque(true);
   }

   public void setViewName(String name)
   {
      this.text = name == null ? "" : name.toUpperCase(Locale.ROOT);
      getAccessibleContext().setAccessibleName(name);
      repaint();
   }

   @Override
   public Dimension getPreferredSize()
   {
      return new Dimension(22, 100);
   }

   @Override
   protected void paintComponent(Graphics g0)
   {
      Graphics2D g = (Graphics2D) g0.create();
      g.setColor(BG);
      g.fillRect(0, 0, getWidth(), getHeight());
      g.setColor(new Color(0xCFCFCF));
      g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      Font font = Style.UI_FONT.deriveFont(Font.BOLD, 11f);
      g.setFont(font);
      FontMetrics fm = g.getFontMetrics();
      int textW = fm.stringWidth(text);
      g.rotate(-Math.PI / 2);
      g.setColor(FG);
      // Nach der Drehung um -90° wächst x nach oben: Text senkrecht mittig, von unten nach oben lesbar
      int x = -(getHeight() + textW) / 2;
      int y = (getWidth() + fm.getAscent() - fm.getDescent()) / 2;
      g.drawString(text, x, y);
      g.dispose();
   }
}
