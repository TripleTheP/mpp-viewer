package io.github.triplethep.mppviewer.ui.view;

import io.github.triplethep.mppviewer.render.Style;
import io.github.triplethep.mppviewer.render.TimescalePainter;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Zeitskala über dem Diagramm; scrollt waagerecht mit.
 */
final class TimescaleHeader extends JComponent
{
   private static final long serialVersionUID = 1L;
   private final ProjectViewModel model;

   TimescaleHeader(ProjectViewModel model)
   {
      this.model = model;
      setOpaque(true);
   }

   @Override
   public Dimension getPreferredSize()
   {
      return new Dimension(model.getTimeline().getWidth(), Style.HEADER_HEIGHT);
   }

   @Override
   protected void paintComponent(Graphics g0)
   {
      Graphics2D g = (Graphics2D) g0.create();
      try
      {
         Rectangle clip = g.getClipBounds();
         if (clip == null)
         {
            clip = new Rectangle(0, 0, getWidth(), getHeight());
         }
         TimescalePainter.paint(g, model.getTimeline(), model.getFormats(), clip.x, clip.x + clip.width);
      }
      finally
      {
         g.dispose();
      }
   }
}
