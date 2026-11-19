package com.example.nummia.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.nummia.model.settings.AppThemePalette

/** Definiciones y resolución de las paletas visuales disponibles. */
internal object AppColorSchemes {
    private val defaultDark = darkColorScheme(
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

    private val defaultLight = lightColorScheme(
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

    private val midnight = darkColorScheme(
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

    private val softBlue = lightColorScheme(
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

    private val classicBlue = lightColorScheme(
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

    private val petroleum = darkColorScheme(
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

    private val greenFinance = lightColorScheme(
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

    private val lilacPremium = lightColorScheme(
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

    private val greyElegant = lightColorScheme(
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

    private val blueCorporate = lightColorScheme(
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

    private val orangeFinance = lightColorScheme(
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

    private val yellowWarm = lightColorScheme(
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

    private val redCoral = lightColorScheme(
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

    private val charcoal = darkColorScheme(
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

    private val neutralLight = lightColorScheme(
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

    @Composable
    fun resolve(
        palette: AppThemePalette,
        darkTheme: Boolean,
        dynamicColor: Boolean
    ): ColorScheme {
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = androidx.compose.ui.platform.LocalContext.current
            return if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        return when (palette) {
            AppThemePalette.MIDNIGHT -> midnight
            AppThemePalette.SOFT -> softBlue
            AppThemePalette.CLASSIC -> classicBlue
            AppThemePalette.PETROLEUM -> petroleum
            AppThemePalette.GREEN -> greenFinance
            AppThemePalette.LILAC -> lilacPremium
            AppThemePalette.GREY -> greyElegant
            AppThemePalette.BLUE -> blueCorporate
            AppThemePalette.ORANGE -> orangeFinance
            AppThemePalette.YELLOW -> yellowWarm
            AppThemePalette.RED -> redCoral
            AppThemePalette.CHARCOAL -> charcoal
            AppThemePalette.LIGHT -> neutralLight
            else -> if (darkTheme) defaultDark else defaultLight
        }
    }
}
