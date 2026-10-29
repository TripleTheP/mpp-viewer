package io.github.triplethep.mppviewer.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Schlanker Writer für Excel-Arbeitsmappen (.xlsx, Office Open XML) – ohne Fremdbibliothek. Unterstützt Text, Zahlen,
 * Datumswerte, Prozent, Währung, Fettdruck, Einzüge, Gliederung (Zeilengruppierung), fixierte Kopfzeile und
 * AutoFilter.
 */
public final class XlsxWriter
{
   /** Zellformate (Index in cellXfs). */
   public enum Style
   {
      DEFAULT,
      HEADER,
      TEXT,
      DATE,
      NUMBER,
      PERCENT,
      CURRENCY,
      LABEL
   }

   private static final int MAX_INDENT = 15;

   private final List<Sheet> sheets = new ArrayList<>();
   private final String dateFormat;
   private final String currencyFormat;
   private String title = "";
   private String author = "";

   /**
    * @param dateFormat     Excel-Zahlenformat für Datumswerte, z. B. „dd.mm.yy“
    * @param currencyFormat Excel-Zahlenformat für Beträge, z. B. „#,##0.00 \"€\"“
    */
   public XlsxWriter(String dateFormat, String currencyFormat)
   {
      this.dateFormat = dateFormat;
      this.currencyFormat = currencyFormat;
   }

   public void setTitle(String title)
   {
      this.title = title == null ? "" : title;
   }

   public void setAuthor(String author)
   {
      this.author = author == null ? "" : author;
   }

   public Sheet addSheet(String name)
   {
      Sheet sheet = new Sheet(uniqueName(sanitize(name)));
      sheets.add(sheet);
      return sheet;
   }

   private String uniqueName(String name)
   {
      String candidate = name;
      int n = 2;
      while (true)
      {
         String c = candidate;
         if (sheets.stream().noneMatch(s -> s.name.equalsIgnoreCase(c)))
         {
            return candidate;
         }
         String suffix = " (" + n++ + ")";
         candidate = name.substring(0, Math.min(name.length(), 31 - suffix.length())) + suffix;
      }
   }

   private static String sanitize(String name)
   {
      String s = name == null || name.isBlank() ? "Sheet" : name.replaceAll("[\\[\\]:*?/\\\\]", "_");
      return s.length() > 31 ? s.substring(0, 31) : s;
   }

   // ------------------------------------------------------------------ Datenmodell

   public static final class Sheet
   {
      private final String name;
      private final List<Double> widths = new ArrayList<>();
      private final List<Row> rows = new ArrayList<>();
      private boolean freezeHeader;
      private boolean autoFilter;
      private int filterColumns;

      private Sheet(String name)
      {
         this.name = name;
      }

      /**
       * Spaltenbreite in Zeichen (Excel-Einheit).
       */
      public void setColumnWidth(int column, double chars)
      {
         while (widths.size() <= column)
         {
            widths.add(null);
         }
         widths.set(column, Double.valueOf(chars));
      }

      public Row addRow()
      {
         Row row = new Row();
         rows.add(row);
         return row;
      }

      public void freezeHeader()
      {
         this.freezeHeader = true;
      }

      public void autoFilter(int columns)
      {
         this.autoFilter = true;
         this.filterColumns = columns;
      }

      public String getName()
      {
         return name;
      }

      public int getRowCount()
      {
         return rows.size();
      }
   }

   public static final class Row
   {
      private final List<Cell> cells = new ArrayList<>();
      private int outlineLevel;

      public Row outlineLevel(int level)
      {
         this.outlineLevel = Math.max(0, Math.min(7, level));
         return this;
      }

      public Row add(Object value, Style style)
      {
         cells.add(new Cell(value, style, false, 0));
         return this;
      }

      public Row add(Object value, Style style, boolean bold)
      {
         cells.add(new Cell(value, style, bold, 0));
         return this;
      }

      public Row addIndented(String text, boolean bold, int indent)
      {
         cells.add(new Cell(text, Style.TEXT, bold, Math.max(0, Math.min(MAX_INDENT, indent))));
         return this;
      }
   }

   private record Cell(Object value, Style style, boolean bold, int indent)
   {
   }

   /** Prozentwert (0..1), wird als Prozent formatiert. */
   public record Percent(double value)
   {
   }

   /** Währungsbetrag. */
   public record Money(double value)
   {
   }

   // ------------------------------------------------------------------ Schreiben

   public void write(OutputStream out) throws IOException
   {
      if (sheets.isEmpty())
      {
         addSheet("Sheet1");
      }
      ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8);

      entry(zip, "[Content_Types].xml", contentTypes());
      entry(zip, "_rels/.rels", rootRels());
      entry(zip, "docProps/core.xml", core());
      entry(zip, "docProps/app.xml", app());
      entry(zip, "xl/workbook.xml", workbook());
      entry(zip, "xl/_rels/workbook.xml.rels", workbookRels());
      entry(zip, "xl/styles.xml", styles());
      for (int i = 0; i < sheets.size(); i++)
      {
         ByteArrayOutputStream sheetXml = new ByteArrayOutputStream();
         Writer w = new OutputStreamWriter(sheetXml, StandardCharsets.UTF_8);
         writeSheet(w, sheets.get(i));
         w.flush();
         zip.putNextEntry(new ZipEntry("xl/worksheets/sheet" + (i + 1) + ".xml"));
         sheetXml.writeTo(zip);
         zip.closeEntry();
      }
      zip.finish();
   }

   private static void entry(ZipOutputStream zip, String name, String content) throws IOException
   {
      zip.putNextEntry(new ZipEntry(name));
      zip.write(content.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
   }

   private String contentTypes()
   {
      StringBuilder sb = new StringBuilder();
      sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
      sb.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">");
      sb.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>");
      sb.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>");
      sb.append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
      sb.append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
      for (int i = 0; i < sheets.size(); i++)
      {
         sb.append("<Override PartName=\"/xl/worksheets/sheet").append(i + 1).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
      }
      sb.append("<Override PartName=\"/docProps/core.xml\" ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/>");
      sb.append("<Override PartName=\"/docProps/app.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.extended-properties+xml\"/>");
      sb.append("</Types>");
      return sb.toString();
   }

   private static String rootRels()
   {
      return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
         + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
         + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
         + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\" Target=\"docProps/core.xml\"/>"
         + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties\" Target=\"docProps/app.xml\"/>"
         + "</Relationships>";
   }

   private String core()
   {
      String now = java.time.Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
      return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
         + "<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
         + "<dc:title>" + esc(title) + "</dc:title>"
         + "<dc:creator>" + esc(author) + "</dc:creator>"
         + "<dcterms:created xsi:type=\"dcterms:W3CDTF\">" + now + "</dcterms:created>"
         + "<dcterms:modified xsi:type=\"dcterms:W3CDTF\">" + now + "</dcterms:modified>"
         + "</cp:coreProperties>";
   }

   private static String app()
   {
      return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
         + "<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\"><Application>MPP-Viewer</Application></Properties>";
   }

   private String workbook()
   {
      StringBuilder sb = new StringBuilder();
      sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
      sb.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
      sb.append("<bookViews><workbookView/></bookViews><sheets>");
      for (int i = 0; i < sheets.size(); i++)
      {
         sb.append("<sheet name=\"").append(esc(sheets.get(i).name)).append("\" sheetId=\"").append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>");
      }
      sb.append("</sheets>");
      StringBuilder names = new StringBuilder();
      for (int i = 0; i < sheets.size(); i++)
      {
         Sheet s = sheets.get(i);
         if (s.autoFilter && s.rows.size() > 0 && s.filterColumns > 0)
         {
            names.append("<definedName name=\"_xlnm._FilterDatabase\" localSheetId=\"").append(i).append("\" hidden=\"1\">'")
               .append(esc(s.name.replace("'", "''"))).append("'!$A$1:$").append(columnName(s.filterColumns - 1)).append('$').append(s.rows.size()).append("</definedName>");
         }
      }
      if (!names.isEmpty())
      {
         sb.append("<definedNames>").append(names).append("</definedNames>");
      }
      sb.append("</workbook>");
      return sb.toString();
   }

   private String workbookRels()
   {
      StringBuilder sb = new StringBuilder();
      sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
      sb.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
      for (int i = 0; i < sheets.size(); i++)
      {
         sb.append("<Relationship Id=\"rId").append(i + 1).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(i + 1).append(".xml\"/>");
      }
      sb.append("<Relationship Id=\"rId").append(sheets.size() + 1).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
      sb.append("</Relationships>");
      return sb.toString();
   }

   /*
    * Aufbau von cellXfs:
    * 0 Standard, 1 Kopf, 2 Text, 3 Text fett, 4 Datum, 5 Datum fett, 6 Zahl, 7 Zahl fett, 8 Prozent, 9 Prozent fett,
    * 10 Währung, 11 Währung fett, 12 Beschriftung (fett, grau), ab 13: Text mit Einzug 1..15 (normal/fett im Wechsel)
    */
   private static final int XF_INDENT_BASE = 13;

   private String styles()
   {
      StringBuilder sb = new StringBuilder();
      sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
      sb.append("<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
      sb.append("<numFmts count=\"2\">");
      sb.append("<numFmt numFmtId=\"164\" formatCode=\"").append(esc(dateFormat)).append("\"/>");
      sb.append("<numFmt numFmtId=\"165\" formatCode=\"").append(esc(currencyFormat)).append("\"/>");
      sb.append("</numFmts>");
      sb.append("<fonts count=\"3\">");
      sb.append("<font><sz val=\"10\"/><name val=\"Calibri\"/><family val=\"2\"/></font>");
      sb.append("<font><b/><sz val=\"10\"/><name val=\"Calibri\"/><family val=\"2\"/></font>");
      sb.append("<font><b/><sz val=\"10\"/><color rgb=\"FF595959\"/><name val=\"Calibri\"/><family val=\"2\"/></font>");
      sb.append("</fonts>");
      sb.append("<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>");
      sb.append("<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFDCEBDA\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>");
      sb.append("<borders count=\"2\"><border><left/><right/><top/><bottom/><diagonal/></border>");
      sb.append("<border><left/><right/><top/><bottom style=\"thin\"><color rgb=\"FF31752F\"/></bottom><diagonal/></border></borders>");
      sb.append("<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>");

      List<String> xfs = new ArrayList<>();
      xfs.add("<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>");
      xfs.add("<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>");
      xfs.add("<xf numFmtId=\"49\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>");
      xfs.add("<xf numFmtId=\"49\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\"/>");
      xfs.add("<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\" applyAlignment=\"1\"><alignment horizontal=\"right\"/></xf>");
      xfs.add("<xf numFmtId=\"164\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyAlignment=\"1\"><alignment horizontal=\"right\"/></xf>");
      xfs.add("<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>");
      xfs.add("<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>");
      xfs.add("<xf numFmtId=\"9\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>");
      xfs.add("<xf numFmtId=\"9\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\"/>");
      xfs.add("<xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>");
      xfs.add("<xf numFmtId=\"165\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\"/>");
      xfs.add("<xf numFmtId=\"49\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\"/>");
      for (int i = 1; i <= MAX_INDENT; i++)
      {
         for (int bold = 0; bold <= 1; bold++)
         {
            xfs.add("<xf numFmtId=\"49\" fontId=\"" + bold + "\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyAlignment=\"1\"><alignment horizontal=\"left\" indent=\"" + i + "\"/></xf>");
         }
      }
      sb.append("<cellXfs count=\"").append(xfs.size()).append("\">");
      for (String xf : xfs)
      {
         sb.append(xf);
      }
      sb.append("</cellXfs>");
      sb.append("<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>");
      sb.append("</styleSheet>");
      return sb.toString();
   }

   private static int xf(Cell cell)
   {
      if (cell.indent > 0 && (cell.style == Style.TEXT || cell.style == Style.DEFAULT))
      {
         return XF_INDENT_BASE + (cell.indent - 1) * 2 + (cell.bold ? 1 : 0);
      }
      int b = cell.bold ? 1 : 0;
      return switch (cell.style)
      {
         case DEFAULT -> 0;
         case HEADER -> 1;
         case TEXT -> 2 + b;
         case DATE -> 4 + b;
         case NUMBER -> 6 + b;
         case PERCENT -> 8 + b;
         case CURRENCY -> 10 + b;
         case LABEL -> 12;
      };
   }

   private void writeSheet(Writer w, Sheet sheet) throws IOException
   {
      int maxLevel = 0;
      for (Row r : sheet.rows)
      {
         maxLevel = Math.max(maxLevel, r.outlineLevel);
      }
      w.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
      w.write("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
      // Sammelzeilen stehen wie in Project über den Teilvorgängen
      w.write("<sheetPr><outlinePr summaryBelow=\"0\"/></sheetPr>");
      w.write("<sheetViews><sheetView workbookViewId=\"0\"" + (sheets.indexOf(sheet) == 0 ? " tabSelected=\"1\"" : "") + ">");
      if (sheet.freezeHeader)
      {
         w.write("<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/><selection pane=\"bottomLeft\" activeCell=\"A2\" sqref=\"A2\"/>");
      }
      w.write("</sheetView></sheetViews>");
      w.write("<sheetFormatPr defaultRowHeight=\"13.8\"" + (maxLevel > 0 ? " outlineLevelRow=\"" + maxLevel + "\"" : "") + "/>");
      if (!sheet.widths.isEmpty())
      {
         w.write("<cols>");
         for (int i = 0; i < sheet.widths.size(); i++)
         {
            Double width = sheet.widths.get(i);
            if (width != null)
            {
               w.write("<col min=\"" + (i + 1) + "\" max=\"" + (i + 1) + "\" width=\"" + String.format(java.util.Locale.ROOT, "%.2f", width.doubleValue()) + "\" customWidth=\"1\"/>");
            }
         }
         w.write("</cols>");
      }
      w.write("<sheetData>");
      for (int r = 0; r < sheet.rows.size(); r++)
      {
         Row row = sheet.rows.get(r);
         w.write("<row r=\"" + (r + 1) + "\"" + (row.outlineLevel > 0 ? " outlineLevel=\"" + row.outlineLevel + "\"" : "") + ">");
         for (int c = 0; c < row.cells.size(); c++)
         {
            Cell cell = row.cells.get(c);
            String ref = columnName(c) + (r + 1);
            writeCell(w, ref, cell);
         }
         w.write("</row>");
      }
      w.write("</sheetData>");
      if (sheet.autoFilter && !sheet.rows.isEmpty() && sheet.filterColumns > 0)
      {
         w.write("<autoFilter ref=\"A1:" + columnName(sheet.filterColumns - 1) + sheet.rows.size() + "\"/>");
      }
      w.write("<pageMargins left=\"0.5\" right=\"0.5\" top=\"0.6\" bottom=\"0.6\" header=\"0.3\" footer=\"0.3\"/>");
      w.write("<pageSetup orientation=\"landscape\" fitToHeight=\"0\"/>");
      w.write("</worksheet>");
   }

   private static void writeCell(Writer w, String ref, Cell cell) throws IOException
   {
      Object v = cell.value;
      int s = xf(cell);
      if (v == null)
      {
         if (s != 0)
         {
            w.write("<c r=\"" + ref + "\" s=\"" + s + "\"/>");
         }
         return;
      }
      if (v instanceof LocalDateTime dt)
      {
         w.write("<c r=\"" + ref + "\" s=\"" + s + "\"><v>" + num(serial(dt)) + "</v></c>");
         return;
      }
      if (v instanceof LocalDate d)
      {
         w.write("<c r=\"" + ref + "\" s=\"" + s + "\"><v>" + num(serial(d.atStartOfDay())) + "</v></c>");
         return;
      }
      if (v instanceof Percent p)
      {
         w.write("<c r=\"" + ref + "\" s=\"" + s + "\"><v>" + num(p.value()) + "</v></c>");
         return;
      }
      if (v instanceof Money m)
      {
         w.write("<c r=\"" + ref + "\" s=\"" + s + "\"><v>" + num(m.value()) + "</v></c>");
         return;
      }
      if (v instanceof Number n)
      {
         w.write("<c r=\"" + ref + "\" s=\"" + s + "\"><v>" + num(n.doubleValue()) + "</v></c>");
         return;
      }
      if (v instanceof Boolean b)
      {
         w.write("<c r=\"" + ref + "\" s=\"" + s + "\" t=\"b\"><v>" + (b.booleanValue() ? 1 : 0) + "</v></c>");
         return;
      }
      String text = v.toString();
      if (text.length() > 32_000)
      {
         text = text.substring(0, 32_000);
      }
      w.write("<c r=\"" + ref + "\" s=\"" + s + "\" t=\"inlineStr\"><is><t xml:space=\"preserve\">" + esc(text) + "</t></is></c>");
   }

   /**
    * Excel-Seriennummer: Tage seit dem 30.12.1899 mit Tagesbruchteil.
    */
   static double serial(LocalDateTime dt)
   {
      long minutes = ChronoUnit.MINUTES.between(LocalDate.of(1899, 12, 30).atStartOfDay(), dt);
      return minutes / 1440.0;
   }

   private static String num(double d)
   {
      if (d == Math.rint(d) && Math.abs(d) < 1e15)
      {
         return Long.toString((long) d);
      }
      return Double.toString(d);
   }

   static String columnName(int index)
   {
      StringBuilder sb = new StringBuilder();
      int i = index + 1;
      while (i > 0)
      {
         int rem = (i - 1) % 26;
         sb.insert(0, (char) ('A' + rem));
         i = (i - 1) / 26;
      }
      return sb.toString();
   }

   static String esc(String s)
   {
      StringBuilder sb = new StringBuilder(s.length());
      for (int i = 0; i < s.length(); i++)
      {
         char c = s.charAt(i);
         switch (c)
         {
            case '&' -> sb.append("&amp;");
            case '<' -> sb.append("&lt;");
            case '>' -> sb.append("&gt;");
            case '"' -> sb.append("&quot;");
            default ->
            {
               // In XML 1.0 unzulässige Steuerzeichen entfernen
               if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r')
               {
                  sb.append(c);
               }
            }
         }
      }
      return sb.toString();
   }
}
