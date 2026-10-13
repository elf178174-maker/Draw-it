package com.drawit.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/*
 * A bright, chunky palette built around the logo orange. Every accent comes
 * as a pair: the face colour and a darker "lip" used for the 3D edge of
 * buttons and cards, the way Duolingo builds its controls.
 */

val BrandOrange = Color(0xFFFF5001)
val BrandLip = Color(0xFFD13F00)

@Immutable
data class DrawItColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val sunken: Color,
    val line: Color,
    val lineStrong: Color,
    val text: Color,
    val textMuted: Color,
    val textFaint: Color,

    val brand: Color = BrandOrange,
    val brandLip: Color = BrandLip,
    val brandSoft: Color,
    val onBrand: Color = Color.White,

    val flame: Color = Color(0xFFFF9600),
    val flameLip: Color = Color(0xFFE07A00),
    val flameSoft: Color,

    val ink: Color = Color(0xFF4F7BFF),
    val inkLip: Color = Color(0xFF3557DB),
    val inkSoft: Color,

    val gold: Color = Color(0xFFFFC800),
    val goldLip: Color = Color(0xFFE09A00),
    val goldSoft: Color,

    val ice: Color = Color(0xFF1CB0F6),
    val iceLip: Color = Color(0xFF1287C7),
    val iceSoft: Color,

    val green: Color = Color(0xFF3CC85A),
    val greenLip: Color = Color(0xFF2B9E45),
    val greenSoft: Color,

    val red: Color = Color(0xFFFF4B4B),
    val redLip: Color = Color(0xFFD93030),
    val redSoft: Color,

    val purple: Color = Color(0xFFA560FF),
    val purpleLip: Color = Color(0xFF7C3FD6),
    val purpleSoft: Color,

    val scrim: Color
)

val LightPalette = DrawItColors(
    isDark = false,
    background = Color(0xFFFFFCF8),
    surface = Color(0xFFFFFFFF),
    sunken = Color(0xFFF6EFE7),
    line = Color(0xFFEFE5DA),
    lineStrong = Color(0xFFE2D5C6),
    text = Color(0xFF2B2118),
    textMuted = Color(0xFF8A7B6D),
    textFaint = Color(0xFFB9AC9F),
    brandSoft = Color(0xFFFFE9DD),
    flameSoft = Color(0xFFFFF0D9),
    inkSoft = Color(0xFFE5ECFF),
    goldSoft = Color(0xFFFFF5CC),
    iceSoft = Color(0xFFDDF4FF),
    greenSoft = Color(0xFFDFF7E4),
    redSoft = Color(0xFFFFE3E3),
    purpleSoft = Color(0xFFF1E6FF),
    scrim = Color(0xCC1C140E)
)

val DarkPalette = DrawItColors(
    isDark = true,
    background = Color(0xFF17120F),
    surface = Color(0xFF221B17),
    sunken = Color(0xFF2B231E),
    line = Color(0xFF392F28),
    lineStrong = Color(0xFF4A3D34),
    text = Color(0xFFFFF4EA),
    textMuted = Color(0xFFB8A698),
    textFaint = Color(0xFF7D6E62),
    brand = Color(0xFFFF6A26),
    brandLip = Color(0xFFC24300),
    brandSoft = Color(0xFF4A2412),
    flameSoft = Color(0xFF45300F),
    inkSoft = Color(0xFF1F2A52),
    goldSoft = Color(0xFF473B10),
    iceSoft = Color(0xFF0F3447),
    greenSoft = Color(0xFF173A22),
    redSoft = Color(0xFF4A1C1C),
    purpleSoft = Color(0xFF33214F),
    scrim = Color(0xDD000000)
)
