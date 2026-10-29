package io.github.triplethep.mppviewer.ui.ribbon;

import io.github.triplethep.mppviewer.render.Style;
import io.github.triplethep.mppviewer.ui.Icons;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/**
 * Fabrik für die Bedienelemente des Menübands.
 */
public final class RibbonButtons
{
   private RibbonButtons()
   {
   }

   /**
    * Großer Knopf mit 32-px-Symbol. Zeilenumbrüche im Text werden mit „\n“ angegeben.
    */
   public static JButton large(Icons.Name icon, String text, String tooltip, ActionListener action)
   {
      JButton b = new JButton(html(text), Icons.get(icon, 32));
      styleLarge(b, tooltip == null ? text.replace('\n', ' ') : tooltip);
      if (action != null)
      {
         b.addActionListener(action);
      }
      return b;
   }

   public static JToggleButton largeToggle(Icons.Name icon, String text, String tooltip, ActionListener action)
   {
      JToggleButton b = new JToggleButton(html(text), Icons.get(icon, 32));
      styleLarge(b, tooltip == null ? text.replace('\n', ' ') : tooltip);
      if (action != null)
      {
         b.addActionListener(action);
      }
      return b;
   }

   /**
    * Großer Knopf, der ein Menü aufklappt.
    */
   public static JButton largeMenu(Icons.Name icon, String text, String tooltip, Consumer<JPopupMenu> filler)
   {
      JButton b = new JButton(html(text + " ▾"), Icons.get(icon, 32));
      styleLarge(b, tooltip == null ? text.replace('\n', ' ') : tooltip);
      b.addActionListener(e ->
      {
         JPopupMenu menu = new JPopupMenu();
         filler.accept(menu);
         menu.show(b, 0, b.getHeight());
      });
      return b;
   }

   public static JButton small(Icons.Name icon, String text, String tooltip, ActionListener action)
   {
      JButton b = new JButton(text, Icons.get(icon, 16));
      styleSmall(b, tooltip);
      if (action != null)
      {
         b.addActionListener(action);
      }
      return b;
   }

   public static JButton smallMenu(Icons.Name icon, String text, String tooltip, Consumer<JPopupMenu> filler)
   {
      JButton b = new JButton("<html>" + escape(text) + "&nbsp;&#9662;</html>", Icons.get(icon, 16));
      styleSmall(b, tooltip == null ? text : tooltip);
      b.addActionListener(e ->
      {
         JPopupMenu menu = new JPopupMenu();
         filler.accept(menu);
         menu.show(b, 0, b.getHeight());
      });
      return b;
   }

   public static JCheckBox check(String text, String tooltip, boolean selected, Consumer<Boolean> onChange)
   {
      JCheckBox box = new JCheckBox(text, selected);
      box.setOpaque(false);
      box.setFont(Style.UI_FONT);
      box.setToolTipText(tooltip);
      box.setFocusable(false);
      box.setMargin(new Insets(1, 2, 1, 6));
      box.addActionListener(e -> onChange.accept(Boolean.valueOf(box.isSelected())));
      return box;
   }

   private static void styleLarge(AbstractButton b, String tooltip)
   {
      b.putClientProperty("JButton.buttonType", "toolBarButton");
      b.setVerticalTextPosition(SwingConstants.BOTTOM);
      b.setHorizontalTextPosition(SwingConstants.CENTER);
      b.setIconTextGap(2);
      b.setMargin(new Insets(3, 5, 3, 5));
      b.setFocusable(false);
      b.setFont(Style.UI_FONT);
      b.setToolTipText(tooltip);
      Dimension d = b.getPreferredSize();
      b.setPreferredSize(new Dimension(Math.max(54, d.width), 74));
      b.setMaximumSize(new Dimension(Math.max(54, d.width), 74));
      b.getAccessibleContext().setAccessibleName(tooltip);
   }

   private static void styleSmall(AbstractButton b, String tooltip)
   {
      b.putClientProperty("JButton.buttonType", "toolBarButton");
      b.setHorizontalAlignment(SwingConstants.LEFT);
      b.setMargin(new Insets(2, 4, 2, 6));
      b.setFocusable(false);
      b.setFont(Style.UI_FONT);
      b.setToolTipText(tooltip);
      b.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 6));
      Dimension d = b.getPreferredSize();
      b.setMaximumSize(new Dimension(Math.max(d.width, 40), 24));
      b.getAccessibleContext().setAccessibleName(tooltip == null ? b.getText() : tooltip);
   }

   private static String html(String text)
   {
      if (!text.contains("\n"))
      {
         return "<html><center>" + escape(text) + "<br>&nbsp;</center></html>";
      }
      return "<html><center>" + escape(text).replace("\n", "<br>") + "</center></html>";
   }

   private static String escape(String s)
   {
      return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
   }

   public static Icon icon(Icons.Name name)
   {
      return Icons.get(name, 16);
   }
}
