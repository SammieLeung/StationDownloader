package com.station.stationdownloader.utils

import android.util.Log

interface DLogger {
    fun logger(message: String) {
        Log.w(tag(), "[${tag()}]>>$message<<")
    }

    fun printCodeLine() {
        val stackTrace = Thread.currentThread().stackTrace
        if (stackTrace.size > 4) {
            val className=stackTrace[4].className
            val parentMethod = stackTrace[4].methodName
            val lineNumber = stackTrace[4].lineNumber
            Log.w(className, "$parentMethod()->line:$lineNumber")
        }
    }

    fun takeTime(t: Long) {
        val stackTrace = Thread.currentThread().stackTrace
        if (stackTrace.size > 4) {
            val parentMethod = stackTrace[4].methodName
            Log.w(tag(), ">>$parentMethod takes ${System.currentTimeMillis() - t}ms<<")
        } else {
            Log.w(tag(), ">>${tag()} takes ${System.currentTimeMillis() - t}ms<<")
        }
    }

    fun DLogger.tag(): String
}