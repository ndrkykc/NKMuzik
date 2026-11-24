package com.nkapps.nkmuzik

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import com.nkapps.nkmuzik.ui.main.MainScreen
import com.nkapps.nkmuzik.ui.search.SearchViewModel
import com.nkapps.nkmuzik.ui.settings.SettingsViewModel
import com.nkapps.nkmuzik.ui.theme.NKMüzikTheme
import com.nkapps.nkmuzik.update.GitHubUpdateDialog
import com.nkapps.nkmuzik.update.GitHubUpdateManager
import com.nkapps.nkmuzik.utils.LanguageManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val searchViewModel: SearchViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    @Inject
    lateinit var gitHubUpdateManager: GitHubUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        super.onCreate(savedInstanceState)
        
        // SplashScreen'in 1 saniye (1000 ms) boyunca ekranda kalmasını sağlar
        val scope = (this as androidx.lifecycle.LifecycleOwner).lifecycleScope
        scope.launch {
            kotlinx.coroutines.delay(1000)
            keepSplash = false
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT,
            ),
        )
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val dynamicColor by settingsViewModel.dynamicColor.collectAsState()
            val language by settingsViewModel.language.collectAsState()
            val updateState by gitHubUpdateManager.updateState.collectAsState()
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                settingsViewModel.logUsageMode("local")
                gitHubUpdateManager.checkForUpdates()
            }

            LaunchedEffect(language) {
                if (language.isNotBlank() && LanguageManager.getLanguage() != language) {
                    LanguageManager.setLanguage(language)
                }
            }

            NKMüzikTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor,
            ) {
                MainScreen(viewModel = searchViewModel)

                GitHubUpdateDialog(
                    updateState = updateState,
                    onUpdateClick = { release ->
                        scope.launch {
                            gitHubUpdateManager.downloadAndInstallUpdate(release)
                        }
                    },
                    onInstallClick = { apkFile ->
                        gitHubUpdateManager.installApk(apkFile)
                    },
                    onDismiss = {
                        gitHubUpdateManager.dismissUpdate()
                    }
                )
            }
        }
    }

    fun checkUpdatesManually() {
        if (::gitHubUpdateManager.isInitialized) {
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                gitHubUpdateManager.checkForUpdates(showToastIfUpToDate = true)
            }
        }
    }
}
