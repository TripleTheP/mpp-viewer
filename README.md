# MPP-Viewer

Portabler, **schreibgeschützter** Viewer für Microsoft-Project-Dateien (`.mpp`) – mit Druck und Export.
Die Oberfläche ist Microsoft Project nachempfunden (Menüband, Gantt-Diagramm, Tabellen, Seitenansicht),
damit sich Kolleginnen und Kollegen nicht umgewöhnen müssen. Microsoft Project wird **nicht** benötigt.

![Gantt-Diagramm](docs/screenshot-gantt.png)

## Funktionen

**Anzeigen wie in Project**
- Gantt-Diagramm, Überwachung: Gantt, Vorgang: Tabelle, Ressource: Tabelle
- Spalten, Spaltennamen (auch eigene wie „Text1“ → „Kunde“), Reihenfolge und Breiten werden aus der Datei übernommen
- Gespeicherte Sortierung, Filter und AutoFilter der Datei, Zellformate (Hintergrundfarben, Textfarbe, fett, kursiv,
  unterstrichen) und Textarten
- Sammelvorgänge zum Auf- und Zuklappen, Gliederungsebenen, Meilensteine, Basisplan, Fortschritt, kritischer Weg,
  Stichtage, Unterbrechungen, Verknüpfungspfeile (EA/AA/EE/AE), arbeitsfreie Zeit, Heute-Linie
- Indikatoren (Notizen, Einschränkungen, erledigt, Hyperlinks) mit Tooltip
- Zoom per Strg+Mausrad, Zoomregler, „Gesamtes Projekt“, „Ausgewählte Vorgänge“, „Bildlauf zu Vorgang“
- AutoFilter-Pfeile in den Spaltenköpfen zum Sortieren und Filtern
- Informationen zum Vorgang / zur Ressource, Projektinformationen und Statistik (alles schreibgeschützt)
- Suchen (Strg+F), Kopieren markierter Zeilen nach Excel (Strg+C)
- Oberfläche **Deutsch oder Englisch** (Datei → Optionen), Datums- und Dauerformate wie in Project
  („Mo 26.10.26“, „5 Tage“, „3EA+2 Tage“)

**Drucken** (Datei → Drucken, Strg+P)

![Drucken](docs/screenshot-drucken.png)

- Seitenansicht wie in Office mit Drucker, Kopien, Papierformat, Hoch-/Querformat
- „An 1/2/3 Seite(n) Breite anpassen“, „Alles auf einer Seite“ oder fester Zoom
- Zeitskala automatisch passend zur Seite, wie in der Ansicht oder fest (Tage/Wochen/Monate/Quartale)
- Gesamtes Projekt oder bestimmter Zeitraum, nur Spalten links vom Teiler oder alle Spalten
- Fußzeile mit Legende, Projektname, Datum und Seitenzahlen

**Exportieren** (Datei → Exportieren, F12)
- **PDF** – Seiten wie in der Seitenansicht oder als ein großes Blatt, Vektorgrafik mit durchsuchbarem Text
- **Excel (.xlsx)** – Vorgänge, Ressourcen und Projektdaten; mit Gliederung (Gruppierung), Datumswerten,
  fixierter Kopfzeile und AutoFilter
- **PNG** – die komplette Ansicht als Bild (100 % oder 200 %)
- **CSV** – Semikolon (deutsch) bzw. Komma (englisch), UTF-8, öffnet direkt in Excel

![AutoFilter](docs/screenshot-autofilter.png)

**Unterstützte Dateien:** `.mpp` (Project 98 bis Project 2021 / Microsoft 365), `.mpt`, `.mpx`, Project-XML sowie
weitere Formate, die [MPXJ](https://www.mpxj.org) lesen kann (z. B. Primavera XER/PMXML, Asta, GanttProject).
Kennwortgeschützte Dateien können nicht geöffnet werden.

## Download und Start (Windows)

1. Unter [Releases](https://github.com/TripleTheP/mpp-viewer/releases) die Datei
   `MPP-Viewer-<Version>-windows-x64-portable.zip` herunterladen.
2. ZIP an einen beliebigen Ort entpacken – z. B. `C:\Tools`, einen USB-Stick oder ein Netzlaufwerk.
3. `MPP-Viewer\MPP-Viewer.exe` starten. **Keine Installation, keine Administratorrechte, kein Java nötig.**

Tipps:
- `.mpp`-Datei per Drag & Drop ins Fenster ziehen oder im Explorer **Öffnen mit → MPP-Viewer.exe** wählen.
- Zum Ausprobieren liegt `Beispielprojekt.xml` im Programmordner.
- Einstellungen (Sprache, zuletzt geöffnete Dateien) werden pro Benutzer unter `%APPDATA%\MPP-Viewer` gespeichert.
  Soll alles im Programmordner bleiben, dort eine leere Datei `portable.properties` anlegen.

## Export ohne Fenster (Skripte, geplante Aufgaben)

```bat
MPP-Viewer.exe --export "\\server\projekte\Plan.mpp" "C:\Berichte\Plan.pdf"
```

Das Format ergibt sich aus der Endung: `.pdf` (Seiten wie beim Drucken), `.xlsx`, `.csv` oder `.png`.

## Tastenkürzel

| Kürzel | Funktion |
| --- | --- |
| Strg+O | Datei öffnen |
| Strg+P | Drucken (Seitenansicht) |
| F12 | Exportieren |
| Strg+F | Suchen |
| Strg+C | Markierte Zeilen kopieren |
| Strg+Mausrad, Strg+Plus/Minus | Zeitskala zoomen |
| Umschalt+Mausrad | Diagramm waagerecht scrollen |
| Strg+Umschalt+F5 | Bildlauf zu Vorgang |
| Alt+Umschalt+Minus/Plus | Teilvorgänge aus-/einblenden |
| Doppelklick / Enter | Informationen zum Vorgang |
| Strg+W | Datei schließen |
| Esc | Backstage („Datei“) verlassen |

## Selbst bauen

Voraussetzungen: JDK 21 und Maven 3.9.

```bash
mvn package                       # Programm + Tests, Ergebnis in target/dist
java -jar target/dist/mpp-viewer.jar
bash packaging/build-portable.sh  # portable Version mit eigener Java-Laufzeit (unter Windows in Git Bash)
```

Die Windows-Version wird automatisch von GitHub Actions gebaut:
- jeder Push: Tests und ein portables ZIP als Build-Artefakt
- ein Tag `v*` (z. B. `v1.0.1`): zusätzlich ein GitHub-Release mit dem ZIP

## Aufbau des Quellcodes

| Paket | Inhalt |
| --- | --- |
| `core` | Laden der Datei (MPXJ), Spalten, Gliederung, Sortierung/Filter, Formate wie in Project |
| `render` | Zeichnen von Tabelle, Zeitskala und Gantt-Diagramm – gemeinsam für Bildschirm, Druck und Export |
| `print` | Seitenaufteilung, Druck und Seitenansicht |
| `export` | PDF, PNG, Excel (eigener schlanker XLSX-Writer) und CSV |
| `ui` | Hauptfenster, Menüband, Backstage, Dialoge |
| `i18n` | Deutsche und englische Texte |

## Lizenz

MIT – siehe [LICENSE](LICENSE). Verwendete Bibliotheken und deren Lizenzen: [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
„Microsoft Project“ ist eine Marke der Microsoft Corporation; dieses Projekt steht in keiner Verbindung zu Microsoft.

---

## English

MPP-Viewer is a portable, read-only viewer for Microsoft Project files with a Project-like user interface
(ribbon, Gantt chart, sheets, print preview), printing and export to PDF, Excel, PNG and CSV.
Download the portable Windows ZIP from the releases page, unzip it anywhere and start `MPP-Viewer.exe` – no
installation and no Java required. Switch the language under File → Options.
