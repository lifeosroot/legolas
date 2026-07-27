package kr.co.root.legolas.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object RootColors {
    val Background = Color(0xFF0F0F14)
    val Accent = Color(0xFF6366F2)
    val TextPrimary = Color.White.copy(alpha = 0.92f)
    val TextSecondary = Color.White.copy(alpha = 0.60f)
    val TextTertiary = Color.White.copy(alpha = 0.50f)
    val Surface = Color.White.copy(alpha = 0.06f)
    val Stroke = Color.White.copy(alpha = 0.15f)
    val StrokeSubtle = Color.White.copy(alpha = 0.08f)
    val Success = Color(0xFF22C55E)
    val Danger = Color(0xFFEF4444)
}

private val RootColorScheme = darkColorScheme(
    primary = RootColors.Accent,
    onPrimary = Color.White,
    background = RootColors.Background,
    onBackground = RootColors.TextPrimary,
    surface = RootColors.Background,
    onSurface = RootColors.TextPrimary,
    surfaceVariant = RootColors.Surface,
    onSurfaceVariant = RootColors.TextSecondary,
    outline = RootColors.Stroke,
    tertiary = RootColors.Success,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF123D26),
    onTertiaryContainer = Color(0xFF86EFAC),
    error = RootColors.Danger,
    onError = Color.White,
)

private val RootTypography = Typography(
    displaySmall = rootText(FontWeight.SemiBold, 30, 36),
    headlineMedium = rootText(FontWeight.SemiBold, 24, 30),
    titleLarge = rootText(FontWeight.SemiBold, 18, 24),
    titleMedium = rootText(FontWeight.SemiBold, 15, 21),
    titleSmall = rootText(FontWeight.SemiBold, 12, 17),
    bodyLarge = rootText(FontWeight.Normal, 14, 20),
    bodyMedium = rootText(FontWeight.Normal, 12, 18),
    bodySmall = rootText(FontWeight.Normal, 11, 16),
    labelLarge = rootText(FontWeight.SemiBold, 12, 18),
    labelMedium = rootText(FontWeight.Medium, 11, 16),
    labelSmall = rootText(FontWeight.Medium, 10, 14),
)

private val RootShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun RootTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RootColorScheme,
        typography = RootTypography,
        shapes = RootShapes,
        content = content,
    )
}

private fun rootText(weight: FontWeight, size: Int, lineHeight: Int) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
)
