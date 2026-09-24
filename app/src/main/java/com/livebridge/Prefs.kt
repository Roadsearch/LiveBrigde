package com.livebridge

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Stockage local privé (SharedPreferences, allowBackup=false).
 * Les clés de stream et mots de passe y sont enregistrés en clair : ils ne sortent pas de l'app,
 * mais ne les utilise pas sur un téléphone rooté / partagé.
 */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("livebridge", Context.MODE_PRIVATE)

    fun get(key: String, def: String = ""): String = sp.getString(key, def) ?: def

    fun put(key: String, value: String) {
        sp.edit().putString(key, value).apply()
    }
}

/** État Compose sauvegardé automatiquement dans [Prefs]. */
@Composable
fun rememberPref(prefs: Prefs, key: String, def: String = ""): MutableState<String> {
    val state = remember(key) { mutableStateOf(prefs.get(key, def)) }
    LaunchedEffect(state.value) { prefs.put(key, state.value) }
    return state
}
