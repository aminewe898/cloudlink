package com.cloudlink.app.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore("theme_prefs")

@Singleton
class ThemeManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val themeKey = stringPreferencesKey("app_theme")
    private val secureScreenKey = booleanPreferencesKey("secure_screen")

    val currentTheme: Flow<AppThemeType> = context.themeDataStore.data.map { prefs ->
        val themeName = prefs[themeKey] ?: AppThemeType.DARK.name
        try {
            AppThemeType.valueOf(themeName)
        } catch (e: Exception) {
            AppThemeType.DARK
        }
    }

    val secureScreen: Flow<Boolean> = context.themeDataStore.data.map { prefs ->
        prefs[secureScreenKey] ?: true
    }

    suspend fun setTheme(theme: AppThemeType) {
        context.themeDataStore.edit { prefs ->
            prefs[themeKey] = theme.name
        }
    }

    suspend fun setSecureScreen(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[secureScreenKey] = enabled
        }
    }
}
