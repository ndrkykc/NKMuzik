package com.nkapps.nkmuzik.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nkapps.nkmuzik.ui.theme.ThemeMode
import com.nkapps.nkmuzik.utils.LanguageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

data class LastPlayedTrackInfo(
    val trackId: String,
    val title: String,
    val artist: String,
    val artworkUri: String,
    val contentUri: String,
    val position: Long
)

@Singleton
class ThemePreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    private val COVER_DYNAMIC_COLOR_KEY = booleanPreferencesKey("cover_dynamic_color")
    private val LANGUAGE_KEY = stringPreferencesKey("app_language")

    private val LAST_PLAYED_TRACK_ID = stringPreferencesKey("last_played_track_id")
    private val LAST_PLAYED_TITLE = stringPreferencesKey("last_played_title")
    private val LAST_PLAYED_ARTIST = stringPreferencesKey("last_played_artist")
    private val LAST_PLAYED_ARTWORK_URI = stringPreferencesKey("last_played_artwork_uri")
    private val LAST_PLAYED_CONTENT_URI = stringPreferencesKey("last_played_content_uri")
    private val LAST_PLAYED_POSITION = longPreferencesKey("last_played_position")
    
    // Mağaza güvenliği için Paskalya Yumurtası (Easter Egg) / Gizli Kilit durumu
    private val IS_ONLINE_UNLOCKED = booleanPreferencesKey("is_online_unlocked")

    val initialThemeMode: ThemeMode = ThemeMode.DARK

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val name = prefs[THEME_MODE_KEY] ?: ThemeMode.DARK.name
        try {
            ThemeMode.valueOf(name)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DYNAMIC_COLOR_KEY] ?: false
    }

    val isCoverDynamicColorEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[COVER_DYNAMIC_COLOR_KEY] ?: false
    }

    val lastPlayedTrack: Flow<LastPlayedTrackInfo?> = context.dataStore.data.map { prefs ->
        val id = prefs[LAST_PLAYED_TRACK_ID] ?: return@map null
        val title = prefs[LAST_PLAYED_TITLE] ?: ""
        val artist = prefs[LAST_PLAYED_ARTIST] ?: ""
        val artworkUri = prefs[LAST_PLAYED_ARTWORK_URI] ?: ""
        val contentUri = prefs[LAST_PLAYED_CONTENT_URI] ?: ""
        val position = prefs[LAST_PLAYED_POSITION] ?: 0L
        if (id.isBlank() && title.isBlank()) null
        else LastPlayedTrackInfo(id, title, artist, artworkUri, contentUri, position)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    suspend fun setCoverDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[COVER_DYNAMIC_COLOR_KEY] = enabled
        }
    }

    val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LANGUAGE_KEY] ?: LanguageManager.getInitialLanguage()
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = lang
        }
    }
    
    val isOnlineUnlocked: Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)
    
    suspend fun setOnlineUnlocked(unlocked: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_ONLINE_UNLOCKED] = unlocked
        }
    }

    suspend fun saveLastPlayedTrack(
        trackId: String,
        title: String,
        artist: String,
        artworkUri: String,
        contentUri: String,
        position: Long
    ) {
        context.dataStore.edit { prefs ->
            prefs[LAST_PLAYED_TRACK_ID] = trackId
            prefs[LAST_PLAYED_TITLE] = title
            prefs[LAST_PLAYED_ARTIST] = artist
            prefs[LAST_PLAYED_ARTWORK_URI] = artworkUri
            prefs[LAST_PLAYED_CONTENT_URI] = contentUri
            prefs[LAST_PLAYED_POSITION] = position
        }
    }
}
