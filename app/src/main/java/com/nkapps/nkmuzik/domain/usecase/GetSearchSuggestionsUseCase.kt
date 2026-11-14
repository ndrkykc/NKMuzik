package com.nkapps.nkmuzik.domain.usecase

import com.nkapps.nkmuzik.data.repository.OnlineSearchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetSearchSuggestionsUseCase @Inject constructor(
    private val onlineSearchRepository: OnlineSearchRepository
) {

    suspend fun execute(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        return onlineSearchRepository.getSearchCompletions(query)
    }
}
