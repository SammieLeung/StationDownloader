package com.station.stationdownloader.data.source.local.engine.aria2.connection.transport

import com.orhanobut.logger.Logger
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
            "files",
            "totalLength",
            "completedLength",
            "downloadSpeed",
            "infoHash",
            "status",
            "errorCode",
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
                    if (taskJSONObject.getJSONObject("bittorrent").has("info"))//FIXME ????
                        aria2TaskList.add(Aria2TorrentTask.create(taskJSONObject))
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

    @Throws(JSONException::class)
    fun addUri(
        uris: Collection<String?> = emptyList(),
        pos: Int = Int.MAX_VALUE,
        options: OptionsMap = OptionsMap()
    ): Aria2RequestWithResult<String> {
        return Aria2RequestWithResult(
            WebSocketClient.Method.ADD_URI,
            STRING_PROCESSOR,
            arrayOf(
                CommonUtils.toJSONArray(uris, true),
                options.toJson(),
                pos
            )
        )
    }

    fun pause(gid: String): Aria2Request {
        return Aria2Request(WebSocketClient.Method.FORCE_PAUSE, arrayOf(gid))
    }

    fun unpause(gid: String): Aria2Request {
        return Aria2Request(WebSocketClient.Method.UNPAUSE, arrayOf(gid))
    }

    fun remove(gid: String): Aria2Request {
        return Aria2Request(WebSocketClient.Method.FORCE_REMOVE, arrayOf(gid))
    }

    fun removeDownloadResult(gid: String): Aria2Request {
        return Aria2Request(WebSocketClient.Method.REMOVE_RESULT, arrayOf(gid))
    }

    fun changeGlobalOptions(options: OptionsMap): Aria2Request {
        return Aria2Request(WebSocketClient.Method.CHANGE_GLOBAL_OPTIONS, arrayOf(options.toJson()))
    }

    fun saveSession(): Aria2Request {
        return Aria2Request(WebSocketClient.Method.SAVE_SESSION, emptyArray())
    }

    fun tellStatus(gid: String): Aria2RequestWithResult<TaskStatus> {
        return Aria2RequestWithResult(
            WebSocketClient.Method.TELL_STATUS,
            object : ResponseProcessor<TaskStatus>() {
                override fun process(client: WebSocketClient, obj: JSONObject): TaskStatus {
                    return TaskStatus.createForAria2(obj.getJSONObject("result"))
                }
            },
            arrayOf(
                gid,
                STATUS_KEYS
            )
        )
    }

    fun getFiles(gid: String): Aria2RequestWithResult<Any> {
        return Aria2RequestWithResult(
            WebSocketClient.Method.GET_FILES,
            object : ResponseProcessor<Any>() {
                override fun process(client: WebSocketClient, obj: JSONObject): Unit {
                    Logger.d(obj.toString())
                    return
                }
            },
            arrayOf(
                gid,
                TASKLIST_KEYS
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