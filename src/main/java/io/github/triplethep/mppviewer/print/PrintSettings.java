package io.github.triplethep.mppviewer.print;

import io.github.triplethep.mppviewer.i18n.Language;

import java.time.LocalDate;

/**
 * Einstellungen der Seitenansicht (entspricht „Seite einrichten“ in Project).
 */
public final class PrintSettings
{
   public enum Paper
   {
      A4(595.28, 841.89),
      A3(841.89, 1190.55),
      A5(419.53, 595.28),
      LETTER(612, 792),
      LEGAL(612, 1008),
      TABLOID(792, 1224);

      private final double width;
      private final double height;

      Paper(double width, double height)
      {
         this.width = width;
         this.height = height;
      }

      /** Breite im Hochformat in Punkt (1/72 Zoll). */
      public double width()
      {
         return width;
      }

      /** Höhe im Hochformat in Punkt. */
      public double height()
      {
         return height;
      }

      public String label()
      {
         return switch (this)
         {
            case A4 -> "A4 (210 × 297 mm)";
            case A3 -> "A3 (297 × 420 mm)";
            case A5 -> "A5 (148 × 210 mm)";
            case LETTER -> "Letter (8.5 × 11 in)";
            case LEGAL -> "Legal (8.5 × 14 in)";
            case TABLOID -> "Tabloid (11 × 17 in)";
         };
      }
   }

   public enum Scaling
   {
      /** Auf n Seiten Breite anpassen, Höhe nach Bedarf */
      FIT_PAGES_WIDE,
      /** Alles auf eine Seite */
      FIT_ONE_PAGE,
      /** Fester Prozentwert */
      PERCENT
   }

   public enum Range
   {
      WHOLE_PROJECT,
      DATES
   }

   /**
    * Zeitskala im Druck: automatisch passend zur Seitenbreite, wie in der Ansicht oder fest.
    */
   public enum Timescale
   {
      AUTO(0),
      VIEW(0),
      DAYS(21),
      WEEKS(6),
      MONTHS(1.6),
      QUARTERS(0.6);

      private final double pxPerDay;

      Timescale(double pxPerDay)
      {
         this.pxPerDay = pxPerDay;
      }

      public double pxPerDay()
      {
         return pxPerDay;
      }
   }

   public enum Order
   {
      DOWN_THEN_OVER,
      OVER_THEN_DOWN
   }

   public Paper paper = Paper.A4;
   public boolean landscape = true;
   public Scaling scaling = Scaling.FIT_PAGES_WIDE;
   public int pagesWide = 1;
   public int percent = 100;
   public Range range = Range.WHOLE_PROJECT;
   public Timescale timescale = Timescale.AUTO;
   public LocalDate from;
   public LocalDate to;
   public boolean allColumns;
   public boolean legend = true;
   public boolean title = true;
   public boolean pageNumbers = true;
   public Order order = Order.DOWN_THEN_OVER;
   /** Rand in Punkt (1 cm) */
   public double margin = 28.35;

   public static PrintSettings defaults(Language language)
   {
      PrintSettings s = new PrintSettings();
      s.paper = language == Language.EN ? Paper.LETTER : Paper.A4;
      return s;
   }

   public PrintSettings copy()
   {
      PrintSettings s = new PrintSettings();
      s.paper = paper;
      s.landscape = landscape;
      s.scaling = scaling;
      s.pagesWide = pagesWide;
      s.percent = percent;
      s.range = range;
      s.timescale = timescale;
      s.from = from;
      s.to = to;
      s.allColumns = allColumns;
      s.legend = legend;
      s.title = title;
      s.pageNumbers = pageNumbers;
      s.order = order;
      s.margin = margin;
      return s;
   }

   public double pageWidth()
   {
      return landscape ? paper.height() : paper.width();
   }

   public double pageHeight()
   {
      return landscape ? paper.width() : paper.height();
   }
}
