package com.nkapps.nkmuzik.domain.usecase

import com.nkapps.nkmuzik.data.model.LocalTrack
import com.nkapps.nkmuzik.ui.search.calculateLocalTrackSearchScore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchLocalTracksUseCase @Inject constructor() {

    fun execute(localTracks: List<LocalTrack>, query: String): List<LocalTrack> {
        if (query.isBlank()) return localTracks
        return localTracks
            .map { track -> track to calculateLocalTrackSearchScore(track, query) }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (track, _) -> track }
    }
}
