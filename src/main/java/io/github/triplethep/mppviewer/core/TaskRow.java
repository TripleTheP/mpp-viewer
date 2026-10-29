package io.github.triplethep.mppviewer.core;

import org.mpxj.Task;

/**
 * Eine sichtbare Zeile in einer Vorgangsansicht.
 *
 * @param task        der Vorgang
 * @param level       Gliederungsebene (0 = Projektsammelvorgang, 1 = oberste Ebene)
 * @param hasChildren hat Teilvorgänge (zeigt Auf-/Zuklapp-Dreieck)
 * @param expanded    Teilvorgänge sind eingeblendet
 */
public record TaskRow(Task task, int level, boolean hasChildren, boolean expanded)
{
   public boolean isProjectSummary()
   {
      return level == 0;
   }
}
