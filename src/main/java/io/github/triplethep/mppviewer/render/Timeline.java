package io.github.triplethep.mppviewer.render;

import io.github.triplethep.mppviewer.core.ProjectDocument;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Abbildung von Zeitpunkten auf x-Koordinaten im Gantt-Diagramm.
 */
public final class Timeline
{
   private final LocalDateTime origin;
   private final LocalDateTime end;
   private final double pxPerDay;
   private final DayOfWeek weekStart;

   public Timeline(LocalDateTime origin, LocalDateTime end, double pxPerDay, DayOfWeek weekStart)
   {
      this.origin = origin;
      this.end = end.isAfter(origin) ? end : origin.plusDays(1);
      this.pxPerDay = pxPerDay;
      this.weekStart = weekStart == null ? DayOfWeek.MONDAY : weekStart;
   }

   /**
    * Zeitleiste über das ganze Projekt mit etwas Luft links und rechts – wie in Project lässt sich darüber
    * hinaus scrollen.
    */
   public static Timeline forProject(ProjectDocument doc, double pxPerDay)
   {
      DayOfWeek weekStart = doc.getWeekStartDay();
      LocalDateTime start = doc.getStart().toLocalDate().atStartOfDay();
      LocalDateTime finish = doc.getFinish().toLocalDate().atStartOfDay().plusDays(1);
      long days = Math.max(1, ChronoUnit.DAYS.between(start, finish));
      long padding = Math.max(14, Math.min(400, days / 2));
      LocalDateTime origin = alignStart(start.minusDays(padding), pxPerDay, weekStart);
      LocalDateTime end = finish.plusDays(padding);
      return new Timeline(origin, end, pxPerDay, weekStart);
   }

   /**
    * Zeitleiste genau über einen Zeitraum (für Druck und Export).
    */
   public static Timeline forRange(LocalDateTime from, LocalDateTime to, double pxPerDay, DayOfWeek weekStart)
   {
      LocalDateTime origin = alignStart(from, pxPerDay, weekStart);
      return new Timeline(origin, to, pxPerDay, weekStart);
   }

   /**
    * Linken Rand auf den Beginn einer Einheit der unteren Zeitskalenebene legen, damit die erste Spalte vollständig
    * ist.
    */
   private static LocalDateTime alignStart(LocalDateTime t, double pxPerDay, DayOfWeek weekStart)
   {
      ZoomLevel level = ZoomLevel.forPxPerDay(pxPerDay);
      TimeUnitScale top = level.top().unit();
      LocalDateTime day = t.toLocalDate().atStartOfDay();
      return switch (top)
      {
         case WEEKS -> day.with(TemporalAdjusters.previousOrSame(weekStart == null ? DayOfWeek.MONDAY : weekStart));
         default -> top.floor(day, weekStart);
      };
   }

   public LocalDateTime getOrigin()
   {
      return origin;
   }

   public LocalDateTime getEnd()
   {
      return end;
   }

   public double getPxPerDay()
   {
      return pxPerDay;
   }

   public DayOfWeek getWeekStart()
   {
      return weekStart;
   }

   public ZoomLevel getLevel()
   {
      return ZoomLevel.forPxPerDay(pxPerDay);
   }

   public double x(LocalDateTime t)
   {
      return ChronoUnit.MINUTES.between(origin, t) / 1440.0 * pxPerDay;
   }

   public LocalDateTime time(double x)
   {
      long minutes = Math.round(x / pxPerDay * 1440.0);
      return origin.plusMinutes(minutes);
   }

   public int getWidth()
   {
      return (int) Math.ceil(x(end));
   }

   public Timeline withPxPerDay(double ppd)
   {
      return new Timeline(origin, end, ppd, weekStart);
   }

   /**
    * Gleiche Skalierung, aber anderer Zeitraum.
    */
   public Timeline withRange(LocalDateTime from, LocalDateTime to)
   {
      return forRange(from, to, pxPerDay, weekStart);
   }
}
