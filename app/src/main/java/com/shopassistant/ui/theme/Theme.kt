package com.shopassistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F3D36),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3EDE8),
    onPrimaryContainer = Color(0xFF14292A),
    secondary = Color(0xFF5B6B66),
    background = Color(0xFFF8F7F4),
    onBackground = Color(0xFF1B1C1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1C1A),
    surfaceVariant = Color(0xFFF0EFEA),
    onSurfaceVariant = Color(0xFF6B6F6C),
    outline = Color(0xFFD3D1CA),
    outlineVariant = Color(0xFFE8E6E0),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CC7B8),
    onPrimary = Color(0xFF0E2A22),
    primaryContainer = Color(0xFF24403A),
    onPrimaryContainer = Color(0xFFD5EAE1),
    secondary = Color(0xFFA9BAB4),
    background = Color(0xFF121413),
    onBackground = Color(0xFFE8EAE8),
    surface = Color(0xFF1A1D1B),
    onSurface = Color(0xFFE8EAE8),
    surfaceVariant = Color(0xFF242826),
    onSurfaceVariant = Color(0xFFA3A8A5),
    outline = Color(0xFF454A47),
    outlineVariant = Color(0xFF2B2F2D),
    error = Color(0xFFF2B8B5)
)

@Composable
fun ShopAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
