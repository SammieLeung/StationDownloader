package com.station.stationdownloader.data.datasource.engine.aria2

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

inline fun InternalResponse.wait(timeout:Long) = (this as Object).wait(timeout)