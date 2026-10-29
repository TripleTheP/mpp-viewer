package io.github.triplethep.mppviewer.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import io.github.triplethep.mppviewer.render.Style;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Look-and-Feel im Stil von Office/Project: grüne Titelleiste, helles Menüband, Segoe UI.
 */
public final class Theme
{
   private Theme()
   {
   }

   public static void install()
   {
      System.setProperty("flatlaf.useWindowDecorations", "true");
      System.setProperty("flatlaf.menuBarEmbedded", "true");

      Map<String, String> defaults = new HashMap<>();
      defaults.put("@accentColor", hex(Style.ACCENT));
      defaults.put("TitlePane.background", hex(Style.ACCENT));
      defaults.put("TitlePane.inactiveBackground", hex(Style.ACCENT_DARK));
      defaults.put("TitlePane.foreground", "#FFFFFF");
      defaults.put("TitlePane.inactiveForeground", "#E0E0E0");
      defaults.put("TitlePane.embeddedForeground", "#FFFFFF");
      defaults.put("TitlePane.buttonHoverBackground", hex(Style.ACCENT_HOVER));
      defaults.put("TitlePane.buttonPressedBackground", hex(Style.ACCENT_DARK));
      defaults.put("TitlePane.closeHoverBackground", "#E81123");
      defaults.put("TitlePane.centerTitle", "true");
      defaults.put("TitlePane.unifiedBackground", "false");
      defaults.put("MenuBar.background", hex(Style.ACCENT));
      defaults.put("MenuBar.foreground", "#FFFFFF");
      defaults.put("MenuBar.hoverBackground", hex(Style.ACCENT_HOVER));
      defaults.put("MenuBar.borderColor", hex(Style.ACCENT));
      defaults.put("Table.gridColor", hex(Style.GRID));
      defaults.put("Table.selectionBackground", hex(Style.ROW_SELECTED));
      defaults.put("Table.selectionForeground", "#1F1F1F");
      defaults.put("Table.selectionInactiveBackground", hex(Style.ROW_SELECTED));
      defaults.put("Table.selectionInactiveForeground", "#1F1F1F");
      defaults.put("TableHeader.background", hex(Style.HEADER_BG));
      defaults.put("TableHeader.separatorColor", hex(Style.HEADER_BORDER));
      defaults.put("TableHeader.bottomSeparatorColor", hex(Style.HEADER_BORDER));
      defaults.put("SplitPane.dividerSize", "6");
      defaults.put("SplitPaneDivider.gripColor", "#9A9A9A");
      defaults.put("ScrollBar.showButtons", "true");
      defaults.put("ScrollBar.width", "14");
      defaults.put("Component.focusWidth", "1");
      defaults.put("Button.arc", "4");
      defaults.put("Component.arc", "4");
      defaults.put("TabbedPane.selectedBackground", "#FFFFFF");
      defaults.put("TabbedPane.underlineColor", hex(Style.ACCENT));
      defaults.put("ToolTip.background", "#FFFFFF");
      FlatLaf.setGlobalExtraDefaults(defaults);

      FlatLightLaf.setup();
      UIManager.put("defaultFont", new FontUIResource(Style.UI_FONT));
      UIManager.put("Table.showHorizontalLines", Boolean.TRUE);
      UIManager.put("Table.showVerticalLines", Boolean.TRUE);
      UIManager.put("Table.intercellSpacing", new java.awt.Dimension(1, 1));
   }

   private static String hex(Color c)
   {
      return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
   }
}
