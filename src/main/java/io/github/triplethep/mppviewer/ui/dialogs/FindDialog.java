package io.github.triplethep.mppviewer.ui.dialogs;

import io.github.triplethep.mppviewer.core.ProjectDocument;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.ui.AppInfo;
import org.mpxj.Task;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * „Suchen“ (Strg+F): findet Vorgänge nach Name, auch in zugeklappten Sammelvorgängen.
 */
public final class FindDialog extends JDialog
{
   private static final long serialVersionUID = 1L;

   private final ProjectDocument doc;
   private final Supplier<Task> current;
   private final Consumer<Task> reveal;
   private final JTextField text = new JTextField(26);
   private final JCheckBox matchCase = new JCheckBox();
   private final JLabel result = new JLabel(" ");

   /**
    * @param current liefert den aktuell markierten Vorgang (Startpunkt der Suche)
    * @param reveal  klappt die Gliederung auf und markiert den gefundenen Vorgang
    */
   public FindDialog(Window owner, ProjectDocument doc, Supplier<Task> current, Consumer<Task> reveal)
   {
      super(owner, I18n.t("find.title") + " – " + AppInfo.NAME, ModalityType.MODELESS);
      this.doc = doc;
      this.current = current;
      this.reveal = reveal;

      JPanel root = new JPanel(new BorderLayout(0, 8));
      root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
      JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
      JLabel label = new JLabel(I18n.t("find.label"));
      label.setLabelFor(text);
      row.add(label);
      row.add(text);
      root.add(row, BorderLayout.NORTH);

      matchCase.setText(I18n.t("find.matchCase"));
      JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
      options.add(matchCase);
      result.setForeground(new Color(0x7A7A7A));
      options.add(result);
      root.add(options, BorderLayout.CENTER);

      JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
      JButton prev = new JButton(I18n.t("find.prev"));
      JButton next = new JButton(I18n.t("find.next"));
      JButton close = new JButton(I18n.t("common.close"));
      prev.addActionListener(e -> find(false));
      next.addActionListener(e -> find(true));
      close.addActionListener(e -> dispose());
      buttons.add(prev);
      buttons.add(next);
      buttons.add(close);
      root.add(buttons, BorderLayout.SOUTH);
      setContentPane(root);
      getRootPane().setDefaultButton(next);
      TaskInfoDialog.closeOnEscape(this);
      pack();
      setResizable(false);
      setLocationRelativeTo(owner);
   }

   public void focusText()
   {
      text.requestFocusInWindow();
      text.selectAll();
   }

   private void find(boolean forward)
   {
      String query = text.getText();
      if (query == null || query.isEmpty())
      {
         return;
      }
      List<Task> tasks = doc.getTasks();
      if (tasks.isEmpty())
      {
         return;
      }
      boolean cs = matchCase.isSelected();
      String q = cs ? query : query.toLowerCase(Locale.ROOT);
      int start = tasks.indexOf(current.get());
      int n = tasks.size();
      for (int step = 1; step <= n; step++)
      {
         int i = Math.floorMod(start + (forward ? step : -step), n);
         if (start < 0)
         {
            i = forward ? step - 1 : n - step;
         }
         Task t = tasks.get(i);
         String name = t.getName();
         if (name == null)
         {
            continue;
         }
         if ((cs ? name : name.toLowerCase(Locale.ROOT)).contains(q))
         {
            result.setText(" ");
            reveal.accept(t);
            return;
         }
      }
      result.setText(I18n.t("find.notFound"));
   }
}
