package com.cloudlink.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private val UiFont = FontFamily.SansSerif
private fun cloudTextStyle(
    fontSize: TextUnit,
    lineHeight: TextUnit,
    fontWeight: FontWeight,
    letterSpacing: TextUnit = 0.sp
) = TextStyle(
    fontFamily = UiFont,
    fontWeight = fontWeight,
    fontSize = fontSize,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing
)

val CloudLinkTypography = Typography(
    displayLarge = cloudTextStyle(44.sp, 52.sp, FontWeight.SemiBold, (-0.4).sp),
    displayMedium = cloudTextStyle(36.sp, 44.sp, FontWeight.SemiBold, (-0.25).sp),
    displaySmall = cloudTextStyle(32.sp, 40.sp, FontWeight.SemiBold),
    headlineLarge = cloudTextStyle(30.sp, 38.sp, FontWeight.Bold, (-0.2).sp),
    headlineMedium = cloudTextStyle(26.sp, 34.sp, FontWeight.Bold, (-0.1).sp),
    headlineSmall = cloudTextStyle(22.sp, 29.sp, FontWeight.SemiBold),
    titleLarge = cloudTextStyle(20.sp, 27.sp, FontWeight.SemiBold),
    titleMedium = cloudTextStyle(16.sp, 23.sp, FontWeight.SemiBold, 0.1.sp),
    titleSmall = cloudTextStyle(14.sp, 20.sp, FontWeight.SemiBold, 0.1.sp),
    bodyLarge = cloudTextStyle(16.sp, 24.sp, FontWeight.Normal, 0.1.sp),
    bodyMedium = cloudTextStyle(14.sp, 21.sp, FontWeight.Normal, 0.1.sp),
    bodySmall = cloudTextStyle(12.sp, 18.sp, FontWeight.Normal, 0.2.sp),
    labelLarge = cloudTextStyle(14.sp, 20.sp, FontWeight.SemiBold, 0.1.sp),
    labelMedium = cloudTextStyle(12.sp, 17.sp, FontWeight.SemiBold, 0.3.sp),
    labelSmall = cloudTextStyle(11.sp, 16.sp, FontWeight.SemiBold, 0.6.sp)
)
