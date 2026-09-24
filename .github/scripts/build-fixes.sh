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
mkdir -p app/src/main/java/com/livebridge/gestures
cp ../app-template/LiveBridgeTransformEngine.kt app/src/main/java/com/livebridge/gestures/LiveBridgeTransformEngine.kt

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

# Add screen capture and audio-source controls directly to the existing RootEncoder controller.
python3 - <<'PY'
from pathlib import Path
p = Path("app/src/main/java/com/livebridge/rtmp/StreamController.kt")
s = p.read_text()
if "import android.media.projection.MediaProjection" not in s:
    s = s.replace("import android.graphics.Bitmap", "import android.graphics.Bitmap\nimport android.media.projection.MediaProjection")
# Add only missing imports; older controller revisions may already contain some of them.
imports = [
    "import com.pedro.encoder.input.sources.video.ScreenSource",
    "import com.pedro.encoder.input.sources.audio.InternalAudioSource",
    "import com.pedro.encoder.input.sources.audio.MixAudioSource",
    "import com.pedro.encoder.input.sources.audio.MicrophoneSource",
]
for imp in imports:
    if imp not in s:
        s = s.replace("import com.pedro.encoder.input.sources.video.Camera2Source",
                      "import com.pedro.encoder.input.sources.video.Camera2Source\\n" + imp, 1)
if "val audioSource: String" not in s:
    needle = "    val message: String? = null"
    s = s.replace(needle, needle + ',\n    val audioSource: String = "Microphone",\n    val videoSource: String = "Caméra"')
if "var onRequestScreenCapture" not in s:
    s = s.replace("    var onPreviewStarted: (() -> Unit)? = null",
                  "    var onPreviewStarted: (() -> Unit)? = null\n    var onRequestScreenCapture: (() -> Unit)? = null\n    private var mediaProjection: MediaProjection? = null")
needle = "    fun switchCamera() {\n"
if "fun requestScreenCapture()" not in s:
    methods = '''    fun requestScreenCapture() {
        onRequestScreenCapture?.invoke() ?: _ui.update { it.copy(message = "Autorise la capture d'écran avec Android.") }
    }

    fun setScreenProjection(projection: MediaProjection?) {
        if (projection == null) {
            _ui.update { it.copy(message = "La capture d'écran n'a pas été autorisée.") }
            return
        }
        mediaProjection?.let { old -> if (old !== projection) runCatching { old.stop() } }
        mediaProjection = projection
        runCatching {
            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    if (mediaProjection === projection) {
                        mediaProjection = null
                        main.post { useCameraSource() }
                    }
                }
            }, main)
            stream.getGlInterface().setForceRender(true, 15)
            stream.changeVideoSource(ScreenSource(app, projection))
            _ui.update { it.copy(message = null, videoSource = "Écran") }
            log("Capture d'écran activée")
        }.onFailure { err ->
            _ui.update { it.copy(message = "Capture écran indisponible : ${err.message}") }
            log("Capture écran : ${err.message}")
        }
    }

    fun useCameraSource() {
        runCatching { mediaProjection?.stop() }
        mediaProjection = null
        runCatching { stream.changeVideoSource(Camera2Source(app)) }
            .onSuccess { _ui.update { it.copy(message = null, videoSource = "Caméra") } }
            .onFailure { err -> _ui.update { it.copy(message = "Caméra indisponible : ${err.message}") } }
    }

    fun useMicrophoneAudio() {
        runCatching { stream.changeAudioSource(MicrophoneSource()) }
            .onSuccess { _ui.update { it.copy(audioSource = "Microphone", message = null) } }
            .onFailure { err -> _ui.update { it.copy(message = "Microphone indisponible : ${err.message}") } }
    }

    fun useInternalAudio() {
        val projection = mediaProjection
        if (android.os.Build.VERSION.SDK_INT < 29 || projection == null) {
            _ui.update { it.copy(message = "Autorise d'abord la capture d'écran pour l'audio système.") }
            return
        }
        runCatching { stream.changeAudioSource(InternalAudioSource(projection)) }
            .onSuccess { _ui.update { it.copy(audioSource = "Audio système", message = null) } }
            .onFailure { err -> _ui.update { it.copy(message = "Audio système indisponible : ${err.message}") } }
    }

    fun useMixedAudio() {
        val projection = mediaProjection
        if (android.os.Build.VERSION.SDK_INT < 29 || projection == null) {
            _ui.update { it.copy(message = "Autorise d'abord la capture d'écran pour mixer l'audio.") }
            return
        }
        runCatching { stream.changeAudioSource(MixAudioSource(projection)) }
            .onSuccess { _ui.update { it.copy(audioSource = "Micro + système", message = null) } }
            .onFailure { err -> _ui.update { it.copy(message = "Mixage audio indisponible : ${err.message}") } }
    }

'''
    s = s.replace(needle, methods + needle)
p.write_text(s)
PY

# Ensure the foreground service requests the MediaProjection type when available.
python3 - <<'PY'
from pathlib import Path
p = Path("app/src/main/java/com/livebridge/LiveService.kt")
s = p.read_text()
old = "ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE"
new = "ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION"
s = s.replace(old, new)
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
s = s.replace('android:foregroundServiceType="camera|microphone|mediaProjection"',
              'android:foregroundServiceType="camera|microphone|mediaProjection"')
p.write_text(s)
PY


# Complete tactile transform support: rotation, precise pinch, reset, and persistent model fields.
python3 - <<'PY'
from pathlib import Path

# Persist rotation in Source and remain backward-compatible with existing studio JSON.
p = Path("app/src/main/java/com/livebridge/studio/Model.kt")
m = p.read_text()
if "val rotation: Float = 0f" not in m:
    m = m.replace("    val size: Float = 25f,\n", "    val size: Float = 25f,\n    val rotation: Float = 0f,\n")
if '.put("rotation", s.rotation)' not in m:
    m = m.replace('.put("x", s.x).put("y", s.y).put("size", s.size)\n',
                  '.put("x", s.x).put("y", s.y).put("size", s.size).put("rotation", s.rotation)\n')
if "rotation = so.optDouble" not in m:
    m = m.replace('size = so.optDouble("size", 25.0).toFloat(),\n',
                  'size = so.optDouble("size", 25.0).toFloat(),\n                rotation = so.optDouble("rotation", 0.0).toFloat(),\n')
p.write_text(m)

# Add transform operations used by the mobile editor.
p = Path("app/src/main/java/com/livebridge/studio/StudioStore.kt")
s = p.read_text()
if "fun rotateSource(" not in s:
    needle = '    private fun clamp(v: Float, lo: Float, hi: Float) = max(lo, min(hi, v))\n'
    methods = '''    fun rotateSource(id: String, deltaDegrees: Float) = editCurrent(transform = mapSource(id) { s ->
        s.copy(rotation = ((s.rotation + deltaDegrees + 180f) % 360f) - 180f)
    })

    fun resetTransform(id: String) = editCurrent(transform = mapSource(id) {
        it.copy(x = 10f, y = 10f, size = 25f, rotation = 0f)
    })

'''
    s = s.replace(needle, methods + needle)
# Respect locks for all tactile transforms.
s = s.replace('fun moveSource(id: String, dxPercent: Float, dyPercent: Float) = editCurrent(transform = mapSource(id) { s ->',
              'fun moveSource(id: String, dxPercent: Float, dyPercent: Float) = editCurrent(transform = mapSource(id) { s ->\n        if (s.locked) return@mapSource s')
s = s.replace('fun resizeSource(id: String, size: Float) = editCurrent(transform = mapSource(id) { s ->',
              'fun resizeSource(id: String, size: Float) = editCurrent(transform = mapSource(id) { s ->\n        if (s.locked) return@mapSource s')
s = s.replace('fun rotateSource(id: String, deltaDegrees: Float) = editCurrent(transform = mapSource(id) { s ->',
              'fun rotateSource(id: String, deltaDegrees: Float) = editCurrent(transform = mapSource(id) { s ->\n        if (s.locked) return@mapSource s')
p.write_text(s)

# Apply Source.rotation to the actual RootEncoder object filters.
p = Path("app/src/main/java/com/livebridge/rtmp/StreamController.kt")
s = p.read_text()
if "private fun applyRotation(" not in s:
    marker = "    fun renderScene(scene: Scene, imageFor: (Source) -> Bitmap?) {\n"
    fn = '''    private fun applyRotation(filter: BaseObjectFilterRender, degrees: Float) {
        runCatching {
            val m = filter.javaClass.methods.firstOrNull {
                it.name == "setRotation" && it.parameterTypes.size == 1 &&
                    (it.parameterTypes[0] == Float::class.java || it.parameterTypes[0] == Float::class.javaPrimitiveType)
            }
            m?.invoke(filter, degrees)
        }
    }

'''
    s = s.replace(marker, fn + marker)
s = s.replace("                    applyPosition(f, s.x, s.y)\n", "                    applyPosition(f, s.x, s.y)\n                    applyRotation(f, s.rotation)\n")
s = s.replace("                    applyPosition(f, s.x, s.y)\n                }\n            }\n        }", "                    applyPosition(f, s.x, s.y)\n                    applyRotation(f, s.rotation)\n                }\n            }\n        }")
p.write_text(s)
PY
