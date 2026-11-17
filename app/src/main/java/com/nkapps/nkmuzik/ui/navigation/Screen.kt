package com.nkapps.nkmuzik.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Ana Sayfa", Icons.Filled.Home)
    object Search : Screen("search", "Keşfet", Icons.Filled.Search)
    object Library : Screen("library", "Kütüphane", Icons.Filled.LibraryMusic)
    object Settings : Screen("settings", "Ayarlar", Icons.Filled.Settings)
    object Equalizer : Screen("equalizer", "Ekolayzer", Icons.Filled.Equalizer)
    object VisualSettings : Screen("visual_settings", "Görsel Ayarlar", Icons.Filled.Settings)
}
