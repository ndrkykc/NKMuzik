package com.nkapps.nkmuzik.domain.usecase

import com.nkapps.nkmuzik.data.model.LocalTrack
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.ui.search.calculateLocalTrackSearchScore
import com.nkapps.nkmuzik.ui.search.calculateOnlineTrackSearchScore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RankSearchResultsUseCase @Inject constructor() {

    fun rankLocal(tracks: List<LocalTrack>, query: String): List<LocalTrack> {
        if (query.isBlank()) return tracks
        return tracks
            .map { it to calculateLocalTrackSearchScore(it, query) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    fun rankOnline(tracks: List<OnlineSearchResultDto>, query: String, localTracks: List<LocalTrack> = emptyList()): List<OnlineSearchResultDto> {
        if (query.isBlank()) return tracks
        return tracks
            .map { it to calculateOnlineTrackSearchScore(it, query, localTracks) }
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
