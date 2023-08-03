package com.station.stationdownloader

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.facebook.stetho.Stetho
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.local.engine.IEngine
import com.station.stationdownloader.di.XLEngineAnnotation
import com.station.stationdownloader.utils.DLogger
import com.station.stationkitkt.DimenUtils
import com.station.stationkitkt.MimeTypeHelper
import com.station.stationkitkt.MoshiHelper
import com.station.stationkitkt.PackageTools
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * author: Sam Leung
 * date:  2023/5/12
 */

@HiltAndroidApp
class StationDownloaderApp : Application() {
    val mApplicationScope = CoroutineScope(SupervisorJob())
    val useV2FileManager: Boolean by lazy {
        PackageTools.isAppInstalled(applicationContext, FILE_MANAGER_V2_PACKAGE)
    }
    private var initialized = false

    @Inject
    lateinit var engineRepo: IEngineRepository

    override fun onTerminate() {
        super.onTerminate()
        Logger.d("onTerminate")
        Logger.clearLogAdapters()
        mApplicationScope.launch {
            Stetho.initializeWithDefaults(applicationContext)
        }
    }

    suspend fun initAction() {
        Logger.addLogAdapter(AndroidLogAdapter())
        DimenUtils.init(context = applicationContext)
        MoshiHelper.init()
        MimeTypeHelper.init(context = applicationContext)
        engineRepo.init()
        initialized = true
    }

    fun isInitialized(): Boolean {
        return initialized
    }

    val mBroadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

        }
    }

    companion object {
        const val FILE_MANAGER_V2_PACKAGE = "com.firefly.resourcemanager"
    }


}