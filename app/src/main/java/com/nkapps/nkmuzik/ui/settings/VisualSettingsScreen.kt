package com.nkapps.nkmuzik.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nkapps.nkmuzik.R
import com.nkapps.nkmuzik.ui.theme.DeepNightBlue
import com.nkapps.nkmuzik.ui.theme.MysticGoldMat
import com.nkapps.nkmuzik.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val isCoverDynamicColorEnabled by viewModel.isCoverDynamicColorEnabled.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }

    val cardBackgroundColor = MaterialTheme.colorScheme.surfaceContainer
    val cardIconTint = if (dynamicColor) {
        MaterialTheme.colorScheme.primary
    } else {
        MysticGoldMat
    }

    val switchColors = if (dynamicColor) {
        SwitchDefaults.colors()
    } else {
        SwitchDefaults.colors(
            checkedThumbColor = DeepNightBlue,
            checkedTrackColor = MysticGoldMat
        )
    }

    val settingTitleStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.visual_settings_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. TEMA SEÇİMİ
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = cardBackgroundColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showThemeDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DarkMode,
                            contentDescription = null,
                            tint = cardIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.theme_setting),
                                style = settingTitleStyle
                            )
                            val themeLabel = when (themeMode) {
                                ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                            }
                            Text(
                                text = themeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.select_theme),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. KAPAK GÖRSELİNE GÖRE DİNAMİK RENKLER
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = cardBackgroundColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setCoverDynamicColorEnabled(!isCoverDynamicColorEnabled) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = cardIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.dynamic_colors_cover),
                                style = settingTitleStyle
                            )
                            Text(
                                text = if (isCoverDynamicColorEnabled) stringResource(R.string.setting_enabled) else stringResource(R.string.setting_disabled),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isCoverDynamicColorEnabled,
                        onCheckedChange = { isChecked ->
                            viewModel.setCoverDynamicColorEnabled(isChecked)
                        },
                        colors = switchColors
                    )
                }
            }

            // 3. MATERIAL YOU DİNAMİK RENKLER (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = cardBackgroundColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setDynamicColor(!dynamicColor) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ColorLens,
                                contentDescription = null,
                                tint = cardIconTint,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.dynamic_colors_material),
                                    style = settingTitleStyle
                                )
                                Text(
                                    text = if (dynamicColor) stringResource(R.string.setting_enabled) else stringResource(R.string.setting_disabled),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = { isChecked ->
                                viewModel.setDynamicColor(isChecked)
                            },
                            colors = switchColors
                        )
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.select_theme)) },
            text = {
                Column {
                    val themes = listOf(
                        ThemeMode.DARK to stringResource(R.string.theme_dark),
                        ThemeMode.LIGHT to stringResource(R.string.theme_light),
                        ThemeMode.SYSTEM to stringResource(R.string.theme_system)
                    )
                    themes.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (themeMode == mode),
                                    onClick = {
                                        viewModel.setThemeMode(mode)
                                        showThemeDialog = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (themeMode == mode),
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MysticGoldMat)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
