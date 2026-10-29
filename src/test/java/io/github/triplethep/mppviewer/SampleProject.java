package io.github.triplethep.mppviewer;

import org.mpxj.Availability;
import org.mpxj.ConstraintType;
import org.mpxj.CostRateTable;
import org.mpxj.CostRateTableEntry;
import org.mpxj.CurrencySymbolPosition;
import org.mpxj.Duration;
import org.mpxj.ProjectCalendar;
import org.mpxj.ProjectFile;
import org.mpxj.ProjectProperties;
import org.mpxj.Rate;
import org.mpxj.Relation;
import org.mpxj.RelationType;
import org.mpxj.Resource;
import org.mpxj.ResourceAssignment;
import org.mpxj.ResourceType;
import org.mpxj.Task;
import org.mpxj.TaskMode;
import org.mpxj.TimeUnit;
import org.mpxj.common.LocalDateTimeHelper;
import org.mpxj.mspdi.MSPDIWriter;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Erzeugt ein realistisches deutschsprachiges Beispielprojekt (Sammelvorgänge, Meilensteine, alle
 * Anordnungsbeziehungen, Ressourcen, Fortschritt, Basisplan, Stichtag, Notizen, manueller Vorgang).
 * <p>
 * Aufruf ohne Argumente schreibt {@code samples/Beispielprojekt.xml} (Project-XML, lässt sich auch in Project öffnen).
 */
public final class SampleProject
{
   public static final LocalDateTime START = LocalDateTime.of(2026, 9, 7, 8, 0);

   private final ProjectFile file = new ProjectFile();
   private final Map<String, Resource> resources = new HashMap<>();

   private SampleProject()
   {
   }

   public static void main(String[] args) throws Exception
   {
      File out = new File(args.length > 0 ? args[0] : "samples/Beispielprojekt.xml");
      out.getParentFile().mkdirs();
      new MSPDIWriter().write(create(), out);
      System.out.println("Geschrieben: " + out.getAbsolutePath());
   }

   public static ProjectFile create() throws Exception
   {
      return new SampleProject().build();
   }

   private ProjectFile build() throws Exception
   {
      ProjectProperties props = file.getProjectProperties();
      props.setProjectTitle("Einführung CRM-System");
      props.setAuthor("Projektbüro");
      props.setCompany("Musterfirma GmbH");
      props.setManager("Projektleitung");
      props.setStartDate(START);
      props.setCurrencySymbol("€");
      props.setSymbolPosition(CurrencySymbolPosition.AFTER_WITH_SPACE);
      props.setStatusDate(LocalDateTime.of(2026, 10, 29, 17, 0));

      ProjectCalendar cal = file.addDefaultBaseCalendar();
      cal.setName("Standard");
      file.setDefaultCalendar(cal);
      cal.addCalendarException(LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 25));
      cal.addCalendarException(LocalDate.of(2026, 12, 31));
      cal.addCalendarException(LocalDate.of(2027, 1, 1));
      cal.addCalendarException(LocalDate.of(2027, 1, 6));

      resource("Projektleitung", ResourceType.WORK, "PL", "Organisation", 95);
      resource("IT-Team", ResourceType.WORK, "IT", "IT", 85);
      resource("Fachabteilung Vertrieb", ResourceType.WORK, "VT", "Vertrieb", 70);
      resource("Externer Dienstleister", ResourceType.WORK, "EXT", "Extern", 120);
      resource("Schulungshandbuch", ResourceType.MATERIAL, "SH", "Material", 25);

      Task start = milestone(null, "Projektstart");

      Task p1 = summary("Analyse");
      Task kickoff = task(p1, "Kick-off-Workshop", 1, "Projektleitung", "Fachabteilung Vertrieb");
      link(start, kickoff, RelationType.FINISH_START, 0);
      Task ist = task(p1, "Ist-Analyse der Vertriebsprozesse", 5, "Fachabteilung Vertrieb");
      link(kickoff, ist, RelationType.FINISH_START, 0);
      Task req = task(p1, "Anforderungskatalog erstellen", 4, "Projektleitung");
      req.setNotes("Abstimmung mit Vertrieb und Innendienst.\nVorlage: Lastenheft-Muster aus dem Projekthandbuch.");
      link(ist, req, RelationType.FINISH_START, 0);
      Task reqDone = milestone(p1, "Anforderungen abgenommen");
      link(req, reqDone, RelationType.FINISH_START, 0);

      Task p2 = summary("Auswahl und Konzept");
      Task market = task(p2, "Marktrecherche Anbieter", 3, "Projektleitung");
      link(req, market, RelationType.START_START, 2);
      Task offers = task(p2, "Angebote einholen und bewerten", 6, "Projektleitung", "IT-Team");
      link(market, offers, RelationType.FINISH_START, 0);
      link(reqDone, offers, RelationType.FINISH_START, 0);
      Task concept = task(p2, "Fachkonzept", 8, "IT-Team", "Fachabteilung Vertrieb:50");
      link(offers, concept, RelationType.FINISH_START, 0);
      Task decision = milestone(p2, "Entscheidung Lenkungsausschuss");
      link(concept, decision, RelationType.FINISH_START, 0);
      decision.setDeadline(LocalDateTime.of(2026, 10, 30, 17, 0));

      Task p3 = summary("Umsetzung");
      Task setup = task(p3, "Systemeinrichtung", 10, "Externer Dienstleister");
      setup.setHyperlink("Leistungsbeschreibung");
      setup.setHyperlinkAddress("https://example.com/leistungsbeschreibung");
      link(decision, setup, RelationType.FINISH_START, 0);
      Task migPrep = task(p3, "Datenmigration vorbereiten", 6, "IT-Team");
      link(setup, migPrep, RelationType.START_START, 3);
      Task interfaces = task(p3, "Schnittstelle zum ERP-System", 8, "IT-Team", "Externer Dienstleister:50");
      link(setup, interfaces, RelationType.FINISH_START, -2);
      Task migTest = task(p3, "Testmigration", 3, "IT-Team");
      link(migPrep, migTest, RelationType.FINISH_START, 0);
      link(interfaces, migTest, RelationType.FINISH_START, 0);

      Task p4 = summary("Test und Schulung");
      Task uat = task(p4, "Anwendertests", 5, "Fachabteilung Vertrieb");
      link(migTest, uat, RelationType.FINISH_START, 0);
      Task training = task(p4, "Schulungen", 4, "Projektleitung");
      link(uat, training, RelationType.FINISH_FINISH, 1);
      Task golive = milestone(p4, "Go-live");
      link(training, golive, RelationType.FINISH_START, 0);

      Task p5 = summary("Projektabschluss");
      Task hyper = task(p5, "Hypercare-Support", 10, "Externer Dienstleister:50");
      link(golive, hyper, RelationType.FINISH_START, 0);
      Task lessons = task(p5, "Lessons Learned", 1, "Projektleitung");
      lessons.setTaskMode(TaskMode.MANUALLY_SCHEDULED);
      link(hyper, lessons, RelationType.FINISH_START, 0);
      Task end = milestone(p5, "Projektende");
      link(lessons, end, RelationType.FINISH_START, 0);
      end.setConstraintType(ConstraintType.FINISH_NO_LATER_THAN);
      end.setConstraintDate(LocalDateTime.of(2027, 2, 26, 17, 0));

      file.getTasks().synchronizeTaskIDToHierarchy();

      // Erst planen und als Basisplan sichern, dann Verzug einbauen und neu planen
      schedule();
      for (Task t : file.getTasks())
      {
         t.setBaselineStart(t.getStart());
         t.setBaselineFinish(t.getFinish());
         t.setBaselineDuration(t.getDuration());
      }
      setDuration(ist, 7);
      setDuration(concept, 9);
      schedule();

      progress(start, 100);
      progress(kickoff, 100);
      progress(ist, 100);
      progress(req, 100);
      progress(reqDone, 100);
      progress(market, 100);
      progress(offers, 100);
      progress(concept, 100);
      progress(decision, 100);
      progress(setup, 100);
      progress(migPrep, 100);
      progress(interfaces, 30);
      rollupProgress();
      return file;
   }

   /**
    * Einfache Vorwärtsrechnung in ganzen Arbeitstagen (08:00–17:00), ausreichend für ein Beispielprojekt.
    */
   private void schedule()
   {
      ProjectCalendar cal = file.getDefaultCalendar();
      for (Task t : file.getTasks())
      {
         if (t.getSummary() || t.getID() == null || t.getID().intValue() == 0)
         {
            continue;
         }
         int dur = (int) Math.round(t.getDuration().getDuration());
         LocalDate startDay = nextWorkingDay(cal, START.toLocalDate(), 0);
         boolean milestone = dur == 0;
         LocalDate msDay = null;
         for (Relation r : t.getPredecessors())
         {
            Task p = r.getPredecessorTask();
            int lag = r.getLag() == null ? 0 : (int) Math.round(r.getLag().getDuration());
            LocalDate pStart = p.getStart().toLocalDate();
            LocalDate pFinish = p.getFinish().toLocalDate();
            boolean pAtMorning = p.getMilestone() && p.getFinish().getHour() < 12;
            LocalDate candidate;
            switch (r.getType())
            {
               case START_START -> candidate = addWorkDays(cal, pStart, lag);
               case FINISH_FINISH -> candidate = addWorkDays(cal, addWorkDays(cal, pFinish, lag), -(Math.max(1, dur) - 1));
               default ->
               {
                  if (milestone && lag == 0)
                  {
                     msDay = msDay == null || pFinish.isAfter(msDay) ? pFinish : msDay;
                     candidate = pFinish;
                  }
                  else
                  {
                     candidate = addWorkDays(cal, pAtMorning ? pFinish.minusDays(1) : pFinish, 1 + lag);
                  }
               }
            }
            if (candidate.isAfter(startDay))
            {
               startDay = candidate;
            }
         }
         if (milestone)
         {
            LocalDate day = msDay != null ? msDay : startDay;
            LocalDateTime at = msDay != null ? day.atTime(17, 0) : day.atTime(8, 0);
            t.setStart(at);
            t.setFinish(at);
         }
         else
         {
            t.setStart(startDay.atTime(8, 0));
            t.setFinish(addWorkDays(cal, startDay, dur - 1).atTime(17, 0));
         }
      }
      for (Task t : file.getTasks())
      {
         rollup(t, cal);
      }
   }

   private static void rollup(Task t, ProjectCalendar cal)
   {
      if (!t.getSummary() || t.getChildTasks().isEmpty())
      {
         return;
      }
      LocalDateTime min = null;
      LocalDateTime max = null;
      for (Task c : t.getChildTasks())
      {
         rollup(c, cal);
         min = min == null || c.getStart().isBefore(min) ? c.getStart() : min;
         max = max == null || c.getFinish().isAfter(max) ? c.getFinish() : max;
      }
      t.setStart(min);
      t.setFinish(max);
      int days = 0;
      for (LocalDate d = min.toLocalDate(); !d.isAfter(max.toLocalDate()); d = d.plusDays(1))
      {
         if (cal.isWorkingDate(d))
         {
            days++;
         }
      }
      t.setDuration(Duration.getInstance(days, TimeUnit.DAYS));
   }

   private static LocalDate nextWorkingDay(ProjectCalendar cal, LocalDate d, int dummy)
   {
      LocalDate day = d;
      while (!cal.isWorkingDate(day))
      {
         day = day.plusDays(1);
      }
      return day;
   }

   /**
    * Verschiebt um n Arbeitstage (n darf negativ sein); n = 0 liefert den nächsten Arbeitstag.
    */
   private static LocalDate addWorkDays(ProjectCalendar cal, LocalDate d, int n)
   {
      LocalDate day = d;
      int step = n >= 0 ? 1 : -1;
      int remaining = Math.abs(n);
      while (remaining > 0)
      {
         day = day.plusDays(step);
         if (cal.isWorkingDate(day))
         {
            remaining--;
         }
      }
      while (!cal.isWorkingDate(day))
      {
         day = day.plusDays(step);
      }
      return day;
   }

   private Resource resource(String name, ResourceType type, String initials, String group, double rate)
   {
      Resource r = file.addResource();
      r.setName(name);
      r.setType(type);
      r.setInitials(initials);
      r.setGroup(group);
      CostRateTable table = new CostRateTable();
      if (type == ResourceType.WORK)
      {
         table.add(new CostRateTableEntry(LocalDateTimeHelper.START_DATE_NA, LocalDateTimeHelper.END_DATE_NA, Integer.valueOf(0), new Rate(Double.valueOf(rate), TimeUnit.HOURS), new Rate(Double.valueOf(rate * 1.25), TimeUnit.HOURS)));
         r.getAvailability().add(new Availability(LocalDateTimeHelper.START_DATE_NA, LocalDateTimeHelper.END_DATE_NA, Double.valueOf(100)));
      }
      else
      {
         table.add(new CostRateTableEntry(LocalDateTimeHelper.START_DATE_NA, LocalDateTimeHelper.END_DATE_NA, Integer.valueOf(0), new Rate(Double.valueOf(rate), TimeUnit.HOURS)));
      }
      r.setCostRateTable(0, table);
      resources.put(name, r);
      return r;
   }

   private Task summary(String name)
   {
      Task t = file.addTask();
      t.setName(name);
      t.setSummary(true);
      return t;
   }

   private Task task(Task parent, String name, int days, String... assignments)
   {
      Task t = parent == null ? file.addTask() : parent.addTask();
      t.setName(name);
      setDuration(t, days);
      t.setTaskMode(TaskMode.AUTO_SCHEDULED);
      for (String a : assignments)
      {
         String[] parts = a.split(":");
         Resource r = resources.get(parts[0]);
         ResourceAssignment ra = t.addResourceAssignment(r);
         double units = parts.length > 1 ? Double.parseDouble(parts[1]) : (r.getType() == ResourceType.MATERIAL ? 1 : 100);
         ra.setUnits(Double.valueOf(units));
         Duration work = Duration.getInstance(days * 8 * units / 100.0, TimeUnit.HOURS);
         ra.setWork(work);
         ra.setActualWork(Duration.getInstance(0, TimeUnit.HOURS));
         ra.setRemainingWork(work);
      }
      return t;
   }

   private Task milestone(Task parent, String name)
   {
      Task t = parent == null ? file.addTask() : parent.addTask();
      t.setName(name);
      setDuration(t, 0);
      t.setMilestone(true);
      t.setTaskMode(TaskMode.AUTO_SCHEDULED);
      return t;
   }

   private static void setDuration(Task t, int days)
   {
      t.setDuration(Duration.getInstance(days, TimeUnit.DAYS));
      t.setActualDuration(Duration.getInstance(0, TimeUnit.DAYS));
      t.setRemainingDuration(Duration.getInstance(days, TimeUnit.DAYS));
   }

   private static void link(Task pred, Task succ, RelationType type, int lagDays)
   {
      succ.addPredecessor(new Relation.Builder().predecessorTask(pred).type(type).lag(Duration.getInstance(lagDays, TimeUnit.DAYS)));
   }

   private static void progress(Task t, double pct)
   {
      t.setPercentageComplete(Double.valueOf(pct));
      if (pct > 0)
      {
         t.setActualStart(t.getStart());
      }
      if (pct >= 100)
      {
         t.setActualFinish(t.getFinish());
      }
   }

   private void rollupProgress()
   {
      for (Task t : file.getTasks())
      {
         if (!t.getSummary() || t.getChildTasks().isEmpty())
         {
            continue;
         }
         double total = 0;
         double done = 0;
         for (Task c : t.getChildTasks())
         {
            double d = c.getDuration() == null ? 0 : c.getDuration().getDuration();
            double p = c.getPercentageComplete() == null ? 0 : c.getPercentageComplete().doubleValue();
            total += d;
            done += d * p / 100.0;
         }
         t.setPercentageComplete(Double.valueOf(total == 0 ? 0 : Math.round(done / total * 100.0)));
      }
   }
}
