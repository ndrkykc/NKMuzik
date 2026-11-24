package com.nkapps.nkmuzik.ui.home

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.nkapps.nkmuzik.R
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.downloader.DownloadStatus
import com.nkapps.nkmuzik.ui.home.components.AlbumCard
import com.nkapps.nkmuzik.ui.home.components.AlbumGroup
import com.nkapps.nkmuzik.ui.home.components.ArtistCard
import com.nkapps.nkmuzik.ui.home.components.ArtistGroup
import com.nkapps.nkmuzik.ui.home.components.FolderCard
import com.nkapps.nkmuzik.ui.home.components.FolderGroup
import com.nkapps.nkmuzik.ui.home.components.GroupDetailHeader
import com.nkapps.nkmuzik.ui.home.components.HomeCard
import com.nkapps.nkmuzik.ui.home.components.HomeScreenEmptyState
import com.nkapps.nkmuzik.ui.home.components.LocalTrackGridCard
import com.nkapps.nkmuzik.ui.home.components.LocalTrackItemCard
import com.nkapps.nkmuzik.ui.home.components.MultiSelectionActionBar
import com.nkapps.nkmuzik.ui.home.components.OnlineSearchLiveCard
import com.nkapps.nkmuzik.ui.home.components.PlaylistSummaryCard
import com.nkapps.nkmuzik.ui.home.components.SortAndFilterBar
import com.nkapps.nkmuzik.ui.home.components.TrackCard
import com.nkapps.nkmuzik.ui.home.components.TrackFilterOption
import com.nkapps.nkmuzik.ui.home.components.TrackGridCard
import com.nkapps.nkmuzik.ui.home.components.TrackSortOption
import com.nkapps.nkmuzik.ui.home.components.getFolderName
import com.nkapps.nkmuzik.ui.home.components.getFolderNameForDto
import com.nkapps.nkmuzik.ui.search.SearchUiState
import com.nkapps.nkmuzik.ui.search.SearchViewModel
import com.nkapps.nkmuzik.ui.search.matchesSearchQuery
import com.nkapps.nkmuzik.utils.ShareUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SearchViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedSection by remember { mutableStateOf("all") }
    val isGridViewMode by viewModel.isGridViewMode.collectAsState()
    val currentViewMode = if (isGridViewMode) 1 else 0
    var localSearchQuery by remember { mutableStateOf("") }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                if (selectedSection == "online") {
                    viewModel.onOnlineQueryChanged(spokenText)
                    viewModel.performSearch(spokenText)
                } else {
                    localSearchQuery = spokenText
                }
            }
        }
    }

    val voiceSearchPrompt = stringResource(R.string.voice_search)
    val speechNotSupportedMsg = stringResource(R.string.speech_not_supported)

    val launchVoiceSearch: () -> Unit = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, voiceSearchPrompt)
            }
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, speechNotSupportedMsg, Toast.LENGTH_SHORT).show()
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val onRefresh: () -> Unit = {
        isRefreshing = true
        coroutineScope.launch {
            viewModel.loadLocalTracks()
            if (selectedSection == "online" && viewModel.onlineQuery.value.isNotBlank()) {
                viewModel.performSearch(viewModel.onlineQuery.value)
            }
            delay(600)
            isRefreshing = false
        }
    }

    val onlineQuery by viewModel.onlineQuery.collectAsState()
    val onlineResults by viewModel.onlineResults.collectAsState()
    val isSearchingOnline by viewModel.isSearchingOnline.collectAsState()
    val searchCompletions by viewModel.searchCompletions.collectAsState()
    val showSuggestions by viewModel.showSuggestions.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val rememberSearchHistory by viewModel.rememberSearchHistory.collectAsState()

    val selectedTrackIds by viewModel.selectedTrackIds.collectAsState()
    val selectedFavoriteVideoIds by viewModel.selectedFavoriteVideoIds.collectAsState()
    val playCounts by viewModel.playCounts.collectAsState()

    var currentSort by remember { mutableStateOf(TrackSortOption.DATE_ADDED) }
    var currentFilter by remember { mutableStateOf(TrackFilterOption.SONG) }

    var selectedArtistGroup by remember { mutableStateOf<String?>(null) }
    var selectedAlbumGroup by remember { mutableStateOf<String?>(null) }
    var selectedFolderGroup by remember { mutableStateOf<String?>(null) }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }

    BackHandler(enabled = selectedSection == "playlists" && selectedPlaylistId != null) {
        selectedPlaylistId = null
    }

    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var showAddSelectedToPlaylistDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedFavoritesDialog by remember { mutableStateOf(false) }
    var showAddSelectedFavoritesToPlaylistDialog by remember { mutableStateOf(false) }

    val localTracks by viewModel.localTracks.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val dbPlaylists by viewModel.dbPlaylists.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val enableOnlineFeatures by viewModel.enableOnlineFeatures.collectAsState()
    val onlineTabLabel by viewModel.onlineTabLabel.collectAsState()
    val isOnlineSearchSource by viewModel.isOnlineSearchSource.collectAsState()

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
            viewModel.clearToastMessage()
        }
    }

    LaunchedEffect(currentFilter, selectedSection) {
        selectedArtistGroup = null
        selectedAlbumGroup = null
        selectedFolderGroup = null
        selectedPlaylistId = null
        viewModel.clearSelection()
    }

    LaunchedEffect(enableOnlineFeatures) {
        if (!enableOnlineFeatures && selectedSection == "online") {
            selectedSection = "all"
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadLocalTracks()
    }

    // Filtered local tracks for "Tümü"
    val filteredLocalTracks = remember(localTracks, localSearchQuery, currentSort, currentFilter, playCounts) {
        if (localSearchQuery.isBlank()) {
            when (currentSort) {
                TrackSortOption.NAME -> localTracks.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                TrackSortOption.DATE_ADDED -> localTracks.sortedByDescending { it.id }
                TrackSortOption.PLAY_COUNT -> localTracks.sortedByDescending { viewModel.getPlayCount(it.id.toString()) }
            }
        } else {
            viewModel.searchLocalTracks(localSearchQuery, localTracks)
        }
    }

    // Filtered favorites
    val filteredFavorites = remember(favorites, localSearchQuery, currentSort, currentFilter, playCounts, localTracks) {
        if (localSearchQuery.isBlank()) {
            when (currentSort) {
                TrackSortOption.NAME -> favorites.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                TrackSortOption.DATE_ADDED -> favorites
                TrackSortOption.PLAY_COUNT -> favorites.sortedByDescending { viewModel.getPlayCount(it.videoId) }
            }
        } else {
            viewModel.searchFavorites(localSearchQuery, favorites, localTracks)
        }
    }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    viewModel.dismissSuggestions()
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar with Settings icon button and Oval Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Ayarlar"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                val currentSearchText = if (selectedSection == "online") onlineQuery else localSearchQuery

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var searchBarWidth by remember { mutableIntStateOf(0) }
                        var searchBarHeight by remember { mutableIntStateOf(0) }

                        val placeholderPhrases = remember(selectedSection) {
                            when (selectedSection) {
                                "online" -> listOf("Online ara...", "Şarkı ara...", "Sanatçı ara...", "Albüm ara...")
                                "favorites" -> listOf("Favorilerde ara...")
                                "playlists" -> listOf("Çalma listesinde ara...")
                                else -> listOf("Şarkı ara...", "Sanatçı ara...", "Albüm ara...")
                            }
                        }

                        OutlinedTextField(
                            value = currentSearchText,
                            onValueChange = { query ->
                                if (selectedSection == "online") {
                                    viewModel.onOnlineQueryChanged(query)
                                } else {
                                    localSearchQuery = query
                                }
                            },
                            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                            placeholder = {
                                com.nkapps.nkmuzik.ui.common.TypewriterPlaceholder(
                                    phrases = placeholderPhrases,
                                    isSearching = currentSearchText.isNotEmpty(),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (currentSearchText.isNotEmpty()) {
                                        IconButton(onClick = {
                                            if (selectedSection == "online") {
                                                viewModel.onOnlineQueryChanged("")
                                                viewModel.openSuggestions()
                                            } else {
                                                localSearchQuery = ""
                                            }
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                                        }
                                    }
                                    IconButton(onClick = launchVoiceSearch) {
                                        Icon(
                                            imageVector = Icons.Rounded.Mic,
                                            contentDescription = stringResource(R.string.voice_search),
                                            tint = MaterialTheme.colorScheme.outline
                                        )
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
                                    if (selectedSection == "online") {
                                        viewModel.dismissSuggestions()
                                        viewModel.performSearch(currentSearchText)
                                    }
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused && selectedSection == "online") {
                                        viewModel.openSuggestions()
                                    }
                                }
                                .onSizeChanged {
                                    searchBarWidth = it.width
                                    searchBarHeight = it.height
                                },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        val displayHistory = selectedSection == "online" && rememberSearchHistory && searchHistory.isNotEmpty() && onlineQuery.isBlank()
                        val displayCompletions = selectedSection == "online" && onlineQuery.isNotBlank() && searchCompletions.isNotEmpty()

                        if (selectedSection == "online" && showSuggestions && (displayHistory || displayCompletions)) {
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
                                        if (displayHistory) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.search_history),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = stringResource(R.string.clear_history),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.clickable {
                                                        viewModel.clearSearchHistory()
                                                    }
                                                )
                                            }
                                            searchHistory.take(10).forEach { historyQuery ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            viewModel.onOnlineQueryChanged(historyQuery)
                                                            viewModel.performSearch(historyQuery)
                                                            focusManager.clearFocus()
                                                            keyboardController?.hide()
                                                        }
                                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.History,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp),
                                                            tint = MaterialTheme.colorScheme.outline
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(
                                                            text = historyQuery,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.removeSearchHistoryQuery(historyQuery)
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Clear,
                                                            contentDescription = "Sil",
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.outline
                                                        )
                                                    }
                                                }
                                            }
                                        } else if (displayCompletions) {
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
                    Spacer(modifier = Modifier.height(4.dp))

                    // Action cards row: 1. Tümü, 2. Favoriler, 3. Çalma Listesi, 4. Online
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HomeCard(
                            title = stringResource(R.string.section_all),
                            icon = Icons.Default.MusicNote,
                            isSelected = selectedSection == "all",
                            onClick = {
                                selectedSection = "all"
                                viewModel.logUsageMode("local")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        HomeCard(
                            title = stringResource(R.string.section_favorites),
                            icon = Icons.Default.Favorite,
                            isSelected = selectedSection == "favorites",
                            onClick = {
                                selectedSection = "favorites"
                                viewModel.logUsageMode("local")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        HomeCard(
                            title = stringResource(R.string.section_playlists),
                            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                            isSelected = selectedSection == "playlists",
                            onClick = {
                                selectedSection = "playlists"
                                viewModel.logFeatureUsed("playlist")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (enableOnlineFeatures) {
                            HomeCard(
                                title = if (onlineTabLabel == "Keşfet" || onlineTabLabel == "YT Online") stringResource(R.string.discover) else onlineTabLabel,
                                icon = Icons.Rounded.Search,
                                isSelected = selectedSection == "online",
                                onClick = {
                                    selectedSection = "online"
                                    viewModel.logUsageMode("online")
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Inline Content Display according to selectedSection wrapped with PullToRefreshBox
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (selectedSection == "all") {
                                if (selectedTrackIds.isNotEmpty()) {
                                    MultiSelectionActionBar(
                                        selectedCount = selectedTrackIds.size,
                                        onClearSelection = { viewModel.clearSelection() },
                                        onPlaySelected = { viewModel.playSelectedTracks(selectedTrackIds) },
                                        onAddToFavorites = { viewModel.addSelectedToFavorites(selectedTrackIds) },
                                        onAddToPlaylist = { showAddSelectedToPlaylistDialog = true },
                                        onDeleteSelected = { showDeleteSelectedDialog = true },
                                        onShareSelected = {
                                            val selectedTracks = localTracks.filter { it.id in selectedTrackIds }
                                            ShareUtils.shareMultipleAudioFiles(context, selectedTracks)
                                            viewModel.clearSelection()
                                        }
                                    )
                                } else {
                                    SortAndFilterBar(
                                        selectedSort = currentSort,
                                        onSortSelected = { sortOpt ->
                                            currentSort = sortOpt
                                            val name = when (sortOpt) {
                                                TrackSortOption.NAME -> "name"
                                                TrackSortOption.DATE_ADDED -> "date"
                                                TrackSortOption.PLAY_COUNT -> "play_count"
                                            }
                                            viewModel.logSortUsed(name)
                                        },
                                        selectedFilter = currentFilter,
                                        onFilterSelected = { filterOpt ->
                                            currentFilter = filterOpt
                                            selectedArtistGroup = null
                                            selectedAlbumGroup = null
                                            selectedFolderGroup = null
                                            val name = when (filterOpt) {
                                                TrackFilterOption.SONG -> "songs"
                                                TrackFilterOption.ARTIST -> "artists"
                                                TrackFilterOption.ALBUM -> "albums"
                                                TrackFilterOption.FOLDER -> "folders"
                                            }
                                            viewModel.logFilterUsed(name)
                                        },
                                        viewMode = currentViewMode,
                                        onViewModeChanged = { mode -> viewModel.setGridViewMode(mode == 1) }
                                    )
                                }
                            } else if (selectedSection == "favorites") {
                                if (selectedFavoriteVideoIds.isNotEmpty()) {
                                    MultiSelectionActionBar(
                                        selectedCount = selectedFavoriteVideoIds.size,
                                        onClearSelection = { viewModel.clearFavoriteSelection() },
                                        onPlaySelected = { viewModel.playSelectedFavorites(selectedFavoriteVideoIds) },
                                        onAddToFavorites = { viewModel.removeSelectedFromFavorites(selectedFavoriteVideoIds) },
                                        favoriteIcon = Icons.Rounded.FavoriteBorder,
                                        favoriteContentDescription = "Favorilerden Çıkar",
                                        onAddToPlaylist = { showAddSelectedFavoritesToPlaylistDialog = true },
                                        onDeleteSelected = { showDeleteSelectedFavoritesDialog = true },
                                        onShareSelected = {
                                            val selectedFavs = favorites.filter { it.videoId in selectedFavoriteVideoIds }
                                            val localTracksForFavs = selectedFavs.mapNotNull { fav ->
                                                localTracks.find { it.id.toString() == fav.videoId || it.uri == fav.url }
                                            }
                                            if (localTracksForFavs.isNotEmpty()) {
                                                ShareUtils.shareMultipleAudioFiles(context, localTracksForFavs)
                                            } else if (selectedFavs.isNotEmpty()) {
                                                ShareUtils.shareAudioFile(
                                                    context = context,
                                                    filePath = null,
                                                    uriString = selectedFavs.first().url,
                                                    title = selectedFavs.first().title
                                                )
                                            }
                                            viewModel.clearFavoriteSelection()
                                        }
                                    )
                                } else {
                                    SortAndFilterBar(
                                        selectedSort = currentSort,
                                        onSortSelected = { sortOpt ->
                                            currentSort = sortOpt
                                            val name = when (sortOpt) {
                                                TrackSortOption.NAME -> "name"
                                                TrackSortOption.DATE_ADDED -> "date"
                                                TrackSortOption.PLAY_COUNT -> "play_count"
                                            }
                                            viewModel.logSortUsed(name)
                                        },
                                        selectedFilter = currentFilter,
                                        onFilterSelected = { filterOpt ->
                                            currentFilter = filterOpt
                                            selectedArtistGroup = null
                                            selectedAlbumGroup = null
                                            selectedFolderGroup = null
                                            val name = when (filterOpt) {
                                                TrackFilterOption.SONG -> "songs"
                                                TrackFilterOption.ARTIST -> "artists"
                                                TrackFilterOption.ALBUM -> "albums"
                                                TrackFilterOption.FOLDER -> "folders"
                                            }
                                            viewModel.logFilterUsed(name)
                                        },
                                        viewMode = currentViewMode,
                                        onViewModeChanged = { mode -> viewModel.setGridViewMode(mode == 1) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // List Content
                            when (selectedSection) {
                                "all" -> {
                                    when (currentFilter) {
                                        TrackFilterOption.SONG -> {
                                            if (filteredLocalTracks.isEmpty()) {
                                                HomeScreenEmptyState(
                                                    text = if (localSearchQuery.isBlank()) stringResource(R.string.empty_songs_message) else stringResource(R.string.no_music_found),
                                                    icon = Icons.Default.MusicNote
                                                )
                                            } else {
                                                if (!isGridViewMode) {
                                                    LazyColumn(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        items(filteredLocalTracks, key = { it.id }) { track ->
                                                            val isSelected = selectedTrackIds.contains(track.id)
                                                            LocalTrackItemCard(
                                                                track = track,
                                                                viewModel = viewModel,
                                                                onPlay = { viewModel.playLocalQueue(filteredLocalTracks, filteredLocalTracks.indexOf(track)) },
                                                                isSelected = isSelected,
                                                                isSelectionMode = selectedTrackIds.isNotEmpty(),
                                                                onSelectToggle = { viewModel.toggleTrackSelection(track.id) }
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    LazyVerticalGrid(
                                                        columns = GridCells.Fixed(2),
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        items(filteredLocalTracks, key = { it.id }) { track ->
                                                            val isSelected = selectedTrackIds.contains(track.id)
                                                            LocalTrackGridCard(
                                                                track = track,
                                                                viewModel = viewModel,
                                                                onPlay = { viewModel.playLocalQueue(filteredLocalTracks, filteredLocalTracks.indexOf(track)) },
                                                                isSelected = isSelected,
                                                                isSelectionMode = selectedTrackIds.isNotEmpty(),
                                                                onSelectToggle = { viewModel.toggleTrackSelection(track.id) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        TrackFilterOption.ARTIST -> {
                                            if (selectedArtistGroup == null) {
                                                val artistGroups = remember(localTracks, localSearchQuery) {
                                                    localTracks
                                                        .groupBy { it.artist.ifBlank { "Bilinmeyen Sanatçı" } }
                                                        .map { (artistName, tracks) ->
                                                            ArtistGroup(
                                                                artistName = artistName,
                                                                trackCount = tracks.size
                                                            )
                                                        }
                                                        .filter { group ->
                                                            localSearchQuery.isBlank() || group.artistName.matchesSearchQuery(localSearchQuery) || localTracks.any { it.artist.equals(group.artistName, ignoreCase = true) && it.title.matchesSearchQuery(localSearchQuery) }
                                                        }
                                                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.artistName })
                                                }

                                                if (artistGroups.isEmpty()) {
                                                    HomeScreenEmptyState(
                                                        text = if (localSearchQuery.isBlank()) "Sanatçı bulunamadı." else "Aradığınız kriterlere uygun sanatçı bulunamadı.",
                                                        icon = Icons.Default.Person
                                                    )
                                                } else {
                                                    LazyColumn(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        items(artistGroups, key = { it.artistName }) { group ->
                                                            ArtistCard(
                                                                artistName = group.artistName,
                                                                trackCount = group.trackCount,
                                                                onClick = { selectedArtistGroup = group.artistName }
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                val groupTracks = remember(localTracks, selectedArtistGroup, localSearchQuery) {
                                                    localTracks.filter { track ->
                                                        val artist = track.artist.ifBlank { "Bilinmeyen Sanatçı" }
                                                        artist.equals(selectedArtistGroup, ignoreCase = true) && (localSearchQuery.isBlank() || track.title.matchesSearchQuery(localSearchQuery))
                                                    }
                                                }

                                                Column(modifier = Modifier.fillMaxSize()) {
                                                    GroupDetailHeader(
                                                        title = selectedArtistGroup!!,
                                                        subtitle = "${groupTracks.size} Şarkı",
                                                        onBack = { selectedArtistGroup = null }
                                                    )
                                                    if (groupTracks.isEmpty()) {
                                                        HomeScreenEmptyState(
                                                            text = "Aradığınız kriterlere uygun şarkı bulunamadı.",
                                                            icon = Icons.Default.MusicNote
                                                        )
                                                    } else {
                                                        LazyColumn(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            items(groupTracks, key = { it.id }) { track ->
                                                                val isSelected = selectedTrackIds.contains(track.id)
                                                                LocalTrackItemCard(
                                                                    track = track,
                                                                    viewModel = viewModel,
                                                                    onPlay = { viewModel.playLocalQueue(groupTracks, groupTracks.indexOf(track)) },
                                                                    isSelected = isSelected,
                                                                    isSelectionMode = selectedTrackIds.isNotEmpty(),
                                                                    onSelectToggle = { viewModel.toggleTrackSelection(track.id) }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        TrackFilterOption.ALBUM -> {
                                            if (selectedAlbumGroup == null) {
                                                val albumGroups = remember(localTracks, localSearchQuery) {
                                                    localTracks
                                                        .groupBy { it.album.ifBlank { "Bilinmeyen Albüm" } }
                                                        .map { (albumName, tracks) ->
                                                            AlbumGroup(
                                                                albumTitle = albumName,
                                                                artistName = tracks.firstOrNull()?.artist ?: "",
                                                                trackCount = tracks.size
                                                            )
                                                        }
                                                        .filter { group ->
                                                            localSearchQuery.isBlank() || group.albumTitle.matchesSearchQuery(localSearchQuery) || group.artistName.matchesSearchQuery(localSearchQuery) || localTracks.any { it.album.equals(group.albumTitle, ignoreCase = true) && it.title.matchesSearchQuery(localSearchQuery) }
                                                        }
                                                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.albumTitle })
                                                }

                                                if (albumGroups.isEmpty()) {
                                                    HomeScreenEmptyState(
                                                        text = if (localSearchQuery.isBlank()) "Albüm bulunamadı." else "Aradığınız kriterlere uygun albüm bulunamadı.",
                                                        icon = Icons.Default.Album
                                                    )
                                                } else {
                                                    LazyColumn(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        items(albumGroups, key = { it.albumTitle }) { group ->
                                                            AlbumCard(
                                                                albumTitle = group.albumTitle,
                                                                artistName = group.artistName,
                                                                trackCount = group.trackCount,
                                                                onClick = { selectedAlbumGroup = group.albumTitle }
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                val groupTracks = remember(localTracks, selectedAlbumGroup, localSearchQuery) {
                                                    localTracks.filter { track ->
                                                        val album = track.album.ifBlank { "Bilinmeyen Albüm" }
                                                        album.equals(selectedAlbumGroup, ignoreCase = true) && (localSearchQuery.isBlank() || track.title.matchesSearchQuery(localSearchQuery))
                                                    }
                                                }

                                                Column(modifier = Modifier.fillMaxSize()) {
                                                    GroupDetailHeader(
                                                        title = selectedAlbumGroup!!,
                                                        subtitle = "${groupTracks.size} Şarkı",
                                                        onBack = { selectedAlbumGroup = null }
                                                    )
                                                    if (groupTracks.isEmpty()) {
                                                        HomeScreenEmptyState(
                                                            text = "Aradığınız kriterlere uygun şarkı bulunamadı.",
                                                            icon = Icons.Default.MusicNote
                                                        )
                                                    } else {
                                                        LazyColumn(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            items(groupTracks, key = { it.id }) { track ->
                                                                val isSelected = selectedTrackIds.contains(track.id)
                                                                LocalTrackItemCard(
                                                                    track = track,
                                                                    viewModel = viewModel,
                                                                    onPlay = { viewModel.playLocalQueue(groupTracks, groupTracks.indexOf(track)) },
                                                                    isSelected = isSelected,
                                                                    isSelectionMode = selectedTrackIds.isNotEmpty(),
                                                                    onSelectToggle = { viewModel.toggleTrackSelection(track.id) }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        TrackFilterOption.FOLDER -> {
                                            if (selectedFolderGroup == null) {
                                                val folderGroups = remember(localTracks, localSearchQuery) {
                                                    localTracks
                                                        .groupBy { getFolderName(it.filePath, it.uri) }
                                                        .map { (folderName, tracks) ->
                                                            FolderGroup(
                                                                folderName = folderName,
                                                                trackCount = tracks.size
                                                            )
                                                        }
                                                        .filter { group ->
                                                            localSearchQuery.isBlank() || group.folderName.matchesSearchQuery(localSearchQuery) || localTracks.any { getFolderName(it.filePath, it.uri).equals(group.folderName, ignoreCase = true) && it.title.matchesSearchQuery(localSearchQuery) }
                                                        }
                                                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.folderName })
                                                }

                                                if (folderGroups.isEmpty()) {
                                                    HomeScreenEmptyState(
                                                        text = if (localSearchQuery.isBlank()) "Klasör bulunamadı." else "Aradığınız kriterlere uygun klasör bulunamadı.",
                                                        icon = Icons.Default.Folder
                                                    )
                                                } else {
                                                    LazyColumn(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        items(folderGroups, key = { it.folderName }) { group ->
                                                            FolderCard(
                                                                folderName = group.folderName,
                                                                trackCount = group.trackCount,
                                                                onClick = { selectedFolderGroup = group.folderName }
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                val groupTracks = remember(localTracks, selectedFolderGroup, localSearchQuery) {
                                                    localTracks.filter { track ->
                                                        getFolderName(track.filePath, track.uri).equals(selectedFolderGroup, ignoreCase = true) && (localSearchQuery.isBlank() || track.title.matchesSearchQuery(localSearchQuery))
                                                    }
                                                }

                                                Column(modifier = Modifier.fillMaxSize()) {
                                                    GroupDetailHeader(
                                                        title = selectedFolderGroup!!,
                                                        subtitle = "${groupTracks.size} Şarkı",
                                                        onBack = { selectedFolderGroup = null }
                                                    )
                                                    if (groupTracks.isEmpty()) {
                                                        HomeScreenEmptyState(
                                                            text = "Aradığınız kriterlere uygun şarkı bulunamadı.",
                                                            icon = Icons.Default.MusicNote
                                                        )
                                                    } else {
                                                        LazyColumn(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            items(groupTracks, key = { it.id }) { track ->
                                                                val isSelected = selectedTrackIds.contains(track.id)
                                                                LocalTrackItemCard(
                                                                    track = track,
                                                                    viewModel = viewModel,
                                                                    onPlay = { viewModel.playLocalQueue(groupTracks, groupTracks.indexOf(track)) },
                                                                    isSelected = isSelected,
                                                                    isSelectionMode = selectedTrackIds.isNotEmpty(),
                                                                    onSelectToggle = { viewModel.toggleTrackSelection(track.id) }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                "favorites" -> {
                                    // FAVORİLER
                                    when (currentFilter) {
                                        TrackFilterOption.SONG -> {
                                            if (filteredFavorites.isEmpty()) {
                                                HomeScreenEmptyState(
                                                    text = if (localSearchQuery.isBlank()) stringResource(R.string.empty_favorites_message) else stringResource(R.string.no_music_found),
                                                    icon = Icons.Default.Favorite
                                                )
                                            } else {
                                                if (!isGridViewMode) {
                                                    LazyColumn(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        items(filteredFavorites, key = { it.videoId }) { track ->
                                                            val isSelected = selectedFavoriteVideoIds.contains(track.videoId)
                                                            TrackCard(
                                                                track = track,
                                                                viewModel = viewModel,
                                                                onPlay = { viewModel.playStreamQueue(filteredFavorites, filteredFavorites.indexOf(track)) },
                                                                isSelected = isSelected,
                                                                isSelectionMode = selectedFavoriteVideoIds.isNotEmpty(),
                                                                onSelectToggle = { viewModel.toggleFavoriteSelection(track.videoId) },
                                                                isFavoritesTab = true,
                                                                onRemoveFromFavorites = { viewModel.toggleFavorite(track) }
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    LazyVerticalGrid(
                                                        columns = GridCells.Fixed(2),
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        items(filteredFavorites, key = { it.videoId }) { track ->
                                                            val isSelected = selectedFavoriteVideoIds.contains(track.videoId)
                                                            TrackGridCard(
                                                                track = track,
                                                                viewModel = viewModel,
                                                                onPlay = { viewModel.playStreamQueue(filteredFavorites, filteredFavorites.indexOf(track)) },
                                                                isSelected = isSelected,
                                                                isSelectionMode = selectedFavoriteVideoIds.isNotEmpty(),
                                                                onSelectToggle = { viewModel.toggleFavoriteSelection(track.videoId) },
                                                                isFavoritesTab = true,
                                                                onRemoveFromFavorites = { viewModel.toggleFavorite(track) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        TrackFilterOption.ARTIST -> {
                                            if (selectedArtistGroup == null) {
                                                val artistGroups = remember(favorites, localSearchQuery) {
                                                    favorites
                                                        .groupBy { it.uploaderName.ifBlank { "Bilinmeyen Sanatçı" } }
                                                        .map { (artistName, tracks) ->
                                                            ArtistGroup(
                                                                artistName = artistName,
                                                                trackCount = tracks.size
                                                            )
                                                        }
                                                        .filter { group ->
                                                            localSearchQuery.isBlank() || group.artistName.matchesSearchQuery(localSearchQuery) || favorites.any { it.uploaderName.equals(group.artistName, ignoreCase = true) && it.title.matchesSearchQuery(localSearchQuery) }
                                                        }
                                                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.artistName })
                                                }

                                                if (artistGroups.isEmpty()) {
                                                    HomeScreenEmptyState(
                                                        text = if (localSearchQuery.isBlank()) "Sanatçı bulunamadı." else "Aradığınız kriterlere uygun sanatçı bulunamadı.",
                                                        icon = Icons.Default.Person
                                                    )
                                                } else {
                                                    LazyColumn(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        items(artistGroups, key = { it.artistName }) { group ->
                                                            ArtistCard(
                                                                artistName = group.artistName,
                                                                trackCount = group.trackCount,
                                                                onClick = { selectedArtistGroup = group.artistName }
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                val groupTracks = remember(favorites, selectedArtistGroup, localSearchQuery) {
                                                    favorites.filter { track ->
                                                        val artist = track.uploaderName.ifBlank { "Bilinmeyen Sanatçı" }
                                                        artist.equals(selectedArtistGroup, ignoreCase = true) && (localSearchQuery.isBlank() || track.title.matchesSearchQuery(localSearchQuery))
                                                    }
                                                }

                                                Column(modifier = Modifier.fillMaxSize()) {
                                                    GroupDetailHeader(
                                                        title = selectedArtistGroup!!,
                                                        subtitle = "${groupTracks.size} Şarkı",
                                                        onBack = { selectedArtistGroup = null }
                                                    )
                                                    if (groupTracks.isEmpty()) {
                                                        HomeScreenEmptyState(
                                                            text = "Aradığınız kriterlere uygun şarkı bulunamadı.",
                                                            icon = Icons.Default.Favorite
                                                        )
                                                    } else {
                                                        LazyColumn(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            items(groupTracks, key = { it.videoId }) { track ->
                                                                val isSelected = selectedFavoriteVideoIds.contains(track.videoId)
                                                                TrackCard(
                                                                    track = track,
                                                                    viewModel = viewModel,
                                                                    onPlay = { viewModel.playStreamQueue(groupTracks, groupTracks.indexOf(track)) },
                                                                    isSelected = isSelected,
                                                                    isSelectionMode = selectedFavoriteVideoIds.isNotEmpty(),
                                                                    onSelectToggle = { viewModel.toggleFavoriteSelection(track.videoId) },
                                                                    isFavoritesTab = true,
                                                                    onRemoveFromFavorites = { viewModel.toggleFavorite(track) }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        TrackFilterOption.ALBUM -> {
                                            if (selectedAlbumGroup == null) {
                                                val albumGroups = remember(favorites, localTracks, localSearchQuery) {
                                                    favorites
                                                        .groupBy { track ->
                                                            val matchingLocal = localTracks.find { it.id.toString() == track.videoId || it.uri == track.url }
                                                            matchingLocal?.album?.ifBlank { "Bilinmeyen Albüm" } ?: "Bilinmeyen Albüm"
                                                        }
                                                        .map { (albumName, tracks) ->
                                                            AlbumGroup(
                                                                albumTitle = albumName,
                                                                artistName = tracks.firstOrNull()?.uploaderName ?: "",
                                                                trackCount = tracks.size
                                                            )
                                                        }
                                                        .filter { group ->
                                                            localSearchQuery.isBlank() || group.albumTitle.matchesSearchQuery(localSearchQuery) || group.artistName.matchesSearchQuery(localSearchQuery)
                                                        }
                                                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.albumTitle })
                                                }

                                                if (albumGroups.isEmpty()) {
                                                    HomeScreenEmptyState(
                                                        text = if (localSearchQuery.isBlank()) "Albüm bulunamadı." else "Aradığınız kriterlere uygun albüm bulunamadı.",
                                                        icon = Icons.Default.Album
                                                    )
                                                } else {
                                                    LazyColumn(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        items(albumGroups, key = { it.albumTitle }) { group ->
                                                            AlbumCard(
                                                                albumTitle = group.albumTitle,
                                                                artistName = group.artistName,
                                                                trackCount = group.trackCount,
                                                                onClick = { selectedAlbumGroup = group.albumTitle }
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                val groupTracks = remember(favorites, localTracks, selectedAlbumGroup, localSearchQuery) {
                                                    favorites.filter { track ->
                                                        val matchingLocal = localTracks.find { it.id.toString() == track.videoId || it.uri == track.url }
                                                        val album = matchingLocal?.album?.ifBlank { "Bilinmeyen Albüm" } ?: "Bilinmeyen Albüm"
                                                        album.equals(selectedAlbumGroup, ignoreCase = true) && (localSearchQuery.isBlank() || track.title.matchesSearchQuery(localSearchQuery))
                                                    }
                                                }

                                                Column(modifier = Modifier.fillMaxSize()) {
                                                    GroupDetailHeader(
                                                        title = selectedAlbumGroup!!,
                                                        subtitle = "${groupTracks.size} Şarkı",
                                                        onBack = { selectedAlbumGroup = null }
                                                    )
                                                    if (groupTracks.isEmpty()) {
                                                        HomeScreenEmptyState(
                                                            text = "Aradığınız kriterlere uygun şarkı bulunamadı.",
                                                            icon = Icons.Default.Favorite
                                                        )
                                                    } else {
                                                        LazyColumn(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            items(groupTracks, key = { it.videoId }) { track ->
                                                                val isSelected = selectedFavoriteVideoIds.contains(track.videoId)
                                                                TrackCard(
                                                                    track = track,
                                                                    viewModel = viewModel,
                                                                    onPlay = { viewModel.playStreamQueue(groupTracks, groupTracks.indexOf(track)) },
                                                                    isSelected = isSelected,
                                                                    isSelectionMode = selectedFavoriteVideoIds.isNotEmpty(),
                                                                    onSelectToggle = { viewModel.toggleFavoriteSelection(track.videoId) },
                                                                    isFavoritesTab = true,
                                                                    onRemoveFromFavorites = { viewModel.toggleFavorite(track) }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        TrackFilterOption.FOLDER -> {
                                            if (selectedFolderGroup == null) {
                                                val folderGroups = remember(favorites, localTracks, localSearchQuery) {
                                                    favorites
                                                        .groupBy { track -> getFolderNameForDto(track, localTracks) }
                                                        .map { (folderName, tracks) ->
                                                            FolderGroup(
                                                                folderName = folderName,
                                                                trackCount = tracks.size
                                                            )
                                                        }
                                                        .filter { group ->
                                                            localSearchQuery.isBlank() || group.folderName.matchesSearchQuery(localSearchQuery) || favorites.any { getFolderNameForDto(it, localTracks).equals(group.folderName, ignoreCase = true) && (it.title.matchesSearchQuery(localSearchQuery) || it.uploaderName.matchesSearchQuery(localSearchQuery)) }
                                                        }
                                                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.folderName })
                                                }

                                                if (folderGroups.isEmpty()) {
                                                    HomeScreenEmptyState(
                                                        text = if (localSearchQuery.isBlank()) "Klasör bulunamadı." else "Aradığınız kriterlere uygun klasör bulunamadı.",
                                                        icon = Icons.Default.Folder
                                                    )
                                                } else {
                                                    LazyColumn(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        items(folderGroups, key = { it.folderName }) { group ->
                                                            FolderCard(
                                                                folderName = group.folderName,
                                                                trackCount = group.trackCount,
                                                                onClick = { selectedFolderGroup = group.folderName }
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                val groupTracks = remember(favorites, localTracks, selectedFolderGroup, localSearchQuery) {
                                                    favorites.filter { track ->
                                                        getFolderNameForDto(track, localTracks).equals(selectedFolderGroup, ignoreCase = true) && (localSearchQuery.isBlank() || track.title.matchesSearchQuery(localSearchQuery))
                                                    }
                                                }

                                                Column(modifier = Modifier.fillMaxSize()) {
                                                    GroupDetailHeader(
                                                        title = selectedFolderGroup!!,
                                                        subtitle = "${groupTracks.size} Şarkı",
                                                        onBack = { selectedFolderGroup = null }
                                                    )
                                                    if (groupTracks.isEmpty()) {
                                                        HomeScreenEmptyState(
                                                            text = "Aradığınız kriterlere uygun şarkı bulunamadı.",
                                                            icon = Icons.Default.Favorite
                                                        )
                                                    } else {
                                                        LazyColumn(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            items(groupTracks, key = { it.videoId }) { track ->
                                                                val isSelected = selectedFavoriteVideoIds.contains(track.videoId)
                                                                TrackCard(
                                                                    track = track,
                                                                    viewModel = viewModel,
                                                                    onPlay = { viewModel.playStreamQueue(groupTracks, groupTracks.indexOf(track)) },
                                                                    isSelected = isSelected,
                                                                    isSelectionMode = selectedFavoriteVideoIds.isNotEmpty(),
                                                                    onSelectToggle = { viewModel.toggleFavoriteSelection(track.videoId) },
                                                                    isFavoritesTab = true,
                                                                    onRemoveFromFavorites = { viewModel.toggleFavorite(track) }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            "playlists" -> {
                                // ÇALMA LİSTESİ
                                Column(modifier = Modifier.fillMaxSize()) {
                                    if (selectedPlaylistId == null) {
                                        Button(
                                            onClick = { showCreatePlaylistDialog = true },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.create_playlist))
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (dbPlaylists.isEmpty()) {
                                            HomeScreenEmptyState(
                                                text = stringResource(R.string.no_playlists_yet),
                                                icon = Icons.AutoMirrored.Filled.QueueMusic
                                            )
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                items(dbPlaylists, key = { it.id }) { playlist ->
                                                    PlaylistSummaryCard(
                                                        playlist = playlist,
                                                        viewModel = viewModel,
                                                        onClick = { selectedPlaylistId = playlist.id }
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        val activePlaylist = dbPlaylists.find { it.id == selectedPlaylistId }
                                        if (activePlaylist == null) {
                                            selectedPlaylistId = null
                                        } else {
                                            val playlistTracks by viewModel.getPlaylistTracks(activePlaylist.id).collectAsState(emptyList())
                                            val filteredPlaylistTracks = remember(playlistTracks, localSearchQuery, localTracks) {
                                                if (localSearchQuery.isBlank()) {
                                                    playlistTracks
                                                } else {
                                                    viewModel.searchPlaylistTracks(localSearchQuery, playlistTracks, localTracks)
                                                }
                                            }

                                            // Drill-down header
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(onClick = { selectedPlaylistId = null }) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = stringResource(R.string.back)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = activePlaylist.name,
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = stringResource(R.string.track_count, playlistTracks.size),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                if (playlistTracks.isNotEmpty()) {
                                                    FilledIconButton(
                                                        onClick = { viewModel.playPlaylistTracks(playlistTracks) },
                                                        shape = CircleShape,
                                                        colors = IconButtonDefaults.filledIconButtonColors(
                                                            containerColor = MaterialTheme.colorScheme.primary,
                                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = stringResource(R.string.play_all)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                            Spacer(modifier = Modifier.height(8.dp))

                                            if (filteredPlaylistTracks.isEmpty()) {
                                                HomeScreenEmptyState(
                                                    text = if (playlistTracks.isEmpty()) stringResource(R.string.empty_playlist_message) else stringResource(R.string.empty_songs_message),
                                                    icon = Icons.AutoMirrored.Filled.QueueMusic
                                                )
                                            } else {
                                                LazyColumn(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    items(filteredPlaylistTracks, key = { it.id }) { trackEntity ->
                                                        val trackDto = remember(trackEntity) {
                                                            OnlineSearchResultDto(
                                                                videoId = trackEntity.trackId,
                                                                title = trackEntity.title,
                                                                uploaderName = trackEntity.artist,
                                                                durationString = trackEntity.duration,
                                                                thumbnailUrl = trackEntity.artworkUri ?: "",
                                                                url = trackEntity.mediaUri
                                                            )
                                                        }
                                                        TrackCard(
                                                            track = trackDto,
                                                            viewModel = viewModel,
                                                            onPlay = {
                                                                viewModel.playPlaylistTracks(playlistTracks, playlistTracks.indexOf(trackEntity))
                                                            },
                                                            isInPlaylist = true,
                                                            onRemoveFromPlaylist = {
                                                                viewModel.removeTrackFromPlaylist(activePlaylist.id, trackEntity.trackId)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            "online" -> {
                                // ONLINE MODE
                                if (onlineQuery.isBlank()) {
                                    HomeScreenEmptyState(
                                        text = stringResource(R.string.online_search_prompt, if (onlineTabLabel == "Keşfet" || onlineTabLabel == "YT Online") stringResource(R.string.discover) else onlineTabLabel),
                                        icon = Icons.Default.Search
                                    )
                                } else {
                                    // Online search results
                                    if (isSearchingOnline || uiState is SearchUiState.Loading) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        }
                                    } else if (uiState is SearchUiState.Error) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                (uiState as SearchUiState.Error).message,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    } else if (onlineResults.isNotEmpty() || uiState is SearchUiState.Success) {
                                        val results = if (onlineResults.isNotEmpty()) onlineResults else (uiState as? SearchUiState.Success)?.results ?: emptyList()
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            itemsIndexed(
                                                items = results,
                                                key = { index, track -> if (track.videoId.isNotBlank()) "${track.videoId}_$index" else "idx_$index" }
                                            ) { _, track ->
                                                if (isOnlineSearchSource) {
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
                                                    OnlineSearchLiveCard(
                                                        track = track,
                                                        downloadInfoStatus = downloadStatus,
                                                        downloadProgress = downloadInfo?.progress ?: 0,
                                                        viewModel = viewModel,
                                                        enableDownloads = enableOnlineFeatures,
                                                        onPlayStream = { viewModel.playStreamQueue(results, results.indexOf(track)) },
                                                        onDownload = { viewModel.onDownloadClicked(track) }
                                                    )
                                                } else {
                                                    TrackCard(
                                                        track = track,
                                                        viewModel = viewModel,
                                                        onPlay = { viewModel.playStreamQueue(results, results.indexOf(track)) }
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        HomeScreenEmptyState(
                                            text = "Aramak istediğiniz müzik adını yazın...",
                                            icon = Icons.Default.Search
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        var newPlaylistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text(stringResource(R.string.create_playlist_title)) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text(stringResource(R.string.playlist_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newPlaylistName.isNotBlank()) {
                                viewModel.createPlaylist(newPlaylistName)
                                showCreatePlaylistDialog = false
                            }
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        viewModel.createPlaylist(newPlaylistName)
                        showCreatePlaylistDialog = false
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text(stringResource(R.string.delete_track_title)) },
            text = { Text(stringResource(R.string.delete_selected_favorites_confirm, selectedTrackIds.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteSelectedDialog = false
                        viewModel.deleteSelectedTracks(selectedTrackIds)
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddSelectedToPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showAddSelectedToPlaylistDialog = false },
            title = { Text(stringResource(R.string.action_add_to_playlist)) },
            text = {
                if (dbPlaylists.isEmpty()) {
                    Text(stringResource(R.string.no_playlists_yet))
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dbPlaylists, key = { it.id }) { playlist ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showAddSelectedToPlaylistDialog = false
                                        viewModel.addSelectedToPlaylist(playlist.id, selectedTrackIds)
                                    },
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddSelectedToPlaylistDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showDeleteSelectedFavoritesDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedFavoritesDialog = false },
            title = { Text(stringResource(R.string.delete_selected_favorites_title)) },
            text = { Text(stringResource(R.string.delete_selected_favorites_confirm, selectedFavoriteVideoIds.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteSelectedFavoritesDialog = false
                        viewModel.deleteSelectedFavorites(selectedFavoriteVideoIds)
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedFavoritesDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddSelectedFavoritesToPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showAddSelectedFavoritesToPlaylistDialog = false },
            title = { Text(stringResource(R.string.action_add_to_playlist)) },
            text = {
                if (dbPlaylists.isEmpty()) {
                    Text(stringResource(R.string.no_playlists_yet))
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dbPlaylists, key = { it.id }) { playlist ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showAddSelectedFavoritesToPlaylistDialog = false
                                        viewModel.addSelectedFavoritesToPlaylist(playlist.id, selectedFavoriteVideoIds)
                                    },
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddSelectedFavoritesToPlaylistDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }
}
}
