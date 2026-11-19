package com.example.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AppFont
import com.example.core.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = BrandViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF28235E),
    onPrimaryContainer = Color(0xFFE5E2FF),
    secondary = AccentLavender,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF29204B),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = AccentTeal,
    onTertiary = Color(0xFF042F2E),
    tertiaryContainer = Color(0xFF113B38),
    onTertiaryContainer = Color(0xFF99F6E4),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    error = AccentRose,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BrandViolet,
    onPrimary = Color.White,
    primaryContainer = BrandVioletSoft,
    onPrimaryContainer = BrandIndigoDeep,
    secondary = AccentLavender,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3EEFF),
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = AccentTeal,
    onTertiary = Color.White,
    tertiaryContainer = AccentTealSoft,
    onTertiaryContainer = Color(0xFF134E4A),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    error = AccentRose,
    onError = Color.White
)

private val GradientColorScheme = lightColorScheme(
    primary = BrandViolet,
    onPrimary = Color.White,
    primaryContainer = BrandVioletSoft,
    onPrimaryContainer = BrandIndigoDeep,
    secondary = AccentLavender,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3EEFF),
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = AccentTeal,
    onTertiary = Color.White,
    tertiaryContainer = AccentTealSoft,
    onTertiaryContainer = Color(0xFF134E4A),
    background = Color.Transparent,
    onBackground = LightTextPrimary,
    surface = Color(0xECFFFFFF),
    onSurface = LightTextPrimary,
    surfaceContainerLowest = Color(0xD8FFFFFF),
    surfaceContainerLow = Color(0xE2FFFFFF),
    surfaceContainer = Color(0xECFFFFFF),
    surfaceContainerHigh = Color(0xFAFFFFFF),
    surfaceContainerHighest = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xDAE8F4FF),
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFC2DCFA),
    error = AccentRose,
    onError = Color.White
)

private val StudyTimeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StudyTimeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    appFont: AppFont = AppFont.PLUS_JAKARTA_SANS,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.GRADIENT -> GradientColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = studyTimeTypography(appFont),
        shapes = StudyTimeShapes
    ) {
        if (themeMode == ThemeMode.GRADIENT) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_theme_gradient_bg),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                content()
            }
        } else {
            content()
        }
    }
}
