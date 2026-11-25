package com.duesoon.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import com.duesoon.app.domain.model.AccentPalette
import com.duesoon.app.domain.model.PomodoroSettings
import com.duesoon.app.domain.model.VisualDensity

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class UserPreferencesState(
    val theme: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val notificationsEnabled: Boolean = true,
    val accentPalette: AccentPalette = AccentPalette.INDIGO,
    val visualDensity: VisualDensity = VisualDensity.COMFORTABLE,
    val hapticsEnabled: Boolean = true,
    val alarmReminderEnabled: Boolean = false,
    val pomodoroFocusMinutes: Int = PomodoroSettings.DEFAULT_FOCUS_MINUTES,
    val pomodoroShortBreakMinutes: Int = PomodoroSettings.DEFAULT_SHORT_BREAK_MINUTES,
    val pomodoroLongBreakMinutes: Int = PomodoroSettings.DEFAULT_LONG_BREAK_MINUTES,
    val pomodoroAutoStart: Boolean = false,
    val pomodoroSoundEnabled: Boolean = true
) {
    val pomodoroSettings: PomodoroSettings
        get() = PomodoroSettings(
            focusMinutes = pomodoroFocusMinutes,
            shortBreakMinutes = pomodoroShortBreakMinutes,
            longBreakMinutes = pomodoroLongBreakMinutes,
            autoStartNext = pomodoroAutoStart,
            soundEnabled = pomodoroSoundEnabled
        )
}

open class UserPreferencesRepository(private val context: Context) {
    private val dataStore = context.dataStore
    
    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val VISUAL_DENSITY = stringPreferencesKey("visual_density")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val ALARM_REMINDER_ENABLED = booleanPreferencesKey("alarm_reminder_enabled")
        val POMODORO_FOCUS_MINUTES = intPreferencesKey("pomodoro_focus_minutes")
        val POMODORO_SHORT_BREAK_MINUTES = intPreferencesKey("pomodoro_short_break_minutes")
        val POMODORO_LONG_BREAK_MINUTES = intPreferencesKey("pomodoro_long_break_minutes")
        val POMODORO_AUTO_START = booleanPreferencesKey("pomodoro_auto_start")
        val POMODORO_SOUND_ENABLED = booleanPreferencesKey("pomodoro_sound_enabled")
    }

    open val userPreferencesFlow: Flow<UserPreferencesState> = dataStore.data.map { preferences ->
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
        val alarmReminderEnabled = preferences[PreferencesKeys.ALARM_REMINDER_ENABLED] ?: false
        val pomodoroFocusMinutes = PomodoroSettings.sanitizeFocusMinutes(
            preferences[PreferencesKeys.POMODORO_FOCUS_MINUTES] ?: PomodoroSettings.DEFAULT_FOCUS_MINUTES
        )
        val pomodoroShortBreakMinutes = PomodoroSettings.sanitizeBreakMinutes(
            preferences[PreferencesKeys.POMODORO_SHORT_BREAK_MINUTES] ?: PomodoroSettings.DEFAULT_SHORT_BREAK_MINUTES
        )
        val pomodoroLongBreakMinutes = PomodoroSettings.sanitizeBreakMinutes(
            preferences[PreferencesKeys.POMODORO_LONG_BREAK_MINUTES] ?: PomodoroSettings.DEFAULT_LONG_BREAK_MINUTES
        )
        val pomodoroAutoStart = preferences[PreferencesKeys.POMODORO_AUTO_START] ?: false
        val pomodoroSoundEnabled = preferences[PreferencesKeys.POMODORO_SOUND_ENABLED] ?: true

        UserPreferencesState(
            theme = theme,
            notificationsEnabled = notificationsEnabled,
            accentPalette = accentPalette,
            visualDensity = visualDensity,
            hapticsEnabled = hapticsEnabled,
            alarmReminderEnabled = alarmReminderEnabled,
            pomodoroFocusMinutes = pomodoroFocusMinutes,
            pomodoroShortBreakMinutes = pomodoroShortBreakMinutes,
            pomodoroLongBreakMinutes = pomodoroLongBreakMinutes,
            pomodoroAutoStart = pomodoroAutoStart,
            pomodoroSoundEnabled = pomodoroSoundEnabled
        )
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

    open suspend fun updateAlarmReminderEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALARM_REMINDER_ENABLED] = enabled
        }
    }

    open suspend fun updatePomodoroSettings(settings: PomodoroSettings) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.POMODORO_FOCUS_MINUTES] =
                PomodoroSettings.sanitizeFocusMinutes(settings.focusMinutes)
            preferences[PreferencesKeys.POMODORO_SHORT_BREAK_MINUTES] =
                PomodoroSettings.sanitizeBreakMinutes(settings.shortBreakMinutes)
            preferences[PreferencesKeys.POMODORO_LONG_BREAK_MINUTES] =
                PomodoroSettings.sanitizeBreakMinutes(settings.longBreakMinutes)
            preferences[PreferencesKeys.POMODORO_AUTO_START] = settings.autoStartNext
            preferences[PreferencesKeys.POMODORO_SOUND_ENABLED] = settings.soundEnabled
        }
    }
}
