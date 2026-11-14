package com.nkapps.nkmuzik.ui.common

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import com.nkapps.nkmuzik.R

data class TrackAudioInfo(
    val title: String,
    val artist: String,
    val duration: String,
    val filePathOrUri: String,
    val format: String,
    val bitrate: String,
    val sampleRate: String,
    val channelCount: String
)

@Composable
fun TrackInfoDialog(
    title: String,
    artist: String,
    duration: String,
    uriString: String,
    filePath: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var audioInfo by remember { mutableStateOf<TrackAudioInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(uriString, filePath) {
        withContext(Dispatchers.IO) {
            audioInfo = extractTrackAudioInfo(context, title, artist, duration, uriString, filePath)
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.info),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.song_info_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                audioInfo?.let { info ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoRow(label = stringResource(R.string.track_title_label), value = info.title)
                        InfoRow(label = stringResource(R.string.track_artist_label), value = info.artist)
                        InfoRow(label = stringResource(R.string.track_duration_label), value = info.duration)
                        InfoRow(label = stringResource(R.string.track_path_label), value = info.filePathOrUri)
                        InfoRow(label = "Format", value = info.format)

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Text(
                            text = "Ses Kalitesi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        InfoRow(label = "Bitrate", value = info.bitrate)
                        InfoRow(label = "Örnekleme Hızı", value = info.sampleRate)
                        InfoRow(label = "Kanal Sayısı", value = info.channelCount)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        SelectionContainer {
            Text(
                text = value.ifBlank { "-" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun extractTrackAudioInfo(
    context: Context,
    title: String,
    artist: String,
    durationStr: String,
    uriStr: String,
    filePath: String?
): TrackAudioInfo {
    var extractedTitle = title
    var extractedArtist = artist
    var extractedDuration = durationStr
    var format = "-"
    var bitrate = "-"
    var sampleRate = "-"
    var channelCount = "Stereo (2 kanal)"

    val retriever = MediaMetadataRetriever()
    var isRetrieverSet = false
    try {
        if (!filePath.isNullOrBlank() && File(filePath).exists()) {
            retriever.setDataSource(filePath)
            isRetrieverSet = true
        } else if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
            retriever.setDataSource(context, Uri.parse(uriStr))
            isRetrieverSet = true
        } else if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
            retriever.setDataSource(uriStr, HashMap<String, String>())
            isRetrieverSet = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    if (isRetrieverSet) {
        try {
            val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            if (!metaTitle.isNullOrBlank()) extractedTitle = metaTitle

            val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
            if (!metaArtist.isNullOrBlank()) extractedArtist = metaArtist

            val metaDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            if (metaDuration != null && metaDuration > 0) {
                val totalSeconds = metaDuration / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                extractedDuration = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
            }

            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            val ext = when {
                !filePath.isNullOrBlank() -> File(filePath).extension.uppercase(Locale.getDefault())
                uriStr.contains(".m4a", ignoreCase = true) -> "M4A"
                uriStr.contains(".mp3", ignoreCase = true) -> "MP3"
                uriStr.contains(".webm", ignoreCase = true) -> "WebM"
                uriStr.contains(".flac", ignoreCase = true) -> "FLAC"
                uriStr.contains(".wav", ignoreCase = true) -> "WAV"
                uriStr.contains(".ogg", ignoreCase = true) -> "OGG"
                else -> ""
            }

            format = when {
                ext.isNotBlank() -> ext
                mimeType?.contains("mp4", ignoreCase = true) == true || mimeType?.contains("m4a", ignoreCase = true) == true -> "M4A"
                mimeType?.contains("mpeg", ignoreCase = true) == true || mimeType?.contains("mp3", ignoreCase = true) == true -> "MP3"
                mimeType?.contains("webm", ignoreCase = true) == true -> "WebM"
                mimeType?.contains("flac", ignoreCase = true) == true -> "FLAC"
                mimeType?.contains("wav", ignoreCase = true) == true -> "WAV"
                mimeType?.contains("ogg", ignoreCase = true) == true -> "OGG"
                !mimeType.isNullOrBlank() -> mimeType.substringAfter("/").uppercase(Locale.getDefault())
                else -> "-"
            }

            val metaBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
            if (metaBitrate != null && metaBitrate > 0) {
                bitrate = "${metaBitrate / 1000} kbps"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val sr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
                if (sr != null && sr > 0) {
                    sampleRate = if (sr >= 1000) "${String.format(Locale.US, "%.1f", sr / 1000.0)} kHz" else "$sr Hz"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    if (sampleRate == "-" || channelCount == "Stereo (2 kanal)") {
        val extractor = MediaExtractor()
        var extractorSet = false
        try {
            if (!filePath.isNullOrBlank() && File(filePath).exists()) {
                extractor.setDataSource(filePath)
                extractorSet = true
            } else if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                extractor.setDataSource(context, Uri.parse(uriStr), null)
                extractorSet = true
            }
            if (extractorSet) {
                for (i in 0 until extractor.trackCount) {
                    val formatMedia = extractor.getTrackFormat(i)
                    val mime = formatMedia.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("audio/")) {
                        if (formatMedia.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            val sr = formatMedia.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            if (sr > 0) {
                                sampleRate = if (sr >= 1000) "${String.format(Locale.US, "%.1f", sr / 1000.0)} kHz" else "$sr Hz"
                            }
                        }
                        if (formatMedia.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            val channels = formatMedia.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            channelCount = when (channels) {
                                1 -> "Mono (1 kanal)"
                                2 -> "Stereo (2 kanal)"
                                else -> "$channels kanal"
                            }
                        }
                        break
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {}
        }
    }

    if (bitrate == "-" && (format == "M4A" || format == "WebM")) {
        bitrate = "128 kbps"
    }
    if (sampleRate == "-") {
        sampleRate = "44.1 kHz"
    }

    val displayLocation = if (!filePath.isNullOrBlank()) filePath else uriStr

    return TrackAudioInfo(
        title = extractedTitle,
        artist = extractedArtist,
        duration = extractedDuration,
        filePathOrUri = displayLocation,
        format = format,
        bitrate = bitrate,
        sampleRate = sampleRate,
        channelCount = channelCount
    )
}
