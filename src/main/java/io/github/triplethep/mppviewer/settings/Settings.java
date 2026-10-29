package io.github.triplethep.mppviewer.settings;

import io.github.triplethep.mppviewer.i18n.Language;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Benutzereinstellungen als einfache Properties-Datei – keine Registry, damit das Programm portabel bleibt.
 * <p>
 * Speicherort: liegt neben der MPP-Viewer.exe eine Datei {@code portable.properties} (oder der Ordner
 * {@code settings}), werden die Einstellungen dort abgelegt. Sonst pro Benutzer unter
 * {@code %APPDATA%\MPP-Viewer} bzw. {@code ~/.config/mpp-viewer}. So können Kollegen das Programm
 * auch gemeinsam von einem Netzlaufwerk starten, ohne sich die Liste zuletzt geöffneter Dateien zu teilen.
 */
public final class Settings
{
   private static final int MAX_RECENT = 10;
   private static final String FILE_NAME = "settings.properties";

   private final Path file;
   private final Properties props = new Properties();

   private Settings(Path file)
   {
      this.file = file;
   }

   public static Settings load()
   {
      Settings settings = new Settings(resolveFile());
      if (Files.isRegularFile(settings.file))
      {
         try (InputStream in = Files.newInputStream(settings.file))
         {
            settings.props.load(in);
         }
         catch (IOException ex)
         {
            // Defekte Einstellungsdatei: mit Standardwerten weitermachen
         }
      }
      return settings;
   }

   /**
    * Nur für Tests: Einstellungen ohne Datei.
    */
   public static Settings inMemory()
   {
      return new Settings(null);
   }

   public void save()
   {
      if (file == null)
      {
         return;
      }
      try
      {
         Files.createDirectories(file.getParent());
         try (OutputStream out = Files.newOutputStream(file))
         {
            props.store(out, "MPP-Viewer");
         }
      }
      catch (IOException ex)
      {
         // Einstellungen sind nicht kritisch – Fehler beim Speichern ignorieren
      }
   }

   public Language getLanguage()
   {
      return Language.fromCode(props.getProperty("language"), Language.systemDefault());
   }

   public void setLanguage(Language language)
   {
      props.setProperty("language", language.code());
   }

   public List<File> getRecentFiles()
   {
      List<File> result = new ArrayList<>();
      for (int i = 0; i < MAX_RECENT; i++)
      {
         String value = props.getProperty("recent." + i);
         if (value != null && !value.isBlank())
         {
            result.add(new File(value));
         }
      }
      return result;
   }

   public void addRecentFile(File f)
   {
      List<File> list = getRecentFiles();
      File abs = f.getAbsoluteFile();
      list.removeIf(x -> x.getAbsoluteFile().equals(abs));
      list.add(0, abs);
      setRecentFiles(list);
   }

   public void removeRecentFile(File f)
   {
      List<File> list = getRecentFiles();
      list.removeIf(x -> x.getAbsoluteFile().equals(f.getAbsoluteFile()));
      setRecentFiles(list);
   }

   private void setRecentFiles(List<File> list)
   {
      for (int i = 0; i < MAX_RECENT; i++)
      {
         props.remove("recent." + i);
      }
      for (int i = 0; i < Math.min(MAX_RECENT, list.size()); i++)
      {
         props.setProperty("recent." + i, list.get(i).getPath());
      }
   }

   public File getLastDirectory()
   {
      String value = props.getProperty("lastDirectory");
      return value == null ? null : new File(value);
   }

   public void setLastDirectory(File dir)
   {
      if (dir != null)
      {
         props.setProperty("lastDirectory", dir.getAbsolutePath());
      }
   }

   public Rectangle getWindowBounds()
   {
      String value = props.getProperty("window.bounds");
      if (value == null)
      {
         return null;
      }
      String[] parts = value.split(",");
      if (parts.length != 4)
      {
         return null;
      }
      try
      {
         return new Rectangle(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()), Integer.parseInt(parts[3].trim()));
      }
      catch (NumberFormatException ex)
      {
         return null;
      }
   }

   public void setWindowBounds(Rectangle r)
   {
      props.setProperty("window.bounds", r.x + "," + r.y + "," + r.width + "," + r.height);
   }

   public boolean isWindowMaximized()
   {
      return Boolean.parseBoolean(props.getProperty("window.maximized", "true"));
   }

   public void setWindowMaximized(boolean maximized)
   {
      props.setProperty("window.maximized", Boolean.toString(maximized));
   }

   public String get(String key, String defaultValue)
   {
      return props.getProperty(key, defaultValue);
   }

   public void put(String key, String value)
   {
      if (value == null)
      {
         props.remove(key);
      }
      else
      {
         props.setProperty(key, value);
      }
   }

   public boolean getBoolean(String key, boolean defaultValue)
   {
      String value = props.getProperty(key);
      return value == null ? defaultValue : Boolean.parseBoolean(value);
   }

   public void putBoolean(String key, boolean value)
   {
      props.setProperty(key, Boolean.toString(value));
   }

   public Path getFile()
   {
      return file;
   }

   private static Path resolveFile()
   {
      Path appDir = applicationDirectory();
      if (appDir != null)
      {
         if (Files.isRegularFile(appDir.resolve("portable.properties")))
         {
            return appDir.resolve("portable.properties");
         }
         if (Files.isDirectory(appDir.resolve("settings")))
         {
            return appDir.resolve("settings").resolve(FILE_NAME);
         }
      }

      String appData = System.getenv("APPDATA");
      if (appData != null && !appData.isBlank())
      {
         return Paths.get(appData, "MPP-Viewer", FILE_NAME);
      }
      return Paths.get(System.getProperty("user.home"), ".config", "mpp-viewer", FILE_NAME);
   }

   /**
    * Ordner, in dem die MPP-Viewer.exe liegt (bei jpackage-App-Images ist das der Elternordner von {@code app}).
    */
   public static Path applicationDirectory()
   {
      try
      {
         Path location = Paths.get(Settings.class.getProtectionDomain().getCodeSource().getLocation().toURI());
         Path dir = Files.isRegularFile(location) ? location.getParent() : location;
         if (dir != null && dir.getFileName() != null && dir.getFileName().toString().equalsIgnoreCase("app") && dir.getParent() != null)
         {
            dir = dir.getParent();
         }
         return dir;
      }
      catch (URISyntaxException | SecurityException | NullPointerException ex)
      {
         return null;
      }
   }
}
