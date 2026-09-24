package com.livebridge.ui

import android.view.MotionEvent
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.livebridge.Prefs
import com.livebridge.rtmp.*
import com.livebridge.studio.StudioStore
import com.livebridge.studio.Source

@Composable
fun StudioApp(controller: StreamController, store: StudioStore, prefs: Prefs, ui: RtmpUi, onPickImage: () -> Unit) {
    var server by remember { mutableStateOf(prefs.get("server", "rtmp://")) }
    var key by remember { mutableStateOf(prefs.get("key", "")) }
    var activeTab by remember { mutableStateOf("Studio") }
    var showSettings by remember { mutableStateOf(false) }
    var showSceneManager by remember { mutableStateOf(false) }
    var showSourceManager by remember { mutableStateOf(false) }
    var showDashboard by remember { mutableStateOf(false) }
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
                    ObsTopBar(ui, controller, onSettings = { showSettings = true }, onDashboard = { showDashboard = true })
                    ObsPreview(controller, ui, store, state.current.sources.firstOrNull { it.name == selectedSource }, Modifier.weight(1f).padding(10.dp))
                    SceneStrip(state.scenes, state.currentId, store)
                }
                ObsRail(
                    Modifier.width(350.dp).fillMaxHeight(),
                    activeTab, { activeTab = it }, ui, controller,
                    server, { server = it; prefs.put("server", it) },
                    key, { key = it; prefs.put("key", it) },
                    selectedSource, { selectedSource = it }, store, onPickImage, onSourceManager = { showSourceManager = true },
                    micVolume, { micVolume = it },
                    systemVolume, { systemVolume = it },
                    transition, { transition = it },
                    transitionMs, { transitionMs = it }, onSourceManager = { showSourceManager = true }, onSceneManager = { showSceneManager = true }
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                ObsTopBar(ui, controller, onSettings = { showSettings = true }, onDashboard = { showDashboard = true })
                ObsPreview(controller, ui, store, state.current.sources.firstOrNull { it.name == selectedSource }, Modifier.fillMaxWidth().heightIn(min = 210.dp, max = 330.dp).padding(10.dp))
                SceneStrip(state.scenes, state.currentId, store)
                ObsRail(
                    Modifier.fillMaxWidth().weight(1f),
                    activeTab, { activeTab = it }, ui, controller,
                    server, { server = it; prefs.put("server", it) },
                    key, { key = it; prefs.put("key", it) },
                    selectedSource, { selectedSource = it }, store, onPickImage, onSourceManager = { showSourceManager = true },
                    micVolume, { micVolume = it },
                    systemVolume, { systemVolume = it },
                    transition, { transition = it },
                    transitionMs, { transitionMs = it }, onSourceManager = { showSourceManager = true }, onSceneManager = { showSceneManager = true }
                )
            }
        }
        if (showSettings) {
            ObsSettingsDialog(ui, controller, onDismiss = { showSettings = false })
        }
        if (showSceneManager) {
            SceneManagerDialog(state.scenes, state.currentId, store, onDismiss = { showSceneManager = false })
        }
        if (showSourceManager) {
            state.current.sources.firstOrNull { it.name == selectedSource }?.let { src ->
                SourceManagerDialog(src, store, onDismiss = { showSourceManager = false })
            }
        }
        if (showDashboard) {
            LiveDashboardDialog(ui, controller, onDismiss = { showDashboard = false })
        }
    }
}

@Composable
private fun ObsTopBar(ui: RtmpUi, controller: StreamController, onSettings: () -> Unit, onDashboard: () -> Unit) {
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
        IconButton(onClick = onDashboard) { Icon(Icons.Default.Monitor, "Tableau de bord") }
        IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Paramètres") }
    }
}

@Composable
private fun ObsPreview(controller: StreamController, ui: RtmpUi, store: StudioStore, selectedSource: Source?, modifier: Modifier) {
    val latestSource by rememberUpdatedState(selectedSource)
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                SurfaceView(context).also { view ->
                    var lastX = 0f
                    var lastY = 0f
                    var startDistance = 0f
                    var startSize = 25f
                    var lastAngle = 0f
                    var lastCenterX = 0f
                    var lastCenterY = 0f
                    var moved = false
                    var downAt = 0L
                    var downX = 0f
                    var downY = 0f
                    fun distance(e: MotionEvent): Float {
                        if (e.pointerCount < 2) return 0f
                        val dx = e.getX(1) - e.getX(0)
                        val dy = e.getY(1) - e.getY(0)
                        return kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    }
                    fun angle(e: MotionEvent): Float {
                        if (e.pointerCount < 2) return 0f
                        val dx = e.getX(1) - e.getX(0)
                        val dy = e.getY(1) - e.getY(0)
                        return Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    }
                    fun centerX(e: MotionEvent) = if (e.pointerCount >= 2) (e.getX(0) + e.getX(1)) / 2f else e.x
                    fun centerY(e: MotionEvent) = if (e.pointerCount >= 2) (e.getY(0) + e.getY(1)) / 2f else e.y
                    view.setOnTouchListener { v, event ->
                        val source = latestSource ?: return@setOnTouchListener false
                        val id = source.id
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                lastX = event.x; lastY = event.y
                                downX = event.x; downY = event.y
                                downAt = System.currentTimeMillis(); moved = false
                                true
                            }
                            MotionEvent.ACTION_POINTER_DOWN -> {
                                if (event.pointerCount >= 2) {
                                    startDistance = distance(event).coerceAtLeast(1f)
                                    startSize = source.size
                                    lastAngle = angle(event)
                                    lastCenterX = centerX(event); lastCenterY = centerY(event)
                                }
                                true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                if (event.pointerCount >= 2 && startDistance > 0f) {
                                    val scale = (distance(event) / startDistance).coerceIn(0.25f, 4f)
                                    store.resizeSource(id, startSize * scale)
                                    var delta = angle(event) - lastAngle
                                    if (delta > 180f) delta -= 360f
                                    if (delta < -180f) delta += 360f
                                    if (kotlin.math.abs(delta) > 0.05f) store.rotateSource(id, delta)
                                    lastAngle = angle(event)
                                    val cx = centerX(event); val cy = centerY(event)
                                    if (v.width > 0 && v.height > 0) store.moveSource(id, (cx-lastCenterX)/v.width*100f, (cy-lastCenterY)/v.height*100f)
                                    lastCenterX = cx; lastCenterY = cy; moved = true
                                } else {
                                    val dx = event.x-lastX; val dy = event.y-lastY
                                    if (v.width > 0 && v.height > 0 && (kotlin.math.abs(dx)>0.5f || kotlin.math.abs(dy)>0.5f)) {
                                        store.moveSource(id, dx/v.width*100f, dy/v.height*100f); moved = true
                                    }
                                    lastX=event.x; lastY=event.y
                                }
                                true
                            }
                            MotionEvent.ACTION_UP -> {
                                val held = System.currentTimeMillis()-downAt
                                val movedDistance = kotlin.math.hypot((event.x-downX).toDouble(), (event.y-downY).toDouble())
                                if (!moved && held < 280 && movedDistance < 24) store.resetTransform(id)
                                startDistance=0f; true
                            }
                            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> { startDistance=0f; true }
                            else -> false
                        }
                    }
                    view.holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) { if (holder.surface.isValid) controller.attachPreview(view) }
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { if (holder.surface.isValid) controller.onPreviewSize(width, height) }
                        override fun surfaceDestroyed(holder: SurfaceHolder) { controller.detachPreview() }
                    })
                }
            }
        )
        Box(
            Modifier.fillMaxSize().drawWithContent {
                drawContent()
                val src = latestSource ?: return@drawWithContent
                val left = size.width*(src.x/100f).coerceIn(0f,1f)
                val top = size.height*(src.y/100f).coerceIn(0f,1f)
                val width = size.width*(src.size/100f).coerceIn(0.05f,1f)
                val height = width*0.62f
                val right=(left+width).coerceAtMost(size.width); val bottom=(top+height).coerceAtMost(size.height)
                drawRect(Color(0xFF5B7CFF), androidx.compose.ui.geometry.Offset(left,top), androidx.compose.ui.geometry.Size((right-left).coerceAtLeast(8f),(bottom-top).coerceAtLeast(8f)), style=androidx.compose.ui.graphics.drawscope.Stroke(3f))
                listOf(androidx.compose.ui.geometry.Offset(left,top),androidx.compose.ui.geometry.Offset(right,top),androidx.compose.ui.geometry.Offset(left,bottom),androidx.compose.ui.geometry.Offset(right,bottom)).forEach { drawCircle(Color.White,7f,it) }
            }
        )
        Surface(Modifier.align(Alignment.TopStart).padding(9.dp), color=Color(0xD9000000), shape=RoundedCornerShape(6.dp)) {
            Text(if (ui.streaming) "● LIVE" else "APERÇU", color=if (ui.streaming) Color(0xFFFF3F5E) else Color.White, fontSize=10.sp, fontWeight=FontWeight.Bold, modifier=Modifier.padding(horizontal=8.dp,vertical=5.dp))
        }
        Surface(Modifier.align(Alignment.TopEnd).padding(9.dp), color=Color(0xCC11151C), shape=RoundedCornerShape(8.dp)) {
            Text(latestSource?.let { "x \\${it.x.toInt()}  y \\${it.y.toInt()}  • \\${it.size.toInt()}%  • \\${it.rotation.toInt()}°" } ?: "Aucun élément", color=Color.White, fontSize=8.sp, modifier=Modifier.padding(horizontal=8.dp,vertical=5.dp))
        }
        Surface(Modifier.align(Alignment.BottomStart).padding(9.dp), color=Color(0xCC000000), shape=RoundedCornerShape(6.dp)) {
            Text("1 doigt : déplacer  •  2 doigts : taille + rotation + déplacement  •  tap : réinitialiser", color=Color.White, fontSize=8.sp, modifier=Modifier.padding(horizontal=7.dp,vertical=4.dp))
        }
        if (ui.connecting) Surface(Modifier.align(Alignment.Center), color=Color(0xEE11151C), shape=RoundedCornerShape(10.dp)) { Text("CONNEXION…", color=Color(0xFFFFC857), fontWeight=FontWeight.Bold, modifier=Modifier.padding(14.dp)) }
    }
}
@Composable
private fun ObsRail(
    modifier: Modifier, activeTab: String, onTab: (String) -> Unit, ui: RtmpUi,
    controller: StreamController, server: String, onServer: (String) -> Unit,
    key: String, onKey: (String) -> Unit, selectedSource: String, onSource: (String) -> Unit,
    store: StudioStore, onPickImage: () -> Unit, micVolume: Float, onMicVolume: (Float) -> Unit, systemVolume: Float, onSystemVolume: (Float) -> Unit,
    transition: String, onTransition: (String) -> Unit, transitionMs: Float, onTransitionMs: (Float) -> Unit,
    onSourceManager: () -> Unit, onSceneManager: () -> Unit
) {
    Column(modifier.background(Color(0xFF10141B)).padding(9.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("Studio", "Sources", "Mixeur", "Transitions", "Diffusion", "Outils").forEach { tab ->
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
                ObsSources(selectedSource, onSource, controller, store, onPickImage, onSourceManager)
                Spacer(Modifier.height(8.dp))
                ObsMixer(ui, controller, micVolume, onMicVolume, systemVolume, onSystemVolume)
                Spacer(Modifier.height(8.dp))
                ObsTransitions(transition, onTransition, transitionMs, onTransitionMs)
                Spacer(Modifier.height(8.dp))
                ObsControls(ui, controller, server, key)
            }
            "Sources" -> ObsSources(selectedSource, onSource, controller, store, onPickImage)
            "Mixeur" -> ObsMixer(ui, controller, micVolume, onMicVolume, systemVolume, onSystemVolume)
            "Transitions" -> ObsTransitions(transition, onTransition, transitionMs, onTransitionMs)
            "Diffusion" -> ObsBroadcast(ui, controller, server, onServer, key, onKey)
            "Outils" -> ObsTools(ui, controller, onSceneManager = { showSceneManager = true })
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
private fun ObsSources(selected: String, onSource: (String) -> Unit, controller: StreamController, store: StudioStore, onPickImage: () -> Unit, onSourceManager: () -> Unit) {
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
                            "Image / logo" -> onPickImage()
                            "Texte" -> store.addText("Texte")
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
        OutlinedButton(onClick = onSourceManager, enabled = selected.isNotBlank(), modifier = Modifier.fillMaxWidth().height(38.dp)) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text("PROPRIÉTÉS DE LA SOURCE", fontSize = 10.sp)
        }
    }
}

@Composable
private fun ObsMixer(ui: RtmpUi, controller: StreamController, mic: Float, onMic: (Float) -> Unit, system: Float, onSystem: (Float) -> Unit) {
    ObsPanel("MIXEUR AUDIO", Icons.Default.VolumeUp) {
        Text("SOURCE AUDIO", color = Color(0xFF8F98A8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            FilterChip(selected = ui.audioSource == "Microphone", onClick = { controller.useMicrophoneAudio() }, label = { Text("MIC", fontSize = 9.sp) })
            FilterChip(selected = ui.audioSource == "Audio système", onClick = { controller.useInternalAudio() }, label = { Text("SYSTÈME", fontSize = 9.sp) })
            FilterChip(selected = ui.audioSource == "Micro + système", onClick = { controller.useMixedAudio() }, label = { Text("MIX", fontSize = 9.sp) })
        }
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
private fun ObsSettingsDialog(ui: RtmpUi, controller: StreamController, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PARAMÈTRES DU STUDIO", fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("CANVAS", color = Color(0xFF7C8CFF), fontSize = 10.sp, fontWeight = FontWeight.Black)
                SettingValue("Format", "16:9")
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    FilterChip(selected = !ui.landscape, onClick = { controller.setOrientation(false) }, label = { Text("PORTRAIT", fontSize = 9.sp) })
                    FilterChip(selected = ui.landscape, onClick = { controller.setOrientation(true) }, label = { Text("PAYSAGE", fontSize = 9.sp) })
                }
                SettingValue("FPS", "30")
                Text("SORTIE", color = Color(0xFF7C8CFF), fontSize = 10.sp, fontWeight = FontWeight.Black)
                SettingValue("Résolution", ui.quality.label)
                SettingValue("Débit vidéo", (ui.quality.bitrate / 1000).toString() + " Kbit/s")
                SettingValue("Encodeur", "H.264")
                Text("AUDIO", color = Color(0xFF7C8CFF), fontSize = 10.sp, fontWeight = FontWeight.Black)
                SettingValue("Fréquence", "48 kHz")
                SettingValue("Canaux", "Stéréo")
                Text("INTERFACE", color = Color(0xFF7C8CFF), fontSize = 10.sp, fontWeight = FontWeight.Black)
                SettingValue("Source vidéo", ui.videoSource)
                SettingValue("Source audio", ui.audioSource)
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


@Composable
private fun ObsTools(ui: RtmpUi, controller: StreamController, onSceneManager: () -> Unit) {
    ObsPanel("OUTILS", Icons.Default.Build) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = controller::resync, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Sync, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("SYNC", fontSize = 9.sp)
            }
            OutlinedButton(onClick = onSceneManager, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Movie, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("SCÈNES", fontSize = 9.sp)
            }
        }
        SettingValue("Vidéo", ui.videoSource)
        SettingValue("Audio", ui.audioSource)
        SettingValue("Bitrate", ui.bitrateKbps.toString() + " kbit/s")
        SettingValue("Enregistrement", if (ui.recording) "ACTIF" else "ARRÊTÉ")
        ui.lastRecordPath?.let { Text(it, color = Color(0xFF8F98A8), fontSize = 8.sp) }
    }
}

@Composable
private fun SceneManagerDialog(
    scenes: List<com.livebridge.studio.Scene>,
    currentId: String,
    store: StudioStore,
    onDismiss: () -> Unit
) {
    var editingId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SCÈNES", fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                scenes.forEach { scene ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(scene.name, Modifier.weight(1f), fontSize = 11.sp,
                            fontWeight = if (scene.id == currentId) FontWeight.Bold else FontWeight.Normal)
                        TextButton(onClick = { editingId = scene.id; name = scene.name }) { Text("RENOMMER", fontSize = 8.sp) }
                        TextButton(onClick = { store.duplicateScene(scene.id) }) { Text("COPIER", fontSize = 8.sp) }
                        TextButton(onClick = { store.deleteScene(scene.id) }) { Text("SUPPR.", fontSize = 8.sp) }
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text("Nom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (editingId.isNotBlank()) store.renameScene(editingId, name)
                    else if (name.isNotBlank()) store.addScene(name)
                    editingId = ""
                    name = ""
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (editingId.isBlank()) "AJOUTER" else "ENREGISTRER")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("FERMER") } }
    )
}

@Composable
private fun SourceManagerDialog(
    source: com.livebridge.studio.Source,
    store: StudioStore,
    onDismiss: () -> Unit
) {
    var name by remember(source.id) { mutableStateOf(source.name) }
    var text by remember(source.id) { mutableStateOf(source.text) }
    var size by remember(source.id) { mutableFloatStateOf(source.size) }
    var textSize by remember(source.id) { mutableFloatStateOf(source.textSize.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PROPRIÉTÉS • " + source.name, fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (source.type == com.livebridge.studio.SourceType.TEXT) {
                    OutlinedTextField(text, { text = it }, label = { Text("Texte") }, modifier = Modifier.fillMaxWidth())
                    Text("Taille du texte " + textSize.toInt() + " px", fontSize = 9.sp, color = Color(0xFF8F98A8))
                    Slider(textSize, { textSize = it }, valueRange = 12f..120f)
                }
                Text("Taille " + size.toInt() + "%", fontSize = 9.sp, color = Color(0xFF8F98A8))
                Slider(size, { size = it }, valueRange = 5f..100f)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    OutlinedButton(onClick = { store.moveLayer(source.id, -1) }, modifier = Modifier.weight(1f)) { Text("MONTER", fontSize = 8.sp) }
                    OutlinedButton(onClick = { store.moveLayer(source.id, 1) }, modifier = Modifier.weight(1f)) { Text("DESCENDRE", fontSize = 8.sp) }
                }
                Button(
                    onClick = { store.removeSource(source.id); onDismiss() },
                    enabled = source.type != com.livebridge.studio.SourceType.CAMERA,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3F5E)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("SUPPRIMER") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                store.rename(source.id, name)
                if (source.type == com.livebridge.studio.SourceType.TEXT) {
                    store.updateText(source.id, text, textSize.toInt(), source.color)
                }
                store.resizeSource(source.id, size)
                onDismiss()
            }) { Text("ENREGISTRER") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ANNULER") } }
    )
}

@Composable
private fun LiveDashboardDialog(ui: RtmpUi, controller: StreamController, onDismiss: () -> Unit) {
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(ui.liveSince, ui.streaming) {
        while (ui.streaming && ui.liveSince > 0) {
            elapsed = (System.currentTimeMillis() - ui.liveSince).coerceAtLeast(0L)
            kotlinx.coroutines.delay(1000)
        }
        if (!ui.streaming) elapsed = 0L
    }
    val total = elapsed / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val sec = total % 60
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("TABLEAU DE BORD LIVE", fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                SettingValue("État", if (ui.streaming) "EN DIRECT" else if (ui.connecting) "CONNEXION" else "ARRÊTÉ")
                SettingValue("Durée", String.format("%02d:%02d:%02d", h, m, sec))
                SettingValue("Bitrate", ui.bitrateKbps.toString() + " kbit/s")
                SettingValue("Vidéo", ui.videoSource)
                SettingValue("Audio", ui.audioSource)
                SettingValue("Enregistrement", if (ui.recording) "ACTIF" else "ARRÊTÉ")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = controller::resync, modifier = Modifier.weight(1f)) { Text("SYNC", fontSize = 9.sp) }
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("FERMER", fontSize = 9.sp) }
                }
            }
        },
        confirmButton = {}
    )
}
