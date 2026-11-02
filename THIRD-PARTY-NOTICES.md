# Verwendete Bibliotheken / Third-party notices

Der MPP-Viewer selbst steht unter der MIT-Lizenz. Die portable Version enthält folgende Bibliotheken als
eigenständige JAR-Dateien im Ordner `app/lib` (sie können dort jederzeit durch andere Versionen ersetzt werden):

| Bibliothek | Zweck | Lizenz |
| --- | --- | --- |
| [MPXJ](https://www.mpxj.org) | Lesen von .mpp und anderen Projektformaten | GNU LGPL 2.1 oder später |
| [Apache POI](https://poi.apache.org) (poi) | Dateiformat-Grundlagen für MPXJ | Apache 2.0 |
| Apache Commons (codec, collections4, io, lang3, logging, math3) | Hilfsbibliotheken | Apache 2.0 |
| [Apache Log4j API](https://logging.apache.org/log4j) | Logging-Schnittstelle | Apache 2.0 |
| [SparseBitSet](https://github.com/brettwooldridge/SparseBitSet) | Abhängigkeit von POI | Apache 2.0 |
| [RTF Parser Kit](https://github.com/joniles/rtfparserkit) | Notizen im RTF-Format | Apache 2.0 |
| Jakarta XML Binding / Eclipse JAXB (jaxb-api, jaxb-runtime, jaxb-core, txw2, istack, activation) | Project-XML | Eclipse Distribution License 1.0 (BSD) |
| [Jackcess](https://jackcess.sourceforge.io) | Access-basierte Projektformate | Apache 2.0 |
| [jsoup](https://jsoup.org) | HTML-Notizen | MIT |
| [Jackson](https://github.com/FasterXML/jackson) (core, databind, annotations) | JSON-basierte Formate | Apache 2.0 |
| [FastDoubleParser](https://github.com/wrandelshofer/FastDoubleParser) (in jackson-core enthalten) | Zahlen einlesen | MIT |
| [FlatLaf](https://www.formdev.com/flatlaf) (inkl. `flatlaf-<version>-windows-x86_64.dll` aus dem FlatLaf-Jar) | Look-and-Feel, Titelleiste | Apache 2.0 |
| [Apache PDFBox](https://pdfbox.apache.org) (pdfbox, pdfbox-io, fontbox) | PDF-Export | Apache 2.0 |
| [pdfbox-graphics2d](https://github.com/rototor/pdfbox-graphics2d) | Vektorgrafik im PDF | Apache 2.0 |
| [Eclipse Temurin / OpenJDK](https://adoptium.net) (verkleinerte Laufzeit im Ordner `runtime`) | Java-Laufzeit | GPL 2.0 mit Classpath Exception |

Lizenztexte:

- **Apache 2.0** und **GNU LGPL 2.1**: im Ordner `licenses` (im Repository und im Programmordner der portablen
  Version). Einige Bibliotheken (MPXJ, SparseBitSet, RTF Parser Kit, Jackcess, pdfbox-graphics2d) enthalten selbst
  keinen Lizenztext; für sie gelten diese Dateien.
- Eclipse Distribution License, MIT (jsoup, FastDoubleParser) sowie die NOTICE-Dateien der Apache-Projekte: in den
  jeweiligen JAR-Dateien unter `META-INF`.
- Java-Laufzeit: im Ordner `runtime/legal` der portablen Version.

**LGPL-Hinweis (MPXJ):** MPXJ wird unverändert als eigene Datei `mpxj-<version>.jar` mitgeliefert. Sie dürfen diese
Datei durch eine andere (auch selbst geänderte) Version ersetzen. Der Quellcode von MPXJ ist unter
https://github.com/joniles/mpxj erhältlich.
