package com.nkapps.nkmuzik.di

import com.nkapps.nkmuzik.data.repository.LyricsRepository
import com.nkapps.nkmuzik.data.repository.LyricsRepositoryImpl
import com.nkapps.nkmuzik.data.repository.LocalMediaRepository
import com.nkapps.nkmuzik.data.repository.LocalMediaRepositoryImpl
import com.nkapps.nkmuzik.data.repository.PlaylistRepository
import com.nkapps.nkmuzik.data.repository.PlaylistRepositoryImpl
import com.nkapps.nkmuzik.data.repository.OnlineSearchRepository
import com.nkapps.nkmuzik.data.repository.OnlineSearchRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocalMediaRepository(
        impl: LocalMediaRepositoryImpl
    ): LocalMediaRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        impl: PlaylistRepositoryImpl
    ): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindOnlineSearchRepository(
        impl: OnlineSearchRepositoryImpl
    ): OnlineSearchRepository

    @Binds
    @Singleton
    abstract fun bindLyricsRepository(
        impl: LyricsRepositoryImpl
    ): LyricsRepository
}
