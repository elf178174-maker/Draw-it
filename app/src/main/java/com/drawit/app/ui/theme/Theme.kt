package com.drawit.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val LocalColors = staticCompositionLocalOf { LightPalette }
val LocalType = staticCompositionLocalOf { drawItType() }

/** Shorthand used across the UI: `DI.colors.brand`, `DI.type.title`. */
object DI {
    val colors: DrawItColors
        @Composable get() = LocalColors.current
    val type: DrawItType
        @Composable get() = LocalType.current
}

private fun materialColors(c: DrawItColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.brand, onPrimary = c.onBrand, primaryContainer = c.brandSoft, onPrimaryContainer = c.text,
        secondary = c.ink, onSecondary = Color.White, tertiary = c.flame,
        background = c.background, onBackground = c.text, surface = c.surface, onSurface = c.text,
        surfaceVariant = c.sunken, onSurfaceVariant = c.textMuted, outline = c.line, outlineVariant = c.line,
        error = c.red, onError = Color.White, scrim = c.scrim
    )
} else {
    lightColorScheme(
        primary = c.brand, onPrimary = c.onBrand, primaryContainer = c.brandSoft, onPrimaryContainer = c.text,
        secondary = c.ink, onSecondary = Color.White, tertiary = c.flame,
        background = c.background, onBackground = c.text, surface = c.surface, onSurface = c.text,
        surfaceVariant = c.sunken, onSurfaceVariant = c.textMuted, outline = c.line, outlineVariant = c.line,
        error = c.red, onError = Color.White, scrim = c.scrim
    )
}

val DrawItShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DrawItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkPalette else LightPalette
    val type = drawItType()
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
    CompositionLocalProvider(LocalColors provides colors, LocalType provides type) {
        MaterialTheme(
            colorScheme = materialColors(colors),
            typography = materialTypography(type),
            shapes = DrawItShapes,
            content = content
        )
    }
}
