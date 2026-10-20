package com.beatcontrol.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonPink = Color(0xFFFF007F)
val NeonCyan = Color(0xFF00F0FF)
val NeonPurple = Color(0xFF9D00FF)
val DarkBackground = Color(0xFF0A0A12)
val SurfaceDark = Color(0xFF141423)

private val NeonColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonPink,
    tertiary = NeonPurple,
    background = DarkBackground,
    surface = SurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun BeatControlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeonColorScheme,
        content = content
    )
}
