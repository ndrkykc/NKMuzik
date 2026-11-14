package com.nkapps.nkmuzik.data.model

data class LocalTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String = "Bilinmeyen Albüm",
    val duration: String,
    val uri: String,
    val artworkUri: String? = null,
    val filePath: String? = null,
    val videoId: String? = null,
    val isOnlineStream: Boolean = uri.startsWith("http://") || uri.startsWith("https://")
)
