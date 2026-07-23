package com.beiratv.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tekasTvDataStore by preferencesDataStore(name = "tekastv_tv_preferences")

enum class ThemeMode { LIGHT, DARK, AMOLED }

data class PlaybackPreferences(
    val automaticQuality: Boolean = true,
    val fallbackStreams: Boolean = true
)

class TvPreferencesRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val automaticQuality = booleanPreferencesKey("automatic_quality")
        val fallbackStreams = booleanPreferencesKey("fallback_streams")
    }

    val themeMode: Flow<ThemeMode> = context.tekasTvDataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[Keys.theme] ?: ThemeMode.DARK.name) }.getOrDefault(ThemeMode.DARK)
    }

    val playback: Flow<PlaybackPreferences> = context.tekasTvDataStore.data.map { prefs ->
        PlaybackPreferences(
            automaticQuality = prefs[Keys.automaticQuality] ?: true,
            fallbackStreams = prefs[Keys.fallbackStreams] ?: true
        )
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.tekasTvDataStore.edit { it[Keys.theme] = mode.name }
    }

    suspend fun setAutomaticQuality(enabled: Boolean) {
        context.tekasTvDataStore.edit { it[Keys.automaticQuality] = enabled }
    }

    suspend fun setFallbackStreams(enabled: Boolean) {
        context.tekasTvDataStore.edit { it[Keys.fallbackStreams] = enabled }
    }
}
