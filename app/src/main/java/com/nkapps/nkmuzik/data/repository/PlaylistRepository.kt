package com.nkapps.nkmuzik.data.repository

import com.nkapps.nkmuzik.data.db.entity.PlaylistEntity
import com.nkapps.nkmuzik.data.db.entity.PlaylistTrackEntity
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.data.model.LocalTrack
import kotlinx.coroutines.flow.Flow

data class PlaylistWithTracks(
    val playlist: PlaylistEntity,
    val tracks: List<PlaylistTrackEntity>
)

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>
    fun getPlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>>
    suspend fun createPlaylist(name: String): Long
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun updatePlaylistName(playlistId: Long, newName: String)
    suspend fun addTrackToPlaylist(playlistId: Long, track: OnlineSearchResultDto)
    suspend fun addLocalTrackToPlaylist(playlistId: Long, track: LocalTrack)
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)
    suspend fun deleteTrackFromAllPlaylists(trackId: String, mediaUri: String)

    fun getFavoriteTracks(): Flow<List<OnlineSearchResultDto>>
    suspend fun toggleFavorite(track: OnlineSearchResultDto)
    suspend fun isFavorite(videoId: String, url: String = ""): Boolean
    suspend fun addFavorite(track: OnlineSearchResultDto)
    suspend fun removeFavorite(videoId: String, url: String = "")
}
