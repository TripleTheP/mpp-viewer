package io.github.triplethep.mppviewer.ui.view;

import io.github.triplethep.mppviewer.core.TaskRow;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.render.GanttPainter;
import io.github.triplethep.mppviewer.render.Style;
import io.github.triplethep.mppviewer.render.Timeline;
import org.mpxj.Task;

import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Diagrammteil der Gantt-Ansicht am Bildschirm. Zeichnet nur den sichtbaren Ausschnitt.
 */
final class ChartPanel extends JComponent implements Scrollable
{
   private static final long serialVersionUID = 1L;

   private final ProjectViewModel model;
   private final ListSelectionModel selection;

   ChartPanel(ProjectViewModel model, ListSelectionModel selection)
   {
      this.model = model;
      this.selection = selection;
      setOpaque(true);
      setToolTipText("");
      setFocusable(false);
   }

   @Override
   public Dimension getPreferredSize()
   {
      int rows = model.getOutline().getRows().size();
      return new Dimension(model.getTimeline().getWidth(), rows * Style.ROW_HEIGHT);
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
         g.setColor(java.awt.Color.WHITE);
         g.fillRect(clip.x, clip.y, clip.width, clip.height);
         int firstRow = Math.max(0, clip.y / Style.ROW_HEIGHT);
         int lastRow = (clip.y + clip.height) / Style.ROW_HEIGHT + 1;
         GanttPainter painter = painter();
         int rows = model.getOutline().getRows().size();
         if (firstRow < rows)
         {
            painter.paint(g, clip.x, clip.x + clip.width, firstRow, lastRow, selection::isSelectedIndex);
         }
         int bottom = rows * Style.ROW_HEIGHT;
         if (clip.y + clip.height > bottom)
         {
            painter.paintEmptyArea(g, clip.x, clip.x + clip.width, Math.max(bottom, clip.y), clip.y + clip.height);
         }
      }
      finally
      {
         g.dispose();
      }
   }

   GanttPainter painter()
   {
      return new GanttPainter(model.getDoc(), model.getOutline(), model.getTimeline(), model.getOptions(), model.getFormats());
   }

   int rowAt(int y)
   {
      int row = y / Style.ROW_HEIGHT;
      return row >= 0 && row < model.getOutline().getRows().size() ? row : -1;
   }

   @Override
   public String getToolTipText(MouseEvent e)
   {
      int row = rowAt(e.getY());
      if (row < 0)
      {
         return null;
      }
      TaskRow tr = model.getOutline().getRows().get(row);
      Task task = tr.task();
      if (task.getNull() || task.getStart() == null)
      {
         return null;
      }
      Timeline tl = model.getTimeline();
      double xs = tl.x(task.getStart());
      double xe = task.getFinish() == null ? xs : tl.x(task.getFinish());
      if (e.getX() < xs - 8 || e.getX() > xe + 8)
      {
         return null;
      }
      String kind;
      if (tr.isProjectSummary())
      {
         kind = I18n.t("tooltip.projectSummary");
      }
      else if (tr.hasChildren())
      {
         kind = I18n.t("tooltip.summary");
      }
      else if (task.getMilestone())
      {
         kind = I18n.t("tooltip.milestone");
      }
      else
      {
         kind = I18n.t("tooltip.task");
      }
      StringBuilder sb = new StringBuilder("<html><b>").append(kind).append(": ").append(escape(task.getName())).append("</b><br>");
      sb.append(I18n.t("field.task.START")).append(": ").append(model.getFormats().date(task.getStart())).append("&nbsp;&nbsp;&nbsp;");
      sb.append(I18n.t("field.task.DURATION")).append(": ").append(model.getFormats().duration(task.getDuration(), task.getEstimated())).append("<br>");
      sb.append(I18n.t("field.task.FINISH")).append(": ").append(model.getFormats().date(task.getFinish()));
      Number pct = task.getPercentageComplete();
      if (pct != null && pct.doubleValue() > 0)
      {
         sb.append("<br>").append(I18n.t("field.task.PERCENT_COMPLETE")).append(": ").append(model.getFormats().percent(pct));
      }
      return sb.append("</html>").toString();
   }

   private static String escape(String s)
   {
      if (s == null)
      {
         return "";
      }
      return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
   }

   LocalDateTime timeAt(int x)
   {
      return model.getTimeline().time(x);
   }

   List<TaskRow> rows()
   {
      return model.getOutline().getRows();
   }

   // ------------------------------------------------------------------ Scrollable

   @Override
   public Dimension getPreferredScrollableViewportSize()
   {
      return new Dimension(600, 400);
   }

   @Override
   public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
   {
      if (orientation == SwingConstants.VERTICAL)
      {
         return Style.ROW_HEIGHT;
      }
      return (int) Math.max(12, Math.min(200, model.getTimeline().getPxPerDay()));
   }

   @Override
   public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
   {
      if (orientation == SwingConstants.VERTICAL)
      {
         return Math.max(Style.ROW_HEIGHT, (visibleRect.height / Style.ROW_HEIGHT - 1) * Style.ROW_HEIGHT);
      }
      return Math.max(50, visibleRect.width - 50);
   }

   @Override
   public boolean getScrollableTracksViewportWidth()
   {
      return false;
   }

   @Override
   public boolean getScrollableTracksViewportHeight()
   {
      return getParent() != null && getParent().getHeight() > getPreferredSize().height;
   }
}
