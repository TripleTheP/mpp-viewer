#!/usr/bin/env bash
# Baut die portable Version des MPP-Viewers:
#   1. eigene, verkleinerte Java-Laufzeit (jlink)
#   2. App-Image mit MPP-Viewer.exe (jpackage)
#   3. ZIP-Archiv zum Weitergeben
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

echo ">> App-Image erzeugen ($PLATFORM, Version $VERSION)"
ICON_ARG=()
if [ -n "$ICON" ]; then
   ICON_ARG=(--icon "$ICON")
fi
jpackage \
   --type app-image \
   --name "$NAME" \
   --app-version "$VERSION" \
   --vendor "MPP-Viewer" \
   --description "Schreibgeschützter Viewer für Microsoft-Project-Dateien" \
   --copyright "MIT License" \
   "${ICON_ARG[@]}" \
   --input "$DIST" \
   --main-jar mpp-viewer.jar \
   --main-class "$MAIN_CLASS" \
   --runtime-image "$RUNTIME" \
   --java-options "-Dfile.encoding=UTF-8" \
   --java-options "-Xss2m" \
   --dest "$OUT"

if [ "$PLATFORM" = "macos" ]; then
   IMAGE="$OUT/$NAME.app"
else
   IMAGE="$OUT/$NAME"
fi

cp README.md "$IMAGE/LIESMICH.md"
cp LICENSE "$IMAGE/LICENSE.txt"
cp THIRD-PARTY-NOTICES.md "$IMAGE/THIRD-PARTY-NOTICES.md"
cp samples/Beispielprojekt.xml "$IMAGE/Beispielprojekt.xml"

ARCHIVE="$NAME-$VERSION-$PLATFORM-x64-portable.zip"
echo ">> Archiv $ARCHIVE packen"
(
   cd "$OUT"
   if command -v 7z > /dev/null 2>&1; then
      7z a -tzip -mx=9 "$ARCHIVE" "$(basename "$IMAGE")" > /dev/null
   elif command -v zip > /dev/null 2>&1; then
      zip -qr9 "$ARCHIVE" "$(basename "$IMAGE")"
   else
      powershell -NoProfile -Command "Compress-Archive -Path '$(basename "$IMAGE")' -DestinationPath '$ARCHIVE'"
   fi
)

echo ">> Fertig: $OUT/$ARCHIVE"
ls -la "$OUT"
