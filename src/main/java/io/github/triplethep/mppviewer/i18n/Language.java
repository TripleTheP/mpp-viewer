package io.github.triplethep.mppviewer.i18n;

import java.util.Locale;

/**
 * Unterstützte Oberflächensprachen.
 */
public enum Language
{
   DE("de", Locale.GERMANY, "Deutsch"),
   EN("en", Locale.US, "English");

   private final String code;
   private final Locale locale;
   private final String displayName;

   Language(String code, Locale locale, String displayName)
   {
      this.code = code;
      this.locale = locale;
      this.displayName = displayName;
   }

   public String code()
   {
      return code;
   }

   public Locale locale()
   {
      return locale;
   }

   public String displayName()
   {
      return displayName;
   }

   public static Language fromCode(String code, Language fallback)
   {
      if (code != null)
      {
         for (Language lang : values())
         {
            if (lang.code.equalsIgnoreCase(code.trim()))
            {
               return lang;
            }
         }
      }
      return fallback;
   }

   /**
    * Sprache passend zum Betriebssystem: Deutsch für deutschsprachige Systeme, sonst Englisch.
    */
   public static Language systemDefault()
   {
      return "de".equals(Locale.getDefault().getLanguage()) ? DE : EN;
   }
}
