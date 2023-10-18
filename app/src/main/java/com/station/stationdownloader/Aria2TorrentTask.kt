package com.station.stationdownloader

import org.json.JSONObject

data class Aria2TorrentTask(
    val taskStatus: TaskStatus,
    val hashInfo: String,
){
    companion object{
        fun create(jsonObject: JSONObject): Aria2TorrentTask{
            val hash=jsonObject.getString("infoHash")
            val taskStatus=TaskStatus(
                TaskId(
                    DownloadEngine.ARIA2,
                    jsonObject.getString("gid")
                ),
                "",
                jsonObject.getString("bittorrent").toLong(),
                jsonObject.getString("completedLength").toLong(),
                jsonObject.getString("totalLength").toLong(),
                when (jsonObject.getString("status")) {
                    "active" -> ITaskState.RUNNING.code
                    "waiting" -> ITaskState.LOADING.code
                    "paused" -> ITaskState.STOP.code
                    "complete" -> ITaskState.DONE.code
                    "error" -> ITaskState.ERROR.code
                    else -> ITaskState.UNKNOWN.code
                }
            )
            return Aria2TorrentTask(taskStatus,hash)
        }
    }
}