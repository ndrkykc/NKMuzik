package com.nkapps.nkmuzik.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nkapps.nkmuzik.data.analytics.AnalyticsManager
import com.nkapps.nkmuzik.data.preferences.ThemePreferences
import com.nkapps.nkmuzik.player.AudioFxManager
import com.nkapps.nkmuzik.player.AudioFxState
import com.nkapps.nkmuzik.player.SleepTimerController
import com.nkapps.nkmuzik.player.SleepTimerMode
import com.nkapps.nkmuzik.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.nkapps.nkmuzik.utils.LanguageManager

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
    private val audioFxManager: AudioFxManager,
    private val sleepTimerController: SleepTimerController,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = themePreferences.initialThemeMode
    )

    val dynamicColor: StateFlow<Boolean> = themePreferences.dynamicColor.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val isCoverDynamicColorEnabled: StateFlow<Boolean> = themePreferences.isCoverDynamicColorEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val language: StateFlow<String> = themePreferences.language.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LanguageManager.getLanguage()
    )

    val isOnlineUnlocked: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    val audioFxState: StateFlow<AudioFxState> = audioFxManager.fxState

    val sleepTimerMode: StateFlow<SleepTimerMode> = sleepTimerController.timerMode
    val sleepTimerFormatted: StateFlow<String?> = sleepTimerController.formattedRemainingTime

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setDynamicColor(enabled)
        }
    }

    fun setIsCoverDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setCoverDynamicColorEnabled(enabled)
        }
    }

    fun setCoverDynamicColorEnabled(enabled: Boolean) = setIsCoverDynamicColorEnabled(enabled)

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            themePreferences.setLanguage(lang)
        }
    }

    fun toggleEqualizer(enabled: Boolean) = audioFxManager.setEqualizerEnabled(enabled)
    fun selectPreset(index: Int) = audioFxManager.selectPreset(index)
    fun setBandLevel(bandIndex: Short, levelMilliBel: Short) = audioFxManager.setBandLevel(bandIndex, levelMilliBel)
    fun toggleBassBoost(enabled: Boolean) = audioFxManager.setBassBoostEnabled(enabled)
    fun setBassBoostStrength(strength: Short) = audioFxManager.setBassBoostStrength(strength)

    fun startSleepTimerMinutes(minutes: Int) = sleepTimerController.startTimer(minutes)
    fun startSleepTimerEndTrack() = sleepTimerController.setEndOfTrackMode()
    fun cancelSleepTimer() = sleepTimerController.cancelTimer()

    fun logUsageMode(mode: String) {
        analyticsManager.logUsageMode(mode)
    }

    fun logFeatureUsed(featureName: String) {
        analyticsManager.logFeatureUsed(featureName)
    }
}
