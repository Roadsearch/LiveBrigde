package com.livebridge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.livebridge.ui.*
import com.livebridge.ui.components.LiveBridgeBottomNav

@Composable
fun ElementsScreen(
    onBack: () -> Unit,
    onSelectNav: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var logoEnabled by remember { mutableStateOf(true) }
    var titleEnabled by remember { mutableStateOf(true) }
    var socialsEnabled by remember { mutableStateOf(false) }
    var cameraFrameEnabled by remember { mutableStateOf(true) }
    var customOverlayEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top Bar: < Éléments   +
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
                text = "Éléments",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.weight(1f))

            IconButton(onClick = { /* Add element */ }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ajouter",
                    tint = Color.White
                )
            }
        }

        // Toggles List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ElementToggleRow(
                    name = "Logo",
                    icon = Icons.Default.Image,
                    enabled = logoEnabled,
                    onToggle = { logoEnabled = it }
                )
            }
            item {
                ElementToggleRow(
                    name = "Titre",
                    icon = Icons.Default.TextFields,
                    enabled = titleEnabled,
                    onToggle = { titleEnabled = it }
                )
            }
            item {
                ElementToggleRow(
                    name = "Réseaux sociaux",
                    icon = Icons.Default.Share,
                    enabled = socialsEnabled,
                    onToggle = { socialsEnabled = it }
                )
            }
            item {
                ElementToggleRow(
                    name = "Caméra (frame)",
                    icon = Icons.Default.Videocam,
                    enabled = cameraFrameEnabled,
                    onToggle = { cameraFrameEnabled = it }
                )
            }
            item {
                ElementToggleRow(
                    name = "Overlay personnalisé",
                    icon = Icons.Default.Layers,
                    enabled = customOverlayEnabled,
                    onToggle = { customOverlayEnabled = it }
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
private fun ElementToggleRow(
    name: String,
    icon: ImageVector,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101726))
            .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF19243A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = BrandBlueLight,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
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
