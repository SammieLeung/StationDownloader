package com.station.stationdownloader.utils

import android.util.Log

interface DLogger {
    fun logger(message: String) {
        Log.w(tag(), "[${tag()}]>>$message<<")
    }

    fun takeTime(t:Long){
        val stackTrace=Thread.currentThread().stackTrace
        if(stackTrace.size>4){
            val parentMethod=stackTrace[4].methodName
            Log.d(tag(), ">>$parentMethod takes ${System.currentTimeMillis()-t}ms<<")
        }else{
            Log.d(tag(), ">>${tag()} takes ${System.currentTimeMillis()-t}ms<<")
        }
    }

    fun DLogger.tag(): String
}