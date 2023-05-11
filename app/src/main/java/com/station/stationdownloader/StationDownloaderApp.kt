package com.station.stationdownloader

import android.app.Application
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.facebook.stetho.Stetho
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.station.stationdownloader.domain.LoggerUseCase
import com.tencent.mmkv.MMKV
import com.xunlei.downloadlib.XLTaskHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * author: Sam Leung
 * date:  2023/5/12
 */

@HiltAndroidApp
class StationDownloaderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Logger.addLogAdapter(AndroidLogAdapter())
        CoroutineScope(Job()).launch {
            withContext(Dispatchers.Default) {
                Stetho.initializeWithDefaults(applicationContext)
                DownloaderService.startService(applicationContext)
            }
        }

    }

}