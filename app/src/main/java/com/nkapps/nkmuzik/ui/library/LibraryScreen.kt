package com.nkapps.nkmuzik.ui.library

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import coil.compose.AsyncImage
import com.nkapps.nkmuzik.R
import com.nkapps.nkmuzik.data.db.entity.PlaylistEntity
import com.nkapps.nkmuzik.data.model.LocalTrack
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.data.preferences.DeletedTracksStore
import com.nkapps.nkmuzik.data.repository.LocalMediaRepository
import com.nkapps.nkmuzik.data.repository.PlaylistRepository
import com.nkapps.nkmuzik.downloader.DownloadRepository
import com.nkapps.nkmuzik.ui.common.DeleteTrackConfirmationDialog
import com.nkapps.nkmuzik.ui.common.TrackInfoDialog
import com.nkapps.nkmuzik.ui.search.SearchViewModel
import com.nkapps.nkmuzik.ui.search.matchesSearchQuery
import com.nkapps.nkmuzik.ui.theme.MysticGoldMat
import com.nkapps.nkmuzik.utils.ArtworkUtils
import com.nkapps.nkmuzik.utils.MediaTitleParser
import com.nkapps.nkmuzik.utils.ShareUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject
import androidx.compose.foundation.lazy.rememberLazyListState
import com.nkapps.nkmuzik.ui.common.FastScrollbar

data class ArtistOrAlbumGroup(
    val name: String,
    val trackCount: Int,
    val tracks: List<LocalTrack>
)

@Composable
fun LibraryScreen(
    onPlayTrack: (LocalTrack) -> Unit,
    searchViewModel: SearchViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val tracks by libraryViewModel.tracks.collectAsState()
    val downloadedFiles by libraryViewModel.downloadedFiles.collectAsState()
    val activeDownloads by libraryViewModel.activeDownloads.collectAsState()
    val dbPlaylists by searchViewModel.dbPlaylists.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    var trackToAddToPlaylist by remember { mutableStateOf<LocalTrack?>(null) }
    var selectedPlaylistForDetail by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val allTracks = remember(tracks, downloadedFiles) {
        val trackPaths = tracks.mapNotNull { it.filePath }.toSet()
        val uniqueDownloaded = downloadedFiles.filter { df ->
            df.filePath == null || !trackPaths.contains(df.filePath)
        }
        tracks + uniqueDownloaded
    }

    val filteredTracks = remember(allTracks, searchQuery) {
        if (searchQuery.isBlank()) allTracks else {
            searchViewModel.searchLocalTracks(searchQuery, allTracks)
        }
    }

    val artistGroups = remember(filteredTracks) {
        filteredTracks.groupBy { it.artist }.map { (artist, list) ->
            ArtistOrAlbumGroup(name = artist, trackCount = list.size, tracks = list)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(searchQuery) {
                detectTapGestures(onTap = {
                    if (searchQuery.isNotBlank()) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                })
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_hint_all)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                    }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            selectedPlaylistForDetail?.let { playlist ->
                PlaylistDetailView(
                    playlist = playlist,
                    searchViewModel = searchViewModel,
                    onBack = { selectedPlaylistForDetail = null }
                )
                return@Surface
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.library_songs_tab, filteredTracks.size)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.library_artists_tab, artistGroups.size)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.library_playlists_tab, dbPlaylists.size)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    if (filteredTracks.isEmpty() && activeDownloads.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_music_found), color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(activeDownloads) { workInfo ->
                                val title = workInfo.outputData.getString("title") ?: stringResource(R.string.downloading_music)
                                val progress = workInfo.progress.getInt("progress", 0)
                                val cardBg = MaterialTheme.colorScheme.surfaceContainer
                                val sheenBrush = Brush.horizontalGradient(
                                    listOf(
                                        cardBg,
                                        MysticGoldMat.copy(alpha = 0.10f)
                                    )
                                )
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = cardBg)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(sheenBrush)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                                IconButton(onClick = {
                                                    WorkManager.getInstance(context).cancelWorkById(workInfo.id)
                                                }) {
                                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.cancel), tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { if (progress > 0) progress / 100f else 0.5f },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            items(filteredTracks) { track ->
                                TrackRowCard(
                                    track = track,
                                    onPlayTrack = onPlayTrack,
                                    onPlayNextTrack = { searchViewModel.playNextLocalTrack(track) },
                                    onAddToPlaylist = { trackToAddToPlaylist = track },
                                    onDelete = { libraryViewModel.deleteTrack(track) }
                                )
                            }
                        }
                        
                        if (filteredTracks.size > 20) {
                            FastScrollbar(
                                listState = listState,
                                items = filteredTracks,
                                itemToChar = { (it as LocalTrack).title.firstOrNull() ?: '#' },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            } // closes 0 -> {

            1 -> {
                if (artistGroups.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_artist_found), color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(artistGroups) { group ->
                                var expanded by remember { mutableStateOf(false) }
                                val cardBg = MaterialTheme.colorScheme.surfaceContainer
                                val sheenBrush = Brush.horizontalGradient(
                                    listOf(
                                        cardBg,
                                        MysticGoldMat.copy(alpha = 0.10f)
                                    )
                                )
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = cardBg)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(sheenBrush)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { expanded = !expanded },
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(group.name, style = MaterialTheme.typography.titleMedium)
                                                        Text(stringResource(R.string.track_count, group.trackCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                                    }
                                                }
                                                IconButton(onClick = {
                                                    searchViewModel.playLocalQueue(group.tracks)
                                                }) {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play_all), tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }

                                            AnimatedVisibility(visible = expanded) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                                    group.tracks.forEach { subTrack ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable { onPlayTrack(subTrack) }
                                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(subTrack.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                                            }
                                                            IconButton(onClick = { onPlayTrack(subTrack) }) {
                                                                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.action_play), modifier = Modifier.size(20.dp))
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
                    }
                }

                2 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Button(
                            onClick = { showCreatePlaylistDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.create_playlist))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (dbPlaylists.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_playlists_yet), color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(dbPlaylists) { playlist ->
                                    val playlistTracks by searchViewModel.getPlaylistTracks(playlist.id).collectAsState(emptyList())
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
                                            .clickable { selectedPlaylistForDetail = playlist },
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
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.QueueMusic,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Column {
                                                        Text(playlist.name, style = MaterialTheme.typography.titleMedium)
                                                        Text(stringResource(R.string.song_count, playlistTracks.size), style = MaterialTheme.typography.bodySmall, color = Color(0xFFB0B0B0))
                                                    }
                                                }
                                                Row {
                                                    if (playlistTracks.isNotEmpty()) {
                                                        IconButton(onClick = {
                                                            searchViewModel.playPlaylistTracks(playlistTracks)
                                                        }) {
                                                            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.action_play), tint = MaterialTheme.colorScheme.primary)
                                                        }
                                                    }
                                                    IconButton(onClick = {
                                                        searchViewModel.deletePlaylist(playlist.id)
                                                    }) {
                                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
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
            }
        }
    }

    trackToAddToPlaylist?.let { track ->
        AddToPlaylistDialog(
            playlists = dbPlaylists,
            onSelectPlaylist = { playlist ->
                searchViewModel.addLocalTrackToPlaylist(playlist.id, track)
            },
            onCreateNewPlaylist = { name ->
                searchViewModel.createPlaylist(name)
            },
            onDismiss = { trackToAddToPlaylist = null }
        )
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text(stringResource(R.string.create_playlist_title)) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text(stringResource(R.string.playlist_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newPlaylistName.isNotBlank()) {
                                searchViewModel.createPlaylist(newPlaylistName)
                                newPlaylistName = ""
                                showCreatePlaylistDialog = false
                            }
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        searchViewModel.createPlaylist(newPlaylistName)
                        newPlaylistName = ""
                        showCreatePlaylistDialog = false
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
fun TrackRowCard(
    track: LocalTrack,
    onPlayTrack: (LocalTrack) -> Unit,
    onPlayNextTrack: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
    isInPlaylist: Boolean = false,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    isFavoritesTab: Boolean = false,
    onRemoveFromFavorites: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }


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
            .clickable { onPlayTrack(track) },
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
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.small)
                ) {
                    AsyncImage(
                        model = ArtworkUtils.getArtworkModel(LocalContext.current, track.artworkUri ?: track.uri, track.title, track.id.toString()),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = null
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
                        text = "${track.artist} • ${track.duration}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { onPlayTrack(track) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.action_play), tint = MaterialTheme.colorScheme.primary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val searchViewModel: SearchViewModel = hiltViewModel()
                    val isOnline = track.isOnlineStream || track.uri.startsWith("http://") || track.uri.startsWith("https://")
                    val isLocalFileOnDisk = !track.filePath.isNullOrBlank() && File(track.filePath).exists()
                    val isDownloadedLocally = isLocalFileOnDisk || searchViewModel.isDownloaded(OnlineSearchResultDto(videoId = track.videoId ?: track.id.toString(), title = track.title, uploaderName = track.artist, durationString = track.duration, thumbnailUrl = track.artworkUri ?: "", url = track.uri))
                    if (isOnline && !isDownloadedLocally) {
                        Icon(
                            imageVector = Icons.Rounded.CloudQueue,
                            contentDescription = stringResource(R.string.section_online),
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 2.dp)
                        )
                    }
                    Box {
                        IconButton(onClick = { isExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
                        }
                        DropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_play)) },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                onClick = {
                                    isExpanded = false
                                    onPlayTrack(track)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_add_to_queue)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                                onClick = {
                                    isExpanded = false
                                    onPlayNextTrack()
                                }
                            )
                            if (isInPlaylist || onRemoveFromPlaylist != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_remove_from_playlist)) },
                                    leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        isExpanded = false
                                        onRemoveFromPlaylist?.invoke()
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_add_to_playlist)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                                    onClick = {
                                        isExpanded = false
                                        onAddToPlaylist()
                                    }
                                )
                            }
                            if (isFavoritesTab || onRemoveFromFavorites != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_remove_from_favorites)) },
                                    leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        isExpanded = false
                                        onRemoveFromFavorites?.invoke()
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_add_to_favorites)) },
                                    leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                    onClick = {
                                        isExpanded = false
                                        val dto = OnlineSearchResultDto(
                                            videoId = track.videoId ?: track.id.toString(),
                                            title = track.title,
                                            uploaderName = track.artist,
                                            durationString = track.duration,
                                            thumbnailUrl = track.artworkUri ?: "",
                                            url = track.uri
                                        )
                                        searchViewModel.toggleFavorite(dto)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_share)) },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    isExpanded = false
                                    ShareUtils.shareAudioFile(
                                        context = context,
                                        filePath = track.filePath,
                                        uriString = track.uri,
                                        title = track.title
                                    )
                                }
                            )


                        }
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        TrackInfoDialog(
            title = track.title,
            artist = track.artist,
            duration = track.duration,
            uriString = track.uri,
            filePath = track.filePath,
            onDismiss = { showInfoDialog = false }
        )
    }


}

@Composable
fun PlaylistDetailView(
    playlist: PlaylistEntity,
    searchViewModel: SearchViewModel,
    onBack: () -> Unit
) {
    val tracks by searchViewModel.getPlaylistTracks(playlist.id).collectAsState(emptyList())
    var showRenameDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf(playlist.name) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(playlist.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.song_count, tracks.size), style = MaterialTheme.typography.bodySmall, color = Color(0xFFB0B0B0))
            }
            IconButton(onClick = { showRenameDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
            }
            IconButton(onClick = {
                searchViewModel.deletePlaylist(playlist.id)
                onBack()
            }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
            }
            if (tracks.isNotEmpty()) {
                FilledTonalIconButton(onClick = {
                    searchViewModel.playPlaylistTracks(tracks)
                }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.action_play))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.empty_playlist_message), color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tracks) { trackEntity ->
                    var isExpanded by remember { mutableStateOf(false) }
                    var showInfoDialog by remember { mutableStateOf(false) }
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    val context = LocalContext.current

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
                            .clickable {
                                searchViewModel.playPlaylistTracks(tracks, tracks.indexOf(trackEntity))
                            },
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
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .aspectRatio(1f)
                                        .clip(MaterialTheme.shapes.small)
                                ) {
                                    AsyncImage(
                                        model = ArtworkUtils.getArtworkModel(LocalContext.current, trackEntity.artworkUri, trackEntity.title, trackEntity.trackId),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = trackEntity.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${trackEntity.artist} • ${trackEntity.duration}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                val isOnlineStream = !trackEntity.isLocal || trackEntity.mediaUri.startsWith("http://") || trackEntity.mediaUri.startsWith("https://")
                                val localFilePath = if (trackEntity.mediaUri.startsWith("file://")) trackEntity.mediaUri.removePrefix("file://") else if (trackEntity.mediaUri.startsWith("/")) trackEntity.mediaUri else null
                                val isLocalFileOnDisk = localFilePath != null && File(localFilePath).exists()
                                val isDownloadedLocally = isLocalFileOnDisk || searchViewModel.isDownloaded(OnlineSearchResultDto(videoId = trackEntity.trackId, title = trackEntity.title, uploaderName = trackEntity.artist, durationString = trackEntity.duration, thumbnailUrl = trackEntity.artworkUri ?: "", url = trackEntity.mediaUri))
                                if (isOnlineStream && !isDownloadedLocally) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudQueue,
                                        contentDescription = stringResource(R.string.section_online),
                                        tint = Color(0xFFD4AF37),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(end = 2.dp)
                                    )
                                }
                                Box {
                                    IconButton(onClick = { isExpanded = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
                                    }
                                    DropdownMenu(
                                        expanded = isExpanded,
                                        onDismissRequest = { isExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_play)) },
                                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                            onClick = {
                                                isExpanded = false
                                                searchViewModel.playPlaylistTracks(tracks, tracks.indexOf(trackEntity))
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_add_to_queue)) },
                                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                                            onClick = {
                                                isExpanded = false
                                                val dto = OnlineSearchResultDto(
                                                    videoId = trackEntity.trackId,
                                                    title = trackEntity.title,
                                                    uploaderName = trackEntity.artist,
                                                    durationString = trackEntity.duration,
                                                    thumbnailUrl = trackEntity.artworkUri ?: "",
                                                    url = trackEntity.mediaUri
                                                )
                                                searchViewModel.addToQueue(dto)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_remove_from_playlist)) },
                                            leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                isExpanded = false
                                                searchViewModel.removeTrackFromPlaylist(playlist.id, trackEntity.trackId)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_add_to_favorites)) },
                                            leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                            onClick = {
                                                isExpanded = false
                                                val dto = OnlineSearchResultDto(
                                                    videoId = trackEntity.trackId,
                                                    title = trackEntity.title,
                                                    uploaderName = trackEntity.artist,
                                                    durationString = trackEntity.duration,
                                                    thumbnailUrl = trackEntity.artworkUri ?: "",
                                                    url = trackEntity.mediaUri
                                                )
                                                searchViewModel.toggleFavorite(dto)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_share)) },
                                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                            onClick = {
                                                isExpanded = false
                                                ShareUtils.shareAudioFile(
                                                    context = context,
                                                    filePath = localFilePath,
                                                    uriString = trackEntity.mediaUri,
                                                    title = trackEntity.title
                                                )
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.delete)) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                isExpanded = false
                                                showDeleteDialog = true
                                            }
                                        )
                                    }
                                }

                                if (showInfoDialog) {
                                    TrackInfoDialog(
                                        title = trackEntity.title,
                                        artist = trackEntity.artist,
                                        duration = trackEntity.duration,
                                        uriString = trackEntity.mediaUri,
                                        filePath = localFilePath,
                                        onDismiss = { showInfoDialog = false }
                                    )
                                }

                                if (showDeleteDialog) {
                                    DeleteTrackConfirmationDialog(
                                        trackTitle = trackEntity.title,
                                        onConfirm = {
                                            searchViewModel.removeTrackFromPlaylist(playlist.id, trackEntity.trackId)
                                        },
                                        onDismiss = { showDeleteDialog = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.rename_playlist)) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text(stringResource(R.string.playlist_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newPlaylistName.isNotBlank()) {
                                searchViewModel.updatePlaylistName(playlist.id, newPlaylistName)
                                showRenameDialog = false
                            }
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        searchViewModel.updatePlaylistName(playlist.id, newPlaylistName)
                        showRenameDialog = false
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
