package io.github.triplethep.mppviewer.export;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2DFontTextDrawer;
import io.github.triplethep.mppviewer.render.Style;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Bettet die Bildschirmschrift (z. B. Segoe UI) in das PDF ein, damit der Text auswählbar und durchsuchbar bleibt.
 * Wird die Schriftdatei nicht gefunden, zeichnet PDFBox den Text als Vektorpfade – das PDF sieht dann gleich aus,
 * der Text ist aber nicht markierbar.
 */
final class PdfFonts
{
   private static final Map<String, String[]> FILES = Map.of(
      "segoe ui", new String[]
      {
         "segoeui.ttf",
         "segoeuib.ttf"
      },
      "calibri", new String[]
      {
         "calibri.ttf",
         "calibrib.ttf"
      },
      "arial", new String[]
      {
         "arial.ttf",
         "arialbd.ttf"
      },
      "liberation sans", new String[]
      {
         "LiberationSans-Regular.ttf",
         "LiberationSans-Bold.ttf"
      },
      "dejavu sans", new String[]
      {
         "DejaVuSans.ttf",
         "DejaVuSans-Bold.ttf"
      });

   private static volatile List<File> cached;

   private PdfFonts()
   {
   }

   static PdfBoxGraphics2DFontTextDrawer createDrawer()
   {
      PdfBoxGraphics2DFontTextDrawer drawer = new PdfBoxGraphics2DFontTextDrawer();
      for (File f : fontFiles())
      {
         try
         {
            drawer.registerFont(f);
         }
         catch (RuntimeException ex)
         {
            // Schrift unbrauchbar – Text wird dann als Pfad gezeichnet
         }
      }
      return drawer;
   }

   private static List<File> fontFiles()
   {
      List<File> result = cached;
      if (result != null)
      {
         return result;
      }
      result = new ArrayList<>();
      String[] names = FILES.get(Style.fontFamily().toLowerCase(Locale.ROOT));
      if (names != null)
      {
         for (String name : names)
         {
            File f = find(name);
            if (f != null)
            {
               result.add(f);
            }
         }
      }
      cached = result;
      return result;
   }

   private static File find(String fileName)
   {
      List<Path> dirs = new ArrayList<>();
      String windir = System.getenv("WINDIR");
      if (windir != null)
      {
         dirs.add(Paths.get(windir, "Fonts"));
      }
      String local = System.getenv("LOCALAPPDATA");
      if (local != null)
      {
         dirs.add(Paths.get(local, "Microsoft", "Windows", "Fonts"));
      }
      dirs.add(Paths.get("C:\\Windows\\Fonts"));
      dirs.add(Paths.get("/usr/share/fonts"));
      dirs.add(Paths.get("/usr/local/share/fonts"));
      dirs.add(Paths.get(System.getProperty("user.home"), ".fonts"));
      dirs.add(Paths.get("/Library/Fonts"));
      dirs.add(Paths.get("/System/Library/Fonts"));
      for (Path dir : dirs)
      {
         if (!Files.isDirectory(dir))
         {
            continue;
         }
         Path direct = dir.resolve(fileName);
         if (Files.isRegularFile(direct))
         {
            return direct.toFile();
         }
         try (Stream<Path> walk = Files.walk(dir, 4))
         {
            Path hit = walk.filter(p -> p.getFileName() != null && p.getFileName().toString().equalsIgnoreCase(fileName)).findFirst().orElse(null);
            if (hit != null)
            {
               return hit.toFile();
            }
         }
         catch (Exception ex)
         {
            // weiter mit dem nächsten Ordner
         }
      }
      return null;
   }
}
