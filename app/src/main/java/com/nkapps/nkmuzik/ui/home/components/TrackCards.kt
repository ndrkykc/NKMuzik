package com.nkapps.nkmuzik.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nkapps.nkmuzik.R
import com.nkapps.nkmuzik.data.model.LocalTrack
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.ui.common.DeleteTrackConfirmationDialog
import com.nkapps.nkmuzik.ui.common.TrackInfoDialog
import com.nkapps.nkmuzik.ui.library.AddToPlaylistDialog
import com.nkapps.nkmuzik.ui.search.SearchViewModel
import com.nkapps.nkmuzik.ui.theme.MysticGoldMat
import com.nkapps.nkmuzik.utils.ArtworkUtils
import com.nkapps.nkmuzik.utils.ShareUtils
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalTrackItemCard(
    track: LocalTrack,
    viewModel: SearchViewModel,
    onPlay: () -> Unit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onSelectToggle: () -> Unit = {},
    isInPlaylist: Boolean = false,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    isFavoritesTab: Boolean = false,
    onRemoveFromFavorites: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf(false) }
    val dbPlaylists by viewModel.dbPlaylists.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val isFav = remember(favorites, track) {
        favorites.any { fav ->
            fav.videoId == track.id.toString() || fav.url == track.uri || fav.url == (track.filePath ?: "")
        }
    }

    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val sheenBrush = if (isSelected) {
        Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                cardBg,
                MysticGoldMat.copy(alpha = 0.10f)
            )
        )
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        cardBg
    }

    val cardModifier = if (isSelectionMode) {
        Modifier
            .fillMaxWidth()
            .clickable { onSelectToggle() }
    } else {
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onSelectToggle
            )
    }

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (isSelected) stringResource(R.string.selected_count, 1) else null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium)
                ) {
                    val artModel = ArtworkUtils.getArtworkModel(context, track.artworkUri, track.title, track.id.toString(), track.duration, track.artist)
                    if (artModel != null) {
                        AsyncImage(
                            model = artModel,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isOnline = track.isOnlineStream || track.uri.startsWith("http://") || track.uri.startsWith("https://")
                    val isLocalFileOnDisk = !track.filePath.isNullOrBlank() && File(track.filePath).exists()
                    val isDownloadedLocally = isLocalFileOnDisk || viewModel.isDownloaded(OnlineSearchResultDto(videoId = track.videoId ?: track.id.toString(), title = track.title, uploaderName = track.artist, durationString = track.duration, thumbnailUrl = track.artworkUri ?: "", url = track.uri))
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
                    if (!isSelectionMode) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_play)) },
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                    onClick = { showMenu = false; onPlay() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_add_to_queue)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.addToQueue(track)
                                    }
                                )
                                if (isInPlaylist || onRemoveFromPlaylist != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_remove_from_playlist)) },
                                        leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showMenu = false
                                            onRemoveFromPlaylist?.invoke()
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_add_to_playlist)) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            showAddToPlaylistDialog = true
                                        }
                                    )
                                }
                                if (isFav || isFavoritesTab || onRemoveFromFavorites != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_remove_from_favorites)) },
                                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showMenu = false
                                            if (onRemoveFromFavorites != null) {
                                                onRemoveFromFavorites()
                                            } else {
                                                val dto = viewModel.localTrackToDto(track)
                                                viewModel.toggleFavorite(dto)
                                            }
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_add_to_favorites)) },
                                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            val dto = viewModel.localTrackToDto(track)
                                            viewModel.toggleFavorite(dto)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_share)) },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        ShareUtils.shareAudioFile(
                                            context = context,
                                            filePath = track.filePath,
                                            uriString = track.uri,
                                            title = track.title
                                        )
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_edit)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showMenu = false
                                        showEditDialog = true
                                    }
                                )

                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddToPlaylistDialog) {
        val dto = viewModel.localTrackToDto(track)
        AddToPlaylistDialog(
            playlists = dbPlaylists,
            onSelectPlaylist = { playlist ->
                viewModel.addTrackToPlaylist(playlist.id, dto)
            },
            onCreateNewPlaylist = { name ->
                viewModel.createPlaylist(name)
            },
            onDismiss = { showAddToPlaylistDialog = false }
        )
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

    if (showEditDialog) {
        var isSavingEdit by remember { mutableStateOf(false) }
        com.nkapps.nkmuzik.ui.common.EditTrackScreen(
            track = track,
            isSaving = isSavingEdit,
            onConfirm = { newTitle, newArtist, newAlbum, newArtworkUri, newLyrics ->
                isSavingEdit = true
                viewModel.updateLocalTrackMetaData(
                    track = track,
                    newTitle = newTitle,
                    newArtist = newArtist,
                    newAlbum = newAlbum,
                    newArtworkUri = newArtworkUri,
                    newLyrics = newLyrics
                ) {
                    isSavingEdit = false
                    showEditDialog = false
                }
            },
            onDismiss = { showEditDialog = false }
        )
    }


}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackCard(
    track: OnlineSearchResultDto,
    viewModel: SearchViewModel,
    onPlay: () -> Unit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onSelectToggle: () -> Unit = {},
    isInPlaylist: Boolean = false,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    isFavoritesTab: Boolean = false,
    onRemoveFromFavorites: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf(false) }
    var isSavingEdit by remember { mutableStateOf(false) }
    val dbPlaylists by viewModel.dbPlaylists.collectAsState()

    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val sheenBrush = if (isSelected) {
        Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                cardBg,
                MysticGoldMat.copy(alpha = 0.10f)
            )
        )
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        cardBg
    }

    val cardModifier = if (isSelectionMode) {
        Modifier
            .fillMaxWidth()
            .clickable { onSelectToggle() }
    } else {
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onSelectToggle
            )
    }

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (isSelected) stringResource(R.string.selected_count, 1) else null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium)
                ) {
                    val artModel = ArtworkUtils.getArtworkModel(context, track.thumbnailUrl, track.title, track.videoId, track.durationString, track.uploaderName)
                    if (artModel != null) {
                        AsyncImage(
                            model = artModel,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isOnlineStream = track.url.startsWith("http://") || track.url.startsWith("https://") || track.videoId.isNotBlank()
                    val localFilePath = if (track.url.startsWith("file://")) track.url.removePrefix("file://") else if (track.url.startsWith("/")) track.url else null
                    val isLocalFileOnDisk = localFilePath != null && File(localFilePath).exists()
                    val isDownloadedLocally = isLocalFileOnDisk || viewModel.isDownloaded(track)
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
                    if (!isSelectionMode) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_play)) },
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                    onClick = { showMenu = false; onPlay() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_add_to_queue)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.addToQueue(track)
                                    }
                                )
                                if (isInPlaylist || onRemoveFromPlaylist != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_remove_from_playlist)) },
                                        leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showMenu = false
                                            onRemoveFromPlaylist?.invoke()
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_add_to_playlist)) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            showAddToPlaylistDialog = true
                                        }
                                    )
                                }
                                if (isFavoritesTab || onRemoveFromFavorites != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_remove_from_favorites)) },
                                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showMenu = false
                                            if (onRemoveFromFavorites != null) {
                                                onRemoveFromFavorites()
                                            } else {
                                                viewModel.toggleFavorite(track)
                                            }
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_add_to_favorites)) },
                                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            viewModel.toggleFavorite(track)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_share)) },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        ShareUtils.shareAudioFile(
                                            context = context,
                                            filePath = null,
                                            uriString = track.url,
                                            title = track.title
                                        )
                                    }
                                )

                                if (track.url.startsWith("file://") || track.url.startsWith("/")) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_edit)) },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        onClick = {
                                            showMenu = false
                                            showEditDialog = true
                                        }
                                    )
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        val localTrack = LocalTrack(
            id = track.videoId.hashCode().toLong(),
            title = track.title,
            artist = track.uploaderName,
            album = "İndirilenler",
            duration = track.durationString,
            uri = track.url,
            artworkUri = track.thumbnailUrl,
            filePath = if (track.url.startsWith("file://") || track.url.startsWith("/")) track.url else null,
            videoId = track.videoId
        )

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { if (!isSavingEdit) showEditDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.nkapps.nkmuzik.ui.common.EditTrackScreen(
                track = localTrack,
                isSaving = isSavingEdit,
                onConfirm = { newTitle, newArtist, newAlbum, newArtworkUri, newLyrics ->
                    isSavingEdit = true
                    viewModel.updateLocalTrackMetaData(
                        track = localTrack,
                        newTitle = newTitle,
                        newArtist = newArtist,
                        newAlbum = newAlbum,
                        newArtworkUri = newArtworkUri,
                        newLyrics = newLyrics
                    ) {
                        isSavingEdit = false
                        showEditDialog = false
                    }
                },
                onDismiss = { showEditDialog = false }
            )
        }
    }

    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            playlists = dbPlaylists,
            onSelectPlaylist = { playlist ->
                viewModel.addTrackToPlaylist(playlist.id, track)
            },
            onCreateNewPlaylist = { name ->
                viewModel.createPlaylist(name)
            },
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }

    if (showInfoDialog) {
        TrackInfoDialog(
            title = track.title,
            artist = track.uploaderName,
            duration = track.durationString,
            uriString = track.url,
            filePath = null,
            onDismiss = { showInfoDialog = false }
        )
    }


}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalTrackGridCard(
    track: LocalTrack,
    viewModel: SearchViewModel,
    onPlay: () -> Unit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onSelectToggle: () -> Unit = {},
    isInPlaylist: Boolean = false,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    isFavoritesTab: Boolean = false,
    onRemoveFromFavorites: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val dbPlaylists by viewModel.dbPlaylists.collectAsState()

    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        cardBg
    }

    val cardModifier = if (isSelectionMode) {
        Modifier
            .fillMaxWidth()
            .clickable { onSelectToggle() }
    } else {
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onSelectToggle
            )
    }

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                val artModel = ArtworkUtils.getArtworkModel(context, track.artworkUri, track.title, track.id.toString(), track.duration, track.artist)
                if (artModel != null) {
                    AsyncImage(
                        model = artModel,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${track.artist} • ${track.duration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!isSelectionMode) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = stringResource(R.string.more),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_play)) },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                onClick = { showMenu = false; onPlay() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_add_to_queue)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.addToQueue(track)
                                }
                            )
                            if (isInPlaylist || onRemoveFromPlaylist != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_remove_from_playlist)) },
                                    leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        onRemoveFromPlaylist?.invoke()
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_add_to_playlist)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        showAddToPlaylistDialog = true
                                    }
                                )
                            }
                            if (isFavoritesTab || onRemoveFromFavorites != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_remove_from_favorites)) },
                                    leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        if (onRemoveFromFavorites != null) {
                                            onRemoveFromFavorites()
                                        } else {
                                            val dto = viewModel.localTrackToDto(track)
                                            viewModel.toggleFavorite(dto)
                                        }
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_add_to_favorites)) },
                                    leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        val dto = viewModel.localTrackToDto(track)
                                        viewModel.toggleFavorite(dto)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_share)) },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showMenu = false
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

    if (showAddToPlaylistDialog) {
        val dto = viewModel.localTrackToDto(track)
        AddToPlaylistDialog(
            playlists = dbPlaylists,
            onSelectPlaylist = { playlist ->
                viewModel.addTrackToPlaylist(playlist.id, dto)
            },
            onCreateNewPlaylist = { name ->
                viewModel.createPlaylist(name)
            },
            onDismiss = { showAddToPlaylistDialog = false }
        )
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

    var showEditDialog by remember { mutableStateOf(false) }
    if (showEditDialog) {
        var isSavingEdit by remember { mutableStateOf(false) }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { if (!isSavingEdit) showEditDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.nkapps.nkmuzik.ui.common.EditTrackScreen(
                track = track,
                isSaving = isSavingEdit,
                onConfirm = { newTitle, newArtist, newAlbum, newArtworkUri, newLyrics ->
                    isSavingEdit = true
                    viewModel.updateLocalTrackMetaData(
                        track = track,
                        newTitle = newTitle,
                        newArtist = newArtist,
                        newAlbum = newAlbum,
                        newArtworkUri = newArtworkUri,
                        newLyrics = newLyrics
                    ) {
                        isSavingEdit = false
                        showEditDialog = false
                    }
                },
                onDismiss = { showEditDialog = false }
            )
        }
    }


}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackGridCard(
    track: OnlineSearchResultDto,
    viewModel: SearchViewModel,
    onPlay: () -> Unit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onSelectToggle: () -> Unit = {},
    isInPlaylist: Boolean = false,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    isFavoritesTab: Boolean = false,
    onRemoveFromFavorites: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val dbPlaylists by viewModel.dbPlaylists.collectAsState()

    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        cardBg
    }

    val cardModifier = if (isSelectionMode) {
        Modifier
            .fillMaxWidth()
            .clickable { onSelectToggle() }
    } else {
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onSelectToggle
            )
    }

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                val artModel = ArtworkUtils.getArtworkModel(context, track.thumbnailUrl, track.title, track.videoId, track.durationString, track.uploaderName)
                if (artModel != null) {
                    AsyncImage(
                        model = artModel,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${track.uploaderName} • ${track.durationString}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!isSelectionMode) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = stringResource(R.string.more),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_play)) },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                onClick = { showMenu = false; onPlay() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_add_to_queue)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.addToQueue(track)
                                }
                            )
                            if (isInPlaylist || onRemoveFromPlaylist != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_remove_from_playlist)) },
                                    leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        onRemoveFromPlaylist?.invoke()
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_add_to_playlist)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        showAddToPlaylistDialog = true
                                    }
                                )
                            }
                            if (isFavoritesTab || onRemoveFromFavorites != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_remove_from_favorites)) },
                                    leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        if (onRemoveFromFavorites != null) {
                                            onRemoveFromFavorites()
                                        } else {
                                            viewModel.toggleFavorite(track)
                                        }
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_add_to_favorites)) },
                                    leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.toggleFavorite(track)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_share)) },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    ShareUtils.shareAudioFile(
                                        context = context,
                                        filePath = null,
                                        uriString = track.url,
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

    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            playlists = dbPlaylists,
            onSelectPlaylist = { playlist ->
                viewModel.addTrackToPlaylist(playlist.id, track)
            },
            onCreateNewPlaylist = { name ->
                viewModel.createPlaylist(name)
            },
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }

    if (showInfoDialog) {
        TrackInfoDialog(
            title = track.title,
            artist = track.uploaderName,
            duration = track.durationString,
            uriString = track.url,
            filePath = null,
            onDismiss = { showInfoDialog = false }
        )
    }


}
