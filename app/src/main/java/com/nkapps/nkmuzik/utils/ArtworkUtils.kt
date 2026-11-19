package com.nkapps.nkmuzik.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Coil Transformation that crops the exact center square of the image.
 */
class SquareCropTransformation : Transformation {
    override val cacheKey: String = "SquareCropTransformation"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        return ArtworkUtils.cropCenterSquare(input)
    }
}

typealias OnlineSquareCropTransformation = SquareCropTransformation

object ArtworkUtils {

    /**
     * Crops a bitmap to a 1:1 center square using the smallest dimension.
     */
    fun cropCenterSquare(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= 0 || bitmap.height <= 0) return bitmap

        val cropSize = minOf(bitmap.width, bitmap.height)
        if (cropSize <= 0) return bitmap
        if (bitmap.width == cropSize && bitmap.height == cropSize) return bitmap

        val startX = (bitmap.width - cropSize) / 2
        val startY = (bitmap.height - cropSize) / 2

        return Bitmap.createBitmap(bitmap, startX, startY, cropSize, cropSize)
    }

    fun parseDurationSeconds(durationString: String?): Long {
        if (durationString.isNullOrBlank()) return 0L
        val parts = durationString.trim().split(":")
        return try {
            when (parts.size) {
                3 -> parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()
                2 -> parts[0].toLong() * 60 + parts[1].toLong()
                1 -> parts[0].toLong()
                else -> 0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    fun isMusicTrack(title: String?, uploaderName: String?, durationString: String?): Boolean {
        return true
    }

    /**
     * Decodes a byte array, applies 1:1 center-square cropping, and re-encodes as a JPEG byte array.
     */
    fun cropCenterSquareBytes(imageBytes: ByteArray, quality: Int = 90): ByteArray? {
        if (imageBytes.isEmpty()) return null
        val original = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return null
        val cropped = cropCenterSquare(original)
        val stream = ByteArrayOutputStream()
        val success = cropped.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        if (cropped != original) {
            cropped.recycle()
        }
        original.recycle()
        return if (success) stream.toByteArray() else null
    }

    @Suppress("UNUSED_PARAMETER")
    fun getDirectOnlineThumbnailUrl(videoId: String): String = ""

    @Suppress("UNUSED_PARAMETER")
    fun getDirectStreamThumbnailUrl(videoId: String): String = ""

    /**
     * Extracts cover artwork directly from embedded media metadata using MediaMetadataRetriever.embeddedPicture.
     * Caches the image bytes to a temporary cache file in context.cacheDir and returns its Uri.
     */
    fun extractEmbeddedArtwork(context: Context, filePath: String?, contentUri: Uri? = null): Uri? {
        if (filePath.isNullOrBlank() && contentUri == null) return null
        return try {
            val retriever = MediaMetadataRetriever()
            try {
                if (!filePath.isNullOrBlank() && File(filePath).exists()) {
                    retriever.setDataSource(filePath)
                } else if (contentUri != null) {
                    retriever.setDataSource(context, contentUri)
                } else {
                    return null
                }
                val pictureBytes = retriever.embeddedPicture
                if (pictureBytes != null && pictureBytes.isNotEmpty()) {
                    val key = (filePath ?: contentUri.toString()).hashCode()
                    val cacheFile = File(context.cacheDir, "embedded_art_${key}.jpg")
                    if (!cacheFile.exists() || cacheFile.length() != pictureBytes.size.toLong()) {
                        cacheFile.writeBytes(pictureBytes)
                    }
                    Uri.fromFile(cacheFile)
                } else {
                    null
                }
            } finally {
                retriever.release()
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resolves raw image model (Uri, File, or String URL) for a track.
     * Checks embedded media artwork or local downloaded artwork files first before falling back to remote thumbnail URLs.
     */
    fun getRawArtworkModel(
        context: Context,
        uriOrUrl: String?,
        title: String? = null,
        videoId: String? = null,
        durationString: String? = null,
        uploaderName: String? = null
    ): Any? {
        // If uriOrUrl is provided, check if it points to a local file or content Uri containing embedded picture
        if (!uriOrUrl.isNullOrBlank()) {
            val rawPath = when {
                uriOrUrl.startsWith("file://") -> uriOrUrl.removePrefix("file://")
                uriOrUrl.startsWith("/") -> uriOrUrl
                else -> null
            }

            if (rawPath != null) {
                val file = File(rawPath)
                if (file.exists() && file.length() > 0) {
                    if (file.extension.equals("jpg", true) || file.extension.equals("jpeg", true) || file.extension.equals("png", true)) {
                        return Uri.fromFile(file)
                    }
                    val embedded = extractEmbeddedArtwork(context, rawPath)
                    if (embedded != null) {
                        return embedded
                    }
                }
            }

            if (uriOrUrl.startsWith("content://")) {
                val parsed = try { Uri.parse(uriOrUrl) } catch (_: Exception) { null }
                if (parsed != null) {
                    val embedded = extractEmbeddedArtwork(context, null, parsed)
                    if (embedded != null) {
                        return embedded
                    }
                    if (!uriOrUrl.contains("albumart")) {
                        return parsed
                    }
                }
            }

            if (uriOrUrl.startsWith("http://") || uriOrUrl.startsWith("https://")) {
                return uriOrUrl
            }
        }

        // Check if a local downloaded artwork file exists on disk
        val localArtUri = findArtworkForTrack(context, title, videoId)
        if (localArtUri != null) {
            return localArtUri
        }

        // Fallback to direct online thumbnail if videoId exists
        if (!videoId.isNullOrBlank()) {
            return getDirectOnlineThumbnailUrl(videoId)
        }

        return uriOrUrl.takeIf { !it.isNullOrBlank() }
    }

    /**
     * Resolves the Coil ImageRequest for a track, applying SquareCropTransformation
     * so that all Coil image requests automatically strip side and top/bottom bars.
     * If no artwork is found or image fails to load, falls back to standard default cover image.
     */
    fun getArtworkModel(
        context: Context,
        uriOrUrl: String?,
        title: String? = null,
        videoId: String? = null,
        durationString: String? = null,
        uploaderName: String? = null,
        fallbackDrawableRes: Int = com.nkapps.nkmuzik.ui.player.DefaultArtworkConfig.getFallbackDrawableRes(title, videoId)
    ): ImageRequest {
        val rawModel = getRawArtworkModel(context, uriOrUrl, title, videoId, durationString, uploaderName) ?: fallbackDrawableRes
        return ImageRequest.Builder(context)
            .data(rawModel)
            .placeholder(fallbackDrawableRes)
            .error(fallbackDrawableRes)
            .fallback(fallbackDrawableRes)
            .transformations(SquareCropTransformation())
            .crossfade(true)
            .build()
    }

    fun parseArtworkModel(uriOrUrl: String?): Any? {
        if (uriOrUrl.isNullOrBlank()) return null
        return when {
            uriOrUrl.startsWith("http://") || uriOrUrl.startsWith("https://") -> uriOrUrl
            uriOrUrl.startsWith("content://") || uriOrUrl.startsWith("file://") -> {
                try { Uri.parse(uriOrUrl) } catch (_: Exception) { uriOrUrl }
            }
            uriOrUrl.startsWith("/") -> {
                val f = File(uriOrUrl)
                if (f.exists() && f.length() > 0) Uri.fromFile(f) else uriOrUrl
            }
            else -> uriOrUrl
        }
    }

    fun parseArtworkUri(uriOrUrl: String?): Uri? {
        if (uriOrUrl.isNullOrBlank()) return null
        return when {
            uriOrUrl.startsWith("http://") || uriOrUrl.startsWith("https://") ||
            uriOrUrl.startsWith("content://") || uriOrUrl.startsWith("file://") -> {
                try { Uri.parse(uriOrUrl) } catch (_: Exception) { null }
            }
            uriOrUrl.startsWith("/") -> {
                try {
                    val f = File(uriOrUrl)
                    if (f.exists()) Uri.fromFile(f) else null
                } catch (_: Exception) { null }
            }
            else -> try { Uri.parse(uriOrUrl) } catch (_: Exception) { null }
        }
    }

    fun findArtworkForTrack(context: Context, title: String?, videoId: String? = null): Uri? {
        val candidates = mutableListOf<String>()
        if (!videoId.isNullOrBlank()) {
            candidates.add("art_$videoId.jpg")
            candidates.add("art_$videoId.png")
            candidates.add("$videoId.jpg")
        }
        if (!title.isNullOrBlank()) {
            val sanitized = title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            candidates.add("$sanitized.jpg")
            candidates.add("$sanitized.png")
            candidates.add("$title.jpg")
            candidates.add("$title.png")
        }

        if (candidates.isEmpty()) return null

        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val dirs = listOfNotNull(
            File(musicDir, "NK Müzik"),
            File(musicDir, "NK Muzik")
        )

        for (dir in dirs) {
            if (!dir.exists() || !dir.isDirectory) continue
            for (candidate in candidates) {
                val file = File(dir, candidate)
                if (file.exists() && file.length() > 0) {
                    return Uri.fromFile(file)
                }
            }
        }
        return null
    }
}
