package io.github.triplethep.mppviewer.render;

import java.awt.Color;
import java.util.List;

/**
 * Inhalt einer Tabellenzelle, unabhängig davon, ob sie am Bildschirm, im Druck oder im Export landet.
 *
 * @param text      angezeigter Text
 * @param align     Ausrichtung ({@link javax.swing.SwingConstants})
 * @param bold      fett (Sammelvorgänge)
 * @param strike    durchgestrichen (inaktive Vorgänge)
 * @param muted     grau (inaktive Vorgänge)
 * @param indent    Einrückungsebene (Vorgangsname)
 * @param outline   Auf-/Zuklappdreieck
 * @param icons     Symbole (Indikatoren, Vorgangsmodus)
 * @param header    grau hinterlegte Kopfzelle (Nr.-Spalte)
 * @param style     Formatierung aus der Projektdatei (Textfarbe, Hintergrund, kursiv, unterstrichen)
 */
public record CellContent(String text, int align, boolean bold, boolean strike, boolean muted, int indent, Outline outline, List<Indicator> icons, boolean header, Decoration style)
{
   public enum Outline
   {
      NONE,
      EXPANDED,
      COLLAPSED
   }

   /**
    * Zusätzliche Zellformatierung, wie sie in Project über „Text formatieren“ oder „Hintergrundfarbe“ gesetzt wurde.
    *
    * @param foreground Textfarbe oder {@code null}
    * @param background Hintergrundfarbe oder {@code null}
    */
   public record Decoration(Color foreground, Color background, boolean italic, boolean underline)
   {
      public static final Decoration NONE = new Decoration(null, null, false, false);
   }

   public CellContent(String text, int align, boolean bold, boolean strike, boolean muted, int indent, Outline outline, List<Indicator> icons, boolean header)
   {
      this(text, align, bold, strike, muted, indent, outline, icons, header, Decoration.NONE);
   }

   public static CellContent plain(String text, int align)
   {
      return new CellContent(text, align, false, false, false, 0, Outline.NONE, List.of(), false);
   }

   /**
    * Gleicher Inhalt mit anderer Formatierung.
    */
   public CellContent styled(boolean newBold, boolean newStrike, Decoration decoration)
   {
      return new CellContent(text, align, newBold, newStrike, muted, indent, outline, icons, header, decoration == null ? Decoration.NONE : decoration);
   }
}
