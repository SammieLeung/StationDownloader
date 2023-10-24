package com.station.stationdownloader.data.source.local.engine.aria2.connection.transport

import com.station.stationdownloader.Aria2TorrentTask
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.TaskId
import com.station.stationdownloader.TaskStatus
import com.station.stationdownloader.data.source.local.engine.aria2.connection.client.WebSocketClient
import com.station.stationdownloader.data.source.local.engine.aria2.connection.common.OptionsMap
import com.station.stationdownloader.data.source.local.engine.aria2.connection.util.CommonUtils
import org.json.JSONException
import org.json.JSONObject

object Aria2Requests {
    private val TASKLIST_KEYS = CommonUtils.toJSONArray(
        listOf(
            "gid",
            "bittorrent",
            "totalLength",
            "completedLength",
            "downloadSpeed",
            "infoHash",
            "status",
            "errorCode",
            "errorMessage"
        ), true
    )

    private val STATUS_KEYS = CommonUtils.toJSONArray(
        listOf(
            "gid",
            "status",
            "totalLength",
            "completedLength",
            "downloadSpeed",
            "errorCode",
            "errorMessage"
        ), true
    )

    private val STRING_PROCESSOR: ResponseProcessor<String> = object : ResponseProcessor<String>() {
        @Throws(JSONException::class)
        override fun process(client: WebSocketClient, obj: JSONObject): String {
            return obj.getString("result")
        }
    }

    private val DOWNLOADS_LIST_PROCESSOR: ResponseProcessor<List<Aria2TorrentTask>> =
        object : ResponseProcessor<List<Aria2TorrentTask>>() {
            @Throws(JSONException::class)
            override fun process(client: WebSocketClient, obj: JSONObject): List<Aria2TorrentTask> {
                val aria2TaskList = mutableListOf<Aria2TorrentTask>()
                val array = obj.getJSONArray("result")
                for (i in 0 until array.length()) {
                    val taskJSONObject = array.getJSONObject(i)
                    if (!taskJSONObject.has("bittorrent"))
                        continue
                    if (taskJSONObject.getJSONObject("bittorrent").has("info"))
                        aria2TaskList.add(Aria2TorrentTask.create(array.getJSONObject(i)))
                }
                return aria2TaskList
            }
        }

    @Throws(JSONException::class)
    fun addTorrent(
        base64: String,
        uris: Collection<String?> = emptyList(),
        pos: Int = Int.MAX_VALUE,
        options: OptionsMap = OptionsMap()
    ): Aria2RequestWithResult<String> {
        return Aria2RequestWithResult(
            WebSocketClient.Method.ADD_TORRENT,
            STRING_PROCESSOR,
            arrayOf(
                base64,
                CommonUtils.toJSONArray(uris, true),
                options.toJson(),
                pos
            )
        )
    }

    fun pause(gid: String): Aria2Request {
        return Aria2Request(WebSocketClient.Method.PAUSE, arrayOf(gid))
    }

    fun unpause(gid: String): Aria2Request {
        return Aria2Request(WebSocketClient.Method.PAUSE, arrayOf(gid))
    }

    fun remove(gid: String): Aria2Request {
        return Aria2Request(WebSocketClient.Method.REMOVE, arrayOf(gid))
    }


    fun saveSession(): Aria2Request {
        return Aria2Request(WebSocketClient.Method.SAVE_SESSION, emptyArray())
    }

    fun tellStatus(gid: String): Aria2RequestWithResult<TaskStatus> {
        return Aria2RequestWithResult(
            WebSocketClient.Method.TELL_STATUS,
            object : ResponseProcessor<TaskStatus>() {
                override fun process(client: WebSocketClient, obj: JSONObject): TaskStatus {
                    return TaskStatus(taskId = TaskId(DownloadEngine.ARIA2, gid)).parseJSONObject(
                        obj.getJSONObject("result")
                    )
                }
            },
            arrayOf(
                gid,
                STATUS_KEYS
            )
        )
    }

    fun tellActive(): Aria2RequestWithResult<List<Aria2TorrentTask>> {
        return Aria2RequestWithResult(
            WebSocketClient.Method.TELL_ACTIVE,
            DOWNLOADS_LIST_PROCESSOR,
            arrayOf(
                TASKLIST_KEYS
            )
        )
    }

    fun tellWaiting(offset: Int, count: Int): Aria2RequestWithResult<List<Aria2TorrentTask>> {
        return Aria2RequestWithResult(
            WebSocketClient.Method.TELL_WAITING,
            DOWNLOADS_LIST_PROCESSOR,
            arrayOf(
                offset,
                count,
                TASKLIST_KEYS
            )
        )
    }

    fun tellStopped(offset: Int, count: Int): Aria2RequestWithResult<List<Aria2TorrentTask>> {
        return Aria2RequestWithResult(
            WebSocketClient.Method.TELL_STOPPED,
            DOWNLOADS_LIST_PROCESSOR,
            arrayOf(
                offset,
                count,
                TASKLIST_KEYS
            )
        )
    }


}