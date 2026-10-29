package io.github.triplethep.mppviewer.core;

/**
 * Fehler beim Öffnen einer Projektdatei, mit Grund für eine verständliche Meldung.
 */
public final class ProjectLoadException extends Exception
{
   private static final long serialVersionUID = 1L;

   public enum Reason
   {
      NOT_FOUND,
      PASSWORD_PROTECTED,
      UNSUPPORTED,
      INVALID_FILE,
      READ_ERROR
   }

   private final Reason reason;

   public ProjectLoadException(Reason reason, Throwable cause)
   {
      super(reason.name(), cause);
      this.reason = reason;
   }

   public Reason getReason()
   {
      return reason;
   }
}
