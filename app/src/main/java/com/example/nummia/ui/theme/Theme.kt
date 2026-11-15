package com.example.nummia.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.nummia.model.settings.AppThemePalette

private val DefaultDarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = TextPrimary,
    secondary = BackgroundSecondary,
    onSecondary = TextPrimary,
    tertiary = AccentBlue,
    background = DarkBackground,
    onBackground = TextOnDark,
    surface = DarkSurface,
    onSurface = TextOnDark,
    surfaceVariant = BackgroundSecondary,
    onSurfaceVariant = TextPrimary
)

private val DefaultLightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = TextPrimary,
    secondary = BackgroundSecondary,
    onSecondary = TextPrimary,
    tertiary = AccentBlue,
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSecondary,
    onSurfaceVariant = TextPrimary
)

private val MidnightColorScheme = darkColorScheme(
    primary = MidnightAccent,
    onPrimary = MidnightText,
    secondary = MidnightSecondary,
    onSecondary = MidnightText,
    tertiary = MidnightAccent,
    background = MidnightBackground,
    onBackground = MidnightText,
    surface = MidnightSurface,
    onSurface = MidnightText,
    surfaceVariant = MidnightSecondary,
    onSurfaceVariant = MidnightText,
    outline = MidnightSecondary
)

private val SoftBlueColorScheme = lightColorScheme(
    primary = SoftAccent,
    onPrimary = SoftText,
    secondary = SoftButton,
    onSecondary = SoftText,
    tertiary = SoftButton,
    background = SoftBackground,
    onBackground = SoftText,
    surface = SoftSurface,
    onSurface = SoftText,
    surfaceVariant = SoftSecondary,
    onSurfaceVariant = SoftText,
    outline = SoftBorder
)

private val ClassicBlueColorScheme = lightColorScheme(
    primary = ClassicPrimary,
    onPrimary = Color.White,
    secondary = ClassicSecondary,
    onSecondary = Color.White,
    tertiary = ClassicAccent,
    background = ClassicSoft,
    onBackground = ClassicText,
    surface = ClassicCard,
    onSurface = ClassicText,
    surfaceVariant = ClassicSecondary,
    onSurfaceVariant = ClassicText,
    outline = ClassicSecondary
)

private val PetroleumColorScheme = darkColorScheme(
    primary = PetroleumAccent,
    onPrimary = PetroleumText,
    secondary = PetroleumSecondary,
    onSecondary = PetroleumText,
    tertiary = PetroleumAccent,
    background = PetroleumBackground,
    onBackground = PetroleumText,
    surface = PetroleumSurface,
    onSurface = PetroleumText,
    surfaceVariant = PetroleumSecondary,
    onSurfaceVariant = PetroleumText,
    outline = PetroleumSecondary
)

private val GreenFinanceColorScheme = lightColorScheme(
    primary = GreenAccent,
    onPrimary = Color.White,
    secondary = GreenSecondary,
    onSecondary = GreenText,
    tertiary = GreenButton,
    background = GreenBackground,
    onBackground = GreenText,
    surface = GreenSurface,
    onSurface = GreenText,
    surfaceVariant = GreenSecondary,
    onSurfaceVariant = GreenText,
    outline = GreenSecondary
)

private val LilacPremiumColorScheme = lightColorScheme(
    primary = LilacAccent,
    onPrimary = Color.White,
    secondary = LilacSecondary,
    onSecondary = LilacText,
    tertiary = LilacButton,
    background = LilacBackground,
    onBackground = LilacText,
    surface = LilacSurface,
    onSurface = LilacText,
    surfaceVariant = LilacSecondary,
    onSurfaceVariant = LilacText,
    outline = LilacSecondary
)

private val GreyElegantColorScheme = lightColorScheme(
    primary = GreyAccent,
    onPrimary = Color.White,
    secondary = GreySecondary,
    onSecondary = GreyText,
    tertiary = GreyButton,
    background = GreyBackground,
    onBackground = GreyText,
    surface = GreySurface,
    onSurface = GreyText,
    surfaceVariant = GreySecondary,
    onSurfaceVariant = GreyText,
    outline = GreySecondary
)

private val BlueCorporateColorScheme = lightColorScheme(
    primary = CorporateAccent,
    onPrimary = Color.White,
    secondary = CorporateSecondary,
    onSecondary = CorporateText,
    tertiary = CorporateButton,
    background = CorporateBackground,
    onBackground = CorporateText,
    surface = CorporateSurface,
    onSurface = CorporateText,
    surfaceVariant = CorporateSecondary,
    onSurfaceVariant = CorporateText,
    outline = CorporateSecondary
)

private val OrangeFinanceColorScheme = lightColorScheme(
    primary = OrangeAccent,
    onPrimary = Color.White,
    secondary = OrangeSecondary,
    onSecondary = OrangeText,
    tertiary = OrangeButton,
    background = OrangeBackground,
    onBackground = OrangeText,
    surface = OrangeSurface,
    onSurface = OrangeText,
    surfaceVariant = OrangeSecondary,
    onSurfaceVariant = OrangeText,
    outline = OrangeSecondary
)

private val YellowWarmColorScheme = lightColorScheme(
    primary = YellowAccent,
    onPrimary = Color.White,
    secondary = YellowSecondary,
    onSecondary = YellowText,
    tertiary = YellowButton,
    background = YellowBackground,
    onBackground = YellowText,
    surface = YellowSurface,
    onSurface = YellowText,
    surfaceVariant = YellowSecondary,
    onSurfaceVariant = YellowText,
    outline = YellowSecondary
)

private val RedCoralColorScheme = lightColorScheme(
    primary = RedAccent,
    onPrimary = Color.White,
    secondary = RedSecondary,
    onSecondary = RedText,
    tertiary = RedButton,
    background = RedBackground,
    onBackground = RedText,
    surface = RedSurface,
    onSurface = RedText,
    surfaceVariant = RedSecondary,
    onSurfaceVariant = RedText,
    outline = RedSecondary
)

private val CharcoalColorScheme = darkColorScheme(
    primary = CharcoalAccent,
    onPrimary = CharcoalBackground,
    secondary = CharcoalSecondary,
    onSecondary = CharcoalText,
    tertiary = CharcoalAccent,
    background = CharcoalBackground,
    onBackground = CharcoalText,
    surface = CharcoalSurface,
    onSurface = CharcoalText,
    surfaceVariant = CharcoalSecondary,
    onSurfaceVariant = CharcoalText,
    outline = CharcoalSecondary
)

private val NeutralLightColorScheme = lightColorScheme(
    primary = LightNeutralAccent,
    onPrimary = Color.White,
    secondary = LightNeutralSecondary,
    onSecondary = LightNeutralText,
    tertiary = LightNeutralButton,
    background = LightNeutralBackground,
    onBackground = LightNeutralText,
    surface = LightNeutralSurface,
    onSurface = LightNeutralText,
    surfaceVariant = LightNeutralSecondary,
    onSurfaceVariant = LightNeutralText,
    outline = LightNeutralSecondary
)

/** Aplica la paleta elegida y, si se solicita, los colores dinámicos de Android 12 o posterior. */
@Composable
fun MyApplicationTheme(
    palette: AppThemePalette = AppThemePalette.DEFAULT,
    darkTheme: Boolean = when (palette) {
        AppThemePalette.MIDNIGHT, AppThemePalette.PETROLEUM, AppThemePalette.CHARCOAL -> true
        else -> isSystemInDarkTheme()
    },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        palette == AppThemePalette.MIDNIGHT -> MidnightColorScheme
        palette == AppThemePalette.SOFT -> SoftBlueColorScheme
        palette == AppThemePalette.CLASSIC -> ClassicBlueColorScheme
        palette == AppThemePalette.PETROLEUM -> PetroleumColorScheme
        palette == AppThemePalette.GREEN -> GreenFinanceColorScheme
        palette == AppThemePalette.LILAC -> LilacPremiumColorScheme
        palette == AppThemePalette.GREY -> GreyElegantColorScheme
        palette == AppThemePalette.BLUE -> BlueCorporateColorScheme
        palette == AppThemePalette.ORANGE -> OrangeFinanceColorScheme
        palette == AppThemePalette.YELLOW -> YellowWarmColorScheme
        palette == AppThemePalette.RED -> RedCoralColorScheme
        palette == AppThemePalette.CHARCOAL -> CharcoalColorScheme
        palette == AppThemePalette.LIGHT -> NeutralLightColorScheme
        darkTheme -> DefaultDarkColorScheme
        else -> DefaultLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}