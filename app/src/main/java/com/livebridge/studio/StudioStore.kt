package com.livebridge.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.livebridge.Prefs
import com.livebridge.rtmp.StreamController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Scènes et sources façon OBS, sauvegardées sur le téléphone.
 * Chaque modification de la scène active est appliquée immédiatement au flux.
 */
class StudioStore(
    context: Context,
    private val prefs: Prefs,
    private val controller: StreamController
) {
    private val dir = File(context.filesDir, "sources").apply { mkdirs() }
    private val bitmaps = HashMap<String, Bitmap>()

    private val _state = MutableStateFlow(load())
    val state: StateFlow<StudioState> = _state.asStateFlow()

    init {
        // Ré-applique la scène active à chaque (re)démarrage de l'aperçu.
        controller.onPreviewStarted = { render() }
    }

    private fun load(): StudioState =
        prefs.get("studio").takeIf { it.isNotBlank() }?.let { parseStudio(it) } ?: defaultState()

    private fun commit(s: StudioState, rerender: Boolean = true) {
        _state.value = s
        prefs.put("studio", s.toJson())
        if (rerender) render()
    }

    fun render() {
        controller.renderScene(_state.value.current) { imageFor(it) }
    }

    private fun imageFor(s: Source): Bitmap? {
        if (!s.hasImage) return null
        bitmaps[s.id]?.let { return it }
        val bmp = BitmapFactory.decodeFile(File(dir, "${s.id}.png").absolutePath) ?: return null
        bitmaps[s.id] = bmp
        return bmp
    }

    private fun saveImage(id: String, bmp: Bitmap) {
        File(dir, "$id.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmaps[id] = bmp
    }

    private fun deleteImage(id: String) {
        File(dir, "$id.png").delete()
        bitmaps.remove(id)
    }

    // ---- Scènes --------------------------------------------------------------------------

    fun selectScene(id: String) {
        if (_state.value.currentId != id) commit(_state.value.copy(currentId = id))
    }

    fun addScene(name: String) {
        val sc = Scene(
            name = name.ifBlank { "Scène" },
            sources = listOf(Source(name = "Caméra", type = SourceType.CAMERA))
        )
        commit(StudioState(_state.value.scenes + sc, sc.id))
    }

    fun renameScene(id: String, name: String) {
        if (name.isBlank()) return
        commit(
            _state.value.copy(scenes = _state.value.scenes.map { if (it.id == id) it.copy(name = name.trim()) else it }),
            rerender = false
        )
    }

    fun duplicateScene(id: String) {
        val src = _state.value.scenes.firstOrNull { it.id == id } ?: return
        val copies = src.sources.map { s ->
            val newSource = s.copy(id = newId())
            if (s.hasImage) {
                File(dir, "${s.id}.png").takeIf { it.exists() }
                    ?.copyTo(File(dir, "${newSource.id}.png"), overwrite = true)
            }
            newSource
        }
        val sc = Scene(name = src.name + " (copie)", sources = copies)
        commit(StudioState(_state.value.scenes + sc, sc.id))
    }

    fun deleteScene(id: String) {
        val s = _state.value
        if (s.scenes.size <= 1) return
        s.scenes.firstOrNull { it.id == id }?.sources?.forEach { if (it.hasImage) deleteImage(it.id) }
        val rest = s.scenes.filter { it.id != id }
        commit(StudioState(rest, if (s.currentId == id) rest.first().id else s.currentId))
    }

    // ---- Sources de la scène active ------------------------------------------------------

    private fun editCurrent(rerender: Boolean = true, transform: (Scene) -> Scene) {
        val s = _state.value
        commit(
            s.copy(scenes = s.scenes.map { if (it.id == s.currentId) transform(it) else it }),
            rerender
        )
    }

    private fun mapSource(id: String, f: (Source) -> Source): (Scene) -> Scene = { sc ->
        sc.copy(sources = sc.sources.map { if (it.id == id) f(it) else it })
    }

    fun toggleVisible(id: String) = editCurrent(transform = mapSource(id) { it.copy(visible = !it.visible) })

    fun toggleLock(id: String) = editCurrent(rerender = false, transform = mapSource(id) { it.copy(locked = !it.locked) })

    /** Déplacement au doigt : delta en % de l'aperçu, borné pour garder la source visible. */
    fun moveSource(id: String, dxPercent: Float, dyPercent: Float) = editCurrent(transform = mapSource(id) { s ->
        if (s.locked) return@mapSource s
        // Bornée à 0..100 (positions négatives non garanties par l'API de rendu par réflexion).
        s.copy(
            x = clamp(s.x + dxPercent, 0f, 100f),
            y = clamp(s.y + dyPercent, 0f, 100f)
        )
    })

    /** Pincement : nouvelle taille en % de la largeur de l'aperçu (5 à 100). */
    fun resizeSource(id: String, size: Float) = editCurrent(transform = mapSource(id) { s ->
        if (s.locked) return@mapSource s
        s.copy(size = clamp(size, 5f, 100f))
    })

    fun rotateSource(id: String, deltaDegrees: Float) = editCurrent(transform = mapSource(id) { s ->
        if (s.locked) return@mapSource s
        s.copy(rotation = ((s.rotation + deltaDegrees + 180f) % 360f) - 180f)
    })

    fun resetTransform(id: String) = editCurrent(transform = mapSource(id) {
        it.copy(x = 10f, y = 10f, size = 25f, rotation = 0f)
    })

    fun transformSource(
        id: String,
        dxPercent: Float,
        dyPercent: Float,
        scaleFactor: Float,
        rotationDelta: Float,
        pivotXPercent: Float = 0f,
        pivotYPercent: Float = 0f
    ) = editCurrent(transform = mapSource(id) { s ->
        if (s.locked) return@mapSource s
        val newSize = clamp(s.size * scaleFactor, 5f, 100f)
        val newRotation = ((s.rotation + rotationDelta + 180f) % 360f) - 180f
        val newX = clamp(s.x + dxPercent, 0f, 100f)
        val newY = clamp(s.y + dyPercent, 0f, 100f)
        s.copy(x = newX, y = newY, size = newSize, rotation = newRotation)
    })

    private fun clamp(v: Float, lo: Float, hi: Float) = max(lo, min(hi, v))

    fun updateText(id: String, text: String, textSize: Int, color: Int) =
        editCurrent(transform = mapSource(id) { it.copy(text = text, textSize = textSize, color = color) })

    fun rename(id: String, name: String) {
        if (name.isBlank()) return
        editCurrent(rerender = false, transform = mapSource(id) { it.copy(name = name.trim()) })
    }

    fun addText(name: String) {
        val src = Source(name = name, type = SourceType.TEXT, text = "Mon texte", x = 10f, y = 80f, size = 80f, textSize = 50)
        editCurrent { it.copy(sources = ensureCamera(listOf(src) + it.sources)) }
    }

    fun addImage(name: String, bmp: Bitmap) {
        val id = newId()
        saveImage(id, bmp)
        val src = Source(id = id, name = name, type = SourceType.IMAGE, hasImage = true, x = 65f, y = 5f, size = 25f)
        editCurrent { it.copy(sources = ensureCamera(listOf(src) + it.sources)) }
    }

    fun replaceImage(id: String, bmp: Bitmap) {
        saveImage(id, bmp)
        editCurrent(transform = mapSource(id) { it.copy(hasImage = true) })
    }

    fun removeSource(id: String) {
        val target = _state.value.current.sources.firstOrNull { it.id == id } ?: return
        if (target.type == SourceType.CAMERA) return
        if (target.hasImage) deleteImage(id)
        editCurrent { it.copy(sources = it.sources.filter { s -> s.id != id }) }
    }

    /** delta = -1 : monter (vers le premier plan), +1 : descendre. La caméra reste au fond. */
    fun moveLayer(id: String, delta: Int) = editCurrent { sc ->
        val list = sc.sources.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j < 0 || j >= list.size) return@editCurrent sc
        if (list[i].type == SourceType.CAMERA || list[j].type == SourceType.CAMERA) return@editCurrent sc
        val tmp = list[i]; list[i] = list[j]; list[j] = tmp
        sc.copy(sources = list)
    }
}
