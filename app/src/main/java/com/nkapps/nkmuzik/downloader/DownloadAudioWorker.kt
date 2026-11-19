package com.nkapps.nkmuzik.downloader

import com.nkapps.nkmuzik.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.ServiceInfo
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nkapps.nkmuzik.data.repository.OnlineSearchRepositoryImpl
import com.nkapps.nkmuzik.data.preferences.ThemePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

class DownloadAudioWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo("İndirmeye hazırlanıyor...", 0)
    }

    private fun createForegroundInfo(progressText: String, progress: Int): ForegroundInfo {
        val channelId = "download_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Müzik İndirme",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("NK Müzik İndirici")
            .setContentText(progressText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                1337,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(1337, notification)
        }
    }

    private fun showCompletionNotification(title: String, success: Boolean) {
        val channelId = "download_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Müzik İndirme",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationText = if (success) "İndirme Tamamlandı: $title" else "İndirme Başarısız: $title"

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("NK Müzik İndirici")
            .setContentText(notificationText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val initialAudioUrl = inputData.getString(KEY_STREAM_URL)
            ?: inputData.getString(KEY_AUDIO_URL)
            ?: run {
                Log.e("DownloadWorker", "Hata: STREAM_URL veya AUDIO_URL verisi boş!")
                return@withContext Result.failure()
            }
        val title = inputData.getString(KEY_TITLE) ?: "audio_track"
        val artist = inputData.getString(KEY_ARTIST) ?: ""
        val videoId = inputData.getString(KEY_VIDEO_ID) ?: ""
        val thumbnailUrl = inputData.getString(KEY_THUMBNAIL_URL) ?: inputData.getString(KEY_THUMBNAIL_URI) ?: ""
        
        Log.d("DownloadWorker", "doWork başlatıldı -> Title: '$title', VideoId: '$videoId', URL: ${initialAudioUrl.take(60)}...")

        val parsed = com.nkapps.nkmuzik.utils.MediaTitleParser.parse(title, artist.ifBlank { null })
        val cleanTitle = parsed.title
        val cleanArtist = parsed.artist
        val formattedName = if (cleanArtist.isNotBlank() && !cleanTitle.lowercase().startsWith(cleanArtist.lowercase())) {
            "$cleanArtist - $cleanTitle"
        } else {
            cleanTitle
        }
        val sanitizedTitle = formattedName.replace(Regex("[\\\\/:*?\"<>|]"), "_")

        // Android 12/13/14 Foreground Service kısıtlamalarına takılmamak için 
        // kısa süreli indirme işleminde setForeground çağrısını devre dışı bırakıyoruz.

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        var currentUrl = initialAudioUrl

        // Try downloading initial URL
        var response: Response? = try {
            Log.d("DownloadWorker", "İndirme isteği atılıyor -> $currentUrl")
            executeDownloadRequest(client, currentUrl)
        } catch (e: Exception) {
            Log.e("DownloadWorker", "İlk indirme isteği başarısız: ${e.message}")
            null
        }

        // If response is invalid or expired (e.g. 403, 404, 410, HTML error page), try re-extracting fresh URL
        if ((response == null || !isValidAudioResponse(response)) && videoId.isNotBlank()) {
            Log.w("DownloadWorker", "İlk URL geçersiz veya süresi dolmuş. Yeniden NewPipeExtractor ile taze URL alınıyor...")
            response?.close()
            response = null
            try {
                val themePrefs = ThemePreferences(applicationContext)
                val repository = OnlineSearchRepositoryImpl(themePrefs)
                val freshInfo = repository.getAudioStreamUrl(videoId).getOrNull()
                if (freshInfo != null && freshInfo.audioUrl.isNotBlank()) {
                    currentUrl = freshInfo.audioUrl
                    Log.d("DownloadWorker", "Yeni taze URL alındı -> $currentUrl")
                    response = executeDownloadRequest(client, currentUrl)
                }
            } catch (e: Exception) {
                Log.e("DownloadWorker", "Yeniden taze URL alma başarısız: ${e.message}")
            }
        }

        val activeResponse = response
        if (activeResponse == null || !isValidAudioResponse(activeResponse)) {
            val code = activeResponse?.code ?: -1
            Log.e("DownloadWorker", "İndirme başarısız. HTTP Kod: $code")
            activeResponse?.close()
            showCompletionNotification(title, false)
            return@withContext Result.failure()
        }

        val body = activeResponse.body
        if (body == null) {
            Log.e("DownloadWorker", "HTTP Yanıt Vücudu (body) boş!")
            activeResponse.close()
            showCompletionNotification(title, false)
            return@withContext Result.failure()
        }

        val contentLength = body.contentLength()
        val contentType = activeResponse.header("Content-Type") ?: ""
        Log.d("DownloadWorker", "İndirme başladı! Content-Type: $contentType, Content-Length: $contentLength")
        val extension = if (contentType.contains("webm", true) || contentType.contains("opus", true)) "webm" else "m4a"
        val fileName = "$sanitizedTitle.$extension"
        val mimeType = if (extension == "webm") "audio/x-matroska" else "audio/mp4"

        var fileUri: Uri? = null
        var targetFile: File? = null
        var finalFilePath: String? = null
        var bytesCopied: Long = 0
        var isSuccess = false

        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        targetFile = File(musicDir, fileName)
        val tempAudioFile = File(applicationContext.cacheDir, "temp_$fileName")

        try {
            FileOutputStream(tempAudioFile).use { out ->
                val inputStream = body.byteStream()
                val buffer = ByteArray(128 * 1024) // 128KB buffer for maximum throughput
                var lastProgress = -1
                var lastProgressTime = 0L

                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isStopped) {
                        throw InterruptedIOException("Worker stopped by system")
                    }
                    out.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead

                    if (contentLength > 0) {
                        val progress = ((bytesCopied * 100) / contentLength).coerceIn(0, 100).toInt()
                        val currentTime = System.currentTimeMillis()
                        // Throttle WorkManager progress updates to at most once per 250ms or at 100%
                        if (progress != lastProgress && (currentTime - lastProgressTime >= 250 || progress == 100)) {
                            lastProgress = progress
                            lastProgressTime = currentTime
                            setProgress(
                                workDataOf(
                                    "progress" to progress,
                                    KEY_VIDEO_ID to videoId,
                                    KEY_TITLE to title
                                )
                            )
                        }
                    }
                }
                out.flush()
            }

            // Download temporary thumbnail cover art image if available into cacheDir
            var tempArtworkFile: File? = downloadTemporaryThumbnail(client, thumbnailUrl, videoId, applicationContext.cacheDir)

            // Embed cover art and metadata into tempAudioFile using ArtworkEmbedder
            if (tempAudioFile.exists() && tempAudioFile.length() > 0 && tempArtworkFile != null && tempArtworkFile.exists()) {
                try {
                    ArtworkEmbedder.embedArtwork(
                        audioFile = tempAudioFile,
                        artworkFile = tempArtworkFile,
                        title = title,
                        artist = artist
                    )
                } catch (e: Exception) {
                    Log.w("DownloadAudioWorker", "ID3 Tagging failed for $title: ${e.message}")
                    // Ignore tagging errors so audio download remains completely intact and successful
                } finally {
                    try {
                        if (tempArtworkFile.exists()) {
                            tempArtworkFile.delete()
                        }
                    } catch (_: Exception) {}
                    tempArtworkFile = null
                }
            }

            // Ensure no legacy .jpg image files remain in Music folder
            cleanLegacyJpgFiles(musicDir, videoId, sanitizedTitle)

            // Write tagged audio file directly to destination in Music folder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = applicationContext.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                    put(MediaStore.Audio.Media.IS_MUSIC, 1)
                }
                fileUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (fileUri != null) {
                    resolver.openOutputStream(fileUri)?.use { outputStream ->
                        tempAudioFile.inputStream().use { input ->
                            input.copyTo(outputStream)
                        }
                    }
                    try {
                        resolver.query(fileUri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                                if (dataIdx != -1) {
                                    finalFilePath = cursor.getString(dataIdx)
                                }
                            }
                        }
                    } catch (_: Exception) {}
                    isSuccess = true
                }
            } else {
                val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                if (!musicDir.exists()) musicDir.mkdirs()
                targetFile = File(musicDir, fileName)
                targetFile?.let { tFile ->
                    tempAudioFile.copyTo(tFile, overwrite = true)
                    finalFilePath = tFile.absolutePath
                    isSuccess = tFile.exists() && tFile.length() > 0
                }
            }

            // Verify anti-corruption: file must be > 0 bytes and match contentLength if known
            if (bytesCopied > 0 && (contentLength <= 0 || bytesCopied == contentLength)) {
                isSuccess = true
            }
        } catch (e: Exception) {
            Log.e("DownloadAudioWorker", "Download error: ${e.message}", e)
            isSuccess = false
        } finally {
            activeResponse.close()
            try {
                if (tempAudioFile.exists()) {
                    tempAudioFile.delete()
                }
            } catch (_: Exception) {}
        }

        if (!isSuccess || bytesCopied <= 0) {
            // Cleanup incomplete or corrupted files
            fileUri?.let { applicationContext.contentResolver.delete(it, null, null) }
            targetFile?.let { if (it.exists()) it.delete() }
            showCompletionNotification(title, false)
            return@withContext Result.failure()
        }

        // File successfully downloaded and verified -> set MediaStore IS_PENDING = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && fileUri != null) {
            val updateValues = ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            applicationContext.contentResolver.update(fileUri, updateValues, null, null)
        }

        val scanPath = finalFilePath ?: targetFile?.absolutePath
        if (scanPath != null) {
            MediaScannerConnection.scanFile(
                applicationContext,
                arrayOf(scanPath),
                arrayOf(mimeType),
                null
            )
        }

        showCompletionNotification(title, true)

        val finalAudioUri = fileUri?.toString() ?: finalFilePath ?: targetFile?.absolutePath ?: ""

        Result.success(
            workDataOf(
                KEY_FILE_PATH to finalAudioUri,
                KEY_VIDEO_ID to videoId,
                KEY_TITLE to title,
                KEY_ARTWORK_URI to finalAudioUri
            )
        )
    }

    private fun downloadTemporaryThumbnail(
        client: OkHttpClient,
        thumbnailUrl: String,
        videoId: String,
        cacheDir: File
    ): File? {
        if (thumbnailUrl.isBlank()) return null
        try {
            val request = Request.Builder()
                .url(thumbnailUrl)
                .header("User-Agent", BROWSER_USER_AGENT)
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body
                val contentType = response.header("Content-Type") ?: ""
                if (body != null && (contentType.isBlank() || contentType.contains("image", ignoreCase = true))) {
                    val bytes = body.bytes()
                    response.close()
                    if (bytes.isNotEmpty()) {
                        val tempFile = File(cacheDir, "temp_art_${videoId.ifBlank { "thumb" }}_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(tempFile).use { out -> out.write(bytes) }
                        if (tempFile.exists() && tempFile.length() > 0) {
                            return tempFile
                        }
                    }
                } else {
                    response.close()
                }
            } else {
                response.close()
            }
        } catch (_: Exception) {
            // Failed to download thumbnail
        }
        return null
    }

    private fun cleanLegacyJpgFiles(appDir: File, videoId: String, sanitizedTitle: String) {
        try {
            if (!appDir.exists()) return
            val candidates = mutableListOf<String>()
            if (videoId.isNotBlank()) {
                candidates.add("art_$videoId.jpg")
                candidates.add("art_$videoId.png")
                candidates.add("$videoId.jpg")
                candidates.add("$videoId.png")
            }
            if (sanitizedTitle.isNotBlank()) {
                candidates.add("$sanitizedTitle.jpg")
                candidates.add("$sanitizedTitle.png")
            }
            for (fName in candidates) {
                val f = File(appDir, fName)
                if (f.exists()) {
                    f.delete()
                }
            }
        } catch (_: Exception) {}
    }

    private fun executeDownloadRequest(client: OkHttpClient, url: String): Response {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "*/*")
            .header("Accept-Encoding", "identity")
            .header("Range", "bytes=0-")
            .header("Connection", "keep-alive")
            .build()
        return client.newCall(request).execute()
    }

    private fun isValidAudioResponse(response: Response): Boolean {
        if (!response.isSuccessful) return false // Must be HTTP 200..299 or 206
        val contentType = response.header("Content-Type") ?: ""
        return !(contentType.contains("text/html", ignoreCase = true) ||
                contentType.contains("text/xml", ignoreCase = true) ||
                contentType.contains("application/json", ignoreCase = true) ||
                contentType.contains("html", ignoreCase = true))
    }

    companion object {
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        const val KEY_VIDEO_ID = "video_id"
        const val KEY_AUDIO_URL = "audio_url"
        const val KEY_STREAM_URL = "stream_url"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_THUMBNAIL_URL = "thumbnail_url"
        const val KEY_THUMBNAIL_URI = "thumbnail_uri"
        const val KEY_ARTWORK_URI = "artwork_uri"
    }
}
