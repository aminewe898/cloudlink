package com.cloudlink.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat

@Composable
fun CloudLinkTheme(
    appThemeType: AppThemeType = AppThemeType.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = ThemeSelector.getColorScheme(appThemeType)
    val semanticColors = ThemeSelector.getSemanticColors(appThemeType)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colorScheme.surfaceContainerLow.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            val isLightMode = appThemeType == AppThemeType.LIGHT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLightMode
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLightMode
        }
    }

    CompositionLocalProvider(LocalCloudLinkSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CloudLinkTypography,
            shapes = CloudLinkShapes,
            content = content
        )
    }
}

object CloudLinkThemeValues {
    val semanticColors: CloudLinkSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCloudLinkSemanticColors.current
}
