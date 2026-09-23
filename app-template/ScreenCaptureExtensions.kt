package com.livebridge.rtmp

import android.content.Context
import android.media.projection.MediaProjection
import com.pedro.encoder.input.sources.audio.InternalAudioSource
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.audio.MixAudioSource
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.ScreenSource
import com.pedro.library.generic.GenericStream
import java.util.WeakHashMap

private val projections = WeakHashMap<StreamController, MediaProjection>()
private val screenRequests = WeakHashMap<StreamController, (() -> Unit)>()

var StreamController.onRequestScreenCapture: (() -> Unit)?
    get() = screenRequests[this]
    set(value) { if (value == null) screenRequests.remove(this) else screenRequests[this] = value }

fun StreamController.requestScreenCapture() {
    onRequestScreenCapture?.invoke() ?: setMessage("Autorise la capture d'écran avec Android.")
}

private fun StreamController.streamReflect(): GenericStream = runCatching {
    StreamController::class.java.getDeclaredField("stream").apply { isAccessible = true }
        .get(this) as GenericStream
}.getOrElse { error("Moteur vidéo indisponible") }

private fun StreamController.contextReflect(): Context =
    StreamController::class.java.getDeclaredField("app").apply { isAccessible = true }
        .get(this) as Context

private fun StreamController.setAudioState(source: String? = null, message: String? = null) {
    val field = StreamController::class.java.getDeclaredField("_ui").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val flow = field.get(this) as kotlinx.coroutines.flow.MutableStateFlow<RtmpUi>
    val old = flow.value
    flow.value = old.copy(
        audioSource = source ?: old.audioSource,
        message = message
    )
}

private fun StreamController.setMessage(message: String?) = setAudioState(message = message)
    val field = StreamController::class.java.getDeclaredField("_ui").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val flow = field.get(this) as kotlinx.coroutines.flow.MutableStateFlow<RtmpUi>
    flow.value = flow.value.copy(message = message)
}

fun StreamController.setScreenProjection(projection: MediaProjection?) {
    if (projection == null) {
        setMessage("La capture d'écran n'a pas été autorisée.")
        return
    }
    runCatching {
        projections[this]?.let { if (it !== projection) runCatching { it.stop() } }
        projections[this] = projection
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                if (projections[this@setScreenProjection] === projection) projections.remove(this@setScreenProjection)
                useCameraSource()
            }
        }, android.os.Handler(android.os.Looper.getMainLooper()))
        val stream = streamReflect()
        stream.getGlInterface().setForceRender(true, 15)
        stream.changeVideoSource(ScreenSource(contextReflect(), projection))
        setMessage(null)
        log("Capture d'écran activée")
    }.onFailure { err ->
        setMessage("Capture écran indisponible : ${err.message}")
        log("Capture écran : ${err.message}")
    }
}

fun StreamController.useCameraSource() {
    runCatching { projections.remove(this)?.stop() }
    runCatching { streamReflect().changeVideoSource(Camera2Source(contextReflect())) }
        .onSuccess { setMessage(null); log("Source caméra activée") }
        .onFailure { err -> setMessage("Caméra indisponible : ${err.message}") }
}

fun StreamController.hasScreenProjection(): Boolean = projections[this] != null

fun StreamController.useMicrophoneAudio() = changeAudio(MicrophoneSource(), "Microphone")

fun StreamController.useInternalAudio() {
    val projection = projections[this]
    if (android.os.Build.VERSION.SDK_INT < 29 || projection == null) {
        setMessage("Autorise d'abord la capture d'écran pour l'audio système.")
        return
    }
    changeAudio(InternalAudioSource(projection), "Audio système")
}

fun StreamController.useMixedAudio() {
    val projection = projections[this]
    if (android.os.Build.VERSION.SDK_INT < 29 || projection == null) {
        setMessage("Autorise d'abord la capture d'écran pour mixer l'audio.")
        return
    }
    changeAudio(MixAudioSource(projection), "Micro + système")
}

private fun StreamController.changeAudio(source: Any, label: String) {
    runCatching {
        val stream = streamReflect()
        val method = stream.javaClass.methods.firstOrNull {
            it.name == "changeAudioSource" && it.parameterTypes.size == 1
        } ?: error("Changement audio non disponible")
        method.invoke(stream, source)
        setAudioState(source = label, message = null)
        log("Source audio : $label")
    }.onFailure { err -> setMessage("Audio indisponible : ${err.message}") }
}
