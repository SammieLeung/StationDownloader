package com.station.stationdownloader.data.source.local.engine.aria2

import androidx.annotation.WorkerThread
import com.orhanobut.logger.Logger
import com.station.stationkitkt.MoshiHelper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * author: Sam Leung
 * date:  2023/5/9
 */
class WebSocketClient() {
    private var client: OkHttpClient
    private var webSocket: WeakReference<WebSocket>? = null
    private val msgId = AtomicLong(1)
    private val requests: ConcurrentHashMap<Long, InternalResponse> =
        ConcurrentHashMap<Long, InternalResponse>()

    @Volatile
    private var closed = false

    init {
        client = buildClient()
        webSocket = WeakReference(
            client.newWebSocket(
                createWebsocketRequest(),
                object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        Logger.i("raw#$text")
                        try {
                            val response = JSONObject(text)
                            if (response.getString("method")?.startsWith("aria2.on") == true)
                                return;

                            val internalResponse =
                                requests[response?.getString("id")?.toLong()] ?: return

                            try {
                                validateResponse(response)
                                internalResponse.success(response)
                            } catch (e: Aria2Exception) {
                                internalResponse.failed(e)
                            }
                        } catch (e: JSONException) {
                            Logger.e(e.message.toString())
                        }
                    }

                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        super.onOpen(webSocket, response)
                        Logger.d("onOpen")

                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?
                    ) {
                        super.onFailure(webSocket, t, response)
                        t.printStackTrace()
                    }
                }
            )
        )

    }

    private fun buildClient(): OkHttpClient {
        val timeout = 5
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(timeout.toLong(), TimeUnit.SECONDS)
            .readTimeout(timeout.toLong(), TimeUnit.SECONDS)
            .writeTimeout(timeout.toLong(), TimeUnit.SECONDS)
        return builder.build()
    }


    @WorkerThread
    fun close() {
        closed = true
        if (webSocket != null && webSocket?.get() != null) {
            webSocket?.get()?.close(1000, null)
            webSocket?.clear()
        }
        for (internal in requests.values) internal.failed(IOException("Client has been closed."))
    }

    fun send(request: AriaRequest) {
        val jsonObject = request.build(this)
        send(jsonObject.toString())
    }

    fun send(msg: String) {
        webSocket?.get()?.send(msg)
    }

    @Throws(Exception::class)
    @WorkerThread
    fun sendSync(id: Long, request: JSONObject): JSONObject {
        val internalResponse = InternalResponse()
        requests.put(id, internalResponse)
        webSocket?.get()?.send(request.toString())
        synchronized(internalResponse) {
            internalResponse.wait(5000)
            requests.remove(id)
            if (internalResponse.data != null)
                return internalResponse.data as JSONObject
            else
                throw internalResponse.exception!!
        }
    }


    fun createWebsocketRequest(): Request {
        val builder = Request.Builder()
        builder.url(
            URI(
                "ws",
                null,
                "localhost",
                6801,
                "/jsonrpc",
                null,
                null
            ).toString()
        )
        return builder.build()
    }


    fun nextMsgId(): Long {
        return msgId.getAndIncrement()
    }

    @Throws(JSONException::class, Aria2Exception::class)
    fun validateResponse(resp: JSONObject) {
        if (resp.has("error")) throw Aria2Exception(resp.getJSONObject("error"))
    }

    class InvalidUrlException : java.lang.Exception {
        internal constructor(message: String?) : super(message)
        internal constructor(cause: Throwable?) : super(cause)
    }

    private fun tellStatus(gid: String) {
        val request = Request2(
            id = nextMsgId().toString(),
            method = Aria2Method.TELL_STATUS.method,
            params = arrayOf(
                "token:station",
                gid,
                arrayOf(
                    "status",
                    "totalLength",
                    "completedLength",
                    "downloadSpeed",
                    "dir",
                    "errorMessage"
                )
            )
        )
        val resquestJson = MoshiHelper.toJson(request)

        Logger.d("$resquestJson")
        send(resquestJson)

    }


}

