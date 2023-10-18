package com.station.stationdownloader.data.source.local.engine.aria2.connection.transport

import com.station.stationdownloader.data.source.local.engine.aria2.connection.client.WebSocketClient.Method

class Aria2RequestWithResult<R>(
    method: Method,
    val responseProcessor: ResponseProcessor<R>,
    _params: Array<Any>
) : Aria2Request(method, _params)