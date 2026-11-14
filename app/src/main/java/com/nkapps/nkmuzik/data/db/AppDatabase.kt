package com.nkapps.nkmuzik.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nkapps.nkmuzik.data.db.dao.PlaylistDao
import com.nkapps.nkmuzik.data.db.entity.FavoriteTrackEntity
import com.nkapps.nkmuzik.data.db.entity.PlaylistEntity
import com.nkapps.nkmuzik.data.db.entity.PlaylistTrackEntity

@Database(
    entities = [PlaylistEntity::class, PlaylistTrackEntity::class, FavoriteTrackEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
}
