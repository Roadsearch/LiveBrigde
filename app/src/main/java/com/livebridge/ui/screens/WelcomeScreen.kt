package com.livebridge.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livebridge.ui.*

@Composable
fun WelcomeScreen(
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Decorative glowing waves background in bottom half
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Subtle curved glow lines
            val path1 = Path().apply {
                moveTo(0f, height * 0.72f)
                cubicTo(
                    width * 0.25f, height * 0.65f,
                    width * 0.65f, height * 0.82f,
                    width, height * 0.70f
                )
            }
            drawPath(
                path = path1,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x2238BDF8),
                        Color(0x886366F1),
                        Color(0x44818CF8)
                    )
                ),
                style = Stroke(width = 6f)
            )

            val path2 = Path().apply {
                moveTo(0f, height * 0.78f)
                cubicTo(
                    width * 0.35f, height * 0.84f,
                    width * 0.70f, height * 0.68f,
                    width, height * 0.75f
                )
            }
            drawPath(
                path = path2,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x334F46E5),
                        Color(0x9938BDF8),
                        Color(0x442563EB)
                    )
                ),
                style = Stroke(width = 4f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(40.dp))

            // Center: LiveBridge Logo & Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glowing triangle play badge
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF2563EB), Color(0xFF8B5CF6), Color(0xFFEC4899))
                            )
                        )
                        .border(
                            2.dp,
                            Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFFC084FC))),
                            RoundedCornerShape(26.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "LiveBridge Logo",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "LiveBridge",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Diffusez. Partagez. Connectez.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(20.dp))

            // 4 Feature Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureCard(
                    icon = Icons.Default.CellTower,
                    title = "En direct",
                    subtitle = "où vous voulez",
                    modifier = Modifier.weight(1f)
                )
                FeatureCard(
                    icon = Icons.Default.HighQuality,
                    title = "Qualité",
                    subtitle = "professionnelle",
                    modifier = Modifier.weight(1f)
                )
                FeatureCard(
                    icon = Icons.Default.Devices,
                    title = "Multi-",
                    subtitle = "plateformes",
                    modifier = Modifier.weight(1f)
                )
                FeatureCard(
                    icon = Icons.Default.Tune,
                    title = "Simple",
                    subtitle = "et puissant",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Big Gradient CTA Button: "Commencer >"
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF3B50FF), Color(0xFF6366F1), Color(0xFF7C3AED))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Commencer",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(108.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F1626))
            .border(1.dp, Color(0xFF1E2A3E), RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 10.dp),
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
                    .background(Color(0xFF19253B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandBlueLight,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp
            )
            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 10.sp
            )
        }
    }
}
