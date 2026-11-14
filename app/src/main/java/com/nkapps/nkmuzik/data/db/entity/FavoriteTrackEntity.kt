package com.nkapps.nkmuzik.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val uploaderName: String,
    val durationString: String,
    val thumbnailUrl: String,
    val url: String,
    val addedAt: Long = System.currentTimeMillis()
)
