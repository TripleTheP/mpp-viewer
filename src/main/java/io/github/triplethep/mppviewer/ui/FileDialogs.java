package io.github.triplethep.mppviewer.ui;

import io.github.triplethep.mppviewer.core.ProjectDocument;
import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.settings.Settings;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Datei-Dialoge zum Öffnen und Speichern.
 */
public final class FileDialogs
{
   private FileDialogs()
   {
   }

   public static File chooseProjectFile(Component parent, Settings settings)
   {
      JFileChooser chooser = new JFileChooser(startDirectory(settings, null));
      chooser.setDialogTitle(I18n.t("open.title"));
      FileNameExtensionFilter mpp = new FileNameExtensionFilter(I18n.t("open.filter.mpp"), "mpp", "mpt");
      chooser.addChoosableFileFilter(mpp);
      chooser.addChoosableFileFilter(new FileNameExtensionFilter(I18n.t("open.filter.all"), "mpp", "mpt", "mpx", "xml", "xer", "pmxml", "pp", "gan", "pod", "planner", "mpd", "sdef", "fts", "ppx", "zip"));
      chooser.setFileFilter(mpp);
      chooser.setAcceptAllFileFilterUsed(true);
      if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION)
      {
         return null;
      }
      File file = chooser.getSelectedFile();
      settings.setLastDirectory(file.getParentFile());
      return file;
   }

   /**
    * Speichern-Dialog mit Vorschlag „Projektname.ext“ im Ordner der Projektdatei.
    */
   public static File chooseSaveFile(Component parent, Settings settings, ProjectDocument doc, String extension, String filterText)
   {
      File dir = startDirectory(settings, doc);
      JFileChooser chooser = new JFileChooser(dir);
      chooser.setDialogTitle(I18n.t("export.saveTitle"));
      chooser.setFileFilter(new FileNameExtensionFilter(filterText, extension));
      String base = doc == null ? "Projekt" : baseName(doc.getFileName());
      chooser.setSelectedFile(new File(dir, base + "." + extension));
      while (true)
      {
         if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION)
         {
            return null;
         }
         File file = chooser.getSelectedFile();
         if (!file.getName().toLowerCase(Locale.ROOT).endsWith("." + extension))
         {
            file = new File(file.getParentFile(), file.getName() + "." + extension);
         }
         if (doc != null && doc.getFile() != null && file.getAbsoluteFile().equals(doc.getFile().getAbsoluteFile()))
         {
            JOptionPane.showMessageDialog(parent, I18n.t("export.sameFile"), AppInfo.NAME, JOptionPane.WARNING_MESSAGE);
            continue;
         }
         if (file.exists())
         {
            int answer = JOptionPane.showConfirmDialog(parent, I18n.t("export.overwrite", file.getName()), AppInfo.NAME, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION)
            {
               continue;
            }
         }
         settings.setLastDirectory(file.getParentFile());
         return file;
      }
   }

   /**
    * Nach dem Export fragen, ob die Datei geöffnet werden soll.
    */
   public static void offerOpen(Component parent, File file)
   {
      if (file == null)
      {
         return;
      }
      Object[] options =
      {
         I18n.t("export.done.open"),
         I18n.t("export.done.folder"),
         I18n.t("common.close")
      };
      int choice = JOptionPane.showOptionDialog(parent, I18n.t("export.done", file.getAbsolutePath()), AppInfo.NAME, JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
      try
      {
         if (choice == 0 && Desktop.isDesktopSupported())
         {
            Desktop.getDesktop().open(file);
         }
         else if (choice == 1 && Desktop.isDesktopSupported())
         {
            Desktop.getDesktop().open(file.getParentFile());
         }
      }
      catch (IOException | UnsupportedOperationException | IllegalArgumentException ex)
      {
         Tasks.showError(parent, ex);
      }
   }

   private static File startDirectory(Settings settings, ProjectDocument doc)
   {
      if (doc != null && doc.getFile() != null && doc.getFile().getParentFile() != null && doc.getFile().getParentFile().isDirectory())
      {
         return doc.getFile().getParentFile();
      }
      File last = settings.getLastDirectory();
      if (last != null && last.isDirectory())
      {
         return last;
      }
      return new File(System.getProperty("user.home"));
   }

   private static String baseName(String name)
   {
      int dot = name.lastIndexOf('.');
      return dot > 0 ? name.substring(0, dot) : name;
   }
}
