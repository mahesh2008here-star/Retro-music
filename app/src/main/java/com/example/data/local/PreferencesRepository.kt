package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AudioQualityPreference
import com.example.model.GlassQualityMode
import com.example.model.ThemeModePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.masterWaveDataStore: DataStore<Preferences> by preferencesDataStore(name = "masterwave_prefs")

data class UserPreferencesState(
    val themeMode: ThemeModePreference = ThemeModePreference.SYSTEM,
    val glassQuality: GlassQualityMode = GlassQualityMode.AUTO,
    val audioQuality: AudioQualityPreference = AudioQualityPreference.HIGH,
    val reducedMotion: Boolean = false,
    val dataSaver: Boolean = false,
    val recentSearches: List<String> = emptyList()
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val GLASS_QUALITY = stringPreferencesKey("glass_quality")
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val DATA_SAVER = booleanPreferencesKey("data_saver")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches_delimited")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.masterWaveDataStore.data.map { prefs ->
        val theme = prefs[Keys.THEME_MODE]?.let { raw ->
            enumValues<ThemeModePreference>().firstOrNull { it.name == raw }
        } ?: ThemeModePreference.SYSTEM

        val glass = prefs[Keys.GLASS_QUALITY]?.let { raw ->
            enumValues<GlassQualityMode>().firstOrNull { it.name == raw }
        } ?: GlassQualityMode.AUTO

        val audio = prefs[Keys.AUDIO_QUALITY]?.let { raw ->
            enumValues<AudioQualityPreference>().firstOrNull { it.name == raw }
        } ?: AudioQualityPreference.HIGH

        val history = prefs[Keys.RECENT_SEARCHES]
            ?.split("\u001F")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        UserPreferencesState(
            themeMode = theme,
            glassQuality = glass,
            audioQuality = audio,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            dataSaver = prefs[Keys.DATA_SAVER] ?: false,
            recentSearches = history
        )
    }

    suspend fun setThemeMode(mode: ThemeModePreference) {
        context.masterWaveDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setGlassQuality(mode: GlassQualityMode) {
        context.masterWaveDataStore.edit { it[Keys.GLASS_QUALITY] = mode.name }
    }

    suspend fun setAudioQuality(quality: AudioQualityPreference) {
        context.masterWaveDataStore.edit { it[Keys.AUDIO_QUALITY] = quality.name }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.masterWaveDataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setDataSaver(enabled: Boolean) {
        context.masterWaveDataStore.edit { it[Keys.DATA_SAVER] = enabled }
    }

    suspend fun addRecentSearch(query: String) {
        val cleaned = query.trim()
        if (cleaned.isBlank()) return
        context.masterWaveDataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_SEARCHES]
                ?.split("\u001F")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            val updated = (listOf(cleaned) + current.filterNot { it.equals(cleaned, ignoreCase = true) })
                .take(12)
            prefs[Keys.RECENT_SEARCHES] = updated.joinToString("\u001F")
        }
    }

    suspend fun removeRecentSearch(query: String) {
        context.masterWaveDataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_SEARCHES]
                ?.split("\u001F")
                ?.filter { it.isNotBlank() && !it.equals(query, ignoreCase = true) }
                ?: emptyList()
            prefs[Keys.RECENT_SEARCHES] = current.joinToString("\u001F")
        }
    }

    suspend fun clearRecentSearches() {
        context.masterWaveDataStore.edit { it.remove(Keys.RECENT_SEARCHES) }
    }
}
