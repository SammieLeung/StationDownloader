package com.station.stationdownloader.data.source.local.engine.aria2

import android.content.Context
import android.net.Uri
import android.os.Messenger
import android.util.Base64
import com.gianlu.aria2lib.Aria2Ui
import com.gianlu.aria2lib.BadEnvironmentException
import com.gianlu.aria2lib.commonutils.Prefs
import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.local.engine.IEngine
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.internal.wait
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * author: Sam Leung
 * date:  2023/5/9
 */
class Aria2Engine internal constructor(
    private val appContext: Context,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : IEngine {
    private var aria2Service: Aria2UiDispatcher = Aria2UiDispatcher(appContext)
    override suspend fun init(): IResult<String> = withContext(defaultDispatcher) {
        try {
            if (!aria2Service.ui.hasEnv()) {
                aria2Service.ui.loadEnv(appContext)
                aria2Service.ui.startService()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext IResult.Error(e)
        }
        return@withContext IResult.Success("${DownloadEngine.ARIA2}[${aria2Service.ui.version()}")


//        Prefs.init(appContext)
//        val aria2 = Aria2.get()
//        val parent: File = appContext.filesDir
//        aria2.loadEnv(
//            parent,
//            File(appContext.applicationInfo.nativeLibraryDir, "libaria2c.so"),
//            File(parent, "session")
//        )
//
//        aria2.start()
//        client = WebSocketClient()
    }

    fun addAria2UiListener(listener: Aria2Ui.Listener) {
        aria2Service.listeners.add(listener)
        aria2Service.ui.askForStatus()
    }

    fun removeAria2UiListener(listener: Aria2Ui.Listener) {
        aria2Service.listeners.remove(listener)
    }

    override suspend fun unInit() {
        aria2Service.ui.unbind()
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


    override suspend fun configure(key: String, values: String): IResult<String> {
        return IResult.Success(Pair(key, values).toString())
    }

    override suspend fun getEngineStatus(): IEngine.EngineStatus {
            val data = suspendCoroutine {
                val listener = object : Aria2Ui.Listener {
                    override fun onUpdateLogs(msg: MutableList<Aria2Ui.LogMessage>) {
                    }

                    override fun onMessage(msg: Aria2Ui.LogMessage) {
                    }

                    override fun updateUi(on: Boolean) {
                        it.resume(on)
                        aria2Service.listeners.remove(this)
                    }
                }
                aria2Service.listeners.add(listener)
                aria2Service.ui.askForStatus()
            }
            return if (data)
                IEngine.EngineStatus.ON
            else
                IEngine.EngineStatus.OFF
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
//        val request = addUriRequest(url).build(client)
//        Logger.d("${request.toString()}")
//        client.send(request.toString())
    }


    private fun addTorrent(url: String) {
//        val request = addTorrentRequest(url).build(client)
//        Logger.d("${request}")
//        client.send(request.toString())
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

    private inner class Aria2UiDispatcher(context: Context) : Aria2Ui.Listener {
        init {
            Prefs.init(context)
        }

        val listeners: MutableSet<Aria2Ui.Listener> = mutableSetOf()

        val ui: Aria2Ui = Aria2Ui(context, this)

        override fun onUpdateLogs(msg: MutableList<Aria2Ui.LogMessage>) {
        }

        override fun onMessage(msg: Aria2Ui.LogMessage) {
            for (listener in listeners) listener.onMessage(msg)
        }

        override fun updateUi(on: Boolean) {
            for (listener in listeners) listener.updateUi(on)
        }
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

