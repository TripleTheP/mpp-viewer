package io.github.triplethep.mppviewer.core;

import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.i18n.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mpxj.Duration;
import org.mpxj.ProjectFile;
import org.mpxj.Relation;
import org.mpxj.RelationType;
import org.mpxj.Task;
import org.mpxj.TimeUnit;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatsTest
{
   @AfterEach
   void reset()
   {
      I18n.setLanguage(Language.DE);
   }

   @Test
   void germanDatesAndDurationsLookLikeProject()
   {
      Formats f = new Formats(Language.DE, null);
      assertEquals("Mo 26.10.26", f.date(LocalDateTime.of(2026, 10, 26, 8, 0)));
      assertEquals("26.10.", f.dateShort(LocalDateTime.of(2026, 10, 26, 8, 0)));
      assertEquals("5 Tage", f.duration(Duration.getInstance(5, TimeUnit.DAYS)));
      assertEquals("1 Tag", f.duration(Duration.getInstance(1, TimeUnit.DAYS)));
      assertEquals("0 Tage", f.duration(Duration.getInstance(0, TimeUnit.DAYS)));
      assertEquals("2,5 Std.", f.duration(Duration.getInstance(2.5, TimeUnit.HOURS)));
      assertEquals("3 Wochen?", f.duration(Duration.getInstance(3, TimeUnit.WEEKS), true));
      assertEquals("4 fTage", f.duration(Duration.getInstance(4, TimeUnit.ELAPSED_DAYS)));
      assertEquals("NV", f.date((LocalDateTime) null));
      assertEquals("1.234,50 €", f.currency(Double.valueOf(1234.5)));
      assertEquals("50%", f.percent(Integer.valueOf(50)));
   }

   @Test
   void englishDatesAndDurationsLookLikeProject()
   {
      Formats f = new Formats(Language.EN, null);
      assertEquals("Mon 10/26/26", f.date(LocalDateTime.of(2026, 10, 26, 8, 0)));
      assertEquals("5 days", f.duration(Duration.getInstance(5, TimeUnit.DAYS)));
      assertEquals("1 day", f.duration(Duration.getInstance(1, TimeUnit.DAYS)));
      assertEquals("8 hrs", f.duration(Duration.getInstance(8, TimeUnit.HOURS)));
      assertEquals("$1,234.50", f.currency(Double.valueOf(1234.5)));
      assertEquals("NA", f.date((LocalDateTime) null));
   }

   @Test
   void predecessorColumnUsesProjectNotation()
   {
      ProjectFile file = new ProjectFile();
      Task a = file.addTask();
      a.setID(Integer.valueOf(1));
      Task b = file.addTask();
      b.setID(Integer.valueOf(2));
      Task c = file.addTask();
      c.setID(Integer.valueOf(3));
      c.addPredecessor(new Relation.Builder().predecessorTask(a));
      c.addPredecessor(new Relation.Builder().predecessorTask(b).type(RelationType.START_START).lag(Duration.getInstance(2, TimeUnit.DAYS)));

      assertEquals("1;2AA+2 Tage", new Formats(Language.DE, null).predecessors(c));
      assertEquals("1,2SS+2 days", new Formats(Language.EN, null).predecessors(c));
   }
}
