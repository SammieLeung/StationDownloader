package com.station.stationdownloader.data.source.local.engine.aria2.connection.client

import com.station.stationdownloader.data.source.local.engine.aria2.connection.transport.Aria2Request
import com.station.stationdownloader.data.source.local.engine.aria2.connection.transport.Aria2RequestWithResult

interface ClientInterface {
    fun onClose()
    suspend fun <R> send(
        requestWithResult: Aria2RequestWithResult<R>,
        onResult: WebSocketClient.OnResult<R>
    )

    suspend fun send(request: Aria2Request, onSuccess: WebSocketClient.OnSuccess)
}