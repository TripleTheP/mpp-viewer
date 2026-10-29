package io.github.triplethep.mppviewer.core;

import io.github.triplethep.mppviewer.SampleProject;
import org.junit.jupiter.api.Test;
import org.mpxj.Task;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskOutlineTest
{
   @Test
   void collapseAndExpandSummaryTasks() throws Exception
   {
      ProjectDocument doc = ProjectDocument.of(new File("Beispielprojekt.xml"), SampleProject.create());
      TaskOutline outline = new TaskOutline(doc, false);
      int all = outline.getRows().size();
      assertEquals(doc.getTasks().size(), all);

      Task analyse = doc.getTasks().stream().filter(t -> "Analyse".equals(t.getName())).findFirst().orElseThrow();
      outline.toggle(analyse);
      assertEquals(all - analyse.getChildTasks().size(), outline.getRows().size());
      assertTrue(outline.indexOf(analyse) >= 0);
      assertEquals(-1, outline.indexOf(analyse.getChildTasks().get(0)));

      outline.toggle(analyse);
      assertEquals(all, outline.getRows().size());

      outline.showLevel(1);
      assertTrue(outline.getRows().stream().allMatch(r -> r.level() == 1));

      outline.showLevel(Integer.MAX_VALUE);
      assertEquals(all, outline.getRows().size());
   }

   @Test
   void sortAndFilterKeepOutlineStructure() throws Exception
   {
      ProjectDocument doc = ProjectDocument.of(new File("Beispielprojekt.xml"), SampleProject.create());
      TaskOutline outline = new TaskOutline(doc, false);
      TaskQuery query = new TaskQuery(doc, java.util.Locale.GERMANY);
      io.github.triplethep.mppviewer.render.CellPresenter presenter = new io.github.triplethep.mppviewer.render.CellPresenter(new Formats(io.github.triplethep.mppviewer.i18n.Language.DE, null));

      // Absteigend nach Name: Phasen untereinander sortiert, Teilvorgänge bleiben unter ihrer Phase
      query.sortBy(org.mpxj.TaskField.NAME, false);
      outline.setOrder(query.comparator());
      java.util.List<TaskRow> rows = outline.getRows();
      java.util.List<String> top = rows.stream().filter(r -> r.level() == 1).map(r -> r.task().getName()).toList();
      java.util.List<String> sortedTop = new java.util.ArrayList<>(top);
      sortedTop.sort(java.util.Comparator.<String>naturalOrder().reversed());
      assertEquals(sortedTop, top);
      for (int i = 0; i < rows.size(); i++)
      {
         if (rows.get(i).level() == 2)
         {
            assertTrue(rows.get(i - 1).level() >= 1);
         }
      }

      // AutoFilter auf Ressourcennamen: nur passende Vorgänge plus deren Sammelvorgänge
      query.setColumnFilter(org.mpxj.TaskField.RESOURCE_NAMES, java.util.Set.of("IT-Team"));
      outline.setFilter(query.predicate(presenter::taskText), true);
      java.util.List<String> names = outline.getRows().stream().map(r -> r.task().getName()).toList();
      assertTrue(names.contains("Datenmigration vorbereiten"));
      assertTrue(names.contains("Umsetzung"));
      assertFalse(names.contains("Anwendertests"));
      assertTrue(query.isFiltered());

      query.clearFilters();
      outline.setFilter(query.predicate(presenter::taskText), true);
      assertEquals(doc.getTasks().size(), outline.getRows().size());
   }
}
