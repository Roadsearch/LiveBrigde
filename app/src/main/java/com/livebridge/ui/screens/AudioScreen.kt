package com.livebridge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livebridge.rtmp.RtmpUi
import com.livebridge.rtmp.StreamController
import com.livebridge.ui.*
import com.livebridge.ui.components.LiveBridgeBottomNav
import com.livebridge.ui.components.VuMeterBar

@Composable
fun AudioScreen(
    controller: StreamController,
    ui: RtmpUi,
    onBack: () -> Unit,
    onSelectNav: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var micVolume by remember { mutableFloatStateOf(0.70f) }
    var systemVolume by remember { mutableFloatStateOf(0.85f) }
    var cameraVolume by remember { mutableFloatStateOf(0.90f) }
    var noiseReduction by remember { mutableStateOf(true) }
    var echoMonitoring by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top Bar: < Audio
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(4.dp))

            Text(
                text = "Audio",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Volume faders
            AudioFaderCard("Micro", "-12.0 dB", Icons.Default.Mic, micVolume, { micVolume = it }, if (ui.micMuted) 0f else micVolume * 0.75f)
            AudioFaderCard("Système", "-6.0 dB", Icons.Default.VolumeUp, systemVolume, { systemVolume = it }, systemVolume * 0.85f)
            AudioFaderCard("Caméra", "-3.0 dB", Icons.Default.Videocam, cameraVolume, { cameraVolume = it }, cameraVolume * 0.90f)

            // Périphérique audio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF101726))
                    .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(16.dp))
                    .clickable { controller.useMicrophoneAudio() }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Périphérique audio",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Par défaut",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Options audio
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Options audio",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )

                // Réduction du bruit
                AudioOptionSwitch(
                    title = "Réduction du bruit",
                    checked = noiseReduction,
                    onCheckedChange = { noiseReduction = it }
                )

                // Écho (monitoring)
                AudioOptionSwitch(
                    title = "Écho (monitoring)",
                    checked = echoMonitoring,
                    onCheckedChange = { echoMonitoring = it }
                )
            }

            Spacer(Modifier.height(12.dp))
        }

        // Bottom Navigation Bar
        LiveBridgeBottomNav(
            currentSection = "Studio",
            onSelectSection = onSelectNav
        )
    }
}

@Composable
private fun AudioFaderCard(
    name: String,
    db: String,
    icon: ImageVector,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    meterLevel: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101726))
            .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = BrandBlueLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = db,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            VuMeterBar(levelNormalized = meterLevel, height = 6f)

            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = BrandBlueLight,
                    inactiveTrackColor = Color(0xFF1E293B)
                )
            )
        }
    }
}

@Composable
private fun AudioOptionSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101726))
            .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BrandBlue,
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0xFF1E293B)
                )
            )
        }
    }
}
