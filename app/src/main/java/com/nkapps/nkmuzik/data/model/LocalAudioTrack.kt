package com.nkapps.nkmuzik.data.model

import android.net.Uri

data class LocalAudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val durationFormatted: String,
    val contentUri: Uri,
    val artworkUri: Uri?,
    val filePath: String? = null
)
