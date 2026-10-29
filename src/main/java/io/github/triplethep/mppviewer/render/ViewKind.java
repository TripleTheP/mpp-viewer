package io.github.triplethep.mppviewer.render;

/**
 * Die Ansichten des Viewers – benannt wie in Microsoft Project.
 */
public enum ViewKind
{
   GANTT(true, false),
   TRACKING_GANTT(true, false),
   TASK_SHEET(false, false),
   RESOURCE_SHEET(false, true);

   private final boolean chart;
   private final boolean resources;

   ViewKind(boolean chart, boolean resources)
   {
      this.chart = chart;
      this.resources = resources;
   }

   public boolean hasChart()
   {
      return chart;
   }

   public boolean isResourceView()
   {
      return resources;
   }

   public String key()
   {
      return "view." + name();
   }
}
