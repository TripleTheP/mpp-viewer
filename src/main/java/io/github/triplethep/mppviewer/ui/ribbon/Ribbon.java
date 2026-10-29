package io.github.triplethep.mppviewer.ui.ribbon;

import io.github.triplethep.mppviewer.render.Style;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Menüband im Office-Stil: grüne Registerleiste, darunter die Gruppen der gewählten Registerkarte.
 */
public final class Ribbon extends JPanel
{
   private static final Color BODY = new Color(0xF3F3F3);
   private static final Color BODY_BORDER = new Color(0xD2D2D2);
   public static final int BODY_HEIGHT = 96;

   private final JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
   private final JPanel body = new JPanel(new CardLayout());
   private final List<TabLabel> tabs = new ArrayList<>();
   private int selected = -1;
   private boolean collapsed;

   public Ribbon()
   {
      super(new BorderLayout());
      strip.setBackground(Style.ACCENT);
      strip.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
      body.setBackground(BODY);
      body.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BODY_BORDER));
      body.setPreferredSize(new Dimension(100, BODY_HEIGHT));
      add(strip, BorderLayout.NORTH);
      add(body, BorderLayout.CENTER);
   }

   /**
    * Registerkarte „Datei“: öffnet die Backstage-Ansicht statt Gruppen anzuzeigen.
    */
   public void addFileTab(String title, Runnable action)
   {
      TabLabel label = new TabLabel(title, -1, true);
      label.addMouseListener(new MouseAdapter()
      {
         @Override
         public void mouseClicked(MouseEvent e)
         {
            action.run();
         }
      });
      strip.add(label);
   }

   /**
    * Fügt eine Registerkarte hinzu und liefert den Container für ihre Gruppen.
    */
   public JPanel addTab(String title)
   {
      JPanel content = new JPanel();
      content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
      content.setBackground(BODY);
      content.setBorder(BorderFactory.createEmptyBorder(3, 4, 0, 4));
      int index = tabs.size();
      body.add(wrap(content), Integer.toString(index));
      TabLabel label = new TabLabel(title, index, false);
      label.addMouseListener(new MouseAdapter()
      {
         @Override
         public void mouseClicked(MouseEvent e)
         {
            if (e.getClickCount() == 2)
            {
               setCollapsed(!collapsed);
            }
            else
            {
               if (collapsed)
               {
                  setCollapsed(false);
               }
               select(index);
            }
         }
      });
      tabs.add(label);
      strip.add(label);
      if (selected < 0)
      {
         select(0);
      }
      return content;
   }

   private static JComponent wrap(JPanel content)
   {
      JPanel outer = new JPanel(new BorderLayout());
      outer.setBackground(BODY);
      outer.add(content, BorderLayout.WEST);
      return outer;
   }

   public void select(int index)
   {
      selected = index;
      ((CardLayout) body.getLayout()).show(body, Integer.toString(index));
      for (TabLabel t : tabs)
      {
         t.repaint();
      }
   }

   public int getSelectedIndex()
   {
      return selected;
   }

   public int getTabCount()
   {
      return tabs.size();
   }

   /**
    * Menüband minimieren (Doppelklick auf eine Registerkarte, wie in Office).
    */
   public void setCollapsed(boolean value)
   {
      collapsed = value;
      body.setVisible(!value);
      revalidate();
   }

   /**
    * Vertikaler Trennstrich zwischen Gruppen.
    */
   public static JComponent separator()
   {
      JComponent sep = new JComponent()
      {
         private static final long serialVersionUID = 1L;

         @Override
         protected void paintComponent(Graphics g)
         {
            g.setColor(BODY_BORDER);
            g.drawLine(getWidth() / 2, 4, getWidth() / 2, getHeight() - 6);
         }
      };
      sep.setPreferredSize(new Dimension(9, BODY_HEIGHT - 4));
      sep.setMaximumSize(new Dimension(9, BODY_HEIGHT));
      return sep;
   }

   public static JComponent gap(int w)
   {
      return (JComponent) Box.createHorizontalStrut(w);
   }

   private final class TabLabel extends JPanel
   {
      private static final long serialVersionUID = 1L;
      private final String title;
      private final int index;
      private final boolean fileTab;
      private boolean hover;

      TabLabel(String title, int index, boolean fileTab)
      {
         this.title = title;
         this.index = index;
         this.fileTab = fileTab;
         setFont(Style.UI_FONT.deriveFont(12.5f));
         setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
         setOpaque(false);
         addMouseListener(new MouseAdapter()
         {
            @Override
            public void mouseEntered(MouseEvent e)
            {
               hover = true;
               repaint();
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
               hover = false;
               repaint();
            }
         });
         getAccessibleContext().setAccessibleName(title);
      }

      @Override
      public Dimension getPreferredSize()
      {
         FontMetrics fm = getFontMetrics(getFont());
         return new Dimension(fm.stringWidth(title) + (fileTab ? 30 : 26), 28);
      }

      @Override
      protected void paintComponent(Graphics g0)
      {
         Graphics2D g = (Graphics2D) g0.create();
         g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         boolean active = !fileTab && index == selected && !collapsed;
         if (fileTab)
         {
            g.setColor(hover ? Style.ACCENT_HOVER : Style.ACCENT_DARK);
            g.fillRect(0, 0, getWidth(), getHeight());
         }
         else if (active)
         {
            g.setColor(BODY);
            g.fillRect(0, 3, getWidth(), getHeight() - 3);
         }
         else if (hover)
         {
            g.setColor(Style.ACCENT_HOVER);
            g.fillRect(0, 3, getWidth(), getHeight() - 3);
         }
         Font font = getFont();
         g.setFont(font);
         FontMetrics fm = g.getFontMetrics();
         g.setColor(active ? Style.ACCENT : Color.WHITE);
         int x = (getWidth() - fm.stringWidth(title)) / 2;
         int y = 3 + (getHeight() - 3 + fm.getAscent() - fm.getDescent()) / 2;
         g.drawString(title, x, y);
         g.dispose();
      }
   }
}
