package com.nkapps.nkmuzik.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.nkapps.nkmuzik.R
import com.nkapps.nkmuzik.ui.theme.DeepNightBlue
import com.nkapps.nkmuzik.ui.theme.MysticGoldMat
import com.nkapps.nkmuzik.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToVisualSettings: () -> Unit,
    onCheckForUpdates: (() -> Unit)? = null
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val context = LocalContext.current

    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    val isCoverDynamicColorEnabled by viewModel.isCoverDynamicColorEnabled.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()

    var showVisualSettingsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Persistent Notification Permission State
    var isNotificationPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isNotificationPermissionGranted = isGranted
    }

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

    var showChangelogDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
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
            // 0. GÖRSEL AYARLAR / VISUAL SETTINGS
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = cardBackgroundColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToVisualSettings() }
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
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = cardIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.visual_settings_title),
                                style = settingTitleStyle
                            )
                            val themeLabel = when (themeMode) {
                                ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                            }
                            Text(
                                text = stringResource(R.string.visual_settings_desc, themeLabel),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.visual_settings_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. DİL / LANGUAGE
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = cardBackgroundColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLanguageDialog = true }
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
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = cardIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.language_setting),
                                style = settingTitleStyle
                            )
                            val languageLabel = if (language == "tr") {
                                stringResource(R.string.lang_turkish)
                            } else {
                                stringResource(R.string.lang_english)
                            }
                            Text(
                                text = languageLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.select_language),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3. KALICI BİLDİRİMLERE İZİN VER (PERSISTENT NOTIFICATIONS)
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = cardBackgroundColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = cardIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.persistent_notifications),
                                style = settingTitleStyle
                            )
                            Text(
                                text = if (isNotificationPermissionGranted) stringResource(R.string.setting_enabled) else stringResource(R.string.setting_disabled),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isNotificationPermissionGranted,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        isNotificationPermissionGranted = true
                                    }
                                } else {
                                    isNotificationPermissionGranted = true
                                }
                            } else {
                                isNotificationPermissionGranted = false
                            }
                        },
                        colors = switchColors
                    )
                }
            }

            // 4. EKOLAYZER (EQUALIZER)
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = cardBackgroundColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = {
                        viewModel.logFeatureUsed("equalizer")
                        onNavigateToEqualizer()
                    })
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Equalizer,
                            contentDescription = null,
                            tint = cardIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.equalizer),
                                style = settingTitleStyle
                            )
                            Text(
                                text = stringResource(R.string.equalizer_and_sound_effects),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = {
                        viewModel.logFeatureUsed("equalizer")
                        onNavigateToEqualizer()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = stringResource(R.string.equalizer_open)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val infoColor = if (MaterialTheme.colorScheme.background == DeepNightBlue) {
                    Color(0xFF9E9E9E)
                } else {
                    Color.Black
                }

                Surface(
                    onClick = { onCheckForUpdates?.invoke() },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = stringResource(R.string.check_for_updates),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    onClick = { showChangelogDialog = true },
                    color = Color.Transparent,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = stringResource(R.string.app_version, "v1.0.1"),
                        color = infoColor,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                Image(
                    painter = painterResource(R.drawable.ic_dev_signature),
                    contentDescription = "Geliştirici İmzası",
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(infoColor),
                    modifier = Modifier
                        .height(40.dp)
                        .clickable { showAboutDialog = true }
                )
            }
        }
    }

    if (showChangelogDialog) {
        AlertDialog(
            onDismissRequest = { showChangelogDialog = false },
            title = { Text(text = "NK Müzik v1.0.1", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(text = "Yenilikler:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "- Cihaz içi şarkılarda A'dan Z'ye hızlı gezinme (Fast Scrollbar) eklendi.\n- Oynatma ekranında (Uyku Zamanlayıcı) erişimi kolaylaştırıldı.\n- Çevrimiçi aramada internet kesintilerine özel uyarılar eklendi.\n- Uygulama açılış ekranı (Splash Screen) geliştirildi. \n- Görsel Ayarlar penceresi bağımsız bir görünüme kavuşturuldu.\n- Uygulanma dil desteğinde iyileştirmeler yapıldı.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangelogDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showVisualSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showVisualSettingsDialog = false },
            title = {
                Text(
                    text = "Görsel Ayarlar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Tema Seçimi
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showVisualSettingsDialog = false
                                showThemeDialog = true
                            },
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

                    HorizontalDivider()

                    // 2. Kapak Görseline Göre Dinamik Renkler
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setCoverDynamicColorEnabled(!isCoverDynamicColorEnabled)
                            },
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

                    // 3. Material You Dinamik Renkler (Android 12+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setDynamicColor(!dynamicColor)
                                },
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
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showVisualSettingsDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
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

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.select_language)) },
            text = {
                Column {
                    val languages = listOf("tr" to stringResource(R.string.lang_turkish), "en" to stringResource(R.string.lang_english))
                    languages.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (language == code),
                                    onClick = {
                                        viewModel.setLanguage(code)
                                        showLanguageDialog = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (language == code),
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
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAboutDialog) {
        val openPlayStoreDeveloperPage: () -> Unit = {
            val devUrl = "https://play.google.com/store/apps/dev?id=8720751548372835422"
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(devUrl)
                    setPackage("com.android.vending")
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(devUrl)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Play Store açılamadı", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val openGitHubDeveloperPage: () -> Unit = {
            val githubUrl = "https://github.com/ndrkykc?tab=projects"
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "GitHub açılamadı", Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {},
            dismissButton = null,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            ),
            containerColor = cardBackgroundColor,
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Diğer Uygulamalar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        onClick = openPlayStoreDeveloperPage,
                        shape = MaterialTheme.shapes.medium,
                        color = Color(0xFF000000),
                        border = BorderStroke(1.dp, Color(0xFFCCCCCC)),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_google_play_logo),
                                contentDescription = "Google Play",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "GET IT ON",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp),
                                    color = Color(0xFFCCCCCC)
                                )
                                Text(
                                    text = "Google Play",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        onClick = openGitHubDeveloperPage,
                        shape = MaterialTheme.shapes.medium,
                        color = Color(0xFF000000),
                        border = BorderStroke(1.dp, Color(0xFFCCCCCC)),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_github_logo),
                                contentDescription = "GitHub",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "VIEW ON",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp),
                                    color = Color(0xFFCCCCCC)
                                )
                                Text(
                                    text = "GitHub",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
