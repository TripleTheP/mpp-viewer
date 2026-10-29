package io.github.triplethep.mppviewer.export;

import io.github.triplethep.mppviewer.core.ColumnDef;
import io.github.triplethep.mppviewer.render.CanvasPainter;
import io.github.triplethep.mppviewer.render.Style;
import io.github.triplethep.mppviewer.render.ViewSnapshot;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Exportiert die komplette Ansicht als PNG-Bild.
 */
public final class PngExporter
{
   /** Obergrenzen, damit auch riesige Projekte nicht den Speicher sprengen. */
   private static final long MAX_PIXELS = 50_000_000L;
   private static final int MAX_SIDE = 30_000;

   private PngExporter()
   {
   }

   /**
    * @return tatsächlich verwendeter Skalierungsfaktor (kann kleiner als gewünscht sein)
    */
   public static double export(ViewSnapshot view, List<ColumnDef> columns, double requestedScale, File file) throws IOException
   {
      CanvasPainter canvas = new CanvasPainter(view, columns, ExportTimeline.forView(view));
      double w = canvas.getWidth();
      double h = Style.HEADER_HEIGHT + canvas.getRowCount() * (double) Style.ROW_HEIGHT;
      double scale = requestedScale;
      scale = Math.min(scale, MAX_SIDE / w);
      scale = Math.min(scale, MAX_SIDE / h);
      scale = Math.min(scale, Math.sqrt(MAX_PIXELS / (w * h)));
      int iw = Math.max(1, (int) Math.ceil(w * scale));
      int ih = Math.max(1, (int) Math.ceil(h * scale));
      BufferedImage img = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = img.createGraphics();
      try
      {
         g.setColor(Color.WHITE);
         g.fillRect(0, 0, iw, ih);
         g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
         g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
         g.scale(scale, scale);
         canvas.paint(g, 0, w, 0, canvas.getRowCount() - 1);
      }
      finally
      {
         g.dispose();
      }
      if (!ImageIO.write(img, "png", file))
      {
         throw new IOException("PNG writer not available");
      }
      return scale;
   }
}
