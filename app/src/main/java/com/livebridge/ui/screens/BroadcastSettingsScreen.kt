package com.livebridge.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livebridge.Prefs
import com.livebridge.rtmp.Quality
import com.livebridge.rtmp.RtmpUi
import com.livebridge.rtmp.StreamController
import com.livebridge.ui.*

@Composable
fun BroadcastSettingsScreen(
    controller: StreamController,
    ui: RtmpUi,
    prefs: Prefs,
    initialPlatform: String = "YouTube",
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var title by remember { mutableStateOf(prefs.get("stream_title", "Live !")) }
    var category by remember { mutableStateOf(prefs.get("stream_category", "Discussion & Voyage")) }
    var selectedPlatform by remember { mutableStateOf(initialPlatform) }
    var modeDiffusion by remember { mutableStateOf(prefs.get("stream_mode", "RTMP")) } // RTMP or RTMPS
    var streamKey by remember { mutableStateOf(prefs.get("key", "live_stream_key_77a9")) }
    var serverUrl by remember { mutableStateOf(prefs.get("server", "rtmp://a.rtmp.youtube.com/live2")) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top Bar: < Paramètres de diffusion
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
                text = "Paramètres de diffusion",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Form Fields
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Titre Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Titre",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "${title.length}/100",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF101726),
                        unfocusedContainerColor = Color(0xFF101726),
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color(0xFF1F2D47),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // Catégorie Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Catégorie",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF101726))
                        .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(14.dp))
                        .clickable {
                            category = if (category == "Discussion & Voyage") "Gaming & Esports" else "Discussion & Voyage"
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = category,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Plateforme Field (YouTube, Twitch, Facebook, Autre)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Plateforme",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlatformTile(
                        name = "YouTube",
                        icon = Icons.Default.PlayArrow,
                        accentColor = Color(0xFFFF0000),
                        isSelected = selectedPlatform == "YouTube",
                        onClick = {
                            selectedPlatform = "YouTube"
                            serverUrl = "rtmp://a.rtmp.youtube.com/live2"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PlatformTile(
                        name = "Twitch",
                        icon = Icons.Default.ChatBubble,
                        accentColor = Color(0xFF9146FF),
                        isSelected = selectedPlatform == "Twitch",
                        onClick = {
                            selectedPlatform = "Twitch"
                            serverUrl = "rtmp://live.twitch.tv/app"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PlatformTile(
                        name = "Facebook",
                        icon = Icons.Default.ThumbUp,
                        accentColor = Color(0xFF1877F2),
                        isSelected = selectedPlatform == "Facebook",
                        onClick = {
                            selectedPlatform = "Facebook"
                            serverUrl = "rtmps://live-api-s.facebook.com:443/rtmp/"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PlatformTile(
                        name = "Autre",
                        icon = Icons.Default.Add,
                        accentColor = Color(0xFF94A3B8),
                        isSelected = selectedPlatform == "Autre",
                        onClick = { selectedPlatform = "Autre" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Mode de diffusion (Tabs: [RTMP] [RTMPS])
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Mode de diffusion",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF101726))
                        .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    listOf("RTMP", "RTMPS").forEach { mode ->
                        val isSelected = modeDiffusion == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) BrandBlue else Color.Transparent)
                                .clickable { modeDiffusion = mode }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Clé de stream Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Clé de stream",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = streamKey,
                    onValueChange = { streamKey = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Afficher la clé",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(streamKey))
                                Toast.makeText(context, "Clé de stream copiée", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copier",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF101726),
                        unfocusedContainerColor = Color(0xFF101726),
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color(0xFF1F2D47),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // Paramètres avancés accordion
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF101726))
                    .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(14.dp))
                    .clickable { showAdvanced = !showAdvanced }
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = BrandBlueLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Paramètres avancés",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (showAdvanced) {
                        Divider(color = Color(0xFF1F2D47))
                        Text("URL du serveur RTMP :", fontSize = 11.sp, color = TextSecondary)
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Qualité de flux :", fontSize = 11.sp, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Quality.values().forEach { q ->
                                FilterChip(
                                    selected = ui.quality == q,
                                    onClick = { controller.setQuality(q) },
                                    label = { Text(q.label, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }

        // Bottom Button: Enregistrer (Big blue pill button)
        Box(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = {
                    prefs.put("stream_title", title)
                    prefs.put("stream_category", category)
                    prefs.put("stream_mode", modeDiffusion)
                    prefs.put("key", streamKey)
                    prefs.put("server", serverUrl)
                    Toast.makeText(context, "Paramètres enregistrés", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                Text(
                    text = "Enregistrer",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PlatformTile(
    name: String,
    icon: ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF101726))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) BrandBlue else Color(0xFF1F2D47),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = name,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = Color.White
            )
        }
    }
}
