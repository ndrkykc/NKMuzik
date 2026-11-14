package com.nkapps.nkmuzik.data.repository

import com.nkapps.nkmuzik.data.remote.RemoteConfigManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineSearchRepositoryTest {

    @Test
    fun remoteConfigManagerKeysAndDefaultsAreCorrectlyConfigured() {
        assertEquals("online_search_base_url", RemoteConfigManager.KEY_ONLINE_SEARCH_BASE_URL)
        assertEquals("online_provider_type", RemoteConfigManager.KEY_ONLINE_PROVIDER_TYPE)
        assertEquals("enable_online_features", RemoteConfigManager.KEY_ENABLE_ONLINE_FEATURES)
        assertEquals("is_online_search_source", RemoteConfigManager.KEY_IS_ONLINE_SEARCH_SOURCE)
        assertEquals("", RemoteConfigManager.DEFAULT_ONLINE_SEARCH_BASE_URL)
        assertEquals(true, RemoteConfigManager.DEFAULT_ENABLE_ONLINE_FEATURES)
        assertEquals("custom_api", RemoteConfigManager.DEFAULT_ONLINE_PROVIDER_TYPE)
        assertEquals(true, RemoteConfigManager.DEFAULT_IS_ONLINE_SEARCH_SOURCE)
    }

    @Test
    fun setOnlineSearchBaseUrlForTestTrimsWhitespaceAndTrailingSlashes() {
        val remoteConfigManager = RemoteConfigManager()
        remoteConfigManager.setOnlineSearchBaseUrlForTest("  https://api.example.com/  ")
        assertEquals("https://api.example.com", remoteConfigManager.onlineSearchBaseUrl.value)
    }

    @Test
    fun setIsOnlineSearchSourceForTestWorksCorrectly() {
        val remoteConfigManager = RemoteConfigManager()
        remoteConfigManager.setIsOnlineSearchSourceForTest(false)
        assertEquals(false, remoteConfigManager.isOnlineSearchSource.value)
        remoteConfigManager.setIsOnlineSearchSourceForTest(true)
        assertEquals(true, remoteConfigManager.isOnlineSearchSource.value)
    }

    @Test
    fun searchOnlineReturnsEmptyWhenQueryIsBlank() = runTest {
        val remoteConfigManager = RemoteConfigManager()
        val repository = OnlineSearchRepositoryImpl(remoteConfigManager = remoteConfigManager, preferences = null)
        val results = repository.searchOnline("").first()
        assertTrue("Blank query should return empty list", results.isEmpty())
    }

    @Test
    fun getSearchCompletionsReturnsEmptyWhenQueryIsBlank() = runTest {
        val remoteConfigManager = RemoteConfigManager()
        val repository = OnlineSearchRepositoryImpl(remoteConfigManager = remoteConfigManager, preferences = null)
        val completions = repository.getSearchCompletions("")
        assertTrue("Blank query completions should return empty list", completions.isEmpty())
    }
}
