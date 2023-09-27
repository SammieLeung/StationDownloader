package com.station.stationdownloader

import android.app.Application
import com.facebook.stetho.Stetho
import com.gianlu.aria2lib.Aria2Ui
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.utils.DLogger
import com.station.stationkitkt.DimenUtils
import com.station.stationkitkt.MimeTypeHelper
import com.station.stationkitkt.MoshiHelper
import com.station.stationkitkt.PackageTools
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * author: Sam Leung
 * date:  2023/5/12
 */

@HiltAndroidApp
class StationDownloaderApp : Application(), DLogger {
    val mApplicationScope = CoroutineScope(SupervisorJob())
    val useV2FileManager: Boolean by lazy {
        PackageTools.isAppInstalled(applicationContext, FILE_MANAGER_V2_PACKAGE)
    }
    private var initialized = false


    override fun onTerminate() {
        super.onTerminate()
        Logger.clearLogAdapters()
    }

    fun initialize() {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    DimenUtils.init(context = applicationContext)
                    Logger.addLogAdapter(AndroidLogAdapter())
                    Stetho.initializeWithDefaults(applicationContext)
                    MoshiHelper.init()
                    MimeTypeHelper.init(context = applicationContext)
                    initialized = true
                    Logger.d("StationDownloaderApp initialized")
                }
            }
        }
    }

    fun isInitialized(): Boolean {
        return initialized
    }

    companion object {
        const val FILE_MANAGER_V2_PACKAGE = "com.firefly.resourcemanager"
    }

    override fun DLogger.tag(): String {
        return "StationApp"
    }

}