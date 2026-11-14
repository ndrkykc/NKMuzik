package com.nkapps.nkmuzik.data.repository

import com.nkapps.nkmuzik.data.model.LocalAudioTrack
import com.nkapps.nkmuzik.data.model.OnlineSearchResultDto
import kotlinx.coroutines.flow.Flow

interface LocalMediaRepository {
    fun getLocalAudioTracks(): Flow<List<LocalAudioTrack>>
    suspend fun scanLocalMedia(): List<LocalAudioTrack>
    suspend fun deleteTrack(track: LocalAudioTrack): Boolean
    fun searchTracks(query: String): Flow<List<OnlineSearchResultDto>>
}