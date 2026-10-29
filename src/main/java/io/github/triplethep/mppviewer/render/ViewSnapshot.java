package io.github.triplethep.mppviewer.render;

import io.github.triplethep.mppviewer.core.ColumnDef;
import io.github.triplethep.mppviewer.core.Formats;
import io.github.triplethep.mppviewer.core.ProjectDocument;
import io.github.triplethep.mppviewer.core.TaskOutline;
import io.github.triplethep.mppviewer.core.TaskRow;
import io.github.triplethep.mppviewer.i18n.I18n;
import org.mpxj.Resource;

import java.util.List;

/**
 * Momentaufnahme der aktuellen Ansicht (Zeilen, Spalten, Zoom, Optionen) als Grundlage für Druck und Export.
 *
 * @param columnsInPane Anzahl Spalten, die links vom Teiler sichtbar sind (Project druckt nur diese)
 */
public record ViewSnapshot(ProjectDocument doc, ViewKind kind, TaskOutline outline, List<ColumnDef> columns, int columnsInPane, GanttOptions options, Timeline timeline, Formats formats)
{
   public CellPresenter presenter()
   {
      return new CellPresenter(formats, doc.getProject().getDefaultCalendar(), io.github.triplethep.mppviewer.core.CellStyles.of(doc));
   }

   public List<TaskRow> taskRows()
   {
      return outline == null ? List.of() : outline.getRows();
   }

   public List<Resource> resources()
   {
      return doc.getResources();
   }

   public int rowCount()
   {
      return kind.isResourceView() ? resources().size() : taskRows().size();
   }

   public String viewTitle()
   {
      return I18n.t(kind.key());
   }
}
