package com.nkapps.nkmuzik.data.remote

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class RemoteConfigManager @Inject constructor(
    @param:ApplicationContext private val context: Context?
) {
    constructor() : this(null)

    companion object {
        private const val TAG = "RemoteConfig"
        const val KEY_ENABLE_ONLINE_FEATURES = "enable_online_features"
        const val KEY_ONLINE_TAB_LABEL = "online_tab_label"
        const val KEY_ONLINE_SEARCH_BASE_URL = "online_search_base_url"
        const val KEY_ONLINE_PROVIDER_TYPE = "online_provider_type"
        const val KEY_IS_ONLINE_SEARCH_SOURCE = "is_online_search_source"
        const val KEY_LYRICS_BASE_URL = "lyrics_base_url"

        const val DEFAULT_ENABLE_ONLINE_FEATURES = true
        const val DEFAULT_ONLINE_TAB_LABEL = "Keşfet"
        const val DEFAULT_ONLINE_SEARCH_BASE_URL = ""
        const val DEFAULT_ONLINE_PROVIDER_TYPE = "custom_api"
        const val DEFAULT_IS_ONLINE_SEARCH_SOURCE = true
        const val DEFAULT_LYRICS_BASE_URL = ""
    }

    private val _enableOnlineFeatures = MutableStateFlow(DEFAULT_ENABLE_ONLINE_FEATURES)
    open val enableOnlineFeatures: StateFlow<Boolean> = _enableOnlineFeatures.asStateFlow()

    private val _onlineTabLabel = MutableStateFlow(DEFAULT_ONLINE_TAB_LABEL)
    open val onlineTabLabel: StateFlow<String> = _onlineTabLabel.asStateFlow()

    private val _onlineSearchBaseUrl = MutableStateFlow(DEFAULT_ONLINE_SEARCH_BASE_URL)
    open val onlineSearchBaseUrl: StateFlow<String> = _onlineSearchBaseUrl.asStateFlow()

    private val _onlineProviderType = MutableStateFlow(DEFAULT_ONLINE_PROVIDER_TYPE)
    open val onlineProviderType: StateFlow<String> = _onlineProviderType.asStateFlow()

    private val _isOnlineSearchSource = MutableStateFlow(DEFAULT_IS_ONLINE_SEARCH_SOURCE)
    open val isOnlineSearchSource: StateFlow<Boolean> = _isOnlineSearchSource.asStateFlow()

    private val _lyricsBaseUrl = MutableStateFlow(DEFAULT_LYRICS_BASE_URL)
    open val lyricsBaseUrl: StateFlow<String> = _lyricsBaseUrl.asStateFlow()

    fun initRemoteConfig(ctx: Context? = context) {
        Log.d(TAG, "Local RemoteConfig initialized with default values.")
    }

    open fun fetchAndActivate(ctx: Context? = context) {
        Log.d(TAG, "Local RemoteConfig using default values.")
    }

    fun setEnableOnlineFeaturesForTest(enabled: Boolean) {
        _enableOnlineFeatures.value = enabled
    }

    private fun cleanBaseUrl(rawUrl: String): String {
        if (rawUrl.isBlank()) return ""
        return rawUrl.split(",")
            .map { it.trim().trimEnd('/') }
            .filter { it.isNotBlank() }
            .joinToString(",")
    }

    fun setOnlineSearchBaseUrlForTest(url: String) {
        _onlineSearchBaseUrl.value = cleanBaseUrl(url)
    }

    fun setIsOnlineSearchSourceForTest(isOnline: Boolean) {
        _isOnlineSearchSource.value = isOnline
    }

    fun setLyricsBaseUrlForTest(url: String) {
        _lyricsBaseUrl.value = cleanBaseUrl(url)
    }
}
