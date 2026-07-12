package com.tassiolima.regavaranda.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF2E7D32)
private val GreenLight = Color(0xFF60AD5E)
private val Amber = Color(0xFFFF8F00)

private val LightColors = lightColorScheme(
    primary = Green,
    secondary = Amber,
    tertiary = GreenLight
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    secondary = Amber,
    tertiary = Green
)

@Composable
fun RegaVarandaTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
