package com.livebridge.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livebridge.R
import com.livebridge.rtmp.RtmpUi
import com.livebridge.rtmp.StreamController
import com.livebridge.ui.*
import com.livebridge.ui.components.LiveBridgeBottomNav

@Composable
fun LiveActiveScreen(
    controller: StreamController,
    ui: RtmpUi,
    onBack: () -> Unit,
    onStopStream: () -> Unit,
    onSelectNav: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var elapsedSeconds by remember { mutableLongStateOf(754L) } // 00:12:34 base

    LaunchedEffect(ui.streaming) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            elapsedSeconds++
        }
    }

    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timerString = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top Bar: < Live en cours   🔴 00:12:34
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
                text = "Live en cours",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.weight(1f))

            // Red dot + Live timer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF22111A))
                    .border(1.dp, Color(0xFF4A1828), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(LiveRed)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = timerString,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Main content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Video Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black)
                    .border(1.dp, Color(0xFF223048), RoundedCornerShape(18.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.scene_1),
                    contentDescription = "Live stream",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top left "LIVE" + "👁 1.2K" badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LiveRed)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Spectateurs",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "1.2K",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Stats row 1: Vues (1,2K), Likes (342), Qualité (Excellent)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard("Vues", "1,2K", modifier = Modifier.weight(1f))
                MetricCard("Likes", "342", modifier = Modifier.weight(1f))
                StatusQualityCard("Qualité", "Excellent", modifier = Modifier.weight(1.2f))
            }

            // Stats row 2: Réseau (8.4 Mbps), FPS (30 fps), Batterie (87% ⚡)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF101726))
                        .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Réseau", fontSize = 11.sp, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (ui.bitrateKbps > 0) "${ui.bitrateKbps / 1000f} Mbps" else "8.4 Mbps",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(6.dp))
                        // Mini signal bars
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                            Box(Modifier.width(4.dp).height(8.dp).background(StatusGreen, RoundedCornerShape(1.dp)))
                            Box(Modifier.width(4.dp).height(12.dp).background(StatusGreen, RoundedCornerShape(1.dp)))
                            Box(Modifier.width(4.dp).height(16.dp).background(StatusGreen, RoundedCornerShape(1.dp)))
                            Box(Modifier.width(4.dp).height(14.dp).background(StatusGreen, RoundedCornerShape(1.dp)))
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF101726))
                        .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("FPS", fontSize = 11.sp, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text("30", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("fps", fontSize = 9.sp, color = TextSecondary)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF101726))
                        .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Batterie", fontSize = 11.sp, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("87%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.BatteryChargingFull, null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Big Red Button: "⏹ Arrêter le direct"
            Button(
                onClick = onStopStream,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Arrêter le direct",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
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
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101726))
            .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(text = title, fontSize = 11.sp, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun StatusQualityCard(
    title: String,
    statusText: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101726))
            .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(text = title, fontSize = 11.sp, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusGreen)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
