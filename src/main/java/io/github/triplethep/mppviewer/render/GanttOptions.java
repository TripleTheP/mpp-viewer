package io.github.triplethep.mppviewer.render;

/**
 * Darstellungsoptionen des Gantt-Diagramms (entsprechen den Kontrollkästchen auf der Registerkarte „Format“ in
 * Project).
 */
public final class GanttOptions
{
   /** Überwachungs-Gantt: Basisplanbalken und Prozentangaben, kritische Vorgänge rot */
   public boolean tracking;
   public boolean criticalTasks;
   public boolean links = true;
   public boolean baseline;
   public boolean barText = true;
   public boolean currentDate = true;
   public boolean nonWorking = true;
   public boolean roundBarsToWholeDays;

   public GanttOptions copy()
   {
      GanttOptions o = new GanttOptions();
      o.tracking = tracking;
      o.criticalTasks = criticalTasks;
      o.links = links;
      o.baseline = baseline;
      o.barText = barText;
      o.currentDate = currentDate;
      o.nonWorking = nonWorking;
      o.roundBarsToWholeDays = roundBarsToWholeDays;
      return o;
   }

   public boolean showCritical()
   {
      return tracking || criticalTasks;
   }

   public boolean showBaseline()
   {
      return tracking || baseline;
   }
}
