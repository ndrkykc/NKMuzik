package com.nkapps.nkmuzik.data.model

data class LrcLine(
    val timestampMs: Long,
    val text: String
)

data class LyricsData(
    val isSynced: Boolean = false,
    val lines: List<LrcLine> = emptyList(),
    val plainText: String = ""
)
