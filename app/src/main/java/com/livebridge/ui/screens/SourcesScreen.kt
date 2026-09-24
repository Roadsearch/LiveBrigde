package com.livebridge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livebridge.rtmp.StreamController
import com.livebridge.studio.StudioStore
import com.livebridge.ui.*
import com.livebridge.ui.components.LiveBridgeBottomNav

@Composable
fun SourcesScreen(
    controller: StreamController,
    store: StudioStore,
    onBack: () -> Unit,
    onPickImage: () -> Unit,
    onSelectNav: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    data class SourceItemDef(
        val title: String,
        val subtitle: String,
        val icon: ImageVector,
        val onClick: () -> Unit
    )

    val sourceOptions = listOf(
        SourceItemDef("Caméra", "Caméra avant ou arrière", Icons.Default.Videocam) {
            controller.useCameraSource()
            onBack()
        },
        SourceItemDef("Écran", "Partager l'écran / application", Icons.Default.ScreenShare) {
            controller.requestScreenCapture()
            onBack()
        },
        SourceItemDef("Image", "Logo, image, fond", Icons.Default.Image) {
            onPickImage()
            onBack()
        },
        SourceItemDef("Texte", "Titre, sous-titre, info", Icons.Default.TextFields) {
            store.addText("Mon texte")
            onBack()
        },
        SourceItemDef("Navigateur", "Page web / widget", Icons.Default.Language) {
            store.addText("Widget Web")
            onBack()
        },
        SourceItemDef("Capture audio", "Audio système / application", Icons.Default.Mic) {
            controller.useInternalAudio()
            onBack()
        },
        SourceItemDef("Capture vidéo", "Vidéo depuis un fichier", Icons.Default.Movie) {
            onPickImage()
            onBack()
        },
        SourceItemDef("Groupe", "Regrouper plusieurs sources", Icons.Default.Group) {
            store.addText("Groupe")
            onBack()
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top Bar: < Sources   +
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
                text = "Sources",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.weight(1f))

            IconButton(onClick = { onPickImage() }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ajouter",
                    tint = Color.White
                )
            }
        }

        // Sources list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sourceOptions) { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF101726))
                        .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(16.dp))
                        .clickable(onClick = item.onClick)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF182238)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = BrandBlueLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.subtitle,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Bottom Navigation Bar
        LiveBridgeBottomNav(
            currentSection = "Studio",
            onSelectSection = onSelectNav
        )
    }
}
