# MPP-Viewer

[English](README.md) · **Deutsch**

[![Build](https://github.com/TripleTheP/mpp-viewer/actions/workflows/build.yml/badge.svg)](https://github.com/TripleTheP/mpp-viewer/actions/workflows/build.yml)
[![CodeQL](https://github.com/TripleTheP/mpp-viewer/actions/workflows/codeql.yml/badge.svg)](https://github.com/TripleTheP/mpp-viewer/actions/workflows/codeql.yml)
[![Release](https://img.shields.io/github/v/release/TripleTheP/mpp-viewer)](https://github.com/TripleTheP/mpp-viewer/releases/latest)
[![Lizenz: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Portabler, schreibgeschützter Viewer für Microsoft-Project-Dateien (`.mpp`). Er druckt Ansichten und exportiert sie
nach PDF, Excel, PNG und CSV. Die Oberfläche folgt Microsoft Project (Menüband, Gantt-Diagramm, Tabellen,
Seitenansicht), wer Project kennt, findet sich also sofort zurecht. Er braucht weder eine Lizenz für Microsoft Project
noch eine Installation oder Administratorrechte.

![Gantt-Diagramm](docs/screenshot-gantt.png)

## Funktionen

### Anzeigen

- Ansichten: Gantt-Diagramm, Überwachung: Gantt, Vorgang: Tabelle und Ressource: Tabelle
- Die Tabellen der Datei werden übernommen: Spalten, eigene Spaltentitel, Reihenfolge und Breiten
- Gespeicherte Sortierung, Filter, AutoFilter und Zellformate (Hintergrund- und Textfarbe, fett, kursiv,
  unterstrichen) aus der Datei
- Sammelvorgänge zum Auf- und Zuklappen, Gliederungsebenen, Meilensteine, Basisplan, Fortschritt, kritischer Weg,
  Stichtage, Unterbrechungen, Verknüpfungspfeile (EA/AA/EE/AE), arbeitsfreie Zeit und Heute-Linie
- Indikatoren für Notizen, Einschränkungen, erledigte Vorgänge und Hyperlinks mit QuickInfo
- Zoom per Strg+Mausrad oder Zoomregler, dazu „Gesamtes Projekt“, „Ausgewählte Vorgänge“ und „Bildlauf zu Vorgang“
- AutoFilter-Menüs in den Spaltenköpfen zum Sortieren und Filtern
- Informationen zum Vorgang, zur Ressource, Projektinformationen und Statistik, jeweils schreibgeschützt
- Suchen (Strg+F) und Kopieren markierter Zeilen nach Excel (Strg+C)
- Oberfläche auf Deutsch oder Englisch, Datums- und Dauerformate wie in Microsoft Project

### Drucken

![Seitenansicht](docs/screenshot-drucken.png)

- Seitenansicht im Office-Stil mit Drucker, Kopien, Papierformat und Ausrichtung
- Anpassen an 1, 2 oder 3 Seiten Breite, alles auf einer Seite oder fester Zoom
- Zeitskala automatisch passend zur Seite, wie in der Ansicht oder fest in Tagen, Wochen, Monaten oder Quartalen
- Gesamtes Projekt oder ein Zeitraum, Spalten links vom Teiler oder alle Spalten
- Fußzeile mit Legende, Projektname, Datum und Seitenzahlen

### Exportieren

- **PDF**: Seiten wie in der Seitenansicht oder ein einziges großes Blatt, Vektorgrafik mit durchsuchbarem Text
- **Excel (.xlsx)**: Vorgänge, Ressourcen und Projektdaten mit Gliederung, echten Datumswerten, fixierter
  Kopfzeile und AutoFilter
- **PNG**: die vollständige Ansicht als Bild in 100 % oder 200 %
- **CSV**: UTF-8, Semikolon (Deutsch) bzw. Komma (Englisch) als Trennzeichen, öffnet direkt in Excel

![AutoFilter](docs/screenshot-autofilter.png)

### Unterstützte Dateien

`.mpp` (Project 98 bis Project 2021 und Microsoft 365), `.mpt`, `.mpx`, Project-XML sowie weitere Formate, die
[MPXJ](https://www.mpxj.org) liest, z. B. Primavera XER/PMXML, Asta Powerproject und GanttProject.
Kennwortgeschützte Dateien werden nicht unterstützt.

<a id="download-und-start-windows"></a>

## Download und Start (Windows)

1. `MPP-Viewer-<Version>-windows-x64-portable.zip` aus dem
   [aktuellen Release](https://github.com/TripleTheP/mpp-viewer/releases/latest) herunterladen.
2. Das Archiv an einen beliebigen Ort entpacken, z. B. nach `C:\Tools`, auf einen USB-Stick oder eine
   Netzwerkfreigabe.
3. `MPP-Viewer\MPP-Viewer.exe` starten.

Das Paket bringt eine eigene Java-Laufzeit mit. Installation, Administratorrechte oder ein installiertes Java sind
nicht nötig. Projektdateien lassen sich per Drag & Drop öffnen oder im Explorer über **Öffnen mit →
MPP-Viewer.exe**. Ein Beispielprojekt (`Beispielprojekt.xml`) liegt im Programmordner.

Einstellungen (Sprache, Fenstergröße, zuletzt geöffnete Dateien) werden pro Benutzer unter
`%APPDATA%\MPP-Viewer` gespeichert. Sollen sie im Programmordner liegen, dort neben `MPP-Viewer.exe` eine leere
Datei `portable.properties` anlegen.

**Code-Signierung:** Die Windows-Releases werden über die SignPath Foundation signiert. Free code signing provided
by [SignPath.io](https://about.signpath.io), certificate by [SignPath Foundation](https://signpath.org). Details stehen
in der [Code signing policy](#code-signing-policy). Die Signierung wird derzeit eingerichtet. Bis signierte Releases
verfügbar sind, lassen sich Downloads über die veröffentlichten Prüfsummen und Herkunftsnachweise prüfen (siehe
[Downloads prüfen](#downloads-prüfen)).

## Export über die Kommandozeile

Dateien lassen sich ohne Fenster exportieren, z. B. aus Skripten oder geplanten Aufgaben:

```bat
MPP-Viewer.exe --export "\\server\projekte\Plan.mpp" "C:\Berichte\Plan.pdf"
```

Das Format ergibt sich aus der Dateiendung: `.pdf` (Seiten wie in der Seitenansicht), `.xlsx`, `.csv` oder `.png`.
Bei Erfolg ist der Rückgabewert `0`.

## Tastenkürzel

| Kürzel | Funktion |
| --- | --- |
| Strg+O | Datei öffnen |
| Strg+P | Seitenansicht und Drucken |
| F12 | Exportieren |
| Strg+F | Suchen |
| Strg+C | Markierte Zeilen kopieren |
| Strg+Mausrad, Strg+Plus/Minus | Zeitskala zoomen |
| Umschalt+Mausrad | Diagramm waagerecht scrollen |
| Strg+Umschalt+F5 | Bildlauf zu Vorgang |
| Alt+Umschalt+Minus/Plus | Teilvorgänge aus-/einblenden |
| Doppelklick, Enter | Informationen zum Vorgang |
| Strg+W | Datei schließen |
| Esc | Backstage-Ansicht („Datei“) verlassen |

## Sicherheit und Einsatz im Unternehmen

- Keine Installation, keine Administratorrechte, keine Änderungen an der Registry
- **Keine Netzwerkverbindungen**: kein Update-Check, keine Telemetrie, keine Online-Inhalte
- Projektdateien werden ausschließlich gelesen und nie verändert
- Jedes Release enthält SHA-256-Prüfsummen, GitHub-Herkunftsnachweise (Build Provenance) und eine Stückliste der
  Softwarebestandteile (SBOM, CycloneDX)
- Der Quellcode wird mit CodeQL analysiert, Abhängigkeiten werden von Dependabot überwacht

[SECURITY.md](SECURITY.md) beschreibt Dateizugriffe, Regeln für AppLocker/WDAC und das Melden von
Sicherheitslücken.

### Downloads prüfen

```powershell
# Mit SHA256SUMS.txt aus dem Release vergleichen
Get-FileHash .\MPP-Viewer-<Version>-windows-x64-portable.zip -Algorithm SHA256

# Prüfen, dass die Datei von GitHub Actions aus diesem Repository gebaut wurde (GitHub CLI erforderlich)
gh attestation verify .\MPP-Viewer-<Version>-windows-x64-portable.zip --repo TripleTheP/mpp-viewer

# Digitale Signatur prüfen (signierte Releases)
Get-AuthenticodeSignature .\MPP-Viewer\MPP-Viewer.exe
```

## Code signing policy

Free code signing provided by [SignPath.io](https://about.signpath.io), certificate by
[SignPath Foundation](https://signpath.org).

- Signiert wird ausschließlich `MPP-Viewer.exe`. Die Datei wird von GitHub Actions auf GitHub-gehosteten Runnern
  aus dem öffentlichen Quellcode dieses Repositorys gebaut.
- Jede Signieranfrage wird von einem Approver manuell freigegeben.
- Mitgelieferte Fremdkomponenten (Java-Laufzeit Eclipse Temurin, Java-Bibliotheken, native FlatLaf-Bibliothek)
  werden unverändert ausgeliefert und nicht mit dem Projektzertifikat signiert.

| Rolle | Mitglieder |
| --- | --- |
| Committer und Reviewer | [TripleTheP](https://github.com/TripleTheP) |
| Approver | [TripleTheP](https://github.com/TripleTheP) |

**Datenschutz:** Das Programm überträgt keine Informationen an andere Systeme im Netzwerk, sofern dies nicht
ausdrücklich von der Benutzerin oder dem Benutzer bzw. der installierenden oder betreibenden Person veranlasst wird.
(This program will not transfer any information to other networked systems unless specifically requested by the
user or the person installing or operating it.)

## Aus dem Quellcode bauen

Voraussetzungen: JDK 25 und Maven 3.9.

```bash
mvn package                       # bauen und testen, Ergebnis in target/dist
java -jar target/dist/mpp-viewer.jar
bash packaging/build-portable.sh  # portables Paket mit eigener Java-Laufzeit (unter Windows: Git Bash)
```

GitHub Actions baut und testet jeden Push unter Linux und Windows. Ändert sich die Version in der `pom.xml` auf
`main`, entsteht automatisch ein Release mit dem portablen Windows-Paket.

### Aufbau des Quellcodes

| Paket | Inhalt |
| --- | --- |
| `core` | Laden der Dateien (MPXJ), Spalten, Gliederung, Sortierung und Filter, Formate wie in Project |
| `render` | Zeichnen von Tabelle, Zeitskala und Gantt-Diagramm, gemeinsam für Bildschirm, Druck und Export |
| `print` | Seitenaufteilung, Druck und Seitenansicht |
| `export` | PDF, PNG, Excel (eigener schlanker XLSX-Writer) und CSV |
| `ui` | Hauptfenster, Menüband, Backstage-Ansicht und Dialoge |
| `i18n` | Deutsche und englische Texte |

## Mitwirken

Fehlerberichte und Vorschläge sind als [GitHub-Issues](https://github.com/TripleTheP/mpp-viewer/issues)
willkommen. Sicherheitslücken bitte vertraulich melden, wie in [SECURITY.md](SECURITY.md) beschrieben.

## Lizenz

MPP-Viewer steht unter der [MIT-Lizenz](LICENSE). Verwendete Fremdkomponenten und ihre Lizenzen sind in
[THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) aufgeführt.

Microsoft und Microsoft Project sind Marken der Microsoft-Unternehmensgruppe. Dieses Projekt steht in keiner
Verbindung zu Microsoft und wird von Microsoft weder unterstützt noch gesponsert.
