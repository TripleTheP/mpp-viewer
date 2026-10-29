package io.github.triplethep.mppviewer.render;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Einheiten der Zeitskala und das Runden auf deren Beginn.
 */
public enum TimeUnitScale
{
   HOURS6,
   DAYS,
   WEEKS,
   MONTHS,
   QUARTERS,
   YEARS;

   public LocalDateTime floor(LocalDateTime t, DayOfWeek weekStart)
   {
      return switch (this)
      {
         case HOURS6 -> t.toLocalDate().atStartOfDay().plusHours((t.getHour() / 6) * 6L);
         case DAYS -> t.toLocalDate().atStartOfDay();
         case WEEKS -> t.toLocalDate().with(TemporalAdjusters.previousOrSame(weekStart == null ? DayOfWeek.MONDAY : weekStart)).atStartOfDay();
         case MONTHS -> t.toLocalDate().withDayOfMonth(1).atStartOfDay();
         case QUARTERS -> t.toLocalDate().withDayOfMonth(1).withMonth(((t.getMonthValue() - 1) / 3) * 3 + 1).atStartOfDay();
         case YEARS -> t.toLocalDate().withDayOfYear(1).atStartOfDay();
      };
   }

   public LocalDateTime next(LocalDateTime t)
   {
      return switch (this)
      {
         case HOURS6 -> t.plusHours(6);
         case DAYS -> t.plusDays(1);
         case WEEKS -> t.plusWeeks(1);
         case MONTHS -> t.plusMonths(1);
         case QUARTERS -> t.plusMonths(3);
         case YEARS -> t.plusYears(1);
      };
   }

   /**
    * Ungefähre Länge in Tagen.
    */
   public double approxDays()
   {
      return switch (this)
      {
         case HOURS6 -> 0.25;
         case DAYS -> 1;
         case WEEKS -> 7;
         case MONTHS -> 30.4;
         case QUARTERS -> 91.3;
         case YEARS -> 365.25;
      };
   }
}
