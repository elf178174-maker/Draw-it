package com.drawit.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = TintLight,
    onPrimaryContainer = OnTintLight,
    secondary = InkMutedLight,
    onSecondary = Color.White,
    secondaryContainer = PaperSunken,
    onSecondaryContainer = InkLight,
    tertiary = BrandOrangeSoft,
    onTertiary = Color.White,
    background = PaperLight,
    onBackground = InkLight,
    surface = PaperLight,
    onSurface = InkLight,
    surfaceVariant = PaperSunken,
    onSurfaceVariant = InkMutedLight,
    surfaceContainer = PaperRaised,
    surfaceContainerHigh = PaperRaised,
    surfaceContainerLow = PaperRaised,
    surfaceContainerLowest = Color.White,
    surfaceContainerHighest = PaperSunken,
    outline = LineLight,
    outlineVariant = LineLight,
    error = Color(0xFFB3261E),
    onError = Color.White,
    scrim = Color(0x99120E0A)
)

private val DarkColors = darkColorScheme(
    primary = BrandOrangeSoft,
    onPrimary = Color(0xFF3D1200),
    primaryContainer = TintDark,
    onPrimaryContainer = OnTintDark,
    secondary = InkMutedDark,
    onSecondary = Color(0xFF201A14),
    secondaryContainer = PaperSunkenDark,
    onSecondaryContainer = InkDark,
    tertiary = BrandOrange,
    onTertiary = Color.White,
    background = PaperDark,
    onBackground = InkDark,
    surface = PaperDark,
    onSurface = InkDark,
    surfaceVariant = PaperSunkenDark,
    onSurfaceVariant = InkMutedDark,
    surfaceContainer = PaperRaisedDark,
    surfaceContainerHigh = PaperRaisedDark,
    surfaceContainerLow = PaperRaisedDark,
    surfaceContainerLowest = Color(0xFF100D0A),
    surfaceContainerHighest = PaperSunkenDark,
    outline = LineDark,
    outlineVariant = LineDark,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    scrim = Color(0xAA000000)
)

val DrawItShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun DrawItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = DrawItTypography,
        shapes = DrawItShapes,
        content = content
    )
}
