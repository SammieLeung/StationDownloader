package com.station.stationdownloader.domain

import android.os.SystemClock
import com.orhanobut.logger.Logger
import javax.inject.Inject

class LoggerUseCase @Inject constructor() {
    private var markTime: Long = 0L
    fun mark() {
        markTime = SystemClock.elapsedRealtime()
    }

    fun printTime(msg: String) {
        Logger.d("$msg [${SystemClock.elapsedRealtime() - markTime} ms]")
    }

}