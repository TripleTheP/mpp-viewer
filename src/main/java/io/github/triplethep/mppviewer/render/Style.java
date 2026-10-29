package io.github.triplethep.mppviewer.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Farben, Schriften und Maße im Stil von Microsoft Project 2016–365. Gilt für Bildschirm, Druck und Export.
 */
public final class Style
{
   // Markenfarbe von Project (Titelleiste, Menüband, Auswahl)
   public static final Color ACCENT = new Color(0x31752F);
   public static final Color ACCENT_DARK = new Color(0x245A22);
   public static final Color ACCENT_HOVER = new Color(0x3E8A3B);
   public static final Color ACCENT_LIGHT = new Color(0xDCEBDA);
   public static final Color ACCENT_SELECTION = new Color(0xC6E0C3);

   // Tabelle
   public static final Color GRID = new Color(0xDADADA);
   public static final Color HEADER_BG = new Color(0xF3F3F3);
   public static final Color HEADER_BORDER = new Color(0xC8C8C8);
   public static final Color HEADER_TEXT = new Color(0x262626);
   public static final Color ID_BG = new Color(0xF3F3F3);
   public static final Color ROW_SELECTED = new Color(0xDCEBDA);
   public static final Color ID_SELECTED = new Color(0xBFDBBC);
   public static final Color TEXT = new Color(0x1F1F1F);
   public static final Color TEXT_MUTED = new Color(0x7A7A7A);
   public static final Color OUTLINE_TRIANGLE = new Color(0x505050);

   // Gantt-Diagramm
   public static final Color CHART_BG = Color.WHITE;
   public static final Color NONWORKING = new Color(0xEDEDED);
   public static final Color TIMESCALE_BG = new Color(0xFFFFFF);
   public static final Color TIMESCALE_LINE = new Color(0xD0D0D0);
   public static final Color TASK_FILL = new Color(0x8CB4E8);
   public static final Color TASK_BORDER = new Color(0x6F9BD8);
   public static final Color TASK_PROGRESS = new Color(0x3F6FBF);
   public static final Color MANUAL_FILL = new Color(0xA6DDE6);
   public static final Color MANUAL_BORDER = new Color(0x3FA5B8);
   public static final Color CRITICAL_FILL = new Color(0xF2A4A4);
   public static final Color CRITICAL_BORDER = new Color(0xDB7373);
   public static final Color CRITICAL_PROGRESS = new Color(0xC00000);
   public static final Color SUMMARY = new Color(0x404040);
   public static final Color PROJECT_SUMMARY = new Color(0x8C8C8C);
   public static final Color MILESTONE = new Color(0x1A1A1A);
   public static final Color BASELINE = new Color(0xA6A6A6);
   public static final Color LINK = new Color(0x3E6EB8);
   public static final Color LINK_CRITICAL = new Color(0xC00000);
   public static final Color CURRENT_DATE = new Color(0x2E8B2E);
   public static final Color DEADLINE = new Color(0x2E8B2E);
   public static final Color INACTIVE = new Color(0xB0B0B0);
   public static final Color BAR_TEXT = new Color(0x262626);

   // Symbole in der Indikatorenspalte
   public static final Color ICON_BLUE = new Color(0x2B6CC4);
   public static final Color ICON_GREEN = new Color(0x2E8B2E);
   public static final Color ICON_RED = new Color(0xC42B2B);
   public static final Color ICON_GREY = new Color(0x6E6E6E);

   public static final int ROW_HEIGHT = 20;
   public static final int TIER_HEIGHT = 20;
   public static final int HEADER_HEIGHT = TIER_HEIGHT * 2;
   public static final int INDENT = 14;

   private static final String FAMILY = pickFamily();

   public static final Font TABLE_FONT = new Font(FAMILY, Font.PLAIN, 12);
   public static final Font TABLE_FONT_BOLD = TABLE_FONT.deriveFont(Font.BOLD);
   public static final Font HEADER_FONT = new Font(FAMILY, Font.PLAIN, 12);
   public static final Font TIMESCALE_FONT = new Font(FAMILY, Font.PLAIN, 11);
   public static final Font BAR_FONT = new Font(FAMILY, Font.PLAIN, 11);
   public static final Font UI_FONT = new Font(FAMILY, Font.PLAIN, 12);

   private Style()
   {
   }

   public static String fontFamily()
   {
      return FAMILY;
   }

   private static String pickFamily()
   {
      Set<String> available;
      try
      {
         available = new HashSet<>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
      }
      catch (RuntimeException | Error ex)
      {
         return Font.SANS_SERIF;
      }
      for (String candidate : new String[]
      {
         "Segoe UI",
         "Calibri",
         "Arial",
         "Liberation Sans",
         "DejaVu Sans"
      })
      {
         if (available.contains(candidate))
         {
            return candidate;
         }
      }
      return Font.SANS_SERIF;
   }
}
