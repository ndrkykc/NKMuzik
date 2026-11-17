package com.nkapps.nkmuzik.ui.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

fun getRequiredPermissions(): Array<String> {
    val permissions = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    return permissions.toTypedArray()
}

fun hasAudioPermissionGranted(context: Context): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}

fun checkPermissionsGranted(context: Context): Boolean {
    val required = getRequiredPermissions()
    return required.all { permission ->
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun PermissionHandler(
    onPermissionsGranted: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(checkPermissionsGranted(context)) }
    var showDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        hasPermissions = allGranted
        if (allGranted) {
            showDialog = false
            onPermissionsGranted()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            kotlinx.coroutines.delay(2000) // 2 saniye gecikme
            showDialog = true
        } else {
            onPermissionsGranted()
        }
    }

    if (showDialog && !hasPermissions) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "İzin İsteği",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "NK Müzik uygulamasının sorunsuz çalışabilmesi için aşağıdaki izinlere ihtiyacı vardır:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Medya / Depolama: Cihazınızdaki ses dosyalarını taramak ve kütüphanede göstermek için.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bildirimler: Arka planda müzik çalarken kilit ekranı ve bildirim kontrollerini sunmak için.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        permissionLauncher.launch(getRequiredPermissions())
                    }
                ) {
                    Text("İzin Ver")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { 
                        showDialog = false
                        onPermissionsGranted() // Uyarıyı kapatınca uygulamanın açılmasına izin ver
                    }
                ) {
                    Text("Daha Sonra")
                }
            }
        )
    }

    if (!showDialog) {
        content()
    }
}