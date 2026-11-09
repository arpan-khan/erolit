package dev.erolit.reader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF6F5237),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E2CC),
    onPrimaryContainer = Color(0xFF2A1C0E),
    secondary = Color(0xFF4F6F64),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4E7DF),
    onSecondaryContainer = Color(0xFF0E211B),
    tertiary = Color(0xFF7A4E52),
    onTertiary = Color.White,
    background = Color(0xFFFBF8F3),
    onBackground = Color(0xFF1E1B17),
    surface = Color(0xFFFFFDF9),
    onSurface = Color(0xFF1E1B17),
    surfaceVariant = Color(0xFFEFE6D9),
    onSurfaceVariant = Color(0xFF4F463B),
    outline = Color(0xFF81766A),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE0C29B),
    onPrimary = Color(0xFF3E2B16),
    primaryContainer = Color(0xFF57402A),
    onPrimaryContainer = Color(0xFFF8E7D3),
    secondary = Color(0xFFB7CCC2),
    onSecondary = Color(0xFF22352D),
    secondaryContainer = Color(0xFF384C43),
    onSecondaryContainer = Color(0xFFD3E8DE),
    tertiary = Color(0xFFE3B7BB),
    onTertiary = Color(0xFF472A2D),
    background = Color(0xFF141110),
    onBackground = Color(0xFFECE4DA),
    surface = Color(0xFF1B1715),
    onSurface = Color(0xFFECE4DA),
    surfaceVariant = Color(0xFF2B2521),
    onSurfaceVariant = Color(0xFFD1C5B8),
    outline = Color(0xFF9A8F82),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

private val ErolitTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
)

@Composable
fun ErolitTheme(
    theme: String,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val dark = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = ErolitTypography,
        content = content,
    )
}
