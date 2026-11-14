package com.nkapps.nkmuzik.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.deletedTracksDataStore: DataStore<Preferences> by preferencesDataStore(name = "deleted_tracks_store")

@Singleton
class DeletedTracksStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val DELETED_TRACK_IDS_KEY = stringSetPreferencesKey("deleted_track_ids")
    private val DELETED_FILE_PATHS_KEY = stringSetPreferencesKey("deleted_file_paths")

    val deletedTrackIds: Flow<Set<String>> = context.deletedTracksDataStore.data.map { prefs ->
        prefs[DELETED_TRACK_IDS_KEY] ?: emptySet()
    }

    val deletedFilePaths: Flow<Set<String>> = context.deletedTracksDataStore.data.map { prefs ->
        prefs[DELETED_FILE_PATHS_KEY] ?: emptySet()
    }

    suspend fun getDeletedTrackIds(): Set<String> {
        return deletedTrackIds.first()
    }

    suspend fun getDeletedFilePaths(): Set<String> {
        return deletedFilePaths.first()
    }

    suspend fun addDeletedTrack(trackId: String, filePath: String? = null, contentUri: String? = null) {
        context.deletedTracksDataStore.edit { prefs ->
            val currentIds = prefs[DELETED_TRACK_IDS_KEY] ?: emptySet()
            val newIds = currentIds.toMutableSet()
            if (trackId.isNotBlank()) newIds.add(trackId)
            if (!contentUri.isNullOrBlank()) newIds.add(contentUri)
            prefs[DELETED_TRACK_IDS_KEY] = newIds

            if (!filePath.isNullOrBlank()) {
                val currentPaths = prefs[DELETED_FILE_PATHS_KEY] ?: emptySet()
                prefs[DELETED_FILE_PATHS_KEY] = currentPaths + filePath
            }
        }
    }

    suspend fun removeDeletedTrack(
        trackId: String?,
        filePath: String? = null,
        contentUri: String? = null,
        title: String? = null
    ) {
        context.deletedTracksDataStore.edit { prefs ->
            val currentIds = prefs[DELETED_TRACK_IDS_KEY] ?: emptySet()
            val newIds = currentIds.toMutableSet()
            if (!trackId.isNullOrBlank()) newIds.remove(trackId)
            if (!contentUri.isNullOrBlank()) newIds.remove(contentUri)
            prefs[DELETED_TRACK_IDS_KEY] = newIds

            val currentPaths = prefs[DELETED_FILE_PATHS_KEY] ?: emptySet()
            val newPaths = currentPaths.toMutableSet()
            if (!filePath.isNullOrBlank()) {
                newPaths.remove(filePath)
            }
            if (!title.isNullOrBlank()) {
                val sanitized = title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                newPaths.removeAll { path -> path.contains(sanitized) }
            }
            prefs[DELETED_FILE_PATHS_KEY] = newPaths
        }
    }

    suspend fun isDeleted(
        trackId: String?,
        filePath: String?,
        contentUri: String? = null
    ): Boolean {
        val ids = getDeletedTrackIds()
        val paths = getDeletedFilePaths()
        return isDeleted(trackId, filePath, contentUri, ids, paths)
    }

    fun isDeleted(
        trackId: String?,
        filePath: String?,
        contentUri: String?,
        deletedIds: Set<String>,
        deletedPaths: Set<String>
    ): Boolean {
        if (!trackId.isNullOrBlank() && deletedIds.contains(trackId)) return true
        if (!contentUri.isNullOrBlank() && deletedIds.contains(contentUri)) return true
        if (!filePath.isNullOrBlank() && (deletedPaths.contains(filePath) || deletedIds.contains(filePath))) return true
        return false
    }
}
