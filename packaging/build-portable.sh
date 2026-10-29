#!/usr/bin/env bash
# Baut die portable Version des MPP-Viewers:
#   1. eigene, verkleinerte Java-Laufzeit (jlink)
#   2. App-Image mit MPP-Viewer.exe (jpackage)
#   3. ZIP-Archiv zum Weitergeben (entfällt mit NO_ZIP=1, z. B. wenn vorher noch signiert wird;
#      das ZIP erzeugt dann packaging/zip-portable.sh)
#
# Voraussetzung: JDK 21 oder neuer und ein vorheriges "mvn package".
# Unter Windows in Git Bash ausführen (in GitHub Actions: shell: bash).
set -euo pipefail

cd "$(dirname "$0")/.."

NAME="MPP-Viewer"
MAIN_CLASS="io.github.triplethep.mppviewer.App"
VERSION="${1:-$(sed -n 's:.*<version>\([0-9][^<]*\)</version>.*:\1:p' pom.xml | head -1)}"
DIST="target/dist"
RUNTIME="target/runtime"
OUT="target/package"

# Java-Module, die MPXJ, PDFBox, FlatLaf und Swing benötigen (ermittelt mit jdeps)
MODULES="java.base,java.desktop,java.logging,java.management,java.naming,java.net.http,java.prefs,java.scripting,java.sql,java.xml,jdk.unsupported,jdk.charsets,jdk.localedata,jdk.accessibility,jdk.crypto.ec,jdk.zipfs"

case "$(uname -s)" in
   MINGW*|MSYS*|CYGWIN*) PLATFORM="windows"; ICON="packaging/windows/mpp-viewer.ico" ;;
   Darwin*) PLATFORM="macos"; ICON="" ;;
   *) PLATFORM="linux"; ICON="packaging/mpp-viewer.png" ;;
esac

if [ ! -f "$DIST/mpp-viewer.jar" ]; then
   echo "Bitte zuerst 'mvn package' ausführen." >&2
   exit 1
fi

rm -rf "$RUNTIME" "$OUT"
mkdir -p "$OUT"

echo ">> Java-Laufzeit erzeugen"
jlink \
   --add-modules "$MODULES" \
   --include-locales=de,en \
   --strip-debug \
   --no-header-files \
   --no-man-pages \
   --compress=zip-6 \
   --output "$RUNTIME"

# FlatLaf entpackt seine native Bibliothek sonst beim Start in den Temp-Ordner – in Firmenumgebungen ist das
# Laden von DLLs aus %TEMP% oft gesperrt (AppLocker/WDAC). Liegt die DLL neben flatlaf-<version>.jar, wird sie
# direkt aus dem Programmordner geladen.
rm -f "$DIST"/lib/flatlaf-*-windows-*.dll
if [ "$PLATFORM" = "windows" ]; then
   FLATLAF_JAR=$(ls "$DIST"/lib/flatlaf-*.jar | head -1)
   FLATLAF_BASE=$(basename "$FLATLAF_JAR" .jar)
   NATIVES_TMP="target/flatlaf-natives"
   rm -rf "$NATIVES_TMP" && mkdir -p "$NATIVES_TMP"
   ABS_JAR="$(cd "$(dirname "$FLATLAF_JAR")" && pwd)/$(basename "$FLATLAF_JAR")"
   (cd "$NATIVES_TMP" && jar xf "$ABS_JAR" com/formdev/flatlaf/natives/flatlaf-windows-x86_64.dll)
   cp "$NATIVES_TMP/com/formdev/flatlaf/natives/flatlaf-windows-x86_64.dll" "$DIST/lib/$FLATLAF_BASE-windows-x86_64.dll"
   echo ">> FlatLaf-DLL: $DIST/lib/$FLATLAF_BASE-windows-x86_64.dll"
fi

echo ">> App-Image erzeugen ($PLATFORM, Version $VERSION)"
ICON_ARG=()
if [ -n "$ICON" ]; then
   ICON_ARG=(--icon "$ICON")
fi
jpackage \
   --type app-image \
   --name "$NAME" \
   --app-version "$VERSION" \
   --vendor "TripleTheP" \
   --description "MPP-Viewer - schreibgeschützter Viewer für Microsoft-Project-Dateien" \
   --copyright "Copyright (c) 2026 TripleTheP, MIT License" \
   "${ICON_ARG[@]}" \
   --input "$DIST" \
   --main-jar mpp-viewer.jar \
   --main-class "$MAIN_CLASS" \
   --runtime-image "$RUNTIME" \
   --java-options "-Dfile.encoding=UTF-8" \
   --java-options "-Xss2m" \
   --java-options "-XX:+DisableAttachMechanism" \
   --java-options "-XX:-UsePerfData" \
   --dest "$OUT"

if [ "$PLATFORM" = "macos" ]; then
   IMAGE="$OUT/$NAME.app"
else
   IMAGE="$OUT/$NAME"
fi

cp README.md "$IMAGE/LIESMICH.md"
cp LICENSE "$IMAGE/LICENSE.txt"
cp THIRD-PARTY-NOTICES.md "$IMAGE/THIRD-PARTY-NOTICES.md"
cp SECURITY.md "$IMAGE/SECURITY.md"
cp samples/Beispielprojekt.xml "$IMAGE/Beispielprojekt.xml"

if [ "${NO_ZIP:-0}" = "1" ]; then
   echo ">> Fertig (ohne ZIP): $IMAGE"
   exit 0
fi

bash packaging/zip-portable.sh "$VERSION"
