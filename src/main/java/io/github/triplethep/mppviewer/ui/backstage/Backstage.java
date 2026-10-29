package io.github.triplethep.mppviewer.ui.backstage;

import io.github.triplethep.mppviewer.core.ProjectDocument;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.i18n.Language;
import io.github.triplethep.mppviewer.render.Style;
import io.github.triplethep.mppviewer.ui.AppInfo;
import io.github.triplethep.mppviewer.ui.Icons;
import io.github.triplethep.mppviewer.ui.ProjectInfoPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Backstage-Ansicht hinter der Registerkarte „Datei“ – Informationen, Öffnen, Drucken, Exportieren, Optionen.
 */
public final class Backstage extends JPanel
{
   private static final long serialVersionUID = 1L;

   public enum Page
   {
      INFO,
      OPEN,
      PRINT,
      EXPORT,
      OPTIONS
   }

   static final Color CONTENT_BG = Color.WHITE;
   static final Font TITLE_FONT = Style.UI_FONT.deriveFont(Font.PLAIN, 30f);
   static final Font SECTION_FONT = Style.UI_FONT.deriveFont(Font.PLAIN, 17f);

   private final BackstageHost host;
   private final JPanel content = new JPanel(new CardLayout());
   private final Map<Page, NavItem> nav = new EnumMap<>(Page.class);
   private final Map<Page, JComponent> pages = new EnumMap<>(Page.class);
   private PrintPage printPage;
   private Page current = Page.INFO;

   public Backstage(BackstageHost host)
   {
      super(new BorderLayout());
      this.host = host;
      setBackground(CONTENT_BG);

      JPanel left = new JPanel();
      left.setBackground(Style.ACCENT);
      left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
      left.setPreferredSize(new Dimension(200, 100));
      left.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 0));

      NavItem back = new NavItem("", Icons.mono(Icons.Name.BACK, 28, Color.WHITE), host::closeBackstage);
      back.setToolTipText(I18n.t("backstage.back"));
      back.getAccessibleContext().setAccessibleName(I18n.t("backstage.back"));
      left.add(back);
      left.add(Box.createVerticalStrut(14));
      addNav(left, Page.INFO, I18n.t("backstage.info"));
      addNav(left, Page.OPEN, I18n.t("backstage.open"));
      left.add(Box.createVerticalStrut(10));
      addNav(left, Page.PRINT, I18n.t("backstage.print"));
      addNav(left, Page.EXPORT, I18n.t("backstage.export"));
      left.add(Box.createVerticalStrut(10));
      NavItem close = new NavItem(I18n.t("backstage.close"), null, host::closeDocument);
      close.setEnabled(host.document() != null);
      left.add(close);
      left.add(Box.createVerticalGlue());
      addNav(left, Page.OPTIONS, I18n.t("backstage.options"));
      add(left, BorderLayout.WEST);

      content.setBackground(CONTENT_BG);
      add(content, BorderLayout.CENTER);

      getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeBackstage");
      getActionMap().put("closeBackstage", new javax.swing.AbstractAction()
      {
         private static final long serialVersionUID = 1L;

         @Override
         public void actionPerformed(ActionEvent e)
         {
            host.closeBackstage();
         }
      });
   }

   private void addNav(JPanel left, Page page, String text)
   {
      NavItem item = new NavItem(text, null, () -> show(page));
      boolean needsDoc = page == Page.PRINT || page == Page.EXPORT;
      if (needsDoc && host.document() == null)
      {
         item.setEnabled(false);
      }
      nav.put(page, item);
      left.add(item);
   }

   /**
    * Zeigt eine Seite; Inhalte werden jedes Mal neu aufgebaut, damit sie zum aktuellen Projekt passen.
    */
   public void show(Page page)
   {
      if ((page == Page.PRINT || page == Page.EXPORT) && host.document() == null)
      {
         page = Page.OPEN;
      }
      current = page;
      JComponent comp = pages.remove(page);
      if (comp != null)
      {
         content.remove(comp);
      }
      comp = build(page);
      pages.put(page, comp);
      content.add(comp, page.name());
      ((CardLayout) content.getLayout()).show(content, page.name());
      for (Map.Entry<Page, NavItem> e : nav.entrySet())
      {
         e.getValue().setSelected(e.getKey() == page);
      }
      content.revalidate();
      content.repaint();
   }

   public Page getCurrent()
   {
      return current;
   }

   private JComponent build(Page page)
   {
      return switch (page)
      {
         case INFO -> infoPage();
         case OPEN -> openPage();
         case PRINT ->
         {
            printPage = new PrintPage(host);
            yield printPage;
         }
         case EXPORT -> new ExportPage(host);
         case OPTIONS -> optionsPage();
      };
   }

   // ------------------------------------------------------------------ Seiten

   static JPanel pagePanel(String title)
   {
      JPanel p = new JPanel(new BorderLayout());
      p.setBackground(CONTENT_BG);
      p.setBorder(BorderFactory.createEmptyBorder(22, 36, 20, 36));
      JLabel t = new JLabel(title);
      t.setFont(TITLE_FONT);
      t.setForeground(new Color(0x262626));
      t.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));
      p.add(t, BorderLayout.NORTH);
      return p;
   }

   static JLabel section(String text)
   {
      JLabel l = new JLabel(text);
      l.setFont(SECTION_FONT);
      l.setForeground(new Color(0x262626));
      l.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
      l.setAlignmentX(Component.LEFT_ALIGNMENT);
      return l;
   }

   private JComponent infoPage()
   {
      JPanel page = pagePanel(I18n.t("backstage.info"));
      ProjectDocument doc = host.document();
      if (doc == null)
      {
         JLabel none = new JLabel(I18n.t("info.noDocument"));
         none.setVerticalAlignment(SwingConstants.TOP);
         page.add(none, BorderLayout.CENTER);
         return page;
      }
      ProjectInfoPanel info = new ProjectInfoPanel(doc);
      JScrollPane scroll = new JScrollPane(info);
      scroll.setBorder(BorderFactory.createEmptyBorder());
      scroll.getViewport().setBackground(CONTENT_BG);
      page.add(scroll, BorderLayout.CENTER);
      return page;
   }

   private JComponent openPage()
   {
      JPanel page = pagePanel(I18n.t("backstage.open"));
      JPanel body = new JPanel();
      body.setBackground(CONTENT_BG);
      body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

      JButton browse = tileButton(Icons.Name.OPEN, I18n.t("open.browse"), I18n.t("open.browse.hint"));
      browse.addActionListener(e -> host.openFileDialog());
      body.add(browse);
      body.add(Box.createVerticalStrut(18));
      body.add(section(I18n.t("open.recent")));
      List<File> recent = host.settings().getRecentFiles();
      if (recent.isEmpty())
      {
         JLabel none = new JLabel(I18n.t("open.recent.none"));
         none.setForeground(Style.TEXT_MUTED);
         none.setAlignmentX(Component.LEFT_ALIGNMENT);
         body.add(none);
      }
      for (File f : recent)
      {
         JButton b = recentButton(f);
         b.addActionListener(e -> host.openFile(f));
         body.add(b);
      }
      body.add(Box.createVerticalStrut(18));
      JLabel dnd = new JLabel(I18n.t("open.dragDrop"));
      dnd.setForeground(Style.TEXT_MUTED);
      dnd.setAlignmentX(Component.LEFT_ALIGNMENT);
      body.add(dnd);

      JScrollPane scroll = new JScrollPane(body);
      scroll.setBorder(BorderFactory.createEmptyBorder());
      scroll.getViewport().setBackground(CONTENT_BG);
      page.add(scroll, BorderLayout.CENTER);
      return page;
   }

   static JButton tileButton(Icons.Name icon, String title, String description)
   {
      String html = "<html><b style='font-size:110%'>" + esc(title) + "</b><br><span style='color:#666666'>" + esc(description) + "</span></html>";
      JButton b = new JButton(html, Icons.get(icon, 32));
      b.putClientProperty("JButton.buttonType", "toolBarButton");
      b.setHorizontalAlignment(SwingConstants.LEFT);
      b.setIconTextGap(14);
      b.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 16));
      b.setAlignmentX(Component.LEFT_ALIGNMENT);
      b.setMaximumSize(new Dimension(560, 70));
      b.setPreferredSize(new Dimension(520, 64));
      b.setFocusPainted(true);
      b.getAccessibleContext().setAccessibleName(title);
      return b;
   }

   private static JButton recentButton(File f)
   {
      String parent = f.getParent() == null ? "" : f.getParent();
      String html = "<html><b>" + esc(f.getName()) + "</b><br><span style='color:#666666'>" + esc(parent) + "</span></html>";
      JButton b = new JButton(html, Icons.get(Icons.Name.GANTT, 24));
      b.putClientProperty("JButton.buttonType", "toolBarButton");
      b.setHorizontalAlignment(SwingConstants.LEFT);
      b.setIconTextGap(12);
      b.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
      b.setAlignmentX(Component.LEFT_ALIGNMENT);
      b.setMaximumSize(new Dimension(760, 52));
      b.setToolTipText(f.getAbsolutePath());
      b.setEnabled(f.isFile());
      b.getAccessibleContext().setAccessibleName(f.getName());
      return b;
   }

   private JComponent optionsPage()
   {
      JPanel page = pagePanel(I18n.t("backstage.options"));
      JPanel body = new JPanel();
      body.setBackground(CONTENT_BG);
      body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

      body.add(section(I18n.t("options.language")));
      ButtonGroup group = new ButtonGroup();
      for (Language lang : Language.values())
      {
         JRadioButton rb = new JRadioButton(lang.displayName(), I18n.language() == lang);
         rb.setOpaque(false);
         rb.setAlignmentX(Component.LEFT_ALIGNMENT);
         rb.addActionListener(e ->
         {
            if (I18n.language() != lang)
            {
               host.setLanguage(lang);
            }
         });
         group.add(rb);
         body.add(rb);
      }
      JLabel hint = new JLabel(I18n.t("options.language.hint"));
      hint.setForeground(Style.TEXT_MUTED);
      hint.setAlignmentX(Component.LEFT_ALIGNMENT);
      hint.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 0));
      body.add(hint);

      body.add(Box.createVerticalStrut(16));
      JSeparator sep = new JSeparator();
      sep.setMaximumSize(new Dimension(700, 2));
      sep.setAlignmentX(Component.LEFT_ALIGNMENT);
      body.add(sep);
      body.add(section(I18n.t("options.about")));
      JLabel about = new JLabel("<html>" + I18n.t("options.about.text", AppInfo.version()) + "</html>");
      about.setAlignmentX(Component.LEFT_ALIGNMENT);
      about.setMaximumSize(new Dimension(700, 400));
      body.add(about);
      if (host.settings().getFile() != null)
      {
         body.add(Box.createVerticalStrut(10));
         JLabel file = new JLabel(I18n.t("options.settingsFile", host.settings().getFile().toString()));
         file.setForeground(Style.TEXT_MUTED);
         file.setAlignmentX(Component.LEFT_ALIGNMENT);
         body.add(file);
      }

      JScrollPane scroll = new JScrollPane(body);
      scroll.setBorder(BorderFactory.createEmptyBorder());
      scroll.getViewport().setBackground(CONTENT_BG);
      page.add(scroll, BorderLayout.CENTER);
      return page;
   }

   static String esc(String s)
   {
      return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
   }

   // ------------------------------------------------------------------ Navigation

   private static final class NavItem extends JPanel
   {
      private static final long serialVersionUID = 1L;
      private final String text;
      private final Icon icon;
      private boolean hover;
      private boolean selected;

      NavItem(String text, Icon icon, Runnable action)
      {
         this.text = text;
         this.icon = icon;
         setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
         setAlignmentX(Component.LEFT_ALIGNMENT);
         setFocusable(true);
         setOpaque(false);
         getAccessibleContext().setAccessibleName(text);
         addFocusListener(new java.awt.event.FocusAdapter()
         {
            @Override
            public void focusGained(java.awt.event.FocusEvent e)
            {
               repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e)
            {
               repaint();
            }
         });
         addMouseListener(new MouseAdapter()
         {
            @Override
            public void mouseEntered(MouseEvent e)
            {
               hover = true;
               repaint();
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
               hover = false;
               repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e)
            {
               if (isEnabled())
               {
                  action.run();
               }
            }
         });
         getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "activate");
         getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "activate");
         getActionMap().put("activate", new javax.swing.AbstractAction()
         {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e)
            {
               if (isEnabled())
               {
                  action.run();
               }
            }
         });
      }

      void setSelected(boolean value)
      {
         selected = value;
         repaint();
      }

      @Override
      public Dimension getPreferredSize()
      {
         return new Dimension(200, icon != null ? 46 : 40);
      }

      @Override
      public Dimension getMaximumSize()
      {
         return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
      }

      @Override
      protected void paintComponent(Graphics g)
      {
         if (selected)
         {
            g.setColor(Style.ACCENT_DARK);
            g.fillRect(0, 0, getWidth(), getHeight());
         }
         else if (hover && isEnabled())
         {
            g.setColor(Style.ACCENT_HOVER);
            g.fillRect(0, 0, getWidth(), getHeight());
         }
         if (icon != null)
         {
            icon.paintIcon(this, g, 22, (getHeight() - icon.getIconHeight()) / 2);
         }
         if (!text.isEmpty())
         {
            ((java.awt.Graphics2D) g).setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(Style.UI_FONT.deriveFont(14.5f));
            g.setColor(isEnabled() ? Color.WHITE : new Color(255, 255, 255, 110));
            java.awt.FontMetrics fm = g.getFontMetrics();
            g.drawString(text, 28, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
         }
         if (isFocusOwner())
         {
            g.setColor(new Color(255, 255, 255, 140));
            g.drawRect(2, 2, getWidth() - 5, getHeight() - 5);
         }
      }
   }
}
