package com.nkapps.nkmuzik.domain.usecase

import com.nkapps.nkmuzik.data.model.LocalTrack
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.data.repository.OnlineSearchRepository
import com.nkapps.nkmuzik.ui.search.calculateOnlineTrackSearchScore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchOnlineTracksUseCase @Inject constructor(
    private val onlineSearchRepository: OnlineSearchRepository
) {

    fun execute(query: String, localTracks: List<LocalTrack> = emptyList()): Flow<List<OnlineSearchResultDto>> {
        return onlineSearchRepository.searchOnline(query).map { results ->
            if (query.isBlank()) return@map results
            results
                .map { dto -> dto to calculateOnlineTrackSearchScore(dto, query, localTracks) }
                .sortedByDescending { (_, score) -> score }
                .map { (dto, _) -> dto }
        }
    }
}
