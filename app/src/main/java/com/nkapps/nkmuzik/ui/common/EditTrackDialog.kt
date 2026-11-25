package com.nkapps.nkmuzik.ui.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nkapps.nkmuzik.R
import com.nkapps.nkmuzik.data.model.LocalTrack
import com.nkapps.nkmuzik.ui.theme.MysticGoldMat
import com.nkapps.nkmuzik.utils.ArtworkUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTrackScreen(
    track: LocalTrack,
    isSaving: Boolean,
    onConfirm: (
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newArtworkUri: Uri?,
        newLyrics: String?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isSaving,
            dismissOnClickOutside = false
        )
    ) {
        val context = LocalContext.current
        var title by remember { mutableStateOf(track.title) }
        var artist by remember { mutableStateOf(track.artist) }
        var album by remember { mutableStateOf(if (track.album == "Bilinmeyen Albüm" || track.album == "İndirilenler") "" else track.album) }
        var lyrics by remember { mutableStateOf("") }

        val initialArtwork = ArtworkUtils.parseArtworkUri(track.artworkUri)
        var selectedArtworkUri by remember { mutableStateOf<Uri?>(initialArtwork) }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                selectedArtworkUri = uri
            }
        }

        val lyricsPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val textContent = stream.bufferedReader().use { it.readText() }
                        if (textContent.isNotBlank()) {
                            lyrics = textContent
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.edit_track_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (!isSaving) onDismiss() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(24.dp),
                                color = MysticGoldMat,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    val finalArtworkUri = if (selectedArtworkUri != initialArtwork) selectedArtworkUri else null
                                    onConfirm(
                                        title, artist, album, finalArtworkUri,
                                        lyrics.ifBlank { null }
                                    )
                                },
                                enabled = title.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.edit_track_save),
                                    tint = MysticGoldMat,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.statusBarsPadding(),
                    windowInsets = WindowInsets(0.dp)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Cover Image Selector with clear visible border
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .border(
                            width = 2.dp,
                            color = if (selectedArtworkUri != null) MysticGoldMat.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.extraLarge
                        )
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(enabled = !isSaving) {
                            imagePickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedArtworkUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(selectedArtworkUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = stringResource(R.string.edit_track_change_cover),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.edit_track_change_cover),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.edit_track_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(stringResource(R.string.edit_track_artist)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text(stringResource(R.string.edit_track_album)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = lyrics,
                    onValueChange = { lyrics = it },
                    label = { Text(stringResource(R.string.edit_track_lyrics)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    enabled = !isSaving,
                    trailingIcon = {
                        IconButton(onClick = { lyricsPickerLauncher.launch("*/*") }) {
                            Icon(
                                imageVector = Icons.Rounded.UploadFile,
                                contentDescription = "Şarkı Sözü Dosyası Yükle (.lrc / .txt)",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
