package com.drawit.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private val Editorial = FontFamily.Serif
private val Grotesk = FontFamily.SansSerif

private val trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

val DrawItTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Editorial, fontWeight = FontWeight.Normal,
        fontSize = 52.sp, lineHeight = 56.sp, letterSpacing = (-1.4).sp,
        lineHeightStyle = trim
    ),
    displayMedium = TextStyle(
        fontFamily = Editorial, fontWeight = FontWeight.Normal,
        fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-1.0).sp,
        lineHeightStyle = trim
    ),
    displaySmall = TextStyle(
        fontFamily = Editorial, fontWeight = FontWeight.Normal,
        fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp,
        lineHeightStyle = trim
    ),
    headlineMedium = TextStyle(
        fontFamily = Editorial, fontWeight = FontWeight.Normal,
        fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Editorial, fontWeight = FontWeight.Normal,
        fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Grotesk, fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp, lineHeight = 25.sp, letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Grotesk, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Grotesk, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Grotesk, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Grotesk, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Grotesk, fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp, lineHeight = 18.sp, letterSpacing = 0.15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Grotesk, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Grotesk, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Grotesk, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 1.2.sp
    )
)
