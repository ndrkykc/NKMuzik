package com.nkapps.nkmuzik.ui.main

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nkapps.nkmuzik.R
import com.nkapps.nkmuzik.ui.theme.DeepNightBlue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nkapps.nkmuzik.ui.navigation.AppNavHost
import com.nkapps.nkmuzik.ui.navigation.Screen
import com.nkapps.nkmuzik.ui.permission.PermissionHandler
import com.nkapps.nkmuzik.ui.player.MiniPlayer
import com.nkapps.nkmuzik.ui.player.NowPlayingBottomSheet
import com.nkapps.nkmuzik.ui.search.SearchViewModel

@Composable
fun MainScreen(viewModel: SearchViewModel = hiltViewModel()) {
    PermissionHandler {
        val navController = rememberNavController()
        val currentPlayingTrack by viewModel.currentPlayingTrack.collectAsState()
        val isPlaying by viewModel.isPlaying.collectAsState()
        val currentPosition by viewModel.currentPosition.collectAsState()
        val duration by viewModel.duration.collectAsState()
        val dynamicColor by viewModel.dynamicColor.collectAsState()
        val toastMessage by viewModel.toastMessage.collectAsState()
        var showNowPlaying by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        var backPressedTime by remember { mutableLongStateOf(0L) }
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

        BackHandler(enabled = true) {
            if (showNowPlaying) {
                showNowPlaying = false
            } else if (currentRoute != Screen.Home.route) {
                if (!navController.popBackStack()) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            } else {
                val currentTime = System.currentTimeMillis()
                if (currentTime - backPressedTime < 2000L) {
                    (context as? Activity)?.finish()
                } else {
                    backPressedTime = currentTime
                    Toast.makeText(context, context.resources.getString(R.string.press_again_to_exit), Toast.LENGTH_SHORT).show()
                }
            }
        }

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(toastMessage) {
            toastMessage?.let { msg ->
                snackbarHostState.showSnackbar(msg)
                viewModel.clearToastMessage()
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            containerColor = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    })
                },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                currentPlayingTrack?.let { track ->
                    AnimatedVisibility(
                        visible = !showNowPlaying,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Column {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .background(Color.Transparent)
                            )
                            MiniPlayer(
                                track = track,
                                isPlaying = isPlaying,
                                currentPosition = currentPosition,
                                duration = duration,
                                isFavorite = viewModel.isFavorite(track),
                                onToggleFavorite = { viewModel.toggleFavorite(track) },
                                onSkipPrevious = { viewModel.skipToPrevious() },
                                onPlayPause = { viewModel.togglePlayPause() },
                                onSkipNext = { viewModel.skipToNext() },
                                onClick = { showNowPlaying = true },
                                dynamicColor = dynamicColor,
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 28.dp)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )

            AnimatedVisibility(
                visible = showNowPlaying && currentPlayingTrack != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                currentPlayingTrack?.let { track ->
                    NowPlayingBottomSheet(
                        track = track,
                        isPlaying = isPlaying,
                        isFavorite = viewModel.isFavorite(track),
                        viewModel = viewModel,
                        onDismiss = { showNowPlaying = false },
                        onPlayPauseToggle = { viewModel.togglePlayPause() },
                        onToggleFavorite = { viewModel.toggleFavorite(track) }
                    )
                }
            }
        }
    }
}
