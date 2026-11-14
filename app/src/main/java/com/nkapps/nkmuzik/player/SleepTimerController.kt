package com.nkapps.nkmuzik.player

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SleepTimerMode {
    object Off : SleepTimerMode
    data class Timed(val totalMinutes: Int, val remainingSeconds: Long) : SleepTimerMode
    object EndOfTrack : SleepTimerMode
}

@Singleton
class SleepTimerController @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    private val _timerMode = MutableStateFlow<SleepTimerMode>(SleepTimerMode.Off)
    val timerMode: StateFlow<SleepTimerMode> = _timerMode.asStateFlow()

    private val _formattedRemainingTime = MutableStateFlow<String?>(null)
    val formattedRemainingTime: StateFlow<String?> = _formattedRemainingTime.asStateFlow()

    private val onFinishListeners = mutableSetOf<() -> Unit>()

    fun addOnFinishListener(listener: () -> Unit) {
        onFinishListeners.add(listener)
    }

    fun removeOnFinishListener(listener: () -> Unit) {
        onFinishListeners.remove(listener)
    }

    fun startTimer(minutes: Int, onFinish: (() -> Unit)? = null) {
        cancelTimer()
        onFinish?.let { addOnFinishListener(it) }
        val totalSeconds = minutes * 60L

        _timerMode.value = SleepTimerMode.Timed(minutes, totalSeconds)
        updateFormattedTime(totalSeconds)

        timerJob = scope.launch {
            var currentSec = totalSeconds
            while (currentSec > 0) {
                delay(1000L)
                currentSec--
                _timerMode.value = SleepTimerMode.Timed(minutes, currentSec)
                updateFormattedTime(currentSec)
            }
            triggerFinish()
        }
    }

    fun setEndOfTrackMode(onFinish: (() -> Unit)? = null) {
        cancelTimer()
        onFinish?.let { addOnFinishListener(it) }
        _timerMode.value = SleepTimerMode.EndOfTrack
        _formattedRemainingTime.value = "Şarkı Sonu"
    }

    fun onTrackEnded() {
        if (_timerMode.value is SleepTimerMode.EndOfTrack) {
            triggerFinish()
        }
    }

    private fun triggerFinish() {
        _timerMode.value = SleepTimerMode.Off
        _formattedRemainingTime.value = null
        val listeners = ArrayList(onFinishListeners)
        onFinishListeners.clear()
        timerJob?.cancel()
        timerJob = null
        for (listener in listeners) {
            try {
                listener.invoke()
            } catch (_: Exception) {}
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        onFinishListeners.clear()
        _timerMode.value = SleepTimerMode.Off
        _formattedRemainingTime.value = null
    }

    private fun updateFormattedTime(seconds: Long) {
        val mins = seconds / 60
        val secs = seconds % 60
        _formattedRemainingTime.value = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }
}
