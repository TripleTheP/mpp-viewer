#!/usr/bin/env bash
# Packt das App-Image aus target/package/MPP-Viewer als portables ZIP
# (wird von build-portable.sh aufgerufen oder nach dem Signieren separat).
set -euo pipefail

cd "$(dirname "$0")/.."

NAME="MPP-Viewer"
VERSION="${1:-$(sed -n 's:.*<version>\([0-9][^<]*\)</version>.*:\1:p' pom.xml | head -1)}"
OUT="target/package"

case "$(uname -s)" in
   MINGW*|MSYS*|CYGWIN*) PLATFORM="windows" ;;
   Darwin*) PLATFORM="macos" ;;
   *) PLATFORM="linux" ;;
esac

if [ "$PLATFORM" = "macos" ]; then
   IMAGE_NAME="$NAME.app"
else
   IMAGE_NAME="$NAME"
fi

if [ ! -d "$OUT/$IMAGE_NAME" ]; then
   echo "App-Image $OUT/$IMAGE_NAME fehlt – bitte zuerst packaging/build-portable.sh ausführen." >&2
   exit 1
fi

ARCHIVE="$NAME-$VERSION-$PLATFORM-x64-portable.zip"
echo ">> Archiv $ARCHIVE packen"
(
   cd "$OUT"
   rm -f "$ARCHIVE"
   if command -v 7z > /dev/null 2>&1; then
      7z a -tzip -mx=9 "$ARCHIVE" "$IMAGE_NAME" > /dev/null
   elif command -v zip > /dev/null 2>&1; then
      zip -qr9 "$ARCHIVE" "$IMAGE_NAME"
   else
      powershell -NoProfile -Command "Compress-Archive -Path '$IMAGE_NAME' -DestinationPath '$ARCHIVE'"
   fi
)

echo ">> Fertig: $OUT/$ARCHIVE"
ls -la "$OUT"
