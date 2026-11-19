package com.nkapps.nkmuzik.downloader

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class DownloadProgressInfo(
    val videoId: String,
    val title: String,
    val status: DownloadStatus,
    val progress: Int = 0
)

@Singleton
class DownloadRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _downloadStates = MutableStateFlow<Map<String, DownloadProgressInfo>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadProgressInfo>> = _downloadStates.asStateFlow()

    fun startDownload(item: OnlineSearchResultDto, streamUrl: String): UUID? {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val appDirs = listOfNotNull(musicDir, File(musicDir, "NK Müzik"), File(musicDir, "NK Muzik"))

        val parsed = com.nkapps.nkmuzik.utils.MediaTitleParser.parse(item.title, item.uploaderName.ifBlank { null })
        val cleanTitle = parsed.title
        val cleanArtist = parsed.artist

        val parsedFormatted = if (cleanArtist.isNotBlank() && !cleanTitle.lowercase().startsWith(cleanArtist.lowercase())) {
            "$cleanArtist - $cleanTitle"
        } else {
            cleanTitle
        }
        val sanitizedParsedTitle = parsedFormatted.replace(Regex("[\\\\/:*?\"<>|]"), "_")

        val rawFormatted = if (item.uploaderName.isNotBlank() && !item.title.lowercase().startsWith(item.uploaderName.lowercase())) {
            "${item.uploaderName} - ${item.title}"
        } else {
            item.title
        }
        val sanitizedRawFormatted = rawFormatted.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val sanitizedRawTitle = item.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")

        val fileExists = appDirs.any { dir ->
            listOf("m4a", "webm", "mp3").any { ext ->
                File(dir, "$sanitizedParsedTitle.$ext").exists() ||
                File(dir, "$sanitizedRawFormatted.$ext").exists() ||
                File(dir, "$sanitizedRawTitle.$ext").exists() ||
                File(dir, "$cleanTitle.$ext").exists()
            }
        }
        if (fileExists) {
            Log.d("DownloadRepo", "Şarkı zaten diskte mevcut, indirme atlandı: $sanitizedParsedTitle")
            _downloadStates.value = _downloadStates.value + (item.videoId to DownloadProgressInfo(
                videoId = item.videoId,
                title = item.title,
                status = DownloadStatus.COMPLETED,
                progress = 100
            ))
            return null
        }

        val data = Data.Builder()
            .putString(DownloadAudioWorker.KEY_STREAM_URL, streamUrl)
            .putString(DownloadAudioWorker.KEY_AUDIO_URL, streamUrl)
            .putString(DownloadAudioWorker.KEY_TITLE, item.title)
            .putString(DownloadAudioWorker.KEY_ARTIST, item.uploaderName)
            .putString(DownloadAudioWorker.KEY_VIDEO_ID, item.videoId)
            .putString(DownloadAudioWorker.KEY_THUMBNAIL_URL, item.thumbnailUrl)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadAudioWorker>()
            .setInputData(data)
            .addTag("download_${item.videoId}")
            .build()

        _downloadStates.value = _downloadStates.value + (item.videoId to DownloadProgressInfo(
            videoId = item.videoId,
            title = item.title,
            status = DownloadStatus.DOWNLOADING,
            progress = 0
        ))

        workManager.enqueue(request)

        scope.launch {
            getDownloadInfo(request.id).collectLatest { workInfo ->
                if (workInfo == null) return@collectLatest
                val progress = workInfo.progress.getInt("progress", 0)
                val status = when (workInfo.state) {
                    WorkInfo.State.RUNNING -> DownloadStatus.DOWNLOADING
                    WorkInfo.State.ENQUEUED -> DownloadStatus.DOWNLOADING
                    WorkInfo.State.SUCCEEDED -> DownloadStatus.COMPLETED
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> DownloadStatus.FAILED
                    else -> DownloadStatus.IDLE
                }

                _downloadStates.value = _downloadStates.value + (item.videoId to DownloadProgressInfo(
                    videoId = item.videoId,
                    title = item.title,
                    status = status,
                    progress = if (status == DownloadStatus.COMPLETED) 100 else progress
                ))
            }
        }

        return request.id
    }

    fun getDownloadInfo(workId: UUID): Flow<WorkInfo?> {
        return workManager.getWorkInfoByIdFlow(workId)
    }

    fun cancelDownload(videoId: String) {
        if (videoId.isBlank()) return
        workManager.cancelAllWorkByTag("download_$videoId")
        _downloadStates.value = _downloadStates.value - videoId
    }

    fun clearDownloadState(videoId: String, title: String? = null) {
        if (videoId.isBlank() && title.isNullOrBlank()) return
        _downloadStates.value = _downloadStates.value.filter { (key, info) ->
            if (videoId.isNotBlank()) {
                key != videoId && info.videoId != videoId && !info.videoId.contains(videoId)
            } else if (!title.isNullOrBlank()) {
                !info.title.equals(title, ignoreCase = true) && !info.title.contains(title, ignoreCase = true)
            } else {
                true
            }
        }
    }
}
