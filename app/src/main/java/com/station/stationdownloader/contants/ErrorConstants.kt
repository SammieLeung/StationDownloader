package com.station.stationdownloader.contants


enum class ConfigureError {
    INSUFFICIENT_NUMBER_OF_PARAMETERS,
    CONFIGURE_ERROR,
    NOT_SUPPORT_CONFIGURATION
}

enum class TaskExecuteError {
    NOT_SUPPORT_URL,
    IS_NOT_TORRENT
}

enum class EngineError {
    ENGINE_INIT_FAILED
}
