#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-}"

if [[ "$MODE" != "beta" && "$MODE" != "release" ]]; then
    echo "Usage: $0 <beta|release>" >&2
    exit 1
fi

if [[ "$MODE" == "release" ]]; then
    exit 0
fi

cd "$(dirname "$0")"

./gradlew assembleDebug

APK_DIR="app/build/outputs/apk/debug"
DEST="btcmap-api:/srv/http/static.btcmap.org/android/apk/"

rsync -avz --progress "$APK_DIR/app-arm64-v8a-debug.apk" "$DEST/beta.apk"
rsync -avz --progress "$APK_DIR/app-universal-debug.apk" "$DEST/beta-universal.apk"