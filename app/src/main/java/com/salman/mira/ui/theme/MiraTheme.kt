package com.salman.mira.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Mira design system.
 *
 * Rules (per product direction):
 * - NO emojis anywhere in the UI. Material icons only.
 * - Dark, premium, professional — distinct from Maya's blue.
 * - Lightweight: system font, vector icons, no bundled font files.
 */

// Brand palette — deep slate + mint/teal accent (not Maya blue).
val MiraBackground = Color(0xFF0A0E1A)
val MiraSurface = Color(0xFF121828)
val MiraSurfaceVariant = Color(0xFF1B2440)
val MiraSurfaceHigh = Color(0xFF232D52)
val MiraPrimary = Color(0xFF5EEAD4)      // mint/teal accent
val MiraOnPrimary = Color(0xFF04211C)
val MiraSecondary = Color(0xFF8B9DFF)    // soft indigo for gradients
val MiraText = Color(0xFFF2F5FA)
val MiraTextSecondary = Color(0xFF9AA5BD)
val MiraSuccess = Color(0xFF4ADE80)
val MiraWarning = Color(0xFFFBBF24)
val MiraError = Color(0xFFF87171)

private val MiraColorScheme = darkColorScheme(
    background = MiraBackground,
    onBackground = MiraText,
    surface = MiraSurface,
    onSurface = MiraText,
    surfaceVariant = MiraSurfaceVariant,
    onSurfaceVariant = MiraTextSecondary,
    primary = MiraPrimary,
    onPrimary = MiraOnPrimary,
    secondary = MiraSecondary,
    error = MiraError,
)

private val MiraTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp,
        color = MiraText,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        color = MiraText,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = MiraText,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = MiraText,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = MiraTextSecondary,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = MiraText,
    ),
)

@Composable
fun MiraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MiraColorScheme,
        typography = MiraTypography,
        content = content,
    )
}
