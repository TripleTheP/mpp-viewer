package io.github.triplethep.mppviewer.export;

import io.github.triplethep.mppviewer.render.Timeline;
import io.github.triplethep.mppviewer.render.ViewSnapshot;

import java.time.LocalDateTime;

/**
 * Zeitraum für Exporte: das ganze Projekt mit etwas Rand, im Maßstab der aktuellen Ansicht.
 */
final class ExportTimeline
{
   private ExportTimeline()
   {
   }

   static Timeline forView(ViewSnapshot view)
   {
      if (!view.kind().hasChart())
      {
         return null;
      }
      Timeline screen = view.timeline();
      LocalDateTime from = view.doc().getStart().toLocalDate().minusDays(2).atStartOfDay();
      LocalDateTime to = view.doc().getFinish().toLocalDate().plusDays(4).atStartOfDay();
      to = to.plusMinutes((long) (140.0 / screen.getPxPerDay() * 1440));
      return Timeline.forRange(from, to, screen.getPxPerDay(), screen.getWeekStart());
   }
}
