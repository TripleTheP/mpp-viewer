package io.github.triplethep.mppviewer;

import io.github.triplethep.mppviewer.i18n.I18n;
import io.github.triplethep.mppviewer.settings.Settings;
import io.github.triplethep.mppviewer.ui.MainFrame;
import io.github.triplethep.mppviewer.ui.Tasks;
import io.github.triplethep.mppviewer.ui.Theme;

import javax.swing.SwingUtilities;
import java.io.File;

/**
 * Startpunkt des MPP-Viewers. Ein optionaler Dateipfad als Argument wird direkt geöffnet
 * (z. B. über „Öffnen mit“ im Windows-Explorer).
 */
public final class App
{
   private App()
   {
   }

   public static void main(String[] args)
   {
      // MPXJ und POI melden fehlende Logging-Konfigurationen sonst auf der Konsole
      System.setProperty("log4j2.loggerContextFactory", "org.apache.logging.log4j.simple.SimpleLoggerContextFactory");
      System.setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level", "OFF");
      System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

      Settings settings = Settings.load();
      I18n.setLanguage(settings.getLanguage());

      SwingUtilities.invokeLater(() ->
      {
         Theme.install();
         MainFrame frame = new MainFrame(settings);
         Thread.setDefaultUncaughtExceptionHandler((t, e) -> SwingUtilities.invokeLater(() -> Tasks.showError(frame, e)));
         frame.setVisible(true);
         if (args.length > 0 && !args[0].isBlank())
         {
            frame.openFile(new File(args[0]));
         }
      });
   }
}
