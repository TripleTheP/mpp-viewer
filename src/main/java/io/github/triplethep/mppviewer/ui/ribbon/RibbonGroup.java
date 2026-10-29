package io.github.triplethep.mppviewer.ui.ribbon;

import io.github.triplethep.mppviewer.render.Style;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Eine beschriftete Gruppe im Menüband (z. B. „Zoom“ oder „Aufgabenansichten“).
 */
public final class RibbonGroup extends JPanel
{
   private static final Color LABEL = new Color(0x5F5F5F);

   private final JPanel content = new JPanel();

   public RibbonGroup(String title)
   {
      super(new BorderLayout());
      setOpaque(false);
      content.setOpaque(false);
      content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
      content.setBorder(BorderFactory.createEmptyBorder(1, 2, 0, 2));
      add(content, BorderLayout.CENTER);
      JLabel label = new JLabel(title, SwingConstants.CENTER);
      label.setForeground(LABEL);
      label.setFont(Style.UI_FONT.deriveFont(11f));
      label.setBorder(BorderFactory.createEmptyBorder(0, 4, 3, 4));
      add(label, BorderLayout.SOUTH);
      setMaximumSize(new Dimension(Integer.MAX_VALUE, Ribbon.BODY_HEIGHT));
   }

   /**
    * Großer Knopf (Symbol oben, Text darunter).
    */
   public RibbonGroup large(JComponent c)
   {
      c.setAlignmentY(Component.TOP_ALIGNMENT);
      content.add(c);
      return this;
   }

   /**
    * Bis zu drei kleine Elemente übereinander.
    */
   public RibbonGroup stack(JComponent... items)
   {
      JPanel column = new JPanel();
      column.setOpaque(false);
      column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
      column.setAlignmentY(Component.TOP_ALIGNMENT);
      for (JComponent item : items)
      {
         item.setAlignmentX(Component.LEFT_ALIGNMENT);
         column.add(item);
      }
      column.add(Box.createVerticalGlue());
      content.add(column);
      return this;
   }

   public RibbonGroup gap(int w)
   {
      content.add(Box.createHorizontalStrut(w));
      return this;
   }
}
