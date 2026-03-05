package com.saifit.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = SaiBlue,
    onPrimary = SaiWhite,
    primaryContainer = SaiBlueLight,
    secondary = SaiSaffron,
    onSecondary = SaiWhite,
    secondaryContainer = SaiSaffronLight,
    tertiary = SaiGreen,
    error = SaiRed,
    background = SaiWhite,
    surface = SaiSurface,
    onBackground = androidx.compose.ui.graphics.Color.Black,
    onSurface = androidx.compose.ui.graphics.Color.Black,
    outline = SaiGray
)

private val DarkColorScheme = darkColorScheme(
    primary = SaiBlueLight,
    onPrimary = SaiWhite,
    primaryContainer = SaiBlueDark,
    secondary = SaiSaffronLight,
    onSecondary = SaiWhite,
    secondaryContainer = SaiSaffronDark,
    tertiary = SaiGreenLight,
    error = SaiRedLight,
    background = SaiDarkBg,
    surface = SaiSurfaceDark,
    onBackground = SaiOnSurfaceDark,
    onSurface = SaiOnSurfaceDark,
    outline = SaiGray,
    surfaceVariant = SaiDarkCard
)

@Composable
fun SaiFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalView.current.context
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SaiFitTypography,
        content = content
    )
}
