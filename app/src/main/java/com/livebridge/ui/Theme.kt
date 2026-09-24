package com.livebridge.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Bg = Color(0xFF07090D)
private val Panel = Color(0xFF10141B)
private val Panel2 = Color(0xFF171C25)
private val Text = Color(0xFFF4F6FA)
private val Muted = Color(0xFF8D96A8)
private val Blue = Color(0xFF7C8CFF)
val LiveRed = Color(0xFFFF3F5E)

@Composable
fun LiveBridgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Blue, onPrimary = Color.White,
            background = Bg, onBackground = Text,
            surface = Panel, onSurface = Text,
            surfaceVariant = Panel2, onSurfaceVariant = Muted,
            error = LiveRed, outline = Color(0xFF303744)
        ),
        content = content
    )
}
