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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livebridge.ui.*
import com.livebridge.ui.components.LiveBridgeBottomNav

@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    onSelectNav: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Top Bar: Profil   ⚙️
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Profil",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Paramètres",
                    tint = Color.White
                )
            }
        }

        // Profile details & options
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF101726))
                    .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Blue Circle Avatar with user icon
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Photo de profil",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Yesaa Dubois",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "@yesaa_dubois",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Menu Items List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF101726))
                    .border(1.dp, Color(0xFF1F2D47), RoundedCornerShape(18.dp))
            ) {
                ProfileOptionItem(
                    title = "Mon compte",
                    icon = Icons.Default.PersonOutline,
                    onClick = onOpenSettings
                )
                Divider(color = Color(0xFF1B263C))
                ProfileOptionItem(
                    title = "Abonnement (Gratuit)",
                    icon = Icons.Default.EmojiEvents,
                    iconTint = GoldCrown,
                    onClick = onOpenSettings
                )
                Divider(color = Color(0xFF1B263C))
                ProfileOptionItem(
                    title = "Aide & Support",
                    icon = Icons.Default.HelpOutline,
                    onClick = {}
                )
                Divider(color = Color(0xFF1B263C))
                ProfileOptionItem(
                    title = "À propos de LiveBridge",
                    icon = Icons.Default.Info,
                    onClick = {}
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        // Bottom Navigation Bar
        LiveBridgeBottomNav(
            currentSection = "Profil",
            onSelectSection = onSelectNav
        )
    }
}

@Composable
private fun ProfileOptionItem(
    title: String,
    icon: ImageVector,
    iconTint: Color = BrandBlueLight,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}
