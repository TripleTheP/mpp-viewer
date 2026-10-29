package io.github.triplethep.mppviewer.render;

import io.github.triplethep.mppviewer.core.Formats;
import io.github.triplethep.mppviewer.i18n.Language;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Zoomstufen der Zeitskala mit oberer und unterer Ebene – angelehnt an die Voreinstellungen von Project.
 * Die Standardstufe (21 px pro Tag, Wochen oben, Tage unten) entspricht der Project-Standardansicht.
 */
public enum ZoomLevel
{
   YEARS_QUARTERS_S(0.35, tier(TimeUnitScale.YEARS, Label.YEAR), tier(TimeUnitScale.QUARTERS, Label.QUARTER)),
   YEARS_QUARTERS(0.6, tier(TimeUnitScale.YEARS, Label.YEAR), tier(TimeUnitScale.QUARTERS, Label.QUARTER)),
   QUARTERS_MONTHS_S(1.0, tier(TimeUnitScale.QUARTERS, Label.QUARTER_YEAR), tier(TimeUnitScale.MONTHS, Label.MONTH_SHORT, Label.MONTH_LETTER)),
   QUARTERS_MONTHS(1.6, tier(TimeUnitScale.QUARTERS, Label.QUARTER_YEAR), tier(TimeUnitScale.MONTHS, Label.MONTH_SHORT, Label.MONTH_LETTER)),
   MONTHS_WEEKS_S(2.6, tier(TimeUnitScale.MONTHS, Label.MONTH_YEAR, Label.MONTH_SHORT_YEAR), tier(TimeUnitScale.WEEKS, Label.WEEK_DAYNUM)),
   MONTHS_WEEKS_M(4.0, tier(TimeUnitScale.MONTHS, Label.MONTH_YEAR, Label.MONTH_SHORT_YEAR), tier(TimeUnitScale.WEEKS, Label.WEEK_SHORT, Label.WEEK_DAYNUM)),
   MONTHS_WEEKS(6.0, tier(TimeUnitScale.MONTHS, Label.MONTH_YEAR, Label.MONTH_SHORT_YEAR), tier(TimeUnitScale.WEEKS, Label.WEEK_SHORT, Label.WEEK_DAYNUM)),
   WEEKS_DAYS_S(9.0, tier(TimeUnitScale.WEEKS, Label.WEEK_LONG, Label.WEEK_SHORT), tier(TimeUnitScale.DAYS, Label.DAY_LETTER)),
   WEEKS_DAYS_M(14.0, tier(TimeUnitScale.WEEKS, Label.WEEK_LONG, Label.WEEK_SHORT), tier(TimeUnitScale.DAYS, Label.DAY_LETTER)),
   WEEKS_DAYS(21.0, tier(TimeUnitScale.WEEKS, Label.WEEK_LONG, Label.WEEK_SHORT), tier(TimeUnitScale.DAYS, Label.DAY_LETTER)),
   WEEKS_DAYS_L(32.0, tier(TimeUnitScale.WEEKS, Label.WEEK_LONG), tier(TimeUnitScale.DAYS, Label.DAY_SHORT, Label.DAY_NUM, Label.DAY_LETTER)),
   WEEKS_DAYS_XL(48.0, tier(TimeUnitScale.WEEKS, Label.WEEK_LONG), tier(TimeUnitScale.DAYS, Label.DAY_SHORT, Label.DAY_NUM)),
   DAYS_HOURS_S(80.0, tier(TimeUnitScale.DAYS, Label.DAY_LONG, Label.DAY_SHORT), tier(TimeUnitScale.HOURS6, Label.HOUR)),
   DAYS_HOURS(130.0, tier(TimeUnitScale.DAYS, Label.DAY_LONG), tier(TimeUnitScale.HOURS6, Label.HOUR)),
   DAYS_HOURS_L(200.0, tier(TimeUnitScale.DAYS, Label.DAY_LONG), tier(TimeUnitScale.HOURS6, Label.HOUR));

   public static final ZoomLevel DEFAULT = WEEKS_DAYS;

   private final double pxPerDay;
   private final Tier top;
   private final Tier bottom;

   ZoomLevel(double pxPerDay, Tier top, Tier bottom)
   {
      this.pxPerDay = pxPerDay;
      this.top = top;
      this.bottom = bottom;
   }

   public double pxPerDay()
   {
      return pxPerDay;
   }

   public Tier top()
   {
      return top;
   }

   public Tier bottom()
   {
      return bottom;
   }

   /**
    * Größte Stufe, deren Maßstab nicht größer als der angegebene ist (bestimmt die Ebenen bei freiem Zoom).
    */
   public static ZoomLevel forPxPerDay(double ppd)
   {
      ZoomLevel result = values()[0];
      for (ZoomLevel level : values())
      {
         if (level.pxPerDay <= ppd + 1e-9)
         {
            result = level;
         }
      }
      return result;
   }

   /**
    * Nächstgelegene Stufe (für den Zoomregler).
    */
   public static ZoomLevel nearest(double ppd)
   {
      ZoomLevel best = DEFAULT;
      double bestDiff = Double.MAX_VALUE;
      for (ZoomLevel level : values())
      {
         double diff = Math.abs(Math.log(level.pxPerDay) - Math.log(ppd));
         if (diff < bestDiff)
         {
            bestDiff = diff;
            best = level;
         }
      }
      return best;
   }

   public ZoomLevel zoomIn()
   {
      return values()[Math.min(values().length - 1, ordinal() + 1)];
   }

   public ZoomLevel zoomOut()
   {
      return values()[Math.max(0, ordinal() - 1)];
   }

   private static Tier tier(TimeUnitScale unit, Label... labels)
   {
      return new Tier(unit, List.of(labels));
   }

   /**
    * Eine Ebene der Zeitskala: Einheit und Beschriftungsformate (das erste passende wird verwendet).
    */
   public record Tier(TimeUnitScale unit, List<Label> labels)
   {
   }

   /**
    * Beschriftungsformate der Zeitskala.
    */
   public enum Label
   {
      YEAR,
      QUARTER_YEAR,
      QUARTER,
      MONTH_YEAR,
      MONTH_SHORT_YEAR,
      MONTH_SHORT,
      MONTH_LETTER,
      WEEK_LONG,
      WEEK_SHORT,
      WEEK_DAYNUM,
      DAY_LONG,
      DAY_SHORT,
      DAY_NUM,
      DAY_LETTER,
      HOUR;

      public String format(LocalDateTime t, Formats fmt)
      {
         boolean de = fmt.getLanguage() == Language.DE;
         int year = t.getYear();
         String yy = String.format("%02d", year % 100);
         int month = t.getMonthValue();
         int day = t.getDayOfMonth();
         return switch (this)
         {
            case YEAR -> Integer.toString(year);
            case QUARTER_YEAR -> (de ? "Q" : "Q") + ((month - 1) / 3 + 1) + " " + year;
            case QUARTER -> "Q" + ((month - 1) / 3 + 1);
            case MONTH_YEAR -> fmt.monthLong(month) + " " + year;
            case MONTH_SHORT_YEAR -> fmt.monthShort(month) + " '" + yy;
            case MONTH_SHORT -> fmt.monthShort(month);
            case MONTH_LETTER -> fmt.monthShort(month).substring(0, 1);
            case WEEK_LONG -> de ? day + ". " + fmt.monthShort(month) + " '" + yy : fmt.monthShort(month) + " " + day + ", '" + yy;
            case WEEK_SHORT -> de ? String.format("%02d.%02d.", day, month) : month + "/" + day;
            case WEEK_DAYNUM, DAY_NUM -> Integer.toString(day);
            case DAY_LONG -> fmt.dayShort(t.toLocalDate()) + " " + (de ? String.format("%02d.%02d.%s", day, month, yy) : month + "/" + day + "/" + yy);
            case DAY_SHORT -> de ? fmt.dayShort(t.toLocalDate()) + " " + day + "." : fmt.dayShort(t.toLocalDate()) + " " + day;
            case DAY_LETTER -> fmt.dayLetter(t.toLocalDate());
            case HOUR -> de ? Integer.toString(t.getHour()) : hour12(t.getHour());
         };
      }

      private static String hour12(int hour)
      {
         if (hour == 0)
         {
            return "12 AM";
         }
         if (hour == 12)
         {
            return "12 PM";
         }
         return hour < 12 ? hour + " AM" : (hour - 12) + " PM";
      }
   }
}
