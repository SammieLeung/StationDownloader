package com.station.stationdownloader.data.source.local.engine.aria2.connection.exception

class InvalidUrlException : Exception {
        internal constructor(message: String?) : super(message)
        internal constructor(cause: Throwable?) : super(cause)
    }