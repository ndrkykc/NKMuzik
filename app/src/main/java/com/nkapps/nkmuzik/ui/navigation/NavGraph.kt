package com.nkapps.nkmuzik.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nkapps.nkmuzik.ui.equalizer.EqualizerScreen
import com.nkapps.nkmuzik.ui.home.HomeScreen
import com.nkapps.nkmuzik.ui.library.LibraryScreen
import com.nkapps.nkmuzik.ui.search.SearchScreen
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
            SearchScreen(viewModel = viewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToEqualizer = {
                    navController.navigate(Screen.Equalizer.route)
                }
            )
        }
        composable(Screen.Equalizer.route) {
            EqualizerScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(onPlayTrack = { localTrack ->
                viewModel.playLocalTrack(localTrack)
            })
        }
    }
}
