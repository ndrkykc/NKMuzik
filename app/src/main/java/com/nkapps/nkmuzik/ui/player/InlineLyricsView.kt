package com.nkapps.nkmuzik.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nkapps.nkmuzik.R
import com.nkapps.nkmuzik.data.model.LyricsData
import com.nkapps.nkmuzik.ui.theme.DeepNightBlue

@Composable
fun InlineLyricsView(
    lyricsData: LyricsData,
    isFetchingLyrics: Boolean,
    currentPosition: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
        shape = MaterialTheme.shapes.large
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isFetchingLyrics) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color(0xFFFFB300)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.loading_lyrics),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (lyricsData.lines.isNotEmpty()) {
                val activeLineIndex = remember(lyricsData.lines, currentPosition) {
                    var found = -1
                    for (i in lyricsData.lines.indices) {
                        if (lyricsData.lines[i].timestampMs <= currentPosition) {
                            found = i
                        } else {
                            break
                        }
                    }
                    found
                }

                val listState = rememberLazyListState()

                LaunchedEffect(activeLineIndex) {
                    if (activeLineIndex in lyricsData.lines.indices) {
                        listState.animateScrollToItem(index = maxOf(0, activeLineIndex - 3))
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(start = 8.dp, top = 16.dp, end = 8.dp, bottom = 120.dp)
                ) {
                    itemsIndexed(
                        items = lyricsData.lines,
                        key = { index, line -> "${line.timestampMs}_$index" }
                    ) { index, line ->
                        val isActive = index == activeLineIndex
                        val isDarkTheme = MaterialTheme.colorScheme.background == DeepNightBlue
                        val targetColor = if (isDarkTheme) {
                            if (isActive) Color(0xFFFFB300) else Color.White.copy(alpha = 0.50f)
                        } else {
                            if (isActive) Color.Black else Color(0xFF4B5563)
                        }
                        val animatedColor by animateColorAsState(
                            targetValue = targetColor,
                            animationSpec = tween(durationMillis = 700, easing = LinearOutSlowInEasing),
                            label = "LyricTextColor"
                        )
                        val animatedScale by animateFloatAsState(
                            targetValue = if (isActive) 1.15f else 1.00f,
                            animationSpec = tween(durationMillis = 700, easing = LinearOutSlowInEasing),
                            label = "LyricTextScale"
                        )
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = animatedColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                }
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onSeekTo(line.timestampMs) }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            } else if (lyricsData.plainText.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = lyricsData.plainText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.no_lyrics_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
