package io.github.triplethep.mppviewer;

import io.github.triplethep.mppviewer.core.ProjectDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Eine präparierte Project-XML-Datei darf keine lokalen Dateien (oder Netzwerkadressen) über externe Entitäten
 * einlesen.
 */
class XmlHardeningTest
{
   @Test
   void externalEntitiesAreNotResolved(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path dir) throws Exception
   {
      App.hardenXmlParsing();
      Path secret = dir.resolve("geheim.txt");
      Files.writeString(secret, "STRENG-GEHEIM-4711", StandardCharsets.UTF_8);
      Path xml = dir.resolve("angriff.xml");
      Files.writeString(xml, """
         <?xml version="1.0" encoding="UTF-8"?>
         <!DOCTYPE Project [ <!ENTITY xxe SYSTEM "%s"> ]>
         <Project xmlns="http://schemas.microsoft.com/project">
           <Title>&xxe;</Title>
           <Name>&xxe;</Name>
         </Project>
         """.formatted(secret.toUri()), StandardCharsets.UTF_8);

      String title;
      try
      {
         ProjectDocument doc = ProjectDocument.load(xml.toFile());
         title = String.valueOf(doc.getProject().getProjectProperties().getProjectTitle()) + doc.getTitle();
      }
      catch (Exception ex)
      {
         // Abbruch beim Laden ist ebenfalls sicher
         title = String.valueOf(ex.getMessage());
      }
      assertFalse(title.contains("STRENG-GEHEIM-4711"), "Externe Entität wurde aufgelöst: " + title);
   }
}
