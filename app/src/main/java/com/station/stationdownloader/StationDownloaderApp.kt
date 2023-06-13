package com.station.stationdownloader

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.facebook.stetho.Stetho
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.squareup.moshi.Moshi
import com.station.stationkitkt.MoshiHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * author: Sam Leung
 * date:  2023/5/12
 */

@HiltAndroidApp
class StationDownloaderApp : Application() {
    val mApplicationScope = CoroutineScope(SupervisorJob())
    override fun onCreate() {
        super.onCreate()
        Logger.addLogAdapter(AndroidLogAdapter())
        Logger.d("StationDownloaderApp start!")
        mApplicationScope.launch {
            withContext(Dispatchers.Default) {
                Stetho.initializeWithDefaults(applicationContext)
                MoshiHelper.init()
            }
        }
    }


    val mBroadCastReceiver=object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

        }
    }
}