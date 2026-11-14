package com.nkapps.nkmuzik.data.repository

import android.content.Context
import android.net.Uri
import com.nkapps.nkmuzik.data.db.dao.PlaylistDao
import com.nkapps.nkmuzik.data.db.entity.FavoriteTrackEntity
import com.nkapps.nkmuzik.data.db.entity.PlaylistEntity
import com.nkapps.nkmuzik.data.db.entity.PlaylistTrackEntity
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.data.model.LocalTrack
import com.nkapps.nkmuzik.utils.ArtworkUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    @param:ApplicationContext private val context: Context
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<PlaylistEntity>> {
        return playlistDao.getAllPlaylists()
    }

    override fun getPlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>> {
        return playlistDao.getTracksForPlaylist(playlistId).map { tracks ->
            tracks.map { track ->
                val resolvedLocal = ArtworkUtils.extractEmbeddedArtwork(context, track.mediaUri.removePrefix("file://"), if (track.mediaUri.startsWith("content://")) Uri.parse(track.mediaUri) else null)
                    ?: ArtworkUtils.findArtworkForTrack(context, track.title, track.trackId)
                if (resolvedLocal != null) {
                    track.copy(artworkUri = resolvedLocal.toString())
                } else if (track.artworkUri.isNullOrBlank() || (track.artworkUri.startsWith("file:/") && !isArtworkFileValid(track.artworkUri))) {
                    val fallback = ArtworkUtils.getDirectOnlineThumbnailUrl(track.trackId)
                    if (fallback.isNotBlank()) track.copy(artworkUri = fallback) else track
                } else track
            }
        }
    }

    private fun isArtworkFileValid(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            val path = uri.path
            if (!path.isNullOrBlank()) {
                val file = File(path)
                file.exists() && file.length() > 0
            } else false
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun updatePlaylistName(playlistId: Long, newName: String) {
        playlistDao.updatePlaylistName(playlistId, newName)
    }

    override suspend fun addTrackToPlaylist(playlistId: Long, track: OnlineSearchResultDto) {
        val localArt = ArtworkUtils.findArtworkForTrack(context, track.title, track.videoId)?.toString()
        val resolvedArt = localArt
            ?: if (track.thumbnailUrl.isNotBlank()) track.thumbnailUrl
            else ArtworkUtils.getDirectOnlineThumbnailUrl(track.videoId)

        val entity = PlaylistTrackEntity(
            playlistId = playlistId,
            trackId = track.videoId,
            title = track.title,
            artist = track.uploaderName,
            duration = track.durationString,
            artworkUri = resolvedArt,
            mediaUri = track.url,
            isLocal = false
        )
        playlistDao.insertPlaylistTrack(entity)
    }

    override suspend fun addLocalTrackToPlaylist(playlistId: Long, track: LocalTrack) {
        val localArt = ArtworkUtils.extractEmbeddedArtwork(context, track.filePath, if (track.uri.startsWith("content://")) Uri.parse(track.uri) else null)?.toString()
            ?: ArtworkUtils.findArtworkForTrack(context, track.title, track.id.toString())?.toString()
        val resolvedArt = localArt
            ?: if (!track.artworkUri.isNullOrBlank()) track.artworkUri
            else ArtworkUtils.getDirectOnlineThumbnailUrl(track.id.toString())

        val entity = PlaylistTrackEntity(
            playlistId = playlistId,
            trackId = track.id.toString(),
            title = track.title,
            artist = track.artist,
            duration = track.duration,
            artworkUri = resolvedArt,
            mediaUri = track.uri,
            isLocal = !track.isOnlineStream && !track.uri.startsWith("http://") && !track.uri.startsWith("https://")
        )
        playlistDao.insertPlaylistTrack(entity)
    }

    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    override suspend fun deleteTrackFromAllPlaylists(trackId: String, mediaUri: String) {
        playlistDao.deleteTrackFromAllPlaylists(trackId, mediaUri)
        playlistDao.deleteFavoriteTrackFromAll(trackId, mediaUri)
    }

    override fun getFavoriteTracks(): Flow<List<OnlineSearchResultDto>> {
        return playlistDao.getFavoriteTracks().map { list ->
            list.map { entity ->
                OnlineSearchResultDto(
                    videoId = entity.videoId,
                    title = entity.title,
                    uploaderName = entity.uploaderName,
                    durationString = entity.durationString,
                    thumbnailUrl = entity.thumbnailUrl,
                    url = entity.url
                )
            }
        }
    }

    override suspend fun toggleFavorite(track: OnlineSearchResultDto) {
        val key = if (track.videoId.isNotBlank()) track.videoId else track.url
        if (key.isBlank()) return
        val isFav = playlistDao.isFavorite(track.videoId, track.url)
        if (isFav) {
            playlistDao.deleteFavoriteTrack(track.videoId, track.url)
        } else {
            val entity = FavoriteTrackEntity(
                videoId = key,
                title = track.title,
                uploaderName = track.uploaderName,
                durationString = track.durationString,
                thumbnailUrl = track.thumbnailUrl,
                url = track.url
            )
            playlistDao.insertFavoriteTrack(entity)
        }
    }

    override suspend fun isFavorite(videoId: String, url: String): Boolean {
        if (videoId.isBlank() && url.isBlank()) return false
        return playlistDao.isFavorite(videoId, url)
    }

    override suspend fun addFavorite(track: OnlineSearchResultDto) {
        val key = if (track.videoId.isNotBlank()) track.videoId else track.url
        if (key.isBlank()) return
        val entity = FavoriteTrackEntity(
            videoId = key,
            title = track.title,
            uploaderName = track.uploaderName,
            durationString = track.durationString,
            thumbnailUrl = track.thumbnailUrl,
            url = track.url
        )
        playlistDao.insertFavoriteTrack(entity)
    }

    override suspend fun removeFavorite(videoId: String, url: String) {
        if (videoId.isBlank() && url.isBlank()) return
        playlistDao.deleteFavoriteTrack(videoId, url)
    }
}
