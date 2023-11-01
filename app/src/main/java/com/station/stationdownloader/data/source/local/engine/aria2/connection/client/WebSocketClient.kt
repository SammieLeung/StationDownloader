package com.station.stationdownloader.data.source.local.engine.aria2.connection.client

import android.util.Log
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.source.local.engine.aria2.connection.common.AuthFields
import com.station.stationdownloader.data.source.local.engine.aria2.connection.exception.Aria2Exception
import com.station.stationdownloader.data.source.local.engine.aria2.connection.exception.InvalidUrlException
import com.station.stationdownloader.data.source.local.engine.aria2.connection.profile.UserProfile
import com.station.stationdownloader.data.source.local.engine.aria2.connection.transport.Aria2Request
import com.station.stationdownloader.data.source.local.engine.aria2.connection.transport.Aria2RequestWithResult
import com.station.stationdownloader.data.source.local.engine.aria2.connection.util.Aria2NetUtils
import com.station.stationdownloader.utils.DLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.internal.notifyAll
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Closeable
import java.io.IOException
import java.lang.ref.WeakReference
import java.security.GeneralSecurityException
import java.util.concurrent.atomic.AtomicLong

open class WebSocketClient private constructor(
    val profile: UserProfile,
    val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val uiDispatcher: CoroutineDispatcher = Dispatchers.Main
) : Closeable, DLogger {
    private val requestIds: AtomicLong = AtomicLong(
        Long.MIN_VALUE
    )
    private val client: OkHttpClient = Aria2NetUtils.buildClient(profile)
    private val webSocket: WeakReference<WebSocket> = WeakReference(
        client.newWebSocket(
            Aria2NetUtils.createWebsocketRequest(profile),
            Aria2WebSocketListener()
        )
    )
    private var closed: Boolean = false
    private val requests: MutableMap<Long, InternalResponse> = mutableMapOf()

    /**
     * 发送同步请求
     * @param id 请求id
     * @param request 请求JSON
     */
    @Throws(Exception::class)
    suspend fun sendRequestSync(id: Long, request: JSONObject): JSONObject =
        withContext(defaultDispatcher) {
            if (closed) throw IllegalStateException("WebSocket is closed")

            val response = InternalResponse()

            requests[id] = response
//            logger("sendRequestSync:\n  $request")

            webSocket.get()?.send(request.toString())
                ?: throw IllegalStateException("WebSocket is closed")
            synchronized(response) {
                response.waitResponse(5000)
                requests.remove(id)
                return@withContext response.obj ?: throw response.ex
                    ?: throw Exception("No response")
            }
        }

    private suspend fun sendRequest(id: Long, request: JSONObject, onJsonResult: OnJson) =
        withContext(defaultDispatcher) {
            if (closed) {
                onJsonResult.onException(IllegalStateException("Client is closed $this"))
                return@withContext
            }
            try {
                onJsonResult.onResponse(sendRequestSync(id, request))
            } catch (ex: Exception) {
                onJsonResult.onException(ex)
            }
        }

    @Throws(Exception::class)
    suspend fun sendSync(request: Aria2Request): JSONObject = withContext(defaultDispatcher) {
        request.build(this@WebSocketClient).run {
            sendRequestSync(request.id, this)
        }
    }

    @Throws(Exception::class)
    suspend fun <R> sendSync(request: Aria2RequestWithResult<R>): R =
        withContext(defaultDispatcher) {
            request.build(this@WebSocketClient).run {
                val response = sendRequestSync(request.id, this)
                return@withContext request.responseProcessor.process(this@WebSocketClient, response)
            }
        }

    suspend fun send(request: Aria2Request, @UiThread onSuccess: OnSuccess) =
        withContext(defaultDispatcher) {
            try {
                //必须运行build，否则request.id为0
                request.build(this@WebSocketClient).run {
                    sendRequest(id = request.id, request = this, onJsonResult = object : OnJson {
                        override fun onResponse(response: JSONObject) {
                            launch(uiDispatcher) {
                                onSuccess.onSuccess()
                            }
                        }

                        override fun onException(ex: Exception) {
                            launch(uiDispatcher) {
                                onSuccess.onException(ex)
                            }
                        }
                    })
                }
            } catch (ex: JSONException) {
                launch(uiDispatcher) {
                    onSuccess.onException(ex)
                }
            }
        }

    suspend fun <R> send(request: Aria2RequestWithResult<R>, onResult: OnResult<R>) =
        withContext(defaultDispatcher) {
            try {
                request.build(this@WebSocketClient).run {
                    sendRequest(request.id, this, object : OnJson {
                        override fun onResponse(response: JSONObject) {
                            launch(uiDispatcher) {
                                try {
                                    onResult.onResult(
                                        request.responseProcessor.process(
                                            this@WebSocketClient,
                                            response
                                        )
                                    )
                                } catch (jsonEx: JSONException) {
                                    onResult.onException(jsonEx)
                                }
                            }
                        }

                        override fun onException(ex: Exception) {
                            launch(uiDispatcher) {
                                onResult.onException(ex)
                            }
                        }
                    })
                }

            } catch (ex: JSONException) {
                launch(uiDispatcher) {
                    onResult.onException(ex)
                }
            }
        }


    fun nextRequestId(): Long {
        synchronized(requestIds) { return requestIds.getAndIncrement() }
    }

    @Throws(JSONException::class, Aria2Exception::class)
    fun validateResponse(resp: JSONObject) {
        if (resp.has("error")) throw Aria2Exception(resp.getJSONObject("error"))
    }

    fun baseRequestParams(): JSONArray {
        val array = JSONArray()
        if (profile.authMethod === AuthFields.AuthMethod.TOKEN) array.put("token:" + profile.serverToken)
        return array
    }

    override fun close() {
        closed = true

        webSocket.get()?.close(1000, null)
        webSocket.clear()
        for (internal in requests.values) internal.exception(IOException("Client has been closed."))
        requests.clear()

        ClientInstanceHolder.hasBeenClosed(this)
    }

    inner class Aria2WebSocketListener : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            if (closed) return

            val response =
                try {
                    JSONObject(text)
                } catch (ex: JSONException) {
                    //TODO 捕获错误到日志.
                    ex.printStackTrace()
                    return
                }

            val method: String = response.optString("method", "")
//            logger("onMessage:\n  $text")
            if (method.isNotEmpty() && method.startsWith("aria2.on")) return
            val internal: InternalResponse =
                requests[response.getString("id").toLong()]
                    ?: return
            try {
                validateResponse(response)
                internal.json(response)
            } catch (ex: Aria2Exception) {
                ex.printStackTrace()
                internal.exception(ex)
            } catch (ex: JSONException) {
                ex.printStackTrace()
                internal.exception(ex)
            }
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            logger("WebSocket: onOpen")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            logger("WebSocket: onFailure \n $t \n $response")
        }

    }

    interface OnSuccess {
        @UiThread
        fun onSuccess()

        @UiThread
        fun onException(ex: Exception)
    }

    interface OnResult<R> {
        @UiThread
        fun onResult(result: R)

        @UiThread
        fun onException(ex: Exception)
    }

    interface OnJson {
        @WorkerThread
        @Throws(JSONException::class)
        fun onResponse(response: JSONObject)

        @WorkerThread
        fun onException(ex: Exception)
    }

    private inner class InternalResponse {
        @Volatile
        var obj: JSONObject? = null

        @Volatile
        var ex: Exception? = null

        @Synchronized
        fun json(obj: JSONObject) {
            this.obj = obj
            ex = null
            notifyAll()
        }

        @Synchronized
        fun exception(ex: Exception) {
            this.ex = ex
            obj = null
            notifyAll()
        }

        @Throws(InterruptedException::class)
        fun waitResponse(timeout: Long) {
            (this as Object).wait(timeout)
        }
    }

    enum class Method(val method: String) {
        TELL_STATUS("aria2.tellStatus"),
        TELL_ACTIVE("aria2.tellActive"),
        TELL_WAITING("aria2.tellWaiting"),
        TELL_STOPPED("aria2.tellStopped"),
        UNPAUSE("aria2.unpause"),
        REMOVE("aria2.remove"),
        FORCE_PAUSE("aria2.forcePause"),
        FORCE_REMOVE("aria2.forceRemove"),
        REMOVE_RESULT("aria2.removeDownloadResult"),
        GET_VERSION("aria2.getVersion"),
        PAUSE_ALL("aria2.pauseAll"),
        GET_SESSION_INFO("aria2.getSessionInfo"),
        SAVE_SESSION("aria2.saveSession"),
        UNPAUSE_ALL("aria2.unpauseAll"),
        FORCE_PAUSE_ALL("aria2.forcePauseAll"),
        PURGE_DOWNLOAD_RESULTS("aria2.purgeDownloadResult"),
        PAUSE("aria2.pause"),
        LIST_METHODS("system.listMethods"),
        GET_GLOBAL_STATS("aria2.getGlobalStat"),
        GET_GLOBAL_OPTIONS("aria2.getGlobalOption"),
        CHANGE_GLOBAL_OPTIONS("aria2.changeGlobalOption"),
        ADD_URI("aria2.addUri"),
        ADD_TORRENT("aria2.addTorrent"),
        ADD_METALINK("aria2.addMetalink"),
        GET_SERVERS("aria2.getServers"),
        GET_PEERS("aria2.getPeers"),
        GET_DOWNLOAD_OPTIONS("aria2.getOption"),
        GET_FILES("aria2.getFiles"),
        CHANGE_POSITION("aria2.changePosition"),
        CHANGE_DOWNLOAD_OPTIONS("aria2.changeOption")
    }

    companion object {

        @JvmStatic
        @Throws(InitializationException::class)
        fun instantiate(profile: UserProfile): WebSocketClient {
            return try {
                WebSocketClient(profile)
            } catch (ex: InvalidUrlException) {
                throw InitializationException(ex)
            } catch (ex: GeneralSecurityException) {
                throw InitializationException(ex)
            } catch (ex: IOException) {
                throw InitializationException(ex)
            }
        }
    }

    override fun DLogger.tag(): String {
        return "WebSocketClient"
    }

}

class InitializationException internal constructor(cause: Throwable?) :
    java.lang.Exception(cause)
