package com.station.stationdownloader.data.datasource.engine.aria2

import android.content.Context
import com.gianlu.aria2lib.commonutils.Prefs
import com.gianlu.aria2lib.internal.Aria2
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.datasource.engine.IEngine
import com.station.stationdownloader.data.datasource.model.StationDownloadTask
import com.station.stationdownloader.data.datasource.model.StationTaskInfo
import com.station.stationdownloader.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * author: Sam Leung
 * date:  2023/5/9
 */
class Aria2Engine internal constructor(
    private val appContext: Context,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : IEngine {
    lateinit var client: WebSocketClient
    override suspend fun init() = withContext(defaultDispatcher) {
        Prefs.init(appContext)
        val aria2 = Aria2.get()
        val parent: File = appContext.getFilesDir()
        aria2.loadEnv(
            parent,
            File(appContext.getApplicationInfo().nativeLibraryDir, "libaria2c.so"),
            File(parent, "session")
        )

        aria2.start()
        client = WebSocketClient()
    }

    override suspend fun unInit() {
        TODO("Not yet implemented")
    }

    override suspend fun initTask(url: String): IResult<StationDownloadTask> {
        TODO("Not yet implemented")
    }

    override suspend fun getTaskSize(
        task: StationDownloadTask,
        timeOut: Long
    ): IResult<StationDownloadTask> {
        TODO("Not yet implemented")
    }

    override suspend fun startTask(
        url: String,
        downloadPath: String,
        name: String,
        urlType: DownloadUrlType,
        fileCount: Int,
        selectIndexes: IntArray
    ): IResult<Long> {
        TODO("Not yet implemented")
    }

    override suspend fun stopTask(task: StationDownloadTask) {
        TODO("Not yet implemented")
    }

    override suspend fun getTaskInfo(taskId: Long): StationTaskInfo {
        TODO("Not yet implemented")
    }

    override suspend fun configure(key: String, values: Array<String>): IResult<Unit> {
        TODO("Not yet implemented")
    }

    private fun test() {

        val request = JSONObject()
        request.put("jsonrpc", "2.0")
        request.put("id", "1")
        request.put("method", "aria2.getVersion")
        val params = JSONArray()
        params.put("token:aria2")
        request.put("params", params)
        client.send(request.toString())
    }


}