package com.nkapps.nkmuzik.data.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.nkapps.nkmuzik.data.model.LocalAudioTrack
import com.nkapps.nkmuzik.data.preferences.DeletedTracksStore
import com.nkapps.nkmuzik.utils.ArtworkUtils
import com.nkapps.nkmuzik.utils.MediaTitleParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMediaScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val deletedTracksStore: DeletedTracksStore
) {
    suspend fun scanLocalAudio(): List<LocalAudioTrack> = scanAudioFiles()

    suspend fun scanAudioFiles(): List<LocalAudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<LocalAudioTrack>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val deletedIds = deletedTracksStore.getDeletedTrackIds()
        val deletedPaths = deletedTracksStore.getDeletedFilePaths()

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = if (idColumn != -1) cursor.getLong(idColumn) else 0L
                    val filePath = if (dataColumn != -1) cursor.getString(dataColumn) else null
                    val contentUri = ContentUris.withAppendedId(collection, id)

                    // Fix 1: Check File(filePath).exists(). If File(filePath).exists() returns false, skip!
                    if (!filePath.isNullOrBlank()) {
                        val file = File(filePath)
                        if (!file.exists()) {
                            continue
                        }
                    }

                    // Fix 3: Filter out any path/ID present in the deleted tracks blacklist
                    if (deletedTracksStore.isDeleted(
                            trackId = id.toString(),
                            filePath = filePath,
                            contentUri = contentUri.toString(),
                            deletedIds = deletedIds,
                            deletedPaths = deletedPaths
                        )
                    ) {
                        continue
                    }

                    val rawTitle = if (titleColumn != -1) cursor.getString(titleColumn) else null
                    val rawArtist = if (artistColumn != -1) cursor.getString(artistColumn) else null
                    val parsed = MediaTitleParser.parse(rawTitle, rawArtist)
                    val cleanTitle: String = if (parsed.title.isNotBlank()) parsed.title else "Bilinmeyen Parça"
                    val cleanArtist: String = parsed.artist

                    val rawAlbum = if (albumColumn != -1) cursor.getString(albumColumn) else null
                    val cleanAlbum: String = when {
                        rawAlbum.isNullOrBlank() || rawAlbum == "<unknown>" -> "Bilinmeyen Albüm"
                        else -> rawAlbum
                    }

                    val albumId = if (albumIdColumn != -1) cursor.getLong(albumIdColumn) else -1L
                    val durationMs = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L

                    val fileName = filePath?.let { File(it).nameWithoutExtension }
                    val embeddedArtwork = ArtworkUtils.extractEmbeddedArtwork(context, filePath, contentUri)
                    val localArtwork = ArtworkUtils.findArtworkForTrack(context, fileName)
                        ?: ArtworkUtils.findArtworkForTrack(context, cleanTitle)

                    val artworkUri: Uri? = embeddedArtwork ?: localArtwork

                    tracks.add(
                        LocalAudioTrack(
                            id = id,
                            title = cleanTitle,
                            artist = cleanArtist,
                            album = cleanAlbum,
                            durationMs = durationMs,
                            durationFormatted = formatDuration(durationMs),
                            contentUri = contentUri,
                            artworkUri = artworkUri,
                            filePath = filePath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tracks
    }

    private fun formatDuration(ms: Long): String {
        if (ms <= 0) return "0:00"
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}