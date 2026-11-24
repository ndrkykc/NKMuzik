package com.nkapps.nkmuzik.downloader

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagOptionSingleton
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Utility for safely embedding cover artwork and metadata (title, artist)
 * directly into downloaded audio files (e.g. M4A 'covr' atom, MP3 ID3v2 APIC frame).
 * Uses pure Java/Kotlin tagging without native C++ binaries.
 */
object ArtworkEmbedder {

    init {
        try {
            // Enable Android workarounds in jaudiotagger to avoid AWT/ImageIO classes
            TagOptionSingleton.getInstance().isAndroid = true
        } catch (_: Throwable) {
            // Ignore initialization configuration errors
        }

        try {
            // Suppress verbose jaudiotagger logging
            Logger.getLogger("org.jaudiotagger").level = Level.OFF
        } catch (_: Throwable) {
            // Ignore logging configuration errors
        }
    }

    /**
     * Embeds JPEG/PNG cover artwork and optional metadata (title, artist) directly into [audioFile].
     *
     * @param audioFile The target audio file (e.g., M4A or MP3).
     * @param artworkFile The image file containing JPEG/PNG cover artwork.
     * @param title Optional track title to embed.
     * @param artist Optional artist name to embed.
     * @return `true` if artwork and metadata were successfully embedded, `false` otherwise.
     */
    fun embedArtwork(
        audioFile: File,
        artworkFile: File?,
        title: String? = null,
        artist: String? = null,
        album: String? = null
    ): Boolean {
        if (!audioFile.exists() || audioFile.length() <= 0) return false

        return try {
            val audioFileObj = AudioFileIO.read(audioFile)
            val tag = audioFileObj.getTagOrCreateAndSetDefault() ?: return false

            if (!title.isNullOrBlank()) {
                try {
                    tag.setField(FieldKey.TITLE, title)
                } catch (_: Throwable) {
                    // Ignore individual title field write failures
                }
            }

            if (!artist.isNullOrBlank()) {
                try {
                    tag.setField(FieldKey.ARTIST, artist)
                } catch (_: Throwable) {
                    // Ignore individual artist field write failures
                }
            }

            if (!album.isNullOrBlank()) {
                try {
                    tag.setField(FieldKey.ALBUM, album)
                } catch (_: Throwable) {
                    // Ignore individual album field write failures
                }
            }

            if (artworkFile != null && artworkFile.exists() && artworkFile.length() > 0) {
                try {
                    tag.deleteArtworkField()
                } catch (_: Throwable) {
                    // Ignore failure when clearing existing artwork
                }

                val artwork = ArtworkFactory.createArtworkFromFile(artworkFile)
                if (artwork != null) {
                    tag.setField(artwork)
                }
            }

            AudioFileIO.write(audioFileObj)
            true
        } catch (_: Throwable) {
            // Safe try-catch wrapper: if tagging fails for any unusual or unsupported format,
            // return false so the caller's audio file remains completely intact and successful.
            false
        }
    }

    /**
     * Overload that embeds JPEG artwork bytes directly into [audioFile].
     */
    fun embedArtwork(
        audioFile: File,
        artworkBytes: ByteArray,
        title: String? = null,
        artist: String? = null,
        album: String? = null
    ): Boolean {
        if (!audioFile.exists() || audioFile.length() <= 0) return false
        if (artworkBytes.isEmpty()) return false

        val tempArtworkFile = try {
            File.createTempFile("artwork_embed_", ".jpg", audioFile.parentFile ?: audioFile.contextCacheDir()).apply {
                writeBytes(artworkBytes)
                deleteOnExit()
            }
        } catch (_: Throwable) {
            return false
        }

        return try {
            embedArtwork(audioFile, tempArtworkFile, title, artist, album)
        } finally {
            try {
                if (tempArtworkFile.exists()) {
                    tempArtworkFile.delete()
                }
            } catch (_: Throwable) {}
        }
    }

    private fun File.contextCacheDir(): File {
        return parentFile ?: File(System.getProperty("java.io.tmpdir") ?: ".")
    }
}
