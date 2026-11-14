package com.nkapps.nkmuzik.data.model

data class OnlineSearchResultDto(
    val videoId: String,
    val title: String,
    val uploaderName: String,
    val durationString: String,
    val thumbnailUrl: String,
    val url: String,
    val description: String = ""
)

data class AudioStreamInfo(
    val audioUrl: String,
    val format: String = "M4A",
    val container: String = "m4a",
    val quality: String = "128 kbps",
    val bitrate: Int = 128000,
    val title: String = "",
    val uploaderName: String = "",
    val thumbnailUrl: String = "",
    val headers: Map<String, String> = emptyMap()
)
