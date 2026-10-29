package io.github.triplethep.mppviewer.ui.view;

import io.github.triplethep.mppviewer.core.ColumnDef;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.render.Style;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Aufklappmenü eines AutoFilter-Pfeils: sortieren, Filter löschen und Werte auswählen – wie in Project.
 */
final class AutoFilterPopup
{
   /**
    * Ergebnis-Rückrufe des Menüs.
    */
   interface Actions
   {
      void sort(ColumnDef column, boolean ascending);

      /** {@code null} = Filter dieser Spalte löschen */
      void filter(ColumnDef column, Set<String> allowed);
   }

   private AutoFilterPopup()
   {
   }

   static void show(Component invoker, Rectangle cell, ColumnDef column, List<String> values, Set<String> current, Actions actions)
   {
      JPopupMenu popup = new JPopupMenu();
      JPanel root = new JPanel(new BorderLayout(0, 6));
      root.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
      root.setBackground(Color.WHITE);

      JPanel top = new JPanel();
      top.setOpaque(false);
      top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
      top.add(menuButton(I18n.t("autofilter.sortAsc"), () ->
      {
         popup.setVisible(false);
         actions.sort(column, true);
      }));
      top.add(menuButton(I18n.t("autofilter.sortDesc"), () ->
      {
         popup.setVisible(false);
         actions.sort(column, false);
      }));
      JSeparator sep = new JSeparator();
      sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
      top.add(sep);
      JButton clear = menuButton(I18n.t("autofilter.clear", column.getTitle()), () ->
      {
         popup.setVisible(false);
         actions.filter(column, null);
      });
      clear.setEnabled(current != null);
      top.add(clear);
      top.add(Box.createVerticalStrut(4));
      JTextField search = new JTextField();
      search.putClientProperty("JTextField.placeholderText", I18n.t("autofilter.search"));
      search.setAlignmentX(Component.LEFT_ALIGNMENT);
      search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
      top.add(search);
      root.add(top, BorderLayout.NORTH);

      // Werteliste mit Häkchen
      JPanel list = new JPanel();
      list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
      list.setBackground(Color.WHITE);
      JCheckBox all = new JCheckBox(I18n.t("autofilter.selectAll"));
      all.setOpaque(false);
      List<JCheckBox> boxes = new ArrayList<>();
      for (String v : values)
      {
         JCheckBox b = new JCheckBox(v.isEmpty() ? I18n.t("autofilter.empty") : v, current == null || current.contains(v));
         b.putClientProperty("value", v);
         b.setOpaque(false);
         boxes.add(b);
      }
      Runnable syncAll = () -> all.setSelected(boxes.stream().filter(Component::isVisible).allMatch(JCheckBox::isSelected));
      all.addActionListener(e ->
      {
         for (JCheckBox b : boxes)
         {
            if (b.isVisible())
            {
               b.setSelected(all.isSelected());
            }
         }
      });
      for (JCheckBox b : boxes)
      {
         b.addActionListener(e -> syncAll.run());
      }
      syncAll.run();
      list.add(all);
      for (JCheckBox b : boxes)
      {
         list.add(b);
      }
      search.getDocument().addDocumentListener(new DocumentListener()
      {
         private void update()
         {
            String q = search.getText().trim().toLowerCase(Locale.ROOT);
            for (JCheckBox b : boxes)
            {
               b.setVisible(q.isEmpty() || b.getText().toLowerCase(Locale.ROOT).contains(q));
            }
            syncAll.run();
            list.revalidate();
            list.repaint();
         }

         @Override
         public void insertUpdate(DocumentEvent e)
         {
            update();
         }

         @Override
         public void removeUpdate(DocumentEvent e)
         {
            update();
         }

         @Override
         public void changedUpdate(DocumentEvent e)
         {
            update();
         }
      });
      JScrollPane scroll = new JScrollPane(list);
      scroll.setBorder(BorderFactory.createLineBorder(new Color(0xD0D0D0)));
      scroll.getVerticalScrollBar().setUnitIncrement(16);
      scroll.setPreferredSize(new Dimension(250, Math.min(260, 26 + values.size() * 24)));
      root.add(scroll, BorderLayout.CENTER);

      JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
      buttons.setOpaque(false);
      JButton ok = new JButton(I18n.t("common.ok"));
      JButton cancel = new JButton(I18n.t("common.cancel"));
      ok.addActionListener(e ->
      {
         popup.setVisible(false);
         Set<String> selected = new LinkedHashSet<>();
         boolean everything = true;
         for (JCheckBox b : boxes)
         {
            boolean take = b.isSelected() && b.isVisible();
            if (take)
            {
               selected.add((String) b.getClientProperty("value"));
            }
            everything &= take;
         }
         actions.filter(column, everything ? null : selected);
      });
      cancel.addActionListener(e -> popup.setVisible(false));
      buttons.add(ok);
      buttons.add(cancel);
      root.add(buttons, BorderLayout.SOUTH);

      popup.add(root);
      popup.show(invoker, Math.max(0, cell.x + cell.width - 270), cell.y + cell.height);
      search.requestFocusInWindow();
   }

   private static JButton menuButton(String text, Runnable action)
   {
      JButton b = new JButton(text);
      b.putClientProperty("JButton.buttonType", "toolBarButton");
      b.setHorizontalAlignment(SwingConstants.LEFT);
      b.setAlignmentX(Component.LEFT_ALIGNMENT);
      b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
      b.setFont(Style.UI_FONT);
      b.addActionListener(e -> action.run());
      return b;
   }

   /**
    * Sortierte, eindeutige Werteliste.
    */
   static List<String> distinct(Iterable<String> values, Locale locale)
   {
      Collator c = Collator.getInstance(locale);
      Set<String> set = new TreeSet<>((a, b) ->
      {
         if (a.isEmpty() != b.isEmpty())
         {
            return a.isEmpty() ? 1 : -1;
         }
         int r = c.compare(a, b);
         return r != 0 ? r : a.compareTo(b);
      });
      for (String v : values)
      {
         set.add(v == null ? "" : v);
      }
      return new ArrayList<>(set);
   }
}
