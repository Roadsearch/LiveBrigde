package com.livebridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livebridge.ui.*

@Composable
fun LiveBridgeBottomNav(
    currentSection: String,
    onSelectSection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Normal 4 tabs: Accueil, Studio, Scènes, Profil
    val navItems = listOf(
        Triple("Accueil", Icons.Default.Home, "Accueil"),
        Triple("Studio", Icons.Default.Videocam, "Studio"),
        Triple("Scènes", Icons.Default.Layers, "Scènes"),
        Triple("Profil", Icons.Default.Person, "Profil")
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF090D16),
        shadowElevation = 16.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF141C2B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { (label, icon, destination) ->
                val selected = currentSection == destination
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectSection(destination) }
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) BrandBlueLight else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) BrandBlueLight else TextMuted
                    )
                }
            }
        }
    }
}

/**
 * Vu Meter bar with multi-colored segment (green -> yellow -> red)
 */
@Composable
fun VuMeterBar(
    levelNormalized: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    height: Float = 6f
) {
    Box(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF1A2333))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(levelNormalized.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF10B981), // Green
                            Color(0xFF10B981),
                            Color(0xFFFBBF24), // Yellow
                            Color(0xFFEF4444)  // Red
                        )
                    )
                )
        )
    }
}

/**
 * Common Card container with sleek dark border
 */
@Composable
fun StudioCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = SurfaceCard,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val baseModifier = modifier
        .clip(shape)
        .background(backgroundColor)
        .border(1.dp, BorderColor, shape)

    val finalModifier = if (onClick != null) {
        baseModifier.clickable(onClick = onClick)
    } else baseModifier

    Box(
        modifier = finalModifier.padding(14.dp),
        content = content
    )
}
