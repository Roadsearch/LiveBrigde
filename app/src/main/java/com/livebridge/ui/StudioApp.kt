package com.livebridge.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livebridge.Prefs
import com.livebridge.rtmp.RtmpUi
import com.livebridge.rtmp.StreamController
import com.livebridge.studio.StudioStore
import com.livebridge.ui.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioApp(
    controller: StreamController,
    store: StudioStore,
    prefs: Prefs,
    ui: RtmpUi,
    onPickImage: () -> Unit
) {
    var hasSeenWelcome by remember { mutableStateOf(prefs.get("has_seen_welcome", "false") == "true") }
    var currentSection by remember { mutableStateOf(if (!hasSeenWelcome) "Bienvenue" else "Accueil") }
    var selectedPlatformForSetup by remember { mutableStateOf("YouTube") }
    var showDrawer by remember { mutableStateOf(false) }
    var showVipDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    val studioState by store.state.collectAsState()

    // Automatically transition to Live screen if streaming starts
    LaunchedEffect(ui.streaming) {
        if (ui.streaming && currentSection != "LiveEnCours") {
            currentSection = "LiveEnCours"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        when (currentSection) {
            "Bienvenue" -> {
                WelcomeScreen(
                    onStart = {
                        hasSeenWelcome = true
                        prefs.put("has_seen_welcome", "true")
                        currentSection = "Accueil"
                    }
                )
            }

            "Accueil" -> {
                HomeScreen(
                    onOpenDrawer = { showDrawer = true },
                    onOpenStudio = { currentSection = "Studio" },
                    onOpenPlatformSetup = { platform ->
                        selectedPlatformForSetup = platform
                        currentSection = "Diffusion"
                    },
                    onOpenProject = { title, platform ->
                        selectedPlatformForSetup = platform
                        currentSection = "Diffusion"
                    },
                    onSelectNav = { dest -> currentSection = dest },
                    onOpenVip = { showVipDialog = true }
                )
            }

            "Studio" -> {
                StudioScreen(
                    controller = controller,
                    store = store,
                    ui = ui,
                    state = studioState,
                    onOpenDrawer = { showDrawer = true },
                    onOpenSettings = { currentSection = "Diffusion" },
                    onOpenSourcesManager = { currentSection = "Sources" },
                    onStartStream = {
                        val server = prefs.get("server", "rtmp://a.rtmp.youtube.com/live2")
                        val key = prefs.get("key", "live_stream_key_77a9")
                        val fullUrl = server.trimEnd('/') + "/" + key.trim()
                        controller.start(fullUrl)
                        currentSection = "LiveEnCours"
                    },
                    onStopStream = {
                        controller.stop()
                    },
                    onSelectNav = { dest -> currentSection = dest },
                    onPickImage = onPickImage
                )
            }

            "Scènes" -> {
                ScenesScreen(
                    scenes = studioState.scenes,
                    currentSceneId = studioState.currentId,
                    store = store,
                    onBack = { currentSection = "Studio" },
                    onSelectNav = { dest -> currentSection = dest }
                )
            }

            "Sources" -> {
                SourcesScreen(
                    controller = controller,
                    store = store,
                    onBack = { currentSection = "Studio" },
                    onPickImage = onPickImage,
                    onSelectNav = { dest -> currentSection = dest }
                )
            }

            "Diffusion" -> {
                BroadcastSettingsScreen(
                    controller = controller,
                    ui = ui,
                    prefs = prefs,
                    initialPlatform = selectedPlatformForSetup,
                    onBack = { currentSection = "Accueil" }
                )
            }

            "Éléments" -> {
                ElementsScreen(
                    onBack = { currentSection = "Studio" },
                    onSelectNav = { dest -> currentSection = dest }
                )
            }

            "Audio" -> {
                AudioScreen(
                    controller = controller,
                    ui = ui,
                    onBack = { currentSection = "Studio" },
                    onSelectNav = { dest -> currentSection = dest }
                )
            }

            "Outils" -> {
                ToolsScreen(
                    controller = controller,
                    ui = ui,
                    onOpenDrawer = { showDrawer = true },
                    onOpenStudio = { currentSection = "Studio" },
                    onOpenSettings = { currentSection = "Diffusion" },
                    onOpenStats = { showStatsDialog = true },
                    onSelectNav = { dest -> currentSection = dest }
                )
            }

            "LiveEnCours" -> {
                LiveActiveScreen(
                    controller = controller,
                    ui = ui,
                    onBack = { currentSection = "Studio" },
                    onStopStream = {
                        controller.stop()
                        currentSection = "Studio"
                    },
                    onSelectNav = { dest -> currentSection = dest }
                )
            }

            "Profil" -> {
                ProfileScreen(
                    onOpenSettings = { currentSection = "Diffusion" },
                    onSelectNav = { dest -> currentSection = dest }
                )
            }
        }

        // Navigation Drawer / Quick Menu (Bottom Sheet)
        if (showDrawer) {
            ModalBottomSheet(
                onDismissRequest = { showDrawer = false },
                containerColor = SurfaceCard,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF334155))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "LiveBridge Menu",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(Modifier.height(4.dp))

                    DrawerMenuRow("Accueil", Icons.Default.Home) {
                        currentSection = "Accueil"; showDrawer = false
                    }
                    DrawerMenuRow("Studio", Icons.Default.Videocam) {
                        currentSection = "Studio"; showDrawer = false
                    }
                    DrawerMenuRow("Scènes", Icons.Default.Layers) {
                        currentSection = "Scènes"; showDrawer = false
                    }
                    DrawerMenuRow("Sources", Icons.Default.Collections) {
                        currentSection = "Sources"; showDrawer = false
                    }
                    DrawerMenuRow("Paramètres de diffusion", Icons.Default.Radio) {
                        currentSection = "Diffusion"; showDrawer = false
                    }
                    DrawerMenuRow("Éléments (Overlays)", Icons.Default.ViewQuilt) {
                        currentSection = "Éléments"; showDrawer = false
                    }
                    DrawerMenuRow("Audio", Icons.Default.VolumeUp) {
                        currentSection = "Audio"; showDrawer = false
                    }
                    DrawerMenuRow("Outils", Icons.Default.Build) {
                        currentSection = "Outils"; showDrawer = false
                    }
                    DrawerMenuRow("Live en cours", Icons.Default.PlayCircleFilled) {
                        currentSection = "LiveEnCours"; showDrawer = false
                    }
                    DrawerMenuRow("Profil", Icons.Default.Person) {
                        currentSection = "Profil"; showDrawer = false
                    }
                    DrawerMenuRow("Écran de bienvenue", Icons.Default.AutoAwesome) {
                        currentSection = "Bienvenue"; showDrawer = false
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // VIP / Crown Dialog
        if (showVipDialog) {
            AlertDialog(
                onDismissRequest = { showVipDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = GoldCrown,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text("LiveBridge Pro VIP", fontWeight = FontWeight.Bold, color = Color.White)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Votre formule Pro inclut :",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandBlueLight
                        )
                        Text("• Streaming multi-plateforme en simultané 1080p 60fps", fontSize = 12.sp, color = TextPrimary)
                        Text("• Enregistrement local illimité en H.264 / AAC", fontSize = 12.sp, color = TextPrimary)
                        Text("• Overlays, scènes et filtres illimités", fontSize = 12.sp, color = TextPrimary)
                        Text("• Réduction active du bruit & monitoring écho", fontSize = 12.sp, color = TextPrimary)
                    }
                },
                confirmButton = {
                    Button(onClick = { showVipDialog = false }) {
                        Text("Profiter de LiveBridge")
                    }
                },
                containerColor = SurfaceCard
            )
        }

        // Stats Dialog
        if (showStatsDialog) {
            AlertDialog(
                onDismissRequest = { showStatsDialog = false },
                title = { Text("Statistiques en direct", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Débit montant : ${ui.bitrateKbps} kbps", color = TextPrimary)
                        Text("Images par seconde : 30 FPS", color = TextPrimary)
                        Text("Statut du réseau : Excellent", color = StatusGreen)
                        Text("Codec vidéo : H.264 High Profile", color = TextSecondary)
                        Text("Codec audio : AAC LC 128 kbps", color = TextSecondary)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStatsDialog = false }) {
                        Text("Fermer")
                    }
                },
                containerColor = SurfaceCard
            )
        }
    }
}

@Composable
private fun DrawerMenuRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = BrandBlueLight,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
