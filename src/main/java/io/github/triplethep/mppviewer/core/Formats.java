package io.github.triplethep.mppviewer.core;

import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.i18n.Language;
import org.mpxj.AccrueType;
import org.mpxj.ConstraintType;
import org.mpxj.CurrencySymbolPosition;
import org.mpxj.Duration;
import org.mpxj.Priority;
import org.mpxj.ProjectProperties;
import org.mpxj.Rate;
import org.mpxj.Relation;
import org.mpxj.RelationType;
import org.mpxj.Resource;
import org.mpxj.ResourceAssignment;
import org.mpxj.ResourceType;
import org.mpxj.Task;
import org.mpxj.TaskMode;
import org.mpxj.TaskType;
import org.mpxj.TimeUnit;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Formatiert Werte so, wie Microsoft Project sie in der jeweiligen Sprache anzeigt
 * (z. B. „Mo 26.10.26“, „5 Tage“, „3EA+2 Tage“).
 */
public final class Formats
{
   private static final String[] DAY_SHORT_DE = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
   private static final String[] DAY_SHORT_EN = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
   private static final String[] DAY_LETTER_DE = {"M", "D", "M", "D", "F", "S", "S"};
   private static final String[] DAY_LETTER_EN = {"M", "T", "W", "T", "F", "S", "S"};
   private static final String[] MONTH_SHORT_DE = {"Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"};
   private static final String[] MONTH_SHORT_EN = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
   private static final String[] MONTH_LONG_DE = {"Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember"};
   private static final String[] MONTH_LONG_EN = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

   private final Language language;
   private final String currencySymbol;
   private final CurrencySymbolPosition currencyPosition;
   private final int currencyDigits;
   private final DecimalFormat numberFormat;
   private final DecimalFormat currencyNumberFormat;
   private final DateTimeFormatter dateFormat;
   private final DateTimeFormatter dateTimeFormat;
   private final DateTimeFormatter timeFormat;

   public Formats(Language language, ProjectProperties props)
   {
      this.language = language;
      String symbol = props == null ? null : props.getCurrencySymbol();
      CurrencySymbolPosition position = props == null ? null : props.getSymbolPosition();
      Integer digits = props == null ? null : props.getCurrencyDigits();
      if (symbol == null)
      {
         symbol = language == Language.DE ? "€" : "$";
      }
      if (position == null)
      {
         position = language == Language.DE ? CurrencySymbolPosition.AFTER_WITH_SPACE : CurrencySymbolPosition.BEFORE;
      }
      this.currencySymbol = symbol;
      this.currencyPosition = position;
      this.currencyDigits = digits == null ? 2 : Math.max(0, Math.min(4, digits.intValue()));

      DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(language.locale());
      this.numberFormat = new DecimalFormat("#,##0.##", symbols);
      StringBuilder pattern = new StringBuilder("#,##0");
      if (currencyDigits > 0)
      {
         pattern.append('.');
         pattern.append("0".repeat(currencyDigits));
      }
      this.currencyNumberFormat = new DecimalFormat(pattern.toString(), symbols);

      if (language == Language.DE)
      {
         dateFormat = DateTimeFormatter.ofPattern("dd.MM.yy");
         dateTimeFormat = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
         timeFormat = DateTimeFormatter.ofPattern("HH:mm");
      }
      else
      {
         dateFormat = DateTimeFormatter.ofPattern("M/d/yy");
         dateTimeFormat = DateTimeFormatter.ofPattern("M/d/yy h:mm a", Locale.US);
         timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
      }
   }

   public static Formats current(ProjectProperties props)
   {
      return new Formats(I18n.language(), props);
   }

   public Language getLanguage()
   {
      return language;
   }

   // ---------------------------------------------------------------- Datum

   /**
    * Standard-Datumsformat von Project: „Mo 26.10.26“ bzw. „Mon 10/26/26“.
    */
   public String date(LocalDateTime value)
   {
      if (value == null)
      {
         return na();
      }
      return dayShort(value.toLocalDate()) + " " + dateFormat.format(value);
   }

   public String date(LocalDate value)
   {
      return value == null ? na() : date(value.atStartOfDay());
   }

   public String dateTime(LocalDateTime value)
   {
      if (value == null)
      {
         return na();
      }
      return dayShort(value.toLocalDate()) + " " + dateTimeFormat.format(value);
   }

   public String time(LocalTime value)
   {
      return value == null ? "" : timeFormat.format(value);
   }

   /**
    * Kurzes Datum für Balkentexte: „26.10.“ bzw. „10/26“.
    */
   public String dateShort(LocalDateTime value)
   {
      if (value == null)
      {
         return "";
      }
      if (language == Language.DE)
      {
         return String.format("%02d.%02d.", value.getDayOfMonth(), value.getMonthValue());
      }
      return value.getMonthValue() + "/" + value.getDayOfMonth();
   }

   public String na()
   {
      return language == Language.DE ? "NV" : "NA";
   }

   public String dayShort(LocalDate date)
   {
      int i = date.getDayOfWeek().getValue() - 1;
      return language == Language.DE ? DAY_SHORT_DE[i] : DAY_SHORT_EN[i];
   }

   public String dayLetter(LocalDate date)
   {
      int i = date.getDayOfWeek().getValue() - 1;
      return language == Language.DE ? DAY_LETTER_DE[i] : DAY_LETTER_EN[i];
   }

   public String monthShort(int month)
   {
      return language == Language.DE ? MONTH_SHORT_DE[month - 1] : MONTH_SHORT_EN[month - 1];
   }

   public String monthLong(int month)
   {
      return language == Language.DE ? MONTH_LONG_DE[month - 1] : MONTH_LONG_EN[month - 1];
   }

   // ---------------------------------------------------------------- Zahlen

   public String number(Number value)
   {
      if (value == null)
      {
         return "";
      }
      synchronized (numberFormat)
      {
         return numberFormat.format(value.doubleValue());
      }
   }

   public String number(double value)
   {
      synchronized (numberFormat)
      {
         return numberFormat.format(value);
      }
   }

   public String percent(Number value)
   {
      if (value == null)
      {
         return "0%";
      }
      return number(Math.round(value.doubleValue() * 100.0) / 100.0) + "%";
   }

   public String currency(Number value)
   {
      if (value == null)
      {
         return "";
      }
      String amount;
      synchronized (currencyNumberFormat)
      {
         amount = currencyNumberFormat.format(value.doubleValue());
      }
      return switch (currencyPosition)
      {
         case BEFORE -> currencySymbol + amount;
         case BEFORE_WITH_SPACE -> currencySymbol + " " + amount;
         case AFTER -> amount + currencySymbol;
         case AFTER_WITH_SPACE -> amount + " " + currencySymbol;
      };
   }

   public String rate(Rate rate)
   {
      if (rate == null)
      {
         return currency(0) + "/" + unitShort(TimeUnit.HOURS, 1, false);
      }
      return currency(rate.getAmount()) + "/" + rateUnit(rate.getUnits());
   }

   private String rateUnit(TimeUnit unit)
   {
      if (unit == null)
      {
         return unitShort(TimeUnit.HOURS, 1, false);
      }
      if (language == Language.DE)
      {
         return switch (unit)
         {
            case MINUTES, ELAPSED_MINUTES -> "Min.";
            case HOURS, ELAPSED_HOURS -> "Std.";
            case DAYS, ELAPSED_DAYS -> "Tag";
            case WEEKS, ELAPSED_WEEKS -> "Woche";
            case MONTHS, ELAPSED_MONTHS -> "Monat";
            case YEARS, ELAPSED_YEARS -> "Jahr";
            default -> "Std.";
         };
      }
      return switch (unit)
      {
         case MINUTES, ELAPSED_MINUTES -> "min";
         case HOURS, ELAPSED_HOURS -> "hr";
         case DAYS, ELAPSED_DAYS -> "day";
         case WEEKS, ELAPSED_WEEKS -> "wk";
         case MONTHS, ELAPSED_MONTHS -> "mon";
         case YEARS, ELAPSED_YEARS -> "yr";
         default -> "hr";
      };
   }

   public String bool(boolean value)
   {
      return language == Language.DE ? (value ? "Ja" : "Nein") : (value ? "Yes" : "No");
   }

   // ---------------------------------------------------------------- Dauer

   /**
    * Dauer wie in Project: „5 Tage“, „1 Tag“, „2,5 Std.“, „0 Tage“, mit „?“ bei geschätzter Dauer.
    */
   public String duration(Duration d, boolean estimated)
   {
      if (d == null)
      {
         return "";
      }
      double value = d.getDuration();
      TimeUnit unit = d.getUnits() == null ? TimeUnit.DAYS : d.getUnits();
      String text;
      if (unit == TimeUnit.PERCENT || unit == TimeUnit.ELAPSED_PERCENT)
      {
         text = number(value) + "%";
      }
      else
      {
         text = number(round(value)) + " " + unitShort(unit, value, true);
      }
      return estimated ? text + "?" : text;
   }

   public String duration(Duration d)
   {
      return duration(d, false);
   }

   /**
    * Arbeit wird in Project in Stunden angezeigt.
    */
   public String work(Duration d)
   {
      if (d == null)
      {
         return "";
      }
      return number(round(d.getDuration())) + " " + unitShort(d.getUnits() == null ? TimeUnit.HOURS : d.getUnits(), d.getDuration(), true);
   }

   /**
    * Zeitabstand bei Anordnungsbeziehungen, z. B. „+2 Tage“ oder „-1 Tag“.
    */
   public String lag(Duration lag)
   {
      if (lag == null || lag.getDuration() == 0)
      {
         return "";
      }
      String sign = lag.getDuration() > 0 ? "+" : "-";
      Duration abs = Duration.getInstance(Math.abs(lag.getDuration()), lag.getUnits());
      return sign + duration(abs);
   }

   private static double round(double value)
   {
      return Math.round(value * 100.0) / 100.0;
   }

   /**
    * Einheitenbezeichnung wie in Project. Deutsch: Min., Std., Tag/Tage, Woche/Wochen, Monat/Monate, Jahr/Jahre;
    * fortlaufende Dauern mit vorangestelltem „f“. Englisch: mins, hrs, days, wks, mons, yrs mit „e“.
    */
   public String unitShort(TimeUnit unit, double value, boolean plural)
   {
      boolean one = !plural || Math.abs(value - 1.0) < 0.0001;
      boolean elapsed = unit.isElapsed();
      String text;
      if (language == Language.DE)
      {
         text = switch (unit)
         {
            case MINUTES, ELAPSED_MINUTES -> "Min.";
            case HOURS, ELAPSED_HOURS -> "Std.";
            case DAYS, ELAPSED_DAYS -> one ? "Tag" : "Tage";
            case WEEKS, ELAPSED_WEEKS -> one ? "Woche" : "Wochen";
            case MONTHS, ELAPSED_MONTHS -> one ? "Monat" : "Monate";
            case YEARS, ELAPSED_YEARS -> one ? "Jahr" : "Jahre";
            default -> "";
         };
         return elapsed ? "f" + text : text;
      }
      text = switch (unit)
      {
         case MINUTES, ELAPSED_MINUTES -> one ? "min" : "mins";
         case HOURS, ELAPSED_HOURS -> one ? "hr" : "hrs";
         case DAYS, ELAPSED_DAYS -> one ? "day" : "days";
         case WEEKS, ELAPSED_WEEKS -> one ? "wk" : "wks";
         case MONTHS, ELAPSED_MONTHS -> one ? "mon" : "mons";
         case YEARS, ELAPSED_YEARS -> one ? "yr" : "yrs";
         default -> "";
      };
      return elapsed ? "e" + text : text;
   }

   // ---------------------------------------------------------------- Beziehungen und Ressourcen

   /**
    * Kürzel der Anordnungsbeziehung: EA/AA/EE/AE bzw. FS/SS/FF/SF.
    */
   public String relationType(RelationType type)
   {
      if (type == null)
      {
         type = RelationType.FINISH_START;
      }
      if (language == Language.DE)
      {
         return switch (type)
         {
            case FINISH_START -> "EA";
            case START_START -> "AA";
            case FINISH_FINISH -> "EE";
            case START_FINISH -> "AE";
         };
      }
      return switch (type)
      {
         case FINISH_START -> "FS";
         case START_START -> "SS";
         case FINISH_FINISH -> "FF";
         case START_FINISH -> "SF";
      };
   }

   public String relationTypeLong(RelationType type)
   {
      return I18n.t("relation." + (type == null ? RelationType.FINISH_START : type).name());
   }

   public String listSeparator()
   {
      return language == Language.DE ? ";" : ",";
   }

   /**
    * Vorgängerspalte wie in Project: „3;5AA;7EA+2 Tage“.
    */
   public String predecessors(Task task)
   {
      return relations(task.getPredecessors(), true);
   }

   public String successors(Task task)
   {
      return relations(task.getSuccessors(), false);
   }

   private String relations(List<Relation> relations, boolean predecessors)
   {
      if (relations == null || relations.isEmpty())
      {
         return "";
      }
      StringBuilder sb = new StringBuilder();
      for (Relation rel : relations)
      {
         Task other = predecessors ? rel.getPredecessorTask() : rel.getSuccessorTask();
         if (other == null || other.getID() == null)
         {
            continue;
         }
         if (!sb.isEmpty())
         {
            sb.append(listSeparator());
         }
         sb.append(other.getID());
         String lag = lag(rel.getLag());
         RelationType type = rel.getType() == null ? RelationType.FINISH_START : rel.getType();
         if (type != RelationType.FINISH_START || !lag.isEmpty())
         {
            sb.append(relationType(type));
         }
         sb.append(lag);
      }
      return sb.toString();
   }

   /**
    * Ressourcennamen wie in Project: „Anna;Bernd[50%]“.
    */
   public String resourceNames(Task task)
   {
      List<ResourceAssignment> assignments = task.getResourceAssignments();
      if (assignments == null || assignments.isEmpty())
      {
         return "";
      }
      StringBuilder sb = new StringBuilder();
      for (ResourceAssignment a : assignments)
      {
         Resource r = a.getResource();
         if (r == null || r.getName() == null || r.getName().isBlank())
         {
            continue;
         }
         if (!sb.isEmpty())
         {
            sb.append(listSeparator());
         }
         sb.append(r.getName());
         Number units = a.getUnits();
         if (r.getType() == ResourceType.MATERIAL)
         {
            if (units != null)
            {
               sb.append('[').append(number(units)).append(r.getMaterialLabel() == null ? "" : " " + r.getMaterialLabel()).append(']');
            }
         }
         else if (units != null && Math.abs(units.doubleValue() - 100.0) > 0.001 && r.getType() != ResourceType.COST)
         {
            sb.append('[').append(number(units)).append("%]");
         }
      }
      return sb.toString();
   }

   // ---------------------------------------------------------------- Aufzählungen

   public String taskMode(TaskMode mode)
   {
      if (mode == TaskMode.MANUALLY_SCHEDULED)
      {
         return I18n.t("taskMode.manual");
      }
      return I18n.t("taskMode.auto");
   }

   public String constraint(ConstraintType type)
   {
      return I18n.t("constraint." + (type == null ? ConstraintType.AS_SOON_AS_POSSIBLE : type).name());
   }

   public String taskType(TaskType type)
   {
      return type == null ? "" : I18n.t("taskType." + type.name());
   }

   public String resourceType(ResourceType type)
   {
      return I18n.t("resourceType." + (type == null ? ResourceType.WORK : type).name());
   }

   public String accrue(AccrueType type)
   {
      return I18n.t("accrue." + (type == null ? AccrueType.PRORATED : type).name());
   }

   public String priority(Priority priority)
   {
      return priority == null ? "500" : Integer.toString(priority.getValue());
   }

   /**
    * Generische Formatierung für beliebige Feldwerte (benutzerdefinierte Felder, seltene Spalten).
    */
   public String value(Object value)
   {
      if (value == null)
      {
         return "";
      }
      if (value instanceof LocalDateTime ldt)
      {
         return date(ldt);
      }
      if (value instanceof LocalDate ld)
      {
         return date(ld);
      }
      if (value instanceof LocalTime lt)
      {
         return time(lt);
      }
      if (value instanceof Duration d)
      {
         return duration(d);
      }
      if (value instanceof Rate r)
      {
         return rate(r);
      }
      if (value instanceof Boolean b)
      {
         return bool(b.booleanValue());
      }
      if (value instanceof Number n)
      {
         return number(n);
      }
      if (value instanceof TaskMode tm)
      {
         return taskMode(tm);
      }
      if (value instanceof ConstraintType ct)
      {
         return constraint(ct);
      }
      if (value instanceof TaskType tt)
      {
         return taskType(tt);
      }
      if (value instanceof ResourceType rt)
      {
         return resourceType(rt);
      }
      if (value instanceof AccrueType at)
      {
         return accrue(at);
      }
      if (value instanceof Priority p)
      {
         return priority(p);
      }
      if (value instanceof Task t)
      {
         return t.getName() == null ? "" : t.getName();
      }
      if (value instanceof Resource r)
      {
         return r.getName() == null ? "" : r.getName();
      }
      if (value instanceof List<?> list)
      {
         StringBuilder sb = new StringBuilder();
         for (Object o : list)
         {
            if (!sb.isEmpty())
            {
               sb.append(listSeparator());
            }
            sb.append(value(o));
         }
         return sb.toString();
      }
      return value.toString();
   }
}
