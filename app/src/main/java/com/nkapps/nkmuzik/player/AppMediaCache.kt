package com.nkapps.nkmuzik.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.nkapps.nkmuzik.data.repository.OnlineSearchRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Provider

@OptIn(UnstableApi::class)
@Singleton
class AppMediaCache @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repositoryProvider: Provider<OnlineSearchRepository>
) {
    private val cacheSize: Long = 100 * 1024 * 1024 // 100 MB
    private val evictor = LeastRecentlyUsedCacheEvictor(cacheSize)
    private val databaseProvider = StandaloneDatabaseProvider(context)

    val simpleCache: SimpleCache = SimpleCache(
        File(context.cacheDir, "media_cache"),
        evictor,
        databaseProvider
    )

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)

    private val dynamicUpstreamFactory = ResolvingDataSource.Factory(httpDataSourceFactory) { dataSpec: DataSpec ->
        val mediaId = dataSpec.key ?: dataSpec.uri.toString()
        val streamInfo = repositoryProvider.get().getCachedStreamInfo(mediaId)

        val updatedHeaders = HashMap<String, String>(dataSpec.httpRequestHeaders)
        if (streamInfo != null && streamInfo.headers.isNotEmpty()) {
            streamInfo.headers.forEach { (k, v) ->
                updatedHeaders[k] = v
            }
        } else {
            // Fallback headers
            updatedHeaders["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            updatedHeaders["Referer"] = "https://www.youtube.com/"
            updatedHeaders["Origin"] = "https://www.youtube.com"
        }

        dataSpec.buildUpon()
            .setHttpRequestHeaders(updatedHeaders)
            .build()
    }

    val defaultDataSourceFactory = DefaultDataSource.Factory(context, dynamicUpstreamFactory)

    val cacheDataSourceFactory: CacheDataSource.Factory = CacheDataSource.Factory()
        .setCache(simpleCache)
        .setUpstreamDataSourceFactory(defaultDataSourceFactory)
        .setCacheReadDataSourceFactory(FileDataSource.Factory())
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
}
