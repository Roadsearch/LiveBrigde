#!/usr/bin/env bash
set -euo pipefail

# Rebuild the application from a clean launcher/UI. No legacy Compose screens are kept.
cd LiveBridge

rm -rf app/src/main/java/com/livebridge/ui
mkdir -p app/src/main/java/com/livebridge/ui
mkdir -p app/src/main/java/com/livebridge/rtmp

cp ../app-template/MainActivity.kt app/src/main/java/com/livebridge/MainActivity.kt
cp ../app-template/CrashReporter.kt app/src/main/java/com/livebridge/CrashReporter.kt
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
find app/src/main/java/com/livebridge -type f -name '*.kt' ! -path 'app/src/main/java/com/livebridge/MainActivity.kt' ! -path 'app/src/main/java/com/livebridge/CrashReporter.kt' ! -path 'app/src/main/java/com/livebridge/LiveService.kt' ! -path 'app/src/main/java/com/livebridge/Prefs.kt' ! -path 'app/src/main/java/com/livebridge/rtmp/*' ! -path 'app/src/main/java/com/livebridge/studio/*' ! -path 'app/src/main/java/com/livebridge/ui/*' -delete

# Copy the extension after legacy-file cleanup so it cannot be removed by the cleanup pass.
cp ../app-template/ScreenCaptureExtensions.kt app/src/main/java/com/livebridge/rtmp/ScreenCaptureExtensions.kt

# Extend the controller state for the mobile audio mixer.
python3 - <<'PY'
from pathlib import Path
p = Path("app/src/main/java/com/livebridge/rtmp/StreamController.kt")
s = p.read_text()
if "val audioSource: String" not in s:
    s = s.replace(
        "    val message: String? = null\n)",
        "    val message: String? = null,\n    val audioSource: String = \"Microphone\"\n)"
    )
    p.write_text(s)
PY

# Add the MediaProjection capability to the existing manifest without injecting literal "\\n" text.
python3 - <<'PY'
from pathlib import Path
p = Path("app/src/main/AndroidManifest.xml")
s = p.read_text()
perm = '    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />'
if "android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" not in s:
    marker = '    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />'
    if marker in s:
        s = s.replace(marker, marker + "\n" + perm)
    else:
        s = s.replace('    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />',
                      '    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />\n' + perm)
s = s.replace('android:foregroundServiceType="camera|microphone"',
              'android:foregroundServiceType="camera|microphone|mediaProjection"')
p.write_text(s)
PY
