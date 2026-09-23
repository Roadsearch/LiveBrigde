package com.livebridge.ui

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.viewinterop.AndroidView
import com.livebridge.Prefs
import com.livebridge.rtmp.*
import com.livebridge.studio.StudioStore

@Composable
fun StudioApp(controller: StreamController, store: StudioStore, prefs: Prefs, ui: RtmpUi) {
    var server by remember { mutableStateOf(prefs.get("server", "rtmp://")) }
    var key by remember { mutableStateOf(prefs.get("key", "")) }
    val state by store.state.collectAsState()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("LIVEBRIDGE", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Text(if (ui.streaming) "● EN DIRECT" else "● PRÊT",
                    color = if (ui.streaming) LiveRed else Color(0xFF42D98A),
                    style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { controller.switchCamera() }) { Icon(Icons.Default.Cameraswitch, null) }
            IconButton(onClick = { controller.setMicMuted(!ui.micMuted) }) {
                Icon(if (ui.micMuted) Icons.Default.MicOff else Icons.Default.Mic, null)
            }
        }
        Box(Modifier.padding(horizontal = 10.dp).fillMaxWidth().weight(1f)
            .clip(RoundedCornerShape(18.dp)).background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    SurfaceView(context).also { view ->
                        view.holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                if (holder.surface.isValid) {
                                    controller.attachPreview(view)
                                }
                            }

                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int
                            ) {
                                if (holder.surface.isValid) {
                                    controller.onPreviewSize(width, height)
                                }
                            }

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                controller.detachPreview()
                            }
                        })
                    }
                }
            )
            Surface(modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                color = Color(0xAA000000), shape = RoundedCornerShape(7.dp)) {
                Text(if (ui.streaming) "● LIVE" else "APERÇU",
                    color = if (ui.streaming) LiveRed else Color.White,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SCÈNES", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { store.addScene("Scène ${state.scenes.size + 1}") }) { Text("+ Ajouter") }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                state.scenes.forEach { s ->
                    FilterChip(selected = s.id == state.currentId, onClick = { store.selectScene(s.id) }, label = { Text(s.name) })
                }
            }
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SmallAction("REC", ui.recording) { controller.toggleRecord() }
                SmallAction("Sync", false) { if (ui.streaming) controller.resync() }
            }
        }
        if (ui.streaming || ui.connecting) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (ui.connecting) "Connexion au serveur…" else "Diffusion active",
                    modifier = Modifier.weight(1f),
                    color = if (ui.connecting) Color(0xFFFFC857) else LiveRed)
                TextButton(onClick = { controller.stop() }) { Text("ARRÊTER", color = LiveRed) }
            }
        } else {
            Column(Modifier.padding(12.dp).clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DIFFUSION", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = server, onValueChange = { server = it; prefs.put("server", it) },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Serveur RTMP / RTMPS") }, singleLine = true)
                OutlinedTextField(value = key, onValueChange = { key = it; prefs.put("key", it) },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Clé de stream") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation())
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Quality.values().forEach { q ->
                        FilterChip(selected = ui.quality == q, onClick = { controller.setQuality(q) }, label = { Text(q.label) })
                    }
                }
                Button(
                    onClick = { controller.start(server.trimEnd('/') + "/" + key.trim()) },
                    enabled = server.startsWith("rtmp") && key.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Icon(Icons.Default.Videocam, null); Spacer(Modifier.width(7.dp)); Text("COMMENCER LE DIRECT", fontWeight = FontWeight.Bold) }
            }
        }
        ui.message?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp))
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
