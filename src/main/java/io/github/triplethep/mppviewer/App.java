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
 * (z. B. über „Öffnen mit“ im Windows-Explorer). Mit {@code --export <Datei> <Ziel>} wird ohne Fenster exportiert.
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
      hardenXmlParsing();

      Settings settings = Settings.load();
      I18n.setLanguage(settings.getLanguage());

      if (args.length >= 3 && "--export".equalsIgnoreCase(args[0]))
      {
         System.setProperty("java.awt.headless", "true");
         System.exit(BatchExport.run(args[1], args[2]));
         return;
      }

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

   /**
    * XML-basierte Projektdateien (Project-XML, Primavera, GanttProject …) dürfen keine externen Dateien oder
    * Netzwerkadressen nachladen (Schutz vor „XML External Entity“-Angriffen über präparierte Dateien).
    */
   static void hardenXmlParsing()
   {
      for (String key : new String[] {"javax.xml.accessExternalDTD", "javax.xml.accessExternalSchema", "javax.xml.accessExternalStylesheet"})
      {
         if (System.getProperty(key) == null)
         {
            System.setProperty(key, "");
         }
      }
      if (System.getProperty("jdk.xml.enableExtensionFunctions") == null)
      {
         System.setProperty("jdk.xml.enableExtensionFunctions", "false");
      }
   }
}
