package com.livebridge.ui.screens

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
import com.livebridge.ui.components.LiveBridgeBottomNav

@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    onOpenStudio: () -> Unit,
    onOpenPlatformSetup: (String) -> Unit,
    onOpenProject: (String, String) -> Unit,
    onSelectNav: (String) -> Unit,
    onOpenVip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                text = "LiveBridge",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF38BDF8)
            )

            Spacer(Modifier.weight(1f))

            IconButton(onClick = onOpenVip) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "VIP Pro",
                    tint = GoldCrown
                )
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Studio Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF4328E0), Color(0xFF2563EB))
                        )
                    )
                    .clickable(onClick = onOpenStudio)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Glowing concentric aperture icon
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .border(1.5.dp, Color(0x66FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RadioButtonChecked,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Studio",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Créez, gérez et diffusez\nen direct comme un pro.",
                            fontSize = 12.sp,
                            color = Color(0xDDFFFFFF),
                            lineHeight = 16.sp
                        )
                    }

                    // Circle Arrow Action Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Ouvrir Studio",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Diffusion rapide section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Diffusion rapide",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickPlatformButton(
                        name = "YouTube",
                        icon = Icons.Default.PlayArrow,
                        accentColor = Color(0xFFFF0000),
                        onClick = { onOpenPlatformSetup("YouTube") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickPlatformButton(
                        name = "Twitch",
                        icon = Icons.Default.ChatBubble,
                        accentColor = Color(0xFF9146FF),
                        onClick = { onOpenPlatformSetup("Twitch") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickPlatformButton(
                        name = "Facebook",
                        icon = Icons.Default.ThumbUp,
                        accentColor = Color(0xFF1877F2),
                        onClick = { onOpenPlatformSetup("Facebook") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickPlatformButton(
                        name = "Autre",
                        icon = Icons.Default.Add,
                        accentColor = Color(0xFF94A3B8),
                        onClick = { onOpenPlatformSetup("Autre") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Mes projets section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Mes projets",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                ProjectRow(
                    title = "Live - Gaming",
                    platform = "YouTube",
                    icon = Icons.Default.PlayArrow,
                    platformColor = Color(0xFFFF0000),
                    onClick = { onOpenProject("Live - Gaming", "YouTube") }
                )

                ProjectRow(
                    title = "Live - Discussion",
                    platform = "Twitch",
                    icon = Icons.Default.ChatBubble,
                    platformColor = Color(0xFF9146FF),
                    onClick = { onOpenProject("Live - Discussion", "Twitch") }
                )

                ProjectRow(
                    title = "Podcast",
                    platform = "Facebook",
                    icon = Icons.Default.ThumbUp,
                    platformColor = Color(0xFF1877F2),
                    onClick = { onOpenProject("Podcast", "Facebook") }
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        // Bottom Navigation Bar
        LiveBridgeBottomNav(
            currentSection = "Accueil",
            onSelectSection = onSelectNav
        )
    }
}

@Composable
private fun QuickPlatformButton(
    name: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101726))
            .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
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
            Spacer(Modifier.height(6.dp))
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ProjectRow(
    title: String,
    platform: String,
    icon: ImageVector,
    platformColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101726))
            .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
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
                    .background(platformColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = platform,
                    tint = platformColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = platform,
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
