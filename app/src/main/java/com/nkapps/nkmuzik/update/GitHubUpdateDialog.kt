package com.nkapps.nkmuzik.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GitHubUpdateDialog(
    updateState: UpdateState,
    onUpdateClick: (GitHubRelease) -> Unit,
    onInstallClick: (java.io.File) -> Unit,
    onDismiss: () -> Unit
) {
    when (updateState) {
        is UpdateState.UpdateAvailable -> {
            val release = updateState.release
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        text = "🚀 Yeni Güncelleme Mevcut! (${release.tagName})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = release.title.ifBlank { "NK Müzik Yeni Sürüm" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = release.releaseNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { onUpdateClick(release) }) {
                        Text("Şimdi Güncelle")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Daha Sonra")
                    }
                }
            )
        }

        is UpdateState.Downloading -> {
            val progress = updateState.progress
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Güncelleme İndiriliyor...") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "%$progress indirildi",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {}
            )
        }

        is UpdateState.ReadyToInstall -> {
            val apkFile = updateState.apkFile
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("İndirme Tamamlandı!") },
                text = { Text("Yeni sürüm kurulmaya hazır. Yükleme işlemini başlatmak için tıklayın.") },
                confirmButton = {
                    Button(onClick = { onInstallClick(apkFile) }) {
                        Text("Yüklemeyi Başlat")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Kapat")
                    }
                }
            )
        }

        else -> {}
    }
}
