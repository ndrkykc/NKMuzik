package com.nkapps.nkmuzik.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.data.remote.RemoteConfigManager
import com.nkapps.nkmuzik.data.preferences.ThemePreferences
import com.nkapps.nkmuzik.data.repository.OnlineSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class OnlineSearchViewModel @Inject constructor(
    private val searchRepository: OnlineSearchRepository,
    private val remoteConfigManager: RemoteConfigManager,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    val enableOnlineFeatures: StateFlow<Boolean> = remoteConfigManager.enableOnlineFeatures

    val onlineTabLabel: StateFlow<String> = remoteConfigManager.onlineTabLabel

    private val _onlineQuery = MutableStateFlow("")
    val onlineQuery: StateFlow<String> = _onlineQuery.asStateFlow()

    private val _searchCompletions = MutableStateFlow<List<String>>(emptyList())
    val searchCompletions: StateFlow<List<String>> = _searchCompletions.asStateFlow()

    private val _showSuggestions = MutableStateFlow(false)
    val showSuggestions: StateFlow<Boolean> = _showSuggestions.asStateFlow()

    private val _onlineResults = MutableStateFlow<List<OnlineSearchResultDto>>(emptyList())
    val onlineResults: StateFlow<List<OnlineSearchResultDto>> = _onlineResults.asStateFlow()

    private val _isSearchingOnline = MutableStateFlow(false)
    val isSearchingOnline: StateFlow<Boolean> = _isSearchingOnline.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        _onlineQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotBlank() && _showSuggestions.value) {
                    _searchCompletions.value = searchRepository.getSearchCompletions(query)
                } else {
                    _searchCompletions.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(query: String) {
        _onlineQuery.value = query
        _showSuggestions.value = true
    }

    fun hideSuggestions() {
        _showSuggestions.value = false
    }

    fun performSearch(query: String) {
        if (query.isBlank()) return
        _onlineQuery.value = query
        _showSuggestions.value = false
        _isSearchingOnline.value = true
        _uiState.value = SearchUiState.Loading

        viewModelScope.launch {
            try {
                searchRepository.searchOnline(query).collect { results ->
                    _onlineResults.value = results
                    _isSearchingOnline.value = false
                    _uiState.value = if (results.isEmpty()) {
                        SearchUiState.Error("Sonuç bulunamadı.")
                    } else {
                        SearchUiState.Success(results)
                    }
                }
            } catch (e: Exception) {
                _isSearchingOnline.value = false
                _uiState.value = SearchUiState.Error(e.message ?: "Arama hatası")
            }
        }
    }
}
