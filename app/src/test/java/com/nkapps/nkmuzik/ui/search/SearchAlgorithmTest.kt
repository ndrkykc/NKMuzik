package com.nkapps.nkmuzik.ui.search

import com.nkapps.nkmuzik.data.db.entity.PlaylistTrackEntity
import com.nkapps.nkmuzik.data.model.LocalTrack
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchAlgorithmTest {

    @Test
    fun testLocalTrackSearchScore_titleMatch_gives100Points() {
        val track = LocalTrack(
            id = 1L,
            title = "Tarkan - Dudu",
            artist = "Pop Artist",
            album = "Pop Hits",
            duration = "3:30",
            uri = "content://media/1",
            filePath = "/storage/emulated/0/Music/01.mp3"
        )

        val score = calculateLocalTrackSearchScore(track, "Dudu")
        assertEquals(100, score)
    }

    @Test
    fun testLocalTrackSearchScore_artistMatch_gives60Points() {
        val track = LocalTrack(
            id = 1L,
            title = "Sen Başkasın",
            artist = "Tarkan",
            album = "Karma",
            duration = "4:15",
            uri = "content://media/1",
            filePath = "/storage/emulated/0/Music/01.mp3"
        )

        val score = calculateLocalTrackSearchScore(track, "Tarkan")
        assertEquals(60, score)
    }

    @Test
    fun testLocalTrackSearchScore_albumMatch_gives30Points() {
        val track = LocalTrack(
            id = 1L,
            title = "Sen Başkasın",
            artist = "Artist",
            album = "Best of Tarkan",
            duration = "4:15",
            uri = "content://media/1",
            filePath = "/storage/emulated/0/Music/01.mp3"
        )

        val score = calculateLocalTrackSearchScore(track, "Tarkan")
        assertEquals(30, score)
    }

    @Test
    fun testLocalTrackSearchScore_folderMatch_gives10Points() {
        val track = LocalTrack(
            id = 1L,
            title = "Sen Başkasın",
            artist = "Artist",
            album = "Karma",
            duration = "4:15",
            uri = "content://media/1",
            filePath = "/storage/emulated/0/Music/Tarkan/01.mp3"
        )

        val score = calculateLocalTrackSearchScore(track, "Tarkan")
        assertEquals(10, score)
    }

    @Test
    fun testLocalTrackSearchScore_multipleFieldMatch_accumulatesPoints() {
        val track = LocalTrack(
            id = 1L,
            title = "Tarkan - Dudu",
            artist = "Tarkan",
            album = "Best of Tarkan",
            duration = "3:30",
            uri = "content://media/1",
            filePath = "/storage/emulated/0/Music/Tarkan/01.mp3"
        )

        val score = calculateLocalTrackSearchScore(track, "Tarkan")
        assertEquals(200, score)
    }

    @Test
    fun testLocalTrackSearchRanking_sortsDescendingByScore() {
        val track1 = LocalTrack(
            id = 1L,
            title = "Sen Başkasın",
            artist = "Tarkan",
            album = "Karma",
            duration = "4:15",
            uri = "content://media/1",
            filePath = "/storage/emulated/0/Music/pop/01.mp3"
        )

        val track2 = LocalTrack(
            id = 2L,
            title = "Tarkan Dudu",
            artist = "Tarkan",
            album = "Dudu",
            duration = "3:30",
            uri = "content://media/2",
            filePath = "/storage/emulated/0/Music/Tarkan/02.mp3"
        )

        val track3 = LocalTrack(
            id = 3L,
            title = "Song 3",
            artist = "Artist 3",
            album = "Album 3",
            duration = "3:00",
            uri = "content://media/3",
            filePath = "/storage/emulated/0/Music/Tarkan/03.mp3"
        )

        val tracks = listOf(track1, track2, track3)
        val searchResults = tracks
            .map { it to calculateLocalTrackSearchScore(it, "Tarkan") }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }

        assertEquals(3, searchResults.size)
        assertEquals(track2.id, searchResults[0].id)
        assertEquals(track1.id, searchResults[1].id)
        assertEquals(track3.id, searchResults[2].id)
    }

    @Test
    fun testOnlineTrackSearchScore_matchesTitleAndArtist() {
        val dto = OnlineSearchResultDto(
            videoId = "v1",
            title = "Tarkan - Kuzu Kuzu",
            uploaderName = "Tarkan Channel",
            durationString = "3:50",
            thumbnailUrl = "",
            url = "https://stream.provider/v1"
        )

        val score = calculateOnlineTrackSearchScore(dto, "Tarkan")
        assertEquals(160, score)
    }

    @Test
    fun testPlaylistTrackSearchScore_matchesTitleAndArtist() {
        val entity = PlaylistTrackEntity(
            id = 10L,
            playlistId = 1L,
            trackId = "v1",
            title = "Dudu",
            artist = "Tarkan",
            duration = "3:30",
            artworkUri = null,
            mediaUri = "/storage/emulated/0/Music/pop/01.mp3"
        )

        val score = calculatePlaylistTrackSearchScore(entity, "Tarkan")
        assertEquals(60, score)
    }
}
