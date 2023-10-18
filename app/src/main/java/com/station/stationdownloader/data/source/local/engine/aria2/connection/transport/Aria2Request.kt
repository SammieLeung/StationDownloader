package com.station.stationdownloader.data.source.local.engine.aria2.connection.transport

import android.util.Log
import com.station.stationdownloader.data.source.local.engine.aria2.connection.client.WebSocketClient
import com.station.stationdownloader.data.source.local.engine.aria2.connection.client.WebSocketClient.Method
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

open class Aria2Request constructor(
    val method: Method,
     val params: Array<Any>
) {
    var id: Long = 0

    @Throws(JSONException::class)
    fun build(client: WebSocketClient): JSONObject {
        id = client.nextRequestId()
        val request = JSONObject()
        request.put("jsonrpc", "2.0")
        request.put("id", id.toString())
        request.put("method", method.method)
        val params: JSONArray = client.baseRequestParams()

        for (obj in this.params) {
            params.put(obj)
        }
        request.put("params", params)
        return request
    }
}