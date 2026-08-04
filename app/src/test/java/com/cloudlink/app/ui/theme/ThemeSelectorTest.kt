package com.cloudlink.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeSelectorTest {
    @Test
    fun `every theme defines the component color roles used by the app shell`() {
        AppThemeType.entries.forEach { theme ->
            val colors = ThemeSelector.getColorScheme(theme)
            assertNotEquals("$theme primary", Color.Unspecified, colors.primary)
            assertNotEquals("$theme primary container", Color.Unspecified, colors.primaryContainer)
            assertNotEquals("$theme navigation surface", Color.Unspecified, colors.surfaceContainerLow)
            assertNotEquals("$theme navigation indicator", colors.surfaceContainerLow, colors.primaryContainer)
            assertNotEquals("$theme surface hierarchy", colors.background, colors.surfaceContainerHigh)
            assertNotEquals("$theme outline hierarchy", colors.outline, colors.outlineVariant)
        }
    }

    @Test
    fun `every theme exposes explicit operational status colors`() {
        AppThemeType.entries.forEach { theme ->
            val colors = ThemeSelector.getSemanticColors(theme)
            assertNotEquals("$theme success", Color.Unspecified, colors.success)
            assertNotEquals("$theme warning", Color.Unspecified, colors.warning)
            assertNotEquals("$theme info", Color.Unspecified, colors.info)
            assertNotEquals("$theme success container", colors.success, colors.successContainer)
        }
    }
}
