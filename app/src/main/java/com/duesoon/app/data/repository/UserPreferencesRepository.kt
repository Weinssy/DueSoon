package com.duesoon.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import com.duesoon.app.domain.model.AccentPalette
import com.duesoon.app.domain.model.VisualDensity

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class UserPreferencesState(
    val theme: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val notificationsEnabled: Boolean = true,
    val accentPalette: AccentPalette = AccentPalette.INDIGO,
    val visualDensity: VisualDensity = VisualDensity.COMFORTABLE,
    val hapticsEnabled: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {
    private val dataStore = context.dataStore
    
    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val VISUAL_DENSITY = stringPreferencesKey("visual_density")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
    }

    val userPreferencesFlow: Flow<UserPreferencesState> = dataStore.data.map { preferences ->
        val theme = preferences[PreferencesKeys.THEME] ?: "SYSTEM"
        val notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
        
        val accentPaletteStr = preferences[PreferencesKeys.ACCENT_PALETTE]
        val accentPalette = try {
            if (accentPaletteStr != null) AccentPalette.valueOf(accentPaletteStr) else AccentPalette.INDIGO
        } catch (e: IllegalArgumentException) {
            AccentPalette.INDIGO
        }

        val visualDensityStr = preferences[PreferencesKeys.VISUAL_DENSITY]
        val visualDensity = try {
            if (visualDensityStr != null) VisualDensity.valueOf(visualDensityStr) else VisualDensity.COMFORTABLE
        } catch (e: IllegalArgumentException) {
            VisualDensity.COMFORTABLE
        }
        
        val hapticsEnabled = preferences[PreferencesKeys.HAPTIC_FEEDBACK_ENABLED] ?: true

        UserPreferencesState(theme, notificationsEnabled, accentPalette, visualDensity, hapticsEnabled)
    }

    suspend fun updateTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateAccentPalette(palette: AccentPalette) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCENT_PALETTE] = palette.name
        }
    }

    suspend fun updateVisualDensity(density: VisualDensity) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.VISUAL_DENSITY] = density.name
        }
    }

    suspend fun updateHapticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }
}
