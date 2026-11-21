package com.gateprep.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun hex(s: String): Color = Color(android.graphics.Color.parseColor(s))

val BrandA = Color(0xFF5B5BF0)
val BrandB = Color(0xFF8E54E9)

/** Colours the Material scheme has no slot for. */
data class Extra(
    val bg: Color,
    val card: Color,
    val cardAlt: Color,
    val muted: Color,
    val outline: Color,
    val success: Color,
    val successBg: Color,
    val danger: Color,
    val dangerBg: Color,
    val brand: Brush,
    val brandStart: Color,
    val brandEnd: Color,
    val isDark: Boolean,
)

val LocalExtra = staticCompositionLocalOf<Extra> { error("GateTheme missing") }

private val LightExtra = Extra(
    bg = Color(0xFFF4F5FB), card = Color(0xFFFFFFFF), cardAlt = Color(0xFFECEEFA),
    muted = Color(0xFF6B7089), outline = Color(0xFFE2E5F2),
    success = Color(0xFF12B76A), successBg = Color(0xFFE3F7EC),
    danger = Color(0xFFF04438), dangerBg = Color(0xFFFDEAE8),
    brand = Brush.linearGradient(listOf(BrandA, BrandB)), brandStart = BrandA, brandEnd = BrandB, isDark = false,
)

private val DarkExtra = Extra(
    bg = Color(0xFF0D0F1E), card = Color(0xFF171A30), cardAlt = Color(0xFF20254A),
    muted = Color(0xFF9AA0C0), outline = Color(0xFF2A3055),
    success = Color(0xFF32D583), successBg = Color(0xFF123A2B),
    danger = Color(0xFFF97066), dangerBg = Color(0xFF3F1D1F),
    brand = Brush.linearGradient(listOf(Color(0xFF6C6CFF), Color(0xFFA06BFF))), brandStart = Color(0xFF6C6CFF), brandEnd = Color(0xFFA06BFF), isDark = true,
)

private val LightScheme = lightColorScheme(
    primary = BrandA, onPrimary = Color.White, secondary = BrandB,
    background = LightExtra.bg, surface = LightExtra.card, onBackground = Color(0xFF1B1D2A), onSurface = Color(0xFF1B1D2A),
    surfaceVariant = LightExtra.cardAlt, outline = LightExtra.outline,
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF8B8BFF), onPrimary = Color.White, secondary = Color(0xFFB794FF),
    background = DarkExtra.bg, surface = DarkExtra.card, onBackground = Color(0xFFECEEFA), onSurface = Color(0xFFECEEFA),
    surfaceVariant = DarkExtra.cardAlt, outline = DarkExtra.outline,
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
)

@Composable
fun GateTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val extra = if (dark) DarkExtra else LightExtra
    CompositionLocalProvider(LocalExtra provides extra) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
