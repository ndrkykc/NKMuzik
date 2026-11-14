package com.nkapps.nkmuzik

import android.app.Application
import android.util.Log
import com.nkapps.nkmuzik.utils.NewPipeDownloader
import dagger.hilt.android.HiltAndroidApp
import org.schabi.newpipe.extractor.NewPipe

@HiltAndroidApp
class NKApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initNewPipe()
    }

    private fun initNewPipe() {
        try {
            NewPipe.init(NewPipeDownloader())
            Log.d("NKApplication", "NewPipeExtractor başarıyla başlatıldı.")
        } catch (e: Exception) {
            Log.e("NKApplication", "NewPipe başlatılamadı: ${e.message}", e)
        }
    }
}
