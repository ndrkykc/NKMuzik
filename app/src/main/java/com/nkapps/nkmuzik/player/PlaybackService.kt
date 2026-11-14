package com.nkapps.nkmuzik.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import com.nkapps.nkmuzik.R
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nkapps.nkmuzik.MainActivity
import com.nkapps.nkmuzik.data.model.LocalAudioTrack
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.data.preferences.LastPlayedTrackInfo
import com.nkapps.nkmuzik.data.preferences.ThemePreferences
import com.nkapps.nkmuzik.data.repository.OnlineSearchRepository
import com.nkapps.nkmuzik.data.repository.PlaylistRepository
import com.nkapps.nkmuzik.data.scanner.LocalMediaScanner
import com.nkapps.nkmuzik.utils.ArtworkUtils
import com.nkapps.nkmuzik.utils.MediaTitleParser
import androidx.glance.appwidget.updateAll
import com.nkapps.nkmuzik.widget.NKMediaGlanceWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var appMediaCache: AppMediaCache

    @Inject
    lateinit var audioFxManager: AudioFxManager

    @Inject
    lateinit var sleepTimerController: SleepTimerController

    @Inject
    lateinit var playlistRepository: PlaylistRepository

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var localMediaScanner: LocalMediaScanner

    @Inject
    lateinit var searchRepository: OnlineSearchRepository

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val mediaSourceFactory = DefaultMediaSourceFactory(appMediaCache.cacheDataSourceFactory)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    audioFxManager.init(audioSessionId)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                notifyWidgetUpdate()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    sleepTimerController.onTrackEnded()
                }
                notifyWidgetUpdate()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    sleepTimerController.onTrackEnded()
                }
                notifyWidgetUpdate()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("PlaybackService", "ExoPlayer Error: ${error.errorCodeName} (${error.errorCode})", error)
            }
        })

        sleepTimerController.addOnFinishListener {
            if (::player.isInitialized) {
                player.pause()
            }
        }

        val sessionId = player.audioSessionId
        if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
            audioFxManager.init(sessionId)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(MediaSessionCallback())
            .build()

        loadAndPlayFromStorage(nextOrPrev = 0, autoPlay = false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (::player.isInitialized) {
            when (intent?.action) {
                ACTION_PLAY_PAUSE -> {
                    if (player.mediaItemCount == 0) {
                        loadAndPlayFromStorage()
                    } else if (player.isPlaying) {
                        player.pause()
                    } else {
                        if (player.playbackState == Player.STATE_ENDED) {
                            player.seekTo(0, 0)
                        }
                        player.play()
                    }
                }
                ACTION_NEXT -> {
                    if (player.mediaItemCount == 0) {
                        loadAndPlayFromStorage(nextOrPrev = 1)
                    } else if (player.hasNextMediaItem()) {
                        player.seekToNextMediaItem()
                    }
                }
                ACTION_PREVIOUS -> {
                    if (player.mediaItemCount == 0) {
                        loadAndPlayFromStorage(nextOrPrev = -1)
                    } else if (player.hasPreviousMediaItem()) {
                        player.seekToPreviousMediaItem()
                    }
                }
                ACTION_FAVORITE -> {
                    val currentItem = player.currentMediaItem
                    if (currentItem != null) {
                        val meta = currentItem.mediaMetadata
                        val videoId = currentItem.mediaId
                        val rawTitle = meta.title?.toString()
                        val rawArtist = meta.artist?.toString()
                        val parsed = MediaTitleParser.parse(rawTitle, rawArtist)
                        val title = parsed.title
                        val artist = parsed.artist
                        val rawArt = meta.artworkUri?.toString() ?: ""
                        val dto = OnlineSearchResultDto(
                            videoId = videoId,
                            title = title,
                            uploaderName = artist,
                            durationString = "",
                            thumbnailUrl = rawArt,
                            url = ""
                        )
                        serviceScope.launch(Dispatchers.IO) {
                            playlistRepository.toggleFavorite(dto)
                            withContext(Dispatchers.Main) {
                                notifyWidgetUpdate()
                            }
                        }
                    }
                }
            }
            notifyWidgetUpdate()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun loadAndPlayFromStorage(nextOrPrev: Int = 0, autoPlay: Boolean = true) {
        serviceScope.launch(Dispatchers.IO) {
            val lastTrack: LastPlayedTrackInfo? = try {
                themePreferences.lastPlayedTrack.firstOrNull()
            } catch (_: Exception) {
                null
            }
            val localTracks: List<LocalAudioTrack> = try {
                localMediaScanner.scanAudioFiles()
            } catch (_: Exception) {
                emptyList()
            }

            val items = mutableListOf<MediaItem>()
            var startIndex = 0
            var startPosition = 0L

            if (lastTrack != null && lastTrack.contentUri.isNotBlank()) {
                val isLocal = lastTrack.contentUri.startsWith("content://") ||
                        lastTrack.contentUri.startsWith("file://") ||
                        lastTrack.contentUri.startsWith("/")

                val matchedIndex = localTracks.indexOfFirst {
                    it.contentUri.toString() == lastTrack.contentUri ||
                            it.id.toString() == lastTrack.trackId ||
                            (it.filePath != null && it.filePath == lastTrack.contentUri)
                }

                if (matchedIndex != -1) {
                    items.addAll(localTracks.map { buildMediaItemFromLocalTrack(it) })
                    startIndex = matchedIndex
                    startPosition = lastTrack.position.coerceAtLeast(0L)
                } else {
                    val audioUrl = if (isLocal) {
                        lastTrack.contentUri
                    } else {
                        val streamRes = searchRepository.getAudioStreamUrl(lastTrack.trackId)
                        streamRes.getOrNull()?.audioUrl ?: lastTrack.contentUri
                    }

                    if (audioUrl.isNotBlank()) {
                        val validUri = getValidMediaUri(audioUrl, null)
                        val mimeType = inferMimeType(audioUrl, lastTrack.contentUri)
                        val lastMediaItem = MediaItem.Builder()
                            .setUri(validUri)
                            .setMediaId(lastTrack.trackId)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(lastTrack.title)
                                    .setArtist(lastTrack.artist)
                                    .setArtworkUri(ArtworkUtils.parseArtworkUri(lastTrack.artworkUri))
                                    .build()
                            ).apply {
                                if (mimeType != null) setMimeType(mimeType)
                            }
                            .build()

                        items.add(lastMediaItem)
                        if (localTracks.isNotEmpty()) {
                            items.addAll(localTracks.map { buildMediaItemFromLocalTrack(it) })
                        }
                        startIndex = 0
                        startPosition = lastTrack.position.coerceAtLeast(0L)
                    } else if (localTracks.isNotEmpty()) {
                        items.addAll(localTracks.map { buildMediaItemFromLocalTrack(it) })
                        startIndex = 0
                        startPosition = 0L
                    }
                }
            } else if (localTracks.isNotEmpty()) {
                items.addAll(localTracks.map { buildMediaItemFromLocalTrack(it) })
                startIndex = 0
                startPosition = 0L
            }

            if (items.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    if (::player.isInitialized && player.mediaItemCount == 0) {
                        if (nextOrPrev == 1 && items.size > 1) {
                            startIndex = (startIndex + 1) % items.size
                            startPosition = 0L
                        } else if (nextOrPrev == -1 && items.size > 1) {
                            startIndex = if (startIndex - 1 < 0) items.size - 1 else startIndex - 1
                            startPosition = 0L
                        }
                        player.setMediaItems(items, startIndex, startPosition)
                        player.prepare()
                        if (autoPlay) {
                            player.play()
                        }
                        notifyWidgetUpdate()
                    }
                }
            }
        }
    }

    private fun buildMediaItemFromLocalTrack(track: LocalAudioTrack): MediaItem {
        val mediaUri = getValidMediaUri(track.contentUri.toString(), track.filePath)
        val mimeType = inferMimeType(track.contentUri.toString(), track.filePath)
        val builder = MediaItem.Builder()
            .setUri(mediaUri)
            .setMediaId(track.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(track.artworkUri)
                    .build()
            )
        if (mimeType != null) {
            builder.setMimeType(mimeType)
        }
        return builder.build()
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

    private fun inferMimeType(uriString: String, filePath: String? = null): String? {
        val target = (filePath ?: uriString).lowercase()
        val lowerUrl = uriString.lowercase()

        return when {
            lowerUrl.contains("mime=audio%2fwebm") || lowerUrl.contains("mime=audio/webm") || target.endsWith(".webm") -> MimeTypes.AUDIO_WEBM
            lowerUrl.contains("mime=audio%2fmpeg") || lowerUrl.contains("mime=audio/mp3") || target.endsWith(".mp3") -> MimeTypes.AUDIO_MPEG
            lowerUrl.contains("mime=audio%2fmp4") || lowerUrl.contains("mime=audio/mp4") || target.endsWith(".m4a") || target.endsWith(".mp4") -> MimeTypes.AUDIO_MP4
            target.endsWith(".ogg") || target.endsWith(".opus") -> MimeTypes.AUDIO_OGG
            target.endsWith(".aac") -> MimeTypes.AUDIO_AAC
            target.endsWith(".flac") -> MimeTypes.AUDIO_FLAC
            target.endsWith(".wav") -> MimeTypes.AUDIO_WAV
            uriString.startsWith("http://") || uriString.startsWith("https://") -> MimeTypes.AUDIO_MP4
            else -> null
        }
    }

    private fun notifyWidgetUpdate() {
        if (!::player.isInitialized) return
        val currentItem = player.currentMediaItem
        val isPlaying = player.isPlaying
        if (currentItem != null) {
            val meta = currentItem.mediaMetadata
            val videoId = currentItem.mediaId
            val rawTitle = meta.title?.toString()
            val rawArtist = meta.artist?.toString()
            val parsed = MediaTitleParser.parse(rawTitle, rawArtist)
            val title = parsed.title
            val artist = parsed.artist
            val rawArt = meta.artworkUri?.toString()
            val resolvedArt = ArtworkUtils.getArtworkModel(this, rawArt, title, videoId)?.data?.toString()
                ?: if (videoId.isNotBlank()) ArtworkUtils.getDirectOnlineThumbnailUrl(videoId) else (rawArt ?: "")

            val currentPos = player.currentPosition.coerceAtLeast(0L)
            val currentContentUri = currentItem.localConfiguration?.uri?.toString() ?: ""

            serviceScope.launch(Dispatchers.IO) {
                val isFav = videoId.isNotBlank() && playlistRepository.isFavorite(videoId, "")
                if (title.isNotBlank()) {
                    try {
                        themePreferences.saveLastPlayedTrack(
                            trackId = videoId,
                            title = title,
                            artist = artist,
                            artworkUri = resolvedArt,
                            contentUri = currentContentUri,
                            position = currentPos
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                try {
                    NKMediaGlanceWidget().updateAll(this@PlaybackService)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    NKMediaGlanceWidget().updateAll(this@PlaybackService)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val channelId = "playback_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "NK Müzik Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("NK Müzik")
            .setContentText("Müzik Çalıyor")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            audioFxManager.release()
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private class MediaSessionCallback : MediaSession.Callback

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY_PAUSE = "com.nkapps.nkmuzik.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.nkapps.nkmuzik.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.nkapps.nkmuzik.ACTION_PREVIOUS"
        const val ACTION_FAVORITE = "com.nkapps.nkmuzik.ACTION_FAVORITE"
    }
}
