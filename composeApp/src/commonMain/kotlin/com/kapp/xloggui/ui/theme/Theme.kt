package com.kapp.xloggui.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

val PrimaryAccent = Color(0xFF7C4DFF) // Vibrant Indigo
val SecondaryAccent = Color(0xFF00E5FF) // Bright Cyan
val BackgroundDark = Color(0xFF1E1E1E) // Deep Gray
val SurfaceDark = Color(0xFF252526) // Slightly Lighter Gray
val SurfaceVariant = Color(0xFF333333) 
val TextPrimary = Color(0xFFE0E0E0) // Off-white
val TextSecondary = Color(0xFFA0A0A0) // Muted

// Log Level Colors
val LevelVerbose = Color(0xFFA0A0A0) // Grey
val LevelDebug = Color(0xFF4FC3F7) // Light Blue
val LevelInfo = Color(0xFF66BB6A) // Green
val LevelWarn = Color(0xFFFFCA28) // Amber
val LevelError = Color(0xFFEF5350) // Red
val LevelFatal = Color(0xFFD32F2F) // Dark Red

val ErrorRed = Color(0xFFEF5350)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAccent,
    secondary = SecondaryAccent,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariant,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    errorContainer = ErrorRed.copy(alpha = 0.1f)
)

val AppTypography = androidx.compose.material3.Typography(
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
        lineHeight = 20.sp
    ),
    bodySmall = androidx.compose.ui.text.TextStyle(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
        lineHeight = 16.sp,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
    ),
    labelSmall = androidx.compose.ui.text.TextStyle(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
