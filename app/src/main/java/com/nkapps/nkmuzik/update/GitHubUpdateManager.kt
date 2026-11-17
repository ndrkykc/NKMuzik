package com.nkapps.nkmuzik.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.nkapps.nkmuzik.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class GitHubRelease(
    val tagName: String,
    val title: String,
    val releaseNotes: String,
    val apkUrl: String
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val release: GitHubRelease) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data class ReadyToInstall(val apkFile: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@Singleton
class GitHubUpdateManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GitHubUpdateManager"

        // Varsayılan GitHub Repository Sahibi ve Adı
        var GITHUB_OWNER = "ndrkykc"
        var GITHUB_REPO = "NKMuzik"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    suspend fun checkForUpdates(owner: String = GITHUB_OWNER, repo: String = GITHUB_REPO, showToastIfUpToDate: Boolean = false) = withContext(Dispatchers.IO) {
        if (_updateState.value is UpdateState.Checking || _updateState.value is UpdateState.Downloading) return@withContext
        _updateState.value = UpdateState.Checking

        try {
            val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "NK-Muzik-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "GitHub release fetch failed with code: ${response.code}")
                    _updateState.value = UpdateState.Idle
                    if (showToastIfUpToDate) {
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Güncelleme kontrolü yapılamadı", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    return@withContext
                }

                val bodyStr = response.body?.string() ?: run {
                    _updateState.value = UpdateState.Idle
                    return@withContext
                }

                val json = JSONObject(bodyStr)
                val rawTagName = json.optString("tag_name", "")
                val cleanTag = rawTagName.trimStart('v', 'V').trim()
                val releaseName = json.optString("name", rawTagName)
                val releaseNotes = json.optString("body", "Yeni performans iyileştirmeleri ve hata düzeltmeleri.")

                var apkDownloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                val currentVersion = BuildConfig.VERSION_NAME.trimStart('v', 'V').trim()
                Log.d(TAG, "Mevcut Versiyon: '$currentVersion', GitHub Son Versiyon: '$cleanTag'")

                if (isNewerVersion(cleanTag, currentVersion) && apkDownloadUrl.isNotBlank()) {
                    val release = GitHubRelease(
                        tagName = rawTagName,
                        title = releaseName,
                        releaseNotes = releaseNotes,
                        apkUrl = apkDownloadUrl
                    )
                    _updateState.value = UpdateState.UpdateAvailable(release)
                } else {
                    _updateState.value = UpdateState.Idle
                    if (showToastIfUpToDate) {
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Uygulamanız güncel (v${BuildConfig.VERSION_NAME})", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "GitHub güncelleme kontrolü sırasında hata: ${e.message}", e)
            _updateState.value = UpdateState.Idle
            if (showToastIfUpToDate) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Uygulamanız güncel (v${BuildConfig.VERSION_NAME})", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        if (latest.isBlank()) return false
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    suspend fun downloadAndInstallUpdate(release: GitHubRelease) = withContext(Dispatchers.IO) {
        if (release.apkUrl.isBlank()) return@withContext
        _updateState.value = UpdateState.Downloading(0)

        try {
            val request = Request.Builder()
                .url(release.apkUrl)
                .header("User-Agent", "NK-Muzik-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful || response.body == null) {
                    _updateState.value = UpdateState.Error("İndirme başarısız oldu (Hata Kodu: ${response.code})")
                    return@withContext
                }

                val body = response.body!!
                val contentLength = body.contentLength()
                val targetDir = context.externalCacheDir ?: context.cacheDir
                val apkFile = File(targetDir, "NK_Muzik_Update_${release.tagName}.apk")

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                val progress = ((totalRead * 100) / contentLength).toInt()
                                _updateState.value = UpdateState.Downloading(progress)
                            }
                        }
                    }
                }

                _updateState.value = UpdateState.ReadyToInstall(apkFile)
                installApk(apkFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "APK indirme hatası: ${e.message}", e)
            _updateState.value = UpdateState.Error("İndirme sırasında bir sorun oluştu.")
        }
    }

    fun installApk(apkFile: File) {
        if (!apkFile.exists()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
            }
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "APK yükleyici başlatılamadı: ${e.message}", e)
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
    }
}
