package com.station.stationdownloader

import org.json.JSONObject

data class Aria2TorrentTask(
    val taskStatus: TaskStatus,
    val hashInfo: String,
) {
    companion object {
        fun create(jsonObject: JSONObject): Aria2TorrentTask {
            val hash = jsonObject.getString("infoHash")
            val taskStatus = TaskStatus.createForAria2(jsonObject)
            return Aria2TorrentTask(taskStatus, hash)
        }
    }
}