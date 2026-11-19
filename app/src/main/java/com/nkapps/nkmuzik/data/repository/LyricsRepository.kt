package com.nkapps.nkmuzik.data.repository

import com.nkapps.nkmuzik.data.model.LyricsData
import com.nkapps.nkmuzik.data.model.LrcLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

interface LyricsRepository {
    suspend fun getLyrics(title: String, artist: String): LyricsData
}

@Singleton
class LyricsRepositoryImpl @Inject constructor() : LyricsRepository {

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    private fun getLyricsBaseUrl(): String = "https://lrclib.net/api"

    override suspend fun getLyrics(title: String, artist: String): LyricsData = withContext(Dispatchers.IO) {
        val parsed = com.nkapps.nkmuzik.utils.MediaTitleParser.parse(title, artist)
        val cleanTitle = parsed.title.ifBlank { cleanSearchTerm(title) }
        val cleanArtist = if (parsed.artist != "Sanatçı" && !com.nkapps.nkmuzik.utils.MediaTitleParser.isUploaderChannelName(parsed.artist)) parsed.artist else ""

        if (cleanTitle.isBlank()) {
            return@withContext LyricsData(plainText = "Şarkı sözü bilgisi bulunamadı.")
        }

        // 1. Try LrcLib Direct Get with cleanTitle and cleanArtist
        if (cleanArtist.isNotBlank()) {
            val directResult = fetchLrcLibGet(cleanTitle, cleanArtist)
            if (directResult != null) return@withContext directResult
        }

        // 2. Try LrcLib Search with "$cleanArtist $cleanTitle"
        if (cleanArtist.isNotBlank()) {
            val searchResult = fetchLrcLibSearch("$cleanArtist $cleanTitle".trim())
            if (searchResult != null) return@withContext searchResult
        }

        // 3. Try LrcLib Search with cleanTitle only
        val titleSearchResult = fetchLrcLibSearch(cleanTitle)
        if (titleSearchResult != null) return@withContext titleSearchResult

        // 4. Try with raw title if cleaned version didn't work
        val rawTitle = cleanSearchTerm(title)
        if (rawTitle.isNotBlank() && rawTitle != cleanTitle) {
            val rawSearchResult = fetchLrcLibSearch(rawTitle)
            if (rawSearchResult != null) return@withContext rawSearchResult
        }

        return@withContext LyricsData(isSynced = false, plainText = "Bu şarkı için söz bulunamadı.")
    }

    private fun fetchLrcLibGet(title: String, artist: String): LyricsData? {
        val baseUrl = getLyricsBaseUrl()
        if (baseUrl.isBlank()) return null
        try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val url = "$baseUrl/get?track_name=$encodedTitle&artist_name=$encodedArtist"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "NKMuzik-AndroidApp/1.0 (https://github.com/ndrkykc/NKMuzik)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val syncedLrc = json.optString("syncedLyrics", "")
                val plainLrc = json.optString("plainLyrics", "")

                return processLyricsResponse(syncedLrc, plainLrc)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun fetchLrcLibSearch(query: String): LyricsData? {
        val baseUrl = getLyricsBaseUrl()
        if (baseUrl.isBlank()) return null
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$baseUrl/search?q=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "NKMuzik-AndroidApp/1.0 (https://github.com/ndrkykc/NKMuzik)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return null
                val jsonArray = JSONArray(body)
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(i) ?: continue
                    val syncedLrc = item.optString("syncedLyrics", "")
                    val plainLrc = item.optString("plainLyrics", "")

                    val result = processLyricsResponse(syncedLrc, plainLrc)
                    if (result != null && (result.lines.isNotEmpty() || result.plainText.isNotBlank())) {
                        return result
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun processLyricsResponse(syncedLrc: String, plainLrc: String): LyricsData? {
        if (syncedLrc.isNotBlank()) {
            val parsedLines = parseLrc(syncedLrc)
            if (parsedLines.isNotEmpty()) {
                return LyricsData(
                    isSynced = true,
                    lines = parsedLines,
                    plainText = plainLrc.ifBlank { parsedLines.joinToString("\n") { it.text } }
                )
            }
        }

        if (plainLrc.isNotBlank()) {
            val staticLines = plainLrc.lines().map { LrcLine(0L, it) }
            return LyricsData(
                isSynced = false,
                lines = staticLines,
                plainText = plainLrc
            )
        }

        return null
    }

    private fun parseLrc(lrcContent: String): List<LrcLine> {
        val result = mutableListOf<LrcLine>()
        val lines = lrcContent.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            if (trimmed.startsWith("[ar:", ignoreCase = true) ||
                trimmed.startsWith("[ti:", ignoreCase = true) ||
                trimmed.startsWith("[al:", ignoreCase = true) ||
                trimmed.startsWith("[by:", ignoreCase = true) ||
                trimmed.startsWith("[offset:", ignoreCase = true) ||
                trimmed.startsWith("[length:", ignoreCase = true)
            ) {
                continue
            }

            val tagMatcher = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:[\\.:](\\d{1,3}))?\\]").matcher(trimmed)
            val timestamps = mutableListOf<Long>()
            var lastTagEnd = 0
            while (tagMatcher.find()) {
                val min = tagMatcher.group(1)?.toLongOrNull() ?: 0L
                val sec = tagMatcher.group(2)?.toLongOrNull() ?: 0L
                val fracStr = tagMatcher.group(3) ?: "0"
                val frac = fracStr.toLongOrNull() ?: 0L
                val fracMs = when (fracStr.length) {
                    1 -> frac * 100
                    2 -> frac * 10
                    3 -> frac
                    else -> 0L
                }
                val timestampMs = (min * 60 + sec) * 1000 + fracMs
                timestamps.add(timestampMs)
                lastTagEnd = tagMatcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val text = trimmed.substring(lastTagEnd).trim()
                if (text.isNotBlank()) {
                    for (ts in timestamps) {
                        result.add(LrcLine(timestampMs = ts, text = text))
                    }
                }
            }
        }

        return result.sortedBy { it.timestampMs }
    }

    private fun cleanSearchTerm(term: String): String {
        return term
            .replace(Regex("(?i)\\(official.*?\\)"), "")
            .replace(Regex("(?i)\\[official.*?\\]"), "")
            .replace(Regex("(?i)\\(lyric.*?\\)"), "")
            .replace(Regex("(?i)\\[lyric.*?\\]"), "")
            .replace(Regex("(?i)\\(audio\\)"), "")
            .replace(Regex("(?i)ft\\..*"), "")
            .replace(Regex("(?i)feat\\..*"), "")
            .replace(Regex("(?i)video"), "")
            .trim()
    }
}
