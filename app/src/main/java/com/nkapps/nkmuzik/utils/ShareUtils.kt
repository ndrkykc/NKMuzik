package com.nkapps.nkmuzik.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.nkapps.nkmuzik.data.model.LocalTrack
import java.io.File

object ShareUtils {
    fun shareAudioFile(
        context: Context,
        filePath: String?,
        uriString: String?,
        title: String = ""
    ) {
        var shareUri: Uri? = null

        // 1. Try direct filePath
        if (!filePath.isNullOrBlank()) {
            val file = File(filePath)
            if (file.exists()) {
                try {
                    shareUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 2. Try uriString if content:// or file://
        if (shareUri == null && !uriString.isNullOrBlank()) {
            if (uriString.startsWith("content://")) {
                shareUri = Uri.parse(uriString)
            } else if (uriString.startsWith("file://")) {
                val path = Uri.parse(uriString).path
                if (!path.isNullOrBlank()) {
                    val file = File(path)
                    if (file.exists()) {
                        try {
                            shareUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        // 3. Try checking NK Müzik downloaded directory
        if (shareUri == null && title.isNotBlank()) {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val appDirs = listOfNotNull(File(musicDir, "NK Müzik"), File(musicDir, "NK Muzik"))
            for (appDir in appDirs) {
                if (appDir.exists()) {
                    val matchedFile = appDir.listFiles()?.firstOrNull { file ->
                        file.nameWithoutExtension.equals(title, ignoreCase = true)
                    }
                    if (matchedFile != null && matchedFile.exists()) {
                        try {
                            shareUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                matchedFile
                            )
                            break
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        if (shareUri != null) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val shareIntent = Intent.createChooser(sendIntent, "Müzik Paylaş")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } else {
            if (!uriString.isNullOrBlank() && (uriString.startsWith("http://") || uriString.startsWith("https://"))) {
                shareText(context, if (title.isNotBlank()) "$title: $uriString" else uriString, "Müzik Paylaş")
            } else {
                Toast.makeText(context, "Müzik dosyası bulunamadı.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareText(context: Context, text: String, chooserTitle: String = "Paylaş") {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val shareIntent = Intent.createChooser(sendIntent, chooserTitle)
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareMultipleAudioFiles(
        context: Context,
        tracks: List<LocalTrack>
    ) {
        if (tracks.isEmpty()) return
        if (tracks.size == 1) {
            val t = tracks.first()
            shareAudioFile(context, t.filePath, t.uri, t.title)
            return
        }

        val uriList = ArrayList<Uri>()
        val textList = mutableListOf<String>()

        for (track in tracks) {
            var shareUri: Uri? = null
            if (!track.filePath.isNullOrBlank()) {
                val file = File(track.filePath)
                if (file.exists()) {
                    try {
                        shareUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            if (shareUri == null && track.uri.isNotBlank()) {
                if (track.uri.startsWith("content://")) {
                    shareUri = Uri.parse(track.uri)
                } else if (track.uri.startsWith("file://")) {
                    val path = Uri.parse(track.uri).path
                    if (!path.isNullOrBlank()) {
                        val file = File(path)
                        if (file.exists()) {
                            try {
                                shareUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }

            if (shareUri != null) {
                uriList.add(shareUri)
            } else if (track.uri.startsWith("http://") || track.uri.startsWith("https://")) {
                textList.add("${track.title}: ${track.uri}")
            }
        }

        if (uriList.isNotEmpty()) {
            val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "audio/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val shareIntent = Intent.createChooser(sendIntent, "Müzikleri Paylaş")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } else if (textList.isNotEmpty()) {
            shareText(context, textList.joinToString("\n"), "Müzikleri Paylaş")
        } else {
            Toast.makeText(context, "Paylaşılacak dosya bulunamadı.", Toast.LENGTH_SHORT).show()
        }
    }
}
