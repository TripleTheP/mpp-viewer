package io.github.triplethep.mppviewer.ui;

import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.render.Style;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Startseite, solange keine Datei geöffnet ist.
 */
final class WelcomePanel extends JPanel
{
   private static final long serialVersionUID = 1L;

   WelcomePanel(List<File> recent, Runnable onOpen, Consumer<File> onOpenFile)
   {
      super(new GridBagLayout());
      setBackground(Color.WHITE);

      JPanel box = new JPanel();
      box.setOpaque(false);
      box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

      JLabel logo = new JLabel(new ImageIcon(Icons.appImage(72)));
      logo.setAlignmentX(Component.CENTER_ALIGNMENT);
      box.add(logo);
      box.add(Box.createVerticalStrut(14));

      JLabel title = new JLabel(AppInfo.NAME);
      title.setFont(Style.UI_FONT.deriveFont(Font.PLAIN, 30f));
      title.setForeground(new Color(0x262626));
      title.setAlignmentX(Component.CENTER_ALIGNMENT);
      box.add(title);
      box.add(Box.createVerticalStrut(6));

      JLabel subtitle = new JLabel(I18n.t("welcome.subtitle"));
      subtitle.setForeground(Style.TEXT_MUTED);
      subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
      box.add(subtitle);
      box.add(Box.createVerticalStrut(24));

      JButton open = new JButton(I18n.t("welcome.open"), Icons.get(Icons.Name.OPEN, 24));
      open.setFont(Style.UI_FONT.deriveFont(14f));
      open.putClientProperty("JButton.buttonType", "default");
      open.setAlignmentX(Component.CENTER_ALIGNMENT);
      open.setPreferredSize(new Dimension(260, 44));
      open.setMaximumSize(new Dimension(260, 44));
      open.addActionListener(e -> onOpen.run());
      box.add(open);
      box.add(Box.createVerticalStrut(10));

      JLabel dnd = new JLabel(I18n.t("open.dragDrop"));
      dnd.setForeground(Style.TEXT_MUTED);
      dnd.setAlignmentX(Component.CENTER_ALIGNMENT);
      box.add(dnd);

      if (!recent.isEmpty())
      {
         box.add(Box.createVerticalStrut(26));
         JLabel rl = new JLabel(I18n.t("open.recent"));
         rl.setFont(Style.UI_FONT.deriveFont(Font.BOLD, 13f));
         rl.setAlignmentX(Component.CENTER_ALIGNMENT);
         box.add(rl);
         box.add(Box.createVerticalStrut(6));
         int n = 0;
         for (File f : recent)
         {
            if (n++ >= 6)
            {
               break;
            }
            JButton b = new JButton(f.getName(), Icons.get(Icons.Name.GANTT, 16));
            b.putClientProperty("JButton.buttonType", "toolBarButton");
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setToolTipText(f.getAbsolutePath());
            b.setEnabled(f.isFile());
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(360, 28));
            b.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            b.addActionListener(e -> onOpenFile.accept(f));
            box.add(b);
         }
      }
      add(box);
   }
}
