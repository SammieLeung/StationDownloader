package com.station.stationdownloader.data.source.local.engine.aria2.connection.client

import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.source.local.engine.aria2.connection.profile.UserProfile
import com.station.stationdownloader.data.source.local.engine.aria2.connection.transport.Aria2Request
import com.station.stationdownloader.data.source.local.engine.aria2.connection.transport.Aria2RequestWithResult

class ClientInstanceHolder private constructor() {
    private var current: WebSocketClient? = null
    private var reference: Reference=Reference()

    @Throws(InitializationException::class)
    private fun handleInstantiate(profile: UserProfile) {
        current?.close()
        current = WebSocketClient.instantiate(profile)
    }


    companion object {
        @JvmStatic
        private val instance: ClientInstanceHolder = ClientInstanceHolder()

        @JvmStatic
        fun close() {
            instance.current?.close()
            instance.current = null
        }

        fun hasBeenClosed(client: WebSocketClient) {
            if (instance.current == client) {
                instance.current = null
            }
        }

        @JvmStatic
        @Throws(InitializationException::class)
        fun instantiate(profile: UserProfile):Reference {
            instance.handleInstantiate(profile)
            return instance.reference
        }
    }

    inner class Reference : ClientInterface {
        override fun onClose() {
            current?.close()
        }

        override suspend fun <R> send(
            requestWithResult: Aria2RequestWithResult<R>,
            onResult: WebSocketClient.OnResult<R>
        ) {
            current?.send(requestWithResult, onResult)
        }


        override suspend fun send(request: Aria2Request, onSuccess: WebSocketClient.OnSuccess) {
            current?.send(request, onSuccess)
        }

        override suspend fun <R> batch(
            sandbox: WebSocketClient.BatchSandBox<R>,
            listener: WebSocketClient.OnResult<R>
        ) {
            current?.batch(sandbox, listener)
        }

        override fun setNotifyListener(onNotify: WebSocketClient.OnNotify?) {
            current?.setNotifyListener(onNotify)
        }



    }
}