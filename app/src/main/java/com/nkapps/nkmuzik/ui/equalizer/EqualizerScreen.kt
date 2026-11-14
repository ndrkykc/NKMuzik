package com.nkapps.nkmuzik.ui.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nkapps.nkmuzik.player.AudioFxState
import com.nkapps.nkmuzik.ui.settings.SettingsViewModel
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val fxState by viewModel.audioFxState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ekolayzer",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.statusBarsPadding(),
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                EqualizerContent(
                    fxState = fxState,
                    onToggleEqualizer = { viewModel.toggleEqualizer(it) },
                    onSelectPreset = { viewModel.selectPreset(it) },
                    onBandLevelChange = { bandIndex, levelMilliBel ->
                        viewModel.setBandLevel(bandIndex, levelMilliBel)
                    },
                    onToggleBassBoost = { viewModel.toggleBassBoost(it) },
                    onBassBoostStrengthChange = { viewModel.setBassBoostStrength(it) }
                )
            }
        }
    }
}

@Composable
fun EqualizerContent(
    fxState: AudioFxState,
    onToggleEqualizer: (Boolean) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onBandLevelChange: (Short, Short) -> Unit,
    onToggleBassBoost: (Boolean) -> Unit,
    onBassBoostStrengthChange: (Short) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Equalizer Toggle Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Grafik Ekolayzer", style = MaterialTheme.typography.titleMedium)
                        if (fxState.isEqualizerEnabled) {
                            Text(
                                "Açık",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Switch(
                        checked = fxState.isEqualizerEnabled,
                        onCheckedChange = onToggleEqualizer
                    )
                }

                if (fxState.isEqualizerEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Preset Selector Chips
                    Text("Hazır Ayarlar (Presets)", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(fxState.presetNames) { index, name ->
                            FilterChip(
                                selected = fxState.selectedPresetIndex == index,
                                onClick = { onSelectPreset(index) },
                                label = { Text(name) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Frequency Band Sliders
                    Text("Frekans Bantları", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    fxState.bands.forEach { band ->
                        BandSliderItem(
                            label = band.centerFreqFormatted,
                            currentLevelMilliBel = band.currentLevelMilliBel,
                            minMilliBel = band.minLevelMilliBel,
                            maxMilliBel = band.maxLevelMilliBel,
                            onValueChange = { newMilliBel ->
                                onBandLevelChange(band.index, newMilliBel)
                            }
                        )
                    }
                }
            }
        }

        // Bass Boost Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bas Güçlendirme (Bass Boost)", style = MaterialTheme.typography.titleMedium)
                        if (fxState.isBassBoostEnabled) {
                            Text(
                                "Açık",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Switch(
                        checked = fxState.isBassBoostEnabled,
                        onCheckedChange = onToggleBassBoost
                    )
                }

                if (fxState.isBassBoostEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val percent = (fxState.bassBoostStrength.toFloat() / 1000f * 100f).roundToInt()
                    Text("Seviye: %$percent", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = fxState.bassBoostStrength.toFloat(),
                        onValueChange = { newValue ->
                            onBassBoostStrengthChange(newValue.roundToInt().toShort())
                        },
                        valueRange = 0f..1000f
                    )
                }
            }
        }
    }
}

@Composable
fun BandSliderItem(
    label: String,
    currentLevelMilliBel: Short,
    minMilliBel: Short,
    maxMilliBel: Short,
    onValueChange: (Short) -> Unit
) {
    val levelDb = currentLevelMilliBel / 100f

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                String.format(Locale.getDefault(), "%.1f dB", levelDb),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = currentLevelMilliBel.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().toShort()) },
            valueRange = minMilliBel.toFloat()..maxMilliBel.toFloat()
        )
    }
}
