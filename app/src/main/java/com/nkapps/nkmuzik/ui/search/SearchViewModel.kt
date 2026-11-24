package com.nkapps.nkmuzik.ui.search

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.nkapps.nkmuzik.data.analytics.AnalyticsManager
import com.nkapps.nkmuzik.data.db.entity.PlaylistEntity
import com.nkapps.nkmuzik.data.db.entity.PlaylistTrackEntity
import com.nkapps.nkmuzik.data.model.LocalTrack
import com.nkapps.nkmuzik.data.model.LyricsData
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.utils.MediaTitleParser
import android.media.MediaScannerConnection
import android.util.Log
import androidx.media3.common.Timeline
import com.nkapps.nkmuzik.data.preferences.DeletedTracksStore
import com.nkapps.nkmuzik.data.preferences.ThemePreferences
import com.nkapps.nkmuzik.utils.LanguageManager
import com.nkapps.nkmuzik.data.repository.LocalMediaRepository
import com.nkapps.nkmuzik.data.repository.LyricsRepository
import com.nkapps.nkmuzik.data.repository.PlaylistRepository
import com.nkapps.nkmuzik.data.repository.OnlineSearchRepository
import com.nkapps.nkmuzik.downloader.DownloadProgressInfo
import com.nkapps.nkmuzik.downloader.DownloadRepository
import com.nkapps.nkmuzik.downloader.DownloadStatus
import com.nkapps.nkmuzik.utils.ArtworkUtils
import com.nkapps.nkmuzik.player.AudioFxManager
import com.nkapps.nkmuzik.player.AudioFxState
import com.nkapps.nkmuzik.player.PlaybackService
import com.nkapps.nkmuzik.player.SleepTimerController
import com.nkapps.nkmuzik.player.SleepTimerMode
import com.nkapps.nkmuzik.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

fun String.toTurkishLowercase(): String {
    return this.lowercase(Locale.forLanguageTag("tr-TR"))
}

fun String.toNormalizedSearchQuery(): String {
    return this.toTurkishLowercase()
        .replace('ı', 'i')
        .replace('ç', 'c')
        .replace('ş', 's')
        .replace('ğ', 'g')
        .replace('ü', 'u')
        .replace('ö', 'o')
}

fun String.matchesSearchQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val trLowerSelf = this.toTurkishLowercase()
    val trLowerQuery = query.toTurkishLowercase()
    if (trLowerSelf.contains(trLowerQuery)) return true

    val normSelf = this.toNormalizedSearchQuery()
    val normQuery = query.toNormalizedSearchQuery()
    return normSelf.contains(normQuery)
}

fun isFieldMatch(field: String?, query: String): Boolean {
    if (field.isNullOrBlank() || query.isBlank()) return false
    return field.matchesSearchQuery(query)
}

fun calculateLocalTrackSearchScore(track: LocalTrack, query: String): Int {
    if (query.isBlank()) return 0
    var score = 0
    if (isFieldMatch(track.title, query)) score += 100
    if (isFieldMatch(track.artist, query)) score += 60
    if (isFieldMatch(track.album, query)) score += 30
    val path = track.filePath ?: track.uri
    if (isFieldMatch(path, query)) score += 10
    return score
}

fun calculateOnlineTrackSearchScore(
    track: OnlineSearchResultDto,
    query: String,
    localTracks: List<LocalTrack> = emptyList()
): Int {
    if (query.isBlank()) return 0
    var score = 0
    val matchingLocal = localTracks.find {
        it.id.toString() == track.videoId ||
        it.uri == track.url ||
        (it.videoId != null && it.videoId == track.videoId)
    }

    val songTitle = track.title
    val artistName = track.uploaderName.ifBlank { matchingLocal?.artist ?: "" }
    val albumName = matchingLocal?.album ?: track.description
    val folderPath = matchingLocal?.filePath ?: track.url

    if (isFieldMatch(songTitle, query)) score += 100
    if (isFieldMatch(artistName, query)) score += 60
    if (isFieldMatch(albumName, query)) score += 30
    if (isFieldMatch(folderPath, query)) score += 10
    return score
}

fun calculatePlaylistTrackSearchScore(
    track: PlaylistTrackEntity,
    query: String,
    localTracks: List<LocalTrack> = emptyList()
): Int {
    if (query.isBlank()) return 0
    var score = 0
    val matchingLocal = localTracks.find {
        it.id.toString() == track.trackId ||
        it.uri == track.mediaUri ||
        (it.videoId != null && it.videoId == track.trackId)
    }

    val songTitle = track.title
    val artistName = track.artist.ifBlank { matchingLocal?.artist ?: "" }
    val albumName = matchingLocal?.album ?: ""
    val folderPath = matchingLocal?.filePath ?: track.mediaUri

    if (isFieldMatch(songTitle, query)) score += 100
    if (isFieldMatch(artistName, query)) score += 60
    if (isFieldMatch(albumName, query)) score += 30
    if (isFieldMatch(folderPath, query)) score += 10
    return score
}

data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tracks: List<OnlineSearchResultDto> = emptyList()
)

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val results: List<OnlineSearchResultDto>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val searchRepository: OnlineSearchRepository,
    private val downloadRepository: DownloadRepository,
    private val playlistRepository: PlaylistRepository,
    private val lyricsRepository: LyricsRepository,
    private val audioFxManager: AudioFxManager,
    private val sleepTimerController: SleepTimerController,
    private val themePreferences: ThemePreferences,
    private val localMediaRepository: LocalMediaRepository,
    private val deletedTracksStore: DeletedTracksStore,
    private val analyticsManager: AnalyticsManager,
    private val searchLocalTracksUseCase: com.nkapps.nkmuzik.domain.usecase.SearchLocalTracksUseCase,
    private val searchOnlineTracksUseCase: com.nkapps.nkmuzik.domain.usecase.SearchOnlineTracksUseCase,
    private val getSearchSuggestionsUseCase: com.nkapps.nkmuzik.domain.usecase.GetSearchSuggestionsUseCase,
    private val rankSearchResultsUseCase: com.nkapps.nkmuzik.domain.usecase.RankSearchResultsUseCase,
    private val networkConnectivityObserver: com.nkapps.nkmuzik.utils.NetworkConnectivityObserver
) : AndroidViewModel(context as Application) {

    val enableOnlineFeatures: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
    val onlineTabLabel: StateFlow<String> = MutableStateFlow("YT Online").asStateFlow()
    val isOnlineSearchSource: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    private var cachedDeletedIds = emptySet<String>()
    private var cachedDeletedPaths = emptySet<String>()

    private val activeDownloadDtos = ConcurrentHashMap<String, OnlineSearchResultDto>()

    private val _localTracks = MutableStateFlow<List<LocalTrack>>(emptyList())
    val localTracks: StateFlow<List<LocalTrack>> = _localTracks.asStateFlow()

    val downloadStates: StateFlow<Map<String, DownloadProgressInfo>> = downloadRepository.downloadStates

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), themePreferences.initialThemeMode)

    val dynamicColor: StateFlow<Boolean> = themePreferences.dynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isCoverDynamicColorEnabled: StateFlow<Boolean> = themePreferences.isCoverDynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isGridViewMode: StateFlow<Boolean> = themePreferences.isGridViewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val rememberSearchHistory: StateFlow<Boolean> = themePreferences.rememberSearchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val searchHistory: StateFlow<List<String>> = themePreferences.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setGridViewMode(isGrid: Boolean) {
        viewModelScope.launch {
            themePreferences.setGridViewMode(isGrid)
        }
    }

    fun setRememberSearchHistory(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setRememberSearchHistory(enabled)
        }
    }

    fun addSearchHistoryQuery(query: String) {
        if (rememberSearchHistory.value && query.isNotBlank()) {
            viewModelScope.launch {
                themePreferences.addSearchHistoryQuery(query)
            }
        }
    }

    fun removeSearchHistoryQuery(query: String) {
        viewModelScope.launch {
            themePreferences.removeSearchHistoryQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            themePreferences.clearSearchHistory()
        }
    }

    val language: StateFlow<String> = themePreferences.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LanguageManager.getInitialLanguage())

    val audioFxState: StateFlow<AudioFxState> = audioFxManager.fxState
    val sleepTimerMode: StateFlow<SleepTimerMode> = sleepTimerController.timerMode
    val sleepTimerFormatted: StateFlow<String?> = sleepTimerController.formattedRemainingTime

    private val _lyricsData = MutableStateFlow(LyricsData())
    val lyricsData: StateFlow<LyricsData> = _lyricsData.asStateFlow()

    private val _isFetchingLyrics = MutableStateFlow(false)
    val isFetchingLyrics: StateFlow<Boolean> = _isFetchingLyrics.asStateFlow()

    private val _onlineQuery = MutableStateFlow("")
    val onlineQuery: StateFlow<String> = _onlineQuery.asStateFlow()

    private val _searchCompletions = MutableStateFlow<List<String>>(emptyList())
    val searchCompletions: StateFlow<List<String>> = _searchCompletions.asStateFlow()

    private val _showSuggestions = MutableStateFlow(false)
    val showSuggestions: StateFlow<Boolean> = _showSuggestions.asStateFlow()

    private val _onlineResults = MutableStateFlow<List<OnlineSearchResultDto>>(emptyList())
    val onlineResults: StateFlow<List<OnlineSearchResultDto>> = _onlineResults.asStateFlow()

    private val _isSearchingOnline = MutableStateFlow(false)
    val isSearchingOnline: StateFlow<Boolean> = _isSearchingOnline.asStateFlow()

    val searchQuery: StateFlow<String> = _onlineQuery.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _selectedTrack = MutableStateFlow<OnlineSearchResultDto?>(null)
    val selectedTrack: StateFlow<OnlineSearchResultDto?> = _selectedTrack.asStateFlow()

    private val _currentPlayingTrack = MutableStateFlow<OnlineSearchResultDto?>(null)
    val currentPlayingTrack: StateFlow<OnlineSearchResultDto?> = _currentPlayingTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _deleteIntentSender = MutableStateFlow<IntentSenderRequest?>(null)
    val deleteIntentSender: StateFlow<IntentSenderRequest?> = _deleteIntentSender.asStateFlow()

    fun clearDeleteIntentSender() {
        _deleteIntentSender.value = null
    }

    private val _editIntentSender = MutableStateFlow<IntentSenderRequest?>(null)
    val editIntentSender: StateFlow<IntentSenderRequest?> = _editIntentSender.asStateFlow()

    fun clearEditIntentSender() {
        _editIntentSender.value = null
    }

    data class PendingEditTrackData(
        val track: LocalTrack,
        val newTitle: String,
        val newArtist: String,
        val newAlbum: String,
        val newArtworkUri: Uri?,
        val onComplete: (() -> Unit)?
    )

    private var pendingEditTrackData: PendingEditTrackData? = null

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _queueItems = MutableStateFlow<List<OnlineSearchResultDto>>(emptyList())
    val queueItems: StateFlow<List<OnlineSearchResultDto>> = _queueItems.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val favorites: StateFlow<List<OnlineSearchResultDto>> = playlistRepository.getFavoriteTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _recentTracks = MutableStateFlow<List<OnlineSearchResultDto>>(emptyList())
    val recentTracks: StateFlow<List<OnlineSearchResultDto>> = _recentTracks.asStateFlow()

    private val _selectedTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTrackIds: StateFlow<Set<Long>> = _selectedTrackIds.asStateFlow()

    private val _selectedFavoriteVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedFavoriteVideoIds: StateFlow<Set<String>> = _selectedFavoriteVideoIds.asStateFlow()

    private val _playCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val playCounts: StateFlow<Map<String, Int>> = _playCounts.asStateFlow()

    val dbPlaylists: StateFlow<List<PlaylistEntity>> = playlistRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private var mediaController: MediaController? = null
    private val controllerFuture: ListenableFuture<MediaController>
    private var queueInsertionCounter = 0

    fun updateQueueFromController() {
        val controller = mediaController ?: return
        val count = controller.mediaItemCount
        val items = mutableListOf<OnlineSearchResultDto>()
        for (i in 0 until count) {
            val item = controller.getMediaItemAt(i)
            val meta = item.mediaMetadata
            val videoId = item.mediaId
            val rawTitle = meta.title?.toString()
            val rawArtist = meta.artist?.toString()
            val parsed = MediaTitleParser.parse(rawTitle, rawArtist)
            val title = parsed.title
            val artist = parsed.artist
            val rawArt = meta.artworkUri?.toString()
            val resolvedArt = ArtworkUtils.getArtworkModel(context, rawArt, title, videoId)?.data?.toString()
                ?: if (videoId.isNotBlank()) ArtworkUtils.getDirectOnlineThumbnailUrl(videoId) else ""

            val dto = OnlineSearchResultDto(
                videoId = videoId,
                title = title,
                uploaderName = artist,
                durationString = "",
                thumbnailUrl = resolvedArt,
                url = item.localConfiguration?.uri?.toString() ?: ""
            )
            items.add(dto)
        }
        _queueItems.value = items
        _currentQueueIndex.value = controller.currentMediaItemIndex
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (!isPlaying) {
                saveLastPlayedTrackInfo()
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            updateQueueFromController()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            queueInsertionCounter = 0
            updateQueueFromController()
            mediaItem?.let { item ->
                val meta = item.mediaMetadata
                val videoId = item.mediaId
                val rawTitle = meta.title?.toString()
                val rawArtist = meta.artist?.toString()
                val parsed = MediaTitleParser.parse(rawTitle, rawArtist)
                val title = parsed.title
                val artist = parsed.artist
                val rawArt = meta.artworkUri?.toString()
                val resolvedArt = ArtworkUtils.getArtworkModel(context, rawArt, title, videoId)?.data?.toString()
                    ?: if (videoId.isNotBlank()) ArtworkUtils.getDirectOnlineThumbnailUrl(videoId) else ""

                val currentUri = item.localConfiguration?.uri?.toString() ?: ""
                val dto = OnlineSearchResultDto(
                    videoId = videoId,
                    title = title,
                    uploaderName = artist,
                    durationString = "",
                    thumbnailUrl = resolvedArt,
                    url = currentUri
                )
                setCurrentPlayingTrackInternal(dto)

                // Lazy resolution for online queue items that do not have direct stream URLs yet
                val isLocal = currentUri.startsWith("content://") || currentUri.startsWith("file://") || currentUri.startsWith("/")
                if (!isLocal && videoId.isNotBlank() && (currentUri.contains("youtube.com/watch") || currentUri.isBlank())) {
                    viewModelScope.launch {
                        val streamRes = searchRepository.getAudioStreamUrl(videoId)
                        val streamInfo = streamRes.getOrNull()
                        if (streamInfo != null && streamInfo.audioUrl.isNotBlank()) {
                            mediaController?.let { controller ->
                                val currentIndex = controller.currentMediaItemIndex
                                if (currentIndex >= 0 && currentIndex < controller.mediaItemCount) {
                                    val updatedMediaItem = buildMediaItemForTrackFast(
                                        OnlineSearchResultDto(
                                            videoId = videoId,
                                            title = title,
                                            uploaderName = artist,
                                            durationString = "",
                                            thumbnailUrl = resolvedArt,
                                            url = streamInfo.audioUrl
                                        ),
                                        resolvedAudioUrl = streamInfo.audioUrl
                                    )
                                    val currentPos = controller.currentPosition
                                    val wasPlaying = controller.isPlaying
                                    controller.replaceMediaItem(currentIndex, updatedMediaItem)
                                    controller.seekTo(currentIndex, currentPos)
                                    if (wasPlaying) {
                                        controller.play()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _isShuffleEnabled.value = shuffleModeEnabled
            updateQueueFromController()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
            updateQueueFromController()
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e("SearchViewModel", "ExoPlayer error: ${error.errorCodeName} (${error.errorCode})", error)
            mediaController?.currentMediaItem?.let { item ->
                val videoId = item.mediaId
                if (videoId.isNotBlank()) {
                    val title = item.mediaMetadata.title?.toString() ?: ""
                    val artist = item.mediaMetadata.artist?.toString() ?: ""
                    viewModelScope.launch {
                        val freshRes = searchRepository.getAudioStreamUrl(videoId, forceRefresh = true)
                        val freshInfo = freshRes.getOrNull()
                        if (freshInfo != null && freshInfo.audioUrl.isNotBlank()) {
                            mediaController?.let { controller ->
                                val index = controller.currentMediaItemIndex
                                val updatedMediaItem = buildMediaItemForTrackFast(
                                    OnlineSearchResultDto(
                                        videoId = videoId,
                                        title = title,
                                        uploaderName = artist,
                                        durationString = "",
                                        thumbnailUrl = "",
                                        url = freshInfo.audioUrl
                                    ),
                                    resolvedAudioUrl = freshInfo.audioUrl
                                )
                                controller.replaceMediaItem(index, updatedMediaItem)
                                controller.seekTo(index, 0L)
                                controller.prepare()
                                controller.play()
                                return@launch
                            }
                        }
                        _toastMessage.value = "Oynatma hatası: ${error.errorCodeName}"
                    }
                    return
                }
            }
            _toastMessage.value = "Oynatma hatası: ${error.errorCodeName}"
        }
    }

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                val controller = controllerFuture.get()
                mediaController = controller
                controller.addListener(playerListener)
                _isShuffleEnabled.value = controller.shuffleModeEnabled
                _repeatMode.value = controller.repeatMode
                updateQueueFromController()
                restorePlayerQueueOnStartup()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())

        viewModelScope.launch {
            while (true) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _currentPosition.value = controller.currentPosition
                        _duration.value = controller.duration.coerceAtLeast(0L)
                        _isPlaying.value = true
                    } else {
                        _isPlaying.value = controller.playbackState == Player.STATE_BUFFERING || controller.isPlaying
                    }
                }
                delay(200)
            }
        }

        viewModelScope.launch {
            themePreferences.lastPlayedTrack.firstOrNull()?.let { restored ->
                if (_currentPlayingTrack.value == null && restored.trackId.isNotBlank()) {
                    val parsed = MediaTitleParser.parse(restored.title, restored.artist)
                    val dto = OnlineSearchResultDto(
                        videoId = restored.trackId,
                        title = parsed.title,
                        uploaderName = parsed.artist,
                        durationString = "",
                        thumbnailUrl = restored.artworkUri,
                        url = restored.contentUri
                    )
                    _currentPlayingTrack.value = dto
                    _currentPosition.value = restored.position
                    restorePlayerQueueOnStartup()
                }
            }
        }

        viewModelScope.launch {
            deletedTracksStore.deletedTrackIds.collect { cachedDeletedIds = it }
        }
        viewModelScope.launch {
            deletedTracksStore.deletedFilePaths.collect { cachedDeletedPaths = it }
        }

        observeDownloadStates()
        loadLocalTracks()
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

                val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val appDirs = listOfNotNull(File(musicDir, "NK Müzik"), File(musicDir, "NK Muzik"))
                val downloadedList = mutableListOf<LocalTrack>()
                for (appDir in appDirs) {
                    if (appDir.exists() && appDir.isDirectory) {
                        val deletedIds = deletedTracksStore.getDeletedTrackIds()
                        val deletedPaths = deletedTracksStore.getDeletedFilePaths()

                        appDir.listFiles { file ->
                            file.extension.equals("m4a", true) ||
                                    file.extension.equals("webm", true) ||
                                    file.extension.equals("mp3", true)
                        }?.forEach { file ->
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

                                val artworkUriStr = ArtworkUtils.extractEmbeddedArtwork(context, file.absolutePath)?.toString()
                                    ?: ArtworkUtils.findArtworkForTrack(context, file.nameWithoutExtension)?.toString()
                                val parsed = MediaTitleParser.parse(file.nameWithoutExtension, null)
                                val extractedVideoId = artworkUriStr?.let { art ->
                                    val regex = Regex("(?:art_|vi/)([a-zA-Z0-9_-]{11})")
                                    regex.find(art)?.groupValues?.get(1)
                                }

                                downloadedList.add(
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

                val trackPaths = mapped.mapNotNull { it.filePath }.toSet()
                val uniqueDownloaded = downloadedList.filter { df ->
                    df.filePath == null || !trackPaths.contains(df.filePath)
                }

                val scannedList = mapped + uniqueDownloaded
                val scannedPaths = scannedList.mapNotNull { it.filePath }.toSet()
                val scannedVideoIds = scannedList.mapNotNull { it.videoId }.toSet()
                val scannedUris = scannedList.map { it.uri }.toSet()

                val preserved = _localTracks.value.filter { existing ->
                    (existing.album == "İndirilenler" || existing.videoId != null) &&
                            (existing.filePath == null || !scannedPaths.contains(existing.filePath)) &&
                            (existing.videoId == null || !scannedVideoIds.contains(existing.videoId)) &&
                            !scannedUris.contains(existing.uri)
                }

                _localTracks.value = preserved + scannedList
                restorePlayerQueueOnStartup()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observeDownloadStates() {
        viewModelScope.launch {
            var previousCompletedIds = downloadRepository.downloadStates.value
                .filter { it.value.status == DownloadStatus.COMPLETED }
                .keys.toSet()

            downloadRepository.downloadStates.collect { statesMap ->
                val currentCompleted = statesMap.filter { it.value.status == DownloadStatus.COMPLETED }
                for ((videoId, info) in currentCompleted) {
                    if (!previousCompletedIds.contains(videoId)) {
                        handleDownloadCompleted(videoId, info)
                    }
                }
                previousCompletedIds = currentCompleted.keys.toSet()
            }
        }
    }

    private fun handleDownloadCompleted(videoId: String, info: DownloadProgressInfo) {
        Log.d("SearchViewModel", "handleDownloadCompleted triggered for videoId=$videoId, title=${info.title}")

        // 1. Immediately call loadLocalTracks() to re-scan Music/NK Muzik storage and refresh local tracks
        loadLocalTracks()

        // 2. Add the downloaded track directly to _localTracks and _recentTracks StateFlow lists
        val cachedDto = activeDownloadDtos.remove(videoId)
            ?: _onlineResults.value.find { it.videoId == videoId }
            ?: _recentTracks.value.find { it.videoId == videoId }
            ?: favorites.value.find { it.videoId == videoId }
            ?: queueItems.value.find { it.videoId == videoId }

        val rawTitle = cachedDto?.title ?: info.title
        val rawArtist = cachedDto?.uploaderName ?: ""
        val parsed = MediaTitleParser.parse(rawTitle, rawArtist.ifBlank { null })

        val sanitizedTitle = rawTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val appDirs = listOfNotNull(musicDir, File(musicDir, "NK Müzik"), File(musicDir, "NK Muzik"))

        val formattedName = if (parsed.artist.isNotBlank() && !parsed.title.lowercase().startsWith(parsed.artist.lowercase())) {
            "${parsed.artist} - ${parsed.title}"
        } else {
            parsed.title
        }
        val formattedTitle = formattedName.replace(Regex("[\\\\/:*?\"<>|]"), "_")

        var downloadedFile: File? = null
        for (appDir in appDirs) {
            if (appDir.exists() && appDir.isDirectory) {
                val exts = listOf("m4a", "webm", "mp3")
                for (ext in exts) {
                    val candidate1 = File(appDir, "$formattedTitle.$ext")
                    val candidate2 = File(appDir, "$sanitizedTitle.$ext")
                    if (candidate1.exists()) {
                        downloadedFile = candidate1
                        break
                    } else if (candidate2.exists()) {
                        downloadedFile = candidate2
                        break
                    }
                }
            }
            if (downloadedFile != null) break
        }

        val actualFile = downloadedFile ?: return
        val targetFilePath = actualFile.absolutePath
        val targetUri = Uri.fromFile(actualFile).toString()
        val trackId = actualFile.hashCode().toLong()

        var durationStr = cachedDto?.durationString?.ifBlank { null } ?: "0:00"
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(actualFile.absolutePath)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()
            if (dur > 0) {
                val totalSeconds = dur / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                durationStr = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
            }
        } catch (_: Exception) {}

        val artworkUriStr = ArtworkUtils.extractEmbeddedArtwork(context, actualFile.absolutePath)?.toString()
            ?: ArtworkUtils.findArtworkForTrack(context, actualFile.nameWithoutExtension)?.toString()
            ?: cachedDto?.thumbnailUrl?.ifBlank { null }
            ?: ArtworkUtils.getDirectOnlineThumbnailUrl(videoId)

        val newLocalTrack = LocalTrack(
            id = trackId,
            title = parsed.title,
            artist = parsed.artist,
            album = "İndirilenler",
            duration = durationStr,
            uri = targetUri,
            artworkUri = artworkUriStr,
            filePath = targetFilePath,
            videoId = videoId
        )

        val currentLocal = _localTracks.value.toMutableList()
        val existingIdx = currentLocal.indexOfFirst {
            (it.videoId != null && it.videoId == videoId) ||
            it.id == newLocalTrack.id ||
            (it.filePath != null && it.filePath == newLocalTrack.filePath) ||
            (it.uri.isNotBlank() && it.uri == newLocalTrack.uri)
        }
        if (existingIdx >= 0) {
            currentLocal[existingIdx] = newLocalTrack
        } else {
            currentLocal.add(0, newLocalTrack)
        }
        _localTracks.value = currentLocal

        val dtoToRecent = (cachedDto ?: OnlineSearchResultDto(
            videoId = videoId,
            title = parsed.title,
            uploaderName = parsed.artist,
            durationString = durationStr,
            thumbnailUrl = artworkUriStr,
            url = targetUri
        )).copy(url = targetUri)

        val currentRecents = _recentTracks.value.toMutableList()
        currentRecents.removeAll { it.videoId == videoId || (it.url.isNotBlank() && it.url == targetUri) }
        currentRecents.add(0, dtoToRecent)
        if (currentRecents.size > 20) currentRecents.removeAt(currentRecents.size - 1)
        _recentTracks.value = currentRecents
    }

    fun isDownloaded(track: OnlineSearchResultDto): Boolean {
        if (track.videoId.isBlank()) return false

        // 1. Direct check in downloadStates by unique videoId
        val info = downloadStates.value[track.videoId]
        if (info?.status == DownloadStatus.COMPLETED) {
            return true
        }

        // 2. Direct physical disk check in Music directory and subdirectories
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val appDirs = listOfNotNull(musicDir, File(musicDir, "NK Müzik"), File(musicDir, "NK Muzik"))

        val parsed = MediaTitleParser.parse(track.title, track.uploaderName.ifBlank { null })
        val cleanTitle = parsed.title
        val cleanArtist = parsed.artist

        val parsedFormatted = if (cleanArtist.isNotBlank() && !cleanTitle.lowercase().startsWith(cleanArtist.lowercase())) {
            "$cleanArtist - $cleanTitle"
        } else {
            cleanTitle
        }
        val sanitizedParsedTitle = parsedFormatted.replace(Regex("[\\\\/:*?\"<>|]"), "_")

        val rawFormatted = if (track.uploaderName.isNotBlank() && !track.title.lowercase().startsWith(track.uploaderName.lowercase())) {
            "${track.uploaderName} - ${track.title}"
        } else {
            track.title
        }
        val sanitizedRawFormatted = rawFormatted.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val sanitizedRawTitle = track.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")

        val diskFileExists = appDirs.any { dir ->
            listOf("m4a", "webm", "mp3").any { ext ->
                File(dir, "$sanitizedParsedTitle.$ext").exists() ||
                File(dir, "$sanitizedRawFormatted.$ext").exists() ||
                File(dir, "$sanitizedRawTitle.$ext").exists() ||
                File(dir, "$cleanTitle.$ext").exists()
            }
        }
        if (diskFileExists) {
            return true
        }

        // 3. Check in localTracks by videoId, title, or file name
        val matchingLocal = _localTracks.value.find { local ->
            (local.videoId != null && local.videoId == track.videoId) ||
            local.id.toString() == track.videoId ||
            (local.uri.isNotBlank() && local.uri.contains(track.videoId)) ||
            local.title.equals(track.title, ignoreCase = true) ||
            local.title.equals(sanitizedParsedTitle, ignoreCase = true) ||
            local.title.equals(sanitizedRawTitle, ignoreCase = true) ||
            (local.filePath != null && (local.filePath.contains(sanitizedParsedTitle, ignoreCase = true) || local.filePath.contains(sanitizedRawTitle, ignoreCase = true)))
        }

        if (matchingLocal != null) {
            val path = matchingLocal.filePath
            val file = if (!path.isNullOrBlank()) File(path) else null
            val exists = file?.exists() == true || matchingLocal.uri.isNotBlank()
            val isDeleted = deletedTracksStore.isDeleted(
                trackId = matchingLocal.id.toString(),
                filePath = path,
                contentUri = matchingLocal.uri,
                deletedIds = cachedDeletedIds,
                deletedPaths = cachedDeletedPaths
            ) || deletedTracksStore.isDeleted(
                trackId = track.videoId,
                filePath = path,
                contentUri = track.url,
                deletedIds = cachedDeletedIds,
                deletedPaths = cachedDeletedPaths
            )

            if (exists && !isDeleted) {
                return true
            }
        }

        return false
    }

    fun getDownloadStatus(track: OnlineSearchResultDto): DownloadStatus? {
        if (track.videoId.isBlank()) return null

        val info = downloadStates.value[track.videoId]
        if (info?.status == DownloadStatus.DOWNLOADING) {
            return DownloadStatus.DOWNLOADING
        }
        if (info?.status == DownloadStatus.COMPLETED) {
            return DownloadStatus.COMPLETED
        }

        val downloaded = isDownloaded(track)
        if (downloaded) {
            return DownloadStatus.COMPLETED
        }
        return null
    }

    fun dtoToLocalTrack(dto: OnlineSearchResultDto): LocalTrack {
        return LocalTrack(
            id = dto.videoId.hashCode().toLong(),
            title = dto.title,
            artist = dto.uploaderName,
            album = "Çevrimiçi Yayın",
            duration = dto.durationString,
            uri = dto.url,
            artworkUri = dto.thumbnailUrl.ifBlank { null },
            filePath = null,
            videoId = dto.videoId,
            isOnlineStream = true
        )
    }

    fun localTrackToDto(localTrack: LocalTrack): OnlineSearchResultDto {
        val parsed = MediaTitleParser.parse(localTrack.title, localTrack.artist)
        return OnlineSearchResultDto(
            videoId = if (!localTrack.videoId.isNullOrBlank()) localTrack.videoId else localTrack.id.toString(),
            title = parsed.title,
            uploaderName = parsed.artist,
            durationString = localTrack.duration,
            thumbnailUrl = localTrack.artworkUri ?: "",
            url = localTrack.uri
        )
    }

    private fun setCurrentPlayingTrackInternal(track: OnlineSearchResultDto) {
        _currentPlayingTrack.value = track
        incrementPlayCount(track.videoId)
        loadLyricsForTrack(track)
        saveLastPlayedTrackInfo()
    }

    fun saveLastPlayedTrackInfo() {
        val track = _currentPlayingTrack.value ?: return
        val pos = mediaController?.currentPosition?.coerceAtLeast(0L) ?: _currentPosition.value
        viewModelScope.launch {
            themePreferences.saveLastPlayedTrack(
                trackId = track.videoId,
                title = track.title,
                artist = track.uploaderName,
                artworkUri = track.thumbnailUrl,
                contentUri = track.url,
                position = pos
            )
        }
    }

    fun loadLyricsForTrack(track: OnlineSearchResultDto) {
        analyticsManager.logFeatureUsed("lyrics")
        viewModelScope.launch {
            _isFetchingLyrics.value = true
            _lyricsData.value = LyricsData(plainText = "Şarkı sözleri yükleniyor...")
            val fetchedData = lyricsRepository.getLyrics(track.title, track.uploaderName)
            _lyricsData.value = fetchedData
            _isFetchingLyrics.value = false
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) = audioFxManager.setEqualizerEnabled(enabled)
    fun selectEqualizerPreset(presetIndex: Int) = audioFxManager.selectPreset(presetIndex)
    fun setEqualizerBandLevel(bandIndex: Short, levelMilliBel: Short) = audioFxManager.setBandLevel(bandIndex, levelMilliBel)
    fun setBassBoostEnabled(enabled: Boolean) = audioFxManager.setBassBoostEnabled(enabled)
    fun setBassBoostStrength(strength: Short) = audioFxManager.setBassBoostStrength(strength)

    fun startSleepTimer(minutes: Int) {
        analyticsManager.logFeatureUsed("sleep_timer")
        sleepTimerController.startTimer(minutes) {
            mediaController?.pause()
            _isPlaying.value = false
            _toastMessage.value = "Uyku Zamanlayıcısı: Müzik durduruldu."
        }
        _toastMessage.value = "$minutes dakika sonra uyku zamanlayıcı çalışacak"
    }

    fun setSleepTimerEndOfTrack() {
        analyticsManager.logFeatureUsed("sleep_timer")
        sleepTimerController.setEndOfTrackMode {
            mediaController?.pause()
            _isPlaying.value = false
            _toastMessage.value = "Uyku Zamanlayıcısı: Şarkı sonunda müzik durduruldu."
        }
        _toastMessage.value = "Şarkı sonunda müzik durdurulacak"
    }

    fun cancelSleepTimer() {
        sleepTimerController.cancelTimer()
        _toastMessage.value = "Uyku zamanlayıcı iptal edildi"
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setDynamicColor(enabled)
        }
    }

    fun setCoverDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setCoverDynamicColorEnabled(enabled)
        }
    }

    fun setLanguage(lang: String) {
        analyticsManager.logLanguageChanged(if (lang == "tr") "tr" else "en")
        viewModelScope.launch {
            themePreferences.setLanguage(lang)
            LanguageManager.setLanguage(lang)
        }
    }

    fun logUsageMode(mode: String) {
        analyticsManager.logUsageMode(mode)
    }

    fun logTrackPlay(mediaType: String, durationSeconds: Long) {
        analyticsManager.logTrackPlay(mediaType, durationSeconds)
    }

    fun logTrackDownload(title: String) {
        analyticsManager.logTrackDownload(title)
    }

    fun logFeatureUsed(featureName: String) {
        analyticsManager.logFeatureUsed(featureName)
    }

    fun logLanguageChanged(language: String) {
        analyticsManager.logLanguageChanged(language)
    }

    fun logFilterUsed(filterName: String) {
        analyticsManager.logFilterUsed(filterName)
    }

    fun logSortUsed(sortName: String) {
        analyticsManager.logSortUsed(sortName)
    }

    fun dismissSuggestions() {
        searchCompletionsJob?.cancel()
        _showSuggestions.value = false
        _searchCompletions.value = emptyList()
    }

    fun openSuggestions() {
        _showSuggestions.value = true
    }

    fun searchLocalTracks(
        query: String,
        tracks: List<LocalTrack> = _localTracks.value
    ): List<LocalTrack> {
        return searchLocalTracksUseCase.execute(tracks, query)
    }

    fun searchFavorites(
        query: String,
        favs: List<OnlineSearchResultDto> = favorites.value,
        localTracksList: List<LocalTrack> = _localTracks.value
    ): List<OnlineSearchResultDto> {
        return rankSearchResultsUseCase.rankOnline(favs, query, localTracksList)
    }

    fun searchRecents(
        query: String,
        recents: List<OnlineSearchResultDto> = _recentTracks.value,
        localTracksList: List<LocalTrack> = _localTracks.value
    ): List<OnlineSearchResultDto> {
        return rankSearchResultsUseCase.rankOnline(recents, query, localTracksList)
    }

    fun searchPlaylistTracks(
        query: String,
        playlistTracks: List<PlaylistTrackEntity>,
        localTracksList: List<LocalTrack> = _localTracks.value
    ): List<PlaylistTrackEntity> {
        if (query.isBlank()) return playlistTracks
        return playlistTracks
            .map { track -> track to calculatePlaylistTrackSearchScore(track, query, localTracksList) }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (track, _) -> track }
    }

    fun hideSuggestions() {
        dismissSuggestions()
    }

    private fun logDebug(tag: String, message: String) {
        try {
            Log.d(tag, message)
        } catch (_: Throwable) {
            // Ignored in unit test environment
        }
    }

    private var searchCompletionsJob: Job? = null

    fun onOnlineQueryChanged(query: String) {
        _onlineQuery.value = query
        searchCompletionsJob?.cancel()
        if (query.isBlank()) {
            _onlineResults.value = emptyList()
            _isSearchingOnline.value = false
            _uiState.value = SearchUiState.Idle
            logDebug("OnlineSearch", "SearchViewModel -> UI state transition: Idle")
            dismissSuggestions()
        } else {
            _showSuggestions.value = true
            searchCompletionsJob = viewModelScope.launch {
                val completions = getSearchSuggestionsUseCase.execute(query)
                if (_onlineQuery.value == query) {
                    _searchCompletions.value = completions
                    if (query.isNotBlank() && completions.isNotEmpty()) {
                        _showSuggestions.value = true
                    }
                }
            }
        }
    }

    fun selectCompletion(completion: String) {
        _onlineQuery.value = completion
        dismissSuggestions()
        performSearch(completion)
    }

    fun onQueryChanged(query: String) {
        onOnlineQueryChanged(query)
    }

    fun performSearch(query: String) {
        logDebug("OnlineSearch", "SearchViewModel -> executing search query: '$query'")
        dismissSuggestions()
        if (query.isNotBlank()) {
            addSearchHistoryQuery(query)
        }

        if (!networkConnectivityObserver.isCurrentlyConnected()) {
            _isSearchingOnline.value = false
            _onlineResults.value = emptyList()
            _uiState.value = SearchUiState.Error("İnternet bağlantısı yok")
            logDebug("OnlineSearch", "SearchViewModel -> UI state transition: Error(msg='İnternet bağlantısı yok')")
            return
        }

        _isSearchingOnline.value = true
        _uiState.value = SearchUiState.Loading
        logDebug("OnlineSearch", "SearchViewModel -> UI state transition: Loading")
        viewModelScope.launch {
            searchOnlineTracksUseCase.execute(query, _localTracks.value)
                .catch { e ->
                    _isSearchingOnline.value = false
                    _onlineResults.value = emptyList()
                    val errorMsg = e.localizedMessage ?: e.message ?: "Arama sırasında bir hata oluştu."
                    _uiState.value = SearchUiState.Error(errorMsg)
                    logDebug("OnlineSearch", "SearchViewModel -> UI state transition: Error(msg='$errorMsg')")
                    _toastMessage.value = errorMsg
                }
                .collect { results ->
                    _isSearchingOnline.value = false
                    if (results.isNotEmpty()) {
                        _onlineResults.value = results
                        _uiState.value = SearchUiState.Success(results)
                        logDebug("OnlineSearch", "SearchViewModel -> UI state transition: Success(count=${results.size})")
                    } else {
                        _onlineResults.value = emptyList()
                        val errorMsg = "Sonuç bulunamadı."
                        _uiState.value = SearchUiState.Error(errorMsg)
                        logDebug("OnlineSearch", "SearchViewModel -> UI state transition: Error(msg='$errorMsg')")
                        _toastMessage.value = errorMsg
                    }
                }
        }
    }

    fun selectTrack(track: OnlineSearchResultDto?) {
        dismissSuggestions()
        _selectedTrack.value = track
    }

    fun onPlayStreamClicked(track: OnlineSearchResultDto) {
        if (!enableOnlineFeatures.value) {
            _toastMessage.value = "Çevrimiçi dinleme şu an devre dışı."
            return
        }
        val favIndex = favorites.value.indexOfFirst { it.videoId == track.videoId || it.url == track.url }
        if (favIndex != -1) {
            playStreamQueue(favorites.value, favIndex)
            return
        }
        val onlineIndex = _onlineResults.value.indexOfFirst { it.videoId == track.videoId || it.url == track.url }
        if (onlineIndex != -1) {
            playStreamQueue(_onlineResults.value, onlineIndex)
            return
        }
        val recIndex = _recentTracks.value.indexOfFirst { it.videoId == track.videoId || it.url == track.url }
        if (recIndex != -1) {
            playStreamQueue(_recentTracks.value, recIndex)
            return
        }
        playStreamQueue(listOf(track), 0)
    }

    private fun getValidMediaUri(uriString: String, filePath: String? = null): Uri {
        if (uriString.startsWith("content://") || uriString.startsWith("file://") || uriString.startsWith("http://") || uriString.startsWith("https://")) {
            return Uri.parse(uriString)
        }
        if (!filePath.isNullOrBlank()) {
            val file = File(filePath)
            if (file.exists()) {
                return Uri.fromFile(file)
            }
        }
        if (uriString.startsWith("/")) {
            val file = File(uriString)
            if (file.exists()) {
                return Uri.fromFile(file)
            }
        }
        return Uri.parse(uriString)
    }

    private fun inferMimeType(
        uriString: String,
        filePath: String? = null,
        format: String? = null
    ): String? {
        val target = (filePath ?: uriString).lowercase()
        val lowerUrl = uriString.lowercase()
        val lowerFormat = format?.lowercase().orEmpty()

        return when {
            lowerFormat.contains("webm") || lowerFormat.contains("opus") ||
                    lowerUrl.contains("mime=audio%2fwebm") || lowerUrl.contains("mime=audio/webm") ||
                    target.endsWith(".webm") -> MimeTypes.AUDIO_WEBM

            lowerFormat.contains("mp3") || lowerFormat.contains("mpeg") ||
                    lowerUrl.contains("mime=audio%2fmpeg") || lowerUrl.contains("mime=audio/mp3") ||
                    target.endsWith(".mp3") -> MimeTypes.AUDIO_MPEG

            lowerFormat.contains("m4a") || lowerFormat.contains("mp4") ||
                    lowerUrl.contains("mime=audio%2fmp4") || lowerUrl.contains("mime=audio/mp4") ||
                    target.endsWith(".m4a") || target.endsWith(".mp4") -> MimeTypes.AUDIO_MP4

            target.endsWith(".ogg") || target.endsWith(".opus") -> MimeTypes.AUDIO_OGG
            target.endsWith(".aac") -> MimeTypes.AUDIO_AAC
            target.endsWith(".flac") -> MimeTypes.AUDIO_FLAC
            target.endsWith(".wav") -> MimeTypes.AUDIO_WAV

            uriString.startsWith("http://") || uriString.startsWith("https://") -> MimeTypes.AUDIO_MP4
            else -> null
        }
    }

    fun playLocalTrack(localTrack: LocalTrack) {
        val tracks = _localTracks.value
        val index = tracks.indexOfFirst { it.id == localTrack.id || it.uri == localTrack.uri }
        if (index != -1) {
            playLocalQueue(tracks, index)
        } else {
            playLocalQueue(listOf(localTrack), 0)
        }
    }

    fun addToQueue(localTrack: LocalTrack) {
        val mediaUri = getValidMediaUri(localTrack.uri, localTrack.filePath)
        val mimeType = inferMimeType(localTrack.uri, localTrack.filePath)

        val builder = MediaItem.Builder()
            .setUri(mediaUri)
            .setMediaId(localTrack.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(localTrack.title)
                    .setArtist(localTrack.artist)
                    .setArtworkUri(ArtworkUtils.parseArtworkUri(localTrack.artworkUri))
                    .build()
            )
        if (mimeType != null) {
            builder.setMimeType(mimeType)
        }

        val mediaItem = builder.build()

        mediaController?.let { controller ->
            if (controller.mediaItemCount > 0) {
                val currentIndex = controller.currentMediaItemIndex
                val insertIndex = (currentIndex + 1 + queueInsertionCounter).coerceAtMost(controller.mediaItemCount)
                controller.addMediaItem(insertIndex, mediaItem)
                queueInsertionCounter++
                _toastMessage.value = "${localTrack.title} sıraya eklendi"
            } else {
                queueInsertionCounter = 0
                playLocalTrack(localTrack)
            }
        } ?: run {
            queueInsertionCounter = 0
            playLocalTrack(localTrack)
        }
    }

    fun addToQueue(track: OnlineSearchResultDto) {
        viewModelScope.launch {
            searchRepository.getAudioStreamUrl(track.videoId).onSuccess { audioInfo ->
                val mediaItem = MediaItem.Builder()
                    .setUri(audioInfo.audioUrl)
                    .setMediaId(track.videoId)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.uploaderName)
                            .setArtworkUri(ArtworkUtils.parseArtworkUri(track.thumbnailUrl))
                            .build()
                    )
                    .build()

                mediaController?.let { controller ->
                    if (controller.mediaItemCount > 0) {
                        val currentIndex = controller.currentMediaItemIndex
                        val insertIndex = (currentIndex + 1 + queueInsertionCounter).coerceAtMost(controller.mediaItemCount)
                        controller.addMediaItem(insertIndex, mediaItem)
                        queueInsertionCounter++
                        _toastMessage.value = "${track.title} sıraya eklendi"
                    } else {
                        queueInsertionCounter = 0
                        onPlayStreamClicked(track)
                    }
                } ?: run {
                    queueInsertionCounter = 0
                    onPlayStreamClicked(track)
                }
            }.onFailure {
                _toastMessage.value = "Ses akışı alınamadı."
            }
        }
    }

    fun playNextLocalTrack(localTrack: LocalTrack) = addToQueue(localTrack)

    fun playNextTrack(track: OnlineSearchResultDto) = addToQueue(track)

    private fun deleteArtworkFiles(videoId: String) {
        if (videoId.isBlank()) return
        try {
            val candidates = listOf("art_$videoId.jpg", "art_$videoId.png", "$videoId.jpg", "$videoId.png")
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteTrack(trackId: String, uriString: String, filePath: String?, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val actualPath = filePath ?: if (uriString.startsWith("/")) uriString else if (uriString.startsWith("file://")) Uri.parse(uriString).path else null

                // Clear from DownloadRepository cache
                val matchingLocal = _localTracks.value.find { it.id.toString() == trackId || it.uri == uriString || (actualPath != null && it.filePath == actualPath) }
                val matchingDto = _onlineResults.value.find { it.videoId == trackId || it.url == uriString }
                val titleToClear = matchingLocal?.title ?: matchingDto?.title
                downloadRepository.clearDownloadState(trackId, titleToClear)
                matchingDto?.videoId?.let { if (it != trackId) downloadRepository.clearDownloadState(it, titleToClear) }

                // Blacklist track persistently
                deletedTracksStore.addDeletedTrack(
                    trackId = trackId,
                    filePath = actualPath,
                    contentUri = uriString
                )

                if (uriString.startsWith("content://")) {
                    val uri = Uri.parse(uriString)
                    try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (securityException: Exception) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                                _deleteIntentSender.value = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                            val recoverableException = securityException as? android.app.RecoverableSecurityException
                            if (recoverableException != null) {
                                val intentSender = recoverableException.userAction.actionIntent.intentSender
                                _deleteIntentSender.value = IntentSenderRequest.Builder(intentSender).build()
                            }
                        }
                    }
                } else if (uriString.startsWith("file://")) {
                    try {
                        val file = File(Uri.parse(uriString).path ?: "")
                        if (file.exists()) file.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (!actualPath.isNullOrBlank()) {
                    val file = File(actualPath)
                    if (file.exists()) {
                        file.delete()
                    }
                    try {
                        MediaScannerConnection.scanFile(context, arrayOf(actualPath), null, null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                deleteArtworkFiles(trackId)

                playlistRepository.deleteTrackFromAllPlaylists(trackId, uriString)

                _localTracks.value = _localTracks.value.filter {
                    it.id.toString() != trackId &&
                    it.uri != uriString &&
                    (filePath.isNullOrBlank() || it.filePath != filePath) &&
                    (actualPath.isNullOrBlank() || it.filePath != actualPath)
                }
                // favorites state updates automatically via Room DB flow
                _recentTracks.value = _recentTracks.value.filter { it.videoId != trackId && it.url != uriString }
                _onlineResults.value = _onlineResults.value.filter { it.videoId != trackId && it.url != uriString }

                mediaController?.let { controller ->
                    val currentItem = controller.currentMediaItem
                    val isCurrentPlayingDeleted = (currentItem?.mediaId == trackId) ||
                            (_currentPlayingTrack.value?.videoId == trackId) ||
                            (_currentPlayingTrack.value?.url == uriString)

                    val queueSize = controller.mediaItemCount
                    var deletedIndex = -1
                    for (i in 0 until queueSize) {
                        val item = controller.getMediaItemAt(i)
                        if (item.mediaId == trackId || item.localConfiguration?.uri?.toString() == uriString) {
                            deletedIndex = i
                            break
                        }
                    }

                    if (isCurrentPlayingDeleted) {
                        if (controller.hasNextMediaItem()) {
                            controller.seekToNextMediaItem()
                        } else {
                            controller.pause()
                            _isPlaying.value = false
                            _currentPlayingTrack.value = null
                        }
                    }

                    if (deletedIndex != -1) {
                        controller.removeMediaItem(deletedIndex)
                    }
                } ?: run {
                    if (_currentPlayingTrack.value?.videoId == trackId || _currentPlayingTrack.value?.url == uriString) {
                        _isPlaying.value = false
                        _currentPlayingTrack.value = null
                    }
                }

                _toastMessage.value = "Medya silindi."
                loadLocalTracks()
                onComplete?.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.value = "Silme işlemi başarısız oldu."
            }
        }
    }

    fun updateLocalTrackMetaData(
        track: LocalTrack,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newArtworkUri: Uri?,
        onComplete: (() -> Unit)? = null
    ) {
        val actualPath = track.filePath ?: if (track.uri.startsWith("/")) track.uri else if (track.uri.startsWith("file://")) Uri.parse(track.uri).path else null
        
        if (actualPath.isNullOrBlank()) {
            _toastMessage.value = "Dosya yolu bulunamadı"
            onComplete?.invoke()
            return
        }

        viewModelScope.launch {
            try {
                val file = File(actualPath)
                if (!file.exists()) {
                    _toastMessage.value = "Dosya mevcut değil"
                    onComplete?.invoke()
                    return@launch
                }

                val tempArtworkFile = newArtworkUri?.let { uri ->
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val tempFile = File(context.cacheDir, "temp_edit_artwork_${System.currentTimeMillis()}.jpg")
                    inputStream?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile
                }

                try {
                    com.nkapps.nkmuzik.downloader.ArtworkEmbedder.embedArtwork(
                        audioFile = file,
                        artworkFile = tempArtworkFile,
                        title = newTitle,
                        artist = newArtist,
                        album = newAlbum
                    )
                } catch (securityException: SecurityException) {
                    tempArtworkFile?.delete()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val uri = Uri.parse(track.uri)
                            val pendingIntent = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                            pendingEditTrackData = PendingEditTrackData(track, newTitle, newArtist, newAlbum, newArtworkUri, onComplete)
                            _editIntentSender.value = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onComplete?.invoke()
                        }
                    } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                        val recoverableException = securityException as? android.app.RecoverableSecurityException
                        if (recoverableException != null) {
                            val intentSender = recoverableException.userAction.actionIntent.intentSender
                            pendingEditTrackData = PendingEditTrackData(track, newTitle, newArtist, newAlbum, newArtworkUri, onComplete)
                            _editIntentSender.value = IntentSenderRequest.Builder(intentSender).build()
                        } else {
                            onComplete?.invoke()
                        }
                    } else {
                        onComplete?.invoke()
                    }
                    return@launch
                }

                tempArtworkFile?.delete()

                try {
                    MediaScannerConnection.scanFile(context, arrayOf(actualPath), null) { _, _ ->
                        loadLocalTracks()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    loadLocalTracks()
                }

                onComplete?.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.value = "Hata: ${e.message}"
                onComplete?.invoke()
            }
        }
    }

    fun deleteTrack(localTrack: LocalTrack, onComplete: (() -> Unit)? = null) {
        deleteTrack(
            trackId = localTrack.id.toString(),
            uriString = localTrack.uri,
            filePath = localTrack.filePath,
            onComplete = onComplete
        )
    }

    fun deleteTrack(dto: OnlineSearchResultDto, onComplete: (() -> Unit)? = null) {
        val matchingLocal = _localTracks.value.find { it.id.toString() == dto.videoId || it.uri == dto.url }
        deleteTrack(
            trackId = dto.videoId,
            uriString = dto.url,
            filePath = matchingLocal?.filePath ?: if (dto.url.startsWith("/")) dto.url else null,
            onComplete = onComplete
        )
    }

    private fun buildMediaItemForTrackFast(track: OnlineSearchResultDto, resolvedAudioUrl: String? = null): MediaItem {
        val rawArt = ArtworkUtils.getRawArtworkModel(context, track.thumbnailUrl, track.title, track.videoId, track.durationString, track.uploaderName)?.toString()
        val resolvedArt = rawArt ?: ArtworkUtils.getDirectOnlineThumbnailUrl(track.videoId)

        val isLocal = track.url.startsWith("content://") || track.url.startsWith("file://") || track.url.startsWith("/")
        val audioUrl = when {
            isLocal -> track.url
            !resolvedAudioUrl.isNullOrBlank() -> resolvedAudioUrl
            else -> track.url
        }

        val validUri = if (isLocal) {
            val actualPath = if (audioUrl.startsWith("file://")) Uri.parse(audioUrl).path else if (audioUrl.startsWith("/")) audioUrl else null
            getValidMediaUri(audioUrl, actualPath)
        } else {
            getValidMediaUri(audioUrl, null)
        }
        val mimeType = if (isLocal) inferMimeType(audioUrl, track.url, null) else null

        val builder = MediaItem.Builder()
            .setUri(validUri)
            .setMediaId(track.videoId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.uploaderName)
                    .setArtworkUri(ArtworkUtils.parseArtworkUri(resolvedArt))
                    .build()
            )
        if (mimeType != null) {
            builder.setMimeType(mimeType)
        }
        return builder.build()
    }

    fun playStreamQueue(tracks: List<OnlineSearchResultDto>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        if (!enableOnlineFeatures.value) {
            _toastMessage.value = "Çevrimiçi dinleme şu an devre dışı."
            return
        }
        queueInsertionCounter = 0
        val safeIndex = startIndex.coerceIn(0, (tracks.size - 1).coerceAtLeast(0))
        val startTrack = tracks[safeIndex]

        analyticsManager.logUsageMode("online")
        analyticsManager.logTrackPlay(mediaType = "online", durationSeconds = ArtworkUtils.parseDurationSeconds(startTrack.durationString))

        viewModelScope.launch {
            val isLocal = startTrack.url.startsWith("content://") || startTrack.url.startsWith("file://") || startTrack.url.startsWith("/")
            var resolvedStartAudioUrl: String? = null
            if (!isLocal) {
                val streamRes = searchRepository.getAudioStreamUrl(startTrack.videoId)
                val streamInfo = streamRes.getOrNull()
                if (streamInfo == null || streamInfo.audioUrl.isBlank()) {
                    val errText = streamRes.exceptionOrNull()?.message ?: "Ses akışı başlatılamadı."
                    _toastMessage.value = errText
                    _isPlaying.value = false
                    return@launch
                }
                resolvedStartAudioUrl = streamInfo.audioUrl
            }

            setCurrentPlayingTrackInternal(startTrack)
            _isPlaying.value = true
            addToRecent(startTrack)

            val mediaItems = tracks.mapIndexed { index, track ->
                if (index == safeIndex && !resolvedStartAudioUrl.isNullOrBlank()) {
                    buildMediaItemForTrackFast(track, resolvedStartAudioUrl)
                } else {
                    buildMediaItemForTrackFast(track, null)
                }
            }

            mediaController?.let { controller ->
                controller.setMediaItems(mediaItems, safeIndex, 0L)
                controller.prepare()
                controller.play()
            }
        }
    }

    fun playLocalQueue(tracks: List<LocalTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        queueInsertionCounter = 0
        val safeIndex = startIndex.coerceIn(0, (tracks.size - 1).coerceAtLeast(0))
        val firstTrack = tracks[safeIndex]

        analyticsManager.logUsageMode("local")
        analyticsManager.logTrackPlay(mediaType = "local", durationSeconds = ArtworkUtils.parseDurationSeconds(firstTrack.duration))

        val items = tracks.map { localTrack ->
            val mediaUri = getValidMediaUri(localTrack.uri, localTrack.filePath)
            val mimeType = inferMimeType(localTrack.uri, localTrack.filePath)

            val builder = MediaItem.Builder()
                .setUri(mediaUri)
                .setMediaId(localTrack.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(localTrack.title)
                        .setArtist(localTrack.artist)
                        .setArtworkUri(ArtworkUtils.parseArtworkUri(localTrack.artworkUri))
                        .build()
                )
            if (mimeType != null) {
                builder.setMimeType(mimeType)
            }
            builder.build()
        }
        mediaController?.let { controller ->
            controller.setMediaItems(items, safeIndex, 0L)
            controller.prepare()
            controller.play()
        }
        val firstDto = OnlineSearchResultDto(
            videoId = firstTrack.id.toString(),
            title = firstTrack.title,
            uploaderName = firstTrack.artist,
            durationString = firstTrack.duration,
            thumbnailUrl = firstTrack.artworkUri ?: "",
            url = firstTrack.uri
        )
        setCurrentPlayingTrackInternal(firstDto)
        _isPlaying.value = true
    }

    fun playPlaylistTracks(tracks: List<PlaylistTrackEntity>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        queueInsertionCounter = 0
        val safeIndex = startIndex.coerceIn(0, (tracks.size - 1).coerceAtLeast(0))
        val firstTrack = tracks[safeIndex]
        val firstDto = OnlineSearchResultDto(
            videoId = firstTrack.trackId,
            title = firstTrack.title,
            uploaderName = firstTrack.artist,
            durationString = firstTrack.duration,
            thumbnailUrl = firstTrack.artworkUri ?: "",
            url = firstTrack.mediaUri
        )

        analyticsManager.logFeatureUsed("playlist")
        analyticsManager.logTrackPlay(
            mediaType = if (firstTrack.isLocal) "local" else "online",
            durationSeconds = ArtworkUtils.parseDurationSeconds(firstDto.durationString)
        )

        setCurrentPlayingTrackInternal(firstDto)
        _isPlaying.value = true

        viewModelScope.launch {
            val mediaItems = coroutineScope {
                tracks.map { track ->
                    async(Dispatchers.IO) {
                        val rawUri = track.mediaUri
                        val isOnline = !track.isLocal && !rawUri.startsWith("content://") && !rawUri.startsWith("file://") && !rawUri.startsWith("/")
                        val mediaUriStr = if (isOnline) {
                            val streamRes = searchRepository.getAudioStreamUrl(track.trackId)
                            streamRes.getOrNull()?.audioUrl ?: rawUri
                        } else {
                            rawUri
                        }
                        val validUri = getValidMediaUri(mediaUriStr)
                        val mimeType = if (!isOnline) inferMimeType(rawUri) else null

                        val builder = MediaItem.Builder()
                            .setUri(validUri)
                            .setMediaId(track.trackId)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(track.title)
                                    .setArtist(track.artist)
                                    .setArtworkUri(ArtworkUtils.parseArtworkUri(track.artworkUri))
                                    .build()
                            )
                        if (mimeType != null) {
                            builder.setMimeType(mimeType)
                        }
                        builder.build()
                    }
                }.awaitAll()
            }
            mediaController?.let { controller ->
                controller.setMediaItems(mediaItems, safeIndex, 0L)
                controller.prepare()
                controller.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    fun skipToNext() {
        mediaController?.let { controller ->
            if ((controller.mediaItemCount <= 1 || !controller.hasNextMediaItem()) && _localTracks.value.size > 1 && !controller.isPlaying) {
                restorePlayerQueueOnStartup()
            }
            if (controller.hasNextMediaItem() || controller.repeatMode == Player.REPEAT_MODE_ALL) {
                controller.seekToNext()
            } else if (controller.mediaItemCount > 0) {
                controller.seekTo(0, 0L)
            }
        }
    }

    fun skipToPrevious() {
        mediaController?.let { controller ->
            if ((controller.mediaItemCount <= 1 || !controller.hasPreviousMediaItem()) && _localTracks.value.size > 1 && !controller.isPlaying) {
                restorePlayerQueueOnStartup()
            }
            if (controller.hasPreviousMediaItem()) {
                controller.seekToPrevious()
            } else if (controller.mediaItemCount > 0) {
                controller.seekTo(0, 0L)
            }
        }
    }

    fun toggleShuffle() {
        mediaController?.let { controller ->
            val nextState = !controller.shuffleModeEnabled
            controller.shuffleModeEnabled = nextState
            _isShuffleEnabled.value = nextState
            _toastMessage.value = if (nextState) "Karışık Oynat" else "Sıralı Oynat"
        }
    }

    fun toggleRepeat() {
        mediaController?.let { controller ->
            val nextMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            controller.repeatMode = nextMode
            _repeatMode.value = nextMode
            _toastMessage.value = when (nextMode) {
                Player.REPEAT_MODE_ALL -> "Tümünü Tekrar Et"
                Player.REPEAT_MODE_ONE -> "Tek Şarkı Tekrarı"
                else -> "Sıralı Oynat"
            }
        }
        updateQueueFromController()
    }

    fun skipToQueueItem(index: Int) {
        mediaController?.let { controller ->
            if (index in 0 until controller.mediaItemCount) {
                controller.seekToDefaultPosition(index)
                controller.play()
                _isPlaying.value = true
            }
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        mediaController?.let { controller ->
            if (fromIndex in 0 until controller.mediaItemCount && toIndex in 0 until controller.mediaItemCount) {
                controller.moveMediaItem(fromIndex, toIndex)
                val currentList = _queueItems.value.toMutableList()
                if (fromIndex in currentList.indices && toIndex in currentList.indices) {
                    val item = currentList.removeAt(fromIndex)
                    currentList.add(toIndex, item)
                    _queueItems.value = currentList
                } else {
                    updateQueueFromController()
                }
            }
        }
    }

    fun removeFromQueue(index: Int) {
        mediaController?.let { controller ->
            if (index in 0 until controller.mediaItemCount) {
                controller.removeMediaItem(index)
                updateQueueFromController()
                _toastMessage.value = "Sıradan kaldırıldı"
            }
        }
    }

    fun cyclePlaybackMode() {
        val controller = mediaController ?: return
        val isShuffle = controller.shuffleModeEnabled
        val repeat = controller.repeatMode

        when {
            !isShuffle && repeat == Player.REPEAT_MODE_OFF -> {
                // 1. Sequential -> 2. Repeat All
                controller.shuffleModeEnabled = false
                controller.repeatMode = Player.REPEAT_MODE_ALL
                _isShuffleEnabled.value = false
                _repeatMode.value = Player.REPEAT_MODE_ALL
                _toastMessage.value = "Tümünü Tekrar Et"
            }
            !isShuffle && repeat == Player.REPEAT_MODE_ALL -> {
                // 2. Repeat All -> 3. Repeat One
                controller.shuffleModeEnabled = false
                controller.repeatMode = Player.REPEAT_MODE_ONE
                _isShuffleEnabled.value = false
                _repeatMode.value = Player.REPEAT_MODE_ONE
                _toastMessage.value = "Tek Şarkı Tekrarı"
            }
            !isShuffle && repeat == Player.REPEAT_MODE_ONE -> {
                // 3. Repeat One -> 4. Shuffle
                controller.repeatMode = Player.REPEAT_MODE_OFF
                controller.shuffleModeEnabled = true
                _repeatMode.value = Player.REPEAT_MODE_OFF
                _isShuffleEnabled.value = true
                _toastMessage.value = "Karışık Oynat"
            }
            else -> {
                // 4. Shuffle -> 1. Sequential
                controller.shuffleModeEnabled = false
                controller.repeatMode = Player.REPEAT_MODE_OFF
                _isShuffleEnabled.value = false
                _repeatMode.value = Player.REPEAT_MODE_OFF
                _toastMessage.value = "Sıralı Oynat"
            }
        }
        updateQueueFromController()
    }

    fun toggleFavorite(track: OnlineSearchResultDto) {
        viewModelScope.launch {
            val currentlyFav = isFavorite(track)
            playlistRepository.toggleFavorite(track)
            if (currentlyFav) {
                _toastMessage.value = "Favorilerden çıkarıldı"
            } else {
                _toastMessage.value = "Favorilere eklendi"
            }
        }
    }

    fun toggleFavorite(track: LocalTrack) {
        val dto = OnlineSearchResultDto(
            videoId = if (!track.videoId.isNullOrBlank()) track.videoId!! else track.id.toString(),
            title = track.title,
            uploaderName = track.artist,
            durationString = track.duration,
            thumbnailUrl = track.artworkUri ?: "",
            url = track.uri
        )
        toggleFavorite(dto)
    }

    fun isFavorite(track: OnlineSearchResultDto): Boolean {
        val videoId = track.videoId
        val url = track.url
        return favorites.value.any { fav ->
            (videoId.isNotBlank() && fav.videoId == videoId) ||
            (url.isNotBlank() && fav.url == url)
        }
    }

    fun isFavorite(track: LocalTrack): Boolean {
        val videoId = track.videoId ?: track.id.toString()
        val url = track.uri
        return favorites.value.any { fav ->
            (fav.videoId.isNotBlank() && fav.videoId == videoId) ||
            (fav.url.isNotBlank() && fav.url == url)
        }
    }

    fun createPlaylist(name: String) {
        analyticsManager.logFeatureUsed("playlist")
        viewModelScope.launch {
            if (name.isNotBlank()) {
                playlistRepository.createPlaylist(name)
                _toastMessage.value = "Çalma listesi oluşturuldu: $name"
            }
        }
    }

    fun updatePlaylistName(playlistId: Long, newName: String) {
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                playlistRepository.updatePlaylistName(playlistId, newName)
                _toastMessage.value = "Çalma listesi adı güncellendi"
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
            _toastMessage.value = "Çalma listesi silindi"
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: OnlineSearchResultDto) {
        analyticsManager.logFeatureUsed("playlist")
        viewModelScope.launch {
            playlistRepository.addTrackToPlaylist(playlistId, track)
            _toastMessage.value = "Çalma listesine eklendi"
        }
    }

    fun addLocalTrackToPlaylist(playlistId: Long, track: LocalTrack) {
        analyticsManager.logFeatureUsed("playlist")
        viewModelScope.launch {
            playlistRepository.addLocalTrackToPlaylist(playlistId, track)
            _toastMessage.value = "Çalma listesine eklendi"
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            playlistRepository.removeTrackFromPlaylist(playlistId, trackId)
            _toastMessage.value = "Şarkı çalma listesinden çıkarıldı"
        }
    }

    fun toggleTrackSelection(trackId: Long) {
        val current = _selectedTrackIds.value
        if (current.contains(trackId)) {
            _selectedTrackIds.value = current - trackId
        } else {
            _selectedTrackIds.value = current + trackId
        }
    }

    fun toggleFavoriteSelection(videoId: String) {
        val current = _selectedFavoriteVideoIds.value
        if (current.contains(videoId)) {
            _selectedFavoriteVideoIds.value = current - videoId
        } else {
            _selectedFavoriteVideoIds.value = current + videoId
        }
    }

    fun clearSelection() {
        _selectedTrackIds.value = emptySet()
        _selectedFavoriteVideoIds.value = emptySet()
    }

    fun clearFavoriteSelection() {
        _selectedFavoriteVideoIds.value = emptySet()
    }

    fun selectAllTracks(ids: List<Long>) {
        _selectedTrackIds.value = ids.toSet()
    }

    fun selectAllFavorites(videoIds: List<String>) {
        _selectedFavoriteVideoIds.value = videoIds.toSet()
    }

    fun incrementPlayCount(trackId: String) {
        if (trackId.isBlank()) return
        val current = _playCounts.value.toMutableMap()
        current[trackId] = (current[trackId] ?: 0) + 1
        _playCounts.value = current
    }

    fun getPlayCount(trackId: String): Int {
        return _playCounts.value[trackId] ?: 0
    }

    fun playSelectedTracks(selectedIds: Set<Long>) {
        val tracksToPlay = _localTracks.value.filter { it.id in selectedIds }
        if (tracksToPlay.isNotEmpty()) {
            playLocalQueue(tracksToPlay, 0)
            clearSelection()
            _toastMessage.value = "${tracksToPlay.size} parça oynatılıyor"
        }
    }

    fun addSelectedToFavorites(selectedIds: Set<Long>) {
        val tracks = _localTracks.value.filter { it.id in selectedIds }
        if (tracks.isNotEmpty()) {
            tracks.forEach { track ->
                val dto = localTrackToDto(track)
                if (!isFavorite(dto)) {
                    toggleFavorite(dto)
                }
            }
            clearSelection()
            _toastMessage.value = "${tracks.size} parça favorilere eklendi"
        }
    }

    fun addSelectedToPlaylist(playlistId: Long, selectedIds: Set<Long>) {
        val tracks = _localTracks.value.filter { it.id in selectedIds }
        if (tracks.isNotEmpty()) {
            viewModelScope.launch {
                tracks.forEach { track ->
                    playlistRepository.addLocalTrackToPlaylist(playlistId, track)
                }
                clearSelection()
                _toastMessage.value = "${tracks.size} parça çalma listesine eklendi"
            }
        }
    }

    fun deleteSelectedTracks(selectedIds: Set<Long>, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val tracksToDelete = _localTracks.value.filter { it.id in selectedIds }
            tracksToDelete.forEach { track ->
                deleteTrack(
                    trackId = track.id.toString(),
                    uriString = track.uri,
                    filePath = track.filePath
                )
            }
            clearSelection()
            _toastMessage.value = "${tracksToDelete.size} parça silindi"
            onComplete?.invoke()
        }
    }

    fun playSelectedFavorites(selectedVideoIds: Set<String>) {
        val favsToPlay = favorites.value.filter { it.videoId in selectedVideoIds }
        if (favsToPlay.isNotEmpty()) {
            playStreamQueue(favsToPlay, 0)
            clearFavoriteSelection()
            _toastMessage.value = "${favsToPlay.size} favori parça oynatılıyor"
        }
    }

    fun removeSelectedFromFavorites(selectedVideoIds: Set<String>) {
        val favsToRemove = favorites.value.filter { it.videoId in selectedVideoIds }
        if (favsToRemove.isNotEmpty()) {
            favsToRemove.forEach { track ->
                if (isFavorite(track)) {
                    toggleFavorite(track)
                }
            }
            clearFavoriteSelection()
            _toastMessage.value = "${favsToRemove.size} parça favorilerden çıkarıldı"
        }
    }

    fun addSelectedFavoritesToPlaylist(playlistId: Long, selectedVideoIds: Set<String>) {
        val favs = favorites.value.filter { it.videoId in selectedVideoIds }
        if (favs.isNotEmpty()) {
            viewModelScope.launch {
                favs.forEach { track ->
                    playlistRepository.addTrackToPlaylist(playlistId, track)
                }
                clearFavoriteSelection()
                _toastMessage.value = "${favs.size} parça çalma listesine eklendi"
            }
        }
    }

    fun deleteSelectedFavorites(selectedVideoIds: Set<String>, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val favsToDelete = favorites.value.filter { it.videoId in selectedVideoIds }
            favsToDelete.forEach { dto ->
                deleteTrack(dto)
            }
            clearFavoriteSelection()
            _toastMessage.value = "${favsToDelete.size} parça silindi"
            onComplete?.invoke()
        }
    }

    fun getPlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>> {
        return playlistRepository.getPlaylistTracks(playlistId)
    }

    private fun addToRecent(track: OnlineSearchResultDto) {
        val current = _recentTracks.value.toMutableList()
        current.removeAll { it.videoId == track.videoId }
        current.add(0, track)
        if (current.size > 20) current.removeAt(current.size - 1)
        _recentTracks.value = current
    }

    fun startDownload(track: OnlineSearchResultDto, streamUrl: String = "") {
        if (!enableOnlineFeatures.value) {
            _toastMessage.value = "İndirme özelliği devre dışı"
            return
        }

        activeDownloadDtos[track.videoId] = track

        analyticsManager.logTrackDownload(track.title)

        Log.d("SearchViewModel", "startDownload called for track: ${track.title} (videoId: ${track.videoId})")
        viewModelScope.launch {
            deletedTracksStore.removeDeletedTrack(
                trackId = track.videoId,
                filePath = null,
                contentUri = track.url,
                title = track.title
            )
            val urlToUse = if (streamUrl.isNotBlank()) streamUrl else track.url.ifBlank { track.videoId }

            val uuid = downloadRepository.startDownload(track, urlToUse)
            if (uuid != null) {
                Log.i("SearchViewModel", "Download worker enqueued instantly with UUID: $uuid for track: ${track.title}")
                _toastMessage.value = "İndirme başlatıldı: ${track.title}"
            } else {
                Log.i("SearchViewModel", "Track already downloaded: ${track.title}")
                _toastMessage.value = "Bu şarkı zaten indirilmiş!"
            }
        }
    }

    fun onDownloadClicked(track: OnlineSearchResultDto) {
        if (!enableOnlineFeatures.value) {
            _toastMessage.value = "İndirme özelliği devre dışı"
            return
        }
        viewModelScope.launch {
            _toastMessage.value = "İndirmeye hazırlanıyor: ${track.title}"
            val streamRes = searchRepository.getAudioStreamUrl(track.videoId)
            val audioInfo = streamRes.getOrNull()
            if (audioInfo != null && audioInfo.audioUrl.isNotBlank()) {
                startDownload(track, audioInfo.audioUrl)
            } else {
                _toastMessage.value = "Şarkı indirme bağlantısı alınamadı"
            }
        }
    }

    fun cancelDownload(videoId: String) {
        downloadRepository.cancelDownload(videoId)
        _toastMessage.value = "İndirme iptal edildi."
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    private fun buildMediaItemFromLocalTrack(localTrack: LocalTrack): MediaItem {
        val mediaUri = getValidMediaUri(localTrack.uri, localTrack.filePath)
        val mimeType = inferMimeType(localTrack.uri, localTrack.filePath)

        val builder = MediaItem.Builder()
            .setUri(mediaUri)
            .setMediaId(localTrack.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(localTrack.title)
                    .setArtist(localTrack.artist)
                    .setArtworkUri(ArtworkUtils.parseArtworkUri(localTrack.artworkUri))
                    .build()
            )
        if (mimeType != null) {
            builder.setMimeType(mimeType)
        }
        return builder.build()
    }

    fun restorePlayerQueueOnStartup() {
        val controller = mediaController ?: return
        val localTracksList = _localTracks.value
        val restoredTrack = _currentPlayingTrack.value

        if (controller.mediaItemCount > 1) return
        if (controller.mediaItemCount == 1 && controller.isPlaying) return
        if (localTracksList.isEmpty() && restoredTrack == null) return

        val items = mutableListOf<MediaItem>()
        var startIndex = 0
        val startPosition = _currentPosition.value.coerceAtLeast(0L)

        if (restoredTrack != null && restoredTrack.videoId.isNotBlank()) {
            val matchedIndex = localTracksList.indexOfFirst { local ->
                local.id.toString() == restoredTrack.videoId ||
                        local.uri == restoredTrack.url ||
                        (local.filePath != null && local.filePath == restoredTrack.url)
            }

            if (matchedIndex != -1) {
                items.addAll(localTracksList.map { buildMediaItemFromLocalTrack(it) })
                startIndex = matchedIndex
            } else {
                val isLocalOrDirect = restoredTrack.url.startsWith("content://") ||
                        restoredTrack.url.startsWith("file://") ||
                        restoredTrack.url.startsWith("/")
                if (isLocalOrDirect || restoredTrack.url.isNotBlank()) {
                    val mediaUri = getValidMediaUri(restoredTrack.url)
                    val mimeType = inferMimeType(restoredTrack.url)
                    val builder = MediaItem.Builder()
                        .setUri(mediaUri)
                        .setMediaId(restoredTrack.videoId)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(restoredTrack.title)
                                .setArtist(restoredTrack.uploaderName)
                                .setArtworkUri(ArtworkUtils.parseArtworkUri(restoredTrack.thumbnailUrl))
                                .build()
                        )
                    if (mimeType != null) {
                        builder.setMimeType(mimeType)
                    }
                    items.add(builder.build())
                    if (localTracksList.isNotEmpty()) {
                        items.addAll(localTracksList.map { buildMediaItemFromLocalTrack(it) })
                    }
                    startIndex = 0
                } else if (localTracksList.isNotEmpty()) {
                    items.addAll(localTracksList.map { buildMediaItemFromLocalTrack(it) })
                    startIndex = 0
                }
            }
        } else if (localTracksList.isNotEmpty()) {
            items.addAll(localTracksList.map { buildMediaItemFromLocalTrack(it) })
            startIndex = 0
        }

        if (items.isNotEmpty()) {
            controller.setMediaItems(items, startIndex, startPosition)
            controller.prepare()
            updateQueueFromController()
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.currentMediaItem == null && _currentPlayingTrack.value != null) {
                restorePlayerQueueOnStartup()
                if (controller.currentMediaItem != null) {
                    controller.play()
                    _isPlaying.value = true
                } else {
                    val track = _currentPlayingTrack.value!!
                    if (track.url.isNotBlank() && !track.url.startsWith("content://") && !track.url.startsWith("file://") && !track.url.startsWith("/")) {
                        onPlayStreamClicked(track)
                    }
                }
            } else if (controller.isPlaying) {
                controller.pause()
                _isPlaying.value = false
                saveLastPlayedTrackInfo()
            } else {
                controller.play()
                _isPlaying.value = true
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.removeListener(playerListener)
        MediaController.releaseFuture(controllerFuture)
    }
}
