package com.livebridge.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF080C14)
val SurfaceCard = Color(0xFF101726)
val SurfaceCardLight = Color(0xFF162035)
val SurfaceCardActive = Color(0xFF1A2640)
val BorderColor = Color(0xFF1F2D47)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

val BrandBlue = Color(0xFF2563EB)
val BrandBlueLight = Color(0xFF38BDF8)
val BrandPurple = Color(0xFF7C3AED)
val BrandIndigo = Color(0xFF4F46E5)

val LiveRed = Color(0xFFFF3B56)
val StatusGreen = Color(0xFF22C55E)
val GoldCrown = Color(0xFFFBBF24)

@Composable
fun LiveBridgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = BrandBlue,
            onPrimary = Color.White,
            primaryContainer = BrandIndigo,
            onPrimaryContainer = Color.White,
            background = BgDark,
            onBackground = TextPrimary,
            surface = SurfaceCard,
            onSurface = TextPrimary,
            surfaceVariant = SurfaceCardLight,
            onSurfaceVariant = TextSecondary,
            error = LiveRed,
            outline = BorderColor
        ),
        content = content
    )
}

