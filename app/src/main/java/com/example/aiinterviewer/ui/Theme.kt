package com.example.aiinterviewer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Indigo = Color(0xFF4F46E5)
val Teal = Color(0xFF0D9488)
val Good = Color(0xFF16A34A)
val Warn = Color(0xFFF59E0B)
val Bad = Color(0xFFDC2626)

private val LightColors = lightColorScheme(
    primary = Indigo, onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF), onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Teal, secondaryContainer = Color(0xFFCCFBF1),
    background = Color(0xFFF5F7FB), surface = Color.White,
    surfaceVariant = Color(0xFFE8ECF8), onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF818CF8), onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81), onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF2DD4BF), secondaryContainer = Color(0xFF134E4A),
    background = Color(0xFF0B1020), surface = Color(0xFF151B2E),
    surfaceVariant = Color(0xFF232B47), onSurface = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF94A3B8)
)

fun scoreColor(score: Int): Color = when {
    score >= 7 -> Good
    score >= 4 -> Warn
    else -> Bad
}

@Composable
fun AIInterviewerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}
