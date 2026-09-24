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
import com.livebridge.rtmp.RtmpUi
import com.livebridge.rtmp.StreamController
import com.livebridge.ui.*
import com.livebridge.ui.components.LiveBridgeBottomNav

@Composable
fun ToolsScreen(
    controller: StreamController,
    ui: RtmpUi,
    onOpenDrawer: () -> Unit,
    onOpenStudio: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onSelectNav: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    data class ToolDef(
        val title: String,
        val subtitle: String,
        val icon: ImageVector,
        val onClick: () -> Unit
    )

    val tools = listOf(
        ToolDef("Mode Studio", "Aperçu + programme", Icons.Default.DashboardCustomize, onOpenStudio),
        ToolDef(
            "Enregistrement",
            if (ui.recording) "Enregistrement en cours..." else "Enregistrer votre live",
            Icons.Default.RadioButtonChecked
        ) { controller.toggleRecord() },
        ToolDef("Replay instantané", "Retour en arrière", Icons.Default.Replay) { controller.resync() },
        ToolDef("Statistiques", "Réseau, FPS, débit", Icons.Default.BarChart, onOpenStats),
        ToolDef("Paramètres", "Général, compte, etc.", Icons.Default.Settings, onOpenSettings)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(4.dp))

            Text(
                text = "Outils",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Tools list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tools) { tool ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF101726))
                        .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(16.dp))
                        .clickable(onClick = tool.onClick)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF19253C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = tool.title,
                                tint = BrandBlueLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tool.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = tool.subtitle,
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
