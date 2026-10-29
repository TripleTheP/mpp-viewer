package io.github.triplethep.mppviewer.ui.dialogs;

import io.github.triplethep.mppviewer.core.Formats;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.ui.AppInfo;
import org.mpxj.Resource;
import org.mpxj.ResourceAssignment;
import org.mpxj.Task;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;

/**
 * „Informationen zur Ressource“ (schreibgeschützt).
 */
public final class ResourceInfoDialog extends JDialog
{
   private static final long serialVersionUID = 1L;

   public ResourceInfoDialog(Window owner, Resource resource, Formats fmt)
   {
      super(owner, I18n.t("resourceInfo.title") + " – " + AppInfo.NAME, ModalityType.APPLICATION_MODAL);
      JTabbedPane tabs = new JTabbedPane();
      tabs.addTab(I18n.t("taskInfo.general"), general(resource, fmt));
      tabs.addTab(I18n.t("resourceInfo.assignments"), assignments(resource, fmt));
      tabs.addTab(I18n.t("taskInfo.notes"), TaskInfoDialog.notes(resource.getNotes()));

      JPanel root = new JPanel(new BorderLayout());
      root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      root.add(tabs, BorderLayout.CENTER);
      JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
      buttons.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
      JButton ok = new JButton(I18n.t("common.close"));
      ok.addActionListener(e -> dispose());
      buttons.add(ok);
      root.add(buttons, BorderLayout.SOUTH);
      setContentPane(root);
      getRootPane().setDefaultButton(ok);
      TaskInfoDialog.closeOnEscape(this);
      setSize(new Dimension(600, 430));
      setLocationRelativeTo(owner);
   }

   private static JComponent general(Resource r, Formats fmt)
   {
      InfoForm f = new InfoForm();
      f.wide(I18n.t("field.resource.NAME"), r.getName());
      f.half(I18n.t("field.resource.INITIALS"), r.getInitials());
      f.half(I18n.t("field.resource.TYPE"), fmt.resourceType(r.getType()));
      f.half(I18n.t("field.resource.GROUP"), r.getGroup());
      f.half(I18n.t("field.resource.CODE"), r.getCode());
      f.half(I18n.t("field.resource.EMAIL_ADDRESS"), r.getEmailAddress());
      f.half(I18n.t("field.resource.MAX_UNITS"), r.getMaxUnits() == null ? "" : fmt.percent(r.getMaxUnits()));
      f.half(I18n.t("field.resource.STANDARD_RATE"), fmt.rate(r.getStandardRate()));
      f.half(I18n.t("field.resource.OVERTIME_RATE"), fmt.rate(r.getOvertimeRate()));
      f.half(I18n.t("field.resource.COST_PER_USE"), fmt.currency(r.getCostPerUse()));
      f.half(I18n.t("field.resource.ACCRUE_AT"), fmt.accrue(r.getAccrueAt()));
      f.half(I18n.t("field.resource.BASE_CALENDAR"), r.getCalendar() == null ? "" : r.getCalendar().getName());
      f.half(I18n.t("field.resource.WORK"), fmt.work(r.getWork()));
      f.check(I18n.t("resourceInfo.generic"), r.getGeneric());
      f.check(I18n.t("taskInfo.inactive"), !r.getActive());
      return f.finish();
   }

   private static JComponent assignments(Resource r, Formats fmt)
   {
      DefaultTableModel model = TaskInfoDialog.readOnlyModel(I18n.t("field.task.ID"), I18n.t("field.task.NAME"), I18n.t("taskInfo.units"), I18n.t("field.task.WORK"), I18n.t("field.task.START"), I18n.t("field.task.FINISH"));
      for (ResourceAssignment a : r.getTaskAssignments())
      {
         Task t = a.getTask();
         if (t == null)
         {
            continue;
         }
         model.addRow(new Object[]
         {
            t.getID(),
            t.getName(),
            a.getUnits() == null ? "" : fmt.percent(a.getUnits()),
            fmt.work(a.getWork()),
            fmt.date(a.getStart()),
            fmt.date(a.getFinish())
         });
      }
      return TaskInfoDialog.table(model, 40, 200, 70, 80, 100, 100);
   }
}
