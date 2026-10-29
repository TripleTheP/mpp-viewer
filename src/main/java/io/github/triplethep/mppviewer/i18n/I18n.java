package io.github.triplethep.mppviewer.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Oberflächentexte in Deutsch und Englisch. Die Sprache kann zur Laufzeit gewechselt werden.
 */
public final class I18n
{
   private static Language language = Language.DE;
   private static ResourceBundle bundle = load(Language.DE);
   private static ResourceBundle fallback = load(Language.EN);

   private I18n()
   {
   }

   public static synchronized void setLanguage(Language lang)
   {
      language = lang;
      bundle = load(lang);
   }

   public static synchronized Language language()
   {
      return language;
   }

   public static Locale locale()
   {
      return language().locale();
   }

   /**
    * Liefert den Text zum Schlüssel; mit Argumenten wird er per {@link MessageFormat} formatiert.
    */
   public static String t(String key, Object... args)
   {
      String text = lookup(key);
      if (text == null)
      {
         return key;
      }
      // Weiche Trennzeichen sind nur für Spaltenköpfe gedacht (siehe ColumnSets)
      if (!key.startsWith("field."))
      {
         text = text.replace("\u00AD", "");
      }
      if (args.length == 0)
      {
         return text;
      }
      MessageFormat format = new MessageFormat(text.replace("'", "''"), locale());
      return format.format(args);
   }

   /**
    * Liefert den Text oder {@code null}, wenn der Schlüssel nicht existiert.
    */
   public static String opt(String key)
   {
      return lookup(key);
   }

   private static synchronized String lookup(String key)
   {
      try
      {
         return bundle.getString(key);
      }
      catch (MissingResourceException ex)
      {
         try
         {
            return fallback.getString(key);
         }
         catch (MissingResourceException ex2)
         {
            return null;
         }
      }
   }

   private static ResourceBundle load(Language lang)
   {
      String path = "/i18n/messages_" + lang.code() + ".properties";
      try (InputStream in = I18n.class.getResourceAsStream(path))
      {
         if (in == null)
         {
            throw new IllegalStateException("Missing resource " + path);
         }
         return new PropertyResourceBundle(new InputStreamReader(in, StandardCharsets.UTF_8));
      }
      catch (IOException ex)
      {
         throw new UncheckedIOException(ex);
      }
   }
}
