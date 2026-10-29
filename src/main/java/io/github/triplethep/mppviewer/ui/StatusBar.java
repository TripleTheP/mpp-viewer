package io.github.triplethep.mppviewer.ui;

import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.render.Style;
import io.github.triplethep.mppviewer.render.ViewKind;
import io.github.triplethep.mppviewer.render.ZoomLevel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * Statusleiste unten wie in Project: Status links, Ansichtsumschalter und Zoomregler rechts.
 */
public final class StatusBar extends JPanel
{
   private static final long serialVersionUID = 1L;
   private static final Color BG = new Color(0xF0F0F0);

   private final JLabel status = new JLabel();
   private final JLabel readOnly = new JLabel();
   private final JLabel count = new JLabel();
   private final JLabel filterInfo = new JLabel();
   private Runnable onClearFilter;
   private final Map<ViewKind, JToggleButton> viewButtons = new EnumMap<>(ViewKind.class);
   private final JSlider zoom = new JSlider(0, ZoomLevel.values().length - 1, ZoomLevel.DEFAULT.ordinal());
   private final JButton zoomOut;
   private final JButton zoomIn;
   private boolean adjusting;

   public StatusBar(Consumer<ViewKind> onView, DoubleConsumer onZoom)
   {
      super(new BorderLayout());
      setBackground(BG);
      setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xD0D0D0)), BorderFactory.createEmptyBorder(1, 8, 1, 6)));

      JPanel left = new JPanel();
      left.setOpaque(false);
      left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
      status.setText(I18n.t("status.ready"));
      readOnly.setIcon(Icons.get(Icons.Name.LOCK, 14));
      readOnly.setText(I18n.t("status.readOnly"));
      readOnly.setToolTipText(I18n.t("status.readOnly.tooltip"));
      for (JLabel l : new JLabel[]
      {
         status,
         readOnly,
         count
      })
      {
         l.setFont(Style.UI_FONT.deriveFont(11.5f));
         l.setForeground(new Color(0x404040));
      }
      left.add(status);
      left.add(Box.createHorizontalStrut(18));
      left.add(readOnly);
      left.add(Box.createHorizontalStrut(18));
      left.add(count);
      left.add(Box.createHorizontalStrut(18));
      filterInfo.setFont(Style.UI_FONT.deriveFont(11.5f));
      filterInfo.setForeground(Style.ACCENT);
      filterInfo.setIcon(Icons.get(Icons.Name.FILTER, 14));
      filterInfo.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
      filterInfo.setToolTipText(I18n.t("status.filtered.tooltip"));
      filterInfo.setVisible(false);
      filterInfo.addMouseListener(new java.awt.event.MouseAdapter()
      {
         @Override
         public void mouseClicked(java.awt.event.MouseEvent e)
         {
            if (onClearFilter != null)
            {
               onClearFilter.run();
            }
         }
      });
      left.add(filterInfo);
      add(left, BorderLayout.WEST);

      JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
      right.setOpaque(false);
      addViewButton(right, ViewKind.GANTT, Icons.Name.GANTT, onView);
      addViewButton(right, ViewKind.TASK_SHEET, Icons.Name.TASK_SHEET, onView);
      addViewButton(right, ViewKind.RESOURCE_SHEET, Icons.Name.RESOURCE_SHEET, onView);
      right.add(Box.createHorizontalStrut(10));

      zoomOut = small(Icons.Name.ZOOM_OUT, I18n.t("ribbon.zoomOut"));
      zoomIn = small(Icons.Name.ZOOM_IN, I18n.t("ribbon.zoomIn"));
      zoom.setOpaque(false);
      zoom.setPreferredSize(new Dimension(120, 20));
      zoom.setFocusable(false);
      zoom.setToolTipText(I18n.t("status.zoom"));
      zoom.addChangeListener(e ->
      {
         if (!adjusting)
         {
            onZoom.accept(ZoomLevel.values()[zoom.getValue()].pxPerDay());
         }
      });
      zoomOut.addActionListener(e -> zoom.setValue(Math.max(0, zoom.getValue() - 1)));
      zoomIn.addActionListener(e -> zoom.setValue(Math.min(zoom.getMaximum(), zoom.getValue() + 1)));
      right.add(zoomOut);
      right.add(zoom);
      right.add(zoomIn);
      add(right, BorderLayout.EAST);
      setPreferredSize(new Dimension(100, 26));
      setDocumentLoaded(false);
   }

   private void addViewButton(JPanel panel, ViewKind kind, Icons.Name icon, Consumer<ViewKind> onView)
   {
      JToggleButton b = new JToggleButton(Icons.get(icon, 16));
      b.putClientProperty("JButton.buttonType", "toolBarButton");
      b.setFocusable(false);
      b.setToolTipText(I18n.t(kind.key()));
      b.getAccessibleContext().setAccessibleName(I18n.t(kind.key()));
      b.setPreferredSize(new Dimension(24, 22));
      b.addActionListener(e -> onView.accept(kind));
      viewButtons.put(kind, b);
      panel.add(b);
   }

   private static JButton small(Icons.Name icon, String tooltip)
   {
      JButton b = new JButton(Icons.get(icon, 14));
      b.putClientProperty("JButton.buttonType", "toolBarButton");
      b.setFocusable(false);
      b.setToolTipText(tooltip);
      b.getAccessibleContext().setAccessibleName(tooltip);
      b.setPreferredSize(new Dimension(22, 22));
      return b;
   }

   public void setStatus(String text)
   {
      status.setText(text);
   }

   public void setCount(String text)
   {
      count.setText(text);
   }

   /**
    * Hinweis „Gefiltert: x von y – alle anzeigen“; {@code null} blendet ihn aus.
    */
   public void setFilterInfo(String text, Runnable onClear)
   {
      this.onClearFilter = onClear;
      filterInfo.setText(text == null ? "" : "<html>" + text + " · <u>" + I18n.t("status.filtered.clear") + "</u></html>");
      filterInfo.setVisible(text != null);
   }

   public void setActiveView(ViewKind kind)
   {
      for (Map.Entry<ViewKind, JToggleButton> e : viewButtons.entrySet())
      {
         boolean active = e.getKey() == kind || (kind == ViewKind.TRACKING_GANTT && e.getKey() == ViewKind.GANTT);
         e.getValue().setSelected(active);
      }
      boolean chart = kind != null && kind.hasChart();
      zoom.setEnabled(chart);
      zoomIn.setEnabled(chart);
      zoomOut.setEnabled(chart);
   }

   public void setZoom(double pxPerDay)
   {
      adjusting = true;
      zoom.setValue(ZoomLevel.nearest(pxPerDay).ordinal());
      adjusting = false;
   }

   public void setDocumentLoaded(boolean loaded)
   {
      readOnly.setVisible(loaded);
      count.setVisible(loaded);
      for (JToggleButton b : viewButtons.values())
      {
         b.setEnabled(loaded);
         if (!loaded)
         {
            b.setSelected(false);
         }
      }
      zoom.setEnabled(loaded);
      zoomIn.setEnabled(loaded);
      zoomOut.setEnabled(loaded);
   }
}
