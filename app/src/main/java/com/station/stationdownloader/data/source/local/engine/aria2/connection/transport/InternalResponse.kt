package com.station.stationdownloader.data.source.local.engine.aria2.connection.transport

import okhttp3.internal.notifyAll
import org.json.JSONObject

 class InternalResponse(
    @Volatile  var data: JSONObject? = null,
    @Volatile  var exception: Exception? = null
) {
    @Synchronized
    fun success(jsonObject: JSONObject) {
        this.data = jsonObject
        this.exception = null
        notifyAll()
    }

    @Synchronized
    fun failed(exception: Exception) {
        this.exception = exception
        this.data = null
        notifyAll()
    }
}
