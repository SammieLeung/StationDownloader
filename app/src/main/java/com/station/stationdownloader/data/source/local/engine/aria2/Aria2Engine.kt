package com.station.stationdownloader.data.source.local.engine.aria2

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.gianlu.aria2lib.commonutils.Prefs
import com.gianlu.aria2lib.internal.Aria2
import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.local.engine.IEngine
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
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
        val parent: File = appContext.filesDir
        aria2.loadEnv(
            parent,
            File(appContext.applicationInfo.nativeLibraryDir, "libaria2c.so"),
            File(parent, "session")
        )

        aria2.start()
        client = WebSocketClient()
    }

    override suspend fun unInit() {
        client.close()
    }

    override suspend fun initUrl(url: String): IResult<NewTaskConfigModel> {
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
        when (urlType) {
            DownloadUrlType.TORRENT ->
                addTorrent(url)

            DownloadUrlType.HTTP ->
                addUri(url)

            else -> {}
        }
        addTorrent(url)
        return IResult.Success(1)
    }

    override suspend fun stopTask(taskId: Long) {
        TODO("Not yet implemented")
    }


    override suspend fun configure(key: String, values: String): IResult<Unit> {
        return IResult.Success(Unit)
    }


    private fun addUriRequest(url: String): AriaRequest {
        val uriArray = JSONArray().put(url)
        val configObj = JSONObject()
        val params = arrayOf(uriArray, configObj, Int.MAX_VALUE)
        return AriaRequest(
            Aria2Method.ADD_URI,
            params
        )
    }

    private fun addTorrentRequest(url: String): AriaRequest {
        val base64 = base64(url)
        val uris = JSONArray()
        val options = JSONObject()
        val params = arrayOf(base64, uris, options, Int.MAX_VALUE)
        return AriaRequest(
            Aria2Method.ADD_TORRENT,
            params
        )
    }

    private fun addUri(url: String) {
        val request = addUriRequest(url).build(client)
        Logger.d("${request.toString()}")
        client.send(request.toString())
    }


    private fun addTorrent(url: String) {
        val request = addTorrentRequest(url).build(client)
        Logger.d("${request}")
        client.send(request.toString())
    }


    private fun base64(url: String): String {
        val uri = Uri.fromFile(File(url))
        val inputStream = appContext.contentResolver.openInputStream(uri)
        val out = ByteArrayOutputStream()

        val buffer = ByteArray(4096)
        if (inputStream != null) {
            var read: Int
            while ((inputStream.read(buffer).also { read = it }) != -1) {
                out.write(buffer, 0, read)
            }
        }
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }


}


class AriaRequest(
    method: Aria2Method,
    params: Array<*>
) {
    val method: Aria2Method
    val params: Array<*>
    var id: Long = 1

    init {
        this.method = method
        this.params = params
    }

    @Throws(JSONException::class)
    fun build(client: WebSocketClient): JSONObject {
        val request = JSONObject()
        request.put("jsonrpc", "2.0")
        request.put("id", client.nextMsgId().toString())
        request.put("method", method.method)
        val params: JSONArray =
            baseRequestParams()
        for (obj in this.params) params.put(obj)
        request.put("params", params)
        return request
    }
}

data class Request2(
    val jsonrpc: String = "2.0",
    val id: String,
    val method: String,
    val params: Array<Any>
)


private fun baseRequestParams(): JSONArray {
    val array = JSONArray()
    array.put("token:station")
    return array
}

data class AriaResponse(
    val id: String?,
    val method: String?,
    val result: Any?,
    val error: Any?
)

