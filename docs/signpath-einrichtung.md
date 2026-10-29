# Signierung mit SignPath einrichten

Der Release-Workflow (`.github/workflows/release.yml`) ist fertig vorbereitet. Er signiert `MPP-Viewer.exe`
automatisch, sobald die Zugangsdaten von SignPath im Repository hinterlegt sind. Bis dahin entstehen
unsignierte Releases (mit Prüfsummen und Herkunftsnachweis).

## 1. Voraussetzungen

- Zwei-Faktor-Anmeldung (2FA) für das GitHub-Konto ist aktiv. SignPath verlangt sie für alle Beteiligten.
- Das Projekt ist öffentlich, steht unter MIT-Lizenz und hat bereits ein Release. Das ist erfüllt.
- Die README enthält den Abschnitt [Code signing policy](../README.md#code-signing-policy). Das ist ebenfalls erfüllt.

## 2. Antrag stellen

Formular: <https://signpath.org/apply>

| Feld | Angabe |
| --- | --- |
| Project name | MPP-Viewer |
| Repository | https://github.com/TripleTheP/mpp-viewer |
| License | MIT |
| Description | Portable, read-only viewer for Microsoft Project (.mpp) files with print and PDF/Excel/PNG/CSV export |
| Download page | https://github.com/TripleTheP/mpp-viewer/releases |
| Code signing policy | https://github.com/TripleTheP/mpp-viewer#code-signing-policy |
| Build system | GitHub Actions (GitHub-hosted runners) |
| Files to sign | `MPP-Viewer.exe` (Windows launcher built with jpackage) |

Die Prüfung dauert in der Regel einige Tage bis Wochen. Eine Zusage ist nicht garantiert.

## 3. In SignPath (nach der Zusage)

1. Im Projekt den vordefinierten Trusted Build System **GitHub.com** verknüpfen.
2. Unter *Artifact Configurations* den Inhalt von [`.signpath/artifact-configuration.xml`](../.signpath/artifact-configuration.xml)
   einfügen.
3. Die Signing Policy für Tests heißt meist `test-signing`, die für Releases `release-signing`.
4. Einen *CI User* mit der Rolle *Submitter* anlegen und dessen **API-Token** kopieren.
5. Optional: die SignPath-GitHub-App für das Repository installieren.

## 4. In GitHub hinterlegen

Unter *Settings → Secrets and variables → Actions* (oder mit der GitHub CLI):

```bash
gh secret set SIGNPATH_API_TOKEN --repo TripleTheP/mpp-viewer
gh variable set SIGNPATH_ORGANIZATION_ID --repo TripleTheP/mpp-viewer --body "<Organisations-ID aus SignPath>"
# nur für einen ersten Testlauf mit dem Testzertifikat, danach wieder löschen:
gh variable set SIGNPATH_SIGNING_POLICY_SLUG --repo TripleTheP/mpp-viewer --body "test-signing"
```

Weicht der Projekt-Slug in SignPath von `mpp-viewer` ab, zusätzlich `SIGNPATH_PROJECT_SLUG` setzen.

## 5. Signiertes Release erzeugen

1. Version in der `pom.xml` erhöhen (z. B. `1.0.2`) und auf `main` pushen.
2. Der Release-Workflow baut, testet und reicht das App-Image bei SignPath ein. Er wartet bis zu 60 Minuten
   auf die Freigabe.
3. In SignPath den Signing Request **freigeben** (Approve). SignPath schickt dazu eine E-Mail.
4. Der Workflow prüft die Signatur, packt das ZIP und veröffentlicht das Release.

## Empfohlene Repository-Einstellungen

- *Settings → Rules → Rulesets*: Branch `main` schützen (kein Force-Push, kein Löschen).
- *Settings → Code security*: **Private vulnerability reporting** einschalten (wird in `SECURITY.md` genannt).
