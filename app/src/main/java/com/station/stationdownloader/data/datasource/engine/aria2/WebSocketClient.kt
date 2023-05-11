package com.station.stationdownloader.data.datasource.engine.aria2

import android.util.Log
import okhttp3.*
import java.lang.ref.WeakReference
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * author: Sam Leung
 * date:  2023/5/9
 */
class WebSocketClient() {
    protected var client: OkHttpClient
    private var webSocket: WeakReference<WebSocket>? = null

    //endregion
    private fun buildClient(): OkHttpClient {
        val timeout = 5
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(timeout.toLong(), TimeUnit.SECONDS)
            .readTimeout(timeout.toLong(), TimeUnit.SECONDS)
            .writeTimeout(timeout.toLong(), TimeUnit.SECONDS)
        return builder.build()
    }

    init {
        client = buildClient()
        webSocket = WeakReference(
            client.newWebSocket(
                createWebsocketRequest(),
                object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        super.onMessage(webSocket, text)
                        Log.v("test",text)
                    }

                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        super.onOpen(webSocket, response)
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?
                    ) {
                        super.onFailure(webSocket, t, response)
                    }
                }
            )
        )
    }

    fun send(msg:String){
        webSocket?.get()?.send(msg)
    }


    @Throws(InvalidUrlException::class)
    fun createWebsocketRequest(): Request {
        val builder = Request.Builder()
        builder.url(createWebSocketURL().toString())
        return builder.build()
    }

    fun createWebSocketURL(): URI? {
        return try {
            URI(
                "ws",
                null,
                "localhost",
                6800,
                "/jsonrpc",
                null,
                null
            )
        } catch (ex: Exception) {
            throw InvalidUrlException(ex)
        }
    }


    class InvalidUrlException : java.lang.Exception {
        internal constructor(message: String?) : super(message)
        internal constructor(cause: Throwable?) : super(cause)
    }
}