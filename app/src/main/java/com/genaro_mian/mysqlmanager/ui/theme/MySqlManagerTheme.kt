package com.genaro_mian.mysqlmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 🎨 Paleta baseada no ícone azul DB
private val BluePrimary = Color(0xFF2A8CF7)
private val BlueSecondary = Color(0xFF6DB3FF)
private val LightBackground = Color(0xFFF4F6FA)
private val DarkBackground = Color(0xFF0F172A)
private val DarkSurface = Color(0xFF1E293B)
private val ErrorColor = Color(0xFFE57373)

private val LightColors = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    secondary = BlueSecondary,
    onSecondary = Color.White,
    background = LightBackground,
    surface = Color.White,
    onSurface = Color(0xFF1B1B1B),
    onSurfaceVariant = Color(0xFF5F6368),
    error = ErrorColor
)

private val DarkColors = darkColorScheme(
    primary = BlueSecondary,
    onPrimary = Color.Black,
    secondary = BluePrimary,
    onSecondary = Color.Black,
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFF87171)
)

@Composable
fun MySqlManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}

// 🌈 Gradiente usado na TopBar e botões
val BlueGradient = Brush.horizontalGradient(
    listOf(Color(0xFF2A8CF7), Color(0xFF6DB3FF))
)
