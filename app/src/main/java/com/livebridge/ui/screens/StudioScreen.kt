package com.livebridge.ui.screens

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.livebridge.R
import com.livebridge.gestures.LiveBridgeTransformEngine
import com.livebridge.rtmp.RtmpUi
import com.livebridge.rtmp.StreamController
import com.livebridge.studio.Source
import com.livebridge.studio.StudioState
import com.livebridge.studio.StudioStore
import com.livebridge.ui.*
import com.livebridge.ui.components.LiveBridgeBottomNav
import com.livebridge.ui.components.VuMeterBar

@Composable
fun StudioScreen(
    controller: StreamController,
    store: StudioStore,
    ui: RtmpUi,
    state: StudioState,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSourcesManager: () -> Unit,
    onStartStream: () -> Unit,
    onStopStream: () -> Unit,
    onSelectNav: (String) -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSourceName by remember { mutableStateOf("Caméra") }
    var micGain by remember { mutableFloatStateOf(0.70f) }
    var systemGain by remember { mutableFloatStateOf(0.85f) }
    var cameraGain by remember { mutableFloatStateOf(0.90f) }

    val activeSource = state.current.sources.firstOrNull { it.name == selectedSourceName }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(6.dp))

            Column {
                Text(
                    text = "Studio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (ui.streaming) LiveRed else StatusGreen)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (ui.streaming) "En direct" else "Hors ligne",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            IconButton(onClick = { controller.switchCamera() }) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Basculer caméra",
                    tint = Color.White
                )
            }

            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Paramètres",
                    tint = Color.White
                )
            }
        }

        // Main Scrollable Studio Area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Video Preview Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black)
                    .border(1.dp, Color(0xFF223048), RoundedCornerShape(18.dp))
            ) {
                // Background fallback scenic preview if no hardware stream yet
                Image(
                    painter = painterResource(id = R.drawable.scene_1),
                    contentDescription = "Aperçu studio",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // SurfaceView for RootEncoder camera feed with multi-touch gestures
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            val engine = LiveBridgeTransformEngine(
                                onTransform = { delta ->
                                    val w = width
                                    val h = height
                                    if (w > 0 && h > 0) {
                                        val dx = delta.panX / w * 100f
                                        val dy = delta.panY / h * 100f
                                        val px = delta.pivotX / w * 100f
                                        val py = delta.pivotY / h * 100f
                                        activeSource?.id?.let { srcId ->
                                            store.transformSource(
                                                id = srcId,
                                                dxPercent = dx,
                                                dyPercent = dy,
                                                scaleFactor = delta.scale,
                                                rotationDelta = delta.rotationDegrees,
                                                pivotXPercent = px,
                                                pivotYPercent = py
                                            )
                                        }
                                    }
                                },
                                onTap = {
                                    activeSource?.id?.let { store.resetTransform(it) }
                                },
                                onDoubleTap = {
                                    activeSource?.id?.let { store.resetTransform(it) }
                                },
                                onLongPress = {}
                            )
                            setOnTouchListener(engine)
                            holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    if (holder.surface.isValid) controller.attachPreview(this@apply)
                                }
                                override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
                                    if (holder.surface.isValid) controller.onPreviewSize(w, h)
                                }
                                override fun surfaceDestroyed(holder: SurfaceHolder) {
                                    controller.detachPreview()
                                }
                            })
                        }
                    }
                )

                // Bounding box overlay for selected source
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            val src = activeSource ?: return@drawWithContent
                            val left = size.width * (src.x / 100f).coerceIn(0f, 1f)
                            val top = size.height * (src.y / 100f).coerceIn(0f, 1f)
                            val width = size.width * (src.size / 100f).coerceIn(0.05f, 1f)
                            val height = width * 0.62f
                            val right = (left + width).coerceAtMost(size.width)
                            val bottom = (top + height).coerceAtMost(size.height)
                            drawRect(
                                color = BrandBlue,
                                topLeft = Offset(left, top),
                                size = Size((right - left).coerceAtLeast(8f), (bottom - top).coerceAtLeast(8f)),
                                style = Stroke(width = 3f)
                            )
                            listOf(
                                Offset(left, top),
                                Offset(right, top),
                                Offset(left, bottom),
                                Offset(right, bottom)
                            ).forEach { drawCircle(Color.White, radius = 6f, center = it) }
                        }
                )

                // Top Left Badge: "Aperçu"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x99000000))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Aperçu",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Bottom Right Badge: "1920x1080 • 30 FPS"
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x99000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "1920x1080 • 30 FPS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xCCFFFFFF)
                    )
                }
            }

            // Scene chips row: [Scène 1] [Scène 2] [Scène 3] [+]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.scenes.forEach { scene ->
                    val isSelected = scene.id == state.currentId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) BrandBlue else Color(0xFF131A2B))
                            .border(1.dp, if (isSelected) BrandBlue else Color(0xFF1E2B42), RoundedCornerShape(12.dp))
                            .clickable { store.selectScene(scene.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = scene.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                // Add Scene Button "+"
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF131A2B))
                        .border(1.dp, Color(0xFF1E2B42), RoundedCornerShape(12.dp))
                        .clickable { store.addScene("Scène ${state.scenes.size + 1}") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ajouter scène",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Two-Column Section: Sources (Left) and Mixeur audio (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Column: Sources
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF101726))
                        .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Sources",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val sourcesList = listOf(
                        Triple("Caméra", Icons.Default.Videocam, true),
                        Triple("Écran", Icons.Default.ScreenShare, false),
                        Triple("Image", Icons.Default.Image, true),
                        Triple("Texte", Icons.Default.TextFields, true),
                        Triple("Capture audio", Icons.Default.Mic, true),
                        Triple("Capture vidéo", Icons.Default.Movie, false)
                    )

                    sourcesList.forEach { (name, icon, isVisible) ->
                        var visibleState by remember { mutableStateOf(isVisible) }
                        val isSelected = selectedSourceName == name

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF1E293B) else Color.Transparent)
                                .clickable {
                                    selectedSourceName = name
                                    when (name) {
                                        "Caméra" -> controller.useCameraSource()
                                        "Écran" -> controller.requestScreenCapture()
                                        "Image" -> onPickImage()
                                        "Texte" -> store.addText("Texte")
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = name,
                                tint = if (isSelected) BrandBlueLight else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { visibleState = !visibleState },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (visibleState) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Visibilité",
                                    tint = if (visibleState) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Réorganiser",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Right Column: Mixeur audio
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF101726))
                        .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Mixeur audio",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Micro slider & VU meter
                    AudioChannelControl(
                        name = "Micro",
                        dbText = "-12.0 dB",
                        icon = Icons.Default.Mic,
                        gain = micGain,
                        onGainChange = { micGain = it },
                        meterLevel = if (ui.micMuted) 0f else micGain * 0.75f
                    )

                    // Système slider & VU meter
                    AudioChannelControl(
                        name = "Système",
                        dbText = "-6.0 dB",
                        icon = Icons.Default.VolumeUp,
                        gain = systemGain,
                        onGainChange = { systemGain = it },
                        meterLevel = systemGain * 0.85f
                    )

                    // Caméra slider & VU meter
                    AudioChannelControl(
                        name = "Caméra",
                        dbText = "-3.0 dB",
                        icon = Icons.Default.Videocam,
                        gain = cameraGain,
                        onGainChange = { cameraGain = it },
                        meterLevel = cameraGain * 0.90f
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
        }

        // Floating Action Bar at Bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Mute Mic Circular Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF162035))
                    .border(1.dp, Color(0xFF223048), CircleShape)
                    .clickable { controller.setMicMuted(!ui.micMuted) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (ui.micMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Microphone",
                    tint = if (ui.micMuted) LiveRed else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Big Coral / Red Pill Button: "Commencer le direct"
            Button(
                onClick = {
                    if (ui.streaming) onStopStream() else onStartStream()
                },
                modifier = Modifier
                    .height(48.dp)
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (ui.streaming) Color(0xFFDC2626) else Color(0xFFFF3F56)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (ui.streaming) "Arrêter le direct" else "Commencer le direct",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Settings Circular Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF162035))
                    .border(1.dp, Color(0xFF223048), CircleShape)
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Paramètres",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Bottom Navigation Bar
        LiveBridgeBottomNav(
            currentSection = "Studio",
            onSelectSection = onSelectNav
        )
    }
}

@Composable
private fun AudioChannelControl(
    name: String,
    dbText: String,
    icon: ImageVector,
    gain: Float,
    onGainChange: (Float) -> Unit,
    meterLevel: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = dbText,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
        }

        // VU meter bar
        VuMeterBar(levelNormalized = meterLevel, height = 5f)

        // Volume Slider
        Slider(
            value = gain,
            onValueChange = onGainChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = BrandBlueLight,
                inactiveTrackColor = Color(0xFF1E293B)
            )
        )
    }
}
