package com.livebridge.rtmp

import android.content.Context
import android.graphics.Bitmap
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import com.livebridge.LiveService
import com.livebridge.studio.Scene
import com.livebridge.studio.Source
import com.livebridge.studio.SourceType
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.gl.render.filters.`object`.BaseObjectFilterRender
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.encoder.input.gl.render.filters.`object`.TextObjectFilterRender
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.ScreenSource
import com.pedro.encoder.input.sources.audio.InternalAudioSource
import com.pedro.encoder.input.sources.audio.MixAudioSource
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.utils.gl.TranslateTo
import com.pedro.library.generic.GenericStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.time.LocalTime

/**
 * Résolutions PORTRAIT (largeur < hauteur), pour un rendu plein écran sur téléphone,
 * sans bandes noires comme les apps de live verticales.
 */
enum class Quality(val label: String, val portraitW: Int, val portraitH: Int, val bitrate: Int) {
    P480("480p", 480, 854, 1_200_000),
    P720("720p", 720, 1280, 2_500_000),
    P1080("1080p", 1080, 1920, 4_500_000);

    /** Dimensions réelles d'encodage selon l'orientation (les valeurs de base sont en portrait). */
    fun dims(landscape: Boolean): Pair<Int, Int> =
        if (landscape) portraitH to portraitW else portraitW to portraitH
}

data class RtmpUi(
    val streaming: Boolean = false,
    val connecting: Boolean = false,
    val liveSince: Long = 0,
    val bitrateKbps: Long = 0,
    val quality: Quality = Quality.P720,
    val landscape: Boolean = false,
    val sceneName: String = "",
    val recording: Boolean = false,
    val lastRecordPath: String? = null,
    val micMuted: Boolean = false,
    val noiseReduction: Boolean = true,
    val message: String? = null,
    val audioSource: String = "Microphone",
    val videoSource: String = "Caméra"
)

/**
 * Enveloppe de RootEncoder (GenericStream) : caméra + micro -> RTMP / RTMPS / SRT (+ enregistrement local).
 * Singleton : le flux survit aux changements d'onglet et de configuration.
 */
class StreamController private constructor(private val app: Context) : ConnectChecker {

    private val main = Handler(Looper.getMainLooper())

    private val _ui = MutableStateFlow(RtmpUi())
    val ui: StateFlow<RtmpUi> = _ui.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    /** Appelé après chaque démarrage d'aperçu : le studio y ré-applique la scène active. */
    var onPreviewStarted: (() -> Unit)? = null
    var onRequestScreenCapture: (() -> Unit)? = null
    private var mediaProjection: MediaProjection? = null

    private val stream = GenericStream(app, this).apply {
        getGlInterface().autoHandleOrientation = true
    }

    private var preparedQuality: Quality? = null
    private var preparedNoise: Boolean? = null
    private var preparedLandscape: Boolean? = null
    private var surface: SurfaceView? = null

    // Reconnexion automatique
    private var wantLive = false
    private var retries = 0
    private var lastUrl = ""
    private var retryTask: Runnable? = null

    // ---- Journal (sans jamais écrire la clé de stream) -----------------------------------

    fun log(message: String) {
        val line = "${LocalTime.now().withNano(0)}  $message"
        _logs.update { (it + line).takeLast(80) }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun describe(url: String): String = runCatching {
        val u = Uri.parse(url)
        "${u.scheme}://${u.host}" + (if (u.port > 0) ":${u.port}" else "")
    }.getOrDefault("(URL invalide)")

    // ---- Préparation encodeur ------------------------------------------------------------

    /**
     * Coupure d'écho / réduction de bruit : désactivées par défaut, car leur traitement ajoute
     * un léger délai entre l'audio et l'image sur beaucoup d'appareils. Essaie plusieurs
     * signatures par réflexion (elles varient selon la version de RootEncoder) ; si aucune
     * ne correspond, on repasse sur l'appel simple (aucun réglage n'est perdu, juste ignoré).
     */
    private fun prepareAudio(noiseReduction: Boolean): Boolean {
        val m = stream.javaClass.methods.firstOrNull {
            it.name == "prepareAudio" && it.parameterTypes.size == 5
        }
        if (m != null) {
            return runCatching {
                m.invoke(stream, 44100, true, 128_000, noiseReduction, noiseReduction) as Boolean
            }.getOrElse { stream.prepareAudio(44100, true, 128_000) }
        }
        return stream.prepareAudio(44100, true, 128_000)
    }

    /**
     * Cadence vidéo forcée à 30 im/s, intervalle d'images-clés à 2 s : essaie plusieurs
     * signatures de prepareVideo par réflexion (elles varient selon la version de RootEncoder),
     * repli sur l'appel simple (w, h, bitrate) sans cadence forcée si aucune ne correspond.
     *
     * Pourquoi : sans cadence imposée, si la caméra ralentit (faible luminosité, chauffe du
     * téléphone), l'image prend du retard sur le son, qui continue à un rythme fixe — c'est la
     * cause la plus courante d'un décalage audio/vidéo qui grandit au fil du direct.
     */
    private fun prepareVideoLocked(w: Int, h: Int, bitrate: Int): Boolean {
        val fps = 30
        val iFrameInterval = 2
        val candidates = listOf(
            listOf(Int::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java) to
                listOf(w, h, fps, bitrate, iFrameInterval),
            listOf(Int::class.java, Int::class.java, Int::class.java, Int::class.java) to
                listOf(w, h, fps, bitrate)
        )
        for ((types, args) in candidates) {
            val m = runCatching { stream.javaClass.getMethod("prepareVideo", *types.toTypedArray()) }.getOrNull()
            if (m != null) {
                val ok = runCatching { m.invoke(stream, *args.toTypedArray()) as Boolean }
                if (ok.isSuccess) {
                    log("Encodeur : cadence forcée à $fps im/s")
                    return ok.getOrDefault(false)
                }
            }
        }
        log("Encodeur : cadence forcée indisponible sur cette version, réglage par défaut utilisé")
        return stream.prepareVideo(w, h, bitrate)
    }

    private fun prepare(): Boolean {
        val q = _ui.value.quality
        val noise = _ui.value.noiseReduction
        val landscape = _ui.value.landscape
        if (preparedQuality == q && preparedNoise == noise && preparedLandscape == landscape) return true
        val (w, h) = q.dims(landscape)
        return try {
            val ok = prepareVideoLocked(w, h, q.bitrate) && prepareAudio(noise)
            if (ok) { preparedQuality = q; preparedNoise = noise; preparedLandscape = landscape }
            val orient = if (landscape) "paysage" else "portrait"
            log(if (ok) "Encodeur prêt (${q.label} $orient)" else "Encodeur refusé par l'appareil (${q.label} $orient)")
            ok
        } catch (e: IllegalArgumentException) {
            log("Encodeur : ${e.message}")
            false
        }
    }

    // ---- Aperçu --------------------------------------------------------------------------

    fun attachPreview(view: SurfaceView) {
        surface = view
        if (!prepare()) {
            _ui.update { it.copy(message = "Cet appareil ne supporte pas cette qualité.") }
            return
        }
        if (!stream.isOnPreview) {
            stream.startPreview(view)
            onPreviewStarted?.invoke()
        }
    }

    fun onPreviewSize(width: Int, height: Int) {
        stream.getGlInterface().setPreviewResolution(width, height)
    }

    fun detachPreview() {
        if (stream.isOnPreview) stream.stopPreview()
        surface = null
    }

    fun setQuality(q: Quality) {
        if (stream.isStreaming || wantLive || _ui.value.recording) return
        _ui.update { it.copy(quality = q, message = null) }
        reprepare()
    }

    /**
     * Bascule les dimensions d'encodage entre portrait et paysage, d'après la rotation du
     * téléphone. Verrouillée pendant un direct ou un enregistrement (changer les dimensions de
     * l'encodeur en plein direct le couperait) : l'orientation choisie au démarrage est gardée
     * jusqu'à l'arrêt.
     */
    fun setOrientation(landscape: Boolean) {
        if (stream.isStreaming || wantLive || _ui.value.recording) return
        if (_ui.value.landscape == landscape) return
        _ui.update { it.copy(landscape = landscape) }
        reprepare()
    }

    fun setNoiseReduction(enabled: Boolean) {
        if (stream.isStreaming || wantLive || _ui.value.recording) return
        _ui.update { it.copy(noiseReduction = enabled) }
        reprepare()
    }

    private fun reprepare() {
        val view = surface
        if (stream.isOnPreview) stream.stopPreview()
        if (prepare() && view != null) {
            stream.startPreview(view)
            onPreviewStarted?.invoke()
        }
    }

    fun requestScreenCapture() {
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

    fun switchCamera() {
        runCatching { (stream.videoSource as Camera2Source).switchCamera() }
            .onFailure { _ui.update { s -> s.copy(message = "Impossible de changer de caméra.") } }
    }

    // ---- Micro ---------------------------------------------------------------------------

    /**
     * Coupe / réactive le micro. Appel par réflexion : si cette version de RootEncoder n'expose pas
     * la méthode, l'app continue de fonctionner et l'indique dans le journal.
     */
    fun setMicMuted(muted: Boolean) {
        val source = runCatching { stream.javaClass.getMethod("getAudioSource").invoke(stream) }.getOrNull()
        val names = if (muted) listOf("mute") else listOf("unMute", "unmute")
        val ok = source != null && names.any { name ->
            runCatching { source.javaClass.getMethod(name).invoke(source) }.isSuccess
        }
        if (ok) {
            _ui.update { it.copy(micMuted = muted) }
            log(if (muted) "Micro coupé" else "Micro actif")
        } else {
            log("Couper le micro n'est pas supporté par cette version de RootEncoder")
            _ui.update { it.copy(message = "Impossible de couper le micro sur cette version.") }
        }
    }

    // ---- Rendu de la scène active (dessiné dans le flux) ---------------------------------

    /**
     * Position libre (essaye setPosition(Float,Float) en % par réflexion, car cette signature
     * n'est pas garantie selon la version de RootEncoder) avec repli sur l'ancrage TranslateTo
     * le plus proche du point choisi, pour que la source reste visible dans tous les cas.
     */
    private fun applyPosition(filter: BaseObjectFilterRender, xPercent: Float, yPercent: Float) {
        val direct = runCatching {
            val m = filter.javaClass.methods.firstOrNull {
                it.name == "setPosition" && it.parameterTypes.size == 2 &&
                    it.parameterTypes[0] in listOf(Float::class.java, Float::class.javaPrimitiveType)
            }
            m?.invoke(filter, xPercent, yPercent)
            m != null
        }.getOrDefault(false)
        if (direct) return

        val cx = xPercent + 12f; val cy = yPercent + 12f   // centre approximatif de la source
        val anchor = when {
            cy < 33f && cx < 33f -> TranslateTo.TOP_LEFT
            cy < 33f && cx > 66f -> TranslateTo.TOP_RIGHT
            cy < 33f -> TranslateTo.TOP
            cy > 66f && cx < 33f -> TranslateTo.BOTTOM_LEFT
            cy > 66f && cx > 66f -> TranslateTo.BOTTOM_RIGHT
            cy > 66f -> TranslateTo.BOTTOM
            cx < 33f -> TranslateTo.LEFT
            cx > 66f -> TranslateTo.RIGHT
            else -> TranslateTo.CENTER
        }
        filter.setPosition(anchor)
    }

    private fun applyRotation(filter: BaseObjectFilterRender, degrees: Float) {
        runCatching {
            val m = filter.javaClass.methods.firstOrNull {
                it.name == "setRotation" && it.parameterTypes.size == 1 &&
                    (it.parameterTypes[0] == Float::class.java || it.parameterTypes[0] == Float::class.javaPrimitiveType)
            }
            m?.invoke(filter, degrees)
        }
    }

    fun renderScene(scene: Scene, imageFor: (Source) -> Bitmap?) {
        _ui.update { it.copy(sceneName = scene.name) }
        if (!stream.isOnPreview && !stream.isStreaming) return   // rendu différé (onPreviewStarted)

        val gl = stream.getGlInterface()
        val q = _ui.value.quality
        val (qw, qh) = q.dims(_ui.value.landscape)
        gl.clearFilters()

        // Caméra masquée : on la recouvre d'un fond noir plein écran.
        val cam = scene.sources.firstOrNull { it.type == SourceType.CAMERA }
        if (cam == null || !cam.visible) {
            val black = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF000000.toInt()) }
            val f = ImageObjectFilterRender()
            gl.addFilter(f)
            f.setImage(black)
            f.setScale(100f, 100f)
            f.setPosition(TranslateTo.CENTER)
        }

        // asReversed : l'arrière-plan d'abord, le premier plan en dernier.
        for (s in scene.sources.asReversed()) {
            if (!s.visible) continue
            when (s.type) {
                SourceType.CAMERA -> Unit

                SourceType.IMAGE -> {
                    val bmp = imageFor(s) ?: continue
                    val f = ImageObjectFilterRender()
                    gl.addFilter(f)               // ajouter AVANT de configurer
                    f.setImage(bmp)
                    val sx = s.size
                    val sy = sx * (bmp.height.toFloat() / bmp.width) * (qw.toFloat() / qh)
                    f.setScale(sx, sy)
                    applyPosition(f, s.x, s.y)
                    applyRotation(f, s.rotation)
                }

                SourceType.TEXT -> {
                    if (s.text.isBlank()) continue
                    val f = TextObjectFilterRender()
                    gl.addFilter(f)
                    f.setText(s.text.trim(), s.textSize.toFloat(), s.color)
                    applyPosition(f, s.x, s.y)
                    applyRotation(f, s.rotation)
                }
            }
        }
        log("Scène : ${scene.name}")
    }

    // ---- Enregistrement local ------------------------------------------------------------

    fun toggleRecord() {
        if (_ui.value.recording) {
            runCatching { stream.stopRecord() }
            _ui.update { it.copy(recording = false) }
            log("Enregistrement terminé : ${_ui.value.lastRecordPath}")
            return
        }
        if (!prepare()) {
            _ui.update { it.copy(message = "Encodeur indisponible.") }
            return
        }
        val dir = app.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: app.filesDir
        val file = File(dir, "LiveBridge_${System.currentTimeMillis()}.mp4")
        try {
            stream.startRecord(file.absolutePath) { status -> log("Enregistrement : $status") }
            _ui.update { it.copy(recording = true, lastRecordPath = file.absolutePath, message = null) }
            log("Enregistrement démarré")
        } catch (e: Exception) {
            log("Enregistrement impossible : ${e.message}")
            _ui.update { it.copy(message = "Enregistrement impossible : ${e.message}") }
        }
    }

    // ---- Diffusion -----------------------------------------------------------------------

    fun start(url: String) {
        if (stream.isStreaming || wantLive) return
        if (!prepare()) {
            _ui.update { it.copy(message = "Encodeur indisponible.") }
            return
        }
        lastUrl = url.trim()
        retries = 0
        wantLive = true
        _ui.update { it.copy(connecting = true, message = null) }
        log("Démarrage vers ${describe(lastUrl)}")
        launchStream()
    }

    private fun launchStream() {
        try {
            LiveService.start(app, "Live")
            stream.startStream(lastUrl)
        } catch (e: Exception) {
            log("Erreur au démarrage : ${e.message}")
            stop()
            _ui.update { it.copy(message = "Erreur : ${e.message}") }
        }
    }

    fun stop() {
        wantLive = false
        retryTask?.let { main.removeCallbacks(it) }
        retryTask = null
        if (stream.isStreaming) stream.stopStream()
        _ui.update { it.copy(streaming = false, connecting = false, bitrateKbps = 0, liveSince = 0) }
        LiveService.stop(app)
        log("Direct arrêté")
    }

    /**
     * Coupe puis relance la connexion vers le même serveur, sans repasser par l'écran de
     * configuration. Utile en dépannage : un décalage audio/vidéo qui s'installe pendant un long
     * direct vient souvent d'une dérive accumulée entre les horloges audio et vidéo de
     * l'encodeur ; redémarrer proprement la remet à zéro. Coupe quelques secondes le direct pour
     * les spectateurs pendant l'opération.
     */
    fun resync() {
        if (!stream.isStreaming && !wantLive) return
        val url = lastUrl
        if (url.isBlank()) return
        log("Resynchronisation demandée : redémarrage du flux")
        stop()
        main.postDelayed({ start(url) }, 600)
    }

    // ---- ConnectChecker (appelé depuis des threads d'arrière-plan) ---------------------

    override fun onConnectionStarted(url: String) {
        log("Connexion à ${describe(url)}…")
    }

    override fun onConnectionSuccess() {
        retries = 0
        log("Connecté : le serveur accepte le flux")
        _ui.update {
            it.copy(streaming = true, connecting = false, liveSince = System.currentTimeMillis(), message = null)
        }
    }

    override fun onConnectionFailed(reason: String) {
        log("Échec de connexion : $reason")
        main.post {
            if (wantLive && retries < MAX_RETRIES) {
                retries++
                log("Nouvelle tentative $retries/$MAX_RETRIES dans 3 s…")
                if (stream.isStreaming) stream.stopStream()
                _ui.update { it.copy(streaming = false, connecting = true) }
                val task = Runnable { if (wantLive) launchStream() }
                retryTask = task
                main.postDelayed(task, 3000)
            } else {
                stop()
                _ui.update { it.copy(message = "Connexion échouée : $reason") }
            }
        }
    }

    override fun onNewBitrate(bitrate: Long) {
        _ui.update { it.copy(bitrateKbps = bitrate / 1000) }
    }

    override fun onDisconnect() {
        log("Déconnecté du serveur")
        _ui.update { it.copy(streaming = false, bitrateKbps = 0) }
    }

    override fun onAuthError() {
        log("Authentification refusée (clé ou identifiants invalides)")
        main.post {
            stop()
            _ui.update { it.copy(message = "Authentification refusée par le serveur : vérifie la clé de stream.") }
        }
    }

    override fun onAuthSuccess() {
        log("Authentification acceptée")
    }

    companion object {
        private const val MAX_RETRIES = 3
        @Volatile private var instance: StreamController? = null

        fun get(context: Context): StreamController =
            instance ?: synchronized(this) {
                instance ?: StreamController(context.applicationContext).also { instance = it }
            }
    }
}
