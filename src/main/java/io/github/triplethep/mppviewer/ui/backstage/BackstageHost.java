package io.github.triplethep.mppviewer.ui.backstage;

import io.github.triplethep.mppviewer.core.ColumnDef;
import io.github.triplethep.mppviewer.core.ProjectDocument;
import io.github.triplethep.mppviewer.i18n.Language;
import io.github.triplethep.mppviewer.print.PrintSettings;
import io.github.triplethep.mppviewer.render.ViewSnapshot;
import io.github.triplethep.mppviewer.settings.Settings;

import java.awt.Component;
import java.io.File;
import java.util.List;

/**
 * Was die Backstage-Ansicht („Datei“) vom Hauptfenster braucht.
 */
public interface BackstageHost
{
   void closeBackstage();

   void openFileDialog();

   void openFile(File file);

   void closeDocument();

   Settings settings();

   /** Geöffnetes Projekt oder {@code null}. */
   ProjectDocument document();

   /** Momentaufnahme der aktuellen Ansicht oder {@code null}, wenn kein Projekt offen ist. */
   ViewSnapshot snapshot();

   /** Alle Spalten der aktuellen Vorgangstabelle (für Excel/CSV). */
   List<ColumnDef> taskColumns();

   List<ColumnDef> resourceColumns();

   PrintSettings printSettings();

   void setLanguage(Language language);

   void showProjectInfo();

   Component component();
}
