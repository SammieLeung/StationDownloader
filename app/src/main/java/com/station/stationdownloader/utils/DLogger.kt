package com.station.stationdownloader.utils

import android.util.Log
import com.station.stationdownloader.data.source.local.engine.xl.XLEngine

interface DLogger {
    fun logger(message: String) {
        Log.d(tag(), "[${tag()}]>>$message<<")
    }

    fun DLogger.tag(): String
}