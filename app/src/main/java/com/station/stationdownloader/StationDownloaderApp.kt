package com.station.stationdownloader

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.facebook.stetho.Stetho
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.source.local.engine.IEngine
import com.station.stationdownloader.di.XLEngineAnnotation
import com.station.stationkitkt.DimenUtils
import com.station.stationkitkt.MimeTypeHelper
import com.station.stationkitkt.MoshiHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * author: Sam Leung
 * date:  2023/5/12
 */

@HiltAndroidApp
class StationDownloaderApp : Application() {
    val mApplicationScope = CoroutineScope(SupervisorJob())

    @Inject
    @XLEngineAnnotation
    lateinit var mXLEngine: IEngine

    override fun onCreate() {
        super.onCreate()

        mApplicationScope.launch {
            Logger.addLogAdapter(AndroidLogAdapter())
            Logger.d("StationDownloaderApp start!")
            DimenUtils.init(context = applicationContext)
            mXLEngine.init()
            Stetho.initializeWithDefaults(applicationContext)
            MoshiHelper.init()
            MimeTypeHelper.init(applicationContext)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        Logger.clearLogAdapters()
        mApplicationScope.launch {
            mXLEngine.unInit()
        }

    }


    val mBroadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

        }
    }
}