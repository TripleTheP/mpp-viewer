package io.github.triplethep.mppviewer.core;

import org.mpxj.FieldType;

import javax.swing.SwingConstants;

/**
 * Eine Tabellenspalte, wie sie in der Ansicht, im Druck und im Export verwendet wird.
 */
public final class ColumnDef
{
   public enum Kind
   {
      /** Zeilennummer (grau hinterlegt wie in Project) */
      ID,
      /** Symbole für Notizen, erledigt, Einschränkung … */
      INDICATORS,
      /** Symbol für manuell/automatisch geplant */
      TASK_MODE,
      /** Vorgangs- bzw. Ressourcenname mit Gliederung */
      NAME,
      /** Beliebiges anderes Feld */
      FIELD
   }

   private final Kind kind;
   private final FieldType field;
   private final String title;
   private final int width;
   private final int align;

   public ColumnDef(Kind kind, FieldType field, String title, int width, int align)
   {
      this.kind = kind;
      this.field = field;
      this.title = title;
      this.width = width;
      this.align = align;
   }

   public Kind getKind()
   {
      return kind;
   }

   public FieldType getField()
   {
      return field;
   }

   /**
    * Spaltentitel ohne weiche Trennzeichen (für Export, Tooltips und Dialoge).
    */
   public String getTitle()
   {
      return title == null ? null : title.replace("\u00AD", "");
   }

   /**
    * Spaltentitel mit weichen Trennzeichen für den zweizeiligen Spaltenkopf.
    */
   public String getHeaderTitle()
   {
      return title;
   }

   /**
    * Breite in Pixeln bei 100 % Skalierung.
    */
   public int getWidth()
   {
      return width;
   }

   /**
    * {@link SwingConstants#LEFT}, {@link SwingConstants#CENTER} oder {@link SwingConstants#RIGHT}.
    */
   public int getAlign()
   {
      return align;
   }

   public ColumnDef withWidth(int newWidth)
   {
      return new ColumnDef(kind, field, title, newWidth, align);
   }

   /**
    * Spalte, die nur Symbole enthält (im Export als Text ausgegeben).
    */
   public boolean isIconColumn()
   {
      return kind == Kind.INDICATORS || kind == Kind.TASK_MODE;
   }

   @Override
   public String toString()
   {
      return title;
   }
}
