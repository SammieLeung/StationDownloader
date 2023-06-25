package com.station.stationkitkt

import android.content.Context

/**
 * author: Sam Leung
 * date:  2023/3/9
 */


object DimenUtils {
    lateinit var appContext: Context
    fun init(context: Context) {
        appContext = context
    }
}

val Double.dp: Int
    get() {
        return toFloat().dp
    }

val Int.dp: Int
    get() = toFloat().dp


val Float.dp: Int
    get() {
        val scale = DimenUtils.appContext.resources.displayMetrics.density
        return (this * scale + 0.5f).toInt()
    }