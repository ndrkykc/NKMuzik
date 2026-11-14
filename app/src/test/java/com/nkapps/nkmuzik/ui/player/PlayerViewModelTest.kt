package com.nkapps.nkmuzik.ui.player

import com.nkapps.nkmuzik.data.db.entity.PlaylistEntity
import com.nkapps.nkmuzik.data.db.entity.PlaylistTrackEntity
import com.nkapps.nkmuzik.data.model.LocalTrack
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import com.nkapps.nkmuzik.data.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakePlaylistRepository = object : PlaylistRepository {
        override fun getAllPlaylists(): Flow<List<PlaylistEntity>> = flowOf(emptyList())
        override fun getPlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>> = flowOf(emptyList())
        override suspend fun createPlaylist(name: String): Long = 1L
        override suspend fun deletePlaylist(playlistId: Long) {}
        override suspend fun updatePlaylistName(playlistId: Long, newName: String) {}
        override suspend fun addTrackToPlaylist(playlistId: Long, track: OnlineSearchResultDto) {}
        override suspend fun addLocalTrackToPlaylist(playlistId: Long, track: LocalTrack) {}
        override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {}
        override suspend fun deleteTrackFromAllPlaylists(trackId: String, mediaUri: String) {}
        override fun getFavoriteTracks(): Flow<List<OnlineSearchResultDto>> = flowOf(emptyList())
        override suspend fun toggleFavorite(track: OnlineSearchResultDto) {}
        override suspend fun isFavorite(videoId: String, url: String): Boolean = false
        override suspend fun addFavorite(track: OnlineSearchResultDto) {}
        override suspend fun removeFavorite(videoId: String, url: String) {}
    }

    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = PlayerViewModel(fakePlaylistRepository, com.nkapps.nkmuzik.player.SleepTimerController())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun updatePlayingState_updatesStateFlows() {
        val dummyTrack = OnlineSearchResultDto(
            videoId = "test_1",
            title = "Test Song",
            uploaderName = "Test Artist",
            durationString = "3:30",
            thumbnailUrl = "http://example.com/thumb.jpg",
            url = "http://example.com/video"
        )

        viewModel.updatePlayingState(
            track = dummyTrack,
            playing = true,
            position = 1000L,
            trackDuration = 210000L
        )

        assertEquals(dummyTrack, viewModel.currentPlayingTrack.value)
        assertTrue(viewModel.isPlaying.value)
        assertEquals(1000L, viewModel.currentPosition.value)
        assertEquals(210000L, viewModel.duration.value)
    }

    @Test
    fun toggleShuffle_togglesValue() {
        assertFalse(viewModel.isShuffleEnabled.value)
        viewModel.toggleShuffle()
        assertTrue(viewModel.isShuffleEnabled.value)
        viewModel.toggleShuffle()
        assertFalse(viewModel.isShuffleEnabled.value)
    }

    @Test
    fun toggleRepeat_cyclesThroughModes() {
        assertEquals(0, viewModel.repeatMode.value)
        viewModel.toggleRepeat()
        assertEquals(1, viewModel.repeatMode.value)
        viewModel.toggleRepeat()
        assertEquals(2, viewModel.repeatMode.value)
        viewModel.toggleRepeat()
        assertEquals(0, viewModel.repeatMode.value)
    }
}
