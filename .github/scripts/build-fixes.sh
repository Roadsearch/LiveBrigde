#!/usr/bin/env bash
set -euo pipefail

# Rebuild the application from a clean launcher/UI. No legacy Compose screens are kept.
cd LiveBridge

rm -rf app/src/main/java/com/livebridge/ui
mkdir -p app/src/main/java/com/livebridge/ui

cp ../app-template/MainActivity.kt app/src/main/java/com/livebridge/MainActivity.kt
cp ../app-template/Theme.kt app/src/main/java/com/livebridge/ui/Theme.kt
cp ../app-template/StudioApp.kt app/src/main/java/com/livebridge/ui/StudioApp.kt

# Remove legacy UI and unused server material from the packaged application.
rm -f app/src/main/java/com/livebridge/ui/Common.kt
rm -f app/src/main/java/com/livebridge/ui/ElementsScreen.kt
rm -f app/src/main/java/com/livebridge/ui/LiveDashboardScreen.kt
rm -f app/src/main/java/com/livebridge/ui/Permissions.kt
rm -f app/src/main/java/com/livebridge/ui/PreviewCanvas.kt
rm -f app/src/main/java/com/livebridge/ui/SetupScreen.kt
rm -f app/src/main/java/com/livebridge/ui/StudioScreen.kt
rm -rf server

# Remove obsolete source files from previous iterations if present.
find app/src/main/java/com/livebridge -type f -name '*.kt' ! -path 'app/src/main/java/com/livebridge/MainActivity.kt' ! -path 'app/src/main/java/com/livebridge/LiveService.kt' ! -path 'app/src/main/java/com/livebridge/Prefs.kt' ! -path 'app/src/main/java/com/livebridge/rtmp/*' ! -path 'app/src/main/java/com/livebridge/studio/*' ! -path 'app/src/main/java/com/livebridge/ui/*' -delete
