package com.livebridge.studio

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString().take(8)

enum class SourceType(val label: String) {
    CAMERA("Caméra"),
    IMAGE("Image"),
    TEXT("Texte")
}

/**
 * Position et taille libres, réglées au doigt sur l'aperçu.
 * x, y = coin haut-gauche de la source, en % de la largeur/hauteur de l'aperçu (0-100).
 * size = largeur de la source, en % de la largeur de l'aperçu.
 */
data class Source(
    val id: String = newId(),
    val name: String,
    val type: SourceType,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val text: String = "",
    val hasImage: Boolean = false,      // fichier filesDir/sources/<id>.png
    val x: Float = 10f,
    val y: Float = 10f,
    val size: Float = 25f,
    val rotation: Float = 0f,
    val textSize: Int = 40,             // texte : taille de police
    val color: Int = -1                 // texte : couleur ARGB (-1 = blanc)
)

data class Scene(
    val id: String = newId(),
    val name: String,
    val sources: List<Source>           // index 0 = au premier plan (comme OBS)
)

data class StudioState(val scenes: List<Scene>, val currentId: String) {
    val current: Scene get() = scenes.firstOrNull { it.id == currentId } ?: scenes.first()
}

fun defaultState(): StudioState {
    val s1 = Scene(
        id = "scene_1",
        name = "Scène 1",
        sources = listOf(
            Source(name = "Caméra", type = SourceType.CAMERA),
            Source(name = "Logo", type = SourceType.TEXT, text = "LiveBridge", x = 5f, y = 5f, size = 30f, textSize = 36),
            Source(name = "Titre", type = SourceType.TEXT, text = "Discussion en direct", x = 10f, y = 82f, size = 80f, textSize = 44)
        )
    )
    val s2 = Scene(
        id = "scene_2",
        name = "Scène 2",
        sources = listOf(
            Source(name = "Caméra", type = SourceType.CAMERA),
            Source(name = "Écran", type = SourceType.CAMERA)
        )
    )
    val s3 = Scene(
        id = "scene_3",
        name = "Scène 3",
        sources = listOf(
            Source(name = "Caméra", type = SourceType.CAMERA)
        )
    )
    val s4 = Scene(
        id = "scene_4",
        name = "Scène 4",
        sources = listOf(
            Source(
                name = "Texte BRB",
                type = SourceType.TEXT,
                text = "BRB - Je reviens tout de suite !",
                x = 10f, y = 45f, size = 80f, textSize = 54
            ),
            Source(name = "Caméra", type = SourceType.CAMERA, visible = false)
        )
    )
    return StudioState(listOf(s1, s2, s3, s4), s1.id)
}

/** La caméra est toujours la source du fond (dernière de la liste). */
fun ensureCamera(sources: List<Source>): List<Source> {
    val cam = sources.firstOrNull { it.type == SourceType.CAMERA }
        ?: Source(name = "Caméra", type = SourceType.CAMERA)
    return sources.filter { it.type != SourceType.CAMERA } + cam
}

fun StudioState.toJson(): String {
    val scenesJson = JSONArray()
    for (sc in scenes) {
        val sourcesJson = JSONArray()
        for (s in sc.sources) {
            sourcesJson.put(
                JSONObject()
                    .put("id", s.id).put("name", s.name).put("type", s.type.name)
                    .put("visible", s.visible).put("locked", s.locked)
                    .put("text", s.text).put("hasImage", s.hasImage)
                    .put("x", s.x).put("y", s.y).put("size", s.size).put("rotation", s.rotation)
                    .put("textSize", s.textSize).put("color", s.color)
            )
        }
        scenesJson.put(JSONObject().put("id", sc.id).put("name", sc.name).put("sources", sourcesJson))
    }
    return JSONObject().put("current", currentId).put("scenes", scenesJson).toString()
}

fun parseStudio(json: String): StudioState? = runCatching {
    val root = JSONObject(json)
    val arr = root.getJSONArray("scenes")
    val scenes = mutableListOf<Scene>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val sa = o.getJSONArray("sources")
        val sources = mutableListOf<Source>()
        for (j in 0 until sa.length()) {
            val so = sa.getJSONObject(j)
            sources += Source(
                id = so.getString("id"),
                name = so.getString("name"),
                type = SourceType.valueOf(so.getString("type")),
                visible = so.optBoolean("visible", true),
                locked = so.optBoolean("locked", false),
                text = so.optString("text"),
                hasImage = so.optBoolean("hasImage", false),
                x = so.optDouble("x", 10.0).toFloat(),
                y = so.optDouble("y", 10.0).toFloat(),
                size = so.optDouble("size", 25.0).toFloat(),
                rotation = so.optDouble("rotation", 0.0).toFloat(),
                textSize = so.optInt("textSize", 40),
                color = so.optInt("color", -1)
            )
        }
        scenes += Scene(o.getString("id"), o.getString("name"), ensureCamera(sources))
    }
    if (scenes.isEmpty()) return@runCatching null
    val cur = root.optString("current")
    StudioState(scenes, if (scenes.any { it.id == cur }) cur else scenes.first().id)
}.getOrNull()
