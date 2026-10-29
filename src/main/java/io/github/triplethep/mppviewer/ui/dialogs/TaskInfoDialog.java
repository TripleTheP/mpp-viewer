package io.github.triplethep.mppviewer.ui.dialogs;

import io.github.triplethep.mppviewer.core.Formats;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.ui.AppInfo;
import org.mpxj.Relation;
import org.mpxj.Resource;
import org.mpxj.ResourceAssignment;
import org.mpxj.Task;
import org.mpxj.TaskMode;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * „Informationen zum Vorgang“ – wie in Project, aber schreibgeschützt.
 */
public final class TaskInfoDialog extends JDialog
{
   private static final long serialVersionUID = 1L;
   private final JTabbedPane tabs = new JTabbedPane();

   public TaskInfoDialog(Window owner, Task task, Formats fmt)
   {
      super(owner, I18n.t("taskInfo.title") + " – " + AppInfo.NAME, ModalityType.APPLICATION_MODAL);
      tabs.addTab(I18n.t("taskInfo.general"), general(task, fmt));
      tabs.addTab(I18n.t("taskInfo.predecessors"), predecessors(task, fmt));
      tabs.addTab(I18n.t("taskInfo.resources"), resources(task, fmt));
      tabs.addTab(I18n.t("taskInfo.advanced"), advanced(task, fmt));
      tabs.addTab(I18n.t("taskInfo.notes"), notes(task.getNotes()));

      JPanel root = new JPanel(new BorderLayout());
      root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      InfoForm head = new InfoForm();
      head.setBorder(BorderFactory.createEmptyBorder(0, 2, 8, 2));
      head.wide(I18n.t("field.task.NAME"), task.getName());
      head.half(I18n.t("field.task.DURATION"), fmt.duration(task.getDuration(), task.getEstimated()));
      head.check(I18n.t("taskInfo.estimated"), task.getEstimated());
      root.add(head, BorderLayout.NORTH);
      root.add(tabs, BorderLayout.CENTER);

      JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
      buttons.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
      JButton ok = new JButton(I18n.t("common.close"));
      ok.addActionListener(e -> dispose());
      buttons.add(ok);
      root.add(buttons, BorderLayout.SOUTH);
      setContentPane(root);
      getRootPane().setDefaultButton(ok);
      closeOnEscape(this);
      setSize(new Dimension(620, 470));
      setLocationRelativeTo(owner);
   }

   /**
    * Registerkarte vorauswählen (0 = Allgemein … 4 = Notizen).
    */
   public void selectTab(int index)
   {
      if (index >= 0 && index < tabs.getTabCount())
      {
         tabs.setSelectedIndex(index);
      }
   }

   static void closeOnEscape(JDialog dialog)
   {
      dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
      dialog.getRootPane().getActionMap().put("close", new javax.swing.AbstractAction()
      {
         private static final long serialVersionUID = 1L;

         @Override
         public void actionPerformed(ActionEvent e)
         {
            dialog.dispose();
         }
      });
   }

   private static JComponent general(Task task, Formats fmt)
   {
      InfoForm f = new InfoForm();
      f.half(I18n.t("field.task.PERCENT_COMPLETE"), fmt.percent(task.getPercentageComplete()));
      f.half(I18n.t("field.task.PRIORITY"), fmt.priority(task.getPriority()));
      f.half(I18n.t("field.task.TASK_MODE"), fmt.taskMode(task.getTaskMode()));
      f.check(I18n.t("taskInfo.inactive"), !task.getActive());
      f.half(I18n.t("field.task.START"), task.getStart() == null && task.getTaskMode() == TaskMode.MANUALLY_SCHEDULED ? "" : fmt.dateTime(task.getStart()));
      f.half(I18n.t("field.task.FINISH"), task.getFinish() == null && task.getTaskMode() == TaskMode.MANUALLY_SCHEDULED ? "" : fmt.dateTime(task.getFinish()));
      f.half(I18n.t("field.task.ACTUAL_START"), fmt.date(task.getActualStart()));
      f.half(I18n.t("field.task.ACTUAL_FINISH"), fmt.date(task.getActualFinish()));
      f.half(I18n.t("field.task.WORK"), fmt.work(task.getWork()));
      f.half(I18n.t("field.task.COST"), fmt.currency(task.getCost()));
      f.check(I18n.t("taskInfo.hideBar"), task.getHideBar());
      f.check(I18n.t("taskInfo.rollup"), task.getRollup());
      return f.finish();
   }

   private static JComponent predecessors(Task task, Formats fmt)
   {
      DefaultTableModel model = readOnlyModel(I18n.t("field.task.ID"), I18n.t("field.task.NAME"), I18n.t("taskInfo.type"), I18n.t("taskInfo.lag"));
      List<Relation> preds = task.getPredecessors();
      if (preds != null)
      {
         for (Relation r : preds)
         {
            Task p = r.getPredecessorTask();
            model.addRow(new Object[]
            {
               p == null ? "" : p.getID(),
               p == null ? "" : p.getName(),
               fmt.relationTypeLong(r.getType()) + " (" + fmt.relationType(r.getType()) + ")",
               r.getLag() == null || r.getLag().getDuration() == 0 ? fmt.duration(org.mpxj.Duration.getInstance(0, r.getLag() == null ? org.mpxj.TimeUnit.DAYS : r.getLag().getUnits())) : fmt.lag(r.getLag())
            });
         }
      }
      return table(model, 50, 260, 160, 90);
   }

   private static JComponent resources(Task task, Formats fmt)
   {
      DefaultTableModel model = readOnlyModel(I18n.t("field.resource.NAME"), I18n.t("taskInfo.units"), I18n.t("field.task.WORK"), I18n.t("field.task.COST"));
      List<ResourceAssignment> list = task.getResourceAssignments();
      if (list != null)
      {
         for (ResourceAssignment a : list)
         {
            Resource r = a.getResource();
            if (r == null)
            {
               continue;
            }
            model.addRow(new Object[]
            {
               r.getName(),
               a.getUnits() == null ? "" : fmt.percent(a.getUnits()),
               fmt.work(a.getWork()),
               fmt.currency(a.getCost())
            });
         }
      }
      return table(model, 240, 90, 100, 100);
   }

   private static JComponent advanced(Task task, Formats fmt)
   {
      InfoForm f = new InfoForm();
      f.half(I18n.t("field.task.DEADLINE"), task.getDeadline() == null ? fmt.na() : fmt.date(task.getDeadline()));
      f.half(I18n.t("field.task.WBS"), task.getWBS());
      f.half(I18n.t("field.task.CONSTRAINT_TYPE"), fmt.constraint(task.getConstraintType()));
      f.half(I18n.t("field.task.CONSTRAINT_DATE"), task.getConstraintDate() == null ? fmt.na() : fmt.date(task.getConstraintDate()));
      f.half(I18n.t("field.task.TYPE"), fmt.taskType(task.getType()));
      f.half(I18n.t("field.task.OUTLINE_NUMBER"), task.getOutlineNumber());
      f.half(I18n.t("field.task.CALENDAR"), task.getCalendar() == null ? I18n.t("taskInfo.noCalendar") : task.getCalendar().getName());
      f.half(I18n.t("field.task.UNIQUE_ID"), task.getUniqueID() == null ? "" : task.getUniqueID().toString());
      f.check(I18n.t("field.task.MILESTONE"), task.getMilestone());
      f.check(I18n.t("field.task.CRITICAL"), task.getCritical());
      f.half(I18n.t("field.task.TOTAL_SLACK"), fmt.duration(task.getTotalSlack()));
      f.half(I18n.t("field.task.FREE_SLACK"), fmt.duration(task.getFreeSlack()));
      if (task.getHyperlink() != null && !task.getHyperlink().isBlank())
      {
         f.wide(I18n.t("field.task.HYPERLINK"), task.getHyperlink() + (task.getHyperlinkAddress() == null ? "" : " (" + task.getHyperlinkAddress() + ")"));
      }
      return f.finish();
   }

   static JComponent notes(String text)
   {
      JTextArea area = new JTextArea(text == null ? "" : text.strip());
      area.setEditable(false);
      area.setLineWrap(true);
      area.setWrapStyleWord(true);
      area.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
      area.getAccessibleContext().setAccessibleName(I18n.t("taskInfo.notes"));
      JScrollPane scroll = new JScrollPane(area);
      scroll.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      return scroll;
   }

   static DefaultTableModel readOnlyModel(String... columns)
   {
      return new DefaultTableModel(columns, 0)
      {
         private static final long serialVersionUID = 1L;

         @Override
         public boolean isCellEditable(int row, int column)
         {
            return false;
         }
      };
   }

   static JComponent table(DefaultTableModel model, int... widths)
   {
      JTable table = new JTable(model);
      table.setRowHeight(22);
      table.getTableHeader().setReorderingAllowed(false);
      for (int i = 0; i < widths.length && i < table.getColumnCount(); i++)
      {
         table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
      }
      JScrollPane scroll = new JScrollPane(table);
      scroll.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      return scroll;
   }
}
