package io.github.triplethep.mppviewer.ui.dialogs;

import io.github.triplethep.mppviewer.core.ProjectDocument;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.ui.AppInfo;
import io.github.triplethep.mppviewer.ui.ProjectInfoPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;

/**
 * „Projektinformationen“ (schreibgeschützt).
 */
public final class ProjectInfoDialog extends JDialog
{
   private static final long serialVersionUID = 1L;

   public ProjectInfoDialog(Window owner, ProjectDocument doc)
   {
      super(owner, I18n.t("projectInfo.title", doc.getTitle()) + " – " + AppInfo.NAME, ModalityType.APPLICATION_MODAL);
      JPanel root = new JPanel(new BorderLayout());
      root.setBackground(Color.WHITE);
      root.setBorder(BorderFactory.createEmptyBorder(14, 16, 10, 16));
      JScrollPane scroll = new JScrollPane(new ProjectInfoPanel(doc));
      scroll.setBorder(BorderFactory.createEmptyBorder());
      scroll.getViewport().setBackground(Color.WHITE);
      scroll.getVerticalScrollBar().setUnitIncrement(16);
      root.add(scroll, BorderLayout.CENTER);
      JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
      buttons.setOpaque(false);
      buttons.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
      JButton ok = new JButton(I18n.t("common.close"));
      ok.addActionListener(e -> dispose());
      buttons.add(ok);
      root.add(buttons, BorderLayout.SOUTH);
      setContentPane(root);
      getRootPane().setDefaultButton(ok);
      TaskInfoDialog.closeOnEscape(this);
      setSize(new Dimension(620, 640));
      setLocationRelativeTo(owner);
   }
}
