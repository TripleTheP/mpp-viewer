package io.github.triplethep.mppviewer.render;

import io.github.triplethep.mppviewer.core.Formats;
import io.github.triplethep.mppviewer.core.ProjectDocument;
import io.github.triplethep.mppviewer.core.TaskOutline;
import io.github.triplethep.mppviewer.core.TaskRow;
import org.mpxj.LocalDateTimeRange;
import org.mpxj.Relation;
import org.mpxj.RelationType;
import org.mpxj.Task;
import org.mpxj.TaskMode;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Zeichnet den Diagrammteil der Gantt-Ansicht: arbeitsfreie Zeit, Balken, Meilensteine, Sammelvorgänge,
 * Verknüpfungspfeile und Balkentexte. Dieselbe Klasse wird für Bildschirm, Druck, PDF und PNG verwendet.
 */
public final class GanttPainter
{
   private static final Stroke THIN = new BasicStroke(1f);
   private static final Stroke DASHED = new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]
   {
      4f,
      3f
   }, 0f);
   private static final Stroke SPLIT = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]
   {
      2f,
      2f
   }, 0f);

   private final ProjectDocument doc;
   private final TaskOutline outline;
   private final Timeline timeline;
   private final GanttOptions options;
   private final Formats fmt;

   public GanttPainter(ProjectDocument doc, TaskOutline outline, Timeline timeline, GanttOptions options, Formats fmt)
   {
      this.doc = doc;
      this.outline = outline;
      this.timeline = timeline;
      this.options = options;
      this.fmt = fmt;
   }

   /**
    * Zeichnet die Zeilen {@code firstRow..lastRow} im x-Bereich {@code x0..x1}. Zeile i liegt bei
    * y = i * {@link Style#ROW_HEIGHT}.
    *
    * @param selected markiert ausgewählte Zeilen (nur am Bildschirm), darf {@code null} sein
    */
   public void paint(Graphics2D g, double x0, double x1, int firstRow, int lastRow, IntPredicate selected)
   {
      List<TaskRow> rows = outline.getRows();
      int rh = Style.ROW_HEIGHT;
      lastRow = Math.min(lastRow, rows.size() - 1);
      double y0 = firstRow * (double) rh;
      double y1 = Math.max(y0, (lastRow + 1) * (double) rh);

      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

      g.setColor(Style.CHART_BG);
      g.fill(new Rectangle2D.Double(x0, y0, x1 - x0, y1 - y0));

      if (options.nonWorking)
      {
         paintNonWorking(g, x0, x1, y0, y1);
      }

      if (selected != null)
      {
         g.setColor(new Color(0xEEF5ED));
         for (int i = Math.max(0, firstRow); i <= lastRow; i++)
         {
            if (selected.test(i))
            {
               g.fill(new Rectangle2D.Double(x0, i * (double) rh, x1 - x0, rh));
            }
         }
      }

      if (options.currentDate)
      {
         double x = timeline.x(LocalDateTime.now());
         if (x >= x0 && x <= x1)
         {
            g.setColor(Style.CURRENT_DATE);
            g.setStroke(DASHED);
            g.draw(new Line2D.Double(x, y0, x, y1));
            g.setStroke(THIN);
         }
      }

      if (options.links)
      {
         paintLinks(g, rows, x0, x1, firstRow, lastRow);
      }

      for (int i = Math.max(0, firstRow); i <= lastRow; i++)
      {
         paintRow(g, rows.get(i), i);
      }
   }

   /**
    * Fläche unterhalb der letzten Zeile: wie in Project laufen arbeitsfreie Zeit und Heute-Linie bis zum unteren
    * Rand weiter.
    */
   public void paintEmptyArea(Graphics2D g, double x0, double x1, double y0, double y1)
   {
      if (y1 <= y0)
      {
         return;
      }
      g.setColor(Style.CHART_BG);
      g.fill(new Rectangle2D.Double(x0, y0, x1 - x0, y1 - y0));
      if (options.nonWorking)
      {
         paintNonWorking(g, x0, x1, y0, y1);
      }
      if (options.currentDate)
      {
         double x = timeline.x(LocalDateTime.now());
         if (x >= x0 && x <= x1)
         {
            g.setColor(Style.CURRENT_DATE);
            g.setStroke(DASHED);
            g.draw(new Line2D.Double(x, y0, x, y1));
            g.setStroke(THIN);
         }
      }
   }

   // ------------------------------------------------------------------ arbeitsfreie Zeit

   private void paintNonWorking(Graphics2D g, double x0, double x1, double y0, double y1)
   {
      double ppd = timeline.getPxPerDay();
      if (ppd < 3.5)
      {
         return;
      }
      g.setColor(Style.NONWORKING);
      LocalDate day = timeline.time(x0).toLocalDate();
      LocalDate last = timeline.time(x1).toLocalDate().plusDays(1);
      LocalDate runStart = null;
      while (!day.isAfter(last))
      {
         boolean working = doc.isWorkingDate(day);
         if (!working && runStart == null)
         {
            runStart = day;
         }
         if (working && runStart != null)
         {
            fillDays(g, runStart, day, y0, y1);
            runStart = null;
         }
         day = day.plusDays(1);
      }
      if (runStart != null)
      {
         fillDays(g, runStart, day, y0, y1);
      }
   }

   private void fillDays(Graphics2D g, LocalDate from, LocalDate toExclusive, double y0, double y1)
   {
      double xs = timeline.x(from.atStartOfDay());
      double xe = timeline.x(toExclusive.atStartOfDay());
      g.fill(new Rectangle2D.Double(xs, y0, xe - xs, y1 - y0));
   }

   // ------------------------------------------------------------------ Balken

   private enum Shape
   {
      NONE,
      TASK,
      MILESTONE,
      SUMMARY,
      PROJECT_SUMMARY
   }

   /**
    * Geometrie eines Balkens in Zeitleisten-Koordinaten.
    */
   private record Bar(Shape shape, double xs, double xe, double top, double bottom)
   {
      double mid()
      {
         return (top + bottom) / 2.0;
      }
   }

   private Bar bar(TaskRow row, int index)
   {
      Task task = row.task();
      double rowTop = index * (double) Style.ROW_HEIGHT;
      if (task.getNull())
      {
         return new Bar(Shape.NONE, 0, 0, rowTop, rowTop);
      }
      LocalDateTime start = task.getStart();
      LocalDateTime finish = task.getFinish();
      if (start == null && finish == null)
      {
         return new Bar(Shape.NONE, 0, 0, rowTop, rowTop);
      }
      if (start == null)
      {
         start = finish;
      }
      if (finish == null || finish.isBefore(start))
      {
         finish = start;
      }
      if (task.getHideBar())
      {
         return new Bar(Shape.NONE, timeline.x(start), timeline.x(finish), rowTop, rowTop);
      }

      Shape shape;
      if (row.isProjectSummary())
      {
         shape = Shape.PROJECT_SUMMARY;
      }
      else if (task.getSummary() && row.hasChildren())
      {
         shape = Shape.SUMMARY;
      }
      else if (task.getMilestone())
      {
         shape = Shape.MILESTONE;
      }
      else
      {
         shape = Shape.TASK;
      }

      if (options.roundBarsToWholeDays && shape != Shape.MILESTONE)
      {
         start = start.toLocalDate().atStartOfDay();
         LocalDateTime f = finish.toLocalDate().atStartOfDay();
         finish = f.equals(finish) ? f : f.plusDays(1);
      }

      double xs = timeline.x(start);
      double xe = timeline.x(finish);
      int rh = Style.ROW_HEIGHT;
      return switch (shape)
      {
         case MILESTONE -> new Bar(shape, xs, xs, rowTop + 4, rowTop + rh - 4);
         case SUMMARY, PROJECT_SUMMARY -> new Bar(shape, xs, Math.max(xe, xs + 1), rowTop + 6, rowTop + 11);
         case TASK ->
         {
            if (options.showBaseline())
            {
               yield new Bar(shape, xs, Math.max(xe, xs + 2), rowTop + 3, rowTop + 12);
            }
            yield new Bar(shape, xs, Math.max(xe, xs + 2), rowTop + 5, rowTop + 15);
         }
         default -> new Bar(Shape.NONE, xs, xe, rowTop, rowTop);
      };
   }

   private void paintRow(Graphics2D g, TaskRow row, int index)
   {
      Task task = row.task();
      Bar bar = bar(row, index);
      double rowTop = index * (double) Style.ROW_HEIGHT;

      if (options.showBaseline() && !task.getNull())
      {
         paintBaseline(g, task, rowTop);
      }

      switch (bar.shape())
      {
         case TASK -> paintTask(g, task, bar);
         case MILESTONE -> paintMilestone(g, bar, task.getActive() ? Style.MILESTONE : Style.INACTIVE, isCritical(task) ? Style.CRITICAL_PROGRESS : null);
         case SUMMARY -> paintSummary(g, bar, Style.SUMMARY);
         case PROJECT_SUMMARY -> paintSummary(g, bar, Style.PROJECT_SUMMARY);
         default ->
         {
         }
      }

      if (task.getDeadline() != null && !task.getNull())
      {
         paintDeadline(g, timeline.x(task.getDeadline()), rowTop);
      }

      if (options.barText && bar.shape() != Shape.NONE)
      {
         paintBarText(g, task, bar);
      }
   }

   private boolean isCritical(Task task)
   {
      return options.showCritical() && task.getCritical() && !task.getSummary();
   }

   private void paintTask(Graphics2D g, Task task, Bar bar)
   {
      boolean manual = task.getTaskMode() == TaskMode.MANUALLY_SCHEDULED;
      boolean critical = isCritical(task);
      boolean active = task.getActive();
      Color fill = critical ? Style.CRITICAL_FILL : manual ? Style.MANUAL_FILL : Style.TASK_FILL;
      Color border = critical ? Style.CRITICAL_BORDER : manual ? Style.MANUAL_BORDER : Style.TASK_BORDER;
      Color progress = critical ? Style.CRITICAL_PROGRESS : Style.TASK_PROGRESS;
      if (!active)
      {
         fill = Color.WHITE;
         border = Style.INACTIVE;
         progress = Style.INACTIVE;
      }

      double top = bar.top();
      double h = bar.bottom() - bar.top();
      List<double[]> segments = segments(task, bar);

      // Unterbrechungen als gepunktete Linie zwischen den Teilstücken
      if (segments.size() > 1)
      {
         g.setColor(border);
         g.setStroke(SPLIT);
         double y = top + h / 2.0;
         for (int i = 1; i < segments.size(); i++)
         {
            g.draw(new Line2D.Double(segments.get(i - 1)[1], y, segments.get(i)[0], y));
         }
         g.setStroke(THIN);
      }

      double pct = task.getPercentageComplete() == null ? 0 : task.getPercentageComplete().doubleValue();
      double progressEnd = bar.xs() + (bar.xe() - bar.xs()) * Math.max(0, Math.min(100, pct)) / 100.0;

      for (double[] seg : segments)
      {
         Rectangle2D r = new Rectangle2D.Double(seg[0], top, Math.max(1, seg[1] - seg[0]), h);
         g.setColor(fill);
         g.fill(r);
         if (pct > 0)
         {
            double pe = Math.min(seg[1], progressEnd);
            if (pe > seg[0])
            {
               g.setColor(progress);
               if (options.tracking)
               {
                  g.fill(new Rectangle2D.Double(seg[0], top, pe - seg[0], h));
               }
               else
               {
                  // Fortschritt als dunkle Fläche in der Balkenmitte
                  g.fill(new Rectangle2D.Double(seg[0], top + h * 0.3, pe - seg[0], h * 0.4));
               }
            }
         }
         g.setColor(border);
         if (manual && active)
         {
            // Manuell geplante Vorgänge: kräftige Enden wie in Project
            g.fill(new Rectangle2D.Double(seg[0], top, 2, h));
            g.fill(new Rectangle2D.Double(seg[1] - 2, top, 2, h));
            g.draw(new Rectangle2D.Double(seg[0] + 0.5, top + 0.5, Math.max(0, seg[1] - seg[0] - 1), h - 1));
         }
         else
         {
            g.draw(new Rectangle2D.Double(seg[0] + 0.5, top + 0.5, Math.max(0, seg[1] - seg[0] - 1), h - 1));
         }
      }
   }

   /**
    * Teilstücke eines unterbrochenen Vorgangs; sonst ein einziges Stück.
    */
   private List<double[]> segments(Task task, Bar bar)
   {
      List<double[]> result = new ArrayList<>();
      List<LocalDateTimeRange> splits = null;
      try
      {
         splits = task.getWorkSplits();
      }
      catch (RuntimeException ex)
      {
         // ignorieren
      }
      if (splits != null && splits.size() > 1 && !options.roundBarsToWholeDays)
      {
         List<double[]> raw = new ArrayList<>();
         for (LocalDateTimeRange range : splits)
         {
            if (range.getStart() == null || range.getEnd() == null)
            {
               continue;
            }
            raw.add(new double[]
            {
               timeline.x(range.getStart()),
               timeline.x(range.getEnd())
            });
         }
         // Nur Lücken von mindestens einem halben Tag als Unterbrechung zeigen (Nächte/Wochenenden zählen nicht)
         double minGap = timeline.getPxPerDay() * 1.6;
         for (double[] seg : raw)
         {
            if (!result.isEmpty())
            {
               double[] prev = result.get(result.size() - 1);
               if (seg[0] - prev[1] < minGap || !isRealGap(prev[1], seg[0]))
               {
                  prev[1] = Math.max(prev[1], seg[1]);
                  continue;
               }
            }
            result.add(new double[]
            {
               seg[0],
               seg[1]
            });
         }
         if (result.size() > 1)
         {
            result.get(0)[0] = bar.xs();
            result.get(result.size() - 1)[1] = bar.xe();
            return result;
         }
         result.clear();
      }
      result.add(new double[]
      {
         bar.xs(),
         bar.xe()
      });
      return result;
   }

   /**
    * Eine Lücke gilt als echte Unterbrechung, wenn darin mindestens ein Arbeitstag liegt.
    */
   private boolean isRealGap(double xFrom, double xTo)
   {
      LocalDate d = timeline.time(xFrom).toLocalDate().plusDays(1);
      LocalDate end = timeline.time(xTo).toLocalDate();
      while (d.isBefore(end))
      {
         if (doc.isWorkingDate(d))
         {
            return true;
         }
         d = d.plusDays(1);
      }
      return false;
   }

   private void paintMilestone(Graphics2D g, Bar bar, Color color, Color critical)
   {
      double cx = bar.xs();
      double cy = bar.mid();
      double r = 6;
      Path2D diamond = new Path2D.Double();
      diamond.moveTo(cx, cy - r);
      diamond.lineTo(cx + r, cy);
      diamond.lineTo(cx, cy + r);
      diamond.lineTo(cx - r, cy);
      diamond.closePath();
      g.setColor(critical != null ? critical : color);
      g.fill(diamond);
   }

   private void paintSummary(Graphics2D g, Bar bar, Color color)
   {
      double xs = bar.xs();
      double xe = bar.xe();
      double top = bar.top();
      double bottom = bar.bottom();
      double cap = Math.min(6, Math.max(2, (xe - xs) / 2));
      GeneralPath p = new GeneralPath();
      p.moveTo(xs, top);
      p.lineTo(xe, top);
      p.lineTo(xe, bottom + 5);
      p.lineTo(xe - cap, bottom);
      p.lineTo(xs + cap, bottom);
      p.lineTo(xs, bottom + 5);
      p.closePath();
      g.setColor(color);
      g.fill(p);
   }

   private void paintBaseline(Graphics2D g, Task task, double rowTop)
   {
      LocalDateTime bs = task.getBaselineStart();
      LocalDateTime bf = task.getBaselineFinish();
      if (bs == null || bf == null)
      {
         return;
      }
      double xs = timeline.x(bs);
      double xe = timeline.x(bf);
      g.setColor(Style.BASELINE);
      if (task.getMilestone() && !task.getSummary())
      {
         double cy = rowTop + 15;
         double r = 4;
         Path2D diamond = new Path2D.Double();
         diamond.moveTo(xs, cy - r);
         diamond.lineTo(xs + r, cy);
         diamond.lineTo(xs, cy + r);
         diamond.lineTo(xs - r, cy);
         diamond.closePath();
         g.fill(diamond);
         return;
      }
      g.fill(new Rectangle2D.Double(xs, rowTop + 13, Math.max(2, xe - xs), 4));
   }

   private void paintDeadline(Graphics2D g, double x, double rowTop)
   {
      Path2D arrow = new Path2D.Double();
      double top = rowTop + 2;
      arrow.moveTo(x - 1.5, top);
      arrow.lineTo(x + 1.5, top);
      arrow.lineTo(x + 1.5, top + 6);
      arrow.lineTo(x + 4.5, top + 6);
      arrow.lineTo(x, top + 11);
      arrow.lineTo(x - 4.5, top + 6);
      arrow.lineTo(x - 1.5, top + 6);
      arrow.closePath();
      g.setColor(Style.DEADLINE);
      g.fill(arrow);
   }

   private void paintBarText(Graphics2D g, Task task, Bar bar)
   {
      String text;
      switch (bar.shape())
      {
         case MILESTONE:
            text = fmt.dateShort(task.getStart());
            break;
         case SUMMARY:
         case PROJECT_SUMMARY:
            text = null;
            break;
         default:
            text = options.tracking ? fmt.percent(task.getPercentageComplete()) : fmt.resourceNames(task);
            break;
      }
      if (text == null || text.isEmpty())
      {
         return;
      }
      g.setFont(Style.BAR_FONT);
      FontMetrics fm = g.getFontMetrics();
      double x = bar.shape() == Shape.MILESTONE ? bar.xs() + 10 : bar.xe() + 6;
      double rowTop = Math.floor(bar.top() / Style.ROW_HEIGHT) * Style.ROW_HEIGHT;
      float y = (float) (rowTop + (Style.ROW_HEIGHT + fm.getAscent() - fm.getDescent()) / 2.0);
      g.setColor(Style.BAR_TEXT);
      g.drawString(text, (float) x, y);
   }

   // ------------------------------------------------------------------ Verknüpfungen

   private void paintLinks(Graphics2D g, List<TaskRow> rows, double x0, double x1, int firstRow, int lastRow)
   {
      g.setStroke(THIN);
      double yMin = firstRow * (double) Style.ROW_HEIGHT;
      double yMax = (lastRow + 1) * (double) Style.ROW_HEIGHT;
      for (int i = 0; i < rows.size(); i++)
      {
         TaskRow row = rows.get(i);
         List<Relation> successors = row.task().getSuccessors();
         if (successors == null || successors.isEmpty())
         {
            continue;
         }
         Bar from = null;
         for (Relation rel : successors)
         {
            Task succ = rel.getSuccessorTask();
            int j = outline.indexOf(succ);
            if (j < 0 || j == i)
            {
               continue;
            }
            // Nur zeichnen, wenn die Verbindung den sichtbaren Bereich berührt
            double ya = Math.min(i, j) * (double) Style.ROW_HEIGHT;
            double yb = (Math.max(i, j) + 1) * (double) Style.ROW_HEIGHT;
            if (yb < yMin || ya > yMax)
            {
               continue;
            }
            if (from == null)
            {
               from = bar(row, i);
            }
            Bar to = bar(rows.get(j), j);
            if (from.shape() == Shape.NONE || to.shape() == Shape.NONE)
            {
               continue;
            }
            double minX = Math.min(Math.min(from.xs(), from.xe()), Math.min(to.xs(), to.xe())) - 20;
            double maxX = Math.max(Math.max(from.xs(), from.xe()), Math.max(to.xs(), to.xe())) + 20;
            if (maxX < x0 || minX > x1)
            {
               continue;
            }
            boolean critical = options.showCritical() && row.task().getCritical() && succ.getCritical();
            g.setColor(critical ? Style.LINK_CRITICAL : Style.LINK);
            paintLink(g, rel.getType() == null ? RelationType.FINISH_START : rel.getType(), from, to, i, j);
         }
      }
   }

   private static double startX(Bar b)
   {
      return b.shape() == Shape.MILESTONE ? b.xs() - 6 : b.xs();
   }

   private static double endX(Bar b)
   {
      return b.shape() == Shape.MILESTONE ? b.xs() + 6 : b.xe();
   }

   private void paintLink(Graphics2D g, RelationType type, Bar from, Bar to, int fromRow, int toRow)
   {
      boolean down = toRow > fromRow;
      double y1 = from.mid();
      double y2 = to.mid();
      double boundary = down ? (fromRow + 1) * (double) Style.ROW_HEIGHT - 0.5 : fromRow * (double) Style.ROW_HEIGHT + 0.5;
      Path2D path = new Path2D.Double();
      switch (type)
      {
         case FINISH_START ->
         {
            double xa = endX(from);
            double xb = to.shape() == Shape.MILESTONE ? to.xs() : startX(to) + 3;
            if (xb >= xa + 3)
            {
               // Waagerecht bis über den Nachfolger, dann senkrecht in den Balken
               double yEnd = down ? to.top() : to.bottom();
               path.moveTo(xa, y1);
               path.lineTo(xb, y1);
               path.lineTo(xb, yEnd);
               g.draw(path);
               arrow(g, xb, yEnd, down ? Direction.DOWN : Direction.UP);
            }
            else
            {
               double xr = xa + 6;
               double xl = startX(to) - 8;
               path.moveTo(xa, y1);
               path.lineTo(xr, y1);
               path.lineTo(xr, boundary);
               path.lineTo(xl, boundary);
               path.lineTo(xl, y2);
               path.lineTo(startX(to), y2);
               g.draw(path);
               arrow(g, startX(to), y2, Direction.RIGHT);
            }
         }
         case START_START ->
         {
            double xa = startX(from);
            double xl = Math.min(xa, startX(to)) - 8;
            path.moveTo(xa, y1);
            path.lineTo(xl, y1);
            path.lineTo(xl, y2);
            path.lineTo(startX(to), y2);
            g.draw(path);
            arrow(g, startX(to), y2, Direction.RIGHT);
         }
         case FINISH_FINISH ->
         {
            double xa = endX(from);
            double xr = Math.max(xa, endX(to)) + 8;
            path.moveTo(xa, y1);
            path.lineTo(xr, y1);
            path.lineTo(xr, y2);
            path.lineTo(endX(to), y2);
            g.draw(path);
            arrow(g, endX(to), y2, Direction.LEFT);
         }
         case START_FINISH ->
         {
            double xa = startX(from);
            double xl = xa - 8;
            double xr = endX(to) + 8;
            path.moveTo(xa, y1);
            path.lineTo(xl, y1);
            path.lineTo(xl, boundary);
            path.lineTo(xr, boundary);
            path.lineTo(xr, y2);
            path.lineTo(endX(to), y2);
            g.draw(path);
            arrow(g, endX(to), y2, Direction.LEFT);
         }
      }
   }

   private enum Direction
   {
      UP,
      DOWN,
      LEFT,
      RIGHT
   }

   private static void arrow(Graphics2D g, double x, double y, Direction dir)
   {
      double s = 4;
      Path2D p = new Path2D.Double();
      switch (dir)
      {
         case DOWN ->
         {
            p.moveTo(x - s, y - s - 1);
            p.lineTo(x + s, y - s - 1);
            p.lineTo(x, y);
         }
         case UP ->
         {
            p.moveTo(x - s, y + s + 1);
            p.lineTo(x + s, y + s + 1);
            p.lineTo(x, y);
         }
         case RIGHT ->
         {
            p.moveTo(x - s - 1, y - s);
            p.lineTo(x - s - 1, y + s);
            p.lineTo(x, y);
         }
         case LEFT ->
         {
            p.moveTo(x + s + 1, y - s);
            p.lineTo(x + s + 1, y + s);
            p.lineTo(x, y);
         }
      }
      p.closePath();
      g.fill(p);
   }

   /**
    * x-Position des Vorgangsbeginns für „Bildlauf zu Vorgang“.
    */
   public double startXOf(Task task)
   {
      LocalDateTime start = task.getStart();
      return start == null ? -1 : timeline.x(start);
   }
}
