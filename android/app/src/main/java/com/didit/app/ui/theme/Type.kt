package com.didit.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.didit.app.R

// Body text — mirrors `font-sans: Outfit`.
val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)

// Headings — mirrors `font-display: "Cal Sans"`.
val CalSans = FontFamily(Font(R.font.cal_sans, FontWeight.Normal))

val DiditTypography = Typography(
    displayLarge = TextStyle(fontFamily = CalSans, fontWeight = FontWeight.Normal, fontSize = 36.sp),
    displayMedium = TextStyle(fontFamily = CalSans, fontWeight = FontWeight.Normal, fontSize = 30.sp),
    displaySmall = TextStyle(fontFamily = CalSans, fontWeight = FontWeight.Normal, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = CalSans, fontWeight = FontWeight.Normal, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = CalSans, fontWeight = FontWeight.Normal, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Medium, fontSize = 10.sp),
)
