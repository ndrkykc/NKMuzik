package com.nkapps.nkmuzik.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nkapps.nkmuzik.ui.theme.MysticGoldMat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import com.nkapps.nkmuzik.R
import coil.compose.AsyncImage
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.downloader.DownloadStatus
import com.nkapps.nkmuzik.ui.library.AddToPlaylistDialog
import android.widget.Toast
import com.nkapps.nkmuzik.utils.ArtworkUtils
import kotlinx.coroutines.launch

@Composable
fun OnlineSearchScreen(viewModel: SearchViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val dbPlaylists by viewModel.dbPlaylists.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val localTracks by viewModel.localTracks.collectAsState()
    val showSuggestions by viewModel.showSuggestions.collectAsState()
    val enableOnlineFeatures by viewModel.enableOnlineFeatures.collectAsState()
    val isOnlineSearchSource by viewModel.isOnlineSearchSource.collectAsState()

    var trackToAddToPlaylist by remember { mutableStateOf<OnlineSearchResultDto?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        if (uiState is SearchUiState.Error) {
            val errorMsg = (uiState as SearchUiState.Error).message
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            if (uiState !is SearchUiState.Error || (uiState as SearchUiState.Error).message != msg) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            scope.launch {
                snackbarHostState.showSnackbar(msg)
            }
            viewModel.clearToastMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        viewModel.dismissSuggestions()
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    })
                }
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var searchBarWidth by remember { mutableIntStateOf(0) }
                    var searchBarHeight by remember { mutableIntStateOf(0) }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onQueryChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged {
                                searchBarWidth = it.width
                                searchBarHeight = it.height
                            },
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                        placeholder = {
                            Text(
                                if (isOnlineSearchSource) "Çevrimiçi ara..." else "Cihazımda ara...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp)
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    viewModel.onQueryChanged("")
                                    viewModel.dismissSuggestions()
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                                }
                            }
                        },
                        singleLine = true,
                        shape = CircleShape,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    viewModel.dismissSuggestions()
                                    viewModel.performSearch(searchQuery)
                                }
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        )
                    )

                    val searchCompletions by viewModel.searchCompletions.collectAsState()
                    if (showSuggestions && searchCompletions.isNotEmpty() && searchQuery.isNotBlank()) {
                        val searchBarWidthDp = with(LocalDensity.current) { searchBarWidth.toDp() }
                        Popup(
                            alignment = Alignment.TopStart,
                            offset = IntOffset(x = 0, y = searchBarHeight),
                            onDismissRequest = { viewModel.dismissSuggestions() },
                            properties = PopupProperties(
                                focusable = false,
                                dismissOnBackPress = true,
                                dismissOnClickOutside = true
                            )
                        ) {
                            Surface(
                                modifier = Modifier
                                    .then(
                                        if (searchBarWidth > 0) Modifier.width(searchBarWidthDp)
                                        else Modifier.fillMaxWidth()
                                    )
                                    .padding(top = 4.dp)
                                    .zIndex(10f),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                tonalElevation = 4.dp,
                                shadowElevation = 6.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    searchCompletions.forEach { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.selectCompletion(suggestion)
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                }
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = suggestion,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    when (val state = uiState) {
                        is SearchUiState.Idle -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        stringResource(R.string.online_search_idle_prompt),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                        is SearchUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is SearchUiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(state.message, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        is SearchUiState.Success -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(
                                    items = state.results,
                                    key = { index, track -> if (track.videoId.isNotBlank()) "${track.videoId}_$index" else "idx_$index" }
                                ) { _, track ->
                                    val downloadStatus = remember(downloadStates, localTracks, track) {
                                        val info = downloadStates[track.videoId]
                                        if (info?.status == DownloadStatus.DOWNLOADING) {
                                            DownloadStatus.DOWNLOADING
                                        } else if (info?.status == DownloadStatus.COMPLETED || localTracks.any { (it.videoId != null && it.videoId == track.videoId) || it.id.toString() == track.videoId || (track.videoId.isNotBlank() && it.uri.contains(track.videoId)) }) {
                                            DownloadStatus.COMPLETED
                                        } else {
                                            viewModel.getDownloadStatus(track)
                                        }
                                    }
                                    val downloadInfo = downloadStates[track.videoId]
                                    OnlineSearchResultCard(
                                        track = track,
                                        downloadInfoStatus = downloadStatus,
                                        downloadProgress = downloadInfo?.progress ?: 0,
                                        enableDownloads = enableOnlineFeatures,
                                        onClick = {
                                            viewModel.dismissSuggestions()
                                            viewModel.selectTrack(track)
                                        },
                                        onPlayStream = {
                                            viewModel.dismissSuggestions()
                                            viewModel.playStreamQueue(state.results, state.results.indexOf(track))
                                        },
                                        onDownload = {
                                            viewModel.dismissSuggestions()
                                            viewModel.onDownloadClicked(track)
                                        },
                                        onCancelDownload = {
                                            viewModel.cancelDownload(track.videoId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }


            }
        }

        selectedTrack?.let { track ->
            MediaActionBottomSheet(
                track = track,
                enableDownloads = enableOnlineFeatures,
                onDismiss = { viewModel.selectTrack(null) },
                onPlayStream = { viewModel.onPlayStreamClicked(track) },
                onDownload = { viewModel.onDownloadClicked(track) },
                onAddToPlaylist = { trackToAddToPlaylist = track }
            )
        }

        trackToAddToPlaylist?.let { track ->
            AddToPlaylistDialog(
                playlists = dbPlaylists,
                onSelectPlaylist = { playlist ->
                    viewModel.addTrackToPlaylist(playlist.id, track)
                },
                onCreateNewPlaylist = { name ->
                    viewModel.createPlaylist(name)
                },
                onDismiss = { trackToAddToPlaylist = null }
            )
        }
    }
}

@Composable
fun OnlineSearchResultCard(
    track: OnlineSearchResultDto,
    downloadInfoStatus: DownloadStatus?,
    downloadProgress: Int,
    enableDownloads: Boolean = true,
    onClick: () -> Unit,
    onPlayStream: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val onCardClick: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onClick()
    }
    val onPlayStreamClick: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onPlayStream()
    }
    val onDownloadClick: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDownload()
    }

    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val sheenBrush = Brush.horizontalGradient(
        listOf(
            cardBg,
            MysticGoldMat.copy(alpha = 0.10f)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(sheenBrush)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = ArtworkUtils.getArtworkModel(LocalContext.current, track.thumbnailUrl, track.title, track.videoId, track.durationString, track.uploaderName),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${track.uploaderName} • ${track.durationString}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onPlayStreamClick) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.action_listen),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (enableDownloads) {
                    when (downloadInfoStatus) {
                        DownloadStatus.DOWNLOADING -> {
                            TextButton(
                                onClick = onCancelDownload,
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { (if (downloadProgress > 0) downloadProgress / 100f else 0.1f).coerceIn(0f, 1f) },
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("İptal", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            IconButton(onClick = {}) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = stringResource(R.string.downloaded),
                                    tint = Color(0xFF4CAF50)
                                )
                            }
                        }
                        else -> {
                            IconButton(onClick = onDownloadClick) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = stringResource(R.string.action_download),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
