# Sicherheit / Security

## Informationen für Administratoren

Der MPP-Viewer ist ein reines Anzeigeprogramm. Er wurde so gebaut, dass er sich in verwalteten
Firmenumgebungen ohne Sonderrechte betreiben lässt.

| Thema | Verhalten |
| --- | --- |
| Installation | keine – ZIP entpacken, `MPP-Viewer.exe` starten; keine Administratorrechte nötig |
| Registry | keine Einträge, keine Dateizuordnungen, kein Autostart, keine Dienste |
| Netzwerk | **keine Verbindungen** – kein Update-Check, keine Telemetrie, keine Online-Hilfe. Hyperlinks aus Projektdateien werden nicht geöffnet. Dateien auf Netzlaufwerken werden über die normale Windows-Dateifreigabe gelesen. |
| Projektdateien | werden ausschließlich **gelesen**; das Programm hat keine Speicherfunktion für .mpp-Dateien |
| Schreibzugriffe | nur `%APPDATA%\MPP-Viewer\settings.properties` (Sprache, Fenstergröße, zuletzt geöffnete Dateien) und Exportdateien an dem Ort, den der Benutzer wählt. Mit einer Datei `portable.properties` im Programmordner liegen die Einstellungen stattdessen dort. |
| Temporäre Dateien | keine eigenen; die native Bibliothek für den Fensterrahmen wird aus `app\lib` geladen, nicht aus `%TEMP%` |
| Laufzeit | eigene, verkleinerte Java-21-Laufzeit (Eclipse Temurin) im Ordner `runtime`; ein installiertes Java wird weder benötigt noch verwendet. Das Andocken fremder Prozesse an die Java-Laufzeit (Attach-API) ist abgeschaltet. |
| Präparierte Dateien | XML-basierte Projektformate dürfen keine externen Dateien oder Adressen nachladen (Schutz vor XML External Entities) |

### Echtheit eines Downloads prüfen

Jedes Release auf GitHub enthält:

- **Digitale Signatur** von `MPP-Viewer.exe` (sobald die Signierung über SignPath aktiv ist, siehe
  [Code signing policy](README.md#code-signing-policy)). Prüfen in PowerShell:
  ```powershell
  Get-AuthenticodeSignature .\MPP-Viewer\MPP-Viewer.exe | Format-List Status, SignerCertificate, TimeStamperCertificate
  ```
- **SHA-256-Prüfsummen** in `SHA256SUMS.txt`:
  ```powershell
  Get-FileHash .\MPP-Viewer-1.0.1-windows-x64-portable.zip -Algorithm SHA256
  ```
- **Herkunftsnachweis** (Build-Provenance nach SLSA): belegt, dass ZIP und EXE von GitHub Actions aus genau
  diesem Repository und Commit gebaut wurden:
  ```powershell
  gh attestation verify .\MPP-Viewer-1.0.1-windows-x64-portable.zip --repo TripleTheP/mpp-viewer
  gh attestation verify .\MPP-Viewer\MPP-Viewer.exe --repo TripleTheP/mpp-viewer
  ```
- **Stückliste (SBOM)** aller enthaltenen Java-Bibliotheken im CycloneDX-Format (`*-sbom.cdx.json`), z. B. für
  Schwachstellen-Scanner wie Dependency-Track.

### Freigabe in AppLocker / WDAC / Intune

- Empfohlen: das Programm zentral in einem Ordner bereitstellen, auf den Benutzer **nur Lesezugriff** haben
  (z. B. `C:\Program Files\MPP-Viewer` oder eine schreibgeschützte Netzwerkfreigabe). Die Signatur schützt die
  `.exe`; die Java-Bibliotheken in `app\lib` und die Startkonfiguration `app\MPP-Viewer.cfg` sind dagegen nur
  durch die Dateirechte geschützt.
- Mit Signatur: Herausgeberregel für `MPP-Viewer.exe` (Herausgeber „SignPath Foundation“, Produkt „MPP-Viewer“).
- Ohne Signatur: Dateihash-Regel mit dem Hash aus `SHA256SUMS.txt`.
- Die Java-Laufzeit unter `runtime\bin` (z. B. `java.dll`, `jvm.dll`) stammt unverändert aus Eclipse Temurin.
  Falls DLL-Regeln aktiv sind, müssen diese Dateien ebenfalls zugelassen werden.

### Aktualität

Bibliotheken und GitHub Actions werden per Dependabot überwacht, der Quellcode per CodeQL analysiert.
Eine neue Version erscheint als neues Release. Ältere Versionen erhalten keine Korrekturen.

## Sicherheitslücke melden / Reporting a vulnerability

Bitte **nicht** als öffentliches Issue melden, sondern vertraulich über
[GitHub → Security → Report a vulnerability](https://github.com/TripleTheP/mpp-viewer/security/advisories/new).
Please report vulnerabilities privately via GitHub's "Report a vulnerability" button, not as a public issue.

| Version | Unterstützt / Supported |
| --- | --- |
| neueste Version / latest release | ✅ |
| ältere Versionen / older releases | ❌ |
