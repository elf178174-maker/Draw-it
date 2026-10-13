package com.drawit.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.drawit.app.R

/** Nunito: rounded, heavy and friendly. Bundled, so it looks the same everywhere. */
val Nunito = FontFamily(
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black)
)

@Immutable
data class DrawItType(
    val hero: TextStyle,
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val small: TextStyle,
    val label: TextStyle,
    val button: TextStyle,
    val number: TextStyle
)

fun drawItType(family: FontFamily = Nunito) = DrawItType(
    hero = TextStyle(fontFamily = family, fontWeight = FontWeight.Black, fontSize = 64.sp, lineHeight = 66.sp, letterSpacing = (-1).sp),
    display = TextStyle(fontFamily = family, fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    headline = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, lineHeight = 29.sp),
    title = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 22.sp),
    body = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp),
    bodyStrong = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 22.sp),
    small = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    label = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.sp),
    button = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 0.6.sp),
    number = TextStyle(fontFamily = family, fontWeight = FontWeight.Black, fontSize = 18.sp, lineHeight = 22.sp)
)

/** Material components (dialogs, the time picker) pick these up. */
fun materialTypography(t: DrawItType) = Typography(
    displayLarge = t.hero,
    displayMedium = t.display,
    displaySmall = t.display,
    headlineLarge = t.display,
    headlineMedium = t.headline,
    headlineSmall = t.headline,
    titleLarge = t.headline,
    titleMedium = t.title,
    titleSmall = t.bodyStrong,
    bodyLarge = t.body,
    bodyMedium = t.body,
    bodySmall = t.small,
    labelLarge = t.button,
    labelMedium = t.label,
    labelSmall = t.label
)
