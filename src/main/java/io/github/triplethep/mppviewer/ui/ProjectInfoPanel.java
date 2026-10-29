package io.github.triplethep.mppviewer.ui;

import io.github.triplethep.mppviewer.core.Formats;
import io.github.triplethep.mppviewer.core.ProjectDocument;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.render.Style;
import org.mpxj.ProjectCalendar;
import org.mpxj.ProjectProperties;
import org.mpxj.ScheduleFrom;
import org.mpxj.Task;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;

/**
 * Projektinformationen und Statistik (schreibgeschützt), angelehnt an „Projektinformationen“ und
 * „Projektstatistik“ in Project.
 */
public final class ProjectInfoPanel extends JPanel
{
   private static final long serialVersionUID = 1L;
   private int row;

   public ProjectInfoPanel(ProjectDocument doc)
   {
      super(new GridBagLayout());
      setBackground(Color.WHITE);
      setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));
      Formats fmt = Formats.current(doc.getProperties());
      ProjectProperties p = doc.getProperties();

      section(I18n.t("info.section.project"));
      field(I18n.t("info.title"), doc.getTitle());
      field(I18n.t("info.file"), doc.getFile() == null ? "" : doc.getFile().getAbsolutePath());
      field(I18n.t("info.subject"), p.getSubject());
      field(I18n.t("info.author"), p.getAuthor());
      field(I18n.t("info.manager"), p.getManager());
      field(I18n.t("info.company"), p.getCompany());
      field(I18n.t("info.category"), p.getCategory());
      field(I18n.t("info.comments"), oneLine(p.getComments()));

      section(I18n.t("info.section.dates"));
      field(I18n.t("info.start"), fmt.date(doc.getStart()));
      field(I18n.t("info.finish"), fmt.date(doc.getFinish()));
      field(I18n.t("info.statusDate"), date(fmt, p.getStatusDate()));
      field(I18n.t("info.currentDate"), date(fmt, p.getCurrentDate()));
      ScheduleFrom from = p.getScheduleFrom();
      field(I18n.t("info.scheduleFrom"), from == ScheduleFrom.FINISH ? I18n.t("info.scheduleFrom.finish") : I18n.t("info.scheduleFrom.start"));
      ProjectCalendar cal = p.getDefaultCalendar();
      field(I18n.t("info.calendar"), cal == null ? "" : cal.getName());

      section(I18n.t("info.section.statistics"));
      Task summary = doc.getProjectSummaryTask();
      if (summary != null)
      {
         field(I18n.t("field.task.DURATION"), fmt.duration(summary.getDuration()));
         field(I18n.t("field.task.WORK"), fmt.work(summary.getWork()));
         field(I18n.t("field.task.COST"), fmt.currency(summary.getCost()));
         field(I18n.t("field.task.PERCENT_COMPLETE"), fmt.percent(summary.getPercentageComplete()));
         if (summary.getBaselineStart() != null)
         {
            field(I18n.t("field.task.BASELINE_START"), fmt.date(summary.getBaselineStart()));
            field(I18n.t("field.task.BASELINE_FINISH"), fmt.date(summary.getBaselineFinish()));
         }
      }
      int milestones = 0;
      int summaries = 0;
      for (Task t : doc.getTasks())
      {
         if (t.getMilestone())
         {
            milestones++;
         }
         if (t.getSummary())
         {
            summaries++;
         }
      }
      field(I18n.t("info.taskCount"), Integer.toString(doc.getTasks().size()));
      field(I18n.t("info.summaryCount"), Integer.toString(summaries));
      field(I18n.t("info.milestoneCount"), Integer.toString(milestones));
      field(I18n.t("info.resourceCount"), Integer.toString(doc.getResources().size()));

      section(I18n.t("info.section.file"));
      field(I18n.t("info.created"), dateTime(fmt, p.getCreationDate()));
      field(I18n.t("info.lastSaved"), dateTime(fmt, p.getLastSaved()));
      field(I18n.t("info.lastAuthor"), p.getLastAuthor());
      String app = p.getFullApplicationName();
      if (app == null || app.isBlank())
      {
         app = p.getFileApplication();
      }
      field(I18n.t("info.application"), app);
      field(I18n.t("info.format"), p.getFileType());

      GridBagConstraints filler = new GridBagConstraints();
      filler.gridy = row++;
      filler.weighty = 1;
      add(new JLabel(), filler);
   }

   private static String date(Formats fmt, LocalDateTime value)
   {
      return value == null ? "" : fmt.date(value);
   }

   private static String dateTime(Formats fmt, LocalDateTime value)
   {
      return value == null ? "" : fmt.dateTime(value);
   }

   private static String oneLine(String s)
   {
      return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ').strip();
   }

   private void section(String title)
   {
      JLabel l = new JLabel(title);
      l.setFont(Style.UI_FONT.deriveFont(Font.PLAIN, 16f));
      l.setForeground(Style.ACCENT);
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = row++;
      c.gridwidth = 2;
      c.anchor = GridBagConstraints.WEST;
      c.insets = new Insets(row == 1 ? 0 : 16, 0, 6, 0);
      add(l, c);
   }

   private void field(String label, String value)
   {
      JLabel l = new JLabel(label);
      l.setForeground(new Color(0x505050));
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = row;
      c.anchor = GridBagConstraints.WEST;
      c.insets = new Insets(2, 0, 2, 16);
      add(l, c);

      JTextField t = new JTextField(value == null ? "" : value);
      t.setEditable(false);
      t.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
      t.setBackground(Color.WHITE);
      t.getAccessibleContext().setAccessibleName(label);
      c = new GridBagConstraints();
      c.gridx = 1;
      c.gridy = row++;
      c.weightx = 1;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.anchor = GridBagConstraints.WEST;
      c.insets = new Insets(2, 0, 2, 0);
      add(t, c);
   }
}
