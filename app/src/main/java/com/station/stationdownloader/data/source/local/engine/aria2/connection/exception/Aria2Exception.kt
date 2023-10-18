package com.station.stationdownloader.data.source.local.engine.aria2.connection.exception

import org.json.JSONException
import org.json.JSONObject

class Aria2Exception(val reason: String, val code: Int) : Exception(reason) {

    @Throws(JSONException::class)
    constructor(error: JSONObject) : this(
        reason = error.getString("message"),
        code = error.getInt("code")
    )

    fun isNoPeers(): Boolean {
        return reason.startsWith("No peer data is available")
    }

    fun isNoServers(): Boolean {
        return reason.startsWith("No active download")
    }

    fun isNotFound(): Boolean {
        return reason.endsWith("is not found")
    }

    fun isCannotChangeOptions(): Boolean {
        return reason.startsWith("Cannot change option")
    }

    override fun toString(): String {
        return "AriaException #$code: $message"
    }

}