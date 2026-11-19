package com.nkapps.nkmuzik.data.repository

import android.util.Log
import com.nkapps.nkmuzik.data.model.AudioStreamInfo
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.data.preferences.ThemePreferences
import com.nkapps.nkmuzik.utils.MediaTitleParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import javax.inject.Inject
import javax.inject.Singleton

interface OnlineSearchRepository {
    fun searchOnline(query: String): Flow<List<OnlineSearchResultDto>>
    fun searchTracks(query: String): Flow<List<OnlineSearchResultDto>> = searchOnline(query)
    suspend fun getAudioStreamUrl(videoId: String, forceRefresh: Boolean = false): Result<AudioStreamInfo>
    suspend fun getAudioStreamUrl(videoId: String): Result<AudioStreamInfo> = getAudioStreamUrl(videoId, forceRefresh = false)
    suspend fun getSearchCompletions(query: String): List<String>
    fun getCachedStreamInfo(videoId: String): AudioStreamInfo?
}

@Singleton
class OnlineSearchRepositoryImpl @Inject constructor(
    private val preferences: ThemePreferences?
) : OnlineSearchRepository {

    private fun logDebug(tag: String, message: String) {
        try { Log.d(tag, message) } catch (_: Throwable) {}
    }

    private fun logError(tag: String, message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
        } catch (_: Throwable) {}
    }

    private suspend fun isOnlineEnabled(): Boolean = true

    override fun searchTracks(query: String): Flow<List<OnlineSearchResultDto>> = searchOnline(query)

    override fun searchOnline(query: String): Flow<List<OnlineSearchResultDto>> = flow {
        if (!isOnlineEnabled() || query.isBlank()) {
            emit(emptyList())
            return@flow
        }

        logDebug("OnlineSearch", "Starting NewPipe search -> query: '$query'")
        val results = performNewPipeSearch(query)
        emit(results)
    }.flowOn(Dispatchers.IO)

    private suspend fun performNewPipeSearch(query: String): List<OnlineSearchResultDto> = withContext(Dispatchers.IO) {
        try {
            val searchExtractor = ServiceList.YouTube.getSearchExtractor(query)
            searchExtractor.fetchPage()

            val initialResults = searchExtractor.initialPage.items
            val results = mutableListOf<OnlineSearchResultDto>()

            for (item in initialResults) {
                // Sadece video tipindeki sonuçları al
                if (item is StreamInfoItem) {
                    val videoId = item.url.substringAfter("v=").substringBefore("&")
                    if (videoId.isNotBlank()) {
                        val rawTitle = item.name ?: ""
                        val rawArtist = item.uploaderName ?: ""
                        val durationStr = formatDuration(item.duration)
                        val thumbUrl = item.thumbnails.firstOrNull()?.url ?: ""

                        if (rawTitle.isNotBlank()) {
                            val parsed = MediaTitleParser.parse(rawTitle, rawArtist)
                            results.add(
                                OnlineSearchResultDto(
                                    videoId = videoId,
                                    title = parsed.title,
                                    uploaderName = parsed.artist,
                                    durationString = durationStr,
                                    thumbnailUrl = thumbUrl,
                                    url = item.url,
                                    description = ""
                                )
                            )
                        }
                    }
                }
            }

            if (results.isNotEmpty()) {
                logDebug("OnlineSearch", "NewPipe search success -> count: ${results.size}")
                return@withContext results
            } else {
                logError("OnlineSearch", "NewPipe search boş döndü.")
            }
        } catch (e: Exception) {
            logError("OnlineSearch", "NewPipe search failed: ${e.message}", e)
        }
        return@withContext emptyList()
    }

    private fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "0:00"
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%d:%02d", m, s)
    }

    override suspend fun getSearchCompletions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (!isOnlineEnabled() || query.isBlank()) return@withContext emptyList()
        try {
            val suggestions = ServiceList.YouTube.suggestionExtractor.suggestionList(query)
            return@withContext suggestions.map { it }
        } catch (e: Exception) {
            logError("OnlineSearch", "NewPipe suggestions failed: ${e.message}")
        }
        return@withContext emptyList()
    }

    private data class CachedStreamEntry(
        val info: AudioStreamInfo,
        val cachedTimeMs: Long = System.currentTimeMillis()
    )

    private val STREAM_CACHE_TTL_MS = 5_400_000L // 1.5 Saat
    private val streamCache = java.util.concurrent.ConcurrentHashMap<String, CachedStreamEntry>()

    override fun getCachedStreamInfo(videoId: String): AudioStreamInfo? {
        val cleanVideoId = videoId.substringAfterLast("v=").substringBefore("&").substringBefore("?").trim()
        val cached = streamCache[cleanVideoId]
        if (cached != null && cached.info.audioUrl.isNotBlank() && (System.currentTimeMillis() - cached.cachedTimeMs < STREAM_CACHE_TTL_MS)) {
            return cached.info
        }
        return null
    }

    override suspend fun getAudioStreamUrl(videoId: String, forceRefresh: Boolean): Result<AudioStreamInfo> = withContext(Dispatchers.IO) {
        val cleanVideoId = videoId.substringAfterLast("v=").substringBefore("&").substringBefore("?").trim()

        if (!isOnlineEnabled()) {
            return@withContext Result.failure(Exception("Çevrimiçi özellikler şu an kapalı."))
        }

        if (!forceRefresh) {
            val cached = getCachedStreamInfo(cleanVideoId)
            if (cached != null) return@withContext Result.success(cached)
        } else {
            streamCache.remove(cleanVideoId)
        }

        try {
            logDebug("AudioStream", "NewPipe ile stream çıkarılıyor -> $cleanVideoId")
            
            val url = "https://www.youtube.com/watch?v=$cleanVideoId"
            val streamExtractor = ServiceList.YouTube.getStreamExtractor(url)
            streamExtractor.fetchPage()

            val audioStreams = streamExtractor.audioStreams
            if (audioStreams != null && audioStreams.isNotEmpty()) {
                val m4aStream = audioStreams.find { stream ->
                    val fmtId = stream.format?.id ?: 0
                    val fmtName = stream.format?.name?.lowercase() ?: ""
                    fmtId == 140 || fmtName.contains("m4a") || fmtName.contains("aac")
                }
                val bestStream = m4aStream ?: audioStreams.maxByOrNull { it.averageBitrate } ?: audioStreams[0]

                val directUrl = bestStream.content
                if (!directUrl.isNullOrBlank()) {
                    val isWebm = bestStream.format?.name?.lowercase()?.contains("webm") == true

                    val info = AudioStreamInfo(
                        audioUrl = directUrl,
                        format = bestStream.format?.name?.uppercase() ?: "M4A",
                        container = if (isWebm) "webm" else "m4a",
                        quality = "${(bestStream.averageBitrate / 1000)} kbps",
                        bitrate = bestStream.averageBitrate,
                        title = streamExtractor.name ?: "",
                        uploaderName = streamExtractor.uploaderName ?: "",
                        thumbnailUrl = streamExtractor.thumbnails.firstOrNull()?.url ?: "",
                        // Şifre artık NewPipe'ın downloader'ı tarafından çözüldüğü için özel başlığa gerek yok
                        headers = emptyMap()
                    )

                    streamCache[cleanVideoId] = CachedStreamEntry(info, System.currentTimeMillis())
                    logDebug("AudioStream", "NewPipe ile şifresiz akış başarıyla alındı!")
                    return@withContext Result.success(info)
                }
            }
            throw Exception("NewPipe uygun ses formatı bulamadı.")

        } catch (e: Exception) {
            logError("AudioStream", "NewPipe Stream Extraction hatası: ${e.message}", e)
            return@withContext Result.failure(Exception("Ses akışı çözülemedi (NewPipe Hatası)."))
        }
    }
}