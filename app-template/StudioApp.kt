package com.livebridge.ui

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.livebridge.Prefs
import com.livebridge.rtmp.*
import com.livebridge.studio.StudioStore

@Composable
fun StudioApp(controller: StreamController, store: StudioStore, prefs: Prefs, ui: RtmpUi) {
    var server by remember { mutableStateOf(prefs.get("server", "rtmp://")) }
    var key by remember { mutableStateOf(prefs.get("key", "")) }
    var activeTab by remember { mutableStateOf("Studio") }
    var showSettings by remember { mutableStateOf(false) }
    var selectedSource by remember { mutableStateOf("Caméra") }
    var micVolume by remember { mutableFloatStateOf(0.80f) }
    var systemVolume by remember { mutableFloatStateOf(0.60f) }
    var transition by remember { mutableStateOf("Fondu") }
    var transitionMs by remember { mutableFloatStateOf(300f) }
    val state by store.state.collectAsState()

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF080A0F))) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    ObsTopBar(ui, controller, onSettings = { showSettings = true })
                    ObsPreview(controller, ui, Modifier.weight(1f).padding(10.dp))
                    SceneStrip(state.scenes, state.currentId, store)
                }
                ObsRail(
                    Modifier.width(350.dp).fillMaxHeight(),
                    activeTab, { activeTab = it }, ui, controller,
                    server, { server = it; prefs.put("server", it) },
                    key, { key = it; prefs.put("key", it) },
                    selectedSource, { selectedSource = it },
                    micVolume, { micVolume = it },
                    systemVolume, { systemVolume = it },
                    transition, { transition = it },
                    transitionMs, { transitionMs = it }
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                ObsTopBar(ui, controller, onSettings = { showSettings = true })
                ObsPreview(controller, ui, Modifier.fillMaxWidth().heightIn(min = 210.dp, max = 330.dp).padding(10.dp))
                SceneStrip(state.scenes, state.currentId, store)
                ObsRail(
                    Modifier.fillMaxWidth().weight(1f),
                    activeTab, { activeTab = it }, ui, controller,
                    server, { server = it; prefs.put("server", it) },
                    key, { key = it; prefs.put("key", it) },
                    selectedSource, { selectedSource = it },
                    micVolume, { micVolume = it },
                    systemVolume, { systemVolume = it },
                    transition, { transition = it },
                    transitionMs, { transitionMs = it }
                )
            }
        }
        if (showSettings) {
            ObsSettingsDialog(onDismiss = { showSettings = false })
        }
    }
}

@Composable
private fun ObsTopBar(ui: RtmpUi, controller: StreamController, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(58.dp).background(Color(0xFF11151C)).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(color = Color(0xFF7C8CFF), shape = RoundedCornerShape(8.dp)) {
            Text("LB", color = Color.White, fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp))
        }
        Column(Modifier.weight(1f).padding(start = 9.dp)) {
            Text("LIVEBRIDGE", fontWeight = FontWeight.Black, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(if (ui.streaming) Color(0xFFFF3F5E) else Color(0xFF35D07F)))
                Spacer(Modifier.width(5.dp))
                Text(if (ui.streaming) "EN DIRECT" else "PRÊT", fontSize = 10.sp,
                    color = if (ui.streaming) Color(0xFFFF3F5E) else Color(0xFF35D07F), fontWeight = FontWeight.Bold)
            }
        }
        IconButton(onClick = { controller.switchCamera() }) { Icon(Icons.Default.Cameraswitch, "Caméra") }
        IconButton(onClick = { controller.setMicMuted(!ui.micMuted) }) { Icon(if (ui.micMuted) Icons.Default.MicOff else Icons.Default.Mic, "Micro") }
        IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Paramètres") }
    }
}

@Composable
private fun ObsPreview(controller: StreamController, ui: RtmpUi, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                SurfaceView(context).also { view ->
                    view.holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            if (holder.surface.isValid) controller.attachPreview(view)
                        }
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                            if (holder.surface.isValid) controller.onPreviewSize(width, height)
                        }
                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            controller.detachPreview()
                        }
                    })
                }
            }
        )
        Surface(Modifier.align(Alignment.TopStart).padding(9.dp),
            color = Color(0xD9000000), shape = RoundedCornerShape(6.dp)) {
            Text(if (ui.streaming) "● LIVE" else "APERÇU",
                color = if (ui.streaming) Color(0xFFFF3F5E) else Color.White,
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
        }
        Surface(Modifier.align(Alignment.BottomStart).padding(9.dp),
            color = Color(0xCC000000), shape = RoundedCornerShape(6.dp)) {
            Text("PROGRAM  •  16:9", color = Color.White, fontSize = 9.sp,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
        }
        if (ui.connecting) {
            Surface(Modifier.align(Alignment.Center), color = Color(0xEE11151C), shape = RoundedCornerShape(10.dp)) {
                Text("CONNEXION…", color = Color(0xFFFFC857), fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(14.dp))
            }
        }
    }
}

@Composable
private fun ObsRail(
    modifier: Modifier, activeTab: String, onTab: (String) -> Unit, ui: RtmpUi,
    controller: StreamController, server: String, onServer: (String) -> Unit,
    key: String, onKey: (String) -> Unit, selectedSource: String, onSource: (String) -> Unit,
    micVolume: Float, onMicVolume: (Float) -> Unit, systemVolume: Float, onSystemVolume: (Float) -> Unit,
    transition: String, onTransition: (String) -> Unit, transitionMs: Float, onTransitionMs: (Float) -> Unit
) {
    Column(modifier.background(Color(0xFF10141B)).padding(9.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("Studio", "Sources", "Mixeur", "Transitions", "Diffusion").forEach { tab ->
                FilterChip(
                    selected = activeTab == tab,
                    onClick = { onTab(tab) },
                    label = { Text(tab.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        when (activeTab) {
            "Studio" -> {
                ObsSources(selectedSource, onSource, controller)
                Spacer(Modifier.height(8.dp))
                ObsMixer(ui, micVolume, onMicVolume, systemVolume, onSystemVolume)
                Spacer(Modifier.height(8.dp))
                ObsTransitions(transition, onTransition, transitionMs, onTransitionMs)
                Spacer(Modifier.height(8.dp))
                ObsControls(ui, controller, server, key)
            }
            "Sources" -> ObsSources(selectedSource, onSource, controller)
            "Mixeur" -> ObsMixer(ui, micVolume, onMicVolume, systemVolume, onSystemVolume)
            "Transitions" -> ObsTransitions(transition, onTransition, transitionMs, onTransitionMs)
            "Diffusion" -> ObsBroadcast(ui, controller, server, onServer, key, onKey)
        }
        ui.message?.let {
            Text(it, color = Color(0xFFFF3F5E), fontSize = 10.sp, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun ObsPanel(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF181D26))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF7C8CFF), modifier = Modifier.size(17.dp))
            Text(title, Modifier.weight(1f).padding(start = 7.dp), fontSize = 10.sp, fontWeight = FontWeight.Black)
            Icon(Icons.Default.MoreVert, null, tint = Color(0xFF8F98A8), modifier = Modifier.size(17.dp))
        }
        content()
    }
}

@Composable
private fun ObsSources(selected: String, onSource: (String) -> Unit, controller: StreamController) {
    ObsPanel("SOURCES", Icons.Default.Layers) {
        listOf("Caméra", "Écran", "Image / logo", "Texte").forEach { name ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp))
                    .background(if (selected == name) Color(0x332F5BFF) else Color.Transparent)
                    .clickable {
                        onSource(name)
                        when (name) {
                            "Caméra" -> controller.useCameraSource()
                            "Écran" -> controller.requestScreenCapture()
                        }
                    }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when (name) {
                        "Caméra" -> Icons.Default.Videocam
                        "Écran" -> Icons.Default.ScreenShare
                        "Image / logo" -> Icons.Default.Image
                        else -> Icons.Default.TextFields
                    }, null, tint = if (selected == name) Color(0xFF7C8CFF) else Color(0xFF8F98A8),
                    modifier = Modifier.size(17.dp)
                )
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(name, fontSize = 11.sp)
                    if (name == "Écran") Text("Autorisation Android requise", fontSize = 8.sp, color = Color(0xFF727B8B))
                }
                Icon(Icons.Default.Visibility, null, tint = Color(0xFF8F98A8), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Lock, null, tint = Color(0xFF8F98A8), modifier = Modifier.size(14.dp))
            }
        }
        OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth().height(38.dp)) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text("AJOUTER UNE SOURCE", fontSize = 10.sp)
        }
    }
}

@Composable
private fun ObsMixer(ui: RtmpUi, mic: Float, onMic: (Float) -> Unit, system: Float, onSystem: (Float) -> Unit) {
    ObsPanel("MIXEUR AUDIO", Icons.Default.VolumeUp) {
        ObsFader("Microphone", mic, onMic, ui.micMuted)
        ObsFader("Audio système", system, onSystem, false)
    }
}

@Composable
private fun ObsFader(name: String, value: Float, onValue: (Float) -> Unit, muted: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row {
                Text(name, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text((value * 100).toInt().toString() + "%", color = Color(0xFF8F98A8), fontSize = 9.sp)
            }
            Slider(value = value, onValueChange = onValue, modifier = Modifier.fillMaxWidth().height(28.dp))
        }
        Column(Modifier.padding(start = 8.dp).width(10.dp).height(38.dp), verticalArrangement = Arrangement.Bottom) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(value.coerceIn(0.03f, 1f)).background(if (muted) Color(0xFFFF3F5E) else Color(0xFF35D07F), RoundedCornerShape(3.dp)))
        }
    }
}

@Composable
private fun ObsTransitions(type: String, onType: (String) -> Unit, ms: Float, onMs: (Float) -> Unit) {
    ObsPanel("TRANSITIONS", Icons.Default.SwapHoriz) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("Cut", "Fondu", "Glissement").forEach {
                FilterChip(selected = type == it, onClick = { onType(it) }, label = { Text(it, fontSize = 9.sp) })
            }
        }
        Text("DURÉE " + ms.toInt().toString() + " ms", color = Color(0xFF8F98A8), fontSize = 9.sp)
        Slider(value = ms, onValueChange = onMs, valueRange = 100f..2000f)
    }
}

@Composable
private fun ObsControls(ui: RtmpUi, controller: StreamController, server: String, key: String) {
    ObsPanel("CONTRÔLES", Icons.Default.Tune) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ControlTile("MIC", Icons.Default.MicOff) { controller.setMicMuted(!ui.micMuted) }
            ControlTile("CAM", Icons.Default.Cameraswitch) { controller.switchCamera() }
            ControlTile("REC", if (ui.recording) Icons.Default.StopCircle else Icons.Default.Radio) { controller.toggleRecord() }
            ControlTile("SYNC", Icons.Default.Sync) { controller.resync() }
        }
        Spacer(Modifier.height(2.dp))
        if (ui.streaming || ui.connecting) {
            Button(onClick = { controller.stop() }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3F5E))) {
                Icon(Icons.Default.Stop, null)
                Spacer(Modifier.width(6.dp))
                Text("ARRÊTER LA DIFFUSION", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = { controller.start(server.trimEnd('/') + "/" + key.trim()) },
                enabled = server.startsWith("rtmp") && key.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text("COMMENCER LE DIRECT", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RowScope.ControlTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit) {
    Surface(Modifier.weight(1f).height(52.dp).clickable { action() },
        color = Color(0xFF11151C), shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A303C))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, modifier = Modifier.size(17.dp))
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ObsBroadcast(
    ui: RtmpUi, controller: StreamController, server: String, onServer: (String) -> Unit,
    key: String, onKey: (String) -> Unit
) {
    ObsPanel("DIFFUSION", Icons.Default.Radio) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(color = Color(0xFF7C8CFF), shape = RoundedCornerShape(6.dp)) {
                Text("RTMP / RTMPS", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
            }
        }
        OutlinedTextField(server, onServer, Modifier.fillMaxWidth(), label = { Text("URL DU SERVEUR") }, singleLine = true)
        OutlinedTextField(key, onKey, Modifier.fillMaxWidth(), label = { Text("CLÉ DE STREAM") },
            singleLine = true, visualTransformation = PasswordVisualTransformation())
        Text("QUALITÉ VIDÉO", color = Color(0xFF8F98A8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Quality.values().forEach { q ->
                FilterChip(selected = ui.quality == q, onClick = { controller.setQuality(q) },
                    label = { Text(q.label, fontSize = 9.sp) })
            }
        }
        if (ui.streaming || ui.connecting) {
            Button(onClick = { controller.stop() }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3F5E))) {
                Text("ARRÊTER", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(onClick = { controller.start(server.trimEnd('/') + "/" + key.trim()) },
                enabled = server.startsWith("rtmp") && key.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("COMMENCER LE DIRECT", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ObsSettingsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PARAMÈTRES DU STUDIO", fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("CANVAS", color = Color(0xFF7C8CFF), fontSize = 10.sp, fontWeight = FontWeight.Black)
                SettingValue("Format", "16:9")
                SettingValue("Orientation", "Automatique")
                SettingValue("FPS", "30")
                Text("SORTIE", color = Color(0xFF7C8CFF), fontSize = 10.sp, fontWeight = FontWeight.Black)
                SettingValue("Résolution", "1280 × 720")
                SettingValue("Débit vidéo", "4500 Kbit/s")
                SettingValue("Encodeur", "H.264")
                Text("AUDIO", color = Color(0xFF7C8CFF), fontSize = 10.sp, fontWeight = FontWeight.Black)
                SettingValue("Fréquence", "48 kHz")
                SettingValue("Canaux", "Stéréo")
                Text("INTERFACE", color = Color(0xFF7C8CFF), fontSize = 10.sp, fontWeight = FontWeight.Black)
                SettingValue("Mode", "Studio mobile")
                SettingValue("Disposition", "OBS tactile")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("FERMER") } }
    )
}

@Composable
private fun SettingValue(name: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f), fontSize = 12.sp)
        Text(value, color = Color(0xFF8F98A8), fontSize = 12.sp)
    }
}

@Composable
private fun SceneStrip(
    scenes: List<com.livebridge.studio.Scene>,
    currentId: String,
    store: StudioStore
) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).background(Color(0xFF0E1218)).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            scenes.forEach { scene ->
                FilterChip(
                    selected = scene.id == currentId,
                    onClick = { store.selectScene(scene.id) },
                    label = { Text(scene.name, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }
        IconButton(onClick = { store.addScene("Scène ${scenes.size + 1}") }) {
            Icon(Icons.Default.Add, "Ajouter une scène")
        }
    }
}

@Composable
private fun SmallAction(label: String, active: Boolean, action: () -> Unit) {
    Surface(Modifier.clip(RoundedCornerShape(10.dp)).clickable { action() },
        color = if (active) LiveRed else MaterialTheme.colorScheme.surfaceVariant) {
        Text(label, Modifier.padding(horizontal = 15.dp, vertical = 9.dp))
    }
}
