package com.nkapps.nkmuzik.ui.player

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * DefaultArtworkConfig provides global configuration settings for the default album artwork.
 * When a music track has no artwork URI or fails to load an image, [DefaultArtworkView]
 * uses these parameters to render the fallback artwork.
 *
 * ======================================================================================
 * HOW TO CUSTOMIZE OR REPLACE THE DEFAULT ARTWORK IMAGE IN THE FUTURE:
 * ======================================================================================
 * 1. OPTION A: Use a custom drawable image asset (e.g., png, jpg, or vector drawable):
 *    - Place your drawable image resource into `res/drawable/` (e.g., `default_album_cover.png`).
 *    - Set [DEFAULT_DRAWABLE_RES_ID] below to `R.drawable.default_album_cover`.
 *    - When [DEFAULT_DRAWABLE_RES_ID] is non-null and not 0, [DefaultArtworkView] will
 *      automatically load and render that drawable instead of the generated gradient view.
 *
 * 2. OPTION B: Customize the generated gradient artwork:
 *    - Change [GRADIENT_START_COLOR] and [GRADIENT_END_COLOR] to match your app theme.
 *    - Change [DEFAULT_ICON] to any Material [ImageVector] (e.g., `Icons.Default.LibraryMusic`).
 *    - Change [ICON_TINT] to customize the icon color.
 *    - Change or clear [FALLBACK_TITLE] (e.g., "NK Müzik") to display text under the icon.
 * ======================================================================================
 */
object DefaultArtworkConfig {
    /**
     * Primary default artwork resource ID.
     */
    @DrawableRes
    val DEFAULT_DRAWABLE_RES_ID: Int = com.nkapps.nkmuzik.R.drawable.app_logo_splash

    /**
     * Pool of fallback drawable resource IDs for tracks without artwork.
     * Place your custom images into res/drawable/ (e.g. cover_1.png ... cover_6.png)
     * and add R.drawable.cover_1 ... R.drawable.cover_6 to this list!
     */
    @DrawableRes
    val DEFAULT_DRAWABLE_RES_IDS: List<Int> = listOf(
        com.nkapps.nkmuzik.R.drawable.kapak_1,
        com.nkapps.nkmuzik.R.drawable.kapak_2,
        com.nkapps.nkmuzik.R.drawable.kapak_3,
        com.nkapps.nkmuzik.R.drawable.kapak_4,
        com.nkapps.nkmuzik.R.drawable.kapak_5,
        com.nkapps.nkmuzik.R.drawable.kapak_6
    )

    /**
     * Returns a deterministic fallback artwork drawable ID based on track title/ID hash,
     * ensuring each song without artwork gets a consistent, distinct cover image from the pool!
     */
    @DrawableRes
    fun getFallbackDrawableRes(title: String? = null, trackId: String? = null): Int {
        val pool = DEFAULT_DRAWABLE_RES_IDS.filter { it != 0 }
        if (pool.isEmpty()) return DEFAULT_DRAWABLE_RES_ID
        val hash = (title ?: trackId ?: "").hashCode()
        val index = (hash and Int.MAX_VALUE) % pool.size
        return pool[index]
    }

    /**
     * Start color for procedural gradient background.
     */
    val GRADIENT_START_COLOR: Color = Color(0xFF3F51B5) // Indigo

    /**
     * End color for procedural gradient background.
     */
    val GRADIENT_END_COLOR: Color = Color(0xFF00BCD4) // Cyan

    /**
     * Icon vector displayed in center of procedural artwork.
     */
    val DEFAULT_ICON: ImageVector = Icons.Default.MusicNote

    /**
     * Tint color for the center icon in procedural artwork.
     */
    val ICON_TINT: Color = Color.White

    /**
     * Fallback title or app name displayed on the artwork.
     * Set to empty string if no text should be shown.
     */
    const val FALLBACK_TITLE: String = "NK Müzik"
}
