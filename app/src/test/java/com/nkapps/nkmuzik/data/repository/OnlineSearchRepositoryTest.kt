package com.nkapps.nkmuzik.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineSearchRepositoryTest {

    @Test
    fun searchOnlineReturnsEmptyWhenQueryIsBlank() = runTest {
        val repository = OnlineSearchRepositoryImpl(preferences = null)
        val results = repository.searchOnline("").first()
        assertTrue("Blank query should return empty list", results.isEmpty())
    }

    @Test
    fun getSearchCompletionsReturnsEmptyWhenQueryIsBlank() = runTest {
        val repository = OnlineSearchRepositoryImpl(preferences = null)
        val completions = repository.getSearchCompletions("")
        assertTrue("Blank query completions should return empty list", completions.isEmpty())
    }
}
