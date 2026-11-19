package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.AppFont

val PlusJakartaFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val InterFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val ManropeFamily = FontFamily(
    Font(R.font.manrope, FontWeight.Normal),
    Font(R.font.manrope, FontWeight.Medium),
    Font(R.font.manrope, FontWeight.SemiBold),
    Font(R.font.manrope, FontWeight.Bold)
)

val GeistFamily = FontFamily(
    Font(R.font.geist, FontWeight.Normal),
    Font(R.font.geist, FontWeight.Medium),
    Font(R.font.geist, FontWeight.SemiBold),
    Font(R.font.geist, FontWeight.Bold)
)

val GeneralSansFamily = FontFamily(
    Font(R.font.general_sans, FontWeight.Normal),
    Font(R.font.general_sans, FontWeight.Medium),
    Font(R.font.general_sans, FontWeight.SemiBold),
    Font(R.font.general_sans, FontWeight.Bold)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

fun AppFont.toFontFamily(): FontFamily = when (this) {
    AppFont.INTER -> InterFamily
    AppFont.MANROPE -> ManropeFamily
    AppFont.GEIST -> GeistFamily
    AppFont.GENERAL_SANS -> GeneralSansFamily
    AppFont.PLUS_JAKARTA_SANS -> PlusJakartaFamily
}

fun studyTimeTypography(appFont: AppFont = AppFont.PLUS_JAKARTA_SANS): Typography {
    val activeFamily = appFont.toFontFamily()
    return Typography(
        displayLarge = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            lineHeight = 54.sp,
            letterSpacing = (-1).sp
        ),
        displayMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp
        ),
        titleLarge = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp
        ),
        titleMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelLarge = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = activeFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    )
}

val StudyTimeTypography = studyTimeTypography(AppFont.PLUS_JAKARTA_SANS)
