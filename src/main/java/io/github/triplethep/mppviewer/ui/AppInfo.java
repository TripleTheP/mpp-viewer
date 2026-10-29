package io.github.triplethep.mppviewer.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Programmname und Version.
 */
public final class AppInfo
{
   public static final String NAME = "MPP-Viewer";
   private static final String VERSION = loadVersion();

   private AppInfo()
   {
   }

   public static String version()
   {
      return VERSION;
   }

   private static String loadVersion()
   {
      try (InputStream in = AppInfo.class.getResourceAsStream("/app.properties"))
      {
         if (in != null)
         {
            Properties p = new Properties();
            p.load(in);
            String v = p.getProperty("version");
            if (v != null && !v.contains("${"))
            {
               return v;
            }
         }
      }
      catch (IOException ex)
      {
         // ignorieren
      }
      Package pkg = AppInfo.class.getPackage();
      String v = pkg == null ? null : pkg.getImplementationVersion();
      return v == null ? "dev" : v;
   }
}
