package com.nkapps.nkmuzik.data.db.dao

import androidx.room.*
import com.nkapps.nkmuzik.data.db.entity.FavoriteTrackEntity
import com.nkapps.nkmuzik.data.db.entity.PlaylistEntity
import com.nkapps.nkmuzik.data.db.entity.PlaylistTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun getTracksForPlaylist(playlistId: Long): Flow<List<PlaylistTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(track: PlaylistTrackEntity): Long

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)

    @Query("DELETE FROM playlist_tracks WHERE trackId = :trackId OR mediaUri = :mediaUri")
    suspend fun deleteTrackFromAllPlaylists(trackId: String, mediaUri: String)

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun updatePlaylistName(playlistId: Long, newName: String)

    @Query("SELECT * FROM favorite_tracks ORDER BY addedAt DESC")
    fun getFavoriteTracks(): Flow<List<FavoriteTrackEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE videoId = :videoId OR (url = :url AND :url != ''))")
    suspend fun isFavorite(videoId: String, url: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteTrack(track: FavoriteTrackEntity)

    @Query("DELETE FROM favorite_tracks WHERE videoId = :videoId OR (url = :url AND :url != '')")
    suspend fun deleteFavoriteTrack(videoId: String, url: String)

    @Query("DELETE FROM favorite_tracks WHERE videoId = :trackId OR url = :mediaUri")
    suspend fun deleteFavoriteTrackFromAll(trackId: String, mediaUri: String)
}
