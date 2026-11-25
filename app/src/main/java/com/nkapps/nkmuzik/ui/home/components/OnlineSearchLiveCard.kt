package com.nkapps.nkmuzik.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import com.nkapps.nkmuzik.ui.theme.DeepNightBlue
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nkapps.nkmuzik.R
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.downloader.DownloadStatus
import com.nkapps.nkmuzik.ui.common.DeleteTrackConfirmationDialog
import com.nkapps.nkmuzik.ui.common.TrackInfoDialog
import com.nkapps.nkmuzik.ui.library.AddToPlaylistDialog
import com.nkapps.nkmuzik.ui.search.SearchViewModel
import com.nkapps.nkmuzik.ui.theme.MysticGoldMat
import com.nkapps.nkmuzik.utils.ArtworkUtils

@Composable
fun OnlineSearchLiveCard(
    track: OnlineSearchResultDto,
    downloadInfoStatus: DownloadStatus?,
    downloadProgress: Int,
    viewModel: SearchViewModel,
    enableDownloads: Boolean = true,
    onPlayStream: () -> Unit,
    onDownload: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showMenu by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val dbPlaylists by viewModel.dbPlaylists.collectAsState()
    val currentPlayingTrack by viewModel.currentPlayingTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isCurrentlyPlayingTrack = remember(currentPlayingTrack, track) {
        val curr = currentPlayingTrack
        curr != null && (
            (!curr.videoId.isNullOrBlank() && curr.videoId == track.videoId) ||
            (!curr.url.isNullOrBlank() && curr.url == track.url)
        )
    }

    val onDownloadClick: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDownload()
    }

    val onCardBodyClick: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onPlayStream()
    }

    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val isDarkTheme = MaterialTheme.colorScheme.background == DeepNightBlue
    
    val containerColor = if (isCurrentlyPlayingTrack) {
        if (isDarkTheme) MysticGoldMat.copy(alpha = 0.20f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    } else {
        cardBg
    }
    
    val sheenBrush = if (isCurrentlyPlayingTrack) {
        Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                if (isDarkTheme) MysticGoldMat.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardBodyClick),
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
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
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
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (enableDownloads) {
                        androidx.compose.animation.AnimatedContent(
                            targetState = downloadInfoStatus,
                            label = "downloadStatusAnim"
                        ) { status ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (status) {
                                    DownloadStatus.DOWNLOADING -> {
                                        Text(
                                            text = "%$downloadProgress",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MysticGoldMat
                                            ),
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        IconButton(
                                            onClick = { viewModel.cancelDownload(track.videoId) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "İptal",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    DownloadStatus.COMPLETED -> {
                                        IconButton(
                                            onClick = { },
                                            modifier = Modifier.size(36.dp),
                                            enabled = false
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "İndirildi",
                                                tint = Color(0xFF4CAF50)
                                            )
                                        }
                                    }
                                    else -> {
                                        IconButton(
                                            onClick = onDownloadClick,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Download,
                                                contentDescription = stringResource(R.string.action_download),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_add_to_queue)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.addToQueue(track)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_add_to_playlist)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showAddToPlaylistDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_add_to_favorites)) },
                                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleFavorite(track)
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
