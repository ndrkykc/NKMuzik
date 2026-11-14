package com.nkapps.nkmuzik.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.data.repository.PlaylistRepository
import com.nkapps.nkmuzik.player.SleepTimerController
import com.nkapps.nkmuzik.player.SleepTimerMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val sleepTimerController: SleepTimerController
) : ViewModel() {

    private val _currentPlayingTrack = MutableStateFlow<OnlineSearchResultDto?>(null)
    val currentPlayingTrack: StateFlow<OnlineSearchResultDto?> = _currentPlayingTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _queueItems = MutableStateFlow<List<OnlineSearchResultDto>>(emptyList())
    val queueItems: StateFlow<List<OnlineSearchResultDto>> = _queueItems.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    val sleepTimerMode: StateFlow<SleepTimerMode> = sleepTimerController.timerMode
    val sleepTimerFormatted: StateFlow<String?> = sleepTimerController.formattedRemainingTime

    fun updatePlayingState(
        track: OnlineSearchResultDto?,
        playing: Boolean,
        position: Long,
        trackDuration: Long
    ) {
        _currentPlayingTrack.value = track
        _isPlaying.value = playing
        _currentPosition.value = position
        _duration.value = trackDuration
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun toggleRepeat() {
        _repeatMode.value = (_repeatMode.value + 1) % 3
    }

    fun toggleFavorite(track: OnlineSearchResultDto) {
        viewModelScope.launch {
            playlistRepository.toggleFavorite(track)
        }
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerController.startTimer(minutes)
    }

    fun setSleepTimerEndOfTrack() {
        sleepTimerController.setEndOfTrackMode()
    }

    fun cancelSleepTimer() {
        sleepTimerController.cancelTimer()
    }
}
