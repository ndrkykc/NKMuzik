package com.nkapps.nkmuzik.ui.library

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.nkapps.nkmuzik.data.db.entity.PlaylistEntity
import com.nkapps.nkmuzik.data.model.LocalTrack
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.data.preferences.DeletedTracksStore
import com.nkapps.nkmuzik.data.repository.LocalMediaRepository
import com.nkapps.nkmuzik.data.repository.PlaylistRepository
import com.nkapps.nkmuzik.downloader.DownloadRepository
import com.nkapps.nkmuzik.utils.ArtworkUtils
import com.nkapps.nkmuzik.utils.MediaTitleParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val localMediaRepository: LocalMediaRepository,
    private val deletedTracksStore: DeletedTracksStore,
    private val playlistRepository: PlaylistRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _tracks = MutableStateFlow<List<LocalTrack>>(emptyList())
    val tracks: StateFlow<List<LocalTrack>> = _tracks.asStateFlow()

    private val _downloadedFiles = MutableStateFlow<List<LocalTrack>>(emptyList())
    val downloadedFiles: StateFlow<List<LocalTrack>> = _downloadedFiles.asStateFlow()

    private val _activeDownloads = MutableStateFlow<List<WorkInfo>>(emptyList())
    val activeDownloads: StateFlow<List<WorkInfo>> = _activeDownloads.asStateFlow()

    val favoriteTracks: StateFlow<List<OnlineSearchResultDto>> = playlistRepository.getFavoriteTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = playlistRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadLocalTracks()
        loadDownloadedFiles()
        observeWorkManager()
    }

    fun loadLocalTracks() {
        viewModelScope.launch {
            try {
                val scanned = localMediaRepository.scanLocalMedia()
                val mapped = scanned.map { audioTrack ->
                    LocalTrack(
                        id = audioTrack.id,
                        title = audioTrack.title,
                        artist = audioTrack.artist,
                        album = if (audioTrack.album.isNotBlank()) audioTrack.album else "Bilinmeyen Albüm",
                        duration = audioTrack.durationFormatted,
                        uri = audioTrack.contentUri.toString(),
                        artworkUri = audioTrack.artworkUri?.toString(),
                        filePath = audioTrack.filePath
                    )
                }
                _tracks.value = mapped
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadDownloadedFiles() {
        viewModelScope.launch {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val appDirs = listOfNotNull(musicDir, File(musicDir, "NK Müzik"), File(musicDir, "NK Muzik"))
            val list = mutableListOf<LocalTrack>()
            for (appDir in appDirs) {
                if (appDir.exists() && appDir.isDirectory) {
                    val deletedIds = deletedTracksStore.getDeletedTrackIds()
                    val deletedPaths = deletedTracksStore.getDeletedFilePaths()

                    appDir.listFiles { file -> file.extension.equals("m4a", true) || file.extension.equals("webm", true) || file.extension.equals("mp3", true) }?.forEach { file ->
                        if (file.exists() && !deletedTracksStore.isDeleted(
                                trackId = file.hashCode().toLong().toString(),
                                filePath = file.absolutePath,
                                contentUri = Uri.fromFile(file).toString(),
                                deletedIds = deletedIds,
                                deletedPaths = deletedPaths
                            )
                        ) {
                            var durationStr = "0:00"
                            try {
                                val retriever = MediaMetadataRetriever()
                                retriever.setDataSource(file.absolutePath)
                                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                                retriever.release()
                                if (dur > 0) {
                                    val totalSeconds = dur / 1000
                                    val minutes = totalSeconds / 60
                                    val seconds = totalSeconds % 60
                                    durationStr = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
                                }
                            } catch (_: Exception) {}

                            val parsed = MediaTitleParser.parse(file.nameWithoutExtension, null)
                            val artworkUriStr = ArtworkUtils.extractEmbeddedArtwork(context, file.absolutePath)?.toString()
                                ?: ArtworkUtils.findArtworkForTrack(context, file.nameWithoutExtension)?.toString()
                            val extractedVideoId = artworkUriStr?.let { art ->
                                val regex = Regex("(?:art_|vi/)([a-zA-Z0-9_-]{11})")
                                regex.find(art)?.groupValues?.get(1)
                            }
                            list.add(
                                LocalTrack(
                                    id = file.hashCode().toLong(),
                                    title = parsed.title,
                                    artist = parsed.artist,
                                    album = "İndirilenler",
                                    duration = durationStr,
                                    uri = Uri.fromFile(file).toString(),
                                    artworkUri = artworkUriStr,
                                    filePath = file.absolutePath,
                                    videoId = extractedVideoId
                                )
                            )
                        }
                    }
                }
            }
            _downloadedFiles.value = list
        }
    }

    private fun observeWorkManager() {
        val workManager = WorkManager.getInstance(context)
        viewModelScope.launch {
            workManager.getWorkInfosLiveData(
                WorkQuery.fromStates(
                    WorkInfo.State.RUNNING,
                    WorkInfo.State.ENQUEUED
                )
            ).asFlow().collect { workInfos ->
                _activeDownloads.value = workInfos.filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                loadDownloadedFiles()
            }
        }
    }

    fun deleteTrack(track: LocalTrack) {
        viewModelScope.launch {
            try {
                downloadRepository.clearDownloadState(track.id.toString(), track.title)

                deletedTracksStore.addDeletedTrack(
                    trackId = track.id.toString(),
                    filePath = track.filePath,
                    contentUri = track.uri
                )

                track.filePath?.let { path ->
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

                if (track.uri.startsWith("content://")) {
                    try {
                        context.contentResolver.delete(track.uri.toUri(), null, null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                playlistRepository.deleteTrackFromAllPlaylists(track.id.toString(), track.uri)

                _tracks.value = _tracks.value.filter { it.id != track.id && it.uri != track.uri && (track.filePath == null || it.filePath != track.filePath) }
                _downloadedFiles.value = _downloadedFiles.value.filter { it.id != track.id && it.uri != track.uri && (track.filePath == null || it.filePath != track.filePath) }

                loadLocalTracks()
                loadDownloadedFiles()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
