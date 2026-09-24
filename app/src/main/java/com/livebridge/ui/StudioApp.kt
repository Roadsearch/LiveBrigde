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
import com.livebridge.gestures.LiveBridgeTransformEngine


@Composable
fun StudioApp(controller: StreamController, store: StudioStore, prefs: Prefs, ui: RtmpUi, onPickImage: () -> Unit) {
    var server by remember { mutableStateOf(prefs.get("server", "rtmp://")) }
    var key by remember { mutableStateOf(prefs.get("key", "")) }
    var section by remember { mutableStateOf("Accueil") }
    var studioTab by remember { mutableStateOf("Studio") }
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

    Box(Modifier.fillMaxSize().background(Color(0xFF070B12))) {
        Column(Modifier.fillMaxSize()) {
            when (section) {
                "Accueil" -> MobileHome(ui, { section = "Studio" }, { section = "Scènes" }, { section = "Sources" },
                    { studioTab = "Diffusion"; section = "Studio" })
                "Studio" -> MobileStudio(ui, controller, store, state, selectedSource, { selectedSource = it },
                    studioTab, { studioTab = it }, server, { server = it; prefs.put("server", it) },
                    key, { key = it; prefs.put("key", it) }, micVolume, { micVolume = it },
                    systemVolume, { systemVolume = it }, transition, { transition = it }, transitionMs, { transitionMs = it },
                    onPickImage, { showSettings = true }, { showDashboard = true },
                    { showSceneManager = true }, { showSourceManager = true })
                "Scènes" -> MobileScenes(state.scenes, state.currentId, store) { showSceneManager = true }
                "Sources" -> MobileSources(selectedSource, { selectedSource = it }, controller, store, onPickImage) { showSourceManager = true }
                "Audio" -> MobileAudio(ui, controller, micVolume, { micVolume = it }, systemVolume, { systemVolume = it })
                "Outils" -> MobileTools(ui, controller, { showSceneManager = true }) { section = "Studio" }
                "Profil" -> MobileProfile { showSettings = true }
            }
            Spacer(Modifier.weight(1f))
            MobileBottomNav(section) { section = it }
        }
        if (showSettings) ObsSettingsDialog(ui, controller) { showSettings = false }
        if (showSceneManager) SceneManagerDialog(state.scenes, state.currentId, store) { showSceneManager = false }
        if (showSourceManager) state.current.sources.firstOrNull { it.name == selectedSource }?.let {
            SourceManagerDialog(it, store) { showSourceManager = false }
        }
        if (showDashboard) LiveDashboardDialog(ui, controller) { showDashboard = false }
    }
}

@Composable
private fun MobileStudio(
    ui: RtmpUi, controller: StreamController, store: StudioStore, state: com.livebridge.studio.StudioState,
    selected: String, onSelected: (String) -> Unit, tab: String, onTab: (String) -> Unit,
    server: String, onServer: (String) -> Unit, key: String, onKey: (String) -> Unit,
    mic: Float, onMic: (Float) -> Unit, system: Float, onSystem: (Float) -> Unit,
    transition: String, onTransition: (String) -> Unit, transitionMs: Float, onTransitionMs: (Float) -> Unit,
    onPickImage: () -> Unit, onSettings: () -> Unit, onDashboard: () -> Unit,
    onScenes: () -> Unit, onSourceManager: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ObsTopBar(ui, controller, onSettings, onDashboard)
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Studio", fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text(if (ui.streaming) "● En direct" else "● Hors ligne", fontSize = 9.sp,
                    color = if (ui.streaming) Color(0xFFFF3F5E) else Color(0xFF35D07F))
            }
            SmallAction("SCÈNES", false, onScenes)
            Spacer(Modifier.width(5.dp))
            SmallAction("+ SOURCE", false, onSourceManager)
        }
        ObsPreview(controller, ui, store, state.current.sources.firstOrNull { it.name == selected },
            Modifier.fillMaxWidth().height(235.dp).padding(horizontal = 10.dp))
        Text("Scènes", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(12.dp, 7.dp))
        SceneStrip(state.scenes, state.currentId, store)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("Studio", "Sources", "Mixeur", "Transitions", "Diffusion", "Outils").forEach {
                FilterChip(selected = tab == it, onClick = { onTab(it) }, label = { Text(it, fontSize = 9.sp) })
            }
        }
        Spacer(Modifier.height(8.dp))
        when (tab) {
            "Studio" -> {
                ObsSources(selected, onSelected, controller, store, onPickImage, onSourceManager)
                ObsMixer(ui, controller, mic, onMic, system, onSystem)
                ObsControls(ui, controller, server, key)
            }
            "Sources" -> ObsSources(selected, onSelected, controller, store, onPickImage, onSourceManager)
            "Mixeur" -> ObsMixer(ui, controller, mic, onMic, system, onSystem)
            "Transitions" -> ObsTransitions(transition, onTransition, transitionMs, onTransitionMs)
            "Diffusion" -> ObsBroadcast(ui, controller, server, onServer, key, onKey)
            "Outils" -> ObsTools(ui, controller, onScenes)
        }
    }
}

@Composable
private fun MobileHome(ui: RtmpUi, studio: () -> Unit, scenes: () -> Unit, sources: () -> Unit, broadcast: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFF293B80), shape = RoundedCornerShape(10.dp)) {
                Text("LB", color = Color.White, fontWeight = FontWeight.Black, modifier = Modifier.padding(10.dp))
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text("LiveBridge", fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("Diffusez. Partagez. Connectez.", fontSize = 9.sp, color = Color(0xFF8F98A8))
            }
            IconButton(onClick = studio) { Icon(Icons.Default.VideoSettings, "Studio") }
        }
        Spacer(Modifier.height(18.dp))
        Surface(Modifier.fillMaxWidth().height(145.dp).clickable { studio() },
            color = Color(0xFF3021A0), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Radio, null, tint = Color.White, modifier = Modifier.size(25.dp))
                    Spacer(Modifier.width(8.dp)); Text("Studio", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                Text("Créez, gérez et diffusez en direct comme un pro.", color = Color.White, fontSize = 11.sp)
                Text("OUVRIR LE STUDIO  →", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(15.dp))
        Text("Diffusion rapide", fontWeight = FontWeight.Black, fontSize = 14.sp)
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            HomeQuick("YouTube", Icons.Default.PlayCircle, broadcast)
            HomeQuick("Twitch", Icons.Default.LiveTv, broadcast)
            HomeQuick("Facebook", Icons.Default.Public, broadcast)
            HomeQuick("Autre", Icons.Default.Add, broadcast)
        }
        Spacer(Modifier.height(16.dp))
        Text("Mon studio", fontWeight = FontWeight.Black, fontSize = 14.sp)
        HomeList("Studio actuel", "Caméra • Microphone • qualité vidéo", Icons.Default.VideoCameraFront, studio)
        HomeList("Mes scènes", "Gérer, dupliquer et réorganiser", Icons.Default.Layers, scenes)
        HomeList("Mes sources", "Images, texte, écran, caméra", Icons.Default.Collections, sources)
        Spacer(Modifier.height(8.dp))
        Text(if (ui.streaming) "● LIVE EN COURS" else "● PRÊT À DIFFUSER",
            color = if (ui.streaming) Color(0xFFFF3F5E) else Color(0xFF35D07F), fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun RowScope.HomeQuick(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit) {
    Surface(Modifier.weight(1f).height(74.dp).clickable { action() }, color = Color(0xFF111B2A),
        shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF243149))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Color(0xFF7C8CFF), modifier = Modifier.size(22.dp)); Text(label, fontSize = 9.sp)
        }
    }
}

@Composable
private fun HomeList(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { action() }.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = Color(0xFF151D2A), shape = RoundedCornerShape(9.dp)) { Icon(icon, null, tint = Color(0xFF7C8CFF), modifier = Modifier.padding(9.dp)) }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(subtitle, fontSize = 9.sp, color = Color(0xFF8F98A8)) }
        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF8F98A8))
    }
}

@Composable
private fun MobileScenes(scenes: List<com.livebridge.studio.Scene>, currentId: String, store: StudioStore, manage: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        MobileSectionHeader("Scènes", "Gérez vos scènes", Icons.Default.Layers, manage)
        scenes.forEach { scene ->
            Surface(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { store.selectScene(scene.id) },
                color = if (scene.id == currentId) Color(0xFF142B62) else Color(0xFF111923), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(72.dp, 48.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF222A36)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Landscape, null, tint = Color(0xFF66758C))
                    }
                    Text(scene.name, Modifier.weight(1f).padding(horizontal = 10.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (scene.id == currentId) Text("ACTIVE", color = Color(0xFF5B7CFF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.MoreVert, null, tint = Color(0xFF8F98A8))
                }
            }
        }
        Button(onClick = { store.addScene("Scène " + (scenes.size + 1)) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(5.dp)); Text("NOUVELLE SCÈNE")
        }
    }
}

@Composable
private fun MobileSources(selected: String, onSource: (String) -> Unit, controller: StreamController, store: StudioStore, pickImage: () -> Unit, manage: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        MobileSectionHeader("Sources", "Caméra, écran, image, texte et audio", Icons.Default.Layers, manage)
        ObsSources(selected, onSource, controller, store, pickImage, manage)
        ObsPanel("GESTES TACTILES", Icons.Default.TouchApp) {
            SettingValue("1 doigt", "Déplacer")
            SettingValue("2 doigts", "Zoom + rotation + déplacement")
            SettingValue("Appui long", "Menu / propriétés")
            SettingValue("Double-tap", "Édition du texte")
        }
    }
}

@Composable
private fun MobileAudio(ui: RtmpUi, controller: StreamController, mic: Float, onMic: (Float) -> Unit, system: Float, onSystem: (Float) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        MobileSectionHeader("Audio", "Mixeur et niveaux audio", Icons.Default.Mic, null)
        ObsMixer(ui, controller, mic, onMic, system, onSystem)
        ObsPanel("OPTIONS AUDIO", Icons.Default.Tune) {
            SettingValue("Microphone", if (ui.micMuted) "Muet" else "Actif")
            SettingValue("Source", ui.audioSource); SettingValue("Fréquence", "48 kHz"); SettingValue("Canaux", "Stéréo")
        }
    }
}

@Composable
private fun MobileTools(ui: RtmpUi, controller: StreamController, scenes: () -> Unit, studio: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        MobileSectionHeader("Outils", "Contrôle rapide du studio", Icons.Default.Build, null)
        ToolCard("Mode Studio", "Aperçu + programme", Icons.Default.VideoSettings, studio)
        ToolCard("Enregistrement", if (ui.recording) "Enregistrement actif" else "Enregistrer votre live", Icons.Default.Radio) { controller.toggleRecord() }
        ToolCard("Resynchroniser", "Recaler audio / vidéo", Icons.Default.Sync) { controller.resync() }
        ToolCard("Scènes", "Gérer les scènes", Icons.Default.Layers, scenes)
        ToolCard("Qualité et paramètres", "Orientation, résolution et sortie", Icons.Default.Settings, studio)
    }
}

@Composable
private fun ToolCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { action() }, color = Color(0xFF111923),
        shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF243149))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF7C8CFF), modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(subtitle, fontSize = 9.sp, color = Color(0xFF8F98A8)) }
            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF8F98A8))
        }
    }
}

@Composable
private fun MobileProfile(settings: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        MobileSectionHeader("Profil", "Compte et préférences", Icons.Default.Person, settings)
        Surface(Modifier.fillMaxWidth(), color = Color(0xFF111923), shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(52.dp), color = Color(0xFF3223A0), shape = RoundedCornerShape(26.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("YD", color = Color.White, fontWeight = FontWeight.Black) }
                }
                Column(Modifier.padding(start = 12.dp)) { Text("Profil LiveBridge", fontWeight = FontWeight.Black, fontSize = 14.sp); Text("Compte local", color = Color(0xFF8F98A8), fontSize = 9.sp) }
            }
        }
        ProfileRow("Mon compte", Icons.Default.AccountCircle)
        ProfileRow("Abonnement", Icons.Default.WorkspacePremium)
        ProfileRow("Aide & Support", Icons.Default.HelpOutline)
        ProfileRow("À propos de LiveBridge", Icons.Default.Info)
        ProfileRow("Paramètres", Icons.Default.Settings, settings)
    }
}

@Composable
private fun ProfileRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().clickable { action() }.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFF8F98A8), modifier = Modifier.size(20.dp))
        Text(title, Modifier.weight(1f).padding(horizontal = 12.dp), fontSize = 12.sp)
        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF8F98A8), modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun MobileSectionHeader(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFF7C8CFF), modifier = Modifier.size(23.dp))
        Column(Modifier.weight(1f).padding(horizontal = 9.dp)) { Text(title, fontSize = 21.sp, fontWeight = FontWeight.Black); Text(subtitle, fontSize = 9.sp, color = Color(0xFF8F98A8)) }
        if (action != null) IconButton(onClick = action) { Icon(Icons.Default.Add, "Ajouter") }
    }
}

@Composable
private fun MobileBottomNav(section: String, onSection: (String) -> Unit) {
    val items = listOf("Accueil" to Icons.Default.Home, "Studio" to Icons.Default.VideoSettings, "Scènes" to Icons.Default.Layers, "Outils" to Icons.Default.Build, "Profil" to Icons.Default.Person)
    Surface(color = Color(0xFF0D121A), shadowElevation = 12.dp) {
        Row(Modifier.fillMaxWidth().height(68.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            items.forEach { item ->
                val selected = section == item.first
                Column(Modifier.weight(1f).clickable { onSection(item.first) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(item.second, null, tint = if (selected) Color(0xFF5B7CFF) else Color(0xFF7B8595), modifier = Modifier.size(21.dp))
                    Text(item.first, fontSize = 8.sp, color = if (selected) Color(0xFF5B7CFF) else Color(0xFF7B8595),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
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
                    val engine = LiveBridgeTransformEngine(
                        onTransform = { delta ->
                            val w = view.width
                            val h = view.height
                            if (w > 0 && h > 0) {
                                val dx = delta.panX / w * 100f
                                val dy = delta.panY / h * 100f
                                val px = delta.pivotX / w * 100f
                                val py = delta.pivotY / h * 100f
                                store.transformSource(
                                    id = latestSource?.id ?: return@LiveBridgeTransformEngine,
                                    dxPercent = dx,
                                    dyPercent = dy,
                                    scaleFactor = delta.scale,
                                    rotationDelta = delta.rotationDegrees,
                                    pivotXPercent = px,
                                    pivotYPercent = py
                                )
                            }
                        },
                        onTap = {
                            latestSource?.let { store.resetTransform(it.id) }
                        },
                        onDoubleTap = {
                            latestSource?.let { store.resetTransform(it.id) }
                        },
                        onLongPress = {
                            // Properties remain accessible through the source manager.
                        }
                    )
                    view.setOnTouchListener(engine)
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
            "Sources" -> ObsSources(selectedSource, onSource, controller, store, onPickImage, onSourceManager)
            "Mixeur" -> ObsMixer(ui, controller, micVolume, onMicVolume, systemVolume, onSystemVolume)
            "Transitions" -> ObsTransitions(transition, onTransition, transitionMs, onTransitionMs)
            "Diffusion" -> ObsBroadcast(ui, controller, server, onServer, key, onKey)
            "Outils" -> ObsTools(ui, controller, onSceneManager)
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
