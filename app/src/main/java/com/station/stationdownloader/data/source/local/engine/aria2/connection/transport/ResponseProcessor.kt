package com.station.stationdownloader.data.source.local.engine.aria2.connection.transport

import androidx.annotation.WorkerThread
import com.station.stationdownloader.data.source.local.engine.aria2.connection.client.WebSocketClient
import org.json.JSONException
import org.json.JSONObject

abstract class ResponseProcessor<R> {
    @WorkerThread
    @Throws(JSONException::class)
    abstract fun process(client: WebSocketClient, obj: JSONObject): R
}