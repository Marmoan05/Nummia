package com.example.nummia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.nummia.model.settings.AppThemePalette

/** Aplica la paleta elegida y, si se solicita, los colores dinámicos de Android 12 o posterior. */
@Composable
fun MyApplicationTheme(
    palette: AppThemePalette = AppThemePalette.DEFAULT,
    darkTheme: Boolean = when (palette) {
        AppThemePalette.MIDNIGHT,
        AppThemePalette.PETROLEUM,
        AppThemePalette.CHARCOAL -> true

        else -> isSystemInDarkTheme()
    },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = AppColorSchemes.resolve(
        palette = palette,
        darkTheme = darkTheme,
        dynamicColor = dynamicColor
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
