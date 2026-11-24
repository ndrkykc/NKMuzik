package com.nkapps.nkmuzik.data.repository

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import com.nkapps.nkmuzik.data.db.dao.PlaylistDao
import com.nkapps.nkmuzik.data.model.LocalAudioTrack
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.data.preferences.DeletedTracksStore
import com.nkapps.nkmuzik.data.scanner.LocalMediaScanner
import com.nkapps.nkmuzik.downloader.DownloadRepository
import com.nkapps.nkmuzik.utils.MediaTitleParser
import dagger.hilt.android.qualifiers.ApplicationContext
import com.nkapps.nkmuzik.ui.search.matchesSearchQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMediaRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaScanner: LocalMediaScanner,
    private val playlistDao: PlaylistDao,
    private val deletedTracksStore: DeletedTracksStore,
    private val downloadRepository: DownloadRepository
) : LocalMediaRepository {

    override fun getLocalAudioTracks(): Flow<List<LocalAudioTrack>> = flow {
        emit(mediaScanner.scanAudioFiles())
    }

    override suspend fun scanLocalMedia(): List<LocalAudioTrack> {
        return mediaScanner.scanAudioFiles()
    }

    override fun searchTracks(query: String): Flow<List<OnlineSearchResultDto>> = flow {
        val audioTracks = mediaScanner.scanAudioFiles()
        val filtered = if (query.isBlank()) {
            audioTracks
        } else {
            audioTracks.filter { track ->
                track.title.matchesSearchQuery(query) ||
                track.artist.matchesSearchQuery(query) ||
                track.album.matchesSearchQuery(query)
            }
        }
        val dtos = filtered.map { audioTrack ->
            val parsed = MediaTitleParser.parse(audioTrack.title, audioTrack.artist)
            OnlineSearchResultDto(
                videoId = audioTrack.id.toString(),
                title = parsed.title,
                uploaderName = parsed.artist,
                durationString = audioTrack.durationFormatted,
                thumbnailUrl = audioTrack.artworkUri?.toString() ?: "",
                url = audioTrack.contentUri.toString()
            )
        }
        emit(dtos)
    }

    override suspend fun deleteTrack(track: LocalAudioTrack): Boolean {
        return try {
            val contentUriStr = track.contentUri.toString()

            downloadRepository.clearDownloadState(track.id.toString(), track.title)

            deletedTracksStore.addDeletedTrack(
                trackId = track.id.toString(),
                filePath = track.filePath,
                contentUri = contentUriStr
            )

            if (contentUriStr.startsWith("content://")) {
                try {
                    context.contentResolver.delete(Uri.parse(contentUriStr), null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            track.filePath?.let { path ->
                if (path.isNotBlank()) {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                    try {
                        MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            deleteArtworkFiles(track.id.toString(), track.title, track.artworkUri)

            playlistDao.deleteTrackFromAllPlaylists(track.id.toString(), contentUriStr)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun deleteArtworkFiles(trackId: String, title: String?, artworkUri: Uri?) {
        try {
            val candidates = mutableListOf("art_$trackId.jpg", "art_$trackId.png", "$trackId.jpg", "$trackId.png")
            if (!title.isNullOrBlank()) {
                val sanitized = title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                candidates.add("$sanitized.jpg")
                candidates.add("$sanitized.png")
            }
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val dirs = listOfNotNull(
                File(musicDir, "NK Müzik"),
                File(musicDir, "NK Muzik")
            )
            for (dir in dirs) {
                if (!dir.exists()) continue
                for (candidate in candidates) {
                    val f = File(dir, candidate)
                    if (f.exists()) f.delete()
                }
            }
            if (artworkUri?.scheme == "file") {
                artworkUri.path?.let { path ->
                    val f = File(path)
                    if (f.exists()) f.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}