@file:OptIn(UnstableApi::class)

package com.nkapps.nkmuzik.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class BandInfo(
    val index: Short,
    val centerFreqHz: Int,
    val minLevelMilliBel: Short,
    val maxLevelMilliBel: Short,
    val currentLevelMilliBel: Short
) {
    val centerFreqFormatted: String
        get() = if (centerFreqHz >= 1000) "${centerFreqHz / 1000}kHz" else "${centerFreqHz}Hz"

    val currentLevelDb: Float
        get() = currentLevelMilliBel / 100f
}

data class AudioFxState(
    val isAvailable: Boolean = true,
    val isEqualizerEnabled: Boolean = false,
    val presetNames: List<String> = listOf("Normal", "Pop", "Rock", "Jazz", "Klasik", "Bas Güçlendirme"),
    val selectedPresetIndex: Int = 0, // 0 = Normal/Flat
    val bands: List<BandInfo> = emptyList(),
    val isBassBoostEnabled: Boolean = false,
    val bassBoostStrength: Short = 0 // 0 to 1000
)

@Singleton
class AudioFxManager @Inject constructor() {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var currentAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    private val _fxState = MutableStateFlow(AudioFxState())
    val fxState: StateFlow<AudioFxState> = _fxState.asStateFlow()

    // Default 5-band frequency presets (in mB, where 100 mB = 1 dB)
    private val presetBandGains = mapOf(
        0 to shortArrayOf(0, 0, 0, 0, 0),             // Normal
        1 to shortArrayOf(150, 300, 450, 150, -150),  // Pop
        2 to shortArrayOf(450, 300, -150, 250, 400),  // Rock
        3 to shortArrayOf(300, 200, -150, 200, 350),  // Jazz
        4 to shortArrayOf(450, 350, 200, 300, 400),   // Classical
        5 to shortArrayOf(600, 450, 0, 0, 0)          // Bass Boost
    )

    fun init(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        if (currentAudioSessionId == audioSessionId && equalizer != null) return

        release()
        currentAudioSessionId = audioSessionId

        try {
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = _fxState.value.isEqualizerEnabled
            equalizer = eq

            val bb = BassBoost(0, audioSessionId)
            bb.enabled = _fxState.value.isBassBoostEnabled
            if (bb.strengthSupported) {
                bb.setStrength(_fxState.value.bassBoostStrength)
            }
            bassBoost = bb

            updateHardwareState()
        } catch (e: Exception) {
            e.printStackTrace()
            setupFallbackBands()
        }
    }

    private fun updateHardwareState() {
        val eq = equalizer ?: run {
            setupFallbackBands()
            return
        }

        try {
            val numBands = eq.numberOfBands
            val range = eq.bandLevelRange ?: shortArrayOf(-1500, 1500)
            val minLevel = range[0]
            val maxLevel = range[1]

            val bandList = mutableListOf<BandInfo>()
            for (i in 0 until numBands) {
                val bandIndex = i.toShort()
                val centerFreq = eq.getCenterFreq(bandIndex) / 1000 // mHz to Hz
                val currentLevel = eq.getBandLevel(bandIndex)

                bandList.add(
                    BandInfo(
                        index = bandIndex,
                        centerFreqHz = centerFreq,
                        minLevelMilliBel = minLevel,
                        maxLevelMilliBel = maxLevel,
                        currentLevelMilliBel = currentLevel
                    )
                )
            }

            val hardwarePresetNames = mutableListOf<String>()
            val numPresets = try { eq.numberOfPresets.toInt() } catch (e: Exception) { 0 }
            if (numPresets > 0) {
                for (p in 0 until numPresets) {
                    try {
                        hardwarePresetNames.add(eq.getPresetName(p.toShort()))
                    } catch (e: Exception) {
                        hardwarePresetNames.add("Preset $p")
                    }
                }
            }

            val finalPresetNames = if (hardwarePresetNames.isNotEmpty()) {
                hardwarePresetNames
            } else {
                _fxState.value.presetNames
            }

            _fxState.value = _fxState.value.copy(
                isAvailable = true,
                presetNames = finalPresetNames,
                bands = bandList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            setupFallbackBands()
        }
    }

    private fun setupFallbackBands() {
        val fallbackCenterFreqs = intArrayOf(60, 230, 910, 3600, 14000)
        val bandList = fallbackCenterFreqs.mapIndexed { index, freq ->
            BandInfo(
                index = index.toShort(),
                centerFreqHz = freq,
                minLevelMilliBel = -1500,
                maxLevelMilliBel = 1500,
                currentLevelMilliBel = 0
            )
        }
        _fxState.value = _fxState.value.copy(
            isAvailable = true,
            bands = bandList
        )
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _fxState.value = _fxState.value.copy(isEqualizerEnabled = enabled)
    }

    fun selectPreset(presetIndex: Int) {
        _fxState.value = _fxState.value.copy(selectedPresetIndex = presetIndex)

        // Hardware preset if supported
        try {
            val eq = equalizer
            if (eq != null && presetIndex in 0 until eq.numberOfPresets) {
                eq.usePreset(presetIndex.toShort())
                updateHardwareState()
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Custom preset band gains fallback
        val gains = presetBandGains[presetIndex] ?: presetBandGains[0]!!
        val currentBands = _fxState.value.bands
        val updatedBands = currentBands.mapIndexed { index, band ->
            val gain = if (index < gains.size) gains[index] else 0.toShort()
            try {
                equalizer?.setBandLevel(band.index, gain)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            band.copy(currentLevelMilliBel = gain)
        }
        _fxState.value = _fxState.value.copy(bands = updatedBands)
    }

    fun setBandLevel(bandIndex: Short, levelMilliBel: Short) {
        try {
            equalizer?.setBandLevel(bandIndex, levelMilliBel)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val updatedBands = _fxState.value.bands.map { band ->
            if (band.index == bandIndex) {
                band.copy(currentLevelMilliBel = levelMilliBel)
            } else {
                band
            }
        }
        _fxState.value = _fxState.value.copy(
            selectedPresetIndex = -1, // Custom
            bands = updatedBands
        )
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        try {
            bassBoost?.enabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _fxState.value = _fxState.value.copy(isBassBoostEnabled = enabled)
    }

    fun setBassBoostStrength(strength: Short) {
        try {
            val bb = bassBoost
            if (bb != null && bb.strengthSupported) {
                bb.setStrength(strength)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _fxState.value = _fxState.value.copy(bassBoostStrength = strength)
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        equalizer = null
        bassBoost = null
        currentAudioSessionId = C.AUDIO_SESSION_ID_UNSET
    }
}
