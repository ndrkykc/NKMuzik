package com.nkapps.nkmuzik.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nkapps.nkmuzik.ui.equalizer.EqualizerScreen
import com.nkapps.nkmuzik.ui.home.HomeScreen
import com.nkapps.nkmuzik.ui.library.LibraryScreen
import com.nkapps.nkmuzik.ui.search.OnlineSearchScreen
import com.nkapps.nkmuzik.ui.search.SearchViewModel
import com.nkapps.nkmuzik.ui.settings.SettingsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(Screen.Search.route) {
            OnlineSearchScreen(viewModel = viewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToEqualizer = {
                    navController.navigate(Screen.Equalizer.route)
                },
                onNavigateToVisualSettings = {
                    navController.navigate(Screen.VisualSettings.route)
                },
                onNavigateToAdvancedSettings = {
                    navController.navigate(Screen.AdvancedSettings.route)
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                }
            )
        }
        composable(Screen.Equalizer.route) {
            EqualizerScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.VisualSettings.route) {
            com.nkapps.nkmuzik.ui.settings.VisualSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AdvancedSettings.route) {
            com.nkapps.nkmuzik.ui.settings.AdvancedSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.About.route) {
            com.nkapps.nkmuzik.ui.settings.AboutScreen(
                onBack = { navController.popBackStack() },
                onCheckForUpdates = {
                    val activity = navController.context as? com.nkapps.nkmuzik.MainActivity
                    activity?.checkUpdatesManually()
                }
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(onPlayTrack = { localTrack ->
                viewModel.playLocalTrack(localTrack)
            })
        }
    }
}
