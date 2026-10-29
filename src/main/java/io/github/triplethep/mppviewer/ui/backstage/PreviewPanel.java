package io.github.triplethep.mppviewer.ui.backstage;

import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.print.PrintLayout;
import io.github.triplethep.mppviewer.ui.Icons;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Seitenansicht: zeigt die Druckseiten so, wie sie auf Papier und im PDF erscheinen.
 */
final class PreviewPanel extends JPanel
{
   private static final long serialVersionUID = 1L;
   private static final Color BG = new Color(0xE6E6E6);

   private PrintLayout layout;
   private int page;
   private boolean multi;
   private final Canvas canvas = new Canvas();
   private final JLabel pageLabel = new JLabel();
   private final JButton prev;
   private final JButton next;

   PreviewPanel()
   {
      super(new BorderLayout());
      setBackground(BG);
      add(canvas, BorderLayout.CENTER);

      JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 3));
      bar.setBackground(BG);
      prev = navButton(Icons.Name.PREV, I18n.t("preview.prev"));
      next = navButton(Icons.Name.NEXT, I18n.t("preview.next"));
      prev.addActionListener(e -> setPage(page - 1));
      next.addActionListener(e -> setPage(page + 1));
      JToggleButton multiButton = new JToggleButton(I18n.t("preview.multi"));
      multiButton.setFocusable(false);
      multiButton.addActionListener(e ->
      {
         multi = multiButton.isSelected();
         canvas.repaint();
         updateLabel();
      });
      bar.add(prev);
      bar.add(pageLabel);
      bar.add(next);
      bar.add(javax.swing.Box.createHorizontalStrut(20));
      bar.add(multiButton);
      add(bar, BorderLayout.SOUTH);

      canvas.addMouseWheelListener(new MouseAdapter()
      {
         @Override
         public void mouseWheelMoved(MouseWheelEvent e)
         {
            if (!multi)
            {
               setPage(page + (e.getWheelRotation() > 0 ? 1 : -1));
            }
         }
      });
   }

   private static JButton navButton(Icons.Name icon, String tooltip)
   {
      JButton b = new JButton(Icons.get(icon, 16));
      b.putClientProperty("JButton.buttonType", "toolBarButton");
      b.setToolTipText(tooltip);
      b.getAccessibleContext().setAccessibleName(tooltip);
      b.setFocusable(false);
      return b;
   }

   void showLayout(PrintLayout layout)
   {
      this.layout = layout;
      this.page = Math.max(0, Math.min(page, layout.getNumberOfPages() - 1));
      updateLabel();
      canvas.repaint();
   }

   private void setPage(int p)
   {
      if (layout == null)
      {
         return;
      }
      page = Math.max(0, Math.min(layout.getNumberOfPages() - 1, p));
      updateLabel();
      canvas.repaint();
   }

   private void updateLabel()
   {
      if (layout == null)
      {
         pageLabel.setText("");
         return;
      }
      int n = layout.getNumberOfPages();
      pageLabel.setText(multi ? I18n.t("preview.pages", n) : I18n.t("print.page", page + 1, n));
      prev.setEnabled(!multi && page > 0);
      next.setEnabled(!multi && page < n - 1);
   }

   private final class Canvas extends JComponent
   {
      private static final long serialVersionUID = 1L;

      Canvas()
      {
         setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
      }

      @Override
      public Dimension getPreferredSize()
      {
         return new Dimension(500, 400);
      }

      @Override
      protected void paintComponent(Graphics g0)
      {
         Graphics2D g = (Graphics2D) g0.create();
         g.setColor(BG);
         g.fillRect(0, 0, getWidth(), getHeight());
         if (layout == null)
         {
            g.dispose();
            return;
         }
         double pw = layout.getSettings().pageWidth();
         double ph = layout.getSettings().pageHeight();
         int availW = getWidth() - 32;
         int availH = getHeight() - 32;
         if (!multi)
         {
            double s = Math.min(availW / pw, availH / ph);
            drawPage(g, page, (getWidth() - pw * s) / 2.0, (getHeight() - ph * s) / 2.0, s);
         }
         else
         {
            int n = layout.getNumberOfPages();
            int cols = Math.max(1, layout.getPagesWide());
            int rows = (int) Math.ceil(n / (double) cols);
            if (cols * rows > 1 && (double) availW / cols < 60)
            {
               cols = (int) Math.ceil(Math.sqrt(n));
               rows = (int) Math.ceil(n / (double) cols);
            }
            double gap = 10;
            double s = Math.min((availW - gap * (cols - 1)) / (cols * pw), (availH - gap * (rows - 1)) / (rows * ph));
            double totalW = cols * pw * s + (cols - 1) * gap;
            double totalH = rows * ph * s + (rows - 1) * gap;
            double x0 = (getWidth() - totalW) / 2.0;
            double y0 = (getHeight() - totalH) / 2.0;
            for (int i = 0; i < n; i++)
            {
               // Anordnung entsprechend der Seitenreihenfolge (Spalten = Seiten in der Breite)
               int c;
               int r;
               if (layout.getSettings().order == io.github.triplethep.mppviewer.print.PrintSettings.Order.DOWN_THEN_OVER && layout.getPagesWide() == cols)
               {
                  c = i / layout.getPagesTall();
                  r = i % layout.getPagesTall();
               }
               else
               {
                  c = i % cols;
                  r = i / cols;
               }
               drawPage(g, i, x0 + c * (pw * s + gap), y0 + r * (ph * s + gap), s);
            }
         }
         g.dispose();
      }

      private void drawPage(Graphics2D g, int index, double x, double y, double s)
      {
         int w = (int) Math.round(layout.getSettings().pageWidth() * s);
         int h = (int) Math.round(layout.getSettings().pageHeight() * s);
         g.setColor(new Color(0, 0, 0, 40));
         g.fillRect((int) x + 3, (int) y + 3, w, h);
         // In ein Bild zeichnen (schärfer als direkte Skalierung kleiner Schrift)
         double dpiScale = 1.0;
         AffineTransform tx = g.getTransform();
         if (tx != null)
         {
            dpiScale = Math.max(1.0, tx.getScaleX());
         }
         int iw = Math.max(1, (int) Math.round(w * dpiScale));
         int ih = Math.max(1, (int) Math.round(h * dpiScale));
         BufferedImage img = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_RGB);
         Graphics2D ig = img.createGraphics();
         ig.setColor(Color.WHITE);
         ig.fillRect(0, 0, iw, ih);
         ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         ig.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         ig.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
         ig.scale(s * dpiScale, s * dpiScale);
         ig.translate(layout.getSettings().margin, layout.getSettings().margin);
         layout.paintPage(ig, index);
         ig.dispose();
         g.drawImage(img, (int) Math.round(x), (int) Math.round(y), w, h, null);
         g.setColor(new Color(0xB0B0B0));
         g.drawRect((int) Math.round(x), (int) Math.round(y), w - 1, h - 1);
      }
   }
}
