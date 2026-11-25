package com.nkapps.nkmuzik.ui.player

import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import com.nkapps.nkmuzik.player.CastManager
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.nkapps.nkmuzik.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.nkapps.nkmuzik.data.model.LocalTrack
import com.nkapps.nkmuzik.data.model.LyricsData
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.player.SleepTimerMode
import com.nkapps.nkmuzik.ui.theme.MysticGoldMat
import com.nkapps.nkmuzik.ui.common.DeleteTrackConfirmationDialog
import com.nkapps.nkmuzik.ui.common.TrackInfoDialog
import com.nkapps.nkmuzik.ui.equalizer.EqualizerContent
import com.nkapps.nkmuzik.ui.library.AddToPlaylistDialog
import com.nkapps.nkmuzik.ui.search.SearchViewModel
import com.nkapps.nkmuzik.ui.theme.DeepNightBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nkapps.nkmuzik.utils.ArtworkUtils
import com.nkapps.nkmuzik.utils.ShareUtils
import java.util.Locale
import kotlin.math.roundToInt

private enum class SeekSide { LEFT, RIGHT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingBottomSheet(
    track: OnlineSearchResultDto,
    isPlaying: Boolean,
    isFavorite: Boolean,
    viewModel: SearchViewModel,
    onDismiss: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var trackToAddToPlaylist by remember { mutableStateOf<OnlineSearchResultDto?>(null) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQueueBottomSheet by remember { mutableStateOf(false) }

    var seekSide by remember { mutableStateOf<SeekSide?>(null) }
    var seekSeconds by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapSide by remember { mutableStateOf<SeekSide?>(null) }
    var activeArrowStep by remember { mutableIntStateOf(-1) }
    var sweepTrigger by remember { mutableIntStateOf(0) }

    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()

    LaunchedEffect(sweepTrigger) {
        if (sweepTrigger > 0) {
            activeArrowStep = 0
            kotlinx.coroutines.delay(200)
            activeArrowStep = 1
            kotlinx.coroutines.delay(200)
            activeArrowStep = 2
            kotlinx.coroutines.delay(220)
            activeArrowStep = -1
        }
    }

    LaunchedEffect(seekSeconds) {
        if (seekSeconds > 0) {
            kotlinx.coroutines.delay(800)
            seekSide = null
            seekSeconds = 0
            lastTapSide = null
            activeArrowStep = -1
        }
    }
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()

    val currentMode = when {
        isShuffleEnabled -> "Shuffle"
        repeatMode == Player.REPEAT_MODE_ALL -> "RepeatAll"
        repeatMode == Player.REPEAT_MODE_ONE -> "RepeatOne"
        else -> "Sequential"
    }
    val dbPlaylists by viewModel.dbPlaylists.collectAsState()
    val localTracks by viewModel.localTracks.collectAsState()
    val enableOnlineFeatures by viewModel.enableOnlineFeatures.collectAsState()
    val lyricsData by viewModel.lyricsData.collectAsState()
    val isFetchingLyrics by viewModel.isFetchingLyrics.collectAsState()

    val queueItems by viewModel.queueItems.collectAsState()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsState()

    val isDownloaded = remember(localTracks, track) {
        localTracks.any { (it.videoId != null && it.videoId == track.videoId) || it.id.toString() == track.videoId || it.uri == track.url } ||
                track.url.startsWith("content:") ||
                track.url.startsWith("file:") ||
                track.url.startsWith("/storage")
    }
    val isUndownloadedOnlineTrack = !isDownloaded && track.videoId.isNotBlank()

    val sleepTimerFormatted by viewModel.sleepTimerFormatted.collectAsState()
    val sleepTimerMode by viewModel.sleepTimerMode.collectAsState()
    val fxState by viewModel.audioFxState.collectAsState()

    val context = LocalContext.current

    val sliderPosition = if (duration > 0) currentPosition.toFloat() else 0f
    val sliderMax = if (duration > 0) duration.toFloat() else 100f

    val yellowColor = Color(0xFFFFB300)

    val isCoverDynamicColorEnabled by viewModel.isCoverDynamicColorEnabled.collectAsState()
    val isMaterialYouEnabled by viewModel.dynamicColor.collectAsState()

    var artworkPaletteColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(track, isCoverDynamicColorEnabled) {
        if (isCoverDynamicColorEnabled) {
            val rawModel = ArtworkUtils.getRawArtworkModel(
                context,
                track.thumbnailUrl,
                track.title,
                track.videoId,
                track.durationString,
                track.uploaderName
            )
            if (rawModel != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(rawModel)
                            .allowHardware(false)
                            .build()
                        val result = context.imageLoader.execute(request)
                        if (result is SuccessResult) {
                            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                            if (bitmap != null) {
                                val palette = Palette.from(bitmap).generate()
                                val swatch = palette.darkVibrantSwatch ?: palette.dominantSwatch
                                val intColor = swatch?.rgb ?: palette.getDarkVibrantColor(palette.getDominantColor(0))
                                if (intColor != 0) {
                                    artworkPaletteColor = Color(intColor)
                                } else {
                                    artworkPaletteColor = null
                                }
                            } else {
                                artworkPaletteColor = null
                            }
                        } else {
                            artworkPaletteColor = null
                        }
                    } catch (_: Exception) {
                        artworkPaletteColor = null
                    }
                }
            } else {
                artworkPaletteColor = null
            }
        } else {
            artworkPaletteColor = null
        }
    }

    val useCoverColor = isCoverDynamicColorEnabled
    val useMaterialYou = isMaterialYouEnabled
    val defaultBg = MaterialTheme.colorScheme.background
    val isDarkTheme = defaultBg == DeepNightBlue

    val backgroundColor = when {
        useCoverColor && artworkPaletteColor != null -> artworkPaletteColor!!
        useMaterialYou -> MaterialTheme.colorScheme.primaryContainer
        else -> defaultBg
    }

    val animatedBgColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "PlayerBackgroundColorAnimation"
    )

    val playerBackgroundModifier = Modifier.background(
        Brush.verticalGradient(listOf(animatedBgColor, defaultBg))
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .then(playerBackgroundModifier),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Top Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.close),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (sleepTimerMode !is SleepTimerMode.Off) {
                    AssistChip(
                        onClick = { showSleepTimerDialog = true },
                        label = { Text(sleepTimerFormatted ?: stringResource(R.string.sleep_timer), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface) },
                        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val context = LocalContext.current

                    // Sleep Timer Quick Access Button
                    IconButton(onClick = { showSleepTimerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = stringResource(R.string.sleep_timer),
                            modifier = Modifier.size(28.dp),
                            tint = if (sleepTimerMode !is SleepTimerMode.Off) yellowColor else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = {
                        CastManager.showCastDialog(context)
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Cast,
                            contentDescription = "Ekran Yansıtma",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Lyrics Toggle Button in Header
                    IconButton(onClick = {
                        showLyrics = !showLyrics
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = stringResource(R.string.lyrics),
                            modifier = Modifier.size(28.dp),
                            tint = if (showLyrics) yellowColor else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more),
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            if (isUndownloadedOnlineTrack || track.url.startsWith("http")) {
                                if (enableOnlineFeatures) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_download)) },
                                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                        onClick = {
                                            showOptionsMenu = false
                                            viewModel.onDownloadClicked(track)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_add_to_queue)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        viewModel.addToQueue(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.equalizer_and_sound_effects)) },
                                    leadingIcon = { Icon(Icons.Default.Equalizer, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showEqualizerDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_add_to_playlist)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        trackToAddToPlaylist = track
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isFavorite) stringResource(R.string.action_remove_from_favorites) else stringResource(R.string.action_add_to_favorites)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = null,
                                            tint = if (isFavorite) yellowColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        onToggleFavorite()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sleep_timer)) },
                                    leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showSleepTimerDialog = true
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_add_to_queue)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        viewModel.addToQueue(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.equalizer_and_sound_effects)) },
                                    leadingIcon = { Icon(Icons.Default.Equalizer, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showEqualizerDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_add_to_playlist)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        trackToAddToPlaylist = track
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sleep_timer)) },
                                    leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showSleepTimerDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.share)) },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        ShareUtils.shareAudioFile(
                                            context = context,
                                            filePath = null,
                                            uriString = track.url,
                                            title = track.title
                                        )
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_edit)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showEditDialog = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete)) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 1. Media Artwork (Full-Width HorizontalPager for Album Covers or Synchronized Lyrics)
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = showLyrics,
                    animationSpec = tween(durationMillis = 300),
                    label = "ArtworkLyricsCrossfade",
                    modifier = Modifier.fillMaxWidth()
                ) { lyricsActive ->
                    if (lyricsActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .sizeIn(maxWidth = 360.dp, maxHeight = 360.dp)
                                    .clip(MaterialTheme.shapes.large)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                InlineLyricsView(
                                    lyricsData = lyricsData,
                                    isFetchingLyrics = isFetchingLyrics,
                                    currentPosition = currentPosition,
                                    onSeekTo = { pos -> viewModel.seekTo(pos) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        val pageCount = if (queueItems.isNotEmpty()) queueItems.size else 1
                        val safeQueueIndex = currentQueueIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                        val pagerState = rememberPagerState(
                            initialPage = safeQueueIndex,
                            pageCount = { pageCount }
                        )

                        LaunchedEffect(safeQueueIndex) {
                            if (pagerState.currentPage != safeQueueIndex && safeQueueIndex in 0 until pageCount) {
                                pagerState.animateScrollToPage(safeQueueIndex)
                            }
                        }

                        LaunchedEffect(pagerState.isScrollInProgress) {
                            if (!pagerState.isScrollInProgress) {
                                val targetPage = pagerState.currentPage
                                if (targetPage != safeQueueIndex && targetPage in queueItems.indices) {
                                    viewModel.skipToQueueItem(targetPage)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 28.dp),
                                pageSpacing = 56.dp
                            ) { page ->
                                val pageTrack = if (queueItems.isNotEmpty() && page in queueItems.indices) {
                                    queueItems[page]
                                } else {
                                    track
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .sizeIn(maxWidth = 360.dp, maxHeight = 360.dp)
                                        .clip(MaterialTheme.shapes.large)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .pointerInput(pageTrack.videoId) {
                                            detectTapGestures(
                                                onTap = { offset ->
                                                    val now = System.currentTimeMillis()
                                                    val isRight = offset.x > size.width / 2
                                                    val clickedSide = if (isRight) SeekSide.RIGHT else SeekSide.LEFT

                                                    if (now - lastTapTime < 450 && (lastTapSide == clickedSide || lastTapSide == null)) {
                                                        if (seekSeconds == 0) {
                                                            seekSeconds = 10
                                                        } else {
                                                            seekSeconds += 10
                                                        }
                                                        seekSide = clickedSide
                                                        lastTapSide = clickedSide
                                                        sweepTrigger++

                                                        val deltaMs = if (clickedSide == SeekSide.RIGHT) 10_000L else -10_000L
                                                        val newPos = (currentPosition + deltaMs).coerceIn(0L, duration)
                                                        viewModel.seekTo(newPos)
                                                    } else {
                                                        lastTapSide = clickedSide
                                                    }
                                                    lastTapTime = now
                                                }
                                            )
                                        }
                                ) {
                                    val artworkModel = ArtworkUtils.getArtworkModel(
                                        context,
                                        pageTrack.thumbnailUrl,
                                        pageTrack.title,
                                        pageTrack.videoId,
                                        pageTrack.durationString,
                                        pageTrack.uploaderName
                                    )
                                    SubcomposeAsyncImage(
                                        model = artworkModel,
                                        contentDescription = pageTrack.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        error = {
                                            DefaultArtworkView(
                                                modifier = Modifier.fillMaxSize(),
                                                title = pageTrack.title,
                                                trackId = pageTrack.videoId
                                            )
                                        }
                                    )

                                    if (seekSide != null && pageTrack.videoId == track.videoId) {
                                        val isRight = (seekSide == SeekSide.RIGHT)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(0.5f)
                                                .align(if (isRight) Alignment.CenterEnd else Alignment.CenterStart)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        if (isRight) listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                                        else listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                                                    )
                                                ),
                                            contentAlignment = if (isRight) Alignment.CenterEnd else Alignment.CenterStart
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(
                                                    start = if (isRight) 0.dp else 20.dp,
                                                    end = if (isRight) 20.dp else 0.dp
                                                )
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (isRight) {
                                                        val a0 = if (activeArrowStep == 0) 1f else 0f
                                                        val a1 = if (activeArrowStep == 1) 1f else 0f
                                                        val a2 = if (activeArrowStep == 2) 1f else 0f
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = a0), modifier = Modifier.size(22.dp))
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = a1), modifier = Modifier.size(22.dp))
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = a2), modifier = Modifier.size(22.dp))
                                                    } else {
                                                        val a2 = if (activeArrowStep == 0) 1f else 0f
                                                        val a1 = if (activeArrowStep == 1) 1f else 0f
                                                        val a0 = if (activeArrowStep == 2) 1f else 0f
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = a2), modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = 180f })
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = a1), modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = 180f })
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = a0), modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = 180f })
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = "$seekSeconds saniye",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    ),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    if (pageTrack.videoId == track.videoId) {
                                        val coverFavoriteTint = if (isDarkTheme) yellowColor else Color.White
                                        IconButton(
                                            onClick = onToggleFavorite,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                contentDescription = stringResource(R.string.section_favorites),
                                                modifier = Modifier.size(32.dp),
                                                tint = coverFavoriteTint
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Track Title & Artist Name
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.uploaderName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Playback Seek Bar (Slider with current position / total duration labels)
            Column(modifier = Modifier.fillMaxWidth()) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val matteGoldBrush = Brush.horizontalGradient(listOf(primaryColor, Color(0xFFD4AF37), Color(0xFFC5A059)))
                val inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)

                Slider(
                    value = sliderPosition,
                    onValueChange = { newValue ->
                        viewModel.seekTo(newValue.toLong())
                    },
                    valueRange = 0f..sliderMax,
                    colors = SliderDefaults.colors(
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = inactiveTrackColor,
                        thumbColor = primaryColor
                    ),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(primaryColor, CircleShape)
                        )
                    },
                    track = { sliderState ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(inactiveTrackColor),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(sliderState.coercedValueAsFraction)
                                    .fillMaxHeight()
                                    .background(matteGoldBrush)
                            )
                        }
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMs(currentPosition),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatMs(duration),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Playback Controls (Queue, Previous, Play/Pause, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Queue Button (replaces far-left shuffle button)
                IconButton(onClick = { showQueueBottomSheet = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = stringResource(R.string.up_next),
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Skip Previous
                IconButton(onClick = { viewModel.skipToPrevious() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = stringResource(R.string.previous_track),
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                val playPauseBg = if (isDarkTheme) {
                    Color(0xFF22334C)
                } else {
                    Color(0xFFE0E0E0)
                }
                val playPauseIconTint = if (isDarkTheme) {
                    Color.White
                } else {
                    Color.Black
                }

                // Play / Pause Toggle
                FilledIconButton(
                    onClick = onPlayPauseToggle,
                    modifier = Modifier.size(62.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = playPauseBg,
                        contentColor = playPauseIconTint
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.action_play),
                        modifier = Modifier.size(36.dp),
                        tint = playPauseIconTint
                    )
                }

                // Skip Next
                IconButton(onClick = { viewModel.skipToNext() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = stringResource(R.string.next_track),
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Playback Mode Cycle Button
                IconButton(onClick = { viewModel.cyclePlaybackMode() }) {
                    val modeIcon = when (currentMode) {
                        "RepeatAll" -> Icons.Rounded.Repeat
                        "RepeatOne" -> Icons.Rounded.RepeatOne
                        "Shuffle" -> Icons.Rounded.Shuffle
                        else -> Icons.Rounded.Repeat
                    }
                    val modeTint = if (currentMode == "Sequential") {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        yellowColor
                    }

                    Icon(
                        imageVector = modeIcon,
                        contentDescription = stringResource(R.string.play_mode),
                        modifier = Modifier.size(28.dp),
                        tint = modeTint
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Queue Bottom Sheet
    if (showQueueBottomSheet) {
        QueueBottomSheet(
            viewModel = viewModel,
            onDismiss = { showQueueBottomSheet = false }
        )
    }

    // Add To Playlist Dialog
    if (showAddToPlaylistDialog || trackToAddToPlaylist != null) {
        val targetTrack = trackToAddToPlaylist ?: track
        AddToPlaylistDialog(
            playlists = dbPlaylists,
            onSelectPlaylist = { playlist ->
                viewModel.addTrackToPlaylist(playlist.id, targetTrack)
                trackToAddToPlaylist = null
                showAddToPlaylistDialog = false
            },
            onCreateNewPlaylist = { name ->
                viewModel.createPlaylist(name)
            },
            onDismiss = {
                trackToAddToPlaylist = null
                showAddToPlaylistDialog = false
            }
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text(stringResource(R.string.sleep_timer)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val timerPresets = listOf(15, 30, 45, 60)
                    timerPresets.forEach { mins ->
                        TextButton(
                            onClick = {
                                viewModel.startSleepTimer(mins)
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.stop_after_minutes, mins), style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    TextButton(
                        onClick = {
                            viewModel.setSleepTimerEndOfTrack()
                            showSleepTimerDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.stop_at_end_of_song), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    }

                    if (sleepTimerMode !is SleepTimerMode.Off) {
                        HorizontalDivider()
                        TextButton(
                            onClick = {
                                viewModel.cancelSleepTimer()
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.cancel_timer), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    // Equalizer Dialog
    if (showEqualizerDialog) {
        AlertDialog(
            onDismissRequest = { showEqualizerDialog = false },
            title = { Text(stringResource(R.string.equalizer_and_sound_effects)) },
            text = {
                Box(modifier = Modifier.heightIn(max = 450.dp)) {
                    EqualizerContent(
                        fxState = fxState,
                        onToggleEqualizer = { viewModel.setEqualizerEnabled(it) },
                        onSelectPreset = { viewModel.selectEqualizerPreset(it) },
                        onBandLevelChange = { bandIndex, levelMilliBel ->
                            viewModel.setEqualizerBandLevel(bandIndex, levelMilliBel)
                        },
                        onToggleBassBoost = { viewModel.setBassBoostEnabled(it) },
                        onBassBoostStrengthChange = { viewModel.setBassBoostStrength(it) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showEqualizerDialog = false }) { Text(stringResource(R.string.ok)) }
            }
        )
    }

    // Track Info Dialog
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

    // Edit Track Dialog
    if (showEditDialog) {
        var isSavingEdit by remember { mutableStateOf(false) }
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

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        DeleteTrackConfirmationDialog(
            trackTitle = track.title,
            onConfirm = {
                viewModel.deleteTrack(track) {
                    onDismiss()
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QueueBottomSheet(
    viewModel: SearchViewModel,
    onDismiss: () -> Unit,
    onAddToPlaylist: ((OnlineSearchResultDto) -> Unit)? = null
) {
    val queueItems by viewModel.queueItems.collectAsState()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()


    val currentMode = when {
        isShuffleEnabled -> "Shuffle"
        repeatMode == Player.REPEAT_MODE_ALL -> "RepeatAll"
        repeatMode == Player.REPEAT_MODE_ONE -> "RepeatOne"
        else -> "Sequential"
    }

    val isolateSheetDragNestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return available
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return available
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(isolateSheetDragNestedScroll)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.up_next),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Playback Mode Cycle Button
                IconButton(
                    onClick = {
                        viewModel.cyclePlaybackMode()
                    }
                ) {
                    val modeIcon = when (currentMode) {
                        "RepeatAll" -> Icons.Rounded.Repeat
                        "RepeatOne" -> Icons.Rounded.RepeatOne
                        "Shuffle" -> Icons.Rounded.Shuffle
                        else -> Icons.Rounded.Repeat
                    }
                    val modeTint = if (currentMode == "Sequential") {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                        Color(0xFFFFB300)
                    }

                    Icon(
                        imageVector = modeIcon,
                        contentDescription = stringResource(R.string.play_mode),
                        tint = modeTint,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            HorizontalDivider()

            if (queueItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.empty_queue),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val listState = rememberLazyListState()
                val density = LocalDensity.current
                val itemHeightPx = with(density) { 68.dp.toPx() }
                var measuredItemHeightPx by remember { mutableFloatStateOf(itemHeightPx) }

                var draggedItemKey by remember { mutableStateOf<String?>(null) }
                var draggedStartIndex by remember { mutableStateOf<Int?>(null) }
                var dragOffsetY by remember { mutableFloatStateOf(0f) }

                var localQueueItems by remember { mutableStateOf(queueItems) }
                
                LaunchedEffect(queueItems) {
                    if (draggedItemKey == null) {
                        localQueueItems = queueItems
                    }
                }

                val stableKeyMap = remember { java.util.IdentityHashMap<OnlineSearchResultDto, String>() }
                val stableKeys = remember(localQueueItems) {
                    localQueueItems.map { item ->
                        var key = stableKeyMap[item]
                        if (key == null) {
                            key = java.util.UUID.randomUUID().toString()
                            stableKeyMap[item] = key
                        }
                        key
                    }
                }
                
                val playingTrack = queueItems.getOrNull(currentQueueIndex)

                val currentTargetIndex = remember(draggedStartIndex, dragOffsetY, measuredItemHeightPx, localQueueItems.size) {
                    val startIdx = draggedStartIndex ?: return@remember null
                    val effectiveHeight = if (measuredItemHeightPx > 0f) measuredItemHeightPx else itemHeightPx
                    val steps = (dragOffsetY / effectiveHeight).roundToInt()
                    (startIdx + steps).coerceIn(0, localQueueItems.size - 1)
                }

                LaunchedEffect(currentTargetIndex, dragOffsetY) {
                    val currentIdx = currentTargetIndex ?: return@LaunchedEffect
                    val layoutInfo = listState.layoutInfo
                    val visibleItems = layoutInfo.visibleItemsInfo
                    if (visibleItems.isEmpty()) return@LaunchedEffect

                    val firstVisible = visibleItems.first().index
                    val lastVisible = visibleItems.last().index

                    if (currentIdx >= lastVisible - 1 && currentIdx < localQueueItems.size - 1) {
                        listState.scrollBy(itemHeightPx)
                    } else if (currentIdx <= firstVisible + 1 && currentIdx > 0) {
                        listState.scrollBy(-itemHeightPx)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        items = localQueueItems,
                        key = { index, _ -> stableKeys.getOrElse(index) { index } }
                    ) { index, item ->
                        val itemKey = stableKeys.getOrElse(index) { "$index" }
                        val isPlayingItem = (item === playingTrack || item == playingTrack)
                        var showTrackMenu by remember { mutableStateOf(false) }
                        val currentItemIndex by rememberUpdatedState(index)
                        val isBeingDragged = (draggedItemKey == itemKey)

                        val animatedOffset = if (isBeingDragged) dragOffsetY else 0f

                        val cardBg = MaterialTheme.colorScheme.surfaceContainer
                        val sheenBrush = if (isPlayingItem) {
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

                        val containerColor = if (isBeingDragged) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        } else if (isPlayingItem) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            cardBg
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { if (it.height > 0) measuredItemHeightPx = it.height.toFloat() }
                                .zIndex(if (isBeingDragged) 2f else 0f)
                                .graphicsLayer {
                                    translationY = animatedOffset
                                }
                                .clip(MaterialTheme.shapes.medium)
                                .pointerInput(itemKey) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedItemKey = itemKey
                                            draggedStartIndex = localQueueItems.indexOfFirst { stableKeys[localQueueItems.indexOf(it)] == itemKey }
                                            dragOffsetY = 0f
                                        },
                                        onDragEnd = {
                                            val finalIdx = draggedStartIndex
                                            val originalIdx = queueItems.indexOfFirst { stableKeys[queueItems.indexOf(it)] == itemKey }
                                            
                                            if (originalIdx != -1 && finalIdx != null && originalIdx != finalIdx) {
                                                viewModel.reorderQueue(originalIdx, finalIdx)
                                            }
                                            
                                            draggedItemKey = null
                                            draggedStartIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            localQueueItems = queueItems
                                            draggedItemKey = null
                                            draggedStartIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            
                                            val currentIdx = draggedStartIndex ?: return@detectDragGesturesAfterLongPress
                                            val effectiveHeight = if (measuredItemHeightPx > 0f) measuredItemHeightPx else itemHeightPx
                                            
                                            if (dragOffsetY > effectiveHeight * 0.5f && currentIdx < localQueueItems.size - 1) {
                                                val list = localQueueItems.toMutableList()
                                                val movedItem = list.removeAt(currentIdx)
                                                list.add(currentIdx + 1, movedItem)
                                                localQueueItems = list
                                                
                                                draggedStartIndex = currentIdx + 1
                                                dragOffsetY -= effectiveHeight
                                            } else if (dragOffsetY < -effectiveHeight * 0.5f && currentIdx > 0) {
                                                val list = localQueueItems.toMutableList()
                                                val movedItem = list.removeAt(currentIdx)
                                                list.add(currentIdx - 1, movedItem)
                                                localQueueItems = list
                                                
                                                draggedStartIndex = currentIdx - 1
                                                dragOffsetY += effectiveHeight
                                            }
                                        }
                                    )
                                }
                                .clickable {
                                    if (draggedItemKey == null) {
                                        viewModel.skipToQueueItem(index)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                                    // Artwork / Playing indicator
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .aspectRatio(1f)
                                            .clip(MaterialTheme.shapes.medium)
                                    ) {
                                        val artworkModel = ArtworkUtils.getArtworkModel(
                                            LocalContext.current,
                                            item.thumbnailUrl,
                                            item.title,
                                            item.videoId
                                        )
                                        if (artworkModel != null) {
                                            SubcomposeAsyncImage(
                                                model = artworkModel,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                error = {
                                                    DefaultArtworkView(
                                                        modifier = Modifier.fillMaxSize(),
                                                        title = item.title
                                                    )
                                                }
                                            )
                                        } else {
                                            DefaultArtworkView(
                                                modifier = Modifier.fillMaxSize(),
                                                title = item.title
                                            )
                                        }

                                        if (isPlayingItem) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.4f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFB300),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Track Title & Artist
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                            fontWeight = FontWeight.Medium,
                                            color = if (isPlayingItem) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (item.durationString.isNotBlank()) "${item.uploaderName} • ${item.durationString}" else item.uploaderName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                            color = Color(0xFFB0B0B0),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Reorder Drag Handle Icon
                                    Icon(
                                        imageVector = Icons.Rounded.Reorder,
                                        contentDescription = "Sırala",
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(22.dp)
                                    )

                                    // Overflow 3-dots menu
                                    Box {
                                        IconButton(onClick = { showTrackMenu = true }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = stringResource(R.string.options),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showTrackMenu,
                                            onDismissRequest = { showTrackMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.action_remove_from_queue)) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                },
                                                onClick = {
                                                    showTrackMenu = false
                                                    viewModel.removeFromQueue(index)
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
        }
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", mins, secs)
}
