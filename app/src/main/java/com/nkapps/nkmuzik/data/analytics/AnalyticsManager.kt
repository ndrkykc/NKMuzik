package com.nkapps.nkmuzik.data.analytics

import android.os.Bundle
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor() {
    companion object {
        private const val TAG = "AnalyticsManager"
    }

    private fun logEvent(eventName: String, params: Bundle? = null) {
        try {
            Log.d(TAG, "Logged event: $eventName with params: $params")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to log event $eventName: ${e.message}")
        }
    }

    fun logUsageMode(mode: String) {
        // mode: "local" | "online"
        val bundle = Bundle().apply {
            putString("mode", mode)
        }
        logEvent("usage_mode", bundle)
    }

    fun logTrackPlay(mediaType: String, durationSeconds: Long) {
        // mediaType: "local" | "online"
        val bundle = Bundle().apply {
            putString("media_type", mediaType)
            putLong("duration_seconds", durationSeconds)
        }
        logEvent("track_play", bundle)
    }

    fun logTrackDownload(title: String) {
        val bundle = Bundle().apply {
            putString("title", title)
        }
        logEvent("track_download", bundle)
    }

    fun logFeatureUsed(featureName: String) {
        // featureName: "equalizer" | "lyrics" | "sleep_timer" | "playlist"
        val bundle = Bundle().apply {
            putString("feature_name", featureName)
        }
        logEvent("feature_used", bundle)
    }

    fun logLanguageChanged(language: String) {
        // language: "tr" | "en"
        val bundle = Bundle().apply {
            putString("language", language)
        }
        logEvent("language_changed", bundle)
    }

    fun logFilterUsed(filterName: String) {
        // filterName: "songs" | "artists" | "albums" | "folders"
        val bundle = Bundle().apply {
            putString("filter_name", filterName)
        }
        logEvent("filter_used", bundle)
    }

    fun logSortUsed(sortName: String) {
        // sortName: "name" | "date" | "play_count"
        val bundle = Bundle().apply {
            putString("sort_name", sortName)
        }
        logEvent("sort_used", bundle)
    }

    fun logNonFatalException(throwable: Throwable, tag: String = TAG, message: String? = null) {
        Log.e(tag, message ?: throwable.message ?: "Non-fatal exception occurred", throwable)
    }
}
