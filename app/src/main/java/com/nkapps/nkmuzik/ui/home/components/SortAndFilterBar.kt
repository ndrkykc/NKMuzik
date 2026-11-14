package com.nkapps.nkmuzik.ui.home.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nkapps.nkmuzik.R

enum class TrackSortOption(@param:StringRes val labelRes: Int) {
    NAME(R.string.sort_by_name),
    DATE_ADDED(R.string.sort_by_date_added),
    PLAY_COUNT(R.string.sort_by_play_count)
}

enum class TrackFilterOption(@param:StringRes val labelRes: Int) {
    SONG(R.string.tab_songs),
    ARTIST(R.string.tab_artists),
    ALBUM(R.string.tab_albums),
    FOLDER(R.string.tab_folders)
}

@Composable
fun SortAndFilterBar(
    selectedSort: TrackSortOption,
    onSortSelected: (TrackSortOption) -> Unit,
    selectedFilter: TrackFilterOption,
    onFilterSelected: (TrackFilterOption) -> Unit,
    viewMode: Int = 0,
    onViewModeChanged: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            IconButton(onClick = { showSortMenu = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Sort,
                    contentDescription = stringResource(R.string.sort_title),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                TrackSortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(option.labelRes),
                                fontWeight = if (selectedSort == option) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedSort == option) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSortSelected(option)
                            showSortMenu = false
                        }
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = stringResource(R.string.filter_title),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    TrackFilterOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(option.labelRes),
                                    fontWeight = if (selectedFilter == option) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedFilter == option) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onFilterSelected(option)
                                showFilterMenu = false
                            }
                        )
                    }
                }
            }

            if (onViewModeChanged != null) {
                IconButton(onClick = {
                    val nextMode = if (viewMode == 0) 1 else 0
                    onViewModeChanged(nextMode)
                }) {
                    Icon(
                        imageVector = when (viewMode) {
                            0 -> Icons.Rounded.ViewList
                            else -> Icons.Rounded.GridView
                        },
                        contentDescription = when (viewMode) {
                            0 -> stringResource(R.string.list_view)
                            else -> stringResource(R.string.grid_view)
                        },
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
