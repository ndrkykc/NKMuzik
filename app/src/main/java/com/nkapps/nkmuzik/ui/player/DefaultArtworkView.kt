package com.nkapps.nkmuzik.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Composable view that renders default album artwork when a track has no artwork or fails to load artwork.
 *
 * Respects configuration options defined in [DefaultArtworkConfig]:
 * - If [DefaultArtworkConfig.DEFAULT_DRAWABLE_RES_ID] is set, renders that drawable resource.
 * - Otherwise, renders a gradient background with [DefaultArtworkConfig.DEFAULT_ICON]
 *   and optional fallback title text.
 */
@Composable
fun DefaultArtworkView(
    modifier: Modifier = Modifier,
    title: String? = null
) {
    val customResId = DefaultArtworkConfig.DEFAULT_DRAWABLE_RES_ID
    if (customResId != null && customResId != 0) {
        Image(
            painter = painterResource(id = customResId),
            contentDescription = title ?: DefaultArtworkConfig.FALLBACK_TITLE,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        DefaultArtworkConfig.GRADIENT_START_COLOR,
                        DefaultArtworkConfig.GRADIENT_END_COLOR
                    )
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = DefaultArtworkConfig.DEFAULT_ICON,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = DefaultArtworkConfig.ICON_TINT
                )

                val displayTitle = if (!title.isNullOrBlank()) {
                    title
                } else {
                    DefaultArtworkConfig.FALLBACK_TITLE
                }

                if (displayTitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
